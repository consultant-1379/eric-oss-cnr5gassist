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
package com.ericsson.oss.apps.service.nrc;

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.PciConflict;
import jakarta.validation.Valid;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

@UtilityClass
public class PciConflictDetector {

    public static Map<PciConflict, List<Long>> detectPCIConflicts(@Valid List<NrCell> neighbourNrCells) {
        return neighbourNrCells.stream().collect(
            collectingAndThen(
                groupingBy(
                    PciConflictDetector::toPciConflict,
                    mapping(NrCell::getId, toList())),
                PciConflictDetector::keepConflicts));
    }

    private static Map<PciConflict, List<Long>> keepConflicts(Map<PciConflict, List<Long>> pciConflictListMap) {
        pciConflictListMap.values().removeIf(elem -> elem.size() <= 1);
        return pciConflictListMap;
    }

    private static PciConflict toPciConflict(NrCell nrCell) {
        return new PciConflict(nrCell.getPhysicalCellIdentity(), nrCell.getDownlinkEARFCN());
    }
}
