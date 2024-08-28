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

import javassist.tools.rmi.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.ericsson.oss.apps.util.Constants.LoggingConstants.SupportedLogLevel;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class LogControlFileWatcherTest {

    private static final String INVALID = "INVALID";

    private LogControlFileWatcher logControlFileWatcher;

    @BeforeEach
    void init() {
        logControlFileWatcher = new LogControlFileWatcher();
    }

    private void initLogControlFile(final String severity) throws IOException {
        final File tempFile = File.createTempFile("logcontrol", ".tmp");
        final String logControlFileContent =
            "[{\"container\": \"eric-oss-cnr5gassist\",\"severity\": \"" + severity + "\"}]";

        Files.write(tempFile.toPath(), logControlFileContent.getBytes(StandardCharsets.UTF_8));
        logControlFileWatcher.setLogControlFile(tempFile.getAbsolutePath());
    }

    @Test
    void whenValidLogControlFileChange_shouldLoggingLevelChange() throws IOException, ObjectNotFoundException {
        logControlFileWatcher.updateLogLevel(SupportedLogLevel.INFO.toString());
        assertEquals(SupportedLogLevel.INFO.toString(), logControlFileWatcher.getLogLevel());

        initLogControlFile(SupportedLogLevel.DEBUG.toString());
        assertEquals(SupportedLogLevel.INFO.toString(), logControlFileWatcher.getLogLevel());

        logControlFileWatcher.reloadLogControlFile();
        assertEquals(SupportedLogLevel.DEBUG.toString(), logControlFileWatcher.getLogLevel());
    }

    @Test
    void whenInvalidLogControlFileChange_shouldLoggingLevelNotChange() throws IOException, ObjectNotFoundException {
        logControlFileWatcher.updateLogLevel(SupportedLogLevel.INFO.toString());
        assertEquals(SupportedLogLevel.INFO.toString(), logControlFileWatcher.getLogLevel());

        initLogControlFile(INVALID);
        assertEquals(SupportedLogLevel.INFO.toString(), logControlFileWatcher.getLogLevel());

        logControlFileWatcher.reloadLogControlFile();
        assertEquals(SupportedLogLevel.INFO.toString(), logControlFileWatcher.getLogLevel());
    }
}

