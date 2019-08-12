package com.VinsWorld.app;

// https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientExecuteProxy.java
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

// https://www.naschenweng.info/2018/02/01/java-mutual-ssl-authentication-2-way-ssl-authentication/
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;

public class SMAT {

    public static void main(String[] args) {
        try {
            String CERT_ALIAS = "clienttest", CERT_PASSWORD = "password";
            
            KeyStore identityKeyStore = KeyStore.getInstance("pkcs12");
            FileInputStream identityKeyStoreFile = new FileInputStream(new File("ca/keystore.p12"));
            identityKeyStore.load(identityKeyStoreFile, CERT_PASSWORD.toCharArray());
            
            KeyStore trustKeyStore = KeyStore.getInstance("pkcs12");
            FileInputStream trustKeyStoreFile = new FileInputStream(new File("ca/truststore.p12"));
            trustKeyStore.load(trustKeyStoreFile, CERT_PASSWORD.toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                // load identity keystore
                .loadKeyMaterial(identityKeyStore, CERT_PASSWORD.toCharArray(), new PrivateKeyStrategy() {
                    @Override
                    public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
                        return CERT_ALIAS;
                    }
                })
                // load trust keystore
                .loadTrustMaterial(trustKeyStore, null)
                .build();
             
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                new String[]{"TLSv1.2", "TLSv1.1"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            
            CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .build();
             
            // Call a SSL-endpoint
            callEndPoint(httpclient);
        } catch (Exception ex) {
            System.out.println("Boom, we failed: " + ex);
            ex.printStackTrace();
        }
    }

    private static void callEndPoint(CloseableHttpClient httpclient)throws Exception {

        try {
            HttpHost target = new HttpHost("localhost", 44430, "https");
            HttpHost proxy = new HttpHost("proxy.domain.org", 80, "http");

            RequestConfig config = RequestConfig.custom()
//                .setProxy(proxy)
                .build();
            HttpGet request = new HttpGet("/");
            request.setConfig(config);
            
            System.out.println("Executing request " + request.getRequestLine() + " to " + target + " via " + proxy);

            CloseableHttpResponse response = httpclient.execute(target, request);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                System.out.println(EntityUtils.toString(response.getEntity()));
            } finally {
                response.close();
            }
        } catch (Exception ex) {
            System.out.println("Boom, we failed: " + ex);
            ex.printStackTrace();
        } finally {
            httpclient.close();
        }
    }
}
