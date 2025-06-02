package batch.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class CSVDownloadFTP {
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
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_";

            if (ftpClient.login(USERNAME, PASSWORD)) {
                long inicio = System.currentTimeMillis();
                deleteOldFiles(prefix);
                downloadNewFiles(prefix, ftpClient);
                long fin = System.currentTimeMillis();
                System.out.println("Total download: " + (fin - inicio) + " ms");
                ftpClient.logout();
            } else {
                System.out.println("Login failed.");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
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

    private static void downloadNewFiles(String prefix, FTPClient ftpClient) {
        for (String fileName : DOWNLOAD_FILENAMES) {
            String remoteFile = prefix + fileName + DOWNLOAD_APPENDIX;
            String localFilePath = DOWNLOAD_PATH + remoteFile;

            File localFile = new File(localFilePath);
            if (localFile.exists()) {
                System.out.println("File already downloaded: " + remoteFile);
                continue;
            }

            try (OutputStream outputStream = new FileOutputStream(localFilePath)) {
                ftpClient.retrieveFile(remoteFile, outputStream);
                System.out.println("Downloaded: " + remoteFile);
            } catch (Exception e) {
                System.err.println("Error downloading " + remoteFile + ": " + e.getMessage());
            }
        }
    }
}
