package com.protoevo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.Function;

public class TimedEventsManager {

    private final List<TimedEvent> timedEvents;

    public TimedEventsManager() {
        timedEvents = new ArrayList<>();
    }

    public void add(float fireTime, Runnable event) {
        timedEvents.add(new TimedEvent(t -> t >= fireTime, event));
    }

    public void add(Supplier<Float> fireTime, Runnable event) {
        timedEvents.add(new TimedEvent(t -> t >= fireTime.get(), event));
    }

    public void add(Function<Float, Boolean> fireCondition, Runnable event) {
        timedEvents.add(new TimedEvent(fireCondition, event));
    }

    public void update(float delta) {
        for (TimedEvent timedEvent : timedEvents)
            timedEvent.update(delta);
    }

}
