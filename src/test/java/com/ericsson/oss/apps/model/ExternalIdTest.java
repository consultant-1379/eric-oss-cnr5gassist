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
package com.ericsson.oss.apps.model;

import com.ericsson.oss.apps.exception.InvalidIdentifierException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.*;

public class ExternalIdTest {

    static final String EXPECTED_RESOURCE_ID = "/node_name-1.0=id_1/name_space-2.0:name-2.0=id_2/name=id_3";

    private static final String EXPECTED_CM_HANDLE = "hash_256";
    private static final String EXTERNAL_ID = EXPECTED_CM_HANDLE + "/node_name-1.0=id_1/name_space-2.0:name-2.0[id=id_2]/name[@id=id_3]";
    private static final List<String> INVALID_IDS = List.of("-Not-Valid-External-Id-", "Not_Valid", "Not_Valid/", "Not_Valid/asd:",
        EXTERNAL_ID + "/invalid[@_id=id_4]");

    @Test
    void validExternalId() {
        ExternalId id = ExternalId.of(EXTERNAL_ID);

        assertNotNull(id);
        assertEquals(EXPECTED_CM_HANDLE, id.getCmHandle());
        assertEquals(EXPECTED_RESOURCE_ID, id.getResourceIdentifier().toString());
    }

    @Test
    void validExternalIdRegEx() {
        ExternalId id1 = ExternalId.of("E53ED082381720CFA346C92EC18DC958/ericsson-enm-ComTop:ManagedElement=NR54gNodeBRadio00030/ericsson-enm-GNBDU:NRCellDU=NR54gNodeBRadio00030-1");
        assertEquals("E53ED082381720CFA346C92EC18DC958", id1.getCmHandle());
        assertEquals("/ericsson-enm-ComTop:ManagedElement=NR54gNodeBRadio00030/ericsson-enm-GNBDU:NRCellDU=NR54gNodeBRadio00030-1", id1.getResourceIdentifier().toString());
    }

    @Test
    void notValidExternalId() {
        INVALID_IDS.forEach(id -> {
            InvalidIdentifierException exception = assertThrows(
                InvalidIdentifierException.class, () -> ExternalId.of(id));

            assertEquals(id, exception.getIdentifier());
        });
    }

    @Test
    void addResourceIdentifierTest() {
        String expectedResult = "hash_256/node_name-1.0=id_1/name_space-2.0:name-2.0=id_2/name=id_3/erienmnrmrtnsctp:SctpEndpoint=0";
        ExternalId id = ExternalId.of(EXTERNAL_ID).add(toNcmpObject(SCTP_ENDPOINT));

        assertEquals(expectedResult, id.toString());
    }

    @Test
    void toFdnTest() {
        assertEquals(PARTIAL_IPV4_FDN, IPV4_EXTERNAL_ID.getResourceIdentifier().toFdn());
    }
}
