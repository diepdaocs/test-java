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
        return new StreamObserver<>() {
            @Override
            public void onNext(EventRequest eventRequest) {
                switch (eventRequest.getRequestCase()) {
                    case SUBSCRIBEREQ -> {
                        System.out.println("Subscribe request");
                        try {
                            subscribe(eventRequest.getSubscribeReq(), ((context, payload) -> responseObserver.onNext(EventResponse.newBuilder()
                                    .setEvent(Event.newBuilder()
                                            .setContext(context)
                                            .setPayload(payload)
                                            .build())
                                    .build())));
                        } catch (Exception e) {
                            System.out.println("Subscribe error");
                            e.printStackTrace();
                            responseObserver.onError(e);
                        }
                        responseObserver.onNext(EventResponse.newBuilder()
                                .setSubscribeResp(SubscribeResponse.newBuilder()
                                        .build())
                                .build());

                    }
                    case UNSUBSCRIBEREQ -> {
                        System.out.println("Unsubscribe request");
                        try {
                            unsubscribe(eventRequest.getUnsubscribeReq());
                        } catch (Exception e) {
                            responseObserver.onError(e);
                        }
                        responseObserver.onNext(EventResponse.newBuilder()
                                .setUnsubscribeResp(UnsubscribeResponse.newBuilder()
                                        .build())
                                .build());
                    }
                    case PUBLISHREQ -> {
                        System.out.println("Publish request");
                        try {
                            publish(eventRequest.getPublishReq());
                        } catch (Exception e) {
                            System.out.println("Publish error");
                            e.printStackTrace();
                            responseObserver.onError(e);
                        }
                        responseObserver.onNext(EventResponse.newBuilder()
                                .setPublishResp(PublishResponse.newBuilder()
                                        .build())
                                .build());
                    }
                    default -> {
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Server onError");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Server onCompleted");
            }
        };
    }

    private void subscribe(SubscribeRequest subscribe, MessagingManager.Handler handler) throws Exception {
        final String context = subscribe.getContext();
        messagingManager.subscribe(context, handler);
    }

    private void unsubscribe(UnsubscribeRequest unsubscribe) throws Exception {
        final String context = unsubscribe.getContext();
        messagingManager.unsubscribe(context);
    }

    private void publish(PublishRequest publish) {
        final String context = publish.getContext();
        final String payload = publish.getPayload();
        messagingManager.publish(context, payload);
    }
}
