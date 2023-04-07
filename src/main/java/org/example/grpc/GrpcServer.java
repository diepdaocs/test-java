package org.example.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.example.grpc.hello.HelloService;

import java.io.IOException;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        final int port = 8080;
        Server server = ServerBuilder
                .forPort(port)
                .addService(new HelloService()).build();

        System.out.printf("Starting gPRC server at %s:%s%n", "localhost", port);

        server.start();
        server.awaitTermination();
    }
}
