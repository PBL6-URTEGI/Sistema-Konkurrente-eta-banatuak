package batch.ftp;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class ConnectToServer {
    final static String SERVER = "168.63.45.161";
    final static int PORT = 21;
    final static String USERNAME = "mondragon_edu";
    final static String PASSWORD = "mondragon@1975";
    final static String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    final static List<String> DOWNLOAD_FILENAMES = Arrays.asList(
            "caudal_rio", "nivel_embalse", "nivel_embalse_visor",
            "nivel_rio", "porcentaje_llenado_embalse", "porcentaje_llenado_embalse_visor", "precipitacion",
            "temperatura_agua", "temperatura", "volumen_embalse", "volumen_embalse_visor");

    private static List<Future<List<ConcurrentHashMap<String, List<String>>>>> futures;
    private static List<CSVDownloaderReader> tareas;
    private static ExecutorService executorService;

    public ConnectToServer() {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(SERVER, PORT);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";

            if (ftpClient.login(USERNAME, PASSWORD)) {
                deleteOldFiles(prefix);
                createThreads(prefix, ftpClient);
                ftpClient.logout();
            } else {
                System.out.println("Login failed.");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void deleteOldFiles(String prefix) {
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

    private static void createThreads(String prefix, FTPClient ftpClient) throws InterruptedException {
        tareas = new ArrayList<>();
        futures = new ArrayList<>();
        int cpu = Runtime.getRuntime().availableProcessors();
        int start, finish = 0;
        int step = DOWNLOAD_FILENAMES.size() / cpu;

        executorService = Executors.newFixedThreadPool(cpu);

        for (int i = 0; i < step - 1; i++) {
            start = 2 + i * cpu;
            finish = 2 + (i + 1) * cpu;
            tareas.add(new CSVDownloaderReader(start, finish, DOWNLOAD_FILENAMES, ftpClient, prefix));
        }
        tareas.add(new CSVDownloaderReader(finish, DOWNLOAD_FILENAMES.size(), DOWNLOAD_FILENAMES, ftpClient, prefix));
        futures = executorService.invokeAll(tareas);

        for (Future<List<ConcurrentHashMap<String, List<String>>>> future : futures) {
            try {
                List<ConcurrentHashMap<String, List<String>>> resultList = future.get();
                for (ConcurrentHashMap<String, List<String>> resultMap : resultList) {
                    for (Map.Entry<String, List<String>> entry : resultMap.entrySet()) {
                        String key = entry.getKey();
                        List<String> values = entry.getValue();
                        if (key.equals("title")) {
                            System.out.println(key + ": " + values.get(0));
                        }
                    }
                }

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        new ConnectToServer();
        long finish = System.currentTimeMillis();
        System.out.println("Tiempo empleado: " + (finish - start) + "ms");
    }
}
