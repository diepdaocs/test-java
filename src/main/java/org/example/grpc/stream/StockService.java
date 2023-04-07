package org.example.grpc.stream;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.ThreadLocalRandom;

public class StockService extends StockQuoteProviderGrpc.StockQuoteProviderImplBase {
    @Override
    public StreamObserver<Stock> get(StreamObserver<StockQuote> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Stock request) {
                System.out.printf("Server onNext [name=%s, ticket=%s]%n",
                        request.getCompanyName(), request.getTickerSymbol());
                for (int i = 1; i <= 5; i++) {
                    StockQuote stockQuote = StockQuote.newBuilder()
                            .setPrice(fetchStockPrice(request))
                            .setOfferNumber(i)
                            .setDescription(request.getTickerSymbol())
                            .build();
                    responseObserver.onNext(stockQuote);
                }
            }

            @Override
            public void onCompleted() {
                System.out.println("Server onCompleted");
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Server onError");
                throwable.printStackTrace();
            }
        };
    }

    private double fetchStockPrice(Stock stock) {
        return stock.getTickerSymbol().length() + ThreadLocalRandom.current()
                .nextDouble(-0.1d, 0.1d);
    }
}
