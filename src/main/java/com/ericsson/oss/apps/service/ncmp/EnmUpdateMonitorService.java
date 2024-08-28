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
import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ExternalId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EnmUpdateMonitorService {

    public void updateSucceeded(EnmUpdateContext enmUpdateContext, String operation) {
        updateSucceeded(enmUpdateContext, operation, null);
    }

    public void updateSucceeded(EnmUpdateContext enmUpdateContext, String operation, NrCell nrCell) {
        EnmUpdate update = createEnmUpdate(enmUpdateContext, operation, nrCell, NrcProcessStatus.SUCCEEDED, null);
        appendEnmUpdate(enmUpdateContext, update);
    }

    public void updateFailed(EnmUpdateContext enmUpdateContext, String operation, String error) {
        updateFailed(enmUpdateContext, operation, null, error);
    }

    public void updateFailed(EnmUpdateContext enmUpdateContext, String operation, NrCell nrCell, String error) {
        EnmUpdate failedUpdate = createEnmUpdate(enmUpdateContext, operation, nrCell, NrcProcessStatus.FAILED, error);
        appendEnmUpdate(enmUpdateContext, failedUpdate);

        enmUpdateContext.getNrcTask().getProcess().setEnmUpdateStatus(NrcProcessStatus.FAILED);
    }

    private EnmUpdate createEnmUpdate(EnmUpdateContext enmUpdateContext, String operation, NrCell nrCell, NrcProcessStatus failed, String error) {
        EnmUpdate enmUpdate = EnmUpdate.builder()
            .eNodeBId(enmUpdateContext.getENodeB().getId())
            .gNodeBDUId(enmUpdateContext.getGnbdu().getGnbduId())
            .name(getName(enmUpdateContext, nrCell))
            .operation(operation)
            .status(failed)
            .error(error)
            .build();

        if (nrCell != null) {
            enmUpdate.setNrCellId(nrCell.getId());
        }

        return enmUpdate;
    }

    private String getName(EnmUpdateContext enmUpdateContext, NrCell nrCell) {
        return nrCell != null ? nrCell.getName() : ExternalId.of(enmUpdateContext.getGnbdu().getExternalId()).getResourceIdentifier().getFirst().getValue();
    }

    private void appendEnmUpdate(EnmUpdateContext enmUpdateContext, EnmUpdate update) {
        enmUpdateContext.getNrcTask().addEnmUpdatesItem(update);
    }
}