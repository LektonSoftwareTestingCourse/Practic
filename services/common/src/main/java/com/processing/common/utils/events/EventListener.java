package com.processing.common.utils.events;

public interface EventListener<E extends Event> {
    void onEvent(E event);
}
