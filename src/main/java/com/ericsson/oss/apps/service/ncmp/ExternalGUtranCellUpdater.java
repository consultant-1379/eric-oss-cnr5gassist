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
import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.ncmp.checkers.ExternalGUtranCellChecker;
import com.ericsson.oss.apps.service.ncmp.checkers.NrCellChecker;
import com.ericsson.oss.apps.service.ncmp.handlers.ExternalCellHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.NRC_FOUND_NEIGHBOURING_CELLS_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalGUtranCellUpdater {

    public static final String CREATE_EXTERNAL_GUTRAN_CELL = "CreateExternalGUtranCell";
    private final ExternalCellHandler externalCellHandler;
    private final EnmUpdateMonitorService enmUpdateMonitorService;
    private final EnmUpdateContextService enmUpdateContextService;
    private final NrCellChecker nrCellChecker;
    private final ExternalGUtranCellChecker externalGUtranCellChecker;
    private final NcmpCounterService ncmpCounterService;

    public List<Map.Entry<NrCell, Optional<NcmpObject<NrCellCU>>>> update(ExternalId externalGNodeBFunctionId, List<NrCell> nrCells, EnmUpdateContext enmUpdateContext) {
        List<NrCell> preFilteredNrCells = nrCells.stream().filter(cell -> filterOutExistNrCell(externalGNodeBFunctionId, cell)).collect(Collectors.toList());
        ncmpCounterService.incrementCounter(NRC_FOUND_NEIGHBOURING_CELLS_COUNT);

        List<Map.Entry<NrCell, Optional<NcmpObject<NrCellCU>>>> nrCellEntry = preFilteredNrCells.stream().collect(Collectors
                .toMap(cell -> cell, nrCell -> enmUpdateContextService.getNrCellCU(nrCell, enmUpdateContext))).entrySet().stream()
            .filter(entry -> externalGUtranCellShouldBeCreated(entry.getKey(), entry.getValue(), enmUpdateContext)).collect(Collectors.toList());
        nrCellEntry.stream().forEach(entry -> externalCellHandler.create(externalGNodeBFunctionId, entry.getKey(), enmUpdateContext));
        return nrCellEntry;
    }

    private boolean filterOutExistNrCell(ExternalId externalGNodeBFunctionId, NrCell cell) {
        return !(nrCellChecker.withLocalCellIdValue(externalGNodeBFunctionId, cell)
            || nrCellChecker.withLocalCellIdNull(cell));
    }

    private boolean externalGUtranCellShouldBeCreated(NrCell nrCell, Optional<NcmpObject<NrCellCU>> nrCellCU, EnmUpdateContext enmUpdateContext) {
        if (nrCellCU.isPresent()) {
            if (!(externalGUtranCellChecker.pSCellCUIsNotFalse(nrCellCU.get().getAttributes()) || externalGUtranCellChecker.isEssConfiguredCell(nrCell))) {
                enmUpdateMonitorService.updateFailed(enmUpdateContext, CREATE_EXTERNAL_GUTRAN_CELL, nrCell,
                    "psCellCapable is false and NrSectorCarrier was not found");
                return false;
            }
            return true;
        }
        String errorMessage = String.format("No NrCellCU was found with cellLocalId set to %h for the cell: %h", nrCell.getLocalCellIdNci(), nrCell.getExternalId());
        log.warn(errorMessage);
        enmUpdateMonitorService.updateFailed(enmUpdateContext, CREATE_EXTERNAL_GUTRAN_CELL, nrCell, errorMessage);
        return false;
    }
}
