package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.RevolverItem;

public class RevolverHudTicker {

    private static final float DECELERATION = 0.88F;
    private static final float STOP_THRESHOLD = 0.2F;

    public static void tick() {
        if (!RevolverHudState.isSpinning) return;

        RevolverHudState.spinAngle += RevolverHudState.spinVelocity;
        RevolverHudState.spinVelocity *= DECELERATION;

        if (Math.abs(RevolverHudState.spinVelocity) < STOP_THRESHOLD) {
            RevolverHudState.isSpinning = false;
            RevolverHudState.spinAngle = 0F;
            RevolverHudState.spinVelocity = 0F;
        }
    }

    public static void startSpin(int steps) {
        // Spin in the positive direction, one full rotation per step minimum
        RevolverHudState.spinAngle = 0F;
        RevolverHudState.spinVelocity = steps * (360F / RevolverItem.CHAMBERS) * 0.25F;
        RevolverHudState.isSpinning = true;
    }
}