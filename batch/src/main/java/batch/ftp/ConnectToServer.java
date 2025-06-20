package batch.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.concurrent.ConcurrentMap;
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
    static final String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    static final String FTP_PATH = "./src/main/resources/credentials/ftp.txt";
    static final String FTP_SERVER_PATH = "./src/main/resources/credentials/server.txt";
    static final String DOWNLOAD_APPENDIX = "_SAI-CHC.csv";
    static final List<String> DOWNLOAD_FILENAMES = Arrays.asList(
            "caudal_rio", "nivel_embalse", "nivel_embalse_visor",
            "nivel_rio", "porcentaje_llenado_embalse", "porcentaje_llenado_embalse_visor", "precipitacion",
            "temperatura_agua", "temperatura", "volumen_embalse", "volumen_embalse_visor");

    private FTPClient ftpClient;

    public ConnectToServer() throws InterruptedException {
        ftpClient = new FTPClient();

        try {
            String key = getKey(FTP_PATH);
            String ip = getKey(FTP_SERVER_PATH);

            // Se conecta al servidor FTP
            ftpClient.connect(ip, 21);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Coge la fecha de hoy como prefijo (por ejemplo 20250611_)
            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";

            if (ftpClient.login("mondragon_edu", key)) {
                // Elimina ficheros antiguos
                deleteOldFiles(prefix);

                // Descarga todos los ficheros de hoy
                for (int i = 0; i < DOWNLOAD_FILENAMES.size(); i++) {
                    String remoteFile = prefix + DOWNLOAD_FILENAMES.get(i) + DOWNLOAD_APPENDIX;
                    String localFilePath = DOWNLOAD_PATH + remoteFile;
                    File localFile = new File(localFilePath);
                    downloadFile("/Processed/" + remoteFile, localFilePath, localFile);
                }

                // Paraleliza la lectura de los ficheros
                createThreads();
                ftpClient.logout();
            } else {
                System.out.println("Login failed.");
            }

        } catch (IOException | InterruptedException e) {
            throw new InterruptedException();
        }
    }

    public static String getKey(String path) throws IOException {
        // Devuelve el contenido de un .txt
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public void downloadFile(String remoteFile, String localFilePath, File localFile) {
        if (localFile.exists()) {
            // Si el fichero ya existe no lo descarga
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

    private static void deleteOldFiles(String prefix) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(DOWNLOAD_PATH))) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                // Si el prefijo del fichero no coincide con el de hoy es antiguo
                if (!fileName.startsWith(prefix)) {
                    Files.delete(file);
                    System.out.println("Deleted old file: " + fileName);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cleaning up old files: " + e.getMessage());
        }
    }

    private static void createThreads() throws InterruptedException {
        List<Future<List<ConcurrentMap<String, String>>>> futures;
        List<CSVDownloaderReader> tasks = new ArrayList<>();

        int cpu = Runtime.getRuntime().availableProcessors();
        int start = 0;
        int total = DOWNLOAD_FILENAMES.size();
        int threads = Math.min(cpu, total);
        int step = (int) Math.ceil((double) total / threads);

        ExecutorService executorService = Executors.newFixedThreadPool(cpu);

        for (int i = 0; i < threads; i++) {
            int end = Math.min(start + step, total);
            tasks.add(new CSVDownloaderReader(start, end, DOWNLOAD_FILENAMES));
            start = end;
        }

        futures = executorService.invokeAll(tasks);

        Map<String, Map<String, String>> tagToFileValues = new LinkedHashMap<>();

        for (Future<List<ConcurrentMap<String, String>>> future : futures) {
            try {
                List<ConcurrentMap<String, String>> resultList = future.get();
                for (ConcurrentMap<String, String> resultMap : resultList) {
                    // Hace un mapa del fichero y sus valores
                    String fileName = resultMap.get("title");
                    tagToFileValues(resultMap, fileName, tagToFileValues);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new InterruptedException();
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.SECONDS);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode arrayNode = mapper.createArrayNode();

            // Crea un JSON con todos los valores
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
            // Exception
        }
    }

    public static void tagToFileValues(ConcurrentMap<String, String> resultMap, String fileName,
            Map<String, Map<String, String>> tagToFileValues) {
        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            // Si la key es el nombre del fichero no lo mete
            if ("title".equals(key))
                continue;

            // Si la key es un ID de estación lo mete
            tagToFileValues
                    .computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .put(fileName, entry.getValue());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        new ConnectToServer();
        long finish = System.currentTimeMillis();
        System.out.println("Tiempo empleado: " + (finish - start) + "ms");
    }
}
