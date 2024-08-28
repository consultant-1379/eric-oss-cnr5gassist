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

package com.ericsson.oss.apps.service.ncmp;

import com.ericsson.oss.apps.api.model.EnmUpdate;
import com.ericsson.oss.apps.api.model.NrcProcessStatus;
import com.ericsson.oss.apps.api.model.NrcTask;
import javassist.tools.rmi.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnmUpdateMonitorServiceTest {

    public static final String CREATE_EXTERNAL_GU_TRAN_CELL = "CreateExternalGUtranCell";
    public static final String CREATE_EXTERNAL_G_NODE_B_FUNCTION = "CreateExternalGNodeBFunction";
    public static final String CREATE_TERM_POINT = "CreateTermPoint";

    @InjectMocks
    private EnmUpdateMonitorService enmUpdateMonitorService;

    @BeforeEach
    void setUp() {
        ENM_UPDATE_CONTEXT.getNrcTask().setEnmUpdates(new ArrayList<>());
    }

    @Test
    void TestUpdateSucceededWhenNrCellIsNull() {
        enmUpdateMonitorService.updateSucceeded(ENM_UPDATE_CONTEXT, CREATE_EXTERNAL_G_NODE_B_FUNCTION);

        NrcTask nrcTask = ENM_UPDATE_CONTEXT.getNrcTask();
        assertEquals(1, nrcTask.getEnmUpdates().size());

        EnmUpdate actualEnmUpdate = nrcTask.getEnmUpdates().get(0);

        assertEquals(E_NODE_B.getId(), actualEnmUpdate.geteNodeBId());
        assertEquals(GNBDU.getGnbduId(), actualEnmUpdate.getgNodeBDUId());
        assertEquals(CREATE_EXTERNAL_G_NODE_B_FUNCTION, actualEnmUpdate.getOperation());
        assertEquals(NrcProcessStatus.SUCCEEDED, actualEnmUpdate.getStatus());
    }

    @Test
    void TestUpdateSucceededWhenNrCellIsNotNull() {
        enmUpdateMonitorService.updateSucceeded(ENM_UPDATE_CONTEXT, CREATE_EXTERNAL_G_NODE_B_FUNCTION, NR_CELL_111);
        enmUpdateMonitorService.updateSucceeded(ENM_UPDATE_CONTEXT, CREATE_TERM_POINT, NR_CELL_111);

        NrcTask nrcTask = ENM_UPDATE_CONTEXT.getNrcTask();
        assertEquals(2, nrcTask.getEnmUpdates().size());

        EnmUpdate actualEnmUpdate1 = nrcTask.getEnmUpdates().get(0);
        EnmUpdate actualEnmUpdate2 = nrcTask.getEnmUpdates().get(1);

        assertEquals(E_NODE_B.getId(), actualEnmUpdate1.geteNodeBId());
        assertEquals(GNBDU.getGnbduId(), actualEnmUpdate1.getgNodeBDUId());
        assertEquals(CREATE_TERM_POINT, actualEnmUpdate2.getOperation());
        assertEquals(CREATE_EXTERNAL_G_NODE_B_FUNCTION, actualEnmUpdate1.getOperation());
        assertEquals(NrcProcessStatus.SUCCEEDED, actualEnmUpdate1.getStatus());
        assertEquals(NR_CELL_111.getName(), actualEnmUpdate1.getName());
    }

    @Test
    void TestUpdateFailedWhenNrCellIsNull() {
        NullPointerException nullPointerException = new NullPointerException();
        enmUpdateMonitorService.updateFailed(ENM_UPDATE_CONTEXT, CREATE_EXTERNAL_G_NODE_B_FUNCTION
            , nullPointerException.getMessage());

        NrcTask nrcTask = ENM_UPDATE_CONTEXT.getNrcTask();

        assertEquals(1, nrcTask.getEnmUpdates().size());
        assertEquals(NrcProcessStatus.FAILED, nrcTask.getProcess().getEnmUpdateStatus());

        EnmUpdate actualEnmUpdate = nrcTask.getEnmUpdates().get(0);

        assertEquals(E_NODE_B.getId(), actualEnmUpdate.geteNodeBId());
        assertEquals(GNBDU.getGnbduId(), actualEnmUpdate.getgNodeBDUId());
        assertEquals(CREATE_EXTERNAL_G_NODE_B_FUNCTION, actualEnmUpdate.getOperation());
        assertEquals(NrcProcessStatus.FAILED, actualEnmUpdate.getStatus());
    }

    @Test
    void TestUpdateFailedWhenNrCellIsNotNull() {
        ObjectNotFoundException objectNotFoundException = new ObjectNotFoundException("Object");
        enmUpdateMonitorService.updateFailed(ENM_UPDATE_CONTEXT, CREATE_EXTERNAL_GU_TRAN_CELL
            , NR_CELL_111, objectNotFoundException.getMessage());
        enmUpdateMonitorService.updateFailed(ENM_UPDATE_CONTEXT, CREATE_EXTERNAL_GU_TRAN_CELL
            , NR_CELL_111, objectNotFoundException.getMessage());

        NrcTask nrcTask = ENM_UPDATE_CONTEXT.getNrcTask();

        assertEquals(2, nrcTask.getEnmUpdates().size());

        EnmUpdate actualEnmUpdate1 = nrcTask.getEnmUpdates().get(0);
        EnmUpdate actualEnmUpdate2 = nrcTask.getEnmUpdates().get(1);

        assertEquals(CREATE_EXTERNAL_GU_TRAN_CELL, actualEnmUpdate2.getOperation());
        assertEquals(CREATE_EXTERNAL_GU_TRAN_CELL, actualEnmUpdate1.getOperation());
        assertEquals(NrcProcessStatus.FAILED, actualEnmUpdate1.getStatus());
        assertEquals(E_NODE_B.getId(), actualEnmUpdate1.geteNodeBId());
        assertEquals(GNBDU.getGnbduId(), actualEnmUpdate1.getgNodeBDUId());
        assertTrue(actualEnmUpdate1.getError().contains("Object"));
    }
}