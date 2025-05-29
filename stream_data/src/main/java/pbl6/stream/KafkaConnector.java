package pbl6.stream;

import com.rabbitmq.client.*;

import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class KafkaConnector {

    private final static String RABBITMQ_EXCHANGE = "datos";
    private final static String KAFKA_TOPIC = "datos";
    ConnectionFactory factory;

    public KafkaConnector() {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");
    }

    public void suscribe() {

        Channel channel = null;
        try (Connection connection = factory.newConnection()) {

            channel = connection.createChannel();
            channel.exchangeDeclare(RABBITMQ_EXCHANGE, "fanout", true);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, RABBITMQ_EXCHANGE, "");

            MyConsumer consumer = new MyConsumer(channel);
            boolean autoack = true;
            String tag = channel.basicConsume(queueName, autoack, consumer);

            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            channel.basicCancel(tag);
            channel.close();

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
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
            String message = new String(body, "UTF-8");
            System.out.println("[Bridge] " + message);

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

    public static void main(String[] args) throws IOException, TimeoutException {
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