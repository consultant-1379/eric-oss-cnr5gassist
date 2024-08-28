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

const jsonData = __ENV.DATA ? JSON.parse(open(__ENV.DATA)) : {};

export const options = defaultE2EOptions;

export default function () {
    dataSync();
}

export function handleSummary(data) {
    return {
        'stdout': runSummary(data),
    };
}

function dataSync() {
    group('dataSync', function() {
        check(ctsCommon.doDataSync(jsonData), {
            ['datasync in CTS was successful (200)']: (r) => r.status === 200
        });
    });
}
