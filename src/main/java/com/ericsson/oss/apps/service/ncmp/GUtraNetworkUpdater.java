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

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.EnmUpdateContext;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.GnbduFunction;
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.service.NcmpCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.NCMP_MISSING_NEIGHBOURS_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class GUtraNetworkUpdater {

    private final ExternalGUtranCellUpdater externalGUtranCellUpdater;
    private final NcmpCounterService ncmpCounterService;
    private final EnmUpdateContextService enmUpdateContextService;

    public void update(List<NrCell> nrCells, GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        Optional<ExternalId> optionalExternalGNBId = enmUpdateContextService.readExternalGNodeBFunction(gnbduFunction, enmUpdateContext);

        if (optionalExternalGNBId.isPresent()) {
            if (enmUpdateContextService.readTermPointToGNB(optionalExternalGNBId.get()).isPresent()) {
                externalGUtranCellUpdater.update(optionalExternalGNBId.get(), nrCells, enmUpdateContext);
            } else {
                enmUpdateContextService.createTermPointToGNB(optionalExternalGNBId.get(), gnbduFunction, enmUpdateContext);
            }
        } else {
            ncmpCounterService.incrementCounter(NCMP_MISSING_NEIGHBOURS_COUNT);
            Optional<NcmpObject<NcmpAttribute>> externalGNodeBFunction = enmUpdateContextService.createExternalGNodeBFunction(gnbduFunction, enmUpdateContext);
            createTermPoint( externalGNodeBFunction, gnbduFunction, enmUpdateContext);
        }
    }

    private void createTermPoint(Optional<NcmpObject<NcmpAttribute>> externalGNodeBFunction, GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        externalGNodeBFunction.ifPresent(
            externalGNodeBFunctionObject -> enmUpdateContextService.createTermPointToGNB(externalGNodeBFunctionObject, gnbduFunction, enmUpdateContext));
    }
}
