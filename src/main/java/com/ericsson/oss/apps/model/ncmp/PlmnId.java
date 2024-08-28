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

import com.ericsson.oss.apps.client.cts.model.WirelessNetwork;
import lombok.*;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlmnId {
    @Getter(AccessLevel.NONE)
    private long mncLength;
    private long mcc;
    private long mnc;

    public PlmnId(WirelessNetwork wirelessNetwork) {
        this.mnc = wirelessNetwork.getMnc();
        this.mcc = wirelessNetwork.getMcc();
    }

    public long getMncLength() {
        return mnc <= 99 ? 2 : 3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mncLength, mcc, mnc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlmnId other = (PlmnId) o;
        return this.getMcc() == other.getMcc() && this.getMnc() == other.getMnc();
    }
}
