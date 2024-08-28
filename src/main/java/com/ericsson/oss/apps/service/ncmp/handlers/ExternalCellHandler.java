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
package com.ericsson.oss.apps.service.ncmp.handlers;

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.client.cts.model.NrSectorCarrier;
import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.Fdn;
import com.ericsson.oss.apps.model.ncmp.ExternalGUtranCell;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.PlmnId;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.NcmpService;
import com.ericsson.oss.apps.service.ncmp.EnmUpdateMonitorService;
import com.ericsson.oss.apps.service.ncmp.FdnService;
import com.ericsson.oss.apps.util.CtsUtils;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP_EXTERNALGUTRANCELL_OBJECT_COUNT;
import static com.ericsson.oss.apps.util.Constants.NCMP_MISSING_NEIGHBOURS_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalCellHandler {

    public static final String CREATE_EXTERNAL_GUTRAN_CELL = " CreateExternalGUtranCell";

    private final NcmpService ncmpService;
    private final FdnService fdnService;
    private final PlmnIdListGrouping plmnIdListGrouping;
    private final NcmpCounterService ncmpCounterService = new NcmpCounterService();
    private final EnmUpdateMonitorService enmUpdateMonitorService;
    private final MetricService metricService;

    public Optional<ExternalId> read(ExternalId externalGNodeBFunctionId, @NonNull Long localCellIdNci) {
        return ncmpService.getResourcesWithOptions(externalGNodeBFunctionId,
                buildExternalGUtranCellAttrs(localCellIdNci), ExternalGUtranCell.class).stream().findAny()
            .map(externalGNodeBFunctionId::add);
    }

    public Optional<NcmpObject<NcmpAttribute>> create(ExternalId externalGNodeBFunctionId, NrCell nrCell, EnmUpdateContext enmUpdateContext) {
        try {
            log.info("Creating an ExternalGUtranCell instance for the NRCell: {} under the {}", nrCell.getExternalId(), externalGNodeBFunctionId);
            Optional<NrSectorCarrier> optionalNrSectorCarrier = CtsUtils.getNrSectorCarrier(nrCell);
            if (optionalNrSectorCarrier.isPresent()) {
                Fdn gUtranSyncSignalFrequencyRef = fdnService.getGUtranSyncSignalFrequencyFdn(externalGNodeBFunctionId.getParent(), optionalNrSectorCarrier.get().getArfcnDL());
                NcmpObject<NcmpAttribute> externalGUtranCell = buildExternalGUtranCell(nrCell, gUtranSyncSignalFrequencyRef, plmnIdListGrouping.getPlmnIdList(nrCell));
                ncmpService.createResource(externalGNodeBFunctionId, externalGUtranCell);
                enmUpdateMonitorService.updateSucceeded(enmUpdateContext, CREATE_EXTERNAL_GUTRAN_CELL, nrCell);
                ncmpCounterService.incrementCounter(NCMP_EXTERNALGUTRANCELL_OBJECT_COUNT);
                metricService.increment(NCMP_OBJECT_COUNT, NCMP_OBJECT, EXTERNALGUTRANCELL);
                metricService.increment(NCMP_MISSING_NEIGHBOURS_COUNT);

                return Optional.of(externalGUtranCell);
            } else {
                log.error("Unable to create the ExternalGUtranCell under the ExternalGNodeBFunction: {}. Failed to find the NRSectorCarrier for the nrCell: {}", externalGNodeBFunctionId, nrCell.getExternalId());
                enmUpdateMonitorService.updateFailed(enmUpdateContext, CREATE_EXTERNAL_GUTRAN_CELL, nrCell,
                    MessageFormat.format("Failed to find the NRSectorCarrier for the nrCell: {0}", nrCell.getExternalId()));
            }
        } catch (ObjectNotFoundException | RestClientException e) {
            log.error("Unable to create the ExternalGUtranCell for the NRCell: {} under the ExternalGNodeBFunction: {}.  Error: {}", nrCell.getExternalId(), externalGNodeBFunctionId, e);
            enmUpdateMonitorService.updateFailed(enmUpdateContext, CREATE_EXTERNAL_GUTRAN_CELL, nrCell, e.getMessage());
        }
        return Optional.empty();
    }

    private NcmpAttribute buildExternalGUtranCellAttrs(Long localCellId) {
        return ExternalGUtranCell.builder()
            .localCellId(localCellId.intValue())
            .build();
    }

    private NcmpObject<NcmpAttribute> buildExternalGUtranCell(NrCell nrCell, Fdn gUtranSyncSignalFrequencyRef, List<PlmnId> plmnIdList) {
        String nrCellRDN = ExternalId.of(nrCell.getExternalId()).getResourceIdentifier().getLast().getValue();
        return NcmpObject.builder()
            .id(nrCellRDN)
            .attributes(new ExternalGUtranCell(nrCellRDN, nrCell, gUtranSyncSignalFrequencyRef, plmnIdList))
            .build();
    }
}

