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

import com.ericsson.oss.apps.model.ncmp.ExternalGNodeBFunction;
import com.ericsson.oss.apps.model.ncmp.ExternalGUtranCell;
import javassist.tools.rmi.ObjectNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.ericsson.oss.apps.util.TestDefaults.*;

public class NcmpAttributeTest {

    @Test
    void externalGNodeBFunction() throws ObjectNotFoundException {
        Assertions.assertEquals(EXTERNAL_G_NODE_B_FUNCTION_ATTRIBUTES, new ExternalGNodeBFunction(EXTERNAL_GNODEB_FUNCTION_ID, GNBDU_FUNCTION, GNBDU, NCMP_OBJECT_ID_NR45));
    }

    @Test
    void externalGUtranCell() {
        Assertions.assertEquals(EXTERNAL_G_UTRAN_CELL, new ExternalGUtranCell(EXTERNAL_GUTRAN_CELL_ID, NR_CELL_111, G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF, Collections.singletonList(PLMNID)));
    }
}
