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

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.ncmp.checkers.ExternalGUtranCellChecker;
import com.ericsson.oss.apps.service.ncmp.checkers.NrCellChecker;
import com.ericsson.oss.apps.service.ncmp.handlers.ExternalCellHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ExternalGUtranCellUpdaterTest {

    @Mock
    private ExternalCellHandler externalCellHandler;
    @Mock
    private EnmUpdateMonitorService enmUpdateMonitorService;
    @Mock
    private EnmUpdateContextService enmUpdateContextService;
    @Mock
    private NrCellChecker nrCellChecker;
    @Mock
    private ExternalGUtranCellChecker externalGUtranCellChecker;
    @Mock
    private NcmpCounterService ncmpCounterService;
    @InjectMocks
    private ExternalGUtranCellUpdater externalGUtranCellUpdater;

    @Test
    public void existNrCellTest() {
        Mockito.when(nrCellChecker.withLocalCellIdValue(any(), any())).thenReturn(true);
        List<Map.Entry<NrCell, Optional<NcmpObject<NrCellCU>>>> entryStream = externalGUtranCellUpdater.update(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, List.of(NR_CELL_111), ENM_UPDATE_CONTEXT);
        assertEquals(0, entryStream.size());
    }

    @Test
    public void pSCellCUIsNotFalseTest() {
        Mockito.when(nrCellChecker.withLocalCellIdValue(any(), any())).thenReturn(false);
        Mockito.when(nrCellChecker.withLocalCellIdNull(any())).thenReturn(false);
        Mockito.when(externalGUtranCellChecker.pSCellCUIsNotFalse(any())).thenReturn(true);
        Mockito.when(enmUpdateContextService.getNrCellCU(any(), any())).thenReturn(Optional.ofNullable(NR_CELL_CU_PSCAPABLE_FALSE));

        List<Map.Entry<NrCell, Optional<NcmpObject<NrCellCU>>>> entryStream = externalGUtranCellUpdater.update(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, List.of(NR_CELL_111), ENM_UPDATE_CONTEXT);

        assertEquals(1, entryStream.size());
        Mockito.verify(enmUpdateMonitorService, Mockito.times(0)).updateFailed(any(), any(), any());
    }

    @Test
    public void externalGUtranCellShouldNotBeCreatedTest() {
        Mockito.when(externalGUtranCellChecker.pSCellCUIsNotFalse(any())).thenReturn(false);

        Boolean result = ReflectionTestUtils.invokeMethod(externalGUtranCellUpdater,
            "externalGUtranCellShouldBeCreated", NR_CELL_111, Optional.of(NR_CELL_CU_PSCAPABLE_FALSE), ENM_UPDATE_CONTEXT);

        assertEquals(false, result);
    }

    @Test
    public void nrCellCUEmptyTest() {
        Boolean result = ReflectionTestUtils.invokeMethod(externalGUtranCellUpdater,
            "externalGUtranCellShouldBeCreated", NR_CELL_111, Optional.empty(), ENM_UPDATE_CONTEXT);

        assertEquals(false, result);
    }
}