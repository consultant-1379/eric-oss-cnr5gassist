/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.config.client;

import com.ericsson.oss.apps.config.ConnectionKeepAliveStrategyCustomizer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static com.ericsson.oss.apps.util.Constants.HTTP;
import static com.ericsson.oss.apps.util.Constants.HTTPS;

public class InsecureRestTemplateCustomizer implements RestTemplateCustomizer {

    private static final int MAX_TOTAL_CONNECTIONS = 50;
    public static final int MAX_TOTAL_CONNECTIONS_PER_ROUTE = 20;
    private static final int REQUEST_TIMEOUT = 10000;
    private static final int SOCKET_TIMEOUT = 60000;

    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public InsecureRestTemplateCustomizer() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
            NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> socketFactoryRegistry =
            RegistryBuilder.<ConnectionSocketFactory>create()
                .register(HTTPS, sslsf)
                .register(HTTP, new PlainConnectionSocketFactory())
                .build();

        PoolingHttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_TOTAL_CONNECTIONS_PER_ROUTE);
        connectionManager.setDefaultSocketConfig(
            SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT)
            ).build());

        RequestConfig requestConfig = RequestConfig.custom()
            .setCookieSpec(StandardCookieSpec.STRICT)
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(REQUEST_TIMEOUT))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .setKeepAliveStrategy(new ConnectionKeepAliveStrategyCustomizer())
            .build();

        this.clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.setRequestFactory(clientHttpRequestFactory);
    }
}
