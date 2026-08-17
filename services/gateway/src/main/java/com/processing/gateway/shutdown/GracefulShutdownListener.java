package com.processing.gateway.shutdown;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private final GracefulShutdownState shutdownState;

    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        shutdownState.startShutdown();
    }
}
