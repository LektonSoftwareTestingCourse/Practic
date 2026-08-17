package com.processing.gateway.shutdown;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GracefulShutdownState {

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    public void startShutdown() {
        shuttingDown.set(true);
    }
}
