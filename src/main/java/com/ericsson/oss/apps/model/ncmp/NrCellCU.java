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

import java.util.List;

@Data
@Builder
@Type(name = "ericsson-enm-gnbcucp:NRCellCU")
public class NrCellCU implements NcmpAttribute {
    private String nRCellCUId;
    private Long cellLocalId;
    private Boolean pSCellCapable;
    private List<PlmnId> pLMNIdList;
}
