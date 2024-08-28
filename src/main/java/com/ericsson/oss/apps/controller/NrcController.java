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

import com.ericsson.oss.apps.api.NrcApi;
import com.ericsson.oss.apps.api.model.NrcProcess;
import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.manager.NrcManager;
import com.ericsson.oss.apps.service.MetricService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.TASK_QUEUE_IS_FULL;


@RestController
@AllArgsConstructor
@Slf4j
public class NrcController implements NrcApi {

    private final NrcManager nrcManager;
    private final MetricService metricService;

    @Override
    public ResponseEntity<UUID> startNrc(@RequestBody NrcRequest nrcRequest) {
        log.info("/startNrc REST interface is invoked with NRC request {}", nrcRequest);
        try {
            return nrcManager.checkRequestIsInHistory(nrcRequest)
                .map(existingUuid -> {
                    metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, START_NRC, METHOD, POST,
                        HTTP_STATUS, String.valueOf(HttpStatus.ALREADY_REPORTED.value()));
                    return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body(existingUuid);
                })
                .orElseGet(() -> {
                    metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, START_NRC, METHOD, POST,
                        HTTP_STATUS, String.valueOf(HttpStatus.OK.value()));
                    return ResponseEntity.status(HttpStatus.OK).body(nrcManager.startNrc(nrcRequest));
                });
        } catch (RejectedExecutionException e) {
            metricService.increment(NRC_THREAD_QUEUE_FULL_COUNT);
            metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, START_NRC, METHOD, POST,
                HTTP_STATUS, String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()), CAUSE, CONGESTION);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, TASK_QUEUE_IS_FULL, e);
        }
    }

    @Override
    public ResponseEntity<List<NrcProcess>> monitoring() {
        log.info("/monitoring REST interface is invoked");
        metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, MONITORING, METHOD, GET,
            HTTP_STATUS, String.valueOf(HttpStatus.OK.value()));
        return ResponseEntity.status(HttpStatus.OK).body(nrcManager.getNrcProcesses());
    }

    @Override
    public ResponseEntity<NrcTask> monitoringById(UUID uuid) {
        log.info("/monitoring/{} REST interface is invoked", uuid);
        return nrcManager.getNrcData(uuid)
                .map(nrcData -> {
                    nrcData.setQueried(true);
                    return nrcData;
                })
                .map(nrcData -> {
                    metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, MONITORING_BY_ID, METHOD, GET,
                        HTTP_STATUS, String.valueOf(HttpStatus.OK.value()));
                    return ResponseEntity.status(HttpStatus.OK).body(nrcData.getNrcTask());
                })
            .orElseGet(() -> {
                metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, MONITORING_BY_ID, METHOD, GET,
                    HTTP_STATUS, String.valueOf(HttpStatus.NOT_FOUND.value()));
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            });
    }
}
