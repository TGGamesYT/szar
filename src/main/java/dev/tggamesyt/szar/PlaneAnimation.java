package dev.tggamesyt.szar;

public enum PlaneAnimation {
    START_ENGINE(false),
    STOP_ENGINE(false),
    FLYING(true),
    LANDING(false),
    LAND_STARTED(true),
    LIFT_UP(false);

    public final boolean looping;

    PlaneAnimation(boolean looping) {
        this.looping = looping;
    }
}

