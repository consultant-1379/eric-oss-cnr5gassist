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

import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.service.NcmpCounterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GUtraNetworkUpdaterTest {

    @Mock
    private ExternalGUtranCellUpdater externalGUtranCellUpdater;
    @Mock
    private NcmpCounterService ncmpCounterService;
    @Mock
    private EnmUpdateContextService enmUpdateContextService;

    @InjectMocks
    private GUtraNetworkUpdater GUtraNetworkUpdater;

    @Test
    public void existingTermPointToGNBTest() {
        Mockito.when(enmUpdateContextService.readExternalGNodeBFunction(any(), any())).thenReturn(Optional.of(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID));
        Mockito.when(enmUpdateContextService.readTermPointToGNB(any())).thenReturn(Optional.of(TERM_POINT_EXTERNAL_ID));

        GUtraNetworkUpdater.update(List.of(NR_CELL_111), GNBDU_FUNCTION_PLMNID_1, ENM_UPDATE_CONTEXT);

        Mockito.verify(externalGUtranCellUpdater, Mockito.times(1)).update(any(), any(), any());
    }

    @Test
    public void notExistingTermPointToGNBTest() {
        Mockito.when(enmUpdateContextService.readExternalGNodeBFunction(any(), any())).thenReturn(Optional.of(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID));
        Mockito.when(enmUpdateContextService.readTermPointToGNB(any())).thenReturn(Optional.empty());

        GUtraNetworkUpdater.update(List.of(NR_CELL_111), GNBDU_FUNCTION_PLMNID_1, ENM_UPDATE_CONTEXT);

        Mockito.verify(externalGUtranCellUpdater, Mockito.times(0)).update(any(), any(), any());
        Mockito.verify(enmUpdateContextService, Mockito.times(1)).createTermPointToGNB((ExternalId) any(), any(), any());
    }

    @Test
    public void notExistingExternalGn() {
        Mockito.when(enmUpdateContextService.readExternalGNodeBFunction(any(), any())).thenReturn(Optional.empty());
        Mockito.when(enmUpdateContextService.createExternalGNodeBFunction(any(), any())).thenReturn(Optional.of(toNcmpObject(EXTERNAL_G_NODE_B_FUNCTION_ATTRIBUTES)));

        GUtraNetworkUpdater.update(List.of(NR_CELL_111), GNBDU_FUNCTION_PLMNID_1, ENM_UPDATE_CONTEXT);

        Mockito.verify(enmUpdateContextService, Mockito.times(1)).createExternalGNodeBFunction(any(), any());
        Mockito.verify(enmUpdateContextService, Mockito.times(1)).createTermPointToGNB((NcmpObject<NcmpAttribute>) any(), any(), any());
    }
}