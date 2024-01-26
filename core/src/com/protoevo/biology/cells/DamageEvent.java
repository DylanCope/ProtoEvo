package com.protoevo.biology.cells;

import com.protoevo.biology.CauseOfDeath;

import java.io.Serializable;

public class DamageEvent implements Serializable {
    public static final long serialVersionUID = 1L;

    private float damageTime;
    private float damageAmount;
    private CauseOfDeath causeOfDamage;


    public DamageEvent(float damageTime, float damageAmount, CauseOfDeath causeOfDamage) {
        this.damageTime = damageTime;
        this.damageAmount = damageAmount;
        this.causeOfDamage = causeOfDamage;
    }

    public float getDamageTime() {
        return damageTime;
    }

    public float getDamageAmount() {
        return damageAmount;
    }

    public CauseOfDeath getCauseOfDamage() {
        return causeOfDamage;
    }

    public void setDamageTime(float damageTime) {
        this.damageTime = damageTime;
    }

    public void setDamageAmount(float damageAmount) {
        this.damageAmount = damageAmount;
    }

    public void setCauseOfDamage(CauseOfDeath causeOfDamage) {
        this.causeOfDamage = causeOfDamage;
    }
}
