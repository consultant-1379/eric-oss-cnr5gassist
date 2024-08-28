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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static com.ericsson.oss.apps.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "gateway.services.cts.url=https://cts.ericsson.se:8080",
    "gateway.services.cts.base-path=/oss-core-ws/rest",
    "gateway.services.cts.headers={\"GS-Database-Name\":\"eai_install\",\"GS-Database-Host-Name\":\"localhost\",\"Authorization\":\"Basic c3lzYWRtOg==\"}",
    "gateway.services.ncmp.url=https://ncmp.ericsson.se:8080",
    "gateway.services.ncmp.base-path=/ncmp",
    "gateway.services.ncmp.headers={}}"})
public class GatewayPropertiesTest {

    private static final String CTS_BASE_PATH = "https://cts.ericsson.se:8080/oss-core-ws/rest";
    private static final String NCMP_BASE_PATH = "https://ncmp.ericsson.se:8080/ncmp";
    private static final String GS_DATABASE_NAME = "GS-Database-Name";
    private static final String EAI_INSTALL = "eai_install";
    private static final String GS_DATABASE_HOST_NAME = "GS-Database-Host-Name";
    private static final String LOCALHOST = "localhost";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC = "Basic c3lzYWRtOg==";
    private static final String PORT = "8080";
    private static final String BAD_JSON = "{bad-json}";

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    public void getUrl() {
        String origPort = gatewayProperties.getPort();
        gatewayProperties.setPort(PORT);
        assertEquals(String.format("%s://%s:%s", HTTP, LOCALHOST, PORT), gatewayProperties.getUrl());
        gatewayProperties.setPort(origPort);
        assertEquals(String.format("%s://%s", HTTP, LOCALHOST), gatewayProperties.getUrl());
    }

    @Test
    public void getBasePath() {
        assertEquals(CTS_BASE_PATH, gatewayProperties.getBasePath(CTS));
        assertEquals(NCMP_BASE_PATH, gatewayProperties.getBasePath(NCMP));
    }

    @Test
    public void getHeadersAsMap() {
        Map<String, String> ctsHeaders = gatewayProperties.getService(CTS).getHeadersAsMap();
        Map<String, String> ncmpHeaders = gatewayProperties.getService(NCMP).getHeadersAsMap();
        assertEquals(EAI_INSTALL, ctsHeaders.get(GS_DATABASE_NAME));
        assertEquals(LOCALHOST, ctsHeaders.get(GS_DATABASE_HOST_NAME));
        assertEquals(BASIC, ctsHeaders.get(AUTHORIZATION));
        assertTrue(ncmpHeaders.isEmpty());
    }

    @Test
    public void getHeadersAsMapBadJson() {
        String origHeaders = gatewayProperties.getService(CTS).getHeaders();
        gatewayProperties.getService(CTS).setHeaders(BAD_JSON);
        assertTrue(gatewayProperties.getService(CTS).getHeadersAsMap().isEmpty());
        gatewayProperties.getService(CTS).setHeaders(origHeaders);
        assertFalse(gatewayProperties.getService(CTS).getHeadersAsMap().isEmpty());
    }
}
