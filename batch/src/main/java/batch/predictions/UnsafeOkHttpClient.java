package batch.predictions;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class UnsafeOkHttpClient {

    private static OkHttpClient client;

    private UnsafeOkHttpClient() {
    }

    public static OkHttpClient getUnsafeClient() {
        if (client == null) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                                // Method unimplemented
                            }

                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                                // Method unimplemented
                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[] {};
                            }
                        }
                };

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier((hostname, session) -> true)
                        .build();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return client;
    }

    public static void shutdown() throws InterruptedException {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            try {
                if (!client.dispatcher().executorService().awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("OkHttpClient executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                throw new InterruptedException();
            }
            client.connectionPool().evictAll();
            if (client.cache() != null) {
                try {
                    client.cache().close();
                } catch (IOException e) {
                    // Exception
                }
            }
        }
    }
}
