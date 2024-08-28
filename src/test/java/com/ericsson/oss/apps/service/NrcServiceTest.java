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

package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.api.model.NrcNeighbor;
import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.service.nrc.NrcNeighboringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class NrcServiceTest {

    @Mock
    private NrcNeighboringService nrcNeighboringService;

    @InjectMocks
    private NrcService nrcService;

    @Test
    public void startNrcTest() {
        NrcTask nrcTask = NrcTask.builder().request(NrcRequest.builder()
                .eNodeBIds(Collections.singleton(1L))
                .build()).build();

        NrcNeighbor nrcNeighbor = NrcNeighbor.builder().eNodeBId(1L).build();
        Mockito.when(nrcNeighboringService.getNrcNeighbor(nrcTask, 1L)).thenReturn(Stream.of(nrcNeighbor));

        List<NrcNeighbor> result = nrcService.startNrc(nrcTask);

        Mockito.verify(nrcNeighboringService, Mockito.times(1)).getNrcNeighbor(nrcTask, 1L);

        assertEquals(List.of(nrcNeighbor), result);
    }
}
