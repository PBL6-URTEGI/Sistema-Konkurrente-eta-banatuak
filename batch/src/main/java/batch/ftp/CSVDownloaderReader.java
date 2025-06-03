package batch.ftp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.net.ftp.FTPClient;

public class CSVDownloaderReader implements Callable<List<ConcurrentHashMap<String, String>>> {
    private int inicio, fin;
    private List<String> fileNames;
    private FTPClient ftpClient;
    private String prefix;
    private List<ConcurrentHashMap<String, String>> mapList;

    final static String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    final static String DOWNLOAD_APPENDIX = "_SAI-CHC.csv";

    public CSVDownloaderReader(int inicio, int fin, List<String> fileNames, FTPClient ftpClient, String prefix) {
        this.inicio = inicio;
        this.fin = fin;
        this.fileNames = fileNames;
        this.ftpClient = ftpClient;
        this.prefix = prefix;
        this.mapList = new ArrayList<ConcurrentHashMap<String, String>>();
    }

    @Override
    public List<ConcurrentHashMap<String, String>> call() throws Exception {
        for (int i = inicio; i < fin; i++) {
            ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
            ValueStorer valueStorer = new ValueStorer(map);

            String remoteFile = prefix + fileNames.get(i) + DOWNLOAD_APPENDIX;
            String localFilePath = DOWNLOAD_PATH + remoteFile;
            File localFile = new File(localFilePath);

            downloadFile(remoteFile, localFilePath, localFile);
            readFile(localFile, valueStorer, fileNames.get(i));
            valueStorer.calculateAverage();

            mapList.add(valueStorer.getMap());
        }
        return mapList;
    }

    public void downloadFile(String remoteFile, String localFilePath, File localFile) {
        if (localFile.exists()) {
            // System.out.println("File already downloaded: " + remoteFile);
        } else {
            try (OutputStream outputStream = new FileOutputStream(localFilePath)) {
                ftpClient.retrieveFile(remoteFile, outputStream);
                System.out.println("Downloaded: " + remoteFile);
            } catch (Exception e) {
                System.err.println("Error downloading or reading " + remoteFile + ": " + e.getMessage());
            }
        }
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
