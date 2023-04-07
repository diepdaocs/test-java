package org.example.grpc.stream;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.example.grpc.hello.HelloRequest;
import org.example.grpc.hello.HelloResponse;
import org.example.grpc.hello.HelloServiceGrpc;

import java.util.Arrays;
import java.util.List;

public class StockClient {
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub stub
                = HelloServiceGrpc.newBlockingStub(channel);

        HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder()
                .setFirstName("Diep")
                .setLastName("Dao")
                .build());

        System.out.println(helloResponse.getGreeting());

        StockQuoteProviderGrpc.StockQuoteProviderStub streamStub = StockQuoteProviderGrpc.newStub(channel);
        StreamObserver<StockQuote> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(StockQuote stockQuote) {
                System.out.printf("- Client onNext [offerNo=%s, name=%s, price=%s]%n",
                        stockQuote.getOfferNumber(), stockQuote.getDescription(), stockQuote.getPrice());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Client onError");
            }

            @Override
            public void onCompleted() {
                System.out.println("Client onCompleted");
            }
        };

        StreamObserver<Stock> streamObserver = streamStub.get(responseObserver);
        try {
            for (Stock stock : stocks()) {
                System.out.printf("Stock request [name=%s, ticket=%s]%n", stock.getCompanyName(), stock.getTickerSymbol());
                streamObserver.onNext(stock);
                Thread.sleep(1000);
            }
        } catch (RuntimeException e) {
            streamObserver.onError(e);
            throw e;
        }
        streamObserver.onCompleted();

        channel.shutdown();
    }

    private static List<Stock> stocks() {
        return Arrays.asList(
                Stock.newBuilder().setTickerSymbol("AU").setCompanyName("Auburn Corp").setDescription("Aptitude Intel").build()
                , Stock.newBuilder().setTickerSymbol("BAS").setCompanyName("Bassel Corp").setDescription("Business Intel").build()
                , Stock.newBuilder().setTickerSymbol("COR").setCompanyName("Corvine Corp").setDescription("Corporate Intel").build()
                , Stock.newBuilder().setTickerSymbol("DIA").setCompanyName("Dialogic Corp").setDescription("Development Intel").build()
                , Stock.newBuilder().setTickerSymbol("EUS").setCompanyName("Euskaltel Corp").setDescription("English Intel").build());
    }
}
