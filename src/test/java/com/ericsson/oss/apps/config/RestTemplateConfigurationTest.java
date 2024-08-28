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

package com.ericsson.oss.apps.config;

import com.ericsson.oss.apps.client.ApiClient;
import com.ericsson.oss.apps.client.gw.GatewayServiceApi;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.util.Map;

import static com.ericsson.oss.apps.util.Constants.*;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"gateway.insecure=true"})
@AutoConfigureWireMock(port = 0, httpsPort = 0)
public class RestTemplateConfigurationTest {

    private static final Map<String, String> HEADER_MAPPING = Map.of(
        LOGIN, "X-Login",
        PASSWORD, "X-password",
        TENANT, "X-tenant"
    );

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private GatewayProperties gatewayProperties;
    @Autowired
    private GatewayServiceApi gatewayServiceApi;

    @Value("${wiremock.server.port}")
    private String httpPort;
    @Value("${wiremock.server.https-port}")
    private String httpsPort;

    @BeforeEach
    public void setUp() {
        MappingBuilder mapper = post(urlPathEqualTo(LOGIN_ENDPOINT));
        HEADER_MAPPING.forEach((key, value) -> mapper.withHeader(value, equalTo(gatewayProperties.getAuth(key))));
        stubFor(mapper.willReturn(WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN_VALUE)
                .withStatus(HttpStatus.SC_OK)
                .withBody(MOCK_UUID_VALUE)));
    }

    private void configureClient(String scheme, String Port) {
        gatewayProperties.setScheme(scheme);
        gatewayProperties.setPort(Port);
        apiClient.setBasePath(gatewayProperties.getUrl());
    }

    @Test
    public void testHttpLogin() {
        configureClient(HTTP, httpPort);
        assertEquals(MOCK_UUID_VALUE, gatewayServiceApi.login());
    }

    @Test
    public void testHttpsLogin() {
        configureClient(HTTPS, httpsPort);
        assertEquals(MOCK_UUID_VALUE, gatewayServiceApi.login());
    }
}
