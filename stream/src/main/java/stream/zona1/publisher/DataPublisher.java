package stream.zona1.publisher;

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
import stream.zona1.publisher.model.EmbalseDato;
import stream.zona1.publisher.model.UnsafeOkHttpClient;

public class DataPublisher {
    ConnectionFactory factory;
    final static String EXCHANGE_ZONA1 = "stream_zona1";
    
    final static String senales = "E030T17NEMBA,E030T65VEMBA,E030T82PORCE,EM30T83PA24H,EM30T84PACUM,E025T17NEMBA,E025T65VEMBA,E025T82PORCE,EM25T83PA24H,EM25T84PACUM,E031Z17NEMBA,E031Z65VEMBA,E031Z82PORCE,E104E01NEMBA,E104E65VEMBA,E104E82PORCE,E201T02NEMBA,E201T65VEMBA,E201T82PORCE,E035H17NEMBA,E035H65VEMBA,E035H82PORCE,EM35H83PA24H,EM35H84PACUM,E021C17NEMBA,E021C65VEMBA,E021C82PORCE,EM21C83PA24H,EM21C84PACUM,E037H06NEMBA,E037H65VEMBA,E037H82PORCE,EM37H83PA24H,EM37H84PACUM,E038H17NEMBA,E038H65VEMBA,E038H82PORCE,EM38H83PA24H,EM38H84PACUM,E039H01NEMBA,E039H65VEMBA,E039H82PORCE,E039H83PA24H,E039H84PACUM,E040H17NEMBA,E040H65VEMBA,E040H82PORCE,EM40H83PA24H,EM40H84PACUME041H17NEMBA,E041H65VEMBA,E041H82PORCE,EM41H83PA24H,EM41H84PACUM,E042H02NEMBA,E042H65VEMBA,E042H82PORCE,E044H17NEMBA,E044H65VEMBA,E044H82PORCE,E044H83PA24H,E044H84PACUM,E077H17NEMBA,E077H65VEMBA,E077H82PORCE,EM77H83PA24H,EM77H84PACUM,E079H17NEMBA,E079H65VEMBA,E079H82PORCE,EM79H83PA24H,EM79H84PACUM,E080O17NEMBA,E080O65VEMBA,E080O82PORCE,E080O83PA24H,E080O84PACUM,E235H02NEMBA,E235H65VEMBA,P092H83PA24H,P092H84PACUM,E071T17NEMBA,E071T65VEMBA,E071T82PORCE,EM71T83PA24H,EM71T84PACUM,E074T17NEMBA,E074T65VEMBA,E074T82PORCE,E074T83PA24H,E074T84PACUM,E281L17NEMBA,E281L65VEMBA,E281L82PORCE,E281L83PA24H,E281L84PACUM";
    
    final static String apiKey = "b3c5a6ce7d856ba4d2fa3ab1d238ab1c";
    final static String apiUrl =
        "https://www.saihebro.com/datos/apiopendata?senal=" + senales + "&inicio=&apikey=" + apiKey;


    public DataPublisher() {
		factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");
	}

    public void suscribe() {
        List<EmbalseDato> datos = recibirDatosAPI();

        Channel channel = null;

		try(Connection connection = factory.newConnection()){
			channel = connection.createChannel();
			
			channel.exchangeDeclare(EXCHANGE_ZONA1, "fanout", true);

            for (EmbalseDato dato : datos) {
                String message = dato.toString();
				System.out.println("[Publisher -> Bridge] " + dato);
				channel.basicPublish(EXCHANGE_ZONA1, "", null, message.getBytes());
			}
			
		} catch (IOException | TimeoutException e) {
			
			e.printStackTrace();
		}
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
            List<EmbalseDato> datos = mapper.readValue(body, new TypeReference<List<EmbalseDato>>() {});

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
		Thread waitingThread = new Thread(()-> {
			scanner.nextLine();
			dp.stop();
		});

		waitingThread.start();
		dp.suscribe();
		scanner.close();
	}
}
