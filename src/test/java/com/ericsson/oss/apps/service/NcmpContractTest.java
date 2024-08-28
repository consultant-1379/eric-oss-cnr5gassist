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

import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ResourceIdentifier;
import com.ericsson.oss.apps.model.ncmp.*;
import com.ericsson.oss.apps.service.ncmp.IpAddressFinder;
import javassist.tools.rmi.ObjectNotFoundException;
import org.apache.http.conn.util.InetAddressUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CREATED;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "gateway.port=9091",
    "gateway.services.ncmp.base-path=/ncmp"})
@AutoConfigureStubRunner(stubsMode = StubRunnerProperties.StubsMode.REMOTE,
    repositoryRoot = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-release-local",
    ids = "com.ericsson.oss.internaltools.stub:eric-oss-ncmp-stub:+:stubs:9091")
public class NcmpContractTest {

    private static final ExternalId ENODEB_FUNCTION_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmlrat:ManagedElement[@id=1]/erienmnrmlrat:ENodeBFunction=1");
    private static final ExternalId GUTRAN_NETWORK_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmlrat:ENodeBFunction=1/erienmnrmlrat:GUtraNetwork=1");
    private static final ExternalId EXTERNAL_GNODEB_FUNCTION_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmlrat:ENodeBFunction=1/erienmnrmlrat:GUtraNetwork=1/erienmnrmlrat:ExternalGNodeBFunction=TownCentreNode");
    private static final ExternalId NRCELLCU_FUNCTION_ID = ExternalId.of(CM_HANDLE,
        new ResourceIdentifier());
    private static final ExternalId ADDITIONALPLMN_FUNCTION_ID = ExternalId.of(CM_HANDLE,
        "/_3gpp-common-managed-element:ManagedElement=NRGNB02_Lake/ericsson-enm-gnbdu:GNBDUFunction=1/ericsson-enm-gnbdu:NRCellDU=NRGNB02_Lake_03");

    @Autowired
    private NcmpService ncmpService;
    @Autowired
    private IpAddressFinder ipAddressFinder;

    @Test
    public void getExternalGNodeBFunctionsUnderENodeB() {
        List<NcmpObject<ExternalGNodeBFunction>> eNodeBs = ncmpService.getResources(ENODEB_FUNCTION_ID, ExternalGNodeBFunction.class);
        assertEquals(1, eNodeBs.size());
    }

    @Test
    public void getTermPointToGNBUnderExternalGNodeBFunction() {
        List<NcmpObject<TermPointToGNB>> termPointToGNBs = ncmpService.getResources(EXTERNAL_GNODEB_FUNCTION_ID, TermPointToGNB.class);
        assertEquals(1, termPointToGNBs.size());
    }

    @Test
    public void getGUtranSyncSignalFrequencyUnderENodeBFunction() {
        GUtranSyncSignalFrequency scopeValue = GUtranSyncSignalFrequency.builder().arfcn(5).build();
        List<NcmpObject<GUtranSyncSignalFrequency>> syncFrequencies = ncmpService.getResourcesWithOptions(GUTRAN_NETWORK_ID, scopeValue, GUtranSyncSignalFrequency.class);
        assertEquals(1, syncFrequencies.size());
    }

    @Test
    public void getExternalGUtranCellUnderExternalGNodeBFunction() {
        ExternalGUtranCell scopeValue = ExternalGUtranCell.builder().localCellId(1).build();
        List<NcmpObject<ExternalGUtranCell>> externalGUtranCells = ncmpService.getResourcesWithOptions(EXTERNAL_GNODEB_FUNCTION_ID, scopeValue, ExternalGUtranCell.class);
        assertEquals(1, externalGUtranCells.size());
    }

    @Test
    public void getNrCellCU() {
        NrCellCU scopeValue = NrCellCU.builder().cellLocalId(1L).build();
        List<NcmpObject<NrCellCU>> nrCellCU = ncmpService.getResourcesWithOptions(NRCELLCU_FUNCTION_ID, scopeValue, NrCellCU.class);
        assertEquals(1, nrCellCU.size());
    }

    @Test
    public void getAdditionalPLMNInfo() {
        List<NcmpObject<AdditionalPLMNInfo>> additionalPLMNInfo= ncmpService.getResources(ADDITIONALPLMN_FUNCTION_ID, AdditionalPLMNInfo.class);
        assertEquals(2, additionalPLMNInfo.size());
    }

    @Test
    public void getNrSectorCarrier(){
        List<NcmpObject<SectorCarrier>> nrSectorCarrier = ncmpService.getResources(NR_SECTOR_CARRIER_EXTERNAL_ID, SectorCarrier.class);
        assertEquals(1,nrSectorCarrier.size());
    }

    @Test
    public void createGUtraNetwork() {
        NcmpObject<NcmpAttribute> ncmpObject = toNcmpObject(GUtraNetwork.builder().gUtraNetworkId(String.valueOf(ENODEB_FUNCTION_ID)).userLabel("2").build());
        ResponseEntity<Object> response = ncmpService.createResource(ENODEB_FUNCTION_ID, ncmpObject);
        assertEquals(CREATED, response.getStatusCode());
    }

    @Test
    public void createExternalGNodeBFunction() {
        NcmpObject<NcmpAttribute> ncmpObject = toNcmpObject(ExternalGNodeBFunction.builder().externalGNodeBFunctionId(String.valueOf(GUTRAN_NETWORK_ID)).gNodeBId(2).gNodeBIdLength(1).build());
        ResponseEntity<Object> response = ncmpService.createResource(GUTRAN_NETWORK_ID, ncmpObject);
        assertEquals(CREATED, response.getStatusCode());
    }

    @Test
    public void createTermPointToGNB() {
        NcmpObject<NcmpAttribute> ncmpObject = toNcmpObject(TermPointToGNB.builder().termPointToGNBId(String.valueOf(EXTERNAL_GNODEB_FUNCTION_ID)).ipAddress("2.33.2.3/23").build());
        ResponseEntity<Object> response = ncmpService.createResource(EXTERNAL_GNODEB_FUNCTION_ID, ncmpObject);
        assertEquals(CREATED, response.getStatusCode());
    }

    @Test
    public void createExternalGUtranCell() {
        PlmnId plmnId = new PlmnId(2, 216, 30);
        NcmpObject<NcmpAttribute> ncmpObject = toNcmpObject(ExternalGUtranCell.builder().externalGUtranCellId(String.valueOf(EXTERNAL_GNODEB_FUNCTION_ID)).localCellId(3).plmnIdList(List.of(plmnId)).isRemoveAllowed("true").build());
        ResponseEntity<Object> response = ncmpService.createResource(EXTERNAL_GNODEB_FUNCTION_ID, ncmpObject);
        assertEquals(CREATED, response.getStatusCode());
    }

    @Test
    public void getIpAddress() throws ObjectNotFoundException {
        String ipAddress = ipAddressFinder.getIpForManagedElement(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION);
        assertTrue(InetAddressUtils.isIPv4Address(ipAddress));
    }
}
