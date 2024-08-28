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

package com.ericsson.oss.apps.util;

import com.ericsson.oss.apps.client.cts.model.*;
import com.ericsson.oss.apps.model.GeoPoint;
import com.ericsson.oss.apps.model.GeoQueryObject;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.LOADED;
import static org.junit.jupiter.api.Assertions.*;

public class CtsUtilsTest {

    private static final Set<Integer> FREQUENCIES = Collections.singleton(5);
    private static final Long DOWN_LINK_FREQUENCY = 3L;
    private static final Map<String, Set<Integer>> FREQ_PAIR = Collections.singletonMap(DOWN_LINK_FREQUENCY.toString(), FREQUENCIES);
    private static final LteCell LTE_CELL = LteCell.builder().fdDearfcnDl(DOWN_LINK_FREQUENCY).build();
    private static final Integer DISTANCE = 3;
    private static final List<Float> COORDINATE = List.of(1.4F, 2.4F);
    private static final GeoPoint GEO_POINT= GeoPoint.of(COORDINATE);
    private static final GeoQueryObject GEO_QUERY_OBJECT = GeoQueryObject.builder()
        .geoPoint(GEO_POINT)
        .distance(3)
        .build();
    private static final int COORDINATES_COUNT = 2;

    @Test
    public void getChildrenLteCells() {
        Association lteCellAssoc1 = Association.builder().mode(LOADED).value(LTE_CELL).build();
        Association lteCellAssoc2 = new Association();
        ENodeB eNodeB = ENodeB.builder().lteCells(Arrays.asList(lteCellAssoc1, lteCellAssoc2)).build();

        List<LteCell> lteCell = CtsUtils.getChildrenCells(eNodeB).collect(Collectors.toList());

        assertEquals(1, lteCell.size());
        assertEquals(LTE_CELL, lteCell.get(0));
    }

    @Test
    public void getWirelessNetwork() {
        WirelessNetwork wirelessNetwork = WirelessNetwork.builder().id(1L).build();
        Association gnbduAllOfWirelessNetworks = Association.builder().mode(LOADED).value(wirelessNetwork).build();
        Gnbdu gnbdu = Gnbdu.builder().id(1L).wirelessNetworks(Collections.singletonList(gnbduAllOfWirelessNetworks)).build();

        assertEquals(Optional.of(wirelessNetwork), CtsUtils.getWirelessNetwork(gnbdu));
    }

    @Test
    public void getNrSectorCarrier() {
        NrSectorCarrier nrSectorCarrier = new NrSectorCarrier();
        Association assoc = Association.builder().mode(LOADED).value(nrSectorCarrier).build();
        NrCell nrCell = NrCell.builder().nrSectorCarriers(Collections.singletonList(assoc)).build();

        Optional<NrSectorCarrier> nodeOptional = CtsUtils.getNrSectorCarrier(nrCell);

        assertTrue(nodeOptional.isPresent());
        assertEquals(nrSectorCarrier, nodeOptional.get());
    }

    @Test
    public void getLteCellGeoSite() {
        GeographicSite site = GeographicSite.builder().id(1L).build();
        Association geoSiteAssoc = Association.builder().mode(LOADED).value(site).build();
        LteCell lteCell = LteCell.builder().geographicSite(Collections.singletonList(geoSiteAssoc)).build();

        Optional<GeographicSite> siteOptional = CtsUtils.getSites(lteCell).findFirst();

        assertTrue(siteOptional.isPresent());
        assertEquals(site, siteOptional.get());
    }

    @Test
    public void getParentGnbduNode() {
        Gnbdu node = Gnbdu.builder().id(1L).build();
        Association nodeAssoc = Association.builder().mode(LOADED).value(node).build();
        NrCell nrCell = NrCell.builder().gnbdu(Collections.singletonList(nodeAssoc)).build();

        Optional<Gnbdu> nodeOptional = CtsUtils.getParentNode(nrCell);

        assertTrue(nodeOptional.isPresent());
        assertEquals(node, nodeOptional.get());
    }

    @Test
    public void getFrequencyList() {
        Set<Integer> frequencyList = CtsUtils.getFrequencies(FREQ_PAIR, DOWN_LINK_FREQUENCY);

        assertEquals(FREQUENCIES, frequencyList);
        assertEquals(Collections.emptySet(), CtsUtils.getFrequencies(null, DOWN_LINK_FREQUENCY));
        assertEquals(Collections.emptySet(), CtsUtils.getFrequencies(Collections.emptyMap(), null));
        assertEquals(Collections.emptySet(), CtsUtils.getFrequencies(Collections.emptyMap(), DOWN_LINK_FREQUENCY));
    }

    @Test
    public void buildGeoQuery() {
        GeoQueryObject geoQueryObjectOptional = CtsUtils.buildGeoQuery(GEO_POINT, DISTANCE);

        assertEquals(COORDINATE, geoQueryObjectOptional.getGeospatialData().getCenter().getCoordinates());
        assertEquals(DISTANCE, geoQueryObjectOptional.getGeospatialData().getDistance());
        assertEquals(GEO_QUERY_OBJECT, geoQueryObjectOptional);
    }

    @Test
    public void getLocationCoordinates() {
        TestSiteBuilder builder = new TestSiteBuilder(COORDINATES_COUNT);

        for (GeographicSite site : builder) {
            assertFalse(CtsUtils.getGeoPoints(site).findAny().isPresent());
        }

        builder.getGeospatialData().coordinates(COORDINATE);

        List<GeoPoint> expectedPoints = Stream.generate(() -> GEO_POINT).limit(COORDINATES_COUNT)
            .collect(Collectors.toList());
        List<GeoPoint> calculatedPoints = CtsUtils.getGeoPoints(builder.getSite())
            .collect(Collectors.toList());
        assertEquals(expectedPoints, calculatedPoints);
    }
}
