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

package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.config.MetricProperties;
import com.ericsson.oss.apps.model.CacheData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.CTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(properties = {"metric.uniqueAppId: app_name", "metric.instance: instance_name"})
public class MetricServiceTest {

    private static final String CTS_CACHE_KEY = "ctsServiceMethod,[service=cts],EXTERNAL_ID";
    private static final String NCMP_CACHE_KEY1 = "ncmpServiceMethod,[service=ncmp],EXTERNAL_ID1";
    private static final String NCMP_CACHE_KEY2 = "ncmpServiceMethod,[service=ncmp],EXTERNAL_ID2";

    private MeterRegistry meterRegistry;
    private MetricService metricService;
    @Autowired
    private MetricProperties metricProperties;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        metricService = new MetricService(meterRegistry, metricProperties);
    }

    @AfterEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void findCountersTest() {
        metricService.increment(CTS_HTTP_REQUESTS, HTTP_STATUS, "200");
        metricService.increment(CTS_HTTP_REQUESTS, HTTP_STATUS, "400");
        metricService.increment(CTS_HTTP_REQUESTS, HTTP_STATUS, "400");

        assertFalse(metricService.findCounter(CTS_HTTP_REQUESTS).isPresent());
        assertTrue(metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "200").isPresent());
        assertTrue(metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "400").isPresent());
        assertEquals(1.0, metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "200").get().count());
        assertEquals(2.0, metricService.findCounter(CTS_HTTP_REQUESTS, HTTP_STATUS, "400").get().count());
    }

    @Test
    void findGaugesTest() {
        Map<String, CacheData> cacheDataMap = new ConcurrentHashMap<>();
        metricService.createGauge(CACHE_SIZE, cacheDataMap,
            e -> e.keySet().stream().filter(key -> key.contains(CTS_TAG)).collect(Collectors.toList()).size(),
            SERVICE, CTS);
        metricService.createGauge(CACHE_SIZE, cacheDataMap,
            e -> e.keySet().stream().filter(key -> key.contains(NCMP_TAG)).collect(Collectors.toList()).size(),
            SERVICE, NCMP);
        cacheDataMap.put(CTS_CACHE_KEY, new CacheData<>("", 0));
        cacheDataMap.put(NCMP_CACHE_KEY1, new CacheData<>("", 0));
        cacheDataMap.put(NCMP_CACHE_KEY2, new CacheData<>("", 0));

        assertFalse(metricService.findGauge(CACHE_SIZE).isPresent());
        assertTrue(metricService.findGauge(CACHE_SIZE, SERVICE, CTS).isPresent());
        assertTrue(metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).isPresent());
        assertEquals(1.0, metricService.findGauge(CACHE_SIZE, SERVICE, CTS).get().value());
        assertEquals(2.0, metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).get().value());
    }
}
