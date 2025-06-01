package batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import batch.model.Prediction;

import java.util.List;

public class ReceivePrediction {
    final static String EXCHANGE_DATOS = "datos";

    final static String API_KEY = "b3c5a6ce7d856ba4d2fa3ab1d238ab1c";
    final static String API_URL = "https://www.saihebro.com/datos/apiopendata?apikey=" + API_KEY
            + "&prevision=prevision_completa";

    public ReceivePrediction() {
        long inicio = System.currentTimeMillis();
        List<Prediction> predictions = getPredictions();
        long fin = System.currentTimeMillis();

        System.out.println(predictions.get(0));

        System.out.println("Tiempo empleado: " + (fin - inicio) + " ms");
    }

    public List<Prediction> getPredictions() {
        OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();

            ObjectMapper mapper = new ObjectMapper();
            var rootNode = mapper.readTree(body);
            var datosNode = rootNode.get("datos");

            List<Prediction> predictions = mapper.readValue(
                    datosNode.toString(),
                    new TypeReference<List<Prediction>>() {
                    });

            return predictions;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
