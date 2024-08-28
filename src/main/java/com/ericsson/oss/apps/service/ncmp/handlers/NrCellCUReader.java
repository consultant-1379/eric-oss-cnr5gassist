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
package com.ericsson.oss.apps.service.ncmp.handlers;

import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ResourceIdentifier;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.service.NcmpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NrCellCUReader {

    private final NcmpService ncmpService;

    public Optional<NcmpObject<NrCellCU>> read(ExternalId externalId, Long localCellIdNci) {
        return ncmpService.getResourcesWithOptions(externalId.of(new ResourceIdentifier()),
            NrCellCU.builder().cellLocalId(localCellIdNci).build(),
            NrCellCU.class).stream().findAny();
    }
}
