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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import static com.ericsson.oss.apps.util.Constants.GUTRA_NETWORK_ID;

@Data
@Builder
@AllArgsConstructor
@Type(name = "erienmnrmlrat:GUtraNetwork")
public class GUtraNetwork implements NcmpAttribute {
    @JsonProperty(GUTRA_NETWORK_ID)
    private String gUtraNetworkId;
    @Builder.Default
    private String userLabel = "1";
}
