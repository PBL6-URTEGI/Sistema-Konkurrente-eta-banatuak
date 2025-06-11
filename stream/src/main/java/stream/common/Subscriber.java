package stream.common;

import com.rabbitmq.client.*;

import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class Subscriber {

    private String rabbitmqTopic;
    private String kafkaTopic;
    ConnectionFactory factory;

    static final String EXCHANGE_STREAM = "stream";
    static final String EXCHANGE_DL = "dle";
    static final String QUEUE_DL = "subs_dlq";
    static final String RABBITMQ_PATH = "./src/main/resources/credentials/rabbit.txt";

    public Subscriber(String rabbitmqTopic, String kafkaTopic, String ipPath) throws IOException {
        String key = getKey(RABBITMQ_PATH);
        String ip = getKey(ipPath);

        factory = new ConnectionFactory();
        factory.setHost(ip);
        factory.setUsername("rabbit");
        factory.setPassword(key);
        this.rabbitmqTopic = rabbitmqTopic;
        this.kafkaTopic = kafkaTopic;
    }

    public static String getKey(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public void suscribe() throws InterruptedException {
        try (Connection connection = factory.newConnection()) {
            try (Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(EXCHANGE_STREAM, "topic", true);
                channel.exchangeDeclare(EXCHANGE_DL, "direct", true);

                String topic = "stream." + rabbitmqTopic;

                // Configura el exchange y el topic
                Map<String, Object> arguments = new HashMap<>();
                arguments.put("x-dead-letter-exchange", EXCHANGE_DL);
                arguments.put("x-dead-letter-routing-key", rabbitmqTopic);

                String deadLetterQueue = QUEUE_DL + "_" + rabbitmqTopic;

                channel.queueDeclare(deadLetterQueue, false, false, false, arguments);
                channel.queueBind(deadLetterQueue, EXCHANGE_STREAM, topic);

                // Recibe los datos que se envían desde el publisher
                MyConsumer consumer = new MyConsumer(channel);
                String tag = channel.basicConsume(deadLetterQueue, false, consumer);

                waitThread();
                channel.basicCancel(tag);
            }
        } catch (IOException | TimeoutException e) {
            // Exception
        }
    }

    public void waitThread() throws InterruptedException {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new InterruptedException();
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

            // Parsea el mensaje que recibe a JSON
            String[] parts = message.split("\\|");
            if (parts.length == 5) {
                String id = parts[0].trim();
                String timestamp = parts[1].trim();
                String altitude = parts[2].trim();
                String location = parts[3].trim();
                String side = parts[4].trim();

                String jsonMessage = String.format(
                        "{\"id\":\"%s\",\"timestamp\":\"%s\",\"altitude\":\"%s\",\"location\":\"%s\",\"side\":\"%s\"}",
                        id, timestamp, altitude, location, side);

                // Envía el mensaje a Kafka
                Properties kafkaProps = new Properties();
                kafkaProps.put("bootstrap.servers", "localhost:9092");
                kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

                try (KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps)) {
                    // Si Kafka está conectado, se envía
                    ProducerRecord<String, String> rec = new ProducerRecord<>(kafkaTopic, jsonMessage);
                    producer.send(rec).get();
                    System.out.println("[Bridge -> Kafka] " + jsonMessage);
                    this.getChannel().basicAck(envelope.getDeliveryTag(), false);
                } catch (Exception e) {
                    // Si Kafka está conectado, se rechaza y se le comunica al publisher
                    System.out.println("[Bridge -> Publisher] Kafka error: " + e.getMessage());
                    this.getChannel().basicReject(envelope.getDeliveryTag(), false);
                }
            } else {
                // Si el mensaje tiene mal formato, se rechaza y se le comunica al publisher
                System.out.println("[Bridge] Message received has unexpected format: " + message);
                this.getChannel().basicReject(envelope.getDeliveryTag(), false);
            }
        }
    }
}