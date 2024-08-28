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

import com.ericsson.oss.apps.api.model.NrcNeighbor;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.client.cts.model.ENodeB;
import com.ericsson.oss.apps.exception.GraniteFaultException;
import com.ericsson.oss.apps.service.CtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class NrcNeighboringService {

    private final CtsService ctsService;
    private final GnbduGroupingService gnbduGroupingService;

    public Stream<NrcNeighbor> getNrcNeighbor(final NrcTask nrcTask, final Long eNodeBId) {
        return fetchLteNode(eNodeBId)
            .map(eNodeB -> NrcNeighbor.builder()
                .eNodeBId(eNodeB.getId())
                .gNodeBDUs(gnbduGroupingService.getNeighbourRelationsAndSaveToENM(nrcTask, eNodeB))
                .build())
            .stream();
    }

    private Optional<ENodeB> fetchLteNode(Long id) {
        try {
            return Optional.of(ctsService.getLteNodeWithCellsAssoc(id));
        } catch (GraniteFaultException e) {
            log.warn("ENodeB with id: {} was not found in CTS.", id, e);
            return Optional.empty();
        }
    }
}
