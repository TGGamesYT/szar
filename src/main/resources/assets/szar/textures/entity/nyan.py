from PIL import Image
import os


def find_pixels_by_color(img, color):
    """Return list of (x, y) coordinates where the pixel matches the given color."""
    width, height = img.size
    pixels = img.load()
    coords = []
    for y in range(height):
        for x in range(width):
            if pixels[x, y][:3] == color:  # ignore alpha if exists
                coords.append((x, y))
    return coords


def place_image(base_img, overlay_img, corner_coords):
    """Resize overlay_img to fit corner_coords and paste onto base_img (sharp, pixel-perfect)."""
    if len(corner_coords) != 2:
        raise ValueError("corner_coords must have exactly two points")

    (x1, y1), (x2, y2) = corner_coords
    # Calculate target box (left, top, right, bottom)
    left = min(x1, x2)
    top = min(y1, y2)
    right = max(x1, x2)
    bottom = max(y1, y2)

    target_width = right - left + 1
    target_height = bottom - top + 1

    # Resize using NEAREST to keep pixels sharp
    resized_overlay = overlay_img.resize((target_width, target_height), Image.Resampling.NEAREST)
    base_img.paste(resized_overlay, (left, top), resized_overlay.convert("RGBA"))



def main(input_folder, texture_file, color1, color2, output_folder):
    os.makedirs(output_folder, exist_ok=True)

    texture = Image.open(texture_file).convert("RGBA")
    texture_width, texture_height = texture.size

    for filename in os.listdir(input_folder):
        if not filename.lower().endswith(".png"):
            continue
        input_path = os.path.join(input_folder, filename)
        overlay = Image.open(input_path).convert("RGBA")

        # Create a transparent image of the same size as the texture
        output_img = Image.new("RGBA", (texture_width, texture_height), (0, 0, 0, 0))

        # Process first color
        coords1 = find_pixels_by_color(texture, color1)
        if len(coords1) != 2:
            print(f"Warning: {filename} - color1 does not have exactly 2 pixels")
        else:
            place_image(output_img, overlay, coords1)

        # Process second color (flipped horizontally)
        coords2 = find_pixels_by_color(texture, color2)
        if len(coords2) != 2:
            print(f"Warning: {filename} - color2 does not have exactly 2 pixels")
        else:
            flipped_overlay = overlay.transpose(Image.FLIP_LEFT_RIGHT)
            place_image(output_img, flipped_overlay, coords2)

        # Save output
        output_path = os.path.join(output_folder, filename)
        output_img.save(output_path)
        print(f"Saved {output_path}")



if __name__ == "__main__":
    # Example usage
    input_folder = "nyan_cat_input"
    texture_file = "nyan_cat_example.png"
    color2 = (255, 0, 0)  # red
    color1 = (0, 138, 255)  # blue
    output_folder = "nyan_cat_textures"

    main(input_folder, texture_file, color1, color2, output_folder)
