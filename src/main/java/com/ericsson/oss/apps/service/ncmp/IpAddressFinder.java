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
import com.ericsson.oss.apps.model.ncmp.GnbduFunction;
import com.ericsson.oss.apps.model.ncmp.IpAddress;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.service.NcmpService;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IpAddressFinder {

    private final FdnService fdnService;
    private final NcmpService ncmpService;

    public String getIpForManagedElement(ExternalId gNodeBManagedElementExtId, GnbduFunction gnbduFunction) throws ObjectNotFoundException {
        Fdn localSctpEndpointFdn = fdnService.getLocalSctpEndpointFdn(gNodeBManagedElementExtId, gnbduFunction);
        Fdn ipAddressFdn = fdnService.getIpAddressFdn(gNodeBManagedElementExtId.of(localSctpEndpointFdn.toResourceIdentifier()));

        return getIpAddress(gNodeBManagedElementExtId.of(ipAddressFdn.toResourceIdentifier()));
    }

    private String getIpAddress(ExternalId ipAddressExternalId) throws ObjectNotFoundException {
        return ncmpService.getResource(ipAddressExternalId, IpAddress.class)
            .map(NcmpObject::getAttributes)
            .map(IpAddress::getIpAddressWithoutNetMask)
            .orElseThrow(() -> new ObjectNotFoundException("IPAddress not found for the resource " + ipAddressExternalId));
    }
}
