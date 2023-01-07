package com.protoevo.utils;

public enum DebugMode {
    OFF,
    SIMPLE_INFO,
    PHYSICS_DEBUG;

    public static DebugMode DEBUG_MODE = OFF;

    public static boolean isDebugMode() {
        return DEBUG_MODE != OFF;
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
}
