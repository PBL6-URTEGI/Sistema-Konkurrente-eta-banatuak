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
import java.util.List;

public class ReceivePrediction {
    final static String API_KEY = "b3c5a6ce7d856ba4d2fa3ab1d238ab1c";
    final static String API_URL = "https://www.saihebro.com/datos/apiopendata?apikey=" + API_KEY
            + "&prevision=prevision_completa";

    public ReceivePrediction() {
        List<Prediction> predictions = getPredictions();
        ValuesManager valuesManager = new ValuesManager();

        long inicio = System.currentTimeMillis();
        new OutdatedPredictionRemover(predictions, valuesManager);
        List<String> tags = valuesManager.getTags();
        for (String tag : tags) {
            System.out.println(tag + ": " + valuesManager.getAverage(tag));;
        }
        long fin = System.currentTimeMillis();
        System.out.println("Total: " + (fin - inicio) + " ms");
        ObjectMapper mapper = new ObjectMapper();

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File("./src/main/resources/predictions.json"), predictions);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
                List<Prediction> predictions = wrapper.getDatos();

                return predictions;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
}
