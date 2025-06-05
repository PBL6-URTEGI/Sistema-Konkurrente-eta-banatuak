package stream.zona2.subscriber;

import stream.common.Subscriber;
import java.util.Scanner;

public class KafkaConnector {

    private static final String RABBITMQ_EXCHANGE = "stream_zona2";
    private static final String KAFKA_TOPIC = "stream_zona2";
    private static final String IP = "10.0.40.16";

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        Subscriber subscriber = new Subscriber(RABBITMQ_EXCHANGE, KAFKA_TOPIC, IP);
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