package stream.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import stream.common.model.EmbalseDato;

public class Publisher {
    ConnectionFactory factory;

    private String exchange;
    private String senales;

    static final String API_KEY_PATH = "./src/main/resources/credentials/apikey.txt";
    static final String RABBITMQ_PASSWORD_PATH = "./src/main/resources/credentials/guest.txt";

    public Publisher(String exchange, String senales) throws IOException {
        String key = getKey(RABBITMQ_PASSWORD_PATH);

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword(key);
        this.exchange = exchange;
        this.senales = senales;
    }

    public void suscribe() throws IOException {
        List<EmbalseDato> datos = recibirDatosAPI();

        try (Connection connection = factory.newConnection()) {
            try (Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(exchange, "fanout", true);

                for (EmbalseDato dato : datos) {
                    String message = dato.toString();
                    System.out.println("[Publisher -> Bridge] " + dato);
                    channel.basicPublish(exchange, "", null, message.getBytes());
                }
            }
        } catch (IOException | TimeoutException e) {
            // Exception
        }
    }

    public static String getKey(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public List<EmbalseDato> recibirDatosAPI() throws IOException {
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
}
