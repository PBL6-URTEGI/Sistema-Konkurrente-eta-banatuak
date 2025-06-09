package stream.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import stream.common.UnsafeOkHttpClient.UnsafeClientInitializationException;
import stream.common.model.EmbalseDato;

public class Publisher {
    ConnectionFactory factory;

    private String topic;
    private String senales;

    static final String EXCHANGE_STREAM = "stream";
    final static String EXCHANGE_DL = "dle";
    final static String QUEUE_DL = "pub_dlq";
    static final String API_KEY_PATH = "./src/main/resources/credentials/apikey.txt";
    static final String RABBITMQ_PATH = "./src/main/resources/credentials/guest.txt";

    public Publisher(String topic, String senales) throws IOException {
        String key = getKey(RABBITMQ_PATH);

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword(key);
        this.topic = topic;
        this.senales = senales;
    }

    public void suscribe() throws Exception {
        List<EmbalseDato> datos = recibirDatosAPI();
        String fullTopic = "stream." + topic;

        try (Connection connection = factory.newConnection()) {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_STREAM, "topic", true);
            channel.exchangeDeclare(EXCHANGE_DL, "direct", true);

            String deadLetterQueue = QUEUE_DL + "_" + topic;

            channel.queueDeclare(deadLetterQueue, false, false, false, null);
            channel.queueBind(deadLetterQueue, EXCHANGE_DL, topic);
            Consumer consumerDeadLetter = new ConsumerDeadLetter(channel);
            channel.basicConsume(deadLetterQueue, true, consumerDeadLetter);

            for (EmbalseDato dato : datos) {
                String message = dato.toString();
                System.out.println("[Publisher -> Bridge] " + dato);
                channel.basicPublish(EXCHANGE_STREAM, fullTopic, null, message.getBytes("UTF-8"));
            }

            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            channel.close();

        } catch (IOException | TimeoutException e) {
            // Exception
        }
    }

    public static String getKey(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public List<EmbalseDato> recibirDatosAPI() throws UnsafeClientInitializationException, IOException {
        String key = getKey(API_KEY_PATH);
        String url = "https://www.saihebro.com/datos/apiopendata?senal=" + senales + "&inicio=&apikey="
                + key;
        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(body, new TypeReference<List<EmbalseDato>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public class ConsumerDeadLetter extends DefaultConsumer {
        public ConsumerDeadLetter(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            String message = new String(body, "UTF-8");
            System.out.println("[Bridge -> Publisher] Rejected: " + message);
        }

    }
}
