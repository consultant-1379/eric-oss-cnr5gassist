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


import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectionKeepAliveStrategyCustomizerTest {

    private ConnectionKeepAliveStrategyCustomizer connectionKeepAliveStrategyCustomizer;

    @BeforeEach
    void setUp() {
        connectionKeepAliveStrategyCustomizer = new ConnectionKeepAliveStrategyCustomizer();
    }

    @Test
    void getKeepAliveDurationWithNoHeadersTest() {
        HttpContext context = new BasicHttpContext(null);
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_OK, "OK");
        TimeValue keepAliveDuration = connectionKeepAliveStrategyCustomizer.getKeepAliveDuration(response, context);
        Assertions.assertEquals(TimeValue.ofMilliseconds(20_000L), keepAliveDuration);
    }

    @Test
    void getKeepAliveDurationWithHeadersTest() {
        HttpContext context = new BasicHttpContext(null);
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_OK, "OK");
        response.addHeader("Keep-Alive", "timeout=10, max=20");
        TimeValue keepAliveDuration = connectionKeepAliveStrategyCustomizer.getKeepAliveDuration(response, context);
        Assertions.assertEquals(TimeValue.ofMilliseconds(10_000L), keepAliveDuration);
    }

    @Test
    void getKeepAliveDurationWithNullTimeoutHeadersTest() {
        HttpContext context = new BasicHttpContext(null);
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_OK, "OK");
        response.addHeader("Keep-Alive", "timeout, max=20");
        TimeValue keepAliveDuration = connectionKeepAliveStrategyCustomizer.getKeepAliveDuration(response, context);
        Assertions.assertEquals(TimeValue.ofMilliseconds(20_000), keepAliveDuration);
    }
}