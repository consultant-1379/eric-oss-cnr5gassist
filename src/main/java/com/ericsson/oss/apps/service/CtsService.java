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

import com.ericsson.oss.apps.client.cts.GeoServiceApi;
import com.ericsson.oss.apps.client.cts.LteServiceApi;
import com.ericsson.oss.apps.client.cts.NrServiceApi;
import com.ericsson.oss.apps.client.cts.model.*;
import com.ericsson.oss.apps.model.GeoQueryObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.COMMA;
import static com.ericsson.oss.apps.util.Constants.CTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;

@RequiredArgsConstructor
@Slf4j
public class CtsService {

    private static final String ATTRS = "attrs";
    private static final String KEY = "key";

    private final NrServiceApi nrServiceApi;
    private final LteServiceApi lteServiceApi;
    private final GeoServiceApi geoServiceApi;
    private final JsonHelper jsonHelper;
    @Autowired
    private InMemoryCacheService cacheService;
    @Autowired
    private MetricService metricService;

    @PostConstruct
    public void createGauge() {
        metricService.createGauge(CACHE_SIZE, cacheService.getCacheDataMap(),
            e -> e.keySet().stream().filter(key -> key.contains(CTS_TAG)).collect(Collectors.toList()).size(),
            SERVICE, CTS);
    }

    public Long getNrDuNodeCount() {
        return cacheMethod(cacheKey(), nrServiceApi::countGnbduTasks);
    }

    public List<Gnbdu> getAllNrDuNodes() {
        return cacheMethod(cacheKey(), nrServiceApi::listGnbdus);
    }

    public Gnbdu getNrDuNode(Long id) {
        return cacheMethod(cacheKey(id, ATTRS), () -> nrServiceApi.fetchGnbdu(id, ATTRS));
    }

    public Long getNrCellCount() {
        return cacheMethod(cacheKey(), nrServiceApi::countNrcellTasks);
    }

    public List<NrCell> getAllNrCells() {
        return cacheMethod(cacheKey(), nrServiceApi::listNrCells);
    }

    public List<NrCell> getNrCellWithFilters(Map<String, String> filterMap) {
        return cacheMethod(cacheKey(filterMap, ATTRS), () -> nrServiceApi.queryNrCellWithFilters(filterMap, ATTRS));
    }

    public List<NrCell> getNrCellWithFilters(GeoQueryObject geoQueryObject) {
        return getNrCellWithFilters(jsonHelper.convertToStringMap(geoQueryObject));
    }

    public NrCell getNrCell(Long id) {
        return cacheMethod(cacheKey(id), () -> nrServiceApi.fetchNrCell(id, null, null));
    }

    public NrCell getNrCellWithAssoc(Long id) {
        return cacheMethod(cacheKey(id, KEY, ATTRS), () -> nrServiceApi.fetchNrCell(id, KEY, ATTRS));
    }

    public List<ENodeB> getAllLteNodes() {
        return cacheMethod(cacheKey(), lteServiceApi::listENodeBs);
    }

    public ENodeB getLteNode(Long id) {
        return cacheMethod(cacheKey(id), () -> lteServiceApi.fetchENodeB(id, null));
    }

    public ENodeB getLteNodeWithCellsAssoc(Long id) {
        return cacheMethod(cacheKey(id, ATTRS), () -> lteServiceApi.fetchENodeB(id, ATTRS));
    }

    public List<LteCell> getAllLteCells() {
        return cacheMethod(cacheKey(), lteServiceApi::listLteCells);
    }

    public LteCell getLteCellWithGeographicSitesAssoc(Long id) {
        return cacheMethod(cacheKey(id, ATTRS), () -> lteServiceApi.fetchLteCell(id, ATTRS));
    }

    public GeographicSite getGeographicSiteWithGeographicLocationsAssoc(Long id) {
        return cacheMethod(cacheKey(id, ATTRS), () -> geoServiceApi.fetchGeographicSite(id, ATTRS));
    }

    private String cacheKey(Object... params) {
        String methodName = new Throwable().getStackTrace()[1].getMethodName();
        StringBuilder key = new StringBuilder(methodName);
        key.append(COMMA + CTS_TAG);
        for (var param : params) {
            key.append(COMMA + param.toString());
        }
        return key.toString();
    }

    private interface CachableMethod<T> {
        T op();
    }

    private <T> T cacheMethod(String key, CachableMethod<T> operator) {
        T result;
        Optional optional = cacheService.get(key);
        if (optional.isEmpty()) {
            result = operator.op();
            cacheService.add(key, result);
            return result;
        } else {
            metricService.increment(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, CTS);
            result = (T) optional.get();
        }
        return result;
    }
}
