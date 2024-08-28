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

package com.ericsson.oss.apps.api.contract;

import com.ericsson.oss.apps.config.GlobalExceptionHandler;
import com.ericsson.oss.apps.controller.NrcController;
import com.ericsson.oss.apps.manager.NrcManager;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.util.TestDefaults;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.TestDefaults.*;

@SpringBootTest
public class ContractVerifierBase {

    @Autowired
    private NrcController nrcController;

    @Autowired
    private MetricService metricService;

    @MockBean
    private NrcManager nrcManager;

    @BeforeEach
    public void setup() {
        Mockito.when(nrcManager.checkRequestIsInHistory(NRC_REQUEST)).thenReturn(Optional.empty());
        Mockito.when(nrcManager.checkRequestIsInHistory(NRC_REQUEST_MULTIPLE_ENODEBS)).thenReturn(
            Optional.of(TestDefaults.MOCK_UUID_2));

        Mockito.when(nrcManager.startNrc(NRC_REQUEST)).thenReturn(TestDefaults.MOCK_UUID);
        Mockito.when(nrcManager.startNrc(NRC_REQUEST_WILL_BE_REJECTED)).thenThrow(new RejectedExecutionException());

        Mockito.when(nrcManager.getNrcTask(MOCK_UUID)).thenReturn(Optional.of(NRC_TASK_SUCCESS));
        Mockito.when(nrcManager.getNrcProcesses()).thenReturn(
            Stream.of(NRC_PROCESS_SUCCESS).collect(Collectors.toCollection(LinkedList::new)));
        Mockito.when(nrcManager.getNrcData(TestDefaults.MOCK_UUID)).thenReturn(Optional.of(TestDefaults.NRC_DATA_SUCCESS));

        final StandaloneMockMvcBuilder standaloneMockMvcBuilder = MockMvcBuilders.standaloneSetup(nrcController, new GlobalExceptionHandler(metricService));
        RestAssuredMockMvc.standaloneSetup(standaloneMockMvcBuilder);
    }
}
