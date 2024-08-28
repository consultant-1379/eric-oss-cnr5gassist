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

import com.ericsson.oss.apps.api.model.NrcProcessStatus;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.client.cts.model.ENodeB;
import com.ericsson.oss.apps.client.cts.model.Gnbdu;
import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ncmp.GnbduFunction;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.SigtermService;
import com.ericsson.oss.apps.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.NRC_FOUND_NEIGHBOURING_NODES_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class NeighboursPersistingService {

    private final EnmUpdateMonitorService enmUpdateMonitorService;
    private final GUtraNetworkUpdater gUtraNetworkUpdater;
    private final NcmpCounterService ncmpCounterService;
    private final EnmUpdateContextService enmUpdateContextService;
    private final SigtermService sigtermService;

    public void createInEnm(NrcTask nrcTask, ENodeB eNodeB, Map.Entry<Gnbdu, List<NrCell>> groups) {
        if (sigtermService.isSigterm()) {
            log.warn("Enm modifications are stopped because sigterm signal was initiated");
            return;
        }
        log.info("grouping to be saved:  eNodeB {}, groups {}", StringUtil.toJson(eNodeB), StringUtil.toJson(groups));

        nrcTask.getProcess().setEnmUpdateStatus(NrcProcessStatus.ONGOING);

        Gnbdu gnbdu = groups.getKey();
        List<NrCell> nrCells = groups.getValue();
        EnmUpdateContext enmUpdateContext = new EnmUpdateContext(nrcTask, eNodeB, gnbdu);
        Optional<NcmpObject<GnbduFunction>> optionalGnbduFunction = enmUpdateContextService.getOptionalGnbduFunction(enmUpdateContext);
        if (optionalGnbduFunction.isPresent()) {
            ncmpCounterService.incrementCounter(NRC_FOUND_NEIGHBOURING_NODES_COUNT);
            gUtraNetworkUpdater.update(nrCells, optionalGnbduFunction.get().getAttributes(), enmUpdateContext);
        } else {
            log.error("Failed to read the GnbduFunction in NCMP for: {}. Cannot process these neighbours for the ENodeB: {}", StringUtil.toJson(gnbdu), StringUtil.toJson(eNodeB));
            enmUpdateMonitorService.updateFailed(enmUpdateContext, "ReadGnbduFunctionNCMP", "Failed to read");
        }

        nrcTask.getProcess().setEnmUpdateStatus(
            nrcTask.getProcess().getEnmUpdateStatus().equals(NrcProcessStatus.ONGOING) ?
                NrcProcessStatus.SUCCEEDED : NrcProcessStatus.FAILED);
    }
}
