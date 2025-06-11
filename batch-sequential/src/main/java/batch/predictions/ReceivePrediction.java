package batch.predictions;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import batch.predictions.model.DatosWrapper;
import batch.predictions.model.Prediction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class ReceivePrediction {
    static final String API_KEY_DIRECTORY = "./src/main/resources/credentials/apikey.txt";

    public ReceivePrediction() throws IOException {
        List<Prediction> predictions = getPredictions();
        // predictions.addAll(predictions);
        // predictions.addAll(predictions);
        // predictions.addAll(predictions);

        long start = System.currentTimeMillis();

        // Descarta las predicciones antiguas
        new OutdatedPredictionRemover(predictions);

        ObjectMapper mapper = new ObjectMapper();

        try {
            // Parsea las predicciones actuales a JSON
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File("./src/main/resources/predictions.json"), predictions);
            long finish = System.currentTimeMillis();
            System.out.println("Reading total: " + (finish - start) + "ms");
        } catch (IOException e) {
            // Exception
        }
    }

    public static String getApiKey() throws IOException {
        // Recibe contenido de un .txt
        return new String(Files.readAllBytes(Paths.get(API_KEY_DIRECTORY)));
    }

    public List<Prediction> getPredictions() throws IOException {
        String apikey = getApiKey();
        String url = "https://www.saihebro.com/datos/apiopendata?apikey=" + apikey + "&prevision=prevision_completa";

        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        // Devuelve las predicciones de la API
        try (Response response = client.newCall(request).execute()) {
            InputStream is = response.body().byteStream();

            ObjectMapper mapper = new ObjectMapper();
            DatosWrapper wrapper = mapper.readValue(is, DatosWrapper.class);
            return wrapper.getDatos();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
