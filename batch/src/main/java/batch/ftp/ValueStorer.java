package batch.ftp;

import java.util.*;
import java.util.concurrent.*;

public class ValueStorer {

    private final ConcurrentHashMap<String, List<String>> values;
    private final ConcurrentHashMap<String, String> averages;

    public ValueStorer(ConcurrentHashMap<String, List<String>> values) {
        this.values = values;
        this.averages = new ConcurrentHashMap<>();
    }

    public void parse(String line, String fileName) {
        String[] elements = line.split(",");

        if (elements[0].equals("Fecha")) {
            values.put("title", Collections.synchronizedList(new ArrayList<>()));
            values.get("title").add(fileName);
            for (int i = 1; i < elements.length; i++) {
                values.put(elements[i], Collections.synchronizedList(new ArrayList<>()));
            }
        } else {
            List<String> keys = new ArrayList<>(values.keySet());
            for (int i = 1; i < elements.length; i++) {
                String key = keys.get(i);
                values.get(key).add(elements[i]);
            }
        }
    }

    public void calculateAverage() {
        List<String> titles = values.get("title");
        averages.put("title", titles.get(0));

        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            double sum = 0.0;
            int count = 0;

            String key = entry.getKey();

            if (!key.equals("title")) {
                List<String> strValues = entry.getValue();

                for (String valStr : strValues) {
                    try {
                        double val = Double.parseDouble(valStr);
                        sum += val;
                        count++;
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }

                double average = sum / count;
                averages.put(key, String.valueOf(average));
            }

        }
    }

    public ConcurrentHashMap<String, String> getMap() {
        return averages;
    }
}
