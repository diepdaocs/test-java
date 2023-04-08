package org.example.solace;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver;
import com.solace.messaging.resources.Topic;
import com.solace.messaging.resources.TopicSubscription;
import org.example.messaging.MessagingManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class SolaceMessagingManager implements MessagingManager {
    private static final String TOPIC_PREFIX = "solace/samples/";  // used as the topic "root"
    private MessagingService messagingService;
    private final DirectMessagePublisher publisher;
    private Map<String, DirectMessageReceiver> receiverByTopic;

    public SolaceMessagingManager() {
        connect();
        // create and start the publisher
        publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1).build().start();
        receiverByTopic = new ConcurrentHashMap<>();

        System.out.println("Connected to Solace messaging, ready to publish/subscribe!");
    }

    private void connect() {
        final Properties properties = new Properties();
        properties.setProperty(SolaceProperties.TransportLayerProperties.HOST, "localhost:55555");          // host:port
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, "default");     // message-vpn
        properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_USER_NAME, "admin");      // client-username
        properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_PASSWORD, "admin");  // client-password
        properties.setProperty(SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect

        messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties).build().connect();  // blocking connect to the broker
    }

    @Override
    public void subscribe(String context, Handler handler) throws InterruptedException {
        String topic = makeTopic(context);
        DirectMessageReceiver receiver = receiverByTopic.computeIfAbsent(topic, t -> messagingService.createDirectMessageReceiverBuilder().build().start());
        receiver.addSubscription(TopicSubscription.of(topic));
        final MessageReceiver.MessageHandler messageHandler = (inboundMessage) -> {
            handler.onEvent(context, inboundMessage.getPayloadAsString());
        };
        receiver.receiveAsync(messageHandler);
    }

    @Override
    public void unsubscribe(String context) throws InterruptedException {
        String topic = makeTopic(context);
        receiverByTopic.get(topic).removeSubscription(TopicSubscription.of(topic));
        receiverByTopic.remove(topic);
    }

    @Override
    public void publish(String context, String payload) {
        OutboundMessage message = messagingService.messageBuilder()
                .build(String.format(payload));

        publisher.publish(message, Topic.of(makeTopic(context)));
    }

    private String makeTopic(String context) {
        return TOPIC_PREFIX + context;
    }
}
