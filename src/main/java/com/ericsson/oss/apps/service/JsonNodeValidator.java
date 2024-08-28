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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ConcurrentModificationException;

import static com.ericsson.oss.apps.util.Constants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonNodeValidator {

    private static final String[] PLMN_ID_FIELDS = new String[] {
        PLMN_ID_LIST,
        EXTERNAL_GUTRAN_CELL_PLMN_ID_LIST,
        EXTERNAL_GNODEB_PLMN_ID,
        DU_PLMN_ID};
    private static final String ENDPOINT_RESOURCE_REF = "endpointResourceRef";

    private final JsonHelper jsonHelper;

    public JsonNode validate(JsonNode jsonNode, JavaType javaType) {
        return validateAndFix(null, jsonNode, javaType);
    }

    private JsonNode validateAndFix(JsonNode parentJsonNode, JsonNode jsonNode, JavaType javaType) {
        if (jsonNode instanceof ArrayNode) {
            validateArrayNode((ArrayNode) jsonNode, javaType);
        } else if (jsonNode instanceof ObjectNode) {
            validateObjectNode(parentJsonNode, (ObjectNode) jsonNode, javaType);
        }
        return jsonNode;
    }

    private void validateArrayNode(ArrayNode arrayNode, JavaType javaType) {
        int size = arrayNode.size();
        try {
            for (JsonNode node : arrayNode) {
                validateAndFix(arrayNode, node, javaType);
            }
        } catch (ConcurrentModificationException e) {
            log.warn("JSON array node modification: {}", e.toString());
            log.debug("Detailed error", e);
            if (arrayNode.size() > 0 && arrayNode.size() < size) {
                validateArrayNode(arrayNode, javaType);
            }
        }
    }

    private void validateObjectNode(JsonNode parentJsonNode, ObjectNode objectNode, JavaType javaType) {
        for (JsonNode jsonNode : objectNode) {
            validateAndFix(objectNode, jsonNode, javaType);
        }

        if (objectNode.get(ATTRIBUTES) != null) {
            try {
                if (objectNode.get(ATTRIBUTES) instanceof ObjectNode) {
                    ObjectNode attributes = (ObjectNode) objectNode.get(ATTRIBUTES);
                    removeEndpointResourceRefIfNull(attributes);
                    removePlmnIdIfNull(attributes);
                }
                if (javaType != null) {
                    jsonHelper.convertValue(objectNode, javaType);
                }
            } catch (Exception e) {
                log.error("JSON conversion error: {}", e.toString());
                log.debug("Detailed error", e);

                removeInvalidNode(parentJsonNode, objectNode);
            }
        }
    }

    private void removeInvalidNode(JsonNode parentJsonNode, JsonNode jsonNode) {
        if (parentJsonNode instanceof ArrayNode parentArrayNode) {
            for (int i = 0; i < parentArrayNode.size(); i++) {
                if (parentArrayNode.get(i).equals(jsonNode)) {
                    parentArrayNode.remove(i);
                    break;
                }
            }
        }
    }

    private void removeEndpointResourceRefIfNull(ObjectNode attributes) {
        if (attributes.get(ENDPOINT_RESOURCE_REF) instanceof TextNode textNode) {
            String value = textNode.asText().trim();
            if (value.isEmpty() || value.equals(NULL)) {
                log.warn("Invalid JSON value. Name: {}, Value: {}", ENDPOINT_RESOURCE_REF, value);
                attributes.remove(ENDPOINT_RESOURCE_REF);
            }
        }
    }

    private void removePlmnIdIfNull(ObjectNode attributes) {
        for (String field : PLMN_ID_FIELDS) {
            if (attributes.get(field) instanceof TextNode textNode) {
                String value = textNode.asText().trim();

                log.warn("Invalid JSON value. Name: {}, Value: {}", ENDPOINT_RESOURCE_REF, value);
                attributes.remove(field);

                if (!value.isEmpty() && !value.equals(NULL)) {
                    log.warn("JSON value should be converted. Name: {}, Value: {}", field, value);
                    try {
                        String newValue = value
                            .replaceAll("\"", "")
                            .replaceAll(MCC + "=", "\"" + MCC + "\":")
                            .replaceAll(MNC + "=", "\"" + MNC + "\":")
                            .replaceAll(MNC_LENGTH + "=", "\"" + MNC_LENGTH + "\":");
                        attributes.putIfAbsent(field, jsonHelper.readValue(newValue, JsonNode.class));
                    } catch (JsonProcessingException e) {
                        log.warn("JSON value can't be converted. Name: {}, Value: {}, Error: {}", ENDPOINT_RESOURCE_REF, value, e.getMessage());
                        log.debug("Detailed error", e);
                    }
                }
            }
        }
    }
}
