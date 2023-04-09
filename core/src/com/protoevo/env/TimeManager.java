package com.protoevo.env;

import java.io.Serializable;

public class TimeManager implements Serializable {
    public static final long serialVersionUID = 1L;

    private float elapsedTime = 0f;
    private float timeOfDay = 0f;
    private long day = 0;

    public void update(float delta) {
        elapsedTime += delta;
        timeOfDay += delta;
        if (timeOfDay >= getDayNightCycleLength()) {
            timeOfDay = 0f;
            day++;
        }
    }

    public float getTimeOfDay() {
        return timeOfDay;
    }

    public float getTimeElapsed() {
        return elapsedTime;
    }

    public long getDay() {
        return day;
    }

    public float getDayNightCycleLength() {
        return Environment.settings.misc.dayNightCycleLength.get();
    }

    public float getTimeOfDayPercentage() {
        return getTimeOfDay() / getDayNightCycleLength();
    }
}
