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
package com.ericsson.oss.apps.model.ncmp;

import lombok.Builder;
import lombok.Data;
import org.apache.http.conn.util.InetAddressUtils;

import static com.ericsson.oss.apps.util.Constants.UNLOCKED;

@Data
@Builder
@Type(name = "erienmnrmlrat:TermPointToGNB")
public class TermPointToGNB implements NcmpAttribute {
    private String termPointToGNBId;
    @Builder.Default
    private String administrativeState = UNLOCKED;
    private String ipAddress;
    private String ipv6Address;

    public static class TermPointToGNBBuilder {
        public TermPointToGNBBuilder ipAddress(String ipAddress) {
            if (ipAddress != null) {
                if (InetAddressUtils.isIPv4Address(ipAddress)) {
                    this.ipAddress = ipAddress;

                } else if (InetAddressUtils.isIPv6Address(ipAddress)) {
                    this.ipv6Address = ipAddress;
                }
            }
            return this;
        }
    }
}
