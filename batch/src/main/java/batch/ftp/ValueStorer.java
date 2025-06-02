package batch.ftp;

import java.util.*;
import java.util.concurrent.*;

public class ValueStorer {

    static ConcurrentHashMap<String, List<String>> map;

    public ValueStorer(ConcurrentHashMap<String, List<String>> map) {
        ValueStorer.map = map;
    }

    public static void parse(String line, String fileName) {
        String[] elements = line.split(",");

        if (elements[0].equals("Fecha")) {
            map.put("title", Collections.synchronizedList(new ArrayList<>()));
            map.get("title").add(fileName);
            for (int i = 1; i < elements.length; i++) {
                map.put(elements[i], Collections.synchronizedList(new ArrayList<>()));
            }
        } else {
            List<String> keys = new ArrayList<>(map.keySet());
            for (int i = 1; i < elements.length; i++) {
                String key = keys.get(i);
                map.get(key).add(elements[i]);
            }
        }
    }

    public ConcurrentHashMap<String, List<String>> getMap() {
        return map;
    }
}
