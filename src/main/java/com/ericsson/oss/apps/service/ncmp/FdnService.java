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

import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.Fdn;
import com.ericsson.oss.apps.model.ncmp.*;
import com.ericsson.oss.apps.service.NcmpService;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdnService {

    public static final LocalSctpEndpoint LOCAL_SCTP_ENDPOINT_OPTIONS = LocalSctpEndpoint.builder().build();

    private final NcmpService ncmpService;

    public Fdn getLocalSctpEndpointFdn(ExternalId managedElementId, GnbduFunction gnbduFunction) throws ObjectNotFoundException {
        List<LocalSctpEndpoint> localSctpEndpoints = findAllX2LocalSctpEndPoints(managedElementId);
        if (localSctpEndpoints.size() == 1) {
            return localSctpEndpoints.get(0).getSctpEndpointRef();
        } else if (localSctpEndpoints.isEmpty()) {
            throw new ObjectNotFoundException("LocalSctpEndpointFdn with an X2 interface was not found for the resource " + managedElementId);
        } else {
            // Handle the MOCN and multiple PLMN case
            PlmnId dUpLMNId = gnbduFunction.getDUpLMNId();
            Optional<ResourcePartitionMember> resourcePartitionMember = findResourcePartitionMemberWithMatchingPlmnId(managedElementId, dUpLMNId);
            if (!resourcePartitionMember.isPresent()) {
                throw new ObjectNotFoundException("ResourcePartitionMember with " + dUpLMNId +
                    " was not found for the resource " + managedElementId);
            }

            Optional<LocalSctpEndpoint> localSctpEndpoint = findLocalSctpEndpointUnderEndPointResource(managedElementId, resourcePartitionMember.get());
            if (!localSctpEndpoint.isPresent()) {
                throw new ObjectNotFoundException(
                    "Failed to find a LocalSctpEndpoint referenced from the ResourcePartitionMember: {}" + resourcePartitionMember.get());
            }

            return localSctpEndpoint.get().getSctpEndpointRef();
        }
    }

    private List<LocalSctpEndpoint> findAllX2LocalSctpEndPoints(ExternalId managedElementId) {
        return ncmpService.getResourcesWithOptions(managedElementId, LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class)
            .stream()
            .map(NcmpObject::getAttributes).collect(Collectors.toList());
    }

    protected Optional<ResourcePartitionMember> findResourcePartitionMemberWithMatchingPlmnId(ExternalId managedElementId, PlmnId dUpLMNId) {
        List<ResourcePartitionMember> allMembers = ncmpService.getResources(managedElementId, ResourcePartitionMember.class)
                .stream().map(NcmpObject::getAttributes).collect(Collectors.toList());
        return allMembers.stream().filter(e -> e.getPLMNIdList().stream().anyMatch(dUpLMNId::equals)).findFirst();
    }

    private Optional<LocalSctpEndpoint> findLocalSctpEndpointUnderEndPointResource(ExternalId managedElementId, ResourcePartitionMember resourcePartitionMember) {
        // Ideally if we had an ExternalId as part of the Ncmp POJOs we could simply do a string comparison based on the
        // localSctpEndpoints list but without this we must do another query towards NCMP:
        ExternalId endpointResourceId = managedElementId.of(resourcePartitionMember.getEndpointResourceRef().toResourceIdentifier());
        return ncmpService.getResourcesWithOptions(endpointResourceId, LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class)
                .stream()
                .map(NcmpObject::getAttributes)
                .findFirst();
    }

    public Fdn getIpAddressFdn(ExternalId sctpEndpointId) throws ObjectNotFoundException {
        return ncmpService.getResource(sctpEndpointId, SctpEndpoint.class).stream()
            .map(NcmpObject::getAttributes)
            .map(SctpEndpoint::getLocalIpAddress)
            .flatMap(Collection::stream)
            .min(Comparator.comparing((Fdn o) -> o.getLast().getKey()))
            .orElseThrow(() -> new ObjectNotFoundException("IpAddressFdn was not found for the resource " + sctpEndpointId));
    }

    public Fdn getDnPrefix(ExternalId externalId) throws ObjectNotFoundException {
        ExternalId meExternalId = externalId.getRoot();
        return ncmpService.getResource(meExternalId, ManagedElement.class)
            .map(NcmpObject::getAttributes)
            .map(ManagedElement::getDnPrefix)
            .orElseThrow(() -> new ObjectNotFoundException("DnPrefix was not found for the resource " + meExternalId));
    }

    public Fdn getGUtranSyncSignalFrequencyFdn(ExternalId gutraNetworkId, Integer downlinkEARFCN) throws ObjectNotFoundException {
        Optional<NcmpObject<GUtranSyncSignalFrequency>> gUtranSyncSignalFrequency = ncmpService.getResourcesWithOptions(gutraNetworkId,
            buildGUtranSyncFrequency(downlinkEARFCN), GUtranSyncSignalFrequency.class).stream().findAny();
        if (gUtranSyncSignalFrequency.isPresent()) {
            return getDnPrefix(gutraNetworkId).addAll(gutraNetworkId.add(gUtranSyncSignalFrequency.get()).toFdn());
        } else {
            throw new ObjectNotFoundException("GUtranSyncSignalFrequencyFdn was not found for the resource " + gutraNetworkId);
        }
    }

    private static NcmpAttribute buildGUtranSyncFrequency(Integer downlinkEARFCN) {
        return GUtranSyncSignalFrequency.builder().arfcn(downlinkEARFCN).build();
    }
}
