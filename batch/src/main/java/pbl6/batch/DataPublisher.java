package pbl6.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import pbl6.batch.model.EmbalseDato;
import pbl6.batch.model.UnsafeOkHttpClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.Scanner;

public class DataPublisher {
    ConnectionFactory factory;
    final static String EXCHANGE_DATOS = "datos";

    final static String apiKey = "b3c5a6ce7d856ba4d2fa3ab1d238ab1c";
    final static String apiUrl = "https://www.saihebro.com/datos/apiopendata?&apikey=" + apiKey
            + "&prevision=prevision_completa";

    public DataPublisher() {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");
    }

    public void suscribe() {
        List<EmbalseDato> datos = recibirDatosAPI();

        for (EmbalseDato dato : datos) {
            System.out.println(dato);
        }

        // Channel channel = null;

        // try(Connection connection = factory.newConnection()){
        // channel = connection.createChannel();

        // channel.exchangeDeclare(EXCHANGE_DATOS, "fanout", true);

        // for (EmbalseDato dato : datos) {
        // String message = dato.toString();
        // System.out.println("[Publisher ➡️ Bridge] " + dato);
        // channel.basicPublish(EXCHANGE_DATOS, "", null, message.getBytes());
        // }

        // } catch (IOException | TimeoutException e) {

        // e.printStackTrace();
        // }
    }

    public List<EmbalseDato> recibirDatosAPI() {
        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            List<EmbalseDato> datos = mapper.readValue(body, new TypeReference<List<EmbalseDato>>() {
            });

            return datos;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void stop() {
        this.notify();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        DataPublisher dp = new DataPublisher();
        Thread waitingThread = new Thread(() -> {
            scanner.nextLine();
            dp.stop();
        });

        waitingThread.start();
        dp.suscribe();
        scanner.close();
    }
}
