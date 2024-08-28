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

import com.ericsson.oss.apps.api.model.NrcGroupingGnbdu;
import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.service.CtsService;
import com.ericsson.oss.apps.service.ncmp.NeighboursPersistingService;
import com.ericsson.oss.apps.util.CtsUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class GnbduGroupingServiceTest {

    @Mock
    private NeighbouringCellService neighbouringCellService;
    @Mock
    private NeighboursPersistingService neighboursPersistingService;
    @Mock
    private CtsService ctsService;

    @InjectMocks
    private GnbduGroupingService gnbduGroupingService;

    @Test
    public void getNeighbourRelationsTest() {
        List<Long> nrCellIds = List.of(NR_CELL_111.getId(), NR_CELL_222.getId());

        NrcGroupingGnbdu expectedResult = NrcGroupingGnbdu.builder().gNodeBDUId(GNBDU.getId()).nrCellIds(
            nrCellIds).build();

        Mockito.when(neighbouringCellService.getFilteredNeighbourNrCellsWithAssoc(NRC_REQUEST, E_NODE_B)).thenReturn(
            nrCellIds.stream().map(i -> NrCell.builder().id(i).build()).collect(Collectors.toList()));
        Mockito.when(ctsService.getNrDuNode(any())).thenReturn(GNBDU);

        try (MockedStatic<CtsUtils> utils = Mockito.mockStatic(CtsUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> CtsUtils.getParentNode(any())).thenReturn(Optional.of(GNBDU));

            List<NrcGroupingGnbdu> result = gnbduGroupingService.getNeighbourRelationsAndSaveToENM(NRC_TASK_ONGOING, E_NODE_B);

            assertEquals(Collections.singletonList(expectedResult), result);
            utils.verify(() -> CtsUtils.getParentNode(any()), Mockito.times(4));
        }

        Mockito.verify(neighbouringCellService, Mockito.times(1))
                .getFilteredNeighbourNrCellsWithAssoc(Mockito.eq(NRC_REQUEST), Mockito.eq(E_NODE_B));
    }

}
