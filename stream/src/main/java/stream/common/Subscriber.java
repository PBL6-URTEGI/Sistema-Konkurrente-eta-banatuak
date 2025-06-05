package stream.common;

import com.rabbitmq.client.*;

import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class Subscriber {

    private String rabbitmqExchange;
    private String kafkaTopic;
    ConnectionFactory factory;

    public Subscriber(String rabbitmqExchange, String kafkaTopic, String ip) {
        factory = new ConnectionFactory();
        factory.setHost(ip);
        factory.setUsername("rabbit");
        factory.setPassword("rabbit");

        this.rabbitmqExchange = rabbitmqExchange;
        this.kafkaTopic = kafkaTopic;
    }

    public void suscribe() throws InterruptedException {
        try (Connection connection = factory.newConnection()) {
            try (Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(rabbitmqExchange, "fanout", true);

                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, rabbitmqExchange, "");

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

            ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTopic, message);
            producer.send(record);
            producer.flush();
            producer.close();
        }

    }
}