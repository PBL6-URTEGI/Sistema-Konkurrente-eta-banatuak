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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ReceivePrediction {

    static final String API_KEY_PATH = "./src/main/resources/credentials/apikey.txt";

    public ReceivePrediction() throws InterruptedException, IOException {
        List<Prediction> predictions = getPredictions();
        // predictions.addAll(predictions);
        // predictions.addAll(predictions);
        // predictions.addAll(predictions);

        List<Prediction> toJSON = new ArrayList<>();

        List<OutdatedPredictionRemover> tasks = new ArrayList<>();
        List<Future<List<Prediction>>> futures;

        int cpu = Runtime.getRuntime().availableProcessors();
        int start = 0;
        int end;
        int total = predictions.size();
        int step = total / cpu;

        ExecutorService executorService = Executors.newFixedThreadPool(cpu);

        long timeStart = System.currentTimeMillis();

        for (int i = 0; i <= cpu - 1; i++) {
            start = i * step;
            if (i == cpu - 1) {
                end = total;
            } else {
                end = (i + 1) * step;
            }
            // Descarta las predicciones antiguas
            tasks.add(new OutdatedPredictionRemover(start, end, predictions));
        }

        futures = executorService.invokeAll(tasks);

        // Añade todas las predicciones a una lista
        for (Future<List<Prediction>> future : futures) {
            try {
                List<Prediction> resultList = future.get();
                toJSON.addAll(resultList);
            } catch (InterruptedException | ExecutionException e) {
                throw new InterruptedException();
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.SECONDS);

        ObjectMapper mapper = new ObjectMapper();

        try {
            // Parsea la lista a JSON
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File("./src/main/resources/predictions.json"), toJSON);
            long timeFinish = System.currentTimeMillis();
            System.out.println("Reading total: " + (timeFinish - timeStart) + "ms");
        } catch (IOException e) {
            // Exception
        }
    }

    public static String getKey(String path) throws IOException {
        // Recibe contenido de un .txt
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public List<Prediction> getPredictions() throws IOException {
        String apikey = getKey(API_KEY_PATH);
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
