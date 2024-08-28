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

import com.ericsson.oss.apps.client.cts.model.NrCell;
import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.AdditionalPLMNInfo;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.PlmnId;
import javassist.tools.rmi.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlmnIdListGrouping {

    private final NrCellCUReader nrCellCUReader;
    private final AdditionalPLMNInfoReader additionalPlmnInfoReader;

    public List<PlmnId> getPlmnIdList(NrCell nrCell) throws ObjectNotFoundException {
        List<PlmnId> plmnIdList = getPlmnIdListFromNrCellCU(nrCell);
        List<PlmnId> additionalPlmnIdList = getAdditionalPlmnIdListUnderNrCellCU(nrCell);
        return groupPlmnIdLists(plmnIdList, additionalPlmnIdList);
    }

    private List<PlmnId> groupPlmnIdLists(List<PlmnId> plmnIdList, List<PlmnId> additionalPlmnIdList) {
        if (additionalPlmnIdList == null || additionalPlmnIdList.isEmpty()){
            return plmnIdList;
        }
        else if(plmnIdList == null){
            return additionalPlmnIdList;
        }
        return Stream.concat(plmnIdList.stream(), additionalPlmnIdList.stream())
            .distinct()
            .collect(Collectors.toList());
    }

    private List<PlmnId> getPlmnIdListFromNrCellCU(NrCell nrCell) throws ObjectNotFoundException {
        return nrCellCUReader.read(ExternalId.of(nrCell.getExternalId()).getRoot(), nrCell.getLocalCellIdNci())
            .orElseThrow(() -> new ObjectNotFoundException("Failed to find the corresponding NRCellCU instance for the NRCellDU: " + nrCell))
            .getAttributes().getPLMNIdList();
    }

    private List<PlmnId> getAdditionalPlmnIdListUnderNrCellCU(NrCell nrCell) {
        ExternalId externalId = ExternalId.of(nrCell.getExternalId());
        List<NcmpObject<AdditionalPLMNInfo>> allAdditionalPlmnInfos = additionalPlmnInfoReader.read(externalId);

        if (allAdditionalPlmnInfos==null || allAdditionalPlmnInfos.isEmpty()){
            log.debug("No AdditionalPLMNInfo instances were found for the cell: {}", nrCell);
            return Collections.emptyList();
        }
        Set<PlmnId> uniqPlmns = getPlmnIdListFromEachAdditionalPlmnIdList(allAdditionalPlmnInfos);
        return List.copyOf(uniqPlmns);
    }

    private Set<PlmnId> getPlmnIdListFromEachAdditionalPlmnIdList(List<NcmpObject<AdditionalPLMNInfo>> allAdditionalPlmnInfos) {
        Set<PlmnId> uniqPlmns = new LinkedHashSet<>();
        allAdditionalPlmnInfos.forEach(additionalPlmnInfo ->
            uniqPlmns.addAll(Optional.ofNullable(additionalPlmnInfo.getAttributes().getPLMNIdList()).orElse(Collections.emptyList())));
        return uniqPlmns;
    }
}
