import os
import random
from PIL import Image
import json

# --------------------- CONFIG ---------------------
INPUT_PATH = r"slotmachine_slots.png"
OUTPUT_DIR = r""  # folder to save output
NUM_SLOTS = 3
SPEED = -4  # pixels per frame, positive = up, negative = down
# ---------------------------------------

os.makedirs(OUTPUT_DIR, exist_ok=True)

original = Image.open(INPUT_PATH)
width, height = original.size
FRAME_SIZE = width  # square frames

if height % FRAME_SIZE != 0:
    raise ValueError("Image height must be divisible by its width (square frames).")

num_frames = height // FRAME_SIZE
total_pixels = height  # total pixels for smooth sliding


# ---------------- HELPER FUNCTIONS ----------------
def create_animation_image(img, start_y, speed=SPEED):
    """
    Create rolling animation starting at absolute pixel y.
    Wraps around the image if crop goes past the bottom.
    Speed determines pixels per frame (positive = up, negative = down)
    """
    frames_list = []
    for i in range(total_pixels):
        y = (start_y + i * speed) % total_pixels
        if y + FRAME_SIZE <= total_pixels:
            frame = img.crop((0, y, width, y + FRAME_SIZE))
        else:
            part1 = img.crop((0, y, width, total_pixels))
            part2 = img.crop((0, 0, width, FRAME_SIZE - (total_pixels - y)))
            frame = Image.new("RGBA", (width, FRAME_SIZE))
            frame.paste(part1, (0, 0))
            frame.paste(part2, (0, part1.height))
        frames_list.append(frame)

    final_img = Image.new("RGBA", (width, FRAME_SIZE * len(frames_list)))
    for idx, frame in enumerate(frames_list):
        final_img.paste(frame, (0, idx * FRAME_SIZE))
    return final_img


def save_png_and_mcmeta(image, base_name):
    png_path = os.path.join(OUTPUT_DIR, base_name + ".png")
    mcmeta_path = png_path + ".mcmeta"
    image.save(png_path)
    json.dump({"animation": {"interpolate": False, "frametime": 1}},
              open(mcmeta_path, "w"), indent=4)


def generate_unique_shuffle(existing_orders):
    frames_indices = list(range(num_frames))
    attempt = 0
    while True:
        random.shuffle(frames_indices)
        conflict = False
        for order in existing_orders:
            for i, frame_idx in enumerate(frames_indices):
                if frame_idx == order[i]:
                    conflict = True
                    break
            if conflict:
                break
        if not conflict:
            return frames_indices
        attempt += 1
        if attempt > 1000:
            raise RuntimeError("Cannot generate unique shuffle after 1000 tries.")


# ---------------- GENERATE SLOTS ----------------
existing_orders = []

for slot_idx in range(1, NUM_SLOTS + 1):
    base_name = f"slot_{slot_idx}"

    # generate fully unique shuffled frame order
    shuffle_order = generate_unique_shuffle(existing_orders)
    existing_orders.append(shuffle_order)

    # build shuffled base image
    shuffled_img = Image.new("RGBA", (width, height))
    for i, frame_idx in enumerate(shuffle_order):
        frame = original.crop((0, frame_idx * FRAME_SIZE, width, (frame_idx + 1) * FRAME_SIZE))
        shuffled_img.paste(frame, (0, i * FRAME_SIZE))

    # main sliding animation
    main_offset = random.randint(0, total_pixels - 1)
    main_img = create_animation_image(shuffled_img, main_offset, speed=SPEED)
    save_png_and_mcmeta(main_img, base_name)

    # up/down relative to main, looping
    up_offset = (main_offset - FRAME_SIZE) % total_pixels
    down_offset = (main_offset + FRAME_SIZE) % total_pixels
    up_img = create_animation_image(shuffled_img, up_offset, speed=SPEED)
    down_img = create_animation_image(shuffled_img, down_offset, speed=SPEED)
    save_png_and_mcmeta(up_img, base_name + "_up")
    save_png_and_mcmeta(down_img, base_name + "_down")

print(f"Generated {NUM_SLOTS * 3 * 2} PNG+mcmeta files in {OUTPUT_DIR}")