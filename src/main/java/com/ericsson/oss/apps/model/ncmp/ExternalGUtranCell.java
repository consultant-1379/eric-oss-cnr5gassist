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

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.Fdn;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.ericsson.oss.apps.util.Constants.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Type(name = "erienmnrmlrat:ExternalGUtranCell")
public class ExternalGUtranCell implements NcmpAttribute {
    @JsonProperty(EXTERNAL_GUTRAN_CELL_ID)
    private String externalGUtranCellId;
    @JsonProperty(GUTRANSYNC_SIGNAL_FREQUENCY)
    private Fdn gUtranSyncSignalFrequencyRef;
    private Integer absTimeOffset;
    private Integer absSubFrameOffset;
    private Integer localCellId;
    private Integer physicalLayerCellIdGroup;
    private Integer physicalLayerSubCellId;
    private List<PlmnId> plmnIdList;
    private String isRemoveAllowed;
    @JsonProperty(NRTAC)
    private String nRTAC;

    public ExternalGUtranCell(NrCell nrCell, Fdn gUtranSyncSignalFrequencyRef, List<PlmnId> plmnIdList) {
        this.gUtranSyncSignalFrequencyRef = gUtranSyncSignalFrequencyRef;
        this.isRemoveAllowed = TRUE;
        this.localCellId = Objects.requireNonNull(nrCell.getLocalCellIdNci()).intValue();
        this.nRTAC = Objects.requireNonNull(nrCell.getTrackingAreaCode()).toString();
        this.physicalLayerCellIdGroup = Objects.requireNonNull(nrCell.getPhysicalCellIdentity()) / 3;
        this.physicalLayerSubCellId = nrCell.getPhysicalCellIdentity() % 3;
        if (plmnIdList != null) {
            this.plmnIdList = new ArrayList<>(plmnIdList);
        }
    }

    public ExternalGUtranCell(@NonNull String externalGUtranCellId, NrCell nrCell, Fdn gUtranSyncSignalFrequencyRef, List<PlmnId> plmnIdList) {
        this(nrCell, gUtranSyncSignalFrequencyRef, plmnIdList);
        this.externalGUtranCellId = externalGUtranCellId;
    }
}
