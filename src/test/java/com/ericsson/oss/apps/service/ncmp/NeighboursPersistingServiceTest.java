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
package com.ericsson.oss.apps.service.ncmp;

import com.ericsson.oss.apps.api.model.NrcProcessStatus;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.SigtermService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
public class NeighboursPersistingServiceTest {

    @Mock
    private EnmUpdateContextService enmUpdateContextService;
    @Mock
    private GUtraNetworkUpdater gUtraNetworkUpdater;
    @Mock
    private SigtermService sigtermService;
    @Mock
    private NcmpCounterService ncmpCounterService;
    @Mock
    private EnmUpdateMonitorService enmUpdateMonitorService;

    @InjectMocks
    private NeighboursPersistingService neighboursPersistingService;

    @BeforeEach
    void setUp() {
        Mockito.reset(enmUpdateContextService);
        Mockito.reset(enmUpdateMonitorService);
    }

    @Test
    public void existingGNBDUFunctionTest() {
        Mockito.when(enmUpdateContextService.getOptionalGnbduFunction(any())).thenReturn(Optional.of(toNcmpObject(GNBDU_FUNCTION)));

        neighboursPersistingService.createInEnm(NRC_TASK_ONGOING, E_NODE_B, Map.entry(GNBDU, List.of(NR_CELL_111)));

        assertEquals(NrcProcessStatus.SUCCEEDED, NRC_TASK_ONGOING.getProcess().getEnmUpdateStatus());
        Mockito.verify(enmUpdateMonitorService, Mockito.times(0)).updateFailed(any(), any(), any());
    }

    @Test
    public void notExistingGNBDUFunctionTest() {
        Mockito.when(enmUpdateContextService.getOptionalGnbduFunction(any())).thenReturn(Optional.empty());

        neighboursPersistingService.createInEnm(NRC_TASK_ONGOING_2, E_NODE_B, Map.entry(GNBDU, List.of(NR_CELL_111)));

        Mockito.verify(enmUpdateMonitorService, Mockito.times(1)).updateFailed(any(), any(), any());
    }

    @Test
    public void EnmUpdateWithSigtermTest(CapturedOutput output) {
        Mockito.when(sigtermService.isSigterm()).thenReturn(true);

        neighboursPersistingService.createInEnm(NRC_TASK_PENDING, E_NODE_B, Map.entry(GNBDU, List.of(NR_CELL_111)));
        String warnOutput = output.toString();

        assertNull(NRC_TASK_PENDING.getProcess().getEnmUpdateStatus());
        assertThat(warnOutput).contains("Enm modifications are stopped because sigterm signal was initiated");
    }
}
