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

package com.ericsson.oss.apps.util.metrics;

import com.ericsson.oss.apps.config.MetricProperties;
import com.ericsson.oss.apps.service.MetricService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static com.ericsson.oss.apps.util.TestDefaults.INSTANCE_ID_VALUE;
import static com.ericsson.oss.apps.util.TestDefaults.UNIQUE_APP_ID_VALUE;

@TestConfiguration
public class MetricsTestConfig {

    private static final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private static final MetricProperties metricProperties = new MetricProperties();
    static {
        metricProperties.setUniqueAppId(UNIQUE_APP_ID_VALUE);
        metricProperties.setInstance(INSTANCE_ID_VALUE);
    }
    private static final MetricService metricService = new MetricService(meterRegistry, metricProperties);

    @Bean
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    @Bean
    public MetricService getMetricService() {
        return metricService;
    }
}