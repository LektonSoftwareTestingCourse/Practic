package com.processing.authorization.events;

import java.util.List;

import org.springframework.stereotype.Component;

import com.processing.authorization.listeners.AuthorizationEventListener;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class AuthorizationEventNotifierImpl implements AuthorizationEventNotifier {
    private final List<AuthorizationEventListener> listeners;

    @Override
    public void notify(AuthorizationEvent event) {
        for (var l : listeners) {
            l.onEvent(event);
        }
    }
}
