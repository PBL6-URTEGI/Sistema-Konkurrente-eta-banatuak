package stream.zona1.subscriber;

import java.io.IOException;
import java.util.Scanner;
import stream.common.Subscriber;

public class KafkaConnector {

    private static final String RABBITMQ_EXCHANGE = "stream_zona1";
    private static final String KAFKA_TOPIC = "stream_zona1";
    private static final String IP = "./src/main/resources/ip1.txt";

    public static void main(String[] args) throws InterruptedException, IOException {
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