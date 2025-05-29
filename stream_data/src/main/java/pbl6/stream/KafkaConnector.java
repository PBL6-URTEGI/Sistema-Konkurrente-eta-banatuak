package pbl6.stream;

import com.rabbitmq.client.*;
import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class KafkaConnector {

    private final static String RABBITMQ_EXCHANGE = "datos";
    private final static String KAFKA_TOPIC = "datos-embalse";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection rmqConnection = factory.newConnection();
        Channel channel = rmqConnection.createChannel();

        channel.exchangeDeclare(RABBITMQ_EXCHANGE, "fanout", true);
        
        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, RABBITMQ_EXCHANGE, "");

        // Kafka setup
        Properties kafkaProps = new Properties();
        kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);

        System.out.println("[Bridge] Waiting for messages from RabbitMQ...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("[Bridge] Received from RMQ: " + message);

            // Forward to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, message);
            // producer.send(record);
        };

        channel.basicConsume(queue, true, deliverCallback, consumerTag -> {});
    }
}
