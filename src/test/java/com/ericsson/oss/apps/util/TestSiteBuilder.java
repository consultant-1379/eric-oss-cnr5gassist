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

import com.ericsson.oss.apps.client.cts.model.Association;
import com.ericsson.oss.apps.client.cts.model.GeographicLocation;
import com.ericsson.oss.apps.client.cts.model.GeographicSite;
import com.ericsson.oss.apps.client.cts.model.GeospatialData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.LOADED;

@RequiredArgsConstructor
public class TestSiteBuilder implements Iterable<GeographicSite> {

    private final List<Runnable> stateFunctions = List.of(this::initGeographicSite, this::setEmptyLocatedAt,
        this::setLocatedAt, this::setLocation, this::setLocationMode, this::setGeospatialData);

    private final Association locationAssoc = new Association();
    private final GeographicLocation location = new GeographicLocation();
    @Getter private final GeospatialData geospatialData = new GeospatialData();
    private final int count;

    @Getter private GeographicSite site;

    private void initGeographicSite() {
        site = new GeographicSite();
    }

    private void setEmptyLocatedAt() {
        site.setLocatedAt(Collections.emptyList());
    }

    private void setLocatedAt() {
        List<Association> locations =  Stream.concat(Stream.of(new Association()),
            Stream.generate(() -> locationAssoc).limit(count)).collect(Collectors.toList());
        site.setLocatedAt(locations);
    }

    private void setLocation() {
        locationAssoc.setValue(location);
    }

    private void setLocationMode() {
        locationAssoc.setMode(LOADED);
    }

    private void setGeospatialData() {
        location.setGeospatialData(geospatialData);
    }

    @Override
    public Iterator<GeographicSite> iterator() {
        return stateFunctions.stream().map(i -> {
            i.run();
            return this.getSite();
        }).iterator();
    }
}
