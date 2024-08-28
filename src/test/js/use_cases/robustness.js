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

import { group, check, sleep } from 'k6';
import * as common from '../modules/common.js';
import * as nrcCommon from '../modules/nrc-common.js';
import * as eiapProxy from '../modules/eiap-proxy.js';
import { e2eObjects } from '../modules/e2e-constants.js';

const robustnessMaxRequestCount = __ENV.ROBUSTNESS_MAX_REQUEST_COUNT ? __ENV.ROBUSTNESS_MAX_REQUEST_COUNT : 100;
const robustnessTimeout = __ENV.ROBUSTNESS_TIMEOUT ? __ENV.ROBUSTNESS_TIMEOUT : 10 * 60 ;

function restart5gcnrDuringLoad() {
    group('Restart 5G CNR service during startNrc load', function() {
        var distance = 100;
        var phase = 0;
        var phaseCounts = [0, 0, 0, 0];
        var startTime = Date.now();
        for (var i = 0; i < robustnessMaxRequestCount; i++) {
            common.logData('ITERATION', i);

            var monitoringResponse = {};
            const response = nrcCommon.startNrc({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': distance + i});
            if (response.status == 200) {
                monitoringResponse = nrcCommon.monitoringId200Succeeded(response.body);
            } else {
                sleep(10);
            }

            phaseCounts[phase]++;
            var elapsedTime = (Date.now() - startTime) / 1000;
            if (elapsedTime > robustnessTimeout) {
                break;
            }

            // Before restart send startNrc until Succeeded then restart the 5G CNR service
            if (phase == 0 && nrcCommon.isNrcStatus(monitoringResponse, 'Succeeded')) {
                phase++;
                eiapProxy.restart5gcnr();
            } else
            // After 5G CNR service restart wait for the first failing startNrc
            if (phase == 1 && !nrcCommon.isNrcStatus(monitoringResponse, 'Succeeded')) {
                phase++;
            } else
            // During the 5G CNR service restart send startNrc until not Succeeded
            if (phase == 2 && nrcCommon.isNrcStatus(monitoringResponse, 'Succeeded')) {
                phaseCounts[++phase]++;
                break;
            }
        }

        eiapProxy.shutdown();

        check(phaseCounts, {
            'Phase 1: Before restart send startNrc until Succeeded then restart the 5G CNR service': (r) => r[0] > 0,
            'Phase 2: After 5G CNR service restart wait for the first failing startNrc': (r) => r[1] > 0,
            'Phase 3: During the 5G CNR service restart sending startNrc until not Succeeded': (r) => r[2] > 0,
            'Phase 4: The 5G CNR service has successfully executed startNrc request after restart': (r) => r[3] > 0,
        });
    });
}

module.exports = {
    restart5gcnrDuringLoad,
}