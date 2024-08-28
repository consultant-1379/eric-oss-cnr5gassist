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

import * as constants from '../modules/constants.js';
import * as ctsConstants from '../modules/cts-constants.js';
import { startNrc200, monitoringId200Succeeded } from '../modules/nrc-common.js';
import * as ctsCommon from '../modules/cts-common.js'

function getId(getIdFunc, options) {
    if (ctsConstants.ctsIdMapping) {
        return getIdFunc(options) ? getIdFunc(options).id : options['nodeId'];
    } else {
        return options['cellId'] ? options['cellId'] : options['nodeId'];
    }
}

function happyPathTest() {
    const id29 = getId(ctsCommon.getEnodeb, {'nodeId': 29});
    const id23 = getId(ctsCommon.getGnbdu, {'nodeId': 23});
    const id26 = getId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 26});
    const id28 = getId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 28});

    const response = startNrc200({'eNodeBIds': [id29], 'distance': 600, 'freqPairs': {'5230': [177000]}});

    monitoringId200Succeeded(response.body, {
            'includes': JSON.stringify([{'eNodeBId': id29, 'gNodeBDUs': [{'gNodeBDUId': id23, 'nrCellIds': [id26, id28]}]}], null)
        }
    );
}

function extraFreq() {
    const id29 = getId(ctsCommon.getEnodeb, {'nodeId': 29});
    const id23 = getId(ctsCommon.getGnbdu, {'nodeId': 23});
    const id26 = getId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 26});
    const id27 = getId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 27});
    const id28 = getId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 28});
    const nrCells = [id26, id27, id28];
    nrCells.sort();

    const response = startNrc200({'eNodeBIds': [id29], 'distance': 600, 'freqPairs': {'5230': [176910, 177000]}});

    monitoringId200Succeeded(response.body, {
            'includes': JSON.stringify([{'eNodeBId': id29, 'gNodeBDUs': [{'gNodeBDUId': id23, 'nrCellIds': nrCells}]}], null)
        }
    );
}

module.exports={
    happyPathTest,
    extraFreq
}