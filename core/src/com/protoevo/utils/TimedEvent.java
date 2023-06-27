package com.protoevo.utils;

import java.util.function.Supplier;

public class TimedEvent {

    private float timer;
    private Runnable event;
    private Supplier<Float> getFireTime;


    public TimedEvent(Supplier<Float> getFireTime, Runnable event) {
        this.getFireTime = getFireTime;
        this.event = event;
        this.timer = 0;
    }

    public void update(float delta) {
        timer += delta;
        if (timer >= getFireTime.get()) {
            event.run();
            timer = 0;
        }
    }
}
