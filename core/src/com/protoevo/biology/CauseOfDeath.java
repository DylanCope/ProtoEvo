package com.protoevo.biology;

import com.protoevo.utils.Utils;

public enum CauseOfDeath {
    SPIKE_DAMAGE("spike damage"),
    EATEN("being eaten"),
    OLD_AGE("old age"),
    LOST_TOO_MUCH_MASS("withering away"),
    HEALTH_TOO_LOW("running out of health", true),
    LIGHTNING_STRIKE("a lightning strike"),
    CYTOKINESIS("cytokinesis", true),
    THE_VOID("the void"),
    SUFFOCATION("suffocation", true),
    GREW_TOO_LARGE("growing too large", true),
    SHRUNK_TOO_MUCH("shrinking too much", true),
    DISPOSED("being disposed of", true),
    ENV_CAPACITY_EXCEEDED("exceeding environment capacity", true),
    HEALED_TO_DEATH("being healed to death?...", true),  // should never happen
    FAILED_TO_CONSTRUCT("failure to construct", true),
    OVERCROWDING("overcrowding", false),
    MEAT_DECAY("decay", true),
    HYPOTHERMIA("hypothermia", false),
    HYPERTHERMIA("hyperthermia", false);

    private final String reason;
    private final boolean debug;

    CauseOfDeath(String reason, boolean debug) {
        this.reason = reason;
        this.debug = debug;
    }

    CauseOfDeath(String reason) {
        this(reason, false);
    }

    public boolean isDebugDeath() {
        return debug;
    }

    public String getReason() {
        return reason;
    }

    public String getReasonSentence() {
        return Utils.capitalize(reason) + ".";
    }
}
