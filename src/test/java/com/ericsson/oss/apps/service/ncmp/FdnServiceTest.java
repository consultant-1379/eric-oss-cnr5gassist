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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.service.ncmp.FdnService.LOCAL_SCTP_ENDPOINT_OPTIONS;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class FdnServiceTest {

    @Mock
    NcmpService ncmpService;
    @InjectMocks
    FdnService fdnService;

    @Test
    public void getLocalSctpEndpointFdnTest() throws ObjectNotFoundException {
        Mockito.when(ncmpService.getResourcesWithOptions(MANAGED_ELEMENT_EXTERNAL_ID, LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class))
            .thenReturn(List.of(
                toNcmpObject(LOCAL_SCTP_ENDPOINT_1_X2),
                toNcmpObject(LOCAL_SCTP_ENDPOINT_2_X2)));

        Mockito.when(ncmpService.getResources(MANAGED_ELEMENT_EXTERNAL_ID, ResourcePartitionMember.class))
            .thenReturn(List.of(
                toNcmpObject(RESOURCE_PARTITION_MEMBER_1),
                toNcmpObject(RESOURCE_PARTITION_MEMBER_2)));

        Mockito.when(ncmpService.getResourcesWithOptions(ENDPOINT_RESOURCE_EXTERNAL_ID, LOCAL_SCTP_ENDPOINT_OPTIONS, LocalSctpEndpoint.class))
                .thenReturn(List.of(toNcmpObject(LOCAL_SCTP_ENDPOINT_1_X2)));

        Fdn localSctpEndpointRef1 = fdnService.getLocalSctpEndpointFdn(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION_PLMNID_1);

        Assertions.assertEquals(LOCAL_SCTP_ENDPOINT_REF_1, localSctpEndpointRef1);
    }

    @Test
    public void getIpAddressFdnTest() throws ObjectNotFoundException {
        Mockito.when(ncmpService.getResource(SCTP_ENDPOINT_EXTERNAL_ID, SctpEndpoint.class))
            .thenReturn(Optional.of(toNcmpObject(SCTP_ENDPOINT)));

        Fdn ipAddressFdn = fdnService.getIpAddressFdn(SCTP_ENDPOINT_EXTERNAL_ID);

        Assertions.assertEquals(IPV4_FDN, ipAddressFdn);
    }

    @Test
    public void getGUtranSyncSignalFrequencyFdnTest() throws ObjectNotFoundException {
        Mockito.when(ncmpService.getResourcesWithOptions(eq(GUTRA_NETWORK_EXTERNAL_ID), any(NcmpAttribute.class),
                eq(GUtranSyncSignalFrequency.class))).thenReturn(List.of(toNcmpObject(G_UTRAN_SYNC_SIGNAL_FREQUENCY)));
        Mockito.when(ncmpService.getResource(any(ExternalId.class), eq(ManagedElement.class)))
            .thenReturn(Optional.of(toNcmpObject(MANAGED_ELEMENT)));

        Assertions.assertEquals(G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF, fdnService.getGUtranSyncSignalFrequencyFdn(GUTRA_NETWORK_EXTERNAL_ID,1111));
    }

    @Test
    public void fdnNotFoundException() {
        Mockito.when(ncmpService.getResourcesWithOptions(any(ExternalId.class), any(NcmpAttribute.class), any()))
            .thenReturn(List.of());
        Mockito.when(ncmpService.getResource(any(ExternalId.class), any())).thenReturn(Optional.empty());

        Assertions.assertThrows(ObjectNotFoundException.class, () -> fdnService.getIpAddressFdn(SCTP_ENDPOINT_EXTERNAL_ID));
        Assertions.assertThrows(ObjectNotFoundException.class, () -> fdnService.getLocalSctpEndpointFdn(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION));
        Assertions.assertThrows(ObjectNotFoundException.class, () -> fdnService.getDnPrefix(MANAGED_ELEMENT_EXTERNAL_ID));
        Assertions.assertThrows(ObjectNotFoundException.class, () -> fdnService.getGUtranSyncSignalFrequencyFdn(GUTRA_NETWORK_EXTERNAL_ID, 1111));
    }

    @Test
    public void testFindResourcePartitionMemberWithMatchingPlmnId(){
        List<PlmnId> plmns = new ArrayList<>();
        plmns.add(PLMNID);
        ResourcePartitionMember test = ResourcePartitionMember.builder().pLMNIdList(plmns).endpointResourceRef(new Fdn("Test=test")).build();

        List<PlmnId> plmns2 = new ArrayList<>();
        plmns2.add(PlmnId.builder().mcc(1).mnc(2).build());
        ResourcePartitionMember test2 = ResourcePartitionMember.builder().pLMNIdList(plmns2).endpointResourceRef(new Fdn("Test=test2")).build();

        Mockito.when(ncmpService.getResources(any(ExternalId.class), any())).thenReturn(List.of(toNcmpObject(test2), toNcmpObject(test)));

        Optional<ResourcePartitionMember> resourcePartitionMember = fdnService.findResourcePartitionMemberWithMatchingPlmnId(MANAGED_ELEMENT_EXTERNAL_ID, PLMNID);
        Assertions.assertEquals(test, resourcePartitionMember.get());
    }

    @Test
    public void testFindResourcePartitionMemberWithMatchingPlmnIdNotFound() {
        List<PlmnId> plmns2 = new ArrayList<>();
        plmns2.add(PlmnId.builder().mcc(1).mnc(2).build());
        ResourcePartitionMember test2 = ResourcePartitionMember.builder().pLMNIdList(plmns2).endpointResourceRef(new Fdn("Test=test2")).build();

        List<ResourcePartitionMember> list = new ArrayList<>();
        list.add(test2);
        Mockito.when(ncmpService.getResources(any(ExternalId.class), any())).thenReturn(List.of(toNcmpObject(test2)));

        Optional<ResourcePartitionMember> resourcePartitionMember = fdnService.findResourcePartitionMemberWithMatchingPlmnId(MANAGED_ELEMENT_EXTERNAL_ID, PLMNID);
        Assertions.assertTrue(resourcePartitionMember.isEmpty());
    }
}
