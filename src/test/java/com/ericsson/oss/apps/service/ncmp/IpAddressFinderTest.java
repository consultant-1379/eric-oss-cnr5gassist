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

import com.ericsson.oss.apps.model.ncmp.IpAddress;
import com.ericsson.oss.apps.service.NcmpService;
import javassist.tools.rmi.ObjectNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;

@ExtendWith(MockitoExtension.class)
public class IpAddressFinderTest {

    @Mock
    FdnService fdnService;
    @Mock
    NcmpService ncmpService;

    @InjectMocks
    IpAddressFinder ipAddressFinder;

    @Test
    public void getIpForManagedElementTest() throws ObjectNotFoundException {
        Mockito.when(fdnService.getLocalSctpEndpointFdn(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION)).thenReturn(LOCAL_SCTP_ENDPOINT_REF);
        Mockito.when(fdnService.getIpAddressFdn(SCTP_ENDPOINT_EXTERNAL_ID)).thenReturn(IPV4_FDN);
        Mockito.when(ncmpService.getResource(IPV4_EXTERNAL_ID, IpAddress.class)).thenReturn(Optional.of(toNcmpObject(IPv4_ADDRESS)));

        String resultIp = ipAddressFinder.getIpForManagedElement(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION);
        Assertions.assertEquals(STRIPPED_IPv4_ADDRESS, resultIp);
    }

    @Test
    public void localSctpEndpointFdnForManagedElementNotFoundTest() throws ObjectNotFoundException {
        Mockito.when(fdnService.getLocalSctpEndpointFdn(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION)).thenThrow(ObjectNotFoundException.class);

        Assertions.assertThrows(ObjectNotFoundException.class, () -> ipAddressFinder.getIpForManagedElement(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION));
    }

    @Test
    public void ipAddressFdnForManagedElementNotFoundTest() throws ObjectNotFoundException {
        Mockito.when(fdnService.getLocalSctpEndpointFdn(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION)).thenReturn(LOCAL_SCTP_ENDPOINT_REF);
        Mockito.when(fdnService.getIpAddressFdn(SCTP_ENDPOINT_EXTERNAL_ID)).thenThrow(ObjectNotFoundException.class);

        Assertions.assertThrows(ObjectNotFoundException.class, () -> ipAddressFinder.getIpForManagedElement(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION));
    }

    @Test
    public void ipAddressForManagedElementNotFoundTest() throws ObjectNotFoundException {
        Mockito.when(fdnService.getLocalSctpEndpointFdn(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION)).thenReturn(LOCAL_SCTP_ENDPOINT_REF);
        Mockito.when(fdnService.getIpAddressFdn(SCTP_ENDPOINT_EXTERNAL_ID)).thenReturn(IPV4_FDN);
        Mockito.when(ncmpService.getResource(IPV4_EXTERNAL_ID, IpAddress.class)).thenReturn(Optional.empty());

        Assertions.assertThrows(ObjectNotFoundException.class, () -> ipAddressFinder.getIpForManagedElement(MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION));
    }
}
