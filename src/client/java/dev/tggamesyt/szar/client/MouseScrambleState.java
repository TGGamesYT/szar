package dev.tggamesyt.szar.client;
import dev.tggamesyt.szar.client.SzarClient.MouseScrambleMode;

public class MouseScrambleState {
    public static MouseScrambleMode active = MouseScrambleMode.NONE;
    public static boolean wasMoving = false;

    public static void reset() {
        active = MouseScrambleMode.NONE;
        wasMoving = false;
    }
}
