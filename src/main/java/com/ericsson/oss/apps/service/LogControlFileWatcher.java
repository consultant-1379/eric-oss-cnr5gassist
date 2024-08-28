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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.LoggingConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogControlFileWatcher {

    @Setter
    @Value("${logging.logcontrol-file}")
    private String logControlFile = "";

    @Scheduled(fixedRate = 40000)
    public void reloadLogControlFile() {
        final ObjectMapper mapper = new ObjectMapper();
        final Path logControlFilePath = Path.of(logControlFile);
        if (logControlFilePath.toFile().exists()) {
            try {
                final LogControl[] logControls = mapper.readValue(logControlFilePath.toFile(), LogControl[].class);
                for (final LogControl logControl : logControls) {
                    updateLogLevel(logControl.getSeverity());
                }
            } catch (final IOException e) {
                log.error("Unable to read logControl file: " + logControlFile, e);
            }
        }
    }

    public String getLogLevel() {
        Optional<Logger> logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLoggerList().stream().findAny();
        return logger.isPresent() ? logger.get().getLevel().toString() : SupportedLogLevel.INFO.toString();
    }

    public void updateLogLevel(final String severity) {
        final SupportedLogLevel logLevel;
        try {
            logLevel = SupportedLogLevel.valueOf(severity.toUpperCase(Locale.US));
            if (logLevel.toString().equals(getLogLevel())) {
                MDC.put(FACILITY_KEY, NON_AUDIT_LOG);
                log.debug("The log level is the same as before ({}), no change needed", logLevel);
                MDC.remove(FACILITY_KEY);
                return;
            }
        } catch (IllegalArgumentException e) {
            MDC.put(FACILITY_KEY, NON_AUDIT_LOG);
            log.error("Not supported log level: {}", severity);
            MDC.remove(FACILITY_KEY);
            return;
        }

        final Level level = Level.toLevel(logLevel.name());
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (final Logger logger : loggerContext.getLoggerList()) {
            logger.setLevel(level);
        }

        MDC.put(FACILITY_KEY, NON_AUDIT_LOG);
        log.info("The log level has been changed to {}", level.toString());
        MDC.remove(FACILITY_KEY);
    }

    @Data
    static class LogControl {
        private String container;
        private String severity;
    }
}
