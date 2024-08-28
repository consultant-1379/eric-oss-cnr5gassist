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

import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.GnbduFunction;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.TermPointToGNB;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.NcmpCounterService;
import com.ericsson.oss.apps.service.NcmpService;
import com.ericsson.oss.apps.service.ncmp.EnmUpdateMonitorService;
import com.ericsson.oss.apps.service.ncmp.IpAddressFinder;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP_TERMPOINTTOGNB_OBJECT_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermPointHandler {

    private static final String CREATE_TERM_POINT = "CreateTermPointToGNB";

    private final NcmpService ncmpService;
    private final IpAddressFinder ipAddressFinder;
    private final NcmpCounterService ncmpCounterService = new NcmpCounterService();
    private final EnmUpdateMonitorService enmUpdateMonitorService;
    private final MetricService metricService;

    public Optional<ExternalId> read(ExternalId externalGNodeBFunctionId) {
        return ncmpService.getResources(externalGNodeBFunctionId, TermPointToGNB.class).stream()
            .findAny().map(externalGNodeBFunctionId::add);
    }

    public Optional<NcmpObject<NcmpAttribute>> create(ExternalId externalGNodeBFunctionId, ExternalId gNodeBExternalId, GnbduFunction gnbduFunction,
                                                      EnmUpdateContext enmUpdateContext) {
        String ipAddress = null;
        try {
            ipAddress = ipAddressFinder.getIpForManagedElement(gNodeBExternalId, gnbduFunction);
        } catch (ObjectNotFoundException e) {
            log.warn("Ip address could not be found for the node: {}.", gNodeBExternalId.getResourceIdentifier().getFirst(), e);
        }
        try {
            NcmpObject<NcmpAttribute> termPoint = buildTermPoint(ipAddress);
            ncmpService.createResource(externalGNodeBFunctionId, termPoint);
            metricService.increment(NCMP_OBJECT_COUNT, NCMP_OBJECT, TERMPOINTTOGNB);
            ncmpCounterService.incrementCounter(NCMP_TERMPOINTTOGNB_OBJECT_COUNT);
            enmUpdateMonitorService.updateSucceeded(enmUpdateContext, CREATE_TERM_POINT);

            return Optional.of(termPoint);
        } catch (RestClientException e) {
            log.warn("TermPointToGNB creation failed.", e);
            enmUpdateMonitorService.updateFailed(enmUpdateContext, CREATE_TERM_POINT, e.getMessage());
            return Optional.empty();
        }
    }

    private static NcmpObject<NcmpAttribute> buildTermPoint(String ipAddress) {
        return NcmpObject
            .builder()
            .id("1")
            .attributes(TermPointToGNB.builder().termPointToGNBId("1").ipAddress(ipAddress).build())
            .build();
    }
}
