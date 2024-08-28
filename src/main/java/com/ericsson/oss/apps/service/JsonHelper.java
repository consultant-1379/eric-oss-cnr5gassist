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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonHelper {

    private static final String ERROR_MSG = "Error parsing JSON";

    private final ObjectMapper mapper;

    public TypeFactory getTypeFactory(){
        return mapper.getTypeFactory();
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) {
            throw new RestClientException("The JSON object is null");
        }
        return mapper.convertValue(fromValue, toValueType);
    }

    public <T> T convertValue(Object fromValue, JavaType toValueType) {
        if (fromValue == null) {
            throw new RestClientException("The JSON object is null");
        }
        return mapper.convertValue(fromValue, toValueType);
    }

    public <T> T readValue(String content, Class<T> toValueType) throws JsonProcessingException {
        return mapper.readValue(content, toValueType);
    }

    public String writeValueAsString(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    public Map<String, String> convertToStringMap(Object object) {
        Map<String, JsonNode> properties = mapper.convertValue(object, new TypeReference<>() {});
        return properties.entrySet().stream()
            .filter(e -> !(e.getValue() instanceof NullNode))
            .collect(LinkedHashMap::new, this::putInStringMap, Map::putAll);
    }

    private void putInStringMap(Map<String, String> map, Map.Entry<String, JsonNode> entry) {
        try {
            map.put(entry.getKey(), valueToString(entry.getValue()));
        } catch (JsonProcessingException e) {
            log.warn(ERROR_MSG, e);
        }
    }

    private String valueToString(JsonNode value) throws JsonProcessingException {
        return value instanceof TextNode ? value.textValue() : this.writeValueAsString(value);
    }
}
