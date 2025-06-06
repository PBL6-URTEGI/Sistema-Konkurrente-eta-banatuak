package batch.ftp;

import java.util.*;

public class ValueStorer {
    private final Map<String, List<String>> values;
    private final Map<String, String> averages;
    private List<String> headerKeys;
    private String title = "title";

    public ValueStorer(Map<String, List<String>> values) {
        this.values = values;
        this.averages = new HashMap<>();
        this.headerKeys = new ArrayList<>();
    }

    public void parse(String line, String fileName) {
        String[] elements = line.split(",");

        if (elements[0].equals("Fecha")) {
            values.put(title, Collections.synchronizedList(new ArrayList<>()));
            values.get(title).add(fileName);

            headerKeys.clear();
            headerKeys.add(title);

            for (int i = 1; i < elements.length; i++) {
                values.put(elements[i], Collections.synchronizedList(new ArrayList<>()));
                headerKeys.add(elements[i]); // keep keys in order
            }
        } else {
            // Use headerKeys for stable order
            for (int i = 1; i < elements.length; i++) {
                String key = headerKeys.get(i);
                values.get(key).add(elements[i]);
            }
        }
    }

    public void calculateAverage() {
        
        List<String> titles = values.get(title);
        averages.put(title, titles.get(0));

        for (String key : headerKeys) {
            if (key.equals(title))
                continue;

            List<String> strValues = values.get(key);
            double sum = 0.0;
            int count = 0;

            for (String valStr : strValues) {
                try {
                    double val = Double.parseDouble(valStr);
                    sum += val;
                    count++;
                } catch (Exception e) {
                    // Exception
                }
            }

            double average = (count == 0) ? Double.NaN : sum / count;
            averages.put(key, String.valueOf(average));
        }
    }

    public Map<String, String> getMap() {
        return averages;
    }
}
