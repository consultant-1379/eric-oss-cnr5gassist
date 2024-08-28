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

import com.ericsson.oss.apps.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static com.ericsson.oss.apps.util.Constants.*;
import static com.ericsson.oss.apps.util.TestDefaults.MOCK_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.MOCK_UUID_VALUE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest()
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "logging.level.com.ericsson.oss.apps.service:DEBUG")
public class NcmpCounterServiceTest {

    @InjectMocks
    private NcmpCounterService ncmpCounterService = new NcmpCounterService();

    @Autowired
    private LogControlFileWatcher logControlFileWatcher;

    @BeforeEach
    public void cleanUp() {
        ncmpCounterService.resetCounters();
    }

    @Test
    void incrementCounterTest() {
        ncmpCounterService.incrementCounter(NRC_FOUND_NEIGHBOURING_NODES_COUNT);
        ncmpCounterService.incrementCounter(NRC_FOUND_NEIGHBOURING_NODES_COUNT);
        ncmpCounterService.incrementCounter(NCMP_TERMPOINTTOGNB_OBJECT_COUNT);
        assertEquals(2, ncmpCounterService.getCounterValue(NRC_FOUND_NEIGHBOURING_NODES_COUNT));
        assertEquals(1, ncmpCounterService.getCounterValue(NCMP_TERMPOINTTOGNB_OBJECT_COUNT));
    }

    @Test
    void checkCounterNameTest() {
        Boolean invalidCounterName = ncmpCounterService.checkCounterName("INVALID_COUNTER_NAME");
        Boolean validCounterName = ncmpCounterService.checkCounterName(NRC_FOUND_NEIGHBOURING_NODES_COUNT);
        assertEquals(false, invalidCounterName);
        assertEquals(true, validCounterName);
    }

    @Test
    void resetCountersTest() {
        ncmpCounterService.incrementCounter(NRC_FOUND_NEIGHBOURING_NODES_COUNT);
        assertEquals(1, ncmpCounterService.getCounterValue(NRC_FOUND_NEIGHBOURING_NODES_COUNT));
        ncmpCounterService.resetCounters();
        assertEquals(0, ncmpCounterService.getCounterValue(NRC_FOUND_NEIGHBOURING_NODES_COUNT));
    }

    @Test
    public void debugCountersTest(CapturedOutput output) {
        logControlFileWatcher.updateLogLevel(Constants.LoggingConstants.SupportedLogLevel.DEBUG.toString());
        ncmpCounterService.debugCounters(MOCK_UUID);
        String debugOutputBefore = output.toString();
        assertThat(debugOutputBefore).contains("NRC request ID : ".concat(MOCK_UUID_VALUE));
        List<String> validCounterNames = ncmpCounterService.getCounterNames();
        for (String counterName : validCounterNames) {
            assertThat(debugOutputBefore).contains(counterName.concat(" = 0"));
        }
        ncmpCounterService.incrementCounter(NCMP_EXTERNALGNODEBFUNCTIONS_OBJECT_COUNT);
        ncmpCounterService.incrementCounter(NCMP_CREATED_OBJECT_COUNT);
        ncmpCounterService.debugCounters(MOCK_UUID);
        String debugOutputAfter = output.toString();
        assertThat(debugOutputAfter).contains(NCMP_EXTERNALGNODEBFUNCTIONS_OBJECT_COUNT.concat(" = 1"));
        assertThat(debugOutputAfter).contains(NCMP_CREATED_OBJECT_COUNT.concat(" = 1"));
    }

    @Test
    public void getCounterValueTest() {
        assertEquals(0, ncmpCounterService.getCounterValue(NCMP_TERMPOINTTOGNB_OBJECT_COUNT));
        ncmpCounterService.incrementCounter(NCMP_TERMPOINTTOGNB_OBJECT_COUNT);
        assertEquals(1, ncmpCounterService.getCounterValue(NCMP_TERMPOINTTOGNB_OBJECT_COUNT));
    }

    @Test
    public void getCounterNamesTest() {
        List<String> validCounterNames = ncmpCounterService.getCounterNames();
        for (String counterName : validCounterNames) {
            assertTrue(ncmpCounterService.checkCounterName(counterName));
        }
    }
}
