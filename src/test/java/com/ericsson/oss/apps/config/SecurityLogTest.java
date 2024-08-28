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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ericsson.oss.apps.client.ApiClient;
import com.ericsson.oss.apps.manager.NrcManager;
import com.ericsson.oss.apps.service.LogControlFileWatcher;
import com.ericsson.oss.apps.util.Constants;
import javassist.tools.rmi.ObjectNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import static com.ericsson.oss.apps.util.Constants.LoggingConstants.*;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.SERVICE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
public class SecurityLogTest {

    private static final String INVALID = "INVALID";

    private final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint1;
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint2;
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint3;
    @Mock
    ResponseEntity result;
    @Mock
    private RetryTemplate retryTemplate;
    @Mock
    private ApiClient apiClient;
    @Mock
    private NrcManager nrcManager;
    @InjectMocks
    LogControlFileWatcher logControlFileWatcher;
    @InjectMocks
    private ClientAspects clientAspects;
    @InjectMocks
    GracefulShutdown gracefulShutdown;

    @BeforeEach
    void setup() {
        logger.addAppender(listAppender);
        listAppender.start();
    }

    @AfterEach
    void cleanup() {
        listAppender.stop();
        listAppender.list.clear();
        logger.detachAppender(listAppender);
    }

    @Test
    void changeLogLevel() throws ObjectNotFoundException {
        logControlFileWatcher.updateLogLevel(Constants.LoggingConstants.SupportedLogLevel.INFO.toString());
        assertEquals(Constants.LoggingConstants.SupportedLogLevel.INFO.toString(), logControlFileWatcher.getLogLevel());
        logControlFileWatcher.updateLogLevel(Constants.LoggingConstants.SupportedLogLevel.DEBUG.toString());
        assertEquals(Constants.LoggingConstants.SupportedLogLevel.DEBUG.toString(), logControlFileWatcher.getLogLevel());
        logControlFileWatcher.updateLogLevel(Constants.LoggingConstants.SupportedLogLevel.DEBUG.toString());
        assertEquals(Constants.LoggingConstants.SupportedLogLevel.DEBUG.toString(), logControlFileWatcher.getLogLevel());
        logControlFileWatcher.updateLogLevel(INVALID);
        assertEquals(Constants.LoggingConstants.SupportedLogLevel.DEBUG.toString(), logControlFileWatcher.getLogLevel());

        assertTrue(listAppender.list.stream()
            .filter(entry -> entry.getMessage().contains("log level")).count() > 0);

        listAppender.list.stream()
            .filter(entry ->
                entry.getMessage().startsWith("The log level has been changed to") ||
                    entry.getMessage().startsWith("The log level is the same as before") ||
                    entry.getMessage().startsWith("Not supported log level"))
            .forEach(entry -> assertEquals(NON_AUDIT_LOG, entry.getMDCPropertyMap().get(FACILITY_KEY)));
    }

    @Test
    void clientAspects() throws Throwable {
        Mockito.when(apiClient.getBasePath()).thenReturn("");
        Mockito.when(retryTemplate.execute(Mockito.any())).thenAnswer(i -> {
            RetryCallback<Runnable, Throwable> retryCallback = i.getArgument(0);
            return retryCallback.doWithRetry(Mockito.mock(RetryContext.class));
        });

        // CTS request
        Mockito.when(proceedingJoinPoint1.getArgs()).thenReturn(new Object[] {
                "/cts/oss-core-ws/rest/ctw/enodeb/{id}", "", "", "", "", "", ""});
        Mockito.when(proceedingJoinPoint1.proceed()).thenReturn("");
        Mockito.when(proceedingJoinPoint1.getTarget()).thenReturn(apiClient);
        clientAspects.handleRestTemplateRetry(proceedingJoinPoint1);

        // API Gateway session ID request
        Mockito.when(proceedingJoinPoint2.getArgs()).thenReturn(new Object[] {
            "/auth/v1/login", "", "", "", "", "", ""});
        Mockito.when(proceedingJoinPoint2.proceed()).thenReturn("");
        Mockito.when(proceedingJoinPoint2.getTarget()).thenReturn(apiClient);
        clientAspects.handleRestTemplateRetry(proceedingJoinPoint2);

        // API Gateway session ID request response
        Mockito.when(proceedingJoinPoint3.getArgs()).thenReturn(new Object[] {
                "/auth/v1/login", "", "", "", "", "", ""});
        Mockito.when(proceedingJoinPoint3.getTarget()).thenReturn(apiClient);
        Mockito.when(result.getStatusCode()).thenReturn(HttpStatus.OK);
        clientAspects.logResult(proceedingJoinPoint3, result);

        var apiClientLogs = listAppender.list.stream()
            .filter(entry -> entry.getMessage().startsWith("apiClient.invokeAPI"));

        var secureLogs = listAppender.list.stream()
            .filter(entry -> entry.getMessage().startsWith("apiClient.invokeAPI"))
            .filter(entry -> entry.getMDCPropertyMap().containsKey(FACILITY_KEY) && entry.getMDCPropertyMap().get(FACILITY_KEY).equals(AUDIT_LOG))
            .filter(entry -> entry.getMDCPropertyMap().containsKey(SUBJECT_KEY) && entry.getMDCPropertyMap().get(SUBJECT_KEY).equals(SERVICE_PREFIX.toUpperCase()));

        var secureLogsWithResponse = listAppender.list.stream()
                .filter(entry -> entry.getMessage().startsWith("apiClient.invokeAPI"))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(FACILITY_KEY) && entry.getMDCPropertyMap().get(FACILITY_KEY).equals(AUDIT_LOG))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(SUBJECT_KEY) && entry.getMDCPropertyMap().get(SUBJECT_KEY).equals(SERVICE_PREFIX.toUpperCase()))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(RESP_MESSAGE_KEY))
                .filter(entry -> entry.getMDCPropertyMap().containsKey(RESP_CODE_KEY) && entry.getMDCPropertyMap().get(RESP_CODE_KEY).equals(HttpStatus.OK.toString()));

        assertEquals(3, apiClientLogs.count());
        assertEquals(2, secureLogs.count());
        assertEquals(1, secureLogsWithResponse.count());
    }

    @Test
    void gracefulShutdown() {
        gracefulShutdown.nrcGracefulShutdown();

        assertTrue(listAppender.list.stream()
            .filter(entry -> entry.getMessage().startsWith("Graceful shutdown")).count() > 0);

        listAppender.list.stream()
            .filter(entry -> entry.getMessage().startsWith("Graceful shutdown"))
            .forEach(entry -> {
                assertEquals(AUDIT_LOG, entry.getMDCPropertyMap().get(FACILITY_KEY));
                assertEquals(SERVICE_PREFIX.toUpperCase(), entry.getMDCPropertyMap().get(SUBJECT_KEY));
            });
    }
}
