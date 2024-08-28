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

import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.exception.EnmAdapterOverloadedException;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.util.NrcUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestClientException;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.*;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;

@Slf4j
@RequiredArgsConstructor
public class NcmpService {

    private final NetworkCmProxyApi networkCmProxyApi;
    private final JsonHelper jsonHelper;
    private final JsonNodeValidator jsonNodeValidator;
    private final NcmpCounterService ncmpCounterService = new NcmpCounterService();
    @Autowired
    private InMemoryCacheService cacheService;
    @Autowired
    private MetricService metricService;

    @PostConstruct
    public void createGauge() {
        metricService.createGauge(CACHE_SIZE, cacheService.getCacheDataMap(),
            e -> e.keySet().stream().filter(key -> key.contains(NCMP_TAG)).collect(Collectors.toList()).size(),
            SERVICE, NCMP);
    }

    public <T extends NcmpAttribute> Optional<NcmpObject<T>> getResource(ExternalId externalId, Class<T> valueType) {
        String name = externalId.getResourceIdentifier().getLast().getFullName();
        return getResourcesWithOptions(externalId, getFields(name, valueType), name, valueType).stream()
            .findFirst();
    }

    public <T extends NcmpAttribute> List<NcmpObject<T>> getResources(ExternalId externalId, Class<T> valueType) {
        String name = NcmpObject.getName(valueType);
        String resourceIdentifier = NcmpObject.getResourceIdentifier(valueType);
        return getResourcesWithOptions(externalId, getFields(resourceIdentifier, valueType), name, valueType);
    }

    public <T extends NcmpAttribute> List<NcmpObject<T>> getResourcesWithOptions(ExternalId externalId, @NonNull NcmpAttribute scopeValue, Class<T> valueType) {
        String name = NcmpObject.getName(valueType);
        String resourceIdentifier = NcmpObject.getResourceIdentifier(valueType);
        String options = getFields(resourceIdentifier, valueType) + COMMA + getScope(name, scopeValue);
        return getResourcesWithOptions(externalId, options, name, valueType);
    }

    @SuppressWarnings("unchecked")
    private <T extends NcmpAttribute> List<NcmpObject<T>> getResourcesWithOptions(ExternalId externalId, String options, String name, Class<T> valueType) {
        var key = cacheKey(externalId, options, name, valueType.getName());
        var value = cacheService.get(key);
        if (value.isPresent()) {
            metricService.increment(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, NCMP);
            return (List<NcmpObject<T>>) value.get();
        }

        List<JsonNode> resourceData;
        try {
            resourceData = getResourceData(externalId, options, name);
        } catch (RestClientException | EnmAdapterOverloadedException e) {
            log.error("NCMP resource doesn't exist. ExternalId: {}, Name: {}, Options: {}, Error: {}", externalId, name, options, e.toString());
            log.debug("Detailed error", e);
            cacheService.add(key, Collections.emptyList());
            return Collections.emptyList();
        }

        TypeFactory typeFactory = jsonHelper.getTypeFactory();
        JavaType javaType = typeFactory.constructParametricType(NcmpObject.class, valueType);
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, javaType);
        List<NcmpObject<T>> ncmpObjects = new ArrayList<>();
        for (JsonNode jsonNode : resourceData) {
            try {
                jsonNode = jsonNodeValidator.validate(jsonNode, javaType);
                ncmpObjects.addAll(jsonHelper.convertValue(jsonNode, collectionType));
            } catch (Exception e) {
                log.error("JSON conversion error: {}, Name: {}, Options: {}, Error: {}", externalId, name, options, e.toString());
                log.debug("Detailed error", e);
            }
        }

        cacheService.add(key, ncmpObjects);
        return ncmpObjects;
    }

    private String cacheKey(ExternalId externalId, Object... params) {
        String methodName = new Throwable().getStackTrace()[1].getMethodName();
        StringBuilder key = new StringBuilder(methodName);
        key.append(COMMA + NCMP_TAG);
        key.append(COMMA + NrcUtil.externalIdKey(externalId));
        for (var param : params) {
            key.append(COMMA + param.toString());
        }
        return key.toString();
    }

    private List<JsonNode> getResourceData(ExternalId externalId, String options, String name) {
        return jsonHelper.convertValue(
            networkCmProxyApi.getResourceDataOperationalForCmHandle(
                externalId.getCmHandle(), externalId.getResourceIdentifier().toString(),
                APPLICATION_YANG_DATA_JSON, options),
            JsonNode.class).findValues(name);
    }

    private <T extends NcmpAttribute> String getFields(String name, Class<T> valueType) {
        return String.format("fields=%s/attributes(%s)", name, NcmpObject.getAttributeNames(valueType));
    }

    private String getScope(String name, @NonNull NcmpAttribute scopeValue) {
        String attributes = jsonHelper.convertToStringMap(scopeValue).entrySet().stream()
            .map(Object::toString)
            .collect(Collectors.joining(SEMI_COLON));
        return String.format("scope=%s/attributes(%s)", name, attributes);
    }

    public ResponseEntity<Object> createResource(ExternalId externalId, NcmpObject<NcmpAttribute> ncmpObject) {
        return createResources(externalId, List.of(ncmpObject));
    }

    @SneakyThrows
    public ResponseEntity<Object> createResources(ExternalId externalId, List<NcmpObject<NcmpAttribute>> ncmpObjects) {
        try {
            Map<String, List<NcmpObject<NcmpAttribute>>> bodyContent = ncmpObjects.stream()
                .collect(Collectors.groupingBy(NcmpObject::getName));
            cacheService.remove(NrcUtil.externalIdKey(externalId));
            ncmpCounterService.incrementCounter(NCMP_CREATED_OBJECT_COUNT);
            return networkCmProxyApi.createResourceDataRunningForCmHandleWithHttpInfo(
                externalId.getCmHandle(), externalId.getResourceIdentifier().toString(),
                jsonHelper.writeValueAsString(bodyContent), null);
        } catch (Exception e) {
            log.error("Failed to create the resources. cmHandle: {}, resourceIdentifier: {}, body: {}, contentType: {}",
                    externalId.getCmHandle(), externalId.getResourceIdentifier().toString(), ncmpObjects, APPLICATION_YANG_DATA_JSON);
            throw e;
        }

    }
}
