package pbl6.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import pbl6.stream.model.EmbalseDato;
import pbl6.stream.model.UnsafeOkHttpClient;

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
    
    final static String senales = "E001L65VEMBA,E002C65VEMBA,E003M65VEMBA,E004C65VEMBA,E005L65VEMBA,E006L65VEMBA,E007Z65VEMBA,E008Z65VEMBA,E009L65VEMBA,E011L65VEMBA,E012Z65VEMBA,E013Z65VEMBA,E014Z65VEMBA,E015Z65VEMBA,E017C65VEMBA,E018C65VEMBA,E019C65VEMBA,E020C65VEMBA,E021C65VEMBA,E022C65VEMBA,E023C65VEMBA,E024E65VEMBA,E025T65VEMBA,E027L65VEMBA,E028L65VEMBA,E029Y65VEMBA,E030T65VEMBA,E031Z65VEMBA,E034Z82PORCE,E035H65VEMBA,E036H65VEMBA,E037H65VEMBA,E038H65VEMBA,E039H65VEMBA,E040H65VEMBA,E041H65VEMBA,E042H65VEMBA,E043C65VEMBA,E044H65VEMBA,E046G65VEMBA,E047G65VEMBA,E048B65VEMBA,E050S65VEMBA,E051S65VEMBA,E052S65VEMBA,E058M65VEMBA,E059M65VEMBA,E060M65VEMBA,E061M65VEMBA,E062O65VEMBA,E063S65VEMBA,E064B65VEMBA,E065S65VEMBA,E071T65VEMBA,E074T65VEMBA,E075E65VEMBA,E076O65VEMBA,E077H65VEMBA,E079H65VEMBA,E080O65VEMBA,E081H65VEMBA,E082H65VEMBA,E084E65VEMBA,E085Z65VEMBA,E086C65VEMBA,E087Z65VEMBA,E089T65VEMBA,E091C65VEMBA,E093H65VEMBA,E094F65VEMBA,E095M65VEMBA,E097C65VEMBA,E098C65VEMBA,E104E65VEMBA,E201T65VEMBA,E281L65VEMBA";
    
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
			
			channel.exchangeDeclare(EXCHANGE_DATOS, "fanout", true);

            for (EmbalseDato dato : datos) {
                String message = dato.toString();
				System.out.println("[Publisher] " + dato);
				channel.basicPublish(EXCHANGE_DATOS, "", null, message.getBytes());
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
