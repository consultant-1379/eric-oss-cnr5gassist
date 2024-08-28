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

package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.api.model.NrcNeighbor;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.service.nrc.NrcNeighboringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NrcService {

    private final NrcNeighboringService nrcNeighboringService;

    public List<NrcNeighbor> startNrc(final NrcTask nrcTask) {
        return nrcTask.getRequest().geteNodeBIds().stream().parallel()
            .flatMap(eNodeBId -> nrcNeighboringService.getNrcNeighbor(nrcTask, eNodeBId))
            .collect(Collectors.toList());
    }
}
