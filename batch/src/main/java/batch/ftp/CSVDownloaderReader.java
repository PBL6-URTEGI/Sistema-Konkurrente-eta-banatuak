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

public class CSVDownloaderReader implements Callable<List<ConcurrentHashMap<String, String>>> {
    private int inicio, fin;
    private List<String> fileNames;
    private List<ConcurrentHashMap<String, String>> mapList;

    final static String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    final static String DOWNLOAD_APPENDIX = "_SAI-CHC.csv";

    public CSVDownloaderReader(int inicio, int fin, List<String> fileNames) {
        this.inicio = inicio;
        this.fin = fin;
        this.fileNames = fileNames;
        this.mapList = new ArrayList<ConcurrentHashMap<String, String>>();
    }

    @Override
    public List<ConcurrentHashMap<String, String>> call() throws Exception {
        for (int i = inicio; i < fin; i++) {
            ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
            ValueStorer valueStorer = new ValueStorer(map);

            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";
            String remoteFile = prefix + fileNames.get(i) + DOWNLOAD_APPENDIX;
            String localFilePath = DOWNLOAD_PATH + remoteFile;
            File localFile = new File(localFilePath);

            readFile(localFile, valueStorer, fileNames.get(i));
            valueStorer.calculateAverage();

            mapList.add(valueStorer.getMap());
        }
        return mapList;
    }

    public void readFile(File localFile, ValueStorer valueStorer, String fileName) {
        try {
            Scanner scanner = new Scanner(localFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    valueStorer.parse(line, fileName);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
