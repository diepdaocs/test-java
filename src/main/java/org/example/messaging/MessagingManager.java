package org.example.messaging;

public interface MessagingManager {
    interface Handler {
        void onEvent(String context, String payload);
    }

    void subscribe(final String context, final Handler handler) throws Exception;

    void unsubscribe(final String context) throws Exception;

    void publish(final String context, final String payload) throws Exception;
}
