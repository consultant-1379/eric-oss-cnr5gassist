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
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.client.cts.model.ENodeB;
import com.ericsson.oss.apps.client.cts.model.Gnbdu;
import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.client.cts.model.Resource;
import com.ericsson.oss.apps.service.CtsService;
import com.ericsson.oss.apps.service.ncmp.NeighboursPersistingService;
import com.ericsson.oss.apps.util.CtsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class GnbduGroupingService {

    private final CtsService ctsService;
    private final NeighbouringCellService neighbouringCellService;
    private final NeighboursPersistingService neighboursPersistingService;

    public List<NrcGroupingGnbdu> getNeighbourRelationsAndSaveToENM(NrcTask nrcTask, ENodeB eNodeB) {
        Map<Gnbdu, List<NrCell>> ctsData = neighbouringCellService.getFilteredNeighbourNrCellsWithAssoc(nrcTask.getRequest(), eNodeB).stream()
                .filter(nrCell -> CtsUtils.getParentNode(nrCell).isPresent())
                .collect(Collectors.collectingAndThen(
                    groupingBy(
                        nrCell -> CtsUtils.getParentNode(nrCell).get(),
                        mapping(Function.identity(), toList())),
                    this::getGnbduWithAssoc));

        return ctsData.entrySet().stream()
            .peek(groups -> neighboursPersistingService.createInEnm(nrcTask, eNodeB, groups))
            .map(entry -> NrcGroupingGnbdu.builder()
                .gNodeBDUId(entry.getKey().getId())
                .nrCellIds(entry.getValue().stream().map(Resource::getId).collect(toList()))
                .build())
            .collect(toList());
    }

    private Map<Gnbdu, List<NrCell>> getGnbduWithAssoc(Map<Gnbdu, List<NrCell>> nrcMapping) {
        return nrcMapping.entrySet().stream()
            .collect(Collectors.toMap(e -> ctsService.getNrDuNode(e.getKey().getId()), Map.Entry::getValue));
    }
}
