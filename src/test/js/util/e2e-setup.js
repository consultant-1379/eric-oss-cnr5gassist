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

import {group, check} from 'k6';
import {defaultE2EOptions} from '../modules/constants.js';
import {runSummary} from '../modules/common.js';
import * as ctsCommon from '../modules/cts-common.js'

export const options = defaultE2EOptions;

export default function () {
    enodebSetup();
    gnbduSetup();
}

export function handleSummary(data) {
    return {
        'stdout': runSummary(data),
    };
}

function checkId(getIdFunc, options) {
    ctsCommon.checkIdExists(getIdFunc, options);
}

function enodebSync(options) {
    return ctsCommon.doDataSync(ctsCommon.createEnodebCtsData(options));
}

function gndbuSync(options) {
    return ctsCommon.doDataSync(ctsCommon.createGndbuCtsData(options));
}

function enodebSetup() {
    group('enodebSetup', function() {
        check(enodebSync({'nodeId': 29, 'FDDearfcnDl': 5230, 'FDDearfcnUl': 23230}), {
            ['datasync in CTS was successful (200)']: (r) => r.status === 200
        });
        checkId(ctsCommon.getEnodeb, {'nodeId': 29});
    });
}

function gnbduSetup() {
    group('gnbduSetup', function() {
        check(gndbuSync({'nodeId': 23, 'cellIds': [26, 28], 'downlinkEARFCN': 177000}), {
            ['datasync in CTS was successful (200)']: (r) => r.status === 200
        });
        check(gndbuSync({'nodeId': 23, 'cellId': 27, 'downlinkEARFCN': 176910}), {
            ['datasync in CTS was successful (200)']: (r) => r.status === 200
        });
        checkId(ctsCommon.getGnbdu, {'nodeId': 23});
        checkId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 26});
        checkId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 27});
        checkId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 28});
    });
}
