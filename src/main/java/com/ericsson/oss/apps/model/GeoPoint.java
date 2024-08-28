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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.*;

@Data
@Builder
@RequiredArgsConstructor
@JsonPropertyOrder({TYPE, COORDINATES})
public class GeoPoint {
    @JsonIgnore @NonNull private final Float longitude;
    @JsonIgnore @NonNull private final Float latitude;
    @JsonIgnore private final Float altitude;

    public static GeoPoint of(@Size(min=2, max=3) Float... coordinate) {
        return of(Arrays.asList(coordinate));
    }

    public static GeoPoint of(@Size(min=2, max=3) List<Float> coordinate) {
        GeoPointBuilder builder = GeoPoint.builder();
        List<Consumer<Float>> setters = List.of(builder::longitude, builder::latitude, builder::altitude);

        IntStream.range(0, Math.min(coordinate.size(), setters.size()))
            .forEach(i -> setters.get(i).accept(coordinate.get(i)));

        return builder.build();
    }

    public String getType() {
        return POINT;
    }

    public List<Float> getCoordinates() {
        return Stream.of(longitude, latitude, altitude).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
