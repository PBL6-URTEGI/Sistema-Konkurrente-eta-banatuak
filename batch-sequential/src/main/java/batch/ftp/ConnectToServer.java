package batch.ftp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConnectToServer {
    CSVDownloaderReader csvReader;

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
        csvReader = new CSVDownloaderReader(DOWNLOAD_FILENAMES);

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
                csvReader.deleteOldFiles(prefix);

                // Descarga todos los ficheros de hoy
                for (String fileName : DOWNLOAD_FILENAMES) {
                    String remoteFile = prefix + fileName + DOWNLOAD_APPENDIX;
                    String localFilePath = DOWNLOAD_PATH + remoteFile;
                    File localFile = new File(localFilePath);
                    csvReader.downloadFile("/Processed/" + remoteFile, localFilePath, localFile, ftpClient);
                }

                // Lee los ficheros
                readFiles();
                ftpClient.logout();
            } else {
                System.out.println("Login failed.");
            }

        } catch (Exception e) {
            throw new InterruptedException();
        }
    }

    public static String getKey(String path) throws IOException {
        // Devuelve el contenido de un .txt
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    private void readFiles() throws Exception {
        List<Map<String, String>> mapList = csvReader.readFiles();
        Map<String, Map<String, String>> tagToFileValues = new LinkedHashMap<>();

        for (Map<String, String> map : mapList) {
            // Hace un mapa del fichero y sus valores
            String fileName = map.get("title");
            tagToFileValues(map, fileName, tagToFileValues);
        }

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

    public static void tagToFileValues(Map<String, String> resultMap, String fileName,
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
