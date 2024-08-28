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
    enodebCleanup();
    gnbduCleanup();
}

export function handleSummary(data) {
    return {
        'stdout': runSummary(data),
    };
}

function checkId(getIdFunc, options) {
    ctsCommon.checkIdNotExists(getIdFunc, options);
}

function enodebSync(options) {
    return ctsCommon.doDataSync(ctsCommon.cleanupEnodebCtsData(options));
}

function gndbuSync(options) {
    return ctsCommon.doDataSync(ctsCommon.cleanupGndbuCtsData(options));
}

function enodebCleanup() {
    group('enodebCleanup', function() {
        check(enodebSync({'nodeId': 29}), {
            ['datasync in CTS was successful (200)']: (r) => r.status === 200
        });
        checkId(ctsCommon.getEnodeb, {'nodeId': 29});
    });
}

function gnbduCleanup() {
    group('gnbduCleanup', function() {
        check(gndbuSync({'nodeId': 23, 'cellIds': [26, 27, 28]}), {
            ['datasync in CTS was successful (200)']: (r) => r.status === 200
        });
        checkId(ctsCommon.getGnbdu, {'nodeId': 23});
        checkId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 26});
        checkId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 27});
        checkId(ctsCommon.getNrcell, {'nodeId': 23, 'cellId': 28});
    });
}