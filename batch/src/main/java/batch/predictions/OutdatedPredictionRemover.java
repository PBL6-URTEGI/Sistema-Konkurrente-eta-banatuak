package batch.predictions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import batch.predictions.model.Prediction;

public class OutdatedPredictionRemover {
    List<Prediction> predictions;
    ValuesManager valuesManager;

    public OutdatedPredictionRemover(List<Prediction> predictions, ValuesManager valuesManager) {
        this.predictions = predictions;
        this.valuesManager = valuesManager;
        removeOutdated();
    }

    public void removeOutdated() {
        Iterator<Prediction> iterator = predictions.iterator();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        while (iterator.hasNext()) {
            Prediction prediction = iterator.next();
            String date = prediction.getMs_FECHA_HORA().getDate();

            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);

            if (dateTime.isBefore(now) || dateTime.isEqual(now)) {
                iterator.remove();
            } else {
                valuesManager.addValue(prediction.getMs_TAG(), prediction.getMs_VALOR());
            }
        }
    }
}
