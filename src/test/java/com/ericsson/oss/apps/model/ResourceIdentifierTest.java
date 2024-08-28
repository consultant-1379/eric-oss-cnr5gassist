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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceIdentifierTest {

    public static final ResourceIdentifier RESOURCE_ID = ResourceIdentifier.of(ExternalIdTest.EXPECTED_RESOURCE_ID);
    private static final String EXPECTED_ANCESTOR_ID = "/node_name-1.0=id_1/name_space-2.0:name-2.0=id_2";
    private static final String ANCESTOR_NAMESPACE = "name_space-2.0";
    private static final String ANCESTOR_NAME = "name-2.0";
    private static final String INVALID_ANCESTOR_NAME = "invalid-ancestor";
    private static final String NEW_RESOURCE = "/new_resource=new_id";

    @Test
    void getAncestor() {
        List.of(RESOURCE_ID.getAncestorIdByFullName(ANCESTOR_NAMESPACE, ANCESTOR_NAME),
            RESOURCE_ID.getAncestorIdByName(ANCESTOR_NAME)
        ).forEach(ancestor -> {
            assertTrue(ancestor.isPresent());
            assertEquals(EXPECTED_ANCESTOR_ID, ancestor.get().toString());
        });
    }

    @Test
    void invalidAncestor() {
        assertFalse(RESOURCE_ID.getAncestorIdByName(INVALID_ANCESTOR_NAME).isPresent());
    }

    @Test
    void addChildrenToExistingResourceTest() {
        ResourceIdentifier newResource = RESOURCE_ID.addAll(ResourceIdentifier.of(NEW_RESOURCE));
        assertEquals(RESOURCE_ID + NEW_RESOURCE, newResource.toString());
    }
}
