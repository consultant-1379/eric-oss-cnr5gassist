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

import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.client.cts.model.ENodeB;
import com.ericsson.oss.apps.client.cts.model.LteCell;
import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.client.cts.model.NrSectorCarrier;
import com.ericsson.oss.apps.model.GeoPoint;
import com.ericsson.oss.apps.service.CtsService;
import com.ericsson.oss.apps.util.CtsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NeighbouringCellService {

    private static final int DEFAULT_DISTANCE = 200;

    private final CtsService ctsService;

    public List<NrCell> getFilteredNeighbourNrCellsWithAssoc(NrcRequest nrcRequest, ENodeB eNodeB) {
        Integer distance = Optional.ofNullable(nrcRequest.getDistance()).orElse(DEFAULT_DISTANCE);

        return CtsUtils.getChildrenCells(eNodeB).parallel()
            .flatMap(lteCell -> getFilteredNeighbourNrCells(lteCell, distance, nrcRequest.getFreqPairs()))
            .map(NrCell::getId).distinct()
            .map(ctsService::getNrCellWithAssoc)
            .collect(Collectors.toList());
    }

    private Stream<NrCell> getFilteredNeighbourNrCells(LteCell lteCell, Integer distance, Map<String, Set<Integer>> freqPairs) {
        return Stream.ofNullable(CtsUtils.getFrequencies(freqPairs, lteCell.getFdDearfcnDl()))
            .flatMap(freqFilter -> findNeighbourNrCells(lteCell, distance).stream()
                .filter(nrCell -> {
                    Optional<NrSectorCarrier>  nrSectorCarrier = CtsUtils.getNrSectorCarrier(nrCell);
                    return freqFilter.isEmpty() || (nrSectorCarrier.isPresent() && freqFilter.contains(nrSectorCarrier.get().getArfcnDL()));
                }));
    }

    private List<NrCell> findNeighbourNrCells(LteCell lteCell, Integer distance) {
        return Optional.of(ctsService.getLteCellWithGeographicSitesAssoc(lteCell.getId()))
            .flatMap(this::getGeoPoint)
            .map(coord -> CtsUtils.buildGeoQuery(coord, distance))
            .map(ctsService::getNrCellWithFilters)
            .orElse(Collections.emptyList());
    }

    private Optional<GeoPoint> getGeoPoint(LteCell lteCell) {
        return CtsUtils.getSites(lteCell)
            .map(site -> ctsService.getGeographicSiteWithGeographicLocationsAssoc(site.getId()))
            .flatMap(CtsUtils::getGeoPoints)
            .findAny();
    }
}
