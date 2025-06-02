package batch.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class FTPDownloadExample {
    final static String SERVER = "168.63.45.161";
    final static int PORT = 21;
    final static String USERNAME = "mondragon_edu";
    final static String PASSWORD = "mondragon@1975";
    final static String DOWNLOAD_PATH = "./src/main/resources/ftp/";
    final static String DOWNLOAD_APPENDIX = "_SAI-CHC.csv";
    final static List<String> DOWNLOAD_FILENAMES = Arrays.asList(
            "caudal_rio", "nivel_embalse", "nivel_embalse_visor",
            "nivel_rio", "porcentaje_llenado_embalse", "porcentaje_llenado_embalse_visor", "precipitacion",
            "temperatura_agua", "temperatura", "volumen_embalse", "volumen_embalse_visor");

    public static void main(String[] args) {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(SERVER, PORT);
            boolean success = ftpClient.login(USERNAME, PASSWORD);

            if (success) {
                // String prefix =
                // LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";
                String prefix = "20250601_";

                for (String fileName : DOWNLOAD_FILENAMES) {
                    String remoteFile = prefix + fileName + DOWNLOAD_APPENDIX;
                    String downloadPath = DOWNLOAD_PATH + remoteFile;

                    try (OutputStream outputStream = new FileOutputStream(downloadPath)) {
                        ftpClient.retrieveFile(remoteFile, outputStream);

                        if (success) {
                        } else {
                            System.out.println("Failed to download: " + remoteFile);
                        }
                    } catch (Exception e) {
                        System.err.println("Error downloading " + fileName + ": " + e.getMessage());
                    }
                }

            } else {
                System.out.println("Login failed.");
            }

            ftpClient.logout();
            ftpClient.disconnect();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
