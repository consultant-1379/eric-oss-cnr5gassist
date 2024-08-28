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

import com.ericsson.oss.apps.client.cts.model.Gnbdu;
import com.fasterxml.jackson.annotation.JsonProperty;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.*;

import static com.ericsson.oss.apps.util.Constants.*;
import static com.ericsson.oss.apps.util.CtsUtils.getWirelessNetwork;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Type(name = "erienmnrmlrat:ExternalGNodeBFunction")
public class ExternalGNodeBFunction implements NcmpAttribute {
    @JsonProperty(EXTERNAL_GNODEB_FUNCTION_ID)
    private String externalGNodeBFunctionId;
    @JsonProperty(EXTERNAL_GNODEB_PLMN_ID)
    private PlmnId gNodeBPlmnId;
    @JsonProperty(GNODEB_ID)
    private long gNodeBId;
    @JsonProperty(GNODEB_ID_LENGTH)
    private long gNodeBIdLength;
    private String userLabel;

    public ExternalGNodeBFunction(@NonNull String externalGNodeBFunctionId, GnbduFunction gnbduFunction, Gnbdu gnbdu, String managedElementValue) throws ObjectNotFoundException {
        this.gNodeBId = gnbduFunction.getGNBId();
        this.gNodeBIdLength = gnbduFunction.getGNBIdLength();
        this.userLabel = managedElementValue;
        this.gNodeBPlmnId = new PlmnId(getWirelessNetwork(gnbdu)
                .orElseThrow(() -> new ObjectNotFoundException("WirelessNetwork not found for the resource " + this.gNodeBId)));
        this.externalGNodeBFunctionId = externalGNodeBFunctionId;
    }
}
