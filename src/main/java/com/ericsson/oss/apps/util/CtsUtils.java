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
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.LOADED;
import static com.ericsson.oss.apps.util.StreamTools.collectionToStream;

@UtilityClass
public class CtsUtils {

    public static <T extends Resource> Stream<T> getAssoc(List<Association> associations, Class<T> type){
        return collectionToStream(associations)
            .filter(item -> item.getMode().equals(LOADED) && type.isInstance(item.getValue()))
            .map(item -> type.cast(item.getValue()));
    }

    public static Stream<LteCell> getChildrenCells(final ENodeB eNodeB) {
        return getAssoc(eNodeB.getLteCells(), LteCell.class);
    }

    public static Optional<WirelessNetwork> getWirelessNetwork(Gnbdu gnbdu){
        return getAssoc(gnbdu.getWirelessNetworks(), WirelessNetwork.class).findAny();
    }

    public static Optional<NrSectorCarrier> getNrSectorCarrier(NrCell nrCell){
        return getAssoc(nrCell.getNrSectorCarriers(), NrSectorCarrier.class).findAny();
    }

    public static Stream<GeographicSite> getSites(final LteCell lteCell) {
        return getAssoc(lteCell.getGeographicSite(), GeographicSite.class);
    }

    public static Optional<Gnbdu> getParentNode(final NrCell nrCell) {
        return getAssoc(nrCell.getGnbdu(), Gnbdu.class).findAny();
    }

    private static Stream<GeographicLocation> getLocations(final GeographicSite geographicSite) {
        return getAssoc(geographicSite.getLocatedAt(), GeographicLocation.class);
    }

    public static Set<Integer> getFrequencies(final Map<String, Set<Integer>> freqPairs, final Long fdDearfcnDl) {
        if (freqPairs != null && !freqPairs.isEmpty()) {
            String downLinkFrequency = Optional.ofNullable(fdDearfcnDl).map(Object::toString).orElse("");
            return freqPairs.get(downLinkFrequency);
        } else {
            return Collections.emptySet();
        }
    }

    public static GeoQueryObject buildGeoQuery(final GeoPoint geoPoint, final Integer distance) {
        return GeoQueryObject.builder()
            .geoPoint(geoPoint)
            .distance(distance).build();
    }

    public static Stream<GeoPoint> getGeoPoints(final GeographicSite geographicSite) {
        return getLocations(geographicSite)
            .map(CtsUtils::getCoordinate)
            .flatMap(Optional::stream)
            .filter(coordinates -> coordinates.size() >= 2)
            .map(GeoPoint::of);
    }

    private static Optional<List<Float>> getCoordinate(final GeographicLocation geographicLocation) {
        return Optional.ofNullable(geographicLocation)
            .map(GeographicLocation::getGeospatialData)
            .map(GeospatialData::getCoordinates);
    }
}
