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

import com.ericsson.oss.apps.client.cts.model.*;
import com.ericsson.oss.apps.config.ClientAspects;
import com.ericsson.oss.apps.config.GatewayProperties;
import com.ericsson.oss.apps.model.GeoPoint;
import com.ericsson.oss.apps.util.CtsUtils;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import lombok.AllArgsConstructor;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.BACKSLASH;
import static com.ericsson.oss.apps.util.Constants.CTS;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "gateway.port=${wiremock.server.port}", "gateway.retry.maxDelay=1",
        "GS-Database-Name=eai_install", "GS-Database-Host-Name=localhost"})
@AutoConfigureWireMock(port = 0)
public class CtsServiceTest {

    @AllArgsConstructor
    public enum StubType {
        ALL_NR_DU_NODES("/ctw/gnbdu", "nr_du_nodes.json"),
        ALL_NR_DU_NODES_COUNT("/ctw/gnbduTask/count", "count"),
        ONE_NR_DU_NODE("/ctw/gnbdu/6", "nr_du_node.json"),
        ALL_NR_CELLS("/ctw/nrcell", "nr_cells.json"),
        ALL_NR_CELLS_COUNT("/ctw/nrcellTask/count", "count"),
        ONE_NR_CELL("/ctw/nrcell/3", "nr_cell.json"),
        ANY_NR_CELLS_GEO("/ctw/nrcell/", "nr_cells_with_geo.json", GEO_DATA),
        OTHER_NR_CELLS_GEO("/ctw/nrcell/", "nr_cells_with_geo_other.json", GEO_DATA_OTHER),
        ANY_NR_CELLS_GEO_WITH_FREQ("/ctw/nrcell/", "nr_cells_with_geo.json", GEO_DATA_WITH_FREQ),
        ONE_NR_CELL_WITH_GNBDU("/ctw/nrcell/23", "nr_cell_with_gnbdu_assoc.json", Map.of("fs.gnbdu", "key", "fs.nrSectorCarriers", "attrs")),
        ALL_LTE_NODES("/ctw/enodeb", "lte_nodes.json"),
        ONE_LTE_NODE("/ctw/enodeb/3", "lte_node.json"),
        LTE_NODE_WITH_CELLS("/ctw/enodeb/3", "lte_node_with_cells_assoc.json"),
        ALL_LTE_CELLS("/ctw/ltecell", "lte_cells.json"),
        ONE_LTE_CELL("/ctw/ltecell/7", "lte_cell.json"),
        ONE_GEO_SITE("/ctg/geographicsite/1", "geo_site.json");

        private final String path;
        private final String response;
        private final Map<String, String> queryParams;

        StubType(String path, String response) {
            this(path, response, Collections.emptyMap());
        }

        public String getMediaType() {
            if (response.endsWith(".json"))
                return MediaType.APPLICATION_JSON_VALUE;
            return MediaType.TEXT_PLAIN_VALUE;
        }
    }

    @Autowired
    private GatewayProperties gatewayProperties;
    @Autowired
    private CtsService ctsService;
    @Autowired
    private InMemoryCacheService cacheService;
    @Autowired
    private MetricService metricService;
    @SpyBean
    private ClientAspects clientAspects;
    private int expectedRestTemplateRetry = 0;
    private int expectedCtsException = 0;

