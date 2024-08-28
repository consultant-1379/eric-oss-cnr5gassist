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
import com.ericsson.oss.apps.model.ncmp.GnbduFunction;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.service.ncmp.handlers.ExternalNodeHandler;
import com.ericsson.oss.apps.service.ncmp.handlers.GnbduReader;
import com.ericsson.oss.apps.service.ncmp.handlers.NrCellCUReader;
import com.ericsson.oss.apps.service.ncmp.handlers.TermPointHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EnmUpdateContextServiceTest {

    @Mock
    private GnbduReader gnbduReader;
    @Mock
    private ExternalNodeHandler externalNodeHandler;
    @Mock
    private TermPointHandler termPointHandler;
    @Mock
    private NrCellCUReader nrCellCUReader;

    @InjectMocks
    private EnmUpdateContextService enmUpdateContextService;

    @Test
    void getExternalGnbduId() {
        ExternalId actual = enmUpdateContextService.getExternalGnbduId(ENM_UPDATE_CONTEXT);
        assertEquals(ExternalId.of(GNBDU.getExternalId()), actual);
    }

    @Test
    void getOptionalGnbduFunction() {
        Mockito.when(gnbduReader.read(any())).thenReturn(Optional.of(toNcmpObject(GNBDU_FUNCTION)));
        Optional<NcmpObject<GnbduFunction>> optionalGnbduFunction = enmUpdateContextService.getOptionalGnbduFunction(ENM_UPDATE_CONTEXT);

        assertEquals(Optional.of(toNcmpObject(GNBDU_FUNCTION)), optionalGnbduFunction);
    }

    @Test
    void getGutraNetworkId() {
        ExternalId gutraNetworkId = enmUpdateContextService.getGutraNetworkId(ENM_UPDATE_CONTEXT);

        assertEquals(ExternalId.of(E_NODE_B.getExternalId()).add(ExternalNodeHandler.GUTRA_NETWORK_OBJECT), gutraNetworkId);
    }

    @Test
    void getGNodeBManagedElementExtId() {
        ExternalId gNodeBManagedElementExtId = enmUpdateContextService.getGNodeBManagedElementExtId(ENM_UPDATE_CONTEXT);

        assertEquals(ExternalId.of(GNBDU.getExternalId()).getParent(), gNodeBManagedElementExtId);
    }

    @Test
    void getNrCellCU() {
        Mockito.when(nrCellCUReader.read(any(), any())).thenReturn(Optional.of(NR_CELL_CU_PSCAPABLE_FALSE));
        Optional<NcmpObject<NrCellCU>> optionalGnbduFunction = enmUpdateContextService.getNrCellCU(NR_CELL_111, ENM_UPDATE_CONTEXT);

        assertEquals(Optional.of(NR_CELL_CU_PSCAPABLE_FALSE), optionalGnbduFunction);
    }

    @Test
    void createExternalGNodeBFunction() {
        Mockito.when(externalNodeHandler.create(any(), any(), any(), any())).thenReturn(Optional.of(toNcmpObject(MANAGED_ELEMENT_RESOURCE_IDENTIFIER, EXTERNAL_G_NODE_B_FUNCTION_ATTRIBUTES)));
        Optional<NcmpObject<NcmpAttribute>> optionalGnbduFunction = enmUpdateContextService.createExternalGNodeBFunction(GNBDU_FUNCTION, ENM_UPDATE_CONTEXT);

        assertEquals(Optional.of(toNcmpObject(MANAGED_ELEMENT_RESOURCE_IDENTIFIER, EXTERNAL_G_NODE_B_FUNCTION_ATTRIBUTES)), optionalGnbduFunction);
    }

    @Test
    void readExternalGNodeBFunction() {
        Mockito.when(externalNodeHandler.read(any(), any(), any())).thenReturn(Optional.of(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID));
        Optional<ExternalId> optionalGnbduFunction = enmUpdateContextService.readExternalGNodeBFunction(GNBDU_FUNCTION, ENM_UPDATE_CONTEXT);

        assertEquals(Optional.of(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), optionalGnbduFunction);
    }

    @Test
    void createTermPointToGNB() {
        Mockito.when(termPointHandler.create(any(), any(), any(), any())).thenReturn(Optional.of(toNcmpObject("1", TERM_POINT_TO_GNB)));
        Optional<NcmpObject<NcmpAttribute>> optionalTermPointToGNB = enmUpdateContextService.createTermPointToGNB(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT);

        assertEquals(Optional.of(toNcmpObject("1", TERM_POINT_TO_GNB)), optionalTermPointToGNB);
    }

    @Test
    void createTermPointToGNBTest() {
        Mockito.when(termPointHandler.create(any(), any(), any(), any())).thenReturn(Optional.of(toNcmpObject("1", TERM_POINT_TO_GNB)));
        Optional<NcmpObject<NcmpAttribute>> optionalTermPointToGNB = enmUpdateContextService.createTermPointToGNB(toNcmpObject("1", TERM_POINT_TO_GNB), GNBDU_FUNCTION, ENM_UPDATE_CONTEXT);

        assertEquals(Optional.of(toNcmpObject("1", TERM_POINT_TO_GNB)), optionalTermPointToGNB);
    }

    @Test
    void readTermPointToGNB() {
        Mockito.when(termPointHandler.read(any())).thenReturn(Optional.of(TERM_POINT_EXTERNAL_ID));
        Optional<ExternalId> optionalTermPointToGNB = enmUpdateContextService.readTermPointToGNB(GNODEB_MANAGED_ELEMENT_EXTERNAL_ID);

        assertEquals(Optional.of(TERM_POINT_EXTERNAL_ID), optionalTermPointToGNB);
    }
}