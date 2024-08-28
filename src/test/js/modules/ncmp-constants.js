/*
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
 */

import * as constants from './constants.js';
import * as e2econstants from './e2e-constants.js';

export const ncmpUrl = __ENV.STAGING_LEVEL === 'PRODUCT' ? constants.ingressUrl :
    __ENV.NCMP_URL ? __ENV.NCMP_URL : '';

export const ncmpBasePath = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/ncmp/v1/ch/' :
    __ENV.NCMP_BASE_PATH ? __ENV.NCMP_BASE_PATH : '';

export const resourceIdentifier = '/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=';

export const eNodeBFunction = '/erienmnrmlrat:ENodeBFunction=1';
export const gNBDUFunction = '/erienmnrmlrat:GNBDUFunction=1';
export const gUtraNetwork = '/erienmnrmlrat:GUtraNetwork=1';
export const externalGNodeBFunction = '/erienmnrmlrat:ExternalGNodeBFunction=' + e2econstants.gnoden1;
export const externalGNodeBFunctionFilter = '&options=fields=erienmnrmlrat:ExternalGNodeBFunction/attributes(gNodeBPlmnId;gNodeBIdLength;gNodeBId)';
export const externalGUtranCellFilter = '&options=scope=erienmnrmlrat:ExternalGUtranCell/attributes(absTimeOffset=0)';
export const termPointToGNBFilter = '/&options=fields=erienmnrmlrat:TermPointToGNB/attributes(termPointToGNBId;administrativeState;ipAddress;ipv6Address)';
export const nRCellCUFilter = '/&options=fields=ericsson-enm-gnbcucp:NRCellCU/attributes(pSCellCapable;absFrameStartOffset;pLMNIdList)';
export const nRSectorCarrierFilter = '&options=fields=ericsson-enm-GNBDU:NRSectorCarrier/attributes(essScPairId)';
export const gUtranSyncSignalFrequencyFilter = '&options=scope=erienmnrmlrat:GUtranSyncSignalFrequency/attributes(arfcn=33523)';
export const dnPrefixFilter = '/&options=fields=ManagedElement/attributes(dnPrefix)';
