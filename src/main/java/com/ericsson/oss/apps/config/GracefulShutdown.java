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

import com.ericsson.oss.apps.api.model.NrcProcess;
import com.ericsson.oss.apps.api.model.NrcProcessStatus;
import com.ericsson.oss.apps.manager.NrcManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.ericsson.oss.apps.util.Constants.LoggingConstants.*;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SERVICE_PREFIX;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GracefulShutdown {

    @Autowired
    private final NrcManager nrcManager;

    @PreDestroy
    public void nrcGracefulShutdown() {
        MDC.put(FACILITY_KEY, AUDIT_LOG);
        MDC.put(SUBJECT_KEY, SERVICE_PREFIX.toUpperCase(Locale.US));
        log.info("Graceful shutdown initiated...");
        nrcManager.shutdownNrc();
        log.info("Graceful shutdown - NCR requests are stopped");
        deferredResult();
        log.info("Graceful shutdown is processed successfully");
        MDC.remove(FACILITY_KEY);
        MDC.remove(SUBJECT_KEY);
    }

    DeferredResult<String> deferredResult() {
        final int monitoringTimeOutSec = 30;
        final long timeOutInMilliSec = 10000L;
        String timeOutResp = "Time Out.";
        DeferredResult<String> deferredResult = new DeferredResult<>(timeOutInMilliSec, timeOutResp);
        CompletableFuture.runAsync(()->{
            boolean nrcProcessFlag = true;
            int monitoringTimeCounter = 1;
            while (nrcProcessFlag || monitoringTimeCounter < monitoringTimeOutSec || !nrcManager.getNrcProcesses().isEmpty()) {
                nrcProcessFlag = false;
                nrcProcessFlag = isNrcProcessOngoing(nrcManager.getNrcProcesses());
                if (nrcManager.isAllNrcTaskMonitored()) {
                    monitoringTimeCounter = monitoringTimeOutSec;
                } else {
                    monitoringTimeCounter++;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            deferredResult.setResult("Task Finished");
        });
        return deferredResult;
    }

    boolean isNrcProcessOngoing(List<NrcProcess> nrcProcess) {
        for (NrcProcess np : nrcProcess) {
            if (np.getEnmUpdateStatus() == NrcProcessStatus.ONGOING) {
                return true;
            }
        }
        return false;
    }
}
