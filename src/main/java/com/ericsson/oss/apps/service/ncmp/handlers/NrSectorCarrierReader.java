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
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.SectorCarrier;
import com.ericsson.oss.apps.service.NcmpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NrSectorCarrierReader {

    private final NcmpService ncmpService;

    public Optional<NcmpObject<SectorCarrier>> read(ExternalId nrSectorCarrierId) {
        return ncmpService.getResource(nrSectorCarrierId, SectorCarrier.class);
    }
}
