package stream.zona1.subscriber;

import com.rabbitmq.client.*;

import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class KafkaConnector {

    private static final String RABBITMQ_EXCHANGE = "stream_zona1";
    private static final String KAFKA_TOPIC = "stream_zona1";
    ConnectionFactory factory;

    public KafkaConnector() {
        factory = new ConnectionFactory();
        factory.setHost("10.0.40.16");
        factory.setUsername("rabbit");
        factory.setPassword("rabbit");
    }

    public void suscribe() {
        try (Connection connection = factory.newConnection()) {
            try (Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(RABBITMQ_EXCHANGE, "fanout", true);

                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, RABBITMQ_EXCHANGE, "");

                MyConsumer consumer = new MyConsumer(channel);
                boolean autoack = true;
                String tag = channel.basicConsume(queueName, autoack, consumer);

                waitThread();
                channel.basicCancel(tag);
            }
        } catch (IOException | TimeoutException e) {
            // Exception
        }
    }

    public void waitThread() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // Exception
            }
        }
    }

    public synchronized void stop() {
        this.notify();
    }

    public class MyConsumer extends DefaultConsumer {

        public MyConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                AMQP.BasicProperties properties, byte[] body) throws IOException {
            String message = new String(body, StandardCharsets.UTF_8);
            System.out.println("[Bridge -> Kafka] " + message);

            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", "localhost:9092");
            kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);

            ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, message);
            producer.send(record);
            producer.flush();
            producer.close();
        }

    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        KafkaConnector subscriber = new KafkaConnector();
        System.out.println("[Bridge] Waiting for messages from RabbitMQ...");
        Thread waitThread = new Thread(() -> {
            scanner.nextLine();
            subscriber.stop();
        });
        waitThread.start();
        subscriber.suscribe();
        scanner.close();
    }
}