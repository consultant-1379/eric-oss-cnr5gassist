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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.*;

@Slf4j
@Service
public class NcmpCounterService {

    private static final Map<String, Integer> ncmpCounters = new HashMap<>();

    public NcmpCounterService() {
        ncmpCounters.put(NRC_FOUND_NEIGHBOURING_NODES_COUNT, 0);
        ncmpCounters.put(NRC_FOUND_NEIGHBOURING_CELLS_COUNT, 0);
        ncmpCounters.put(NCMP_MISSING_NEIGHBOURS_COUNT, 0);
        ncmpCounters.put(NCMP_CREATED_OBJECT_COUNT, 0);
        ncmpCounters.put(NCMP_EXTERNALGNODEBFUNCTIONS_OBJECT_COUNT, 0);
        ncmpCounters.put(NCMP_TERMPOINTTOGNB_OBJECT_COUNT, 0);
        ncmpCounters.put(NCMP_EXTERNALGUTRANCELL_OBJECT_COUNT, 0);
    }

    public void incrementCounter(String counterName) {
        if (checkCounterName(counterName)) {
            ncmpCounters.put(counterName, ncmpCounters.get(counterName) + 1);
        } else {
            log.debug("CounterName is not allowed");
        }
    }

    public boolean checkCounterName(String counterName) {
        return ncmpCounters.containsKey(counterName);
    }

    public void resetCounters() {
        ncmpCounters.replaceAll((k, v) -> 0);
    }

    public void debugCounters(UUID requestUUID) {
        log.debug("NRC request ID : {}", requestUUID);
        ncmpCounters.forEach((k, v) ->
            log.debug(k + " = " + v)
        );
    }

    public int getCounterValue(String counterName) {
        if (checkCounterName(counterName)) {
            return ncmpCounters.get(counterName);
        }
        return 0;
    }

    public List<String> getCounterNames() {
        return ncmpCounters.entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}