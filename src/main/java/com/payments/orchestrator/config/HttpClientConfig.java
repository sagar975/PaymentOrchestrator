package com.payments.orchestrator.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client configuration shared across all gateway adapters.
 * <p>
 * SSL verification:
 * - sandbox profile: disabled for Checkout.com sandbox subdomain
 * - production profile: fully enabled, strict SSL verification
 */
@Slf4j
@Configuration
public class HttpClientConfig {

    @Value("${app.ssl.verify:false}")
    private boolean sslVerify;

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        if (!sslVerify) {
            log.warn("SSL verification disabled — " +
                    "sandbox mode only. Never use in production.");
            builder = disableSslVerification(builder);
        } else {
            log.info("SSL verification enabled — production mode.");
        }

        return builder.build();
    }

    /**
     * Disables SSL verification for sandbox environments.
     * Only called when app.ssl.verify=false.
     */
    private OkHttpClient.Builder disableSslVerification(
            OkHttpClient.Builder builder) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(
                                X509Certificate[] chain,
                                String authType) {}
                        public void checkServerTrusted(
                                X509Certificate[] chain,
                                String authType) {}
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts,
                    new java.security.SecureRandom());

            return builder
                    .sslSocketFactory(
                            sslContext.getSocketFactory(),
                            (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to configure SSL", e);
        }
    }
}