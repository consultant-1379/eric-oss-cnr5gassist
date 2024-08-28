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
import * as ctsCommon from '../modules/cts-common.js';

export const options = defaultE2EOptions;

export default function () {
    enodebCleanup();
    gnbduCleanup();
}

export function teardown() {
    check(true, {
        ['jsonData ' + JSON.stringify(ctsCommon.jsonHolder({'jsonData':''}), null)]: (r) => true
    });
}

export function handleSummary(data) {
    return ctsCommon.handleDataSyncSummary(data);
}

function enodebCleanup() {
    if (__ENV.ENODEB_ID) {
        const options = {'id': parseInt(__ENV.ENODEB_ID)};
        if (ctsCommon.checkIdExists(ctsCommon.getEnodeb, options)) {
            group('enodebCleanup', function() {
                check(true, {
                    ['jsonData ' + JSON.stringify(ctsCommon.cleanupLteCellGeoLocations(options), null)]: (r) => true
                });
            });
        }
    }
}

function gnbduCleanup() {
    if (__ENV.GNBDU_ID) {
        const options = {'id': parseInt(__ENV.GNBDU_ID)};
        if (ctsCommon.checkIdExists(ctsCommon.getGnbdu, options)) {
            group('gnbduCleanup', function() {
                check(true, {
                    ['jsonData ' + JSON.stringify(ctsCommon.cleanupNrCellGeoLocations(options), null)]: (r) => true
                });
            });
        }
    }
}
