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
import com.ericsson.oss.apps.util.CtsUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.TestDefaults.GEO_DATA;
import static com.ericsson.oss.apps.util.TestDefaults.GEO_DATA_WITH_FREQ;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"gateway.port=9091", "gateway.services.cts.base-path="})
@AutoConfigureStubRunner(stubsMode = StubRunnerProperties.StubsMode.REMOTE,
    repositoryRoot = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-dev-local",
    ids = "com.ericsson.oss.apps.stubs:eric-oss-cnr5gassist:+:stubs:9091")
public class CtsContractTest {

    @Autowired
    private CtsService ctsService;

    @Test
    public void getNrCellByIdWithGnbduAssociation() {
        NrCell nrCell = ctsService.getNrCellWithAssoc(26L);
        Optional<Gnbdu> gnbdu = CtsUtils.getParentNode(nrCell);
        assertTrue(gnbdu.isPresent());
        assertEquals(26L, nrCell.getId());
        assertEquals(23L, gnbdu.get().getId());
    }

    @Test
    public void getAllLteNodes() {
        List<ENodeB> eNodeBs = ctsService.getAllLteNodes();
        assertEquals(1, eNodeBs.size());
    }

    @Test
    public void getNrCellsByGeo() {
        List<NrCell> nrCells = ctsService.getNrCellWithFilters(GEO_DATA);
        assertEquals(1, nrCells.size());
    }

    @Test
    public void getNrCellsByGeoWithFreq() {
        List<NrCell> nrCells = ctsService.getNrCellWithFilters(GEO_DATA_WITH_FREQ);
        assertEquals(1, nrCells.size());
    }

    @Test
    public void getLteNodeByIdWithCellsAssociation() {
        ENodeB eNodeB = ctsService.getLteNodeWithCellsAssoc(29L);
        List<LteCell> lteCells = CtsUtils.getChildrenCells(eNodeB).collect(Collectors.toList());
        assertEquals(29L, eNodeB.getId());
        assertEquals(3, lteCells.size());
    }
}
