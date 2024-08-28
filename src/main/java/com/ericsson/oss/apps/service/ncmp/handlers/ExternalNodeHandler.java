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

import com.ericsson.oss.apps.client.cts.model.Gnbdu;
import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.*;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.NcmpService;
import com.ericsson.oss.apps.service.ncmp.EnmUpdateMonitorService;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP_EXTERNALGNODEBFUNCTIONS_OBJECT_COUNT;
import static com.ericsson.oss.apps.util.CtsUtils.getWirelessNetwork;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalNodeHandler {

    private static final String CREATE_EXTERNAL_GNODEB_FUNCTION = "CreateExternalGNodeBFunction";

    public static final NcmpObject<NcmpAttribute> GUTRA_NETWORK_OBJECT = NcmpObject.builder()
        .id("1").attributes(GUtraNetwork.builder().gUtraNetworkId("1").build())
        .build();
    private final NcmpCounterService ncmpCounterService = new NcmpCounterService();
    private final NcmpService ncmpService;
    private final EnmUpdateMonitorService enmUpdateMonitorService;
    private final MetricService metricService;

    public Optional<ExternalId> read(ExternalId gutraNetworkId, Gnbdu gnbdu, GnbduFunction gnbduFunction) {
        return ncmpService.getResourcesWithOptions(gutraNetworkId,
                ExternalGNodeBFunction.builder().gNodeBId(gnbduFunction.getGNBId()).gNodeBIdLength(gnbduFunction.getGNBIdLength()).build(), ExternalGNodeBFunction.class)
            .stream()
            .filter(item -> plmnIdFilter(gnbdu, item))
            .findAny().map(gutraNetworkId::add);
    }

    public Optional<NcmpObject<NcmpAttribute>> create(ExternalId gutraNetworkId, Gnbdu gnbdu, GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        try {
            NcmpObject<NcmpAttribute> externalGNodeBFunction = externalGNodeBFunction(gnbdu, gnbduFunction);
            createExternalNode(gutraNetworkId, externalGNodeBFunction);

            ncmpCounterService.incrementCounter(NCMP_EXTERNALGNODEBFUNCTIONS_OBJECT_COUNT);
            enmUpdateMonitorService.updateSucceeded(enmUpdateContext, CREATE_EXTERNAL_GNODEB_FUNCTION);
            metricService.increment(NCMP_OBJECT_COUNT, NCMP_OBJECT, EXTERNALGNODEBFUNCTIONS);

            return Optional.of(externalGNodeBFunction);
        } catch (ObjectNotFoundException | RestClientException e) {

            log.error("ExternalGNodeBFunction creation failed.", e);
            enmUpdateMonitorService.updateFailed(enmUpdateContext, CREATE_EXTERNAL_GNODEB_FUNCTION, e.getMessage());

            return Optional.empty();
        }
    }

    private void createExternalNode(ExternalId gutraNetworkId, NcmpObject<NcmpAttribute> externalGNodeBFunction) {
        if (ncmpService.getResource(gutraNetworkId, GUtraNetwork.class).isEmpty()) {
            ncmpService.createResource(gutraNetworkId.getParent(), GUTRA_NETWORK_OBJECT);
        }
        ncmpService.createResource(gutraNetworkId, externalGNodeBFunction);
    }

    private static boolean plmnIdFilter(Gnbdu gnbdu, NcmpObject<ExternalGNodeBFunction> externalGNodeBFunction) {
        PlmnId plmnId = externalGNodeBFunction.getAttributes().getGNodeBPlmnId();
        return getWirelessNetwork(gnbdu)
            .map(n -> plmnId.getMcc() == n.getMcc() && plmnId.getMnc() == n.getMnc())
            .orElse(false);
    }

    private static NcmpObject<NcmpAttribute> externalGNodeBFunction(Gnbdu gnbdu, GnbduFunction gnbduFunction) throws ObjectNotFoundException {
        String managedElementValue = ExternalId.of(gnbdu.getExternalId()).getResourceIdentifier().getFirst().getValue();
        return NcmpObject.builder()
            .id(managedElementValue)
            .attributes(new ExternalGNodeBFunction(managedElementValue, gnbduFunction, gnbdu, managedElementValue))
            .build();
    }
}
