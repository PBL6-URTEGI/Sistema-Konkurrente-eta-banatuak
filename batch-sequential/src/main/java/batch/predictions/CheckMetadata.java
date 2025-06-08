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

    static final String FILE = "./src/main/resources/metadata.txt";
    static final String API_KEY_DIRECTORY = "./src/main/resources/credentials/apikey.txt";

    public void suscribe() throws IOException {
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
            // Exception
        }
    }

    public static String getApiKey() throws IOException {
        return new String(Files.readAllBytes(Paths.get(API_KEY_DIRECTORY)));
    }

    public Metadata getMetadata() throws IOException {
        String apikey = getApiKey();
        String url = "https://www.saihebro.com/datos/apiopendata?apikey=" + apikey + "&prevision=metadata";

        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(body).get(0).traverse(mapper).readValueAs(Metadata.class);

        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CheckMetadata cm = new CheckMetadata();
        cm.suscribe();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                UnsafeOkHttpClient.shutdown();
            } catch (InterruptedException e) {
                // Exception
            }
        }));
        System.exit(0);
    }
}