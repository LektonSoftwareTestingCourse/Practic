package com.processing.common.utils.events;

public interface EventNotifier<E extends Event> {
    void notify(E event);
}
