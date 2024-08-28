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

import com.ericsson.oss.apps.api.model.*;
import com.ericsson.oss.apps.config.NrcProperties;
import com.ericsson.oss.apps.model.NrcData;
import com.ericsson.oss.apps.service.*;
import com.ericsson.oss.apps.util.NrcUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NrcManager {

    public static final List<NrcProcessStatus> RUNNING_STATUSES = List.of(NrcProcessStatus.ONGOING, NrcProcessStatus.PENDING);
    private final NcmpCounterService ncmpCounterService = new NcmpCounterService();

    private final NrcProperties nrcProperties;
    private final NrcService nrcService;
    private final MetricService metricService;
    private final TaskExecutor taskExecutor;
    private final SigtermService sigtermService;
    private final InMemoryCacheService cacheService;

    @Getter
    private final Queue<NrcData> nrcTaskHistory = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void createGauges() {
        metricService.createGauge(NRC_THREADQUEUE_SIZE, nrcTaskHistory, e -> e.stream()
            .filter(task -> RUNNING_STATUSES.contains(task.getNrcTask().getProcess().getNrcStatus())).count());
        metricService.createGauge(NRC_THREADQUEUE_PENDING_SIZE, nrcTaskHistory, e -> e.stream()
            .filter(task -> task.getNrcTask().getProcess().getNrcStatus().equals(NrcProcessStatus.PENDING)).count());
        metricService.createGauge(NRC_THREADQUEUE_ONGOING_SIZE, nrcTaskHistory, e -> e.stream()
            .filter(task -> task.getNrcTask().getProcess().getNrcStatus().equals(NrcProcessStatus.ONGOING)).count());
    }

    public Optional<NrcData> getNrcData(UUID id) {
        Optional<NrcData> nrcData = nrcTaskHistory.stream()
            .filter(currentNrcData -> currentNrcData.getNrcTask().getProcess().getId().equals(id))
            .findAny();
        if (nrcData.isPresent() && nrcData.get().getNrcTask().getAllNrcNeighbors() != null) {
            long neighbouringNodesCount = nrcData.get().getNrcTask().getAllNrcNeighbors().size();
            metricService.increment(NRC_FOUND_NEIGHBOURING_NODES_COUNT, neighbouringNodesCount);
            nrcData.get().getNrcTask().getAllNrcNeighbors().stream()
                .map(NrcNeighbor::getgNodeBDUs)
                .flatMap(List::stream)
                .forEach(nrcGroupingGnbdu -> metricService.increment(NRC_FOUND_NEIGHBOURING_CELLS_COUNT,
                    nrcGroupingGnbdu.getNrCellIds().size()));
        }
        return nrcData;
    }

    public Optional<NrcTask> getNrcTask(UUID id) {
        return nrcTaskHistory.stream()
            .filter(nrcData -> nrcData.getNrcTask().getProcess().getId().equals(id))
            .map(NrcData::getNrcTask)
            .findAny();
    }

    public List<NrcProcess> getNrcProcesses() {
        return nrcTaskHistory.stream()
            .map(NrcData::getNrcTask)
            .map(NrcTask::getProcess)
            .collect(Collectors.toList());
    }

    public UUID startNrc(final NrcRequest nrcRequest) {
        if (sigtermService.isSigterm()) {
            throw new RejectedExecutionException("Rejected execution exception due to the app shutdown");
        }

        metricService.stopLastTaskTimer(NRC_THREAD_QUEUE_IDLE_DURATION_SECONDS);
        metricService.clearTimedTasks();

        NrcTask nrcTask = NrcUtil.generateNrcTask(nrcRequest);
        nrcTask.getProcess().setNrcStatus(NrcProcessStatus.PENDING);
        nrcTask.getProcess().setEnmUpdateStatus(NrcProcessStatus.PENDING);
        log.info("NRC task {} was created", nrcTask.getProcess().getId());
        metricService.increment(NRC_REQUEST_SIZE, nrcRequest.geteNodeBIds().size());
        return startNrcTask(nrcTask);
    }

    public void shutdownNrc() {
        sigtermService.setSigterm(true);
    }

    public Optional<UUID> checkRequestIsInHistory(NrcRequest nrcRequest) {
        return nrcTaskHistory.stream()
            .filter(data -> pendingOrOngoingTask(nrcRequest, data.getNrcTask()))
            .map(data -> data.getNrcTask().getProcess().getId())
            .findAny();
    }

    public boolean isAllNrcTaskMonitored() {
        for (NrcData nrcData:nrcTaskHistory) {
            if (!nrcData.isQueried()) {
                return false;
            }
        }
        return true;
    }

    private boolean pendingOrOngoingTask(NrcRequest nrcRequest, NrcTask task) {
        return Objects.equals(task.getRequest(), nrcRequest) &&
            RUNNING_STATUSES.contains(task.getProcess().getNrcStatus());
    }

    private UUID startNrcTask(final NrcTask nrcTask) {
        taskExecutor.execute(() -> executeNrcTask(nrcTask));
        storeNrcTask(nrcTask);
        return nrcTask.getProcess().getId();
    }

    private void storeNrcTask(final NrcTask nrcTask) {
        metricService.startTimer(nrcTask.getProcess().getId().toString(), NRC_HISTORY_HTTP_REQUEST_DURATION_SECONDS);
        nrcTaskHistory.add(NrcData.builder()
            .nrcTask(nrcTask)
            .isQueried(false)
            .build());
        log.info("NRC task {} was stored in the history", nrcTask.getProcess().getId());
        while (!nrcTaskHistory.isEmpty() && nrcTaskHistory.size() > nrcProperties.getHistorySize()) {
            NrcTask removedNrcTask = nrcTaskHistory.poll().getNrcTask();
            if (removedNrcTask != null) {
                metricService.stopTimer(nrcTask.getProcess().getId().toString(), NRC_HISTORY_HTTP_REQUEST_DURATION_SECONDS);
            }
            log.info("NRC task {} was removed from the history",
                removedNrcTask != null ? removedNrcTask.getProcess().getId() : "No");
        }
    }

    private void executeNrcTask(final NrcTask nrcTask) {
        metricService.startTimer(nrcTask.getProcess().getId().toString(), NRC_PROCESS_HTTP_REQUEST_DURATION_SECONDS);
        try {
            nrcTask.getProcess().setNrcStatus(NrcProcessStatus.ONGOING);
            log.info("NRC task {} was started", nrcTask.getProcess().getId());
            cacheService.clear();
            ncmpCounterService.resetCounters();
            nrcTask.setAllNrcNeighbors(nrcService.startNrc(nrcTask));
            ncmpCounterService.debugCounters(nrcTask.getProcess().getId());
            nrcTask.getProcess().setNrcStatus(NrcProcessStatus.SUCCEEDED);
            metricService.increment(NRC_REQUEST_COUNT, NRC_STATUS, NrcProcessStatus.SUCCEEDED.getValue());
        } catch (Exception e) {
            log.error(String.format("NRC task %s was failed", nrcTask.getProcess().getId()), e);
            nrcTask.getProcess().setNrcStatus(NrcProcessStatus.FAILED);
            nrcTask.getProcess().setEnmUpdateStatus(NrcProcessStatus.FAILED);
            metricService.increment(NRC_REQUEST_COUNT, NRC_STATUS, NrcProcessStatus.FAILED.getValue());
        }
        metricService.stopTimer(nrcTask.getProcess().getId().toString(), NRC_PROCESS_HTTP_REQUEST_DURATION_SECONDS);
        metricService.startTimer(nrcTask.getProcess().getId().toString(), NRC_THREAD_QUEUE_IDLE_DURATION_SECONDS);
        log.info("NRC task {} was finished with {} nrcStatus and {} enmUpdateStatus", nrcTask.getProcess().getId(),
            nrcTask.getProcess().getNrcStatus(), nrcTask.getProcess().getEnmUpdateStatus());
    }
}
