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

import { group } from 'k6';
import * as ctsCommon from '../modules/cts-common.js';
import * as ncmpCommon from '../modules/ncmp-common.js'
import { e2eObjects, enodebFdn1 } from '../modules/e2e-constants.js';

function healthCheck() {
    group('NCMP endpoint health check: send a request to NCMP to get ENodeB object', function() {
        let enodeb1 = ctsCommon.getEnodeb({'fdn': enodebFdn1});
        ncmpCommon.getENodeBFunction({'mo': enodeb1, 'valid': true});
    });
}

function knownNodesTest() {
    group('Query known nodes: ENodeBFunction on ENodeB', function() {
        ncmpCommon.getENodeBFunction({'mo': e2eObjects.enodeb1, 'valid': true});
    });

    group('Query known nodes: GNBDUFunction on GNodeB', function() {
        ncmpCommon.getGNBDUFunction({'mo': e2eObjects.gnbdu1, 'valid': true});
    });

    group('Query known nodes: GUtraNetwork on ENodeB', function() {
        ncmpCommon.getGUtraNetwork({'mo': e2eObjects.enodeb1, 'valid': true});
    });

    group('Query known nodes: ExternalGNodeBFunction on ENodeB', function() {
        ncmpCommon.getExternalGNodeBFunction({'mo': e2eObjects.enodeb1, 'valid': true});
    });

    group('Query known cells: NRCellCU  for NRCell', function() {
        ncmpCommon.getNRCellCU({'mo': e2eObjects.nrcell1, 'valid': true});
    });

    group('Query known cells: NRSectorCarrier for NRCellCU', function() {
        ncmpCommon.getNRSectorCarrier({'mo': e2eObjects.nrSectorCarrier1, 'valid': true});
    });

    group('Query known GUtraNetwork: GUtranSyncSignalFrequency', function() {
        ncmpCommon.getGUtranSyncSignalFrequency({'mo': e2eObjects.enodeb1, 'valid': true});
    });

    group('Query known GUtraNetwork: DN Prefix', function() {
        ncmpCommon.getDnPrefix({'mo': e2eObjects.enodeb1, 'valid': true});
    });
}

module.exports = {
    healthCheck,
    knownNodesTest
}