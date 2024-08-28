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

import com.ericsson.oss.apps.model.CacheData;
import com.ericsson.oss.apps.util.StringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InMemoryCacheService {

    @Getter
    private Map<String, CacheData> cacheDataMap = new ConcurrentHashMap<>();

    @Value("${nrc.cache.max-size}")
    int maxKeys;
    @Value("${nrc.cache.expiry-time}")
    int expiryInMinutes;

    public void add(String key, Object value) {
        if (!isValid(key)) {
            if (isFull() && !cleanExpiredData()) {
                log.warn("Key " + key + " is used for a valid object and caching service couldn't provide a slot");
            } else {
                log.info("cacheService PUT: key: {} -> hash: {} value: {}", key, value.hashCode(), StringUtil.toJson(value));
                cacheDataMap.put(key, new CacheData(value, expiryInMinutes));
            }
        } else {
            log.warn("Key " + key + " is used for a valid object and cannot be used");
        }
    }

    public Optional get(String key) {
        if (cacheDataMap.containsKey(key)) {
            CacheData data = cacheDataMap.get(key);
            if (data.isExpired()) {
                cacheDataMap.remove(key);
            } else {
                log.info("cacheService RETURN: key: {} -> hash: {} value: {}", key, data.getData().hashCode(), StringUtil.toJson(data.getData()));
                return Optional.of(data.getData());
            }
        }
        return Optional.empty();
    }

    public boolean remove(String filter) {
        return cacheDataMap.entrySet().removeIf(entry -> entry.getKey().contains(filter));
    }

    public void clear() {
        cacheDataMap.clear();
    }

    public boolean isFull() {
        int numberOfCurrentKeys = cacheDataMap.keySet().size();
        return numberOfCurrentKeys + 1 > maxKeys;
    }

    public boolean isValid(String key) {
        return cacheDataMap.containsKey(key) && !cacheDataMap.get(key).isExpired();
    }

    public int count() {
        return cacheDataMap.entrySet().size();
    }

    public int count(String filter) {
        return filter(filter).size();
    }

    public List<Map.Entry<String, CacheData>> filter(String filter) {
        return cacheDataMap.entrySet().stream()
            .filter(enty -> enty.getKey().contains(filter)).collect(Collectors.toList());
    }

    private boolean cleanExpiredData() {
        if (cacheDataMap.entrySet().removeIf(entry -> entry.getValue().isExpired())) {
            return true;
        }

        var oldestEntry = cacheDataMap.entrySet().stream().min(Comparator.comparing(o -> o.getValue().getTimestamp()));
        if (oldestEntry.isPresent()) {
            return cacheDataMap.remove(oldestEntry.get().getKey()) != null;
        }

        return false;
    }
}