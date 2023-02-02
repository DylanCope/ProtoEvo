package com.protoevo.utils;

public enum DebugMode {
    OFF,
    SIMPLE_INFO,
    INTERACTION_INFO,
    PHYSICS_DEBUG;

    public static DebugMode DEBUG_MODE = OFF;

    public static boolean isDebugMode() {
        return DEBUG_MODE != OFF;
    }

    public static boolean isInteractionInfo() {
        return DEBUG_MODE == INTERACTION_INFO;
    }

    public static boolean isDebugModePhysicsDebug() {
        return DEBUG_MODE == PHYSICS_DEBUG;
    }

    public static void cycleDebugMode() {
        switch (DEBUG_MODE) {
            case OFF:
                DEBUG_MODE = SIMPLE_INFO;
                break;
            case SIMPLE_INFO:
                DEBUG_MODE = INTERACTION_INFO;
                break;
            case INTERACTION_INFO:
                DEBUG_MODE = PHYSICS_DEBUG;
                break;
            case PHYSICS_DEBUG:
                DEBUG_MODE = OFF;
                break;
        }
    }

    public static void toggleDebug() {
        if (DEBUG_MODE == OFF) {
            DEBUG_MODE = SIMPLE_INFO;
        } else {
            DEBUG_MODE = OFF;
        }
    }

    public static boolean isModeOrHigher(DebugMode mode) {
        return DEBUG_MODE.ordinal() >= mode.ordinal();
    }

    public static void setMode(DebugMode mode) {
        DEBUG_MODE = mode;
    }

}
