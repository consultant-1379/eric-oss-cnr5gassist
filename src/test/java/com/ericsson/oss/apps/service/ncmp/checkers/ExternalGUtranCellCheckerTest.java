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

package com.ericsson.oss.apps.service.ncmp.checkers;

import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.model.ncmp.SectorCarrier;
import com.ericsson.oss.apps.service.ncmp.handlers.NrSectorCarrierReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.data.jpa.domain.AbstractPersistable_.ID;

@ExtendWith(MockitoExtension.class)
class ExternalGUtranCellCheckerTest {

    private static final NcmpObject<NrCellCU> NR_CELL_CU_PSCAPABLE_FALSE =
        NcmpObject.<NrCellCU>builder().id(ID).attributes(
            NrCellCU.builder().nRCellCUId(ID).pSCellCapable(false).build()).build();
    private static final NcmpObject<NrCellCU> NR_CELL_CU_PSCAPABLE_TRUE =
        NcmpObject.<NrCellCU>builder().id(ID).attributes(
            NrCellCU.builder().nRCellCUId(ID).pSCellCapable(true).build()).build();
    private static final NcmpObject<NrCellCU> NR_CELL_CU_PSCAPABLE_NULL =
        NcmpObject.<NrCellCU>builder().id(ID).attributes(
            NrCellCU.builder().nRCellCUId(ID).build()).build();
    private static final int ESS_PAIR_ID = 1;
    private static final SectorCarrier sectorCarrier = SectorCarrier.builder().nRSectorCarrierId(ID).essScPairId(ESS_PAIR_ID).build();
    private static final SectorCarrier sectorCarrierNull = SectorCarrier.builder().nRSectorCarrierId(ID).build();

    @Mock
    private NrSectorCarrierReader nrSectorCarrierReader;
    @InjectMocks
    private ExternalGUtranCellChecker externalGUtranCellChecker;

    @Test
    void pSCellCUIsNotFalse() {
        boolean pSCellCUIsNotFalse1 = externalGUtranCellChecker.pSCellCUIsNotFalse(NR_CELL_CU_PSCAPABLE_FALSE.getAttributes());
        boolean pSCellCUIsNotFalse2 = externalGUtranCellChecker.pSCellCUIsNotFalse(NR_CELL_CU_PSCAPABLE_NULL.getAttributes());
        boolean pSCellCUIsNotFalse3 = externalGUtranCellChecker.pSCellCUIsNotFalse(NR_CELL_CU_PSCAPABLE_TRUE.getAttributes());

        assertFalse(pSCellCUIsNotFalse1);
        assertTrue(pSCellCUIsNotFalse2);
        assertTrue(pSCellCUIsNotFalse3);
    }

    @Test
    void isEssConfiguredCellNotExistInCtsTest() {
        boolean isEssConfiguredCell2 = externalGUtranCellChecker.isEssConfiguredCell(NR_CELL_222);

        assertFalse(isEssConfiguredCell2);
    }

    @Test
    void isEssConfiguredCellNotExistInNCMPTest() {
        Mockito.when(nrSectorCarrierReader.read(any())).thenReturn(Optional.empty());
        boolean notFoundNrSectorCarrierInNCMP1 = externalGUtranCellChecker.isEssConfiguredCell(NR_CELL_111);

        assertFalse(notFoundNrSectorCarrierInNCMP1);
    }

    @Test
    void isEssConfiguredCellScPairIdNotNullTest() {
        Mockito.when(nrSectorCarrierReader.read(any())).thenReturn(Optional.of(toNcmpObject(sectorCarrierNull)));
        boolean isEssScPairIdNotNull1 = externalGUtranCellChecker.isEssConfiguredCell(NR_CELL_111);
        Mockito.when(nrSectorCarrierReader.read(any())).thenReturn(Optional.of(toNcmpObject(sectorCarrier)));
        boolean isEssScPairIdNotNull2 = externalGUtranCellChecker.isEssConfiguredCell(NR_CELL_111);

        assertFalse(isEssScPairIdNotNull1);
        assertTrue(isEssScPairIdNotNull2);
    }
}