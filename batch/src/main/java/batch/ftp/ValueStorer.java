package batch.ftp;

import java.util.*;
import java.util.concurrent.*;

public class ValueStorer {

    private final ConcurrentHashMap<String, List<String>> map;

    public ValueStorer(ConcurrentHashMap<String, List<String>> map) {
        this.map = map;
    }

    public void parse(String line, String fileName) {
        String[] elements = line.split(",");

        if (elements[0].equals("Fecha")) {
            map.put("title", Collections.synchronizedList(new ArrayList<>()));
            map.get("title").add(fileName);  // record source of header
            for (int i = 1; i < elements.length; i++) {
                map.put(elements[i], Collections.synchronizedList(new ArrayList<>()));
            }
        } else {
            List<String> keys = new ArrayList<>(map.keySet());
            for (int i = 1; i < elements.length; i++) {
                String key = keys.get(i); // get key name from title
                map.get(key).add(elements[i]);
            }
        }
    }

    public ConcurrentHashMap<String, List<String>> getMap() {
        return map;
    }
}
