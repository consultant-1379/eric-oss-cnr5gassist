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
import com.ericsson.oss.apps.config.ClientAspects;
import com.ericsson.oss.apps.model.GeoPoint;
import com.ericsson.oss.apps.model.GeoQueryObject;
import com.ericsson.oss.apps.service.CtsService;
import com.ericsson.oss.apps.util.CtsUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
public class NeighbouringCellsServiceTest {
    @MockBean
    private CtsService ctsService;
    @SpyBean
    private ClientAspects clientAspects;

    @Autowired
    private NeighbouringCellService neighbouringCellService;

    @Test
    public void getFilterNeighbourNrCellsEmptyFreqPairsTest() {
        List<Long> expectedIds = List.of(111L, 222L);
        List<NrCell> expectedNrCells = expectedIds.stream().map(i -> NrCell.builder().id(i).build())
            .collect(Collectors.toList());
        GeoPoint geoPoint = GeoPoint.of(1.4F, 3.5F);

        Mockito.when(ctsService.getLteCellWithGeographicSitesAssoc(LTE_CELL_ID)).thenReturn(LTE_CELL);
        Mockito.when(ctsService.getGeographicSiteWithGeographicLocationsAssoc(GEOGRAPHIC_SITE_ID))
            .thenReturn(GEOGRAPHIC_SITE);
        Mockito.when(ctsService.getNrCellWithFilters(any(GeoQueryObject.class)))
            .thenReturn(expectedNrCells);
        Mockito.when(ctsService.getNrCellWithAssoc(any()))
            .thenAnswer(i -> NrCell.builder().id(i.getArgument(0)).build());

        try (MockedStatic<CtsUtils> utils = Mockito.mockStatic(CtsUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> CtsUtils.getChildrenCells(any())).thenReturn(Stream.of(LTE_CELL));
            utils.when(() -> CtsUtils.getFrequencies(any(), any())).thenReturn(Collections.emptySet());
            utils.when(() -> CtsUtils.getSites(any())).thenReturn(Stream.of(GEOGRAPHIC_SITE));
            utils.when(() -> CtsUtils.getGeoPoints(GEOGRAPHIC_SITE)).thenReturn(Stream.of(geoPoint));

            List<NrCell> result = neighbouringCellService.getFilteredNeighbourNrCellsWithAssoc(NRC_REQUEST_NO_FREQ_PAIRS, E_NODE_B);

            assertEquals(expectedNrCells, result);
            utils.verify(() -> CtsUtils.getChildrenCells(any()), Mockito.times(1));
            utils.verify(() -> CtsUtils.getFrequencies(any(), any()), Mockito.times(1));
            utils.verify(() -> CtsUtils.getSites(any()), Mockito.times(1));
            utils.verify(() -> CtsUtils.getGeoPoints(GEOGRAPHIC_SITE), Mockito.times(2));
        }

        Mockito.verify(ctsService, Mockito.times(1))
            .getLteCellWithGeographicSitesAssoc(any());
        Mockito.verify(ctsService, Mockito.times(1))
            .getGeographicSiteWithGeographicLocationsAssoc(any());
        Mockito.verify(ctsService, Mockito.times(1))
            .getNrCellWithFilters(any(GeoQueryObject.class));
        Mockito.verify(ctsService, Mockito.times(expectedIds.size()))
            .getNrCellWithAssoc(any());
        Mockito.verify(clientAspects, Mockito.times(1))
            .logNeighbouringNrCellAnomalies(Mockito.any(), Mockito.any());
    }

    @Test
    public void getFilterNeighbourNrCellsByFreqPairsTest() {
        GeoPoint geoPoint = GeoPoint.of(1.4F, 3.5F);
        List<Long> nrCellIds = List.of(111L, 222L);

        Mockito.when(ctsService.getLteCellWithGeographicSitesAssoc(LTE_CELL_ID)).thenReturn(LTE_CELL);
        Mockito.when(ctsService.getGeographicSiteWithGeographicLocationsAssoc(GEOGRAPHIC_SITE_ID))
            .thenReturn(GEOGRAPHIC_SITE);
        Mockito.when(ctsService.getNrCellWithFilters(any(GeoQueryObject.class)))
            .thenReturn(Arrays.asList(NR_CELL_111, NR_CELL_222));
        Mockito.when(ctsService.getNrCellWithAssoc(nrCellIds.get(0))).thenReturn(NR_CELL_111);
        Mockito.when(ctsService.getNrCellWithAssoc(nrCellIds.get(1))).thenReturn(NR_CELL_222);

        try (MockedStatic<CtsUtils> utils = Mockito.mockStatic(CtsUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utils.when(() -> CtsUtils.getChildrenCells(any())).thenReturn(Stream.of(LTE_CELL));
            utils.when(() -> CtsUtils.getFrequencies(any(), any())).thenReturn(FREQUENCY_PAIR_MAP.get("44"));
            utils.when(() -> CtsUtils.getSites(any())).thenReturn(Stream.of(GEOGRAPHIC_SITE));
            utils.when(() -> CtsUtils.getGeoPoints(GEOGRAPHIC_SITE)).thenReturn(Stream.of(geoPoint));
            utils.when(() -> CtsUtils.getNrSectorCarrier(NR_CELL_111)).thenReturn(Optional.of(NR_SECTOR_CARRIER_1));

            List<NrCell> result = neighbouringCellService.getFilteredNeighbourNrCellsWithAssoc(NRC_REQUEST, E_NODE_B);

            assertEquals(Collections.singletonList(NR_CELL_111), result); //NrCell with id 222 is filtered out
            utils.verify(() -> CtsUtils.getChildrenCells(any()), Mockito.times(1));
            utils.verify(() -> CtsUtils.getFrequencies(any(), any()), Mockito.times(1));
            utils.verify(() -> CtsUtils.getSites(any()), Mockito.times(1));
            utils.verify(() -> CtsUtils.getGeoPoints(GEOGRAPHIC_SITE), Mockito.times(2));
        }

        Mockito.verify(ctsService, Mockito.times(1))
            .getLteCellWithGeographicSitesAssoc(any());
        Mockito.verify(ctsService, Mockito.times(1))
            .getGeographicSiteWithGeographicLocationsAssoc(any());
        Mockito.verify(ctsService, Mockito.times(1))
            .getNrCellWithFilters(any(GeoQueryObject.class));
        Mockito.verify(clientAspects, Mockito.times(1))
            .logNeighbouringNrCellAnomalies(Mockito.any(), Mockito.any());
    }
}
