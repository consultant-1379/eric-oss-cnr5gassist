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
import { runSummary } from './modules/common.js';
import { htmlReport } from './resources/eric-k6-static-report-plugin.js';
import { defaultE2EOptions } from './modules/constants.js';

import * as e2eGateway from './use_cases/e2e-gateway.js';
import * as e2eCts from './use_cases/e2e-cts.js';
import * as e2eNcmp from './use_cases/e2e-ncmp.js';
import * as e2e5gcnr from './use_cases/e2e-5gcnr.js';
import * as eiapProxy from './modules/eiap-proxy.js';
import * as robustness from './use_cases/robustness.js';
import { initE2eObjects } from './modules/e2e-constants.js';

export const options = defaultE2EOptions;

export default function () {
    group('API GW access health check', function() {
        e2eGateway.healthCheck();
        e2eCts.healthCheck();
        e2eNcmp.healthCheck();
        e2e5gcnr.healthCheck();
        eiapProxy.healthCheck();
    });

    initE2eObjects();

    group('Robustness tests', function() {
        robustness.restart5gcnrDuringLoad();
    });
}

export function handleSummary(data) {
    let result = {'stdout': runSummary(data)};
    const reportPath = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/doc/Test_Report/' : '/tmp/';
    result[reportPath.concat('k6-robustness-test-results.html')] = htmlReport(data);
    result[reportPath.concat('summary.json')] = JSON.stringify(data);
    return result;
}