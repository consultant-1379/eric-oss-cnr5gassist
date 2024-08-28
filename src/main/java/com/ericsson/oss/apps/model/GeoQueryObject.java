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
package com.ericsson.oss.apps.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.*;

@Builder
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({GEO_FREQUENCY, GEO_QUERY_FILTER, GEO_TYPE})
public class GeoQueryObject {

    private static final String GEOSPATIAL_TYPE = "'GeospatialCoords'";

    @NonNull private GeoPoint geoPoint;
    @NonNull private Integer distance;
    @Singular private List<Integer> frequencies;

    @JsonProperty(GEO_FREQUENCY)
    public String getFrequencies() {
        return !frequencies.isEmpty() ? frequencies.stream()
                .map(frequency -> frequency.toString() + 'I')
                .collect(Collectors.joining(COMMA, "[", "]")) : "";
    }

    @JsonProperty(GEO_TYPE)
    public String getGeospatialType() {
        return GEOSPATIAL_TYPE;
    }

    @JsonProperty(GEO_QUERY_FILTER)
    public GeospatialData getGeospatialData() {
        return new GeospatialData(geoPoint, distance);
    }
}
