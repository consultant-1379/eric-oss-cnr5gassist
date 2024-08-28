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

package com.ericsson.oss.apps;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureObservability
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CoreApplication.class, CoreApplicationTest.class})
public class CoreApplicationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;

    @Value("${info.app.description}")
    private String description;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void restTemplateMetricsAvailable() {
        List<String> metricsCustomizer =
            Arrays.asList(webApplicationContext.getBeanNamesForType(ObservationRestTemplateCustomizer.class));
        List<ClientHttpRequestInterceptor> restTemplateBeanInterceptors =
            webApplicationContext.getBean(RestTemplate.class).getInterceptors();
        List<ClientHttpRequestInterceptor> restTemplateInterceptor =
            webApplicationContext.getBean(RestTemplateBuilder.class).build().getInterceptors();

        Assertions.assertFalse(metricsCustomizer.isEmpty());
        Assertions.assertTrue(restTemplateBeanInterceptors.containsAll(restTemplateInterceptor));
    }

    @Test
    public void prometheusRegistryAvailable() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Assertions.assertNotNull(prometheusRegistry);
    }

    @Test
    public void metricsAvailable() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/prometheus").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
            .andReturn();
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_threads_states_threads"));
    }

    @Test
    public void infoAvailable() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/info").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
            .andReturn();
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(this.description));
    }
}
