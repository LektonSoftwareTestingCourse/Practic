package com.processing.cardmanagement.events;

/**
 * Рассылает сообщения слушателям
 *
 * @see CardEvent
 * @see CardEventListener
 */
public interface CardEventNotifier {

    void notifyListeners(CardEvent event);
}
