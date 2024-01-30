package org.example.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.client.handler.JsonStreamBodyHandler;
import org.example.api.client.handler.JsonStringBodyHandler;
import org.example.api.client.pojo.Todo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.function.Supplier;

public class RestClient {
    public static String sampleApiRequest() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/todos"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void main(String[] args) throws Exception {
//        test01();
//        test02();
        test03();

    }

    private static void test01() throws Exception {
        String body = sampleApiRequest();
        ObjectMapper objectMapper = new ObjectMapper();
        Todo[] todos = objectMapper.readValue(body, Todo[].class);

        Arrays.stream(todos).forEach(System.out::println);
    }

    private static void test02() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(new URI("https://jsonplaceholder.typicode.com/todos/1"))
                .header("Accept", "application/json")
                .build();

        HttpResponse<Supplier<Todo>> response = HttpClient.newHttpClient().send(request, new JsonStreamBodyHandler<>(Todo.class));

        System.out.println(response.body().get());
    }

    private static void test03() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(new URI("https://jsonplaceholder.typicode.com/todos/1"))
                .header("Accept", "application/json")
                .build();

        HttpResponse<Todo> response = HttpClient.newHttpClient().send(request, new JsonStringBodyHandler<>(Todo.class));

        System.out.println(response.body());
    }
}
