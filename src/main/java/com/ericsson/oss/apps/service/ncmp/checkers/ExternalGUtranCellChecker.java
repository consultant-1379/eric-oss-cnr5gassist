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

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.client.cts.model.NrSectorCarrier;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.model.ncmp.SectorCarrier;
import com.ericsson.oss.apps.service.ncmp.handlers.NrSectorCarrierReader;
import com.ericsson.oss.apps.util.CtsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalGUtranCellChecker {

    public static final String CREATE_EXTERNAL_GUTRAN_CELL = "CreateExternalGUtranCell";
    private final NrSectorCarrierReader nrSectorCarrierReader;

    public boolean pSCellCUIsNotFalse(NrCellCU nrCellCU) {
        return nrCellCU.getPSCellCapable() == null || nrCellCU.getPSCellCapable();
    }

    public boolean isEssConfiguredCell(NrCell nrCell) {
        Optional<NrSectorCarrier> nrSectorCarrierInCts = CtsUtils.getNrSectorCarrier(nrCell);
        if (nrSectorCarrierInCts.isPresent()) {
            return isNrSectorCarrierInNCMP(nrSectorCarrierInCts.get().getExternalId());
        }
        log.warn("No NrSectorCarrier was found in CTS for NrCell: {}", nrCell.getExternalId());
        return false;
    }

    private boolean isNrSectorCarrierInNCMP(String nrSectorCarrierExternalId) {
        Optional<NcmpObject<SectorCarrier>> nrSectorCarrierInNCMP = nrSectorCarrierReader.read(ExternalId.of(nrSectorCarrierExternalId));
        if (nrSectorCarrierInNCMP.isPresent()) {
            return isEssScPairIdNotNull(nrSectorCarrierInNCMP.get().getAttributes());
        }
        log.warn("No NrSectorCarrier was found in NCMP for CTS object: {}", nrSectorCarrierExternalId);
        return false;
    }

    private boolean isEssScPairIdNotNull(SectorCarrier nrSectorCarrierExternalId) {
        return nrSectorCarrierExternalId.getEssScPairId() != null;
    }
}
