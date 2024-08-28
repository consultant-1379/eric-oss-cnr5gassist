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

import lombok.*;

@Data
@Builder
@Type(name = "ericsson-enm-GNBDU:NRSectorCarrier")
public class SectorCarrier implements NcmpAttribute {
    @Builder.Default
    private String nRSectorCarrierId = "1";
    private Integer essScPairId;
    private Integer arfcnDL;
}
