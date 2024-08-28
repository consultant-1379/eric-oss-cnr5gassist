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
package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static com.ericsson.oss.apps.util.Constants.ATTRIBUTES;
import static com.ericsson.oss.apps.util.Constants.QUOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class JsonHelperTest {

    private static final String JSON_TEXT = QUOTE + ATTRIBUTES + QUOTE;
    private static final String JSON_OBJECT = "{\"bad\":\"externalNode\",\"wrong\":null}";

    @Mock
    TypeFactory factory;
    @Spy
    ObjectMapper mapper;
    @InjectMocks
    JsonHelper jsonHelper;

    @Test
    public void getTypeFactory() {
        Mockito.when(mapper.getTypeFactory()).thenReturn(factory);
        assertEquals(factory, jsonHelper.getTypeFactory());
        Mockito.verify(mapper, Mockito.times(1)).getTypeFactory();
    }

    @Test
    public void convertValueTest() {
        assertEquals(ATTRIBUTES, jsonHelper.convertValue(ATTRIBUTES, String.class));
        Mockito.verify(mapper, Mockito.times(1)).convertValue(ATTRIBUTES, String.class);
        assertThrows(RestClientException.class, () -> jsonHelper.convertValue(null, String.class));

        JavaType type = factory.constructParametricType(NcmpObject.class, String.class);
        CollectionType collectionType = factory.constructCollectionType(List.class, type);
        assertThrows(RestClientException.class, () -> jsonHelper.convertValue(null, collectionType));
    }

    @Test
    public void writeValueAsAStringTest() throws JsonProcessingException {
        assertEquals(JSON_TEXT, jsonHelper.writeValueAsString(ATTRIBUTES));
        Mockito.verify(mapper, Mockito.times(1)).writeValueAsString(Mockito.eq(ATTRIBUTES));
    }

    @Test
    public void writeValueAsAStringException() throws JsonProcessingException {
        Mockito.when(mapper.writeValueAsString(Mockito.any())).thenThrow(JsonProcessingException.class);
        assertThrows(JsonProcessingException.class, () -> jsonHelper.writeValueAsString(ATTRIBUTES));
    }

    @Test
    public void writeValueAsAStringMapTest() throws JsonProcessingException {
        JsonNode jsonNode = mapper.readValue(JSON_OBJECT, JsonNode.class);
        assertEquals(Map.of("bad", "externalNode"), jsonHelper.convertToStringMap(jsonNode));
    }
}
