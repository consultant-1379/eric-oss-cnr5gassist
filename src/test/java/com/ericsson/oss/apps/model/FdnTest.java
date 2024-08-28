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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.ericsson.oss.apps.util.TestDefaults.*;

public class FdnTest {

    public static final String IPV4_INTERFACE_FDN = "ManagedElement=NR45gNodeBRadio00022,Transport=1,Router=VR_INNER,InterfaceIPv4=NRAT_CP";
    public static final Map.Entry<String, String> IPV4_ADDRESS_MO = Map.entry("AddressIPv4", "1");

    @Test
    public void toResourceIdentifierTest() {
        Assertions.assertEquals(IPV4_EXTERNAL_ID.getResourceIdentifier(), IPV4_FDN.toResourceIdentifier());
    }

    @Test
    public void toStringTest() {
        Assertions.assertEquals(DN_PREFIX_STRING, DN_PREFIX.toString());
    }

    @Test
    public void addTest() {
        Assertions.assertEquals(PARTIAL_IPV4_FDN, Fdn.of(IPV4_INTERFACE_FDN).add(IPV4_ADDRESS_MO));
    }
}
