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

package com.ericsson.oss.apps.manager;

import com.ericsson.oss.apps.api.model.NrcProcessStatus;
import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.config.NrcProperties;
import com.ericsson.oss.apps.controller.NrcController;
import com.ericsson.oss.apps.model.NrcData;
import com.ericsson.oss.apps.service.NrcService;
import com.ericsson.oss.apps.util.TestDefaults;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(properties = {"nrc.thread-queue-size=2", "nrc.history-size=5", "metric.uniqueAppId: app_name", "metric.instance: instance_name"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NrcManagerTest {

    @MockBean
    private NrcService nrcService;
    @SpyBean
    private TaskExecutor taskExecutor;
    @Autowired
    private NrcProperties nrcProperties;
    @Autowired
    private NrcManager nrcManager;
    @Autowired
    private NrcController nrcController;
    @Autowired
    private MeterRegistry meterRegistry;


    private final Queue<NrcData> nrcTaskHistoryMock = new ConcurrentLinkedQueue<>();

    private Callable<Boolean> checkStatus(UUID uuid, NrcProcessStatus status) {
        return () -> nrcManager.getNrcTask(uuid).map(i -> i.getProcess().getNrcStatus() == status).orElse(false);
    }

    @BeforeEach
    void setup() {
        nrcTaskHistoryMock.clear();
        ReflectionTestUtils.setField(nrcManager, "nrcTaskHistory", nrcTaskHistoryMock);
    }

    @AfterEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    @Test
    void nrcTaskHistory() throws InterruptedException {
        doAnswer(nrcRequest -> List.of(NRC_NEIGHBOR)).when(nrcService).startNrc(any());

        List<UUID> uuidList = IntStream.range(0, nrcProperties.getHistorySize() + 1).mapToObj(i -> {
            NrcRequest nrcRequest = NrcRequest.builder().eNodeBIds(Collections.singleton(i + 100L)).build();
            UUID uuid = nrcManager.startNrc(nrcRequest);
            await().atMost(5L, TimeUnit.SECONDS).until(checkStatus(uuid, NrcProcessStatus.SUCCEEDED));
            return uuid;
        }).collect(Collectors.toList());

        //5 sec idle time
        TimeUnit.SECONDS.sleep(1L);

        //sending request after idle time
        nrcManager.startNrc(NrcRequest.builder()
            .eNodeBIds(Collections.singleton(nrcProperties.getHistorySize() + 2 + 100L)).build());

        assertEquals(nrcProperties.getHistorySize(), nrcManager.getNrcProcesses().size());
        assertFalse(nrcManager.getNrcTask(uuidList.remove(0)).isPresent());
        assertTrue(uuidList.stream()
            .map(nrcManager::getNrcTask)
            .flatMap(Optional::stream)
            .allMatch(nrcTask -> NrcProcessStatus.SUCCEEDED.equals(nrcTask.getProcess().getNrcStatus()) &&
                !nrcTask.getAllNrcNeighbors().isEmpty()));

        assertEquals(10.0, nrcProperties.getHistorySize(), meterRegistry.get(NRC_REQUEST_COUNT)
            .tags(NRC_STATUS, NrcProcessStatus.SUCCEEDED.getValue(), UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID,
                INSTANCE_ID_VALUE).counter().count());
        assertThat(meterRegistry.get(NRC_PROCESS_HTTP_REQUEST_DURATION_SECONDS)
                .tags(UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).timer()
                .totalTime(TimeUnit.MILLISECONDS),
            allOf(greaterThan(0.0), lessThan(200.0)));
        assertThat(meterRegistry.get(NRC_HISTORY_HTTP_REQUEST_DURATION_SECONDS)
                .tags(UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).timer()
                .totalTime(TimeUnit.MILLISECONDS),
            allOf(greaterThan(0.0), lessThan(1.0)));
        assertThat(meterRegistry.get(NRC_THREAD_QUEUE_IDLE_DURATION_SECONDS)
                .tags(UNIQUE_APP_ID, UNIQUE_APP_ID_VALUE, INSTANCE_ID, INSTANCE_ID_VALUE).timer()
                .totalTime(TimeUnit.SECONDS),
            allOf(greaterThan(1.0), lessThan(5.0)));
    }

    @Test
    void overloadThreadQueue() {
        doAnswer(answersWithDelay(30000L, i -> NRC_NEIGHBOR)).when(nrcService).startNrc(any());

        long size = nrcProperties.getThreadQueueSize() + nrcProperties.getThreadPoolSize();
        LongStream.range(0, size).forEach(i -> {
            NrcRequest nrcRequest = NrcRequest.builder().eNodeBIds(Collections.singleton(i)).build();
            assertDoesNotThrow(() -> nrcManager.startNrc(nrcRequest));
        });

        NrcRequest nrcRequest = NrcRequest.builder().eNodeBIds(Collections.singleton(size)).build();
        assertThrows(RejectedExecutionException.class, () -> nrcManager.startNrc(nrcRequest));
    }

    @Test
    void filterNrcTaskSuccessfulFound() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(TestDefaults.NRC_TASK_SUCCESS)
            .isQueried(false)
            .build());
        Optional<UUID> uuid = nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST);

        assertFalse(uuid.isPresent());
    }

    @Test
    void filterNrcTaskFailedFound() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(TestDefaults.NRC_TASK_FAILED)
            .isQueried(false)
            .build());
        Optional<UUID> uuid = nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST);

        assertFalse(uuid.isPresent());
    }

    @Test
    void filterNrcTaskNotFound() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(TestDefaults.NRC_TASK_ONGOING_2)
            .isQueried(false)
            .build());
        Optional<UUID> uuid = nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST);

        assertFalse(uuid.isPresent());
    }

    @Test
    void filterNrcTaskPendingFound() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(TestDefaults.NRC_TASK_PENDING)
            .isQueried(false)
            .build());
        Optional<UUID> uuid = nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST);

        assertTrue(uuid.isPresent());
    }

    @Test
    void filterNrcTaskOngoingFound() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(TestDefaults.NRC_TASK_ONGOING)
            .isQueried(false)
            .build());
        Optional<UUID> uuid = nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST);

        assertTrue(uuid.isPresent());
    }

    @Test
    void startNrcTaskScheduled() {
        doNothing().when(taskExecutor).execute(any());

        UUID uuid = nrcManager.startNrc(TestDefaults.NRC_REQUEST);

        NrcTask result = nrcManager.getNrcTask(uuid).orElse(null);
        assertNotNull(result);
        assertEquals(TestDefaults.NRC_REQUEST, result.getRequest());
        assertEquals(NrcProcessStatus.PENDING, result.getProcess().getNrcStatus());
    }

    @Test
    void gracefulShutDownNrc() {
        nrcManager.shutdownNrc();
        NrcRequest nrcRequest = NrcRequest.builder().eNodeBIds(Collections.singleton(100L)).build();

        Exception exception = assertThrows(RejectedExecutionException.class, () -> nrcManager.startNrc(nrcRequest));

        String expectedMessage = "Rejected execution exception due to the app shutdown";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void isAllNrcTaskMonitoredTest() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(TestDefaults.NRC_TASK_ONGOING)
            .isQueried(false)
            .build());
        Optional<UUID> uuid = nrcManager.checkRequestIsInHistory(TestDefaults.NRC_REQUEST);
        assertTrue(uuid.isPresent());
        nrcController.monitoringById(uuid.get());
        assertTrue(nrcManager.isAllNrcTaskMonitored());
    }

    @Test
    void getNrcDataNeighborsTest() {
        nrcTaskHistoryMock.add(NrcData.builder()
            .nrcTask(NRC_TASK_SUCCESS)
            .isQueried(false)
            .build());
        Optional<NrcData> nrcData = nrcManager.getNrcData(MOCK_UUID);
        assertTrue(nrcData.isPresent());
        assertEquals(Collections.singletonList(NRC_NEIGHBOR), nrcData.get().getNrcTask().getAllNrcNeighbors());
    }
}
