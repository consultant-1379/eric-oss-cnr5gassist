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

import com.ericsson.oss.apps.exception.EnmAdapterOverloadedException;
import com.ericsson.oss.apps.service.MetricService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.TimeUnit;

import static com.ericsson.oss.apps.util.Constants.CTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(properties = {"metric.uniqueAppId: app_name", "metric.instance: instance_name"})
public class ClientAspectsMetricTest {

    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Signature signature;
    @Mock
    private RetryTemplate retryTemplate;

    private ClientAspects clientAspects;
    private MeterRegistry meterRegistry;
    private MetricService metricService;
    @Autowired
    private MetricProperties metricProperties;

    private static final String ctsText = "/ctw/gnbdu";
    private static final String ncmpText = "/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational";

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        metricService = new MetricService(meterRegistry, metricProperties);
        clientAspects = new ClientAspects(objectMapper, retryTemplate, metricService);
        Mockito.lenient().when(pjp.getSignature()).thenReturn(signature);
        Mockito.lenient().when(signature.getName()).thenReturn(TEXT);
    }

    @AfterEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void measureCtsRequestTimerTest() {
        clientAspects.measureCtsRequestBefore(pjp);
        clientAspects.measureCtsRequestAfter(pjp);

        assertThat(metricService.findTimer(CTS_PROCESSING_HTTP_REQUEST_DURATION_SECONDS).get()
            .totalTime(TimeUnit.MILLISECONDS), allOf(greaterThan(0.0), lessThan(100.0)));
    }

    @Test
    void measureCtsRequestResultTest() {
        clientAspects.measureCtsRequestBefore(pjp);
        clientAspects.measureCtsRequestAfterReturning(pjp);
        clientAspects.measureCtsRequestBefore(pjp);
        clientAspects.measureCtsRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        clientAspects.measureCtsRequestBefore(pjp);
        clientAspects.measureCtsRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertEquals(1.0, metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "200").get().count());
        assertEquals(2.0, metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
    }

    @Test
    void measureNcmpRequestTimerTest() throws InterruptedException {
        clientAspects.measureNcmpRequestBefore(pjp);
        clientAspects.measureNcmpRequestAfter(pjp);

        assertThat(metricService.findTimer(NCMP_PROCESSING_HTTP_REQUEST_DURATION_SECONDS).get()
            .totalTime(TimeUnit.MILLISECONDS), allOf(greaterThan(0.0), lessThan(100.0)));
    }

    @Test
    void measureNcmpRequestResultTest() {
        clientAspects.measureNcmpRequestBefore(pjp);
        clientAspects.measureNcmpRequestAfterReturning(pjp);
        clientAspects.measureNcmpRequestBefore(pjp);
        clientAspects.measureNcmpRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        clientAspects.measureNcmpRequestBefore(pjp);
        clientAspects.measureNcmpRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertEquals(1.0, metricService.findCounter(NCMP_HTTP_REQUESTS, HTTP_STATUS, "200").get().count());
        assertEquals(2.0, metricService.findCounter(NCMP_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
    }

    @Test
    void measureApiGatewayLoginRequestTimerTest() throws InterruptedException {
        clientAspects.measureApiGatewayLoginRequestBefore(pjp);
        clientAspects.measureApiGatewayLoginRequestAfter(pjp);

        assertThat(metricService.findTimer(APIGATEWAY_PROCESSING_HTTP_REQUEST_DURATION_SECONDS).get()
            .totalTime(TimeUnit.MILLISECONDS), allOf(greaterThan(0.0), lessThan(100.0)));
    }

    @Test
    void measureApiGatewayLoginRequestResultTest() {
        clientAspects.measureApiGatewayLoginRequestBefore(pjp);
        clientAspects.measureApiGatewayLoginRequestAfterReturning(pjp);
        clientAspects.measureApiGatewayLoginRequestBefore(pjp);
        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        clientAspects.measureApiGatewayLoginRequestBefore(pjp);
        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertEquals(1.0, metricService.findCounter(APIGATEWAY_SESSIONID_HTTP_REQUESTS, HTTP_STATUS, "200").get().count());
        assertEquals(2.0, metricService.findCounter(APIGATEWAY_SESSIONID_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
    }

    @Test
    void measureApiGatewaySessionIdQueries200Test() {
        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertEquals(2.0, metricService.findCounter(APIGATEWAY_SESSIONID_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
    }

    @Test
    void measureApiGatewaySessionIdQueries400Test() {
        RestClientException err = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, err);

        assertEquals(1.0, metricService.findCounter(APIGATEWAY_SESSIONID_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
    }

    @Test
    void measureApiGatewaySessionIdQueries500Test() {
        RestClientException err = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, err);

        assertEquals(1.0, metricService.findCounter(APIGATEWAY_SESSIONID_HTTP_REQUESTS, HTTP_STATUS, "500").get().count());
    }

    @Test
    void measureApiGatewaySessionIdQueries300Test() {
        RestClientException err1 = new RestClientException("This message does not contain any status");
        RestClientException err2 = new RestClientException("API returned 300 it wasn't handled by the RestTemplate error handler");

        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, err1);
        clientAspects.measureApiGatewayLoginRequestAfterThrowing(pjp, err2);

        // The 3xx response statuses are not counted
        assertFalse(metricService.findCounter(APIGATEWAY_SESSIONID_HTTP_REQUESTS, HTTP_STATUS, "300").isPresent());
        assertThrows(MeterNotFoundException.class, () -> meterRegistry.get(APIGATEWAY_SESSIONID_HTTP_REQUESTS)
            .tags(HTTP_STATUS, "300", UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter());
    }

    @Test
    void findMetricTest() {
        clientAspects.measureCtsRequestBefore(pjp);
        clientAspects.measureCtsRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        clientAspects.measureNcmpRequestBefore(pjp);
        clientAspects.measureNcmpRequestAfterThrowing(pjp, new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertEquals(1.0, metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
        assertEquals(1.0, metricService.findCounter(NCMP_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
        assertEquals(1.0, meterRegistry.get(CTS_HTTP_REQUESTS)
            .tags(HTTP_STATUS, "400", UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
        assertEquals(1.0, meterRegistry.get(NCMP_HTTP_REQUESTS)
            .tags(HTTP_STATUS, "400", UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void overLoadRetryMetricTest() throws Throwable {
        Mockito.when(pjp.getArgs()).thenReturn(new Object[] {ctsText});
        clientAspects.overLoadRetryMetric(pjp, new ResourceAccessException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertEquals(1.0, meterRegistry.get(RETRY_HTTP_REQUESTS)
            .tags(OBJECT_TYPE, CTS, UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());

        Mockito.when(pjp.getArgs()).thenReturn(new Object[] {ncmpText});
        clientAspects.overLoadRetryMetric(pjp, new ResourceAccessException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertEquals(1.0, meterRegistry.get(RETRY_HTTP_REQUESTS)
            .tags(OBJECT_TYPE, NCMP, UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());

        clientAspects.overLoadRetryMetric(pjp, new EnmAdapterOverloadedException(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertEquals(1.0, meterRegistry.get(ENM_ADAPTER_OVERLOAD_RETRY_COUNT).counter().count());
    }
}
