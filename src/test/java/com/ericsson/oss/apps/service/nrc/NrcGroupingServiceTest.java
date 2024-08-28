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
import com.ericsson.oss.apps.api.model.NrcNeighbor;
import com.ericsson.oss.apps.exception.GraniteFaultException;
import com.ericsson.oss.apps.service.CtsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class NrcGroupingServiceTest {
    @Mock
    private CtsService ctsService;
    @Mock
    private GnbduGroupingService gnbduGroupingService;

    @InjectMocks
    private NrcNeighboringService nrcNeighboringService;

    @Test
    public void getNrcGroupingENodeBTest() {
        long GNBDU_ID = 11L;

        List<NrcGroupingGnbdu> nrcGroupingGnbdus = Collections.singletonList(NrcGroupingGnbdu.builder().gNodeBDUId(GNBDU_ID).build());

        Mockito.when(ctsService.getLteNodeWithCellsAssoc(ENODEB_ID)).thenReturn(E_NODE_B);
        Mockito.when(gnbduGroupingService.getNeighbourRelationsAndSaveToENM(NRC_TASK_ONGOING, E_NODE_B)).thenReturn(nrcGroupingGnbdus);

        Stream<NrcNeighbor> result = nrcNeighboringService.getNrcNeighbor(NRC_TASK_ONGOING, ENODEB_ID);

        Mockito.verify(ctsService, Mockito.times(1))
                .getLteNodeWithCellsAssoc(Mockito.eq(ENODEB_ID));
        Mockito.verify(gnbduGroupingService, Mockito.times(1))
                .getNeighbourRelationsAndSaveToENM(Mockito.eq(NRC_TASK_ONGOING), Mockito.eq(E_NODE_B));

        assertEquals(Collections.singletonList(NrcNeighbor.builder().eNodeBId(ENODEB_ID).gNodeBDUs(nrcGroupingGnbdus).build()), result.collect(Collectors.toList()));
    }

    @Test
    public void getNrcGroupingENodeBTestErroneousBehaviour(){
        Mockito.when(ctsService.getLteNodeWithCellsAssoc(any())).thenThrow(GraniteFaultException.class);
        Stream<NrcNeighbor> result = nrcNeighboringService.getNrcNeighbor(NRC_TASK_ONGOING, ENODEB_ID);

        assertEquals(0, result.count());
    }
}
