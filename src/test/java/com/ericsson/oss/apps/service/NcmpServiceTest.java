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

import com.ericsson.oss.apps.config.GatewayProperties;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.*;
import com.ericsson.oss.apps.service.ncmp.FdnService;
import com.ericsson.oss.apps.util.NrcUtil;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.*;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;

@ExtendWith(OutputCaptureExtension.class)

@SpringBootTest(webEnvironment = RANDOM_PORT,
    properties = {
        "gateway.port=${wiremock.server.port}",
        "gateway.services.ncmp.base-path=/ncmp",
        "gateway.retry.maxDelay=1",
        "nrc.cache.max-size=3",
        "nrc.cache.expiry-time=1",
        "gateway.retry.maxDelayEnmOverload=1000"})
@AutoConfigureWireMock(port = 0)
public class NcmpServiceTest {

    private static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    private static final String OPTIONS = "options";
    private static final ExternalId EXTERNAL_ID = ExternalId.of(CM_HANDLE, MANAGED_ELEMENT_RESOURCE_IDENTIFIER);
    private static final String CACHE_KEY = "getResourcesWithOptions,[service=ncmp]," + NrcUtil.externalIdKey(EXTERNAL_ID);
    private static final Map<String, String> DEFAULT_QUERY_PARAMS = Map.of(
        RESOURCE_IDENTIFIER, MANAGED_ELEMENT_RESOURCE_IDENTIFIER
    );

