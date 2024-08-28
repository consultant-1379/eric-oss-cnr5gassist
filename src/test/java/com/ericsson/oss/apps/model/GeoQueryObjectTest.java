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

import com.ericsson.oss.apps.service.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ericsson.oss.apps.util.TestDefaults.SEED;
import static com.ericsson.oss.apps.util.TestDefaults.matcherRegex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class GeoQueryObjectTest {

    @Spy
    private ObjectMapper objectMapper;
    @InjectMocks
    private JsonHelper jsonHelper;

    private GeoQueryObject.GeoQueryObjectBuilder geoQueryObjectBuilder;

    private String testStringGenerator(Map<String, String> inputMap) {
        return inputMap.entrySet()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining("&"));
    }

    @BeforeEach
    public void setUp() {
        geoQueryObjectBuilder = GeoQueryObject.builder()
            .geoPoint(GeoPoint.of(2.2F, 1.1F))
            .distance(200);
    }

    @Test
    public void missingParameterTest() {
        assertThrows(NullPointerException.class, () ->
            jsonHelper.convertToStringMap(GeoQueryObject.builder().build()));

        assertThrows(NullPointerException.class, () ->
            jsonHelper.convertToStringMap(GeoQueryObject.builder().distance(2).build()));
    }

    @Test
    public void getNrCellsByGeoWithJsonException() throws JsonProcessingException {
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(Mockito.any());
        String filterList = testStringGenerator(jsonHelper.convertToStringMap(geoQueryObjectBuilder.build()));
        assertEquals("geographicSite.locatedAt.type.eq='GeospatialCoords'", filterList);
    }

    @Test
    public void generatedSimpleResultTests() {
        String expectedSimpleResult = "geographicSite.locatedAt.geospatialData.geoDistanceWithin=" +
                "{\"center\":{\"type\":\"Point\",\"coordinates\":[2.2,1.1]},\"distance\":200}" +
                "&geographicSite.locatedAt.type.eq='GeospatialCoords'";
        assertEquals(expectedSimpleResult, testStringGenerator(jsonHelper.convertToStringMap(geoQueryObjectBuilder.build()))
        );
    }

    @Test
    public void generatedFullScaleResultTests() {
        String expectedFullScaleResult = "downlinkEARFCN.in=[2I,3I]" +
                "&geographicSite.locatedAt.geospatialData.geoDistanceWithin=" +
                "{\"center\":{\"type\":\"Point\",\"coordinates\":[2.2,1.1,3.3]},\"distance\":2}" +
                "&geographicSite.locatedAt.type.eq='GeospatialCoords'";

        assertEquals(expectedFullScaleResult, testStringGenerator(jsonHelper.convertToStringMap(
            geoQueryObjectBuilder.frequency(2).frequency(3).distance(2)
                .geoPoint(GeoPoint.of(2.2F, 1.1F, 3.3F)).build())));
    }

    @Test
    public void generatedRandomTests() {
        Random rand = new Random(SEED);

        IntStream.range(0, 99).forEachOrdered(i -> {
            List<Integer> frequencyList = rand.ints(2, 0, 20000).boxed().collect(Collectors.toList());
            assertThat(testStringGenerator(jsonHelper.convertToStringMap(GeoQueryObject.builder()
                    .geoPoint(GeoPoint.of(rand.nextFloat() * 2 - 1, rand.nextFloat() * 2 - 1, (rand.nextBoolean()) ? rand.nextFloat() : null))
                    .frequencies((rand.nextBoolean()) ? frequencyList : Collections.emptyList())
                    .distance(rand.nextInt(20000))
                    .build())), matchesPattern(matcherRegex));
            frequencyList.clear();
        });
    }
}