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
package com.ericsson.oss.apps.service.ncmp.handlers;

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.ncmp.ExternalGUtranCell;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.PlmnId;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.NcmpService;
import com.ericsson.oss.apps.service.ncmp.EnmUpdateMonitorService;
import com.ericsson.oss.apps.service.ncmp.FdnService;
import com.ericsson.oss.apps.util.Constants;
import javassist.tools.rmi.ObjectNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP_MISSING_NEIGHBOURS_COUNT;
import static com.ericsson.oss.apps.util.Constants.TRUE;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ExternalCellHandlerTest {

    private static final NcmpObject<ExternalGUtranCell> NCMP_OBJECT = toNcmpObject("111", EXTERNAL_G_UTRAN_CELL);

    @Mock
    private NcmpService ncmpService;
    @Mock
    private MetricService metricService;
    @Mock
    private FdnService fdnService;
    @Mock
    private EnmUpdateMonitorService enmUpdateMonitorService;
    @InjectMocks
    private ExternalCellHandler externalCellHandler;
    @Mock
    private PlmnIdListGrouping plmnIdListGrouping;

    @Test
    public void readExternalCellTest() {
        Mockito.when(ncmpService.getResourcesWithOptions(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any(), eq(ExternalGUtranCell.class)))
            .thenReturn(Collections.singletonList(NCMP_OBJECT));

        Assertions.assertNotNull(NR_CELL_111.getLocalCellIdNci());
        Assertions.assertEquals(Optional.of(EXTERNAL_CELL_EXTERNAL_ID), externalCellHandler.read(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111.getLocalCellIdNci()));
    }

    @Test
    public void createExternalCellTest() throws ObjectNotFoundException {
        Mockito.when(fdnService.getGUtranSyncSignalFrequencyFdn(eq(GUTRA_NETWORK_EXTERNAL_ID), any())).thenReturn(G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF);
        Mockito.when(plmnIdListGrouping.getPlmnIdList(NR_CELL_111))
            .thenReturn(Collections.singletonList(PLMNID));
        Mockito.doNothing().when(enmUpdateMonitorService).updateSucceeded(any(), any(), any());

        Assertions.assertEquals(Optional.of(NCMP_OBJECT), externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));
        Mockito.verify(ncmpService, Mockito.times(1)).createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any());
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_OBJECT_COUNT), eq(Constants.MetricConstants.NCMP_OBJECT), eq(EXTERNALGUTRANCELL));
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_MISSING_NEIGHBOURS_COUNT));
    }

    @Test
    public void createExternalCellTestWithNullPLMNIdList() throws ObjectNotFoundException {
        Mockito.when(fdnService.getGUtranSyncSignalFrequencyFdn(eq(GUTRA_NETWORK_EXTERNAL_ID), any())).thenReturn(G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF);
        Mockito.when(plmnIdListGrouping.getPlmnIdList(NR_CELL_111))
            .thenReturn(null);
        Mockito.doNothing().when(enmUpdateMonitorService).updateSucceeded(any(), any(), any());

        NcmpObject<ExternalGUtranCell> ncmpObject = toNcmpObject(EXTERNAL_GUTRAN_CELL_ID,
            ExternalGUtranCell.builder()
                .externalGUtranCellId(EXTERNAL_GUTRAN_CELL_ID)
                .gUtranSyncSignalFrequencyRef(G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF)
                .localCellId((int) LOCAL_CELL_ID_NCI)
                .physicalLayerCellIdGroup(46)
                .physicalLayerSubCellId(0)
                .plmnIdList(null)
                .isRemoveAllowed(TRUE)
                .nRTAC(Integer.valueOf(TRACKING_AREA_CODE).toString())
                .build());

        Assertions.assertEquals(Optional.of(ncmpObject), externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));
        Mockito.verify(ncmpService, Mockito.times(1)).createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any());
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_OBJECT_COUNT), eq(Constants.MetricConstants.NCMP_OBJECT), eq(EXTERNALGUTRANCELL));
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_MISSING_NEIGHBOURS_COUNT));
    }

    @Test
    public void createExternalCellNoSectorCarrierTest() throws ObjectNotFoundException {
        Mockito.doNothing().when(enmUpdateMonitorService).updateFailed(any(), any(), any(), any());

        NrCell noSectorCarrier = NrCell.builder()
            .id(1L)
            .localCellIdNci(LOCAL_CELL_ID_NCI)
            .externalId(NR_CELL_EXT_ID.toString()).build();
        Optional<NcmpObject<NcmpAttribute>> result = externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, noSectorCarrier, ENM_UPDATE_CONTEXT);

        Assertions.assertTrue(result.isEmpty());
    }


    @Test
    public void createExternalCellFailed() throws ObjectNotFoundException, RestClientException {
        Mockito.when(fdnService.getGUtranSyncSignalFrequencyFdn(eq(GUTRA_NETWORK_EXTERNAL_ID), any())).thenThrow(ObjectNotFoundException.class);
        Mockito.doNothing().when(enmUpdateMonitorService).updateFailed(any(), any(), any(), any());
        Assertions.assertEquals(Optional.empty(), externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));
        assertDoesNotThrow(() -> externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));

        Mockito.when(fdnService.getGUtranSyncSignalFrequencyFdn(eq(GUTRA_NETWORK_EXTERNAL_ID), any())).thenThrow(RestClientException.class);
        Assertions.assertEquals(Optional.empty(), externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));
        assertDoesNotThrow(() -> externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));

        Mockito.when(fdnService.getGUtranSyncSignalFrequencyFdn(eq(GUTRA_NETWORK_EXTERNAL_ID), any())).thenThrow(IllegalArgumentException.class);
        assertThrows(IllegalArgumentException.class, () -> externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT));
    }

    @Test
    public void createExternalCellWithMultiPLMNIdList() throws ObjectNotFoundException {
        List<PlmnId> plmnIdList = List.of(PlmnId.builder().mncLength(2).mcc(216).mnc(30).build(),
            PlmnId.builder().mcc(44).mnc(888).build(),
            PlmnId.builder().mcc(77).mnc(141).build());

        Mockito.when(plmnIdListGrouping.getPlmnIdList(any()))
            .thenReturn(plmnIdList);
        Mockito.doNothing().when(enmUpdateMonitorService).updateSucceeded(any(), any(), any());

        NcmpAttribute attributes = externalCellHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111, ENM_UPDATE_CONTEXT)
            .stream().findFirst().get().getAttributes();

        ExternalGUtranCell externalGUtranCell = ExternalGUtranCell.builder()
            .externalGUtranCellId("111")
            .localCellId(25)
            .physicalLayerCellIdGroup(46)
            .plmnIdList(plmnIdList)
            .nRTAC("10496")
            .physicalLayerSubCellId(0)
            .isRemoveAllowed("true")
            .build();

        Assertions.assertEquals(externalGUtranCell, attributes);
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_OBJECT_COUNT), eq(Constants.MetricConstants.NCMP_OBJECT), eq(EXTERNALGUTRANCELL));
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_MISSING_NEIGHBOURS_COUNT));
    }
}
