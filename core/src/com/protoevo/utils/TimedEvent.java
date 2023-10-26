package com.protoevo.utils;

import java.util.function.Supplier;
import java.util.function.Function;

public class TimedEvent {

    private float timer;
    private Runnable event;
    private Function<Float, Boolean> fireCondition;

    public TimedEvent(Function<Float, Boolean> fireCondition, Runnable event) {
        this.fireCondition = fireCondition;
        this.event = event;
        this.timer = 0;
    }

    public void update(float delta) {
        timer += delta;
        if (fireCondition.apply(timer)) {
            event.run();
            timer = 0;
        }
    }
}
