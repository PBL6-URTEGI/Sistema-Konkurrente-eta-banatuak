package batch.ftp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;

public class CSVDownloaderReader {
    private List<String> fileNames;
    private List<Map<String, String>> mapList;

    static final String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    static final String DOWNLOAD_APPENDIX = "_SAI-CHC.csv";

    public CSVDownloaderReader(List<String> fileNames) {
        this.fileNames = fileNames;
        this.mapList = new ArrayList<>();
    }

    public List<Map<String, String>> readFiles() throws Exception {
        for (String fileName : fileNames) {
            Map<String, List<String>> map = new HashMap<>();
            ValueStorer valueStorer = new ValueStorer(map);

            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";
            String remoteFile = prefix + fileName + DOWNLOAD_APPENDIX;
            String localFilePath = DOWNLOAD_PATH + remoteFile;
            File localFile = new File(localFilePath);

            readFile(localFile, valueStorer, fileName);
            valueStorer.calculateAverage();

            mapList.add(valueStorer.getMap());
        }
        return mapList;
    }

    public void downloadFile(String remoteFile, String localFilePath, File localFile, FTPClient ftpClient) {
        if (localFile.exists()) {
            System.out.println("File already downloaded: " + remoteFile);
        } else {
            try (OutputStream outputStream = new FileOutputStream(localFilePath)) {
                ftpClient.retrieveFile(remoteFile, outputStream);
                System.out.println("Downloaded: " + remoteFile);
            } catch (Exception e) {
                System.err.println("Error downloading or reading " + remoteFile + ": " + e.getMessage());
            }
        }
    }

    public void deleteOldFiles(String prefix) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(DOWNLOAD_PATH))) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (!fileName.startsWith(prefix)) {
                    Files.delete(file);
                    System.out.println("Deleted old file: " + fileName);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cleaning up old files: " + e.getMessage());
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
            // Exception
        }

    }

}
