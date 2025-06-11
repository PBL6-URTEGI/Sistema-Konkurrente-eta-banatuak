package batch.ftp;

import java.util.*;
import java.util.concurrent.*;

public class ValueStorer {
    private final ConcurrentMap<String, List<String>> values;
    private final ConcurrentHashMap<String, String> averages;
    private List<String> headerKeys;
    private String title = "title";

    public ValueStorer(ConcurrentMap<String, List<String>> values) {
        this.values = values;
        this.averages = new ConcurrentHashMap<>();
        this.headerKeys = new ArrayList<>();
    }

    public void parse(String line, String fileName) {
        String[] elements = line.split(",");

        // Identifica si es la primera línea (header incluye "Fecha")
        if (elements[0].equals("Fecha")) {
            // Primer key-value es el nombre del fichero
            values.put(title, Collections.synchronizedList(new ArrayList<>()));
            values.get(title).add(fileName);

            headerKeys.clear();
            headerKeys.add(title);

            // Mete los demás IDs como keys
            for (int i = 1; i < elements.length; i++) {
                values.put(elements[i], Collections.synchronizedList(new ArrayList<>()));
                headerKeys.add(elements[i]);
            }
        } else {
            // Si no es primera línea del CSV, los mete como values
            for (int i = 1; i < elements.length; i++) {
                String key = headerKeys.get(i);
                values.get(key).add(elements[i]);
            }
        }
    }

    public void calculateAverage() {
        // Vuelve a guardar el nombre del fichero para identificarlo
        List<String> titles = values.get(title);
        averages.put(title, titles.get(0));

        for (String key : headerKeys) {
            if (key.equals(title))
                continue;

            List<String> strValues = values.get(key);
            double sum = 0.0;
            int count = 0;

            // Calcula la media de cada estación
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

    public ConcurrentMap<String, String> getMap() {
        return averages;
    }
}