    @AllArgsConstructor
    public enum StubType {
        EXTERNAL_GNODEB_FUNCTION("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "ExternalGNodeBFunctionUnderEnodeBFunction.json"),
        GNBDU_FUNCTION("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "GNBDUFunction.json"),
        GNBDU_TERMPOINT("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "TermPointToGNBUnderExternalGNodeBFunction.json"),
        GUTRAN_CELL("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "ExternalGUtranCellUnderExternalGnodeBFunction.json"),
        GUTRAN_FREQUENCY("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "GUtranSyncSignalFrequencyUnder5GCell.json"),
        LOCAL_SCTP_ENDPOINT("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "LocalSctpEndpoint.json", "fields=erienmnrmgnbcucp:LocalSctpEndpoint/attributes(sctpEndpointRef;interfaceUsed),scope=erienmnrmgnbcucp:LocalSctpEndpoint/attributes(interfaceUsed=X2)"),
        LOCAL_SCTP_ENDPOINT_MULTI_MATCHES("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "LocalSctpEndpoint3Matches.json", "fields=erienmnrmgnbcucp:LocalSctpEndpoint/attributes(sctpEndpointRef;interfaceUsed),scope=erienmnrmgnbcucp:LocalSctpEndpoint/attributes(interfaceUsed=X2)"),
        SCTP_ENDPOINT("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "SctpEndpoint.json", "fields=erienmnrmrtnsctp:SctpEndpoint/attributes(localIpAddress)"),
        NR_CELL_CU("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "NRCellCU.json", "fields=ericsson-enm-gnbcucp:NRCellCU/attributes(nRCellCUId;cellLocalId;pSCellCapable;pLMNIdList)"),
        NR_CELL_CU_NULL_PLMNIDLIST("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "NRCellCUWithNullPLMNIdList.json", "fields=ericsson-enm-gnbcucp:NRCellCU/attributes(nRCellCUId;cellLocalId;pSCellCapable;pLMNIdList)"),
        RESOURCE_PARTITION_MEMBER("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "ResourcePartitionMember.json", "fields=erienmnrmgnbcucp:GNBCUCPFunction/erienmnrmgnbcucp:ResourcePartitions/erienmnrmgnbcucp:ResourcePartition/erienmnrmgnbcucp:ResourcePartitionMember/attributes(endpointResourceRef;pLMNIdList)"),
        IP_ADDRESS_V4("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "AddressIPv4.json", Map.of(RESOURCE_IDENTIFIER, IPV4_EXTERNAL_ID.getResourceIdentifier().toString(),
            OPTIONS, "fields=erienmnrmrtnl3interfaceipv4:AddressIPv4/attributes(address)")),
        IP_ADDRESS_V6("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "AddressIPv6.json", Map.of(RESOURCE_IDENTIFIER, IPV6_EXTERNAL_ID.getResourceIdentifier().toString(),
            OPTIONS, "fields=erienmnrmrtnl3interfaceipv6:AddressIPv6/attributes(address)")),
        ADDITIONAL_PLMN_INFO("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "AdditionalPLMNInfo.json"),
        NCMP_RETRY("/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-operational",
            "NcmpRetry.json");

        private final String path;
        private final String body;
        private final Map<String, String> queryParams;

        StubType(String path, String body) {
            this(path, body, DEFAULT_QUERY_PARAMS);
        }

        StubType(String path, String response, String optionsParam) {
            this(path, response, new HashMap<>());
            this.queryParams.put(OPTIONS, optionsParam);
            queryParams.putAll(DEFAULT_QUERY_PARAMS);
        }

        public MappingBuilder getMappingBuilder(String url, int httpStatus) {
            MappingBuilder builder = get(urlPathEqualTo(url)).willReturn(getResponseBuilder(httpStatus))
                .withHeader(HttpHeaders.ACCEPT, containing(APPLICATION_YANG_DATA_JSON));
            for (Map.Entry<String, String> param : queryParams.entrySet())
                builder = builder.withQueryParam(param.getKey(), new ContainsPattern(param.getValue()));

            return builder;
        }

        public MappingBuilder getMappingBuilderException(String url) {
            MappingBuilder builder = get(urlPathEqualTo(url)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .withHeader(HttpHeaders.ACCEPT, containing(APPLICATION_YANG_DATA_JSON));
            for (Map.Entry<String, String> param : queryParams.entrySet())
                builder = builder.withQueryParam(param.getKey(), new ContainsPattern(param.getValue()));
            return builder;
        }

        private ResponseDefinitionBuilder getResponseBuilder(int httpStatus) {
            return WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, getMediaType())
                .withStatus(httpStatus)
                .withBodyFile(NCMP + BACKSLASH + body);
        }

        private String getMediaType() {
            return body.endsWith(".json") ? MediaType.APPLICATION_JSON_VALUE : MediaType.TEXT_PLAIN_VALUE;
        }
    }

    @Autowired
    private GatewayProperties gatewayProperties;
    @Autowired
    private NcmpService ncmpService;
    @Autowired
    private InMemoryCacheService cacheService;
    @Autowired
    private MetricService metricService;
    @Autowired
    private MeterRegistry meterRegistry;

    private void stubForNormal(StubType stubType) {
        GatewayProperties.Service service = gatewayProperties.getService(NCMP);
        MappingBuilder mapping = stubType.getMappingBuilder(service.getBasePath() + stubType.path, HttpStatus.OK.value());
        createStubFor(service.getHeadersAsMap(), mapping);
    }

    private void stubForFail(StubType stubType) {
        GatewayProperties.Service service = gatewayProperties.getService(NCMP);
        MappingBuilder mapping = stubType.getMappingBuilder(service.getBasePath() + stubType.path,
            HttpStatus.INTERNAL_SERVER_ERROR.value());
        createStubFor(service.getHeadersAsMap(), mapping);
    }

    private void stubForException(StubType stubType) {
        GatewayProperties.Service service = gatewayProperties.getService(NCMP);
        MappingBuilder mapping = stubType.getMappingBuilderException(service.getBasePath() + stubType.path);
        createStubFor(service.getHeadersAsMap(), mapping);
    }

    private void createStubFor(Map<String, String> map, MappingBuilder mappingBuilder) {
        for (Map.Entry<String, String> entry : map.entrySet())
            mappingBuilder = mappingBuilder.withHeader(entry.getKey(), equalTo(entry.getValue()));
        stubFor(mappingBuilder);
    }

    @BeforeEach
    void resetCache() {
        cacheService.clear();
    }

    @Test
    public void testNcmpCache(CapturedOutput output) {
        stubForNormal(StubType.LOCAL_SCTP_ENDPOINT_MULTI_MATCHES);

        List<NcmpObject<LocalSctpEndpoint>> result1 = ncmpService.getResourcesWithOptions(EXTERNAL_ID, FdnService.LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class);
        List<NcmpObject<LocalSctpEndpoint>> result2 = ncmpService.getResourcesWithOptions(EXTERNAL_ID, FdnService.LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class);

        String debugOutput = output.toString();
        assertThat(debugOutput).contains("cacheService PUT: key: " + CACHE_KEY);
        assertThat(debugOutput).contains("cacheService RETURN: key: " + CACHE_KEY);
        Assertions.assertEquals(3, result1.size());
        Assertions.assertEquals(3, result2.size());
    }

    @Test
    public void getExternalGNodeBFunctionsUnderENodeBTest() {
        stubForNormal(StubType.EXTERNAL_GNODEB_FUNCTION);

        List<NcmpObject<ExternalGNodeBFunction>> result = ncmpService.getResources(EXTERNAL_ID, ExternalGNodeBFunction.class);

        Assertions.assertEquals(7, result.size());
    }

    @Test
    public void getGnbduFunctionTest() {
        stubForNormal(StubType.GNBDU_FUNCTION);

        List<NcmpObject<GnbduFunction>> result = ncmpService.getResources(EXTERNAL_ID, GnbduFunction.class);

        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void getTermPointToGNBUnderExternalGNodeBFunctionTest() {
        stubForNormal(StubType.GNBDU_TERMPOINT);

        List<NcmpObject<TermPointToGNB>> result = ncmpService.getResources(EXTERNAL_ID, TermPointToGNB.class);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getExternalGUtranCellUnderExternalGNodeBFunctionTest() {
        stubForNormal(StubType.GUTRAN_CELL);

        List<NcmpObject<ExternalGUtranCell>> result = ncmpService.getResources(EXTERNAL_ID, ExternalGUtranCell.class);

        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void getGUtranSynchSignalFrequencyUnderENodeBFunctionTest() {
        stubForNormal(StubType.GUTRAN_FREQUENCY);

        List<NcmpObject<GUtranSyncSignalFrequency>> result = ncmpService.getResources(EXTERNAL_ID, GUtranSyncSignalFrequency.class);

        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void getLocalSctpEndpointTest() {
        stubForNormal(StubType.LOCAL_SCTP_ENDPOINT);

        List<NcmpObject<LocalSctpEndpoint>> result = ncmpService.getResourcesWithOptions(EXTERNAL_ID, FdnService.LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getLocalSctpEndpointTestMultiMatches() {
        stubForNormal(StubType.LOCAL_SCTP_ENDPOINT_MULTI_MATCHES);

        List<NcmpObject<LocalSctpEndpoint>> result = ncmpService.getResourcesWithOptions(EXTERNAL_ID, FdnService.LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class);

        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void getSctpEndpointTest() {
        stubForNormal(StubType.SCTP_ENDPOINT);

        List<NcmpObject<SctpEndpoint>> result = ncmpService.getResources(EXTERNAL_ID, SctpEndpoint.class);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void getNrCellCUTest() {
        stubForNormal(StubType.NR_CELL_CU);

        List<NcmpObject<NrCellCU>> nrCellCUObjectList = ncmpService.getResources(EXTERNAL_ID, NrCellCU.class);

        Assertions.assertEquals(1, nrCellCUObjectList.size());
    }

    @Test
    public void getNrCellCUWithOptionsTest() {
        stubForNormal(StubType.NR_CELL_CU);

        List<NcmpObject<NrCellCU>> nrCellCUObjectList = ncmpService.getResourcesWithOptions(EXTERNAL_ID,
            NrCellCU.builder().cellLocalId(NR_CELL_111.getLocalCellIdNci()).build(), NrCellCU.class);

        Assertions.assertEquals(1, nrCellCUObjectList.size());
        Assertions.assertEquals(1, nrCellCUObjectList.get(0).getAttributes().getPLMNIdList().size());
        Assertions.assertEquals(240, nrCellCUObjectList.get(0).getAttributes().getPLMNIdList().get(0).getMcc());
        Assertions.assertEquals(80, nrCellCUObjectList.get(0).getAttributes().getPLMNIdList().get(0).getMnc());
    }

    @Test
    public void getNrCellCUWithOptionsNullPLMNIdListTest() {
        stubForNormal(StubType.NR_CELL_CU_NULL_PLMNIDLIST);

        List<NcmpObject<NrCellCU>> nrCellCUObjectList = ncmpService.getResourcesWithOptions(EXTERNAL_ID,
            NrCellCU.builder().cellLocalId(NR_CELL_111.getLocalCellIdNci()).build(), NrCellCU.class);

        Assertions.assertEquals(1, nrCellCUObjectList.size());
        Assertions.assertNull(nrCellCUObjectList.get(0).getAttributes().getPLMNIdList());
    }

    @Test
    public void getIpAddressTest() {
        stubForNormal(StubType.IP_ADDRESS_V4);
        stubForNormal(StubType.IP_ADDRESS_V6);

        List.of(IPV4_EXTERNAL_ID, IPV6_EXTERNAL_ID).forEach(e -> {
            Optional<NcmpObject<IpAddress>> address = ncmpService.getResource(IPV4_EXTERNAL_ID, IpAddress.class);
            Assertions.assertTrue(address.isPresent());
        });
    }

    @Test
    public void getResourcePartitionManagerTest() {
        stubForNormal(StubType.RESOURCE_PARTITION_MEMBER);

        List<NcmpObject<ResourcePartitionMember>> ncmpObjects = ncmpService.getResources(EXTERNAL_ID, ResourcePartitionMember.class);

        Assertions.assertEquals(7, ncmpObjects.size());

        Assertions.assertEquals("ERR_0", ncmpObjects.get(0).getId());
        Assertions.assertNull(ncmpObjects.get(0).getAttributes().getEndpointResourceRef());
        Assertions.assertNotNull(ncmpObjects.get(0).getAttributes().getPLMNIdList());
        Assertions.assertEquals(0, ncmpObjects.get(0).getAttributes().getPLMNIdList().get(0).getMcc());
        Assertions.assertEquals(0, ncmpObjects.get(0).getAttributes().getPLMNIdList().get(0).getMnc());

        Assertions.assertEquals("ERR_1", ncmpObjects.get(1).getId());
        Assertions.assertNotNull(ncmpObjects.get(1).getAttributes().getEndpointResourceRef());
        Assertions.assertNull(ncmpObjects.get(1).getAttributes().getPLMNIdList());

        Assertions.assertEquals("ERR_2", ncmpObjects.get(2).getId());
        Assertions.assertNotNull(ncmpObjects.get(2).getAttributes().getEndpointResourceRef());
        Assertions.assertNotNull(ncmpObjects.get(2).getAttributes().getPLMNIdList());
        Assertions.assertEquals(128, ncmpObjects.get(2).getAttributes().getPLMNIdList().get(0).getMcc());
        Assertions.assertEquals(49, ncmpObjects.get(2).getAttributes().getPLMNIdList().get(0).getMnc());

        Assertions.assertEquals("ERR_3", ncmpObjects.get(3).getId());
        Assertions.assertNotNull(ncmpObjects.get(3).getAttributes().getEndpointResourceRef());
        Assertions.assertNull(ncmpObjects.get(3).getAttributes().getPLMNIdList());
    }

    @Test
    public void getResourceWithOptionsExceptionTest() {
        stubForException(StubType.LOCAL_SCTP_ENDPOINT);
        List<NcmpObject<LocalSctpEndpoint>> result = ncmpService.getResourcesWithOptions(EXTERNAL_ID, FdnService.LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void createResource() {
        ResponseEntity<Object> result = ncmpService.createResource(EXTERNAL_ID, toNcmpObject(LOCAL_SCTP_ENDPOINT_X2));

        Assertions.assertEquals(CREATED, result.getStatusCode());
    }

    @Test
    public void createResourceCacheCleanup() {
        stubForNormal(StubType.GUTRAN_CELL);
        Assertions.assertEquals(0, cacheService.count());
        Assertions.assertEquals(0, cacheService.count(NrcUtil.externalIdKey(EXTERNAL_ID)));
        Assertions.assertFalse(metricService.findCounter(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, NCMP)
            .isPresent());
        if (metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).isPresent()) {
            // if this unit test is the first the gauge is null, otherwise it is cleaned up (== 0.0) with resetCache()
            Assertions.assertEquals(0.0, metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).get().value());
        }

        ncmpService.getResources(EXTERNAL_ID, LocalSctpEndpoint.class);
        Assertions.assertEquals(1, cacheService.count());
        Assertions.assertEquals(1, cacheService.count(NrcUtil.externalIdKey(EXTERNAL_ID)));
        Assertions.assertFalse(metricService.findCounter(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, NCMP)
            .isPresent());
        Assertions.assertEquals(1.0, metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).get().value());

        ncmpService.getResources(EXTERNAL_ID, LocalSctpEndpoint.class);
        Assertions.assertEquals(1, cacheService.count());
        Assertions.assertEquals(1, cacheService.count(NrcUtil.externalIdKey(EXTERNAL_ID)));
        Assertions.assertEquals(1.0, metricService.findCounter(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, NCMP).get()
            .count());
        Assertions.assertEquals(1.0, metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).get().value());

        ncmpService.getResources(EXTERNAL_ID, LocalSctpEndpoint.class);
        Assertions.assertEquals(1, cacheService.count());
        Assertions.assertEquals(1, cacheService.count(NrcUtil.externalIdKey(EXTERNAL_ID)));
        Assertions.assertEquals(2.0, metricService.findCounter(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, NCMP).get()
            .count());
        Assertions.assertEquals(1.0, metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).get().value());

        // The createResource will clean up the objects from the cache that are related with the external ID
        ncmpService.createResource(EXTERNAL_ID, toNcmpObject(LOCAL_SCTP_ENDPOINT_X2));
        Assertions.assertEquals(0, cacheService.count());
        Assertions.assertEquals(0, cacheService.count(NrcUtil.externalIdKey(EXTERNAL_ID)));
        Assertions.assertEquals(2.0, metricService.findCounter(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, SERVICE, NCMP).get()
            .count());
        Assertions.assertEquals(0.0, metricService.findGauge(CACHE_SIZE, SERVICE, NCMP).get().value());
    }

    @Test
    public void createResourcesException() {
        Assertions.assertThrows(Exception.class, () -> ncmpService.createResources(EXTERNAL_ID, null));
    }

    @Test
    public void getAdditionalPLMNInfoTest() {
        stubForNormal(StubType.ADDITIONAL_PLMN_INFO);

        List<NcmpObject<AdditionalPLMNInfo>> ncmpObjects = ncmpService.getResources(EXTERNAL_ID, AdditionalPLMNInfo.class);

        Assertions.assertEquals(2, ncmpObjects.size());
    }

    @Test
    public void retryNcmpCounterTest() {
        meterRegistry.clear();

        stubForException(StubType.ADDITIONAL_PLMN_INFO);

        ncmpService.getResources(EXTERNAL_ID, AdditionalPLMNInfo.class);

        assertTrue(metricService.findCounter(RETRY_HTTP_REQUESTS, OBJECT_TYPE, NCMP).isPresent());
        Assertions.assertEquals(3.0, metricService.findCounter(RETRY_HTTP_REQUESTS, OBJECT_TYPE, NCMP).get().count());
    }

    @Test
    public void overloadRetryMetricsTest() {
        meterRegistry.clear();

        stubForFail(StubType.NCMP_RETRY);

        ncmpService.getResources(EXTERNAL_ID, AdditionalPLMNInfo.class);

        assertTrue(metricService.findCounter(ENM_ADAPTER_OVERLOAD_RETRY_COUNT).isPresent());
        Assertions.assertEquals(3.0, metricService.findCounter(ENM_ADAPTER_OVERLOAD_RETRY_COUNT).get().count());
    }
}
