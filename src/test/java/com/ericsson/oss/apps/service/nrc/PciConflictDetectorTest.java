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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PciConflictDetectorTest {

    @Test
    public void detectPCIConflicts(){
        NrCell conflictFirst = NrCell.builder().id(1L).physicalCellIdentity(138).downlinkEARFCN(17020).build();
        NrCell conflictSecond = NrCell.builder().id(2L).physicalCellIdentity(138).downlinkEARFCN(17020).build();
        NrCell conflictThird = NrCell.builder().id(3L).physicalCellIdentity(139).downlinkEARFCN(16020).build();
        NrCell conflictFourth = NrCell.builder().id(4L).physicalCellIdentity(139).downlinkEARFCN(16020).build();
        NrCell willBeFilteredBecauseOfNull = NrCell.builder().id(5L).physicalCellIdentity(139).downlinkEARFCN(null).build();
        NrCell willBeFilteredBecauseOfNotConflict = NrCell.builder().id(6L).physicalCellIdentity(140).downlinkEARFCN(17050).build();

        Map<PciConflict, List<Long>> expectedResult = Map.of(
                PciConflict.builder()
                        .physicalCellIdentity(conflictFirst.getPhysicalCellIdentity())
                        .downlinkEARFCN(conflictFirst.getDownlinkEARFCN())
                        .build(),
                List.of(conflictFirst.getId(), conflictSecond.getId()),
                PciConflict.builder()
                        .physicalCellIdentity(conflictThird.getPhysicalCellIdentity())
                        .downlinkEARFCN(conflictThird.getDownlinkEARFCN()).build(),
                List.of(conflictThird.getId(), conflictFourth.getId())
        );
        List<NrCell> neighbourNrCells = List.of(conflictFirst, conflictSecond, conflictThird, conflictFourth,
                willBeFilteredBecauseOfNull, willBeFilteredBecauseOfNotConflict);

        Map<PciConflict, List<Long>> result = PciConflictDetector.detectPCIConflicts(neighbourNrCells);

        assertEquals(expectedResult, result);
    }
}
