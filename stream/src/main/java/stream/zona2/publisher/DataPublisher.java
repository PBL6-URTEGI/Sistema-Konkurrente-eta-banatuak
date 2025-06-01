package stream.zona2.publisher;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
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
import stream.common.UnsafeOkHttpClient;

public class DataPublisher {
    ConnectionFactory factory;
    final static String EXCHANGE_DATOS = "stream_zona2";

    final static String SENALES = "E005L01NEMBA,E005L65VEMBA,E005L82PORCE,E005L83PA24H,E005L84PACUM,E006L17NEMBA,E006L65VEMBA,E006L82PORCE,EM06L83PA24H,EM06L84PACUM,E009L17NEMBA,E009L65VEMBA,E009L82PORCE,EM09L83PA24H,EM09L84PACUM,E089T01NEMBA,E089T65VEMBA,E089T82PORCE,E089T83PA24H,E089T84PACUM,E011L17NEMBA,E011L65VEMBA,E011L82PORCE,EM11L83PA24H,EM11L84PACUM,E008Z04NEMBA,E008Z65VEMBA,E008Z82PORCE,EM08Z83PA24H,EM08Z84PACUM,E087Z02NEMBA,E087Z65VEMBA,E087Z82PORCE,E087Z83PA24H,E087Z84PACUM,E013Z17NEMBA,E013Z65VEMBA,E013Z82PORCE,E013Z83PA24H,E013Z84PACUM,E015Z17NEMBA,E015Z65VEMBA,E015Z82PORCE,E284Z83PA24H,E284Z84PACUM,E085Z02NEMBA,E085Z65VEMBA,E085Z82PORCE,E085Z83PA24H,E085Z84PACUM,E014Z17NEMBA,E014Z65VEMBA,E014Z82PORCE,EM14Z83PA24H,EM14Z84PACUM,E012Z17NEMBA,E012Z65VEMBA,E012Z82PORCE,EM12Z83PA24H,EM12Z84PACUM,E094F01NEMBA,E094F65VEMBA,E094F82PORCE,E094F83PA24H,E094F84PACUM,E093H01NEMBA,E093H65VEMBA,E093H82PORCE,E093H83PA24H,E093H84PACUM,E024E01NEMBA,E024E65VEMBA,E024E82PORCE,E024E83PA24H,E024E84PACUM,E022C17NEMBA,E022C65VEMBA,E022C82PORCE,EM22C83PA24H,EM22C84PACUM,E020C17NEMBA,E020C65VEMBA,E020C82PORCE,EM20C83PA24H,EM20C84PACUM,E018C17NEMBA,E018C65VEMBA,E018C82PORCE,EM18C83PA24H,EM18C84PACUM,E098C02NEMBA,E098C65VEMBA,E098C82PORCE,E098C83PA24H,E098C84PACUM,E019C01NEMBA,E019C65VEMBA,E019C82PORCE,E288C83PA24H,E288C84PACUM";

    final static String API_KEY = "b3c5a6ce7d856ba4d2fa3ab1d238ab1c";
    final static String API_URL = "https://www.saihebro.com/datos/apiopendata?senal=" + SENALES + "&inicio=&apikey="
            + API_KEY;

    public DataPublisher() {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");
    }

    public void suscribe() {
        List<EmbalseDato> data = getData();

        Channel channel = null;

        try (Connection connection = factory.newConnection()) {
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_DATOS, "fanout", true);

            for (EmbalseDato element : data) {
                String message = element.toString();
                System.out.println("[Publisher -> Bridge] " + element);
                channel.basicPublish(EXCHANGE_DATOS, "", null, message.getBytes());
            }

        } catch (IOException | TimeoutException e) {

            e.printStackTrace();
        }
    }

    public List<EmbalseDato> getData() {
        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            List<EmbalseDato> data = mapper.readValue(body, new TypeReference<List<EmbalseDato>>() {
            });

            return data;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        DataPublisher dp = new DataPublisher();
        dp.suscribe();
    }
}
