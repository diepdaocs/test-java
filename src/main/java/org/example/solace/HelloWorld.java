package org.example.solace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.ServiceProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver.MessageHandler;
import com.solace.messaging.resources.Topic;
import com.solace.messaging.resources.TopicSubscription;

/**
 * This simple introductory sample shows an application that both publishes and subscribes.
 */
public class HelloWorld {

    private static final String SAMPLE_NAME = HelloWorld.class.getSimpleName();
    private static final String TOPIC_PREFIX = "solace/samples/";  // used as the topic "root"
    private static final String API = "Java";
    private static volatile boolean isShutdown = false;           // are we done yet?

    /**
     * Simple application for doing pub/sub.
     */
    public static void main(String... args) throws IOException {
//        if (args.length < 3) {  // Check command line arguments
//            System.out.printf("Usage: %s <host:port> <message-vpn> <client-username> [password]%n", SAMPLE_NAME);
//            System.out.printf("  e.g. %s localhost default default%n%n", SAMPLE_NAME);
//            System.exit(-1);
//        }
        // User prompt, what is your name??, to use in the topic
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String uniqueName = "";
        while (uniqueName.isEmpty()) {
            System.out.print("Hello! Enter your name, or a unique word: ");
            uniqueName = reader.readLine().trim().replaceAll("\\s+", "_");  // clean up whitespace
        }

        System.out.println(API + " " + SAMPLE_NAME + " initializing...");
        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, "localhost:55555");          // host:port
        properties.setProperty(ServiceProperties.VPN_NAME, "default");     // message-vpn
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, "admin");      // client-username
//        if (args.length > 3) {
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, "admin");  // client-password
//        }
        properties.setProperty(ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties).build().connect();  // blocking connect to the broker

        // create and start the publisher
        final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1).build().start();

        // create and start the subscriber
        final DirectMessageReceiver receiver = messagingService.createDirectMessageReceiverBuilder()
                .withSubscriptions(TopicSubscription.of(TOPIC_PREFIX + "*/hello/>")).build().start();
        final MessageHandler messageHandler = (inboundMessage) -> {
            System.out.printf("vvv RECEIVED A MESSAGE vvv%n%s===%n", inboundMessage.dump());  // just print
        };
        receiver.receiveAsync(messageHandler);

        System.out.printf("%nConnected and subscribed. Ready to publish. Press [ENTER] to quit.%n");
        System.out.printf(" ~ Run this sample twice split screen to see true publish-subscribe. ~%n%n");

        OutboundMessageBuilder messageBuilder = messagingService.messageBuilder();
        while (System.in.available() == 0 && !isShutdown) {  // loop now, just use main thread
            try {
                Thread.sleep(5000);  // take a pause
                // payload is our "hello world" message from you!
                OutboundMessage message = messageBuilder.build(String.format("Hello World from %s!", uniqueName));
                // make a dynamic topic: solace/samples/java/hello/[uniqueName]
                String topicString = TOPIC_PREFIX + API.toLowerCase() + "/hello/" + uniqueName.toLowerCase();
                System.out.printf(">> Calling send() on %s%n", topicString);
                publisher.publish(message, Topic.of(topicString));
            } catch (RuntimeException e) {
                System.out.printf("### Exception caught during producer.send(): %s%n", e);
                isShutdown = true;
            } catch (InterruptedException e) {
                // Thread.sleep() interrupted... probably getting shut down
            }
        }
        isShutdown = true;
        publisher.terminate(500);
        receiver.terminate(500);
        messagingService.disconnect();
        System.out.println("Main thread quitting.");
    }
}
