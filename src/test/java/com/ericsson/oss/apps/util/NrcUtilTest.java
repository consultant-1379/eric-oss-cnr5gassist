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

package com.ericsson.oss.apps.util;

import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.api.model.NrcTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NrcUtilTest {

    private static final NrcRequest NRC_REQUEST = NrcRequest.builder().build();
    private static final NrcTask NRC_TASK = NrcUtil.generateNrcTask(NRC_REQUEST);

    @Test
    public void generateNrcProcess() {
        assertEquals(NRC_REQUEST, NRC_TASK.getRequest());
        assertNotNull(NRC_TASK.getProcess());
        assertNotNull(NRC_TASK.getProcess().getId());
        assertNotNull(NRC_TASK.getProcess().getHour());
        assertNotNull(NRC_TASK.getProcess().getMinute());
    }
}
