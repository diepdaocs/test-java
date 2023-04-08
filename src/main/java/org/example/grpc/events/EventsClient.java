package org.example.grpc.events;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class EventsClient {
    private final StreamObserver<EventRequest> streamObserver;

    public EventsClient(ManagedChannel channel) {
        EventsServiceGrpc.EventsServiceStub stub = EventsServiceGrpc.newStub(channel);
        streamObserver = stub.event(new EventsResponseObserver());
    }

    public static void main(String[] args) {
        final ManagedChannel channel = makeChannel();

        final EventsClient client = new EventsClient(channel);
        final String context = "Diep";
        final String payload = "You have won a million dollar prize!!!";
        try {
            client.subscribe(context);

            // Fishing
            for (int i = 0; i < 10; i++) {
                final String fishingPayload = payload + " [Fishing=%s]".formatted(i + 1);
                System.out.printf("Publishing [payload=%s]%n", fishingPayload);
                client.publish(context, fishingPayload);
            }
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));

            client.unsubscribe(context);
            client.close();
        } catch (final Throwable t) {
            client.error(t);
        }
    }

    private void unsubscribe(String context) {
        streamObserver.onNext(EventRequest.newBuilder()
                .setUnsubscribeReq(UnsubscribeRequest.newBuilder()
                        .setContext(context)
                        .build())
                .build());
    }

    private void publish(final String context, final String payload) {
        streamObserver.onNext(EventRequest.newBuilder()
                .setPublishReq(PublishRequest.newBuilder()
                        .setContext(context)
                        .setPayload(payload)
                        .build())
                .build());
    }

    private void error(final Throwable t) {
        streamObserver.onError(t);
    }

    private void close() {
        streamObserver.onCompleted();
    }

    private void subscribe(final String context) {
        streamObserver.onNext(EventRequest.newBuilder()
                .setSubscribeReq(SubscribeRequest.newBuilder()
                        .setContext(context)
                        .build())
                .build());
    }

    private static void onEvent(final String context, final String payload) {
        System.out.printf("Event to be handled [context=%s, payload=%s]%n", context, payload);
    }

    private static ManagedChannel makeChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
    }

    private static class EventsResponseObserver implements io.grpc.stub.StreamObserver<EventResponse> {
        @Override
        public void onNext(EventResponse eventResponse) {
            switch (eventResponse.getResponseCase()) {
                case SUBSCRIBERESP -> System.out.println("Subscribe response [SUCCESS]");
                case UNSUBSCRIBERESP -> System.out.println("Unsubscribe response [SUCCESS]");
                case PUBLISHRESP -> System.out.println("Publish response [SUCCESS]");
                case EVENT -> {
                    Event event = eventResponse.getEvent();
                    onEvent(event.getContext(), event.getPayload());
                }
                default -> {
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            System.out.println("Client onError");
            t.printStackTrace();
        }

        @Override
        public void onCompleted() {
            System.out.println("Client onCompleted");
        }
    }
}
