package stream.common;

import com.rabbitmq.client.*;

import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class Subscriber {

    private String rabbitmqTopic;
    private String kafkaTopic;
    ConnectionFactory factory;

    static final String EXCHANGE_STREAM = "stream";
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
                channel.exchangeDeclare("stream", "topic", true);

                String topic = "stream." + rabbitmqTopic;
                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, EXCHANGE_STREAM, topic);

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
            System.out.println("[Bridge -> Kafka] " + message);

            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", "localhost:9092");
            kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);

            ProducerRecord<String, String> rec = new ProducerRecord<>(kafkaTopic, message);
            producer.send(rec);
            producer.flush();
            producer.close();
        }

    }
}