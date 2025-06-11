package batch.ftp;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CSVDownloaderReader implements Callable<List<ConcurrentMap<String, String>>> {
    private int inicio;
    private int fin;
    private List<String> fileNames;
    private List<ConcurrentMap<String, String>> mapList;

    static final String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    static final String DOWNLOAD_APPENDIX = "_SAI-CHC.csv";

    public CSVDownloaderReader(int inicio, int fin, List<String> fileNames) {
        this.inicio = inicio;
        this.fin = fin;
        this.fileNames = fileNames;
        this.mapList = new ArrayList<>();
    }

    @Override
    public List<ConcurrentMap<String, String>> call() throws Exception {
        for (int i = inicio; i < fin; i++) {
            ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
            ValueStorer valueStorer = new ValueStorer(map);

            // Coge la ruta del fichero
            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";
            String remoteFile = prefix + fileNames.get(i) + DOWNLOAD_APPENDIX;
            String localFilePath = DOWNLOAD_PATH + remoteFile;
            File localFile = new File(localFilePath);

            // Lee el fichero
            readFile(localFile, valueStorer, fileNames.get(i));
            // Calcula la media de cada estación
            valueStorer.calculateAverage();

            mapList.add(valueStorer.getMap());
        }

        // Devuelve los mapas con las mediaa
        return mapList;
    }

    public void readFile(File localFile, ValueStorer valueStorer, String fileName) {
        try {
            Scanner scanner = new Scanner(localFile);
            // Lee cada línea del fichero
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    // Guarda los valores de la línea
                    valueStorer.parse(line, fileName);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            // Exception
        }

    }

}
