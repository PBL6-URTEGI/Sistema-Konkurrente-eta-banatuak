package batch.predictions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ValuesManager {

    ConcurrentHashMap<String, List<Double>> values;

    public ValuesManager() {
        values = new ConcurrentHashMap<>();
    }

    public void addValue(String ms_TAG, double ms_VALOR) {
        values.computeIfAbsent(ms_TAG, k -> Collections.synchronizedList(new ArrayList<>())).add(ms_VALOR);
    }

    public double getAverage(String ms_VALOR) {
        List<Double> valuesList = values.get(ms_VALOR);
        return valuesList.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
    }

    public List<String> getTags() {
        return new ArrayList<>(values.keySet());
    }
}