package dev.tggamesyt.szar.client;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BulletDecalStore {

    public record Decal(Vec3d pos, Direction face, long spawnTime) {}

    private static final List<Decal> decals = new ArrayList<>();
    static final long LIFETIME_MS = 30 * 1000;
    static final long FADE_START_MS = 27 * 1000;

    public static void add(Vec3d pos, Direction face) {
        decals.add(new Decal(pos, face, System.currentTimeMillis()));
    }

    public static List<Decal> getDecals() {
        // Clean up expired decals
        long now = System.currentTimeMillis();
        decals.removeIf(d -> now - d.spawnTime() > LIFETIME_MS);
        return decals;
    }
}