package dev.tggamesyt.szar.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.animation.Animation;

import static dev.tggamesyt.szar.PlaneAnimation.*;
import dev.tggamesyt.szar.PlaneAnimation;

@Environment(EnvType.CLIENT)
public class PlaneAnimationResolver {

    public static Animation resolve(PlaneAnimation anim) {
        return switch (anim) {
            case START_ENGINE -> PlaneEntityAnimations.start_engine;
            case STOP_ENGINE -> PlaneEntityAnimations.stop_engine;
            case FLYING -> PlaneEntityAnimations.flying;
            case LANDING -> PlaneEntityAnimations.landing;
            case LAND_STARTED -> PlaneEntityAnimations.land_started;
            case LIFT_UP -> PlaneEntityAnimations.lift_up;
        };
    }
}