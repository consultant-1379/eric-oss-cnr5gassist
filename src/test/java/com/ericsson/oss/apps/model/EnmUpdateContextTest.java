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

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnmUpdateContextTest {

    @Test
    void testSimpleEnmUpdateContext() {
        EnmUpdateContext enmUpdateContext = new EnmUpdateContext(NRC_TASK_ONGOING, E_NODE_B, GNBDU);

        assertEquals(NRC_TASK_ONGOING, enmUpdateContext.getNrcTask());
        assertEquals(E_NODE_B, enmUpdateContext.getENodeB());
        assertEquals(GNBDU, enmUpdateContext.getGnbdu());
    }
}