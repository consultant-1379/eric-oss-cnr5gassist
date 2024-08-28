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
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.service.ncmp.handlers.ExternalNodeHandler;
import com.ericsson.oss.apps.service.ncmp.handlers.GnbduReader;
import com.ericsson.oss.apps.service.ncmp.handlers.NrCellCUReader;
import com.ericsson.oss.apps.service.ncmp.handlers.TermPointHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnmUpdateContextService {

    private final GnbduReader gnbduReader;
    private final ExternalNodeHandler externalNodeHandler;
    private final TermPointHandler termPointHandler;
    private final NrCellCUReader nrCellCUReader;

    public ExternalId getExternalGnbduId(EnmUpdateContext enmUpdateContext) {
        return ExternalId.of(enmUpdateContext.getGnbdu().getExternalId());
    }

    public Optional<NcmpObject<GnbduFunction>> getOptionalGnbduFunction(EnmUpdateContext enmUpdateContext) {
        return gnbduReader.read(getExternalGnbduId(enmUpdateContext)).stream().findFirst();
    }

    public ExternalId getGutraNetworkId(EnmUpdateContext enmUpdateContext) {
        return ExternalId.of(enmUpdateContext.getENodeB().getExternalId()).add(ExternalNodeHandler.GUTRA_NETWORK_OBJECT);
    }

    public ExternalId getGNodeBManagedElementExtId(EnmUpdateContext enmUpdateContext) {
        return ExternalId.of(enmUpdateContext.getGnbdu().getExternalId()).getParent();
    }

    public Optional<NcmpObject<NrCellCU>> getNrCellCU(NrCell nrCell, EnmUpdateContext enmUpdateContext) {
        return nrCellCUReader.read(getGNodeBManagedElementExtId(enmUpdateContext), nrCell.getLocalCellIdNci());
    }

    public Optional<NcmpObject<NcmpAttribute>> createExternalGNodeBFunction(GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        return externalNodeHandler.create(getGutraNetworkId(enmUpdateContext), enmUpdateContext.getGnbdu(), gnbduFunction, enmUpdateContext);
    }

    public Optional<ExternalId> readExternalGNodeBFunction(GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        return externalNodeHandler.read(getGutraNetworkId(enmUpdateContext), enmUpdateContext.getGnbdu(), gnbduFunction);
    }

    public Optional<NcmpObject<NcmpAttribute>> createTermPointToGNB(ExternalId externalGNBId, GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        return termPointHandler.create(externalGNBId, getGNodeBManagedElementExtId(enmUpdateContext), gnbduFunction, enmUpdateContext);
    }

    public Optional<NcmpObject<NcmpAttribute>> createTermPointToGNB(NcmpObject<NcmpAttribute> externalGNodeBFunctionObject, GnbduFunction gnbduFunction, EnmUpdateContext enmUpdateContext) {
        return termPointHandler.create(getGutraNetworkId(enmUpdateContext).add(externalGNodeBFunctionObject), getGNodeBManagedElementExtId(enmUpdateContext), gnbduFunction, enmUpdateContext);
    }

    public Optional<ExternalId> readTermPointToGNB(ExternalId externalGNBId) {
        return termPointHandler.read(externalGNBId);
    }
}
