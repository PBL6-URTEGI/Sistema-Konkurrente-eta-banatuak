package batch.predictions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import batch.predictions.model.Prediction;

public class OutdatedPredictionRemover implements Callable<List<Prediction>> {
    int start;
    int end;
    List<Prediction> predictions;

    public OutdatedPredictionRemover(int start, int end, List<Prediction> predictions) {
        this.start = start;
        this.end = end;
        this.predictions = predictions;
    }

    @Override
    public List<Prediction> call() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        List<Prediction> predictionList = new ArrayList<>();

        for (int i = start; i < end; i++) {
            // Coge la fecha de la predicción
            String date = predictions.get(i).getMs_FECHA_HORA().getDate();
            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
            
            // Si es posterior a la de hoy la guarda
            if (!dateTime.isBefore(now) && !dateTime.isEqual(now)) {
                predictionList.add(predictions.get(i));
            }
        }

        // Devuelve las predicciones con fecha posterior
        return predictionList;
    }
}
