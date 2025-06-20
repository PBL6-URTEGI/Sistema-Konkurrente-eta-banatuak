package batch.predictions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import batch.predictions.model.Prediction;

public class OutdatedPredictionRemover {
    List<Prediction> predictions;

    public OutdatedPredictionRemover(List<Prediction> predictions) {
        this.predictions = predictions;
        removeOutdated();
    }

    public void removeOutdated() {
        Iterator<Prediction> iterator = predictions.iterator();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        while (iterator.hasNext()) {
            Prediction prediction = iterator.next();
            // Coge la fecha de la predicción
            String date = prediction.getMs_FECHA_HORA().getDate();
            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);

            // Si es anterior a la de hoy la descarta
            if (dateTime.isBefore(now) || dateTime.isEqual(now)) {
                iterator.remove();
            }
        }
    }
}
