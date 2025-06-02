package batch.ftp;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    private static List<Future<List<ConcurrentHashMap<String, String>>>> futures;
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

        Map<String, Map<String, String>> tagToFileValues = new LinkedHashMap<>();

        for (Future<List<ConcurrentHashMap<String, String>>> future : futures) {
            try {
                List<ConcurrentHashMap<String, String>> resultList = future.get();
                for (ConcurrentHashMap<String, String> resultMap : resultList) {
                    String fileName = resultMap.get("title");
                    for (Map.Entry<String, String> entry : resultMap.entrySet()) {
                        String key = entry.getKey();
                        if ("title".equals(key))
                            continue;

                        tagToFileValues
                                .computeIfAbsent(key, k -> new LinkedHashMap<>())
                                .put(fileName, entry.getValue());
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.SECONDS);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode arrayNode = mapper.createArrayNode();

            for (Map.Entry<String, Map<String, String>> tagEntry : tagToFileValues.entrySet()) {
                ObjectNode objNode = mapper.createObjectNode();
                objNode.put("id_estacion", tagEntry.getKey());
                for (Map.Entry<String, String> fileEntry : tagEntry.getValue().entrySet()) {
                    objNode.put(fileEntry.getKey(), fileEntry.getValue());
                }
                arrayNode.add(objNode);
            }

            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);
            Files.write(Paths.get("./src/main/resources/averages.json"), jsonOutput.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        new ConnectToServer();
        long finish = System.currentTimeMillis();
        System.out.println("Tiempo empleado: " + (finish - start) + "ms");
    }
}
