package org.example.grpc.events;

import io.grpc.stub.StreamObserver;
import org.example.messaging.MessagingManager;

public class EventsService extends EventsServiceGrpc.EventsServiceImplBase {
    final MessagingManager messagingManager;

    public EventsService(MessagingManager messagingManager) {
        this.messagingManager = messagingManager;
        System.out.println("Event service started");
    }

    @Override
    public StreamObserver<EventRequest> event(StreamObserver<EventResponse> responseObserver) {
        return new EventsRequestObserver(responseObserver, messagingManager);
    }

    private void subscribe(SubscribeRequest subscribe, MessagingManager.Handler handler) throws Exception {
        final String context = subscribe.getContext();
        messagingManager.subscribe(context, handler);
    }

    private void unsubscribe(UnsubscribeRequest unsubscribe) throws Exception {
        final String context = unsubscribe.getContext();
        messagingManager.unsubscribe(context);
    }

    private void publish(PublishRequest publish) throws Exception {
        final String context = publish.getContext();
        final String payload = publish.getPayload();
        messagingManager.publish(context, payload);
    }

    public static class EventsRequestObserver implements StreamObserver<EventRequest> {
        private final StreamObserver<EventResponse> responseObserver;
        private final EventsService eventsService;

        private final Object lock = new Object();

        public EventsRequestObserver(StreamObserver<EventResponse> responseObserver, MessagingManager messagingManager) {
            this.responseObserver = responseObserver;
            this.eventsService = new EventsService(messagingManager);
        }

        @Override
        public void onNext(EventRequest eventRequest) {
            switch (eventRequest.getRequestCase()) {
                case SUBSCRIBEREQ -> {
                    System.out.println("Subscribe request");
                    try {
                        eventsService.subscribe(eventRequest.getSubscribeReq(), (context, payload) -> {
                            System.out.printf("Server stream event [context=%s, payload=%s, type=%s]%n", context, payload, payload.getClass());
                            safeResponse(EventResponse.newBuilder()
                                    .setEvent(Event.newBuilder()
                                            .setContext(context)
                                            .setPayload(payload)
                                            .build())
                                    .build());
                        });
                        safeResponse(EventResponse.newBuilder()
                                .setSubscribeResp(SubscribeResponse.newBuilder()
                                        .build())
                                .build());
                    } catch (Exception e) {
                        System.out.println("Subscribe error");
                        e.printStackTrace();
                        responseObserver.onError(e);
                    }
                }
                case UNSUBSCRIBEREQ -> {
                    System.out.println("Unsubscribe request");
                    try {
                        eventsService.unsubscribe(eventRequest.getUnsubscribeReq());
                        safeResponse(EventResponse.newBuilder()
                                .setUnsubscribeResp(UnsubscribeResponse.newBuilder()
                                        .build())
                                .build());
                    } catch (Exception e) {
                        System.out.println("Unsubscribe error");
                        responseObserver.onError(e);
                    }
                }
                case PUBLISHREQ -> {
                    System.out.println("Publish request");
                    try {
                        eventsService.publish(eventRequest.getPublishReq());
                        safeResponse(EventResponse.newBuilder()
                                .setPublishResp(PublishResponse.newBuilder()
                                        .build())
                                .build());
                    } catch (Exception e) {
                        System.out.println("Publish error");
                        e.printStackTrace();
                        responseObserver.onError(e);
                    }
                }
                default -> {
                }
            }
        }

        public void safeResponse(final EventResponse response) {
            synchronized (lock) {
                responseObserver.onNext(response);
            }
        }

        @Override
        public void onError(Throwable t) {
            System.out.println("Server onError");
            t.printStackTrace();
            System.out.println("------------------------------------------------------------------");
        }

        @Override
        public void onCompleted() {
            System.out.println("Server onCompleted");
            responseObserver.onCompleted();
            System.out.println("------------------------------------------------------------------");
        }
    }
}
