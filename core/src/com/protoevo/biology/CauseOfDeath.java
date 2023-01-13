package com.protoevo.biology;

public enum CauseOfDeath {
    MURDER("murder"),
    OLD_AGE("old age"),
    SUFFOCATION("suffocation"),
    LOST_TOO_MUCH_MASS("withering away"),
    EATEN("being eaten"),
    GREW_TOO_LARGE("growing too large"),
    GREW_TOO_SMALL("growing too small"),
    HEALTH_TOO_LOW("running out of health"),
    DISPOSED("being disposed of"),
    ENV_CAPACITY_EXCEEDED("exceeding environment capacity"),
    LIGHTNING_STRIKE("a lightning strike"),
    CYTOKINESIS("cytokinesis"),
    THE_VOID("the void"),
    HEALED_TO_DEATH("being healed to death?...");  // should never happen

    private String reason;

    CauseOfDeath(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
