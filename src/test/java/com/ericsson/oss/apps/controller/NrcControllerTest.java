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

package com.ericsson.oss.apps.controller;

import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.manager.NrcManager;
import com.ericsson.oss.apps.util.TestDefaults;
import com.ericsson.oss.apps.util.metrics.MetricsTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.TASK_QUEUE_IS_FULL;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NrcController.class)
@Import(MetricsTestConfig.class)
class NrcControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MeterRegistry meterRegistry;
    @MockBean
    private NrcManager nrcManager;

    @AfterEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void startNrc200OKTest() throws Exception {
        when(nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST)).thenReturn(Optional.empty());
        when(nrcManager.startNrc(TestDefaults.NRC_REQUEST)).thenReturn(TestDefaults.MOCK_UUID);

        mockMvc.perform(post(START_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TestDefaults.NRC_REQUEST)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(TestDefaults.MOCK_UUID.toString())));

        assertEquals(1.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, START_NRC, METHOD, POST, HTTP_STATUS, "200",
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void startNrc208OKTest() throws Exception {
        when(nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST)).thenReturn(
            Optional.of(TestDefaults.MOCK_UUID));
        when(nrcManager.startNrc(TestDefaults.NRC_REQUEST)).thenReturn(TestDefaults.MOCK_UUID);

        mockMvc.perform(post(START_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TestDefaults.NRC_REQUEST)))
            .andDo(print())
            .andExpect(status().isAlreadyReported())
            .andExpect(content().string(containsString(TestDefaults.MOCK_UUID.toString())));

        assertEquals(1.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, START_NRC, METHOD, POST, HTTP_STATUS, "208",
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void startNrc400BadRequestTest() throws Exception {
        NrcRequest badNrcRequest1 = NrcRequest.builder().build();
        NrcRequest badNrcRequest2 = NrcRequest.builder().eNodeBIds(Collections.emptySet()).build();

        for (String content : Arrays.asList("", "{}",
            objectMapper.writeValueAsString(badNrcRequest1),
            objectMapper.writeValueAsString(badNrcRequest2))) {
            mockMvc.perform(post(START_ENDPOINT)
                    .contentType(APPLICATION_JSON)
                    .content(content))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }

        assertEquals(4.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, START_NRC, METHOD, POST, HTTP_STATUS, "400",
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void startNrc503ServiceUnavailableTest() throws Exception {
        when(nrcManager.startNrc(TestDefaults.NRC_REQUEST_WILL_BE_REJECTED))
            .thenThrow(new RejectedExecutionException());

        mockMvc.perform(post(START_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(NRC_REQUEST_WILL_BE_REJECTED)))
            .andDo(print())
            .andExpect(status().is(503))
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
            .andExpect(result -> assertTrue(result.getResolvedException().getMessage().contains(TASK_QUEUE_IS_FULL)));

        assertEquals(1.0, meterRegistry.get(NRC_THREAD_QUEUE_FULL_COUNT)
            .tags(UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE)
            .counter().count());
        assertEquals(1.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, START_NRC, METHOD, POST, HTTP_STATUS, "503", CAUSE, CONGESTION,
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void monitoring200OKTest() throws Exception {
        when(nrcManager.getNrcProcesses()).thenReturn(
            Stream.of(NRC_PROCESS_SUCCESS).collect(Collectors.toCollection(LinkedList::new)));

        mockMvc.perform(get(MONITORING_ENDPOINT))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(TestDefaults.MOCK_UUID.toString())));

        assertEquals(1.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, MONITORING, METHOD, GET, HTTP_STATUS, "200",
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void monitoringById200OKTest() throws Exception {
        when(nrcManager.getNrcData(TestDefaults.MOCK_UUID)).thenReturn(Optional.of(TestDefaults.NRC_DATA_SUCCESS));

        mockMvc.perform(get(MONITORING_ENDPOINT_BY_ID, TestDefaults.MOCK_UUID))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(TestDefaults.NRC_TASK_SUCCESS)));

        assertEquals(1.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, MONITORING_BY_ID, METHOD, GET, HTTP_STATUS, "200",
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }

    @Test
    void monitoringById404NotFoundTest() throws Exception {
        when(nrcManager.getNrcTask(TestDefaults.MOCK_UUID)).thenReturn(Optional.empty());

        mockMvc.perform(get(MONITORING_ENDPOINT_BY_ID, TestDefaults.MOCK_UUID))
            .andDo(print())
            .andExpect(status().isNotFound());

        assertEquals(1.0, meterRegistry.get(NRC_HTTP_REQUESTS)
            .tags(ENDPOINT, MONITORING_BY_ID, METHOD, GET, HTTP_STATUS, "404",
                UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).counter().count());
    }
}