    private void stubForNormal(StubType stubType) {
        stubFor(commonGetStub(stubType).willReturn(WireMock.aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, stubType.getMediaType())
            .withBodyFile(CTS + BACKSLASH + stubType.response)));
    }

    private void stubForFail(StubType stubType) {
        stubFor(commonGetStub(stubType).willReturn(WireMock.aResponse()
            .withFault(Fault.EMPTY_RESPONSE)));
    }

    private MappingBuilder commonGetStub(StubType stubType) {
        GatewayProperties.Service service = gatewayProperties.getService(CTS);
        MappingBuilder mapping = get(urlPathEqualTo(service.getBasePath() + stubType.path));
        for (Map.Entry<String, String> param : stubType.queryParams.entrySet())
            mapping = mapping.withQueryParam(param.getKey(), new ContainsPattern(param.getValue()));
        for (Map.Entry<String, String> entry : service.getHeadersAsMap().entrySet())
            mapping = mapping.withHeader(entry.getKey(), equalTo(entry.getValue()));
        return mapping;
    }

    @BeforeEach
    void resetCache() {
        expectedRestTemplateRetry = 1;
        expectedCtsException = 1;
        cacheService.clear();
    }

    @AfterEach
    void assertCommonAspects() throws Throwable {
        Mockito.verify(clientAspects, Mockito.times(expectedRestTemplateRetry)).handleRestTemplateRetry(Mockito.any());
        Mockito.verify(clientAspects, Mockito.times(expectedCtsException)).handleCtsException(Mockito.any());
    }

    @Test
    public void getNrDuNodeCount() {
        stubForNormal(StubType.ALL_NR_DU_NODES_COUNT);
        Long count = ctsService.getNrDuNodeCount();
        assertEquals(123L, count);
    }

    @Test
    public void getAllNrDuNodes() {
        stubForNormal(StubType.ALL_NR_DU_NODES);
        List<Gnbdu> nodes = ctsService.getAllNrDuNodes();
        assertEquals(2, nodes.size());
    }

    @Test
    public void getNrDuNodeById() {
        stubForNormal(StubType.ONE_NR_DU_NODE);
        Gnbdu node = ctsService.getNrDuNode(6L);
        assertEquals(6L, node.getId());
    }

    @Test
    public void getNrCellCount() {
        stubForNormal(StubType.ALL_NR_CELLS_COUNT);
        Long count = ctsService.getNrCellCount();
        assertEquals(123L, count);
    }

    @Test
    public void getAllNrCells() {
        stubForNormal(StubType.ALL_NR_CELLS);
        List<NrCell> nrCells = ctsService.getAllNrCells();
        assertEquals(6, nrCells.size());
    }

    @Test
    public void getNrCellById() {
        stubForNormal(StubType.ONE_NR_CELL);
        NrCell nrCell = ctsService.getNrCell(3L);
        assertEquals(3L, nrCell.getId());
    }

    @Test
    public void getNrCellByIdWithGnbduAssociation() {
        stubForNormal(StubType.ONE_NR_CELL_WITH_GNBDU);
        NrCell nrCell = ctsService.getNrCellWithAssoc(23L);
        Optional<Gnbdu> gnbdu = CtsUtils.getParentNode(nrCell);
        assertTrue(gnbdu.isPresent());
        assertEquals(23L, nrCell.getId());
        assertEquals(17L, gnbdu.get().getId());
    }

    @Test
    public void getNrCellsByGeo() {
        stubForNormal(StubType.ANY_NR_CELLS_GEO);
        List<NrCell> nrCells = ctsService.getNrCellWithFilters(GEO_DATA);
        assertEquals(3, nrCells.size());
    }

    @Test
    public void getNrCellsByGeoWithFreq() {
        stubForNormal(StubType.ANY_NR_CELLS_GEO_WITH_FREQ);
        List<NrCell> nrCells = ctsService.getNrCellWithFilters(GEO_DATA_WITH_FREQ);
        assertEquals(3, nrCells.size());
    }

    @Test
    public void getNrCellsWithGeoQuery() {
        stubForNormal(StubType.ANY_NR_CELLS_GEO);
        List<NrCell> nrCells = ctsService.getNrCellWithFilters(GEO_QUERY_OBJECT);
        assertEquals(3, nrCells.size());
    }

    @Test
    public void getNrCellWithFiltersCacheKey() {
        stubForNormal(StubType.ANY_NR_CELLS_GEO);
        stubForNormal(StubType.OTHER_NR_CELLS_GEO);

        Object object1 = ctsService.getNrCellWithFilters(GEO_QUERY_OBJECT);
        Object object2 = ctsService.getNrCellWithFilters(GEO_QUERY_OBJECT_OTHER);
        Object object3 = ctsService.getNrCellWithFilters(GEO_QUERY_OBJECT);
        Object object4 = ctsService.getNrCellWithFilters(GEO_QUERY_OBJECT_OTHER);

        assertEquals(2, cacheService.count());
        assertEquals(object1.hashCode(), object3.hashCode());
        assertEquals(object2.hashCode(), object4.hashCode());
        assertNotEquals(object1.hashCode(), object2.hashCode());
        assertNotEquals(object3.hashCode(), object4.hashCode());

        assertEquals(2.0, metricService.findCounter(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, CTS).get().count());
        assertEquals(2.0, metricService.findGauge(CACHE_SIZE, SERVICE, CTS).get().value());

        expectedRestTemplateRetry = 2;
        expectedCtsException = 4;
    }

    @Test
    public void getAllLteNodes() {
        stubForNormal(StubType.ALL_LTE_NODES);
        List<ENodeB> eNodeBs = ctsService.getAllLteNodes();
        assertEquals(3, eNodeBs.size());
    }

    @Test
    public void getLteNodeById() {
        stubForNormal(StubType.ONE_LTE_NODE);
        ENodeB eNodeB = ctsService.getLteNode(3L);
        assertEquals(3L, eNodeB.getId());
    }

    @Test
    public void getLteNodeByIdWithCellsAssociation() {
        stubForNormal(StubType.LTE_NODE_WITH_CELLS);
        ENodeB eNodeB = ctsService.getLteNodeWithCellsAssoc(3L);
        List<LteCell> lteCells = CtsUtils.getChildrenCells(eNodeB).collect(Collectors.toList());
        assertEquals(3L, eNodeB.getId());
        assertEquals(3, lteCells.size());
    }

    @Test
    public void getLteCells() {
        stubForNormal(StubType.ALL_LTE_CELLS);
        List<LteCell> lteCells = ctsService.getAllLteCells();
        assertEquals(3, lteCells.size());
        assertEquals(7L, lteCells.get(0).getId());
    }

    @Test
    public void getLteCellByIdWithGeoSite() {
        stubForNormal(StubType.ONE_LTE_CELL);
        LteCell lteCell = ctsService.getLteCellWithGeographicSitesAssoc(7L);
        assertEquals(7L, lteCell.getId());
        assertEquals(2L, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(lteCell.getGeographicSite()).get(0).getValue()).getId()));
        Mockito.verify(clientAspects, Mockito.times(1)).logMissingGeoSiteField(Mockito.eq(lteCell));
    }

    @Test
    public void getGeographicSiteByIdWithLocation() {
        stubForNormal(CtsServiceTest.StubType.ONE_GEO_SITE);
        GeographicSite site = ctsService.getGeographicSiteWithGeographicLocationsAssoc(1L);
        assertEquals(1L, site.getId());
        assertEquals(List.of(new GeoPoint(33.07502F, -96.83138F, null)), CtsUtils.getGeoPoints(site).collect(Collectors.toList()));
        Mockito.verify(clientAspects, Mockito.times(1)).logMissingGeoLocation(Mockito.eq(site));
    }

    @Test
    public void retryCtsCounterTest() {
        stubForFail(StubType.ONE_GEO_SITE);
        assertThrows(ResourceAccessException.class, () -> ctsService.getGeographicSiteWithGeographicLocationsAssoc(1L));
        assertTrue(metricService.findCounter(RETRY_HTTP_REQUESTS, OBJECT_TYPE, CTS).isPresent());
        assertEquals(3.0, metricService.findCounter(RETRY_HTTP_REQUESTS, OBJECT_TYPE, CTS).get().count());
    }
}
