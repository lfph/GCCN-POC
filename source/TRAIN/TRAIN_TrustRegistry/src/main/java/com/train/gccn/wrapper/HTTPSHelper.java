package com.train.gccn.wrapper;


import com.train.gccn.ATVConfiguration;
import okhttp3.*;
import org.apache.log4j.Logger;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class HTTPSHelper {
    
    private static long TIMEOUT = ATVConfiguration.get().getInt("http_timeout", 15); // in seconds
    private static Logger logger = Logger.getLogger(HTTPSHelper.class);
    private OkHttpClient client;
    private OkHttpClient.Builder builder;
    private DANETrustManager trustManager = null;
    
    public HTTPSHelper() {
        // fix for JEP 229, see https://extgit.iaik.tugraz.at/LIGHTest/AutomaticTrustVerifier/issues/47#note_22080
        Security.setProperty("keystore.type", "jks");
    
        this.builder = new OkHttpClient().newBuilder()
                .callTimeout(HTTPSHelper.TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(HTTPSHelper.TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HTTPSHelper.TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(HTTPSHelper.TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(false) // DANETrustManager does not support redirects right now
                .followSslRedirects(true)
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.RESTRICTED_TLS));
    
        rebuildClient();

//        try {
//            if(ATVConfiguration.get().getBoolean("dane_verification_enabled")) {
//                this.enableDANE();
//            } else {
//                this.disableDANE();
//            }
//        } catch(KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
//            e.printStackTrace();
//        }
    }
    
    /**
     * @param timeout in seconds
     */
    public void setTimeout(long timeout, TimeUnit unit) {
        this.builder = new OkHttpClient().newBuilder()
                .callTimeout(timeout, unit)
                .connectTimeout(timeout, unit);
        
        rebuildClient();
    }
    
    public void disableOldTLS() {
        // https://github.com/square/okhttp/wiki/HTTPS
        this.builder = this.builder
                .connectionSpecs(Collections.singletonList(ConnectionSpec.RESTRICTED_TLS));
        
        rebuildClient();
    }
    
    public void enableDANE() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
        
        X509TrustManager defaultTrustManager = this.getDefaultX509TrustManager();
        this.trustManager = new DANETrustManager(defaultTrustManager);
        
        SSLSocketFactory sslSocketFactory = this.getDefaultSSLSocketFactory(this.trustManager);
        
        this.builder = this.builder.sslSocketFactory(sslSocketFactory, this.trustManager);
        
        rebuildClient();
    }
    
    public void disableDANE() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this.trustManager = null;
        
        X509TrustManager defaultTrustManager = this.getDefaultX509TrustManager();
        SSLSocketFactory sslSocketFactory = this.getDefaultSSLSocketFactory(defaultTrustManager);
        
        this.builder = this.builder.sslSocketFactory(sslSocketFactory, defaultTrustManager);
        
        rebuildClient();
    }
    
    private X509TrustManager getDefaultX509TrustManager() throws KeyStoreException, NoSuchAlgorithmException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if(trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        
        return (X509TrustManager) trustManagers[0];
    }
    
    private SSLSocketFactory getDefaultSSLSocketFactory(X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        return sslContext.getSocketFactory();
    }
    
    /**
     * Pins certificates for {@code pattern}.
     *
     * @param pattern lower-case host name or wildcard pattern such as {@code *.example.com}.
     * @param pin     SHA-256 or SHA-1 hash. Each pin is a hash of a certificate's Subject Public Key
     *                Info, base64-encoded and prefixed with either {@code sha256/} or {@code sha1/}.
     */
    public void pinCert(String pattern, String pin) {
        // example:
        // "publicobject.com", "sha256/afwiKY3RxoMmLkuRW1l7QsPZTJPwDS2pdDROQjXw8ig="
        CertificatePinner certPinner = new CertificatePinner.Builder()
                .add(pattern, pin)
                .build();
        this.builder.certificatePinner(certPinner);
        
        rebuildClient();
    }
    
    private void rebuildClient() {
        this.client = this.builder.build();
    }
    
    public String getXML(URL url) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Accept", "application/xml")
                .build();
        
        return this.doRequest(request);
    }
    
    public String get(URL url) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        
        return this.doRequest(request);
    }
    
    private String doRequest(Request request) throws IOException {
        
        if(this.trustManager != null) {
            this.trustManager.setHost(request.url().host());
        }
        
        Response response = this.client.newCall(request).execute();
        
        if(!response.isSuccessful()) {
            HTTPSHelper.logger.error("Cound not GET " + request.url().toString() + ", code: " + response.code());
            return null;
        }
        
        return response.body().string();
    }
}
