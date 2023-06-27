package com.protoevo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TimedEventsManager {

    private final List<TimedEvent> timedEvents;

    public TimedEventsManager() {
        timedEvents = new ArrayList<>();
    }

    public void add(float fireTime, Runnable event) {
        timedEvents.add(new TimedEvent(() -> fireTime, event));
    }

    public void add(Supplier<Float> fireTime, Runnable event) {
        timedEvents.add(new TimedEvent(fireTime, event));
    }

    public void update(float delta) {
        for (TimedEvent timedEvent : timedEvents)
            timedEvent.update(delta);
    }

}
