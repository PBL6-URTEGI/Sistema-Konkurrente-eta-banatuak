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
import java.util.Collections;
import java.util.List;

public class ReceivePrediction {
    static final String API_URL = "https://www.saihebro.com/datos/apiopendata?apikey=b3c5a6ce7d856ba4d2fa3ab1d238ab1c&prevision=prevision_completa";

    public ReceivePrediction() {
        List<Prediction> predictions = getPredictions();
        ValuesManager valuesManager = new ValuesManager();

        long start = System.currentTimeMillis();
        new OutdatedPredictionRemover(predictions, valuesManager);
        long finish = System.currentTimeMillis();
        System.out.println("Tiempo empleado: " + (finish - start) + "ms");

        ObjectMapper mapper = new ObjectMapper();

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File("./src/main/resources/predictions.json"), predictions);
        } catch (IOException e) {
            // Exception
        }
    }

    public List<Prediction> getPredictions() {
        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

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
