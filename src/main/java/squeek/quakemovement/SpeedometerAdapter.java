package squeek.quakemovement;

import squeek.quakemovement.stubpackage.squeek.speedometer.HudSpeedometer;

import cpw.mods.fml.common.Loader;

public class SpeedometerAdapter {
    private static boolean speedometerPresent;
    public static void init() {
        speedometerPresent = Loader.isModLoaded("Squeedometer");
    }

    public static void setDidJumpThisTick(boolean value) {
        if (!speedometerPresent)
            return;

        HudSpeedometer.setDidJumpThisTick(value);
    }

    public static void setIsJumping(boolean value) {
        if (!speedometerPresent)
            return;

        HudSpeedometer.setIsJumping(value);
    }
}
