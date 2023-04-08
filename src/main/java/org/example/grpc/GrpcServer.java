package org.example.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.example.grpc.events.EventsService;
import org.example.grpc.hello.HelloService;
import org.example.grpc.stream.StockService;
import org.example.messaging.MessagingManager;
import org.example.solace.SolaceMessagingManager;

import java.io.IOException;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        final int port = 9090;
        MessagingManager messagingManager = new SolaceMessagingManager();
        Server server = ServerBuilder
                .forPort(port)
                .addService(new HelloService())
                .addService(new StockService())
                .addService(new EventsService(messagingManager))
                .build();

        System.out.printf("Starting gPRC server at %s:%s%n", "localhost", port);

        server.start();
        server.awaitTermination();
    }
}
