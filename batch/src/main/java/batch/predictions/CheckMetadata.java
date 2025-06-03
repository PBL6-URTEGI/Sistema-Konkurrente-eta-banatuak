package batch.predictions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

import batch.predictions.model.Metadata;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CheckMetadata {

    final static String FILE = "./src/main/resources/metadata.txt";
    final static String API_KEY = "b3c5a6ce7d856ba4d2fa3ab1d238ab1c";
    final static String API_URL = "https://www.saihebro.com/datos/apiopendata?apikey=" + API_KEY
            + "&prevision=metadata";

    public void suscribe() {
        Metadata metadata = getMetadata();
        File file = new File(FILE);

        System.out.println(metadata);

        try {
            if (!file.exists()) {
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(metadata.toString());
                }
                System.out.println("File created and data written. Initiating prediction API call.");
                new ReceivePrediction();
            } else {
                String existingContent = new String(Files.readAllBytes(Paths.get(FILE)));

                if (!existingContent.equals(metadata.toString())) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(metadata.toString());
                    }
                    System.out.println("File updated with new metadata. Initiating prediction API call.");
                    new ReceivePrediction();
                } else {
                    System.out.println("No changes in metadata. File not updated.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Metadata getMetadata() {
        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            Metadata metadata = mapper.readTree(body).get(0).traverse(mapper).readValueAs(Metadata.class);

            return metadata;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        CheckMetadata cm = new CheckMetadata();
        cm.suscribe();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            UnsafeOkHttpClient.shutdown();
        }));
        System.exit(0);
    }
}