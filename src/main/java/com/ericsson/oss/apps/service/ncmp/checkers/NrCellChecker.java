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

package com.ericsson.oss.apps.service.ncmp.checkers;

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.service.ncmp.handlers.ExternalCellHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NrCellChecker {

    private final ExternalCellHandler externalCellHandler;

    public boolean withLocalCellIdValue(ExternalId externalGNodeBFunctionId, NrCell nrCell) {
        return externalCellHandler.read(externalGNodeBFunctionId, nrCell.getLocalCellIdNci()).isPresent();
    }

    public boolean withLocalCellIdNull(NrCell nrCell) {
        return nrCell.getLocalCellIdNci() == null;
    }
}
