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

import { Rate } from 'k6/metrics';
import { sleep, fail, check } from 'k6';
import { monitoringId200Succeeded, startNrc200, startNrc } from '../modules/nrc-common.js';
import { strip } from '../modules/common.js';

const processQueueSize = 10;
const taskQueueFullRate = new Rate('503_task_queue_full_rate');
const successfulRequestRate = new Rate('2xx_successful_response_rate');

let idIncrementCounter = 0;

function testSetup() {
    //fill the process queue
    for (let i = 0; i < processQueueSize; i++) {
        startNrc200(generateRequest());
    }
}

function testTaskQueueIsFull() {
    const response = startNrc(generateRequest());
    if (response.status === 200 || response.status === 208) {
        taskQueueFullRate.add(false);
        successfulRequestRate.add(true);
    } else if (check(response, {['POST /startNrc response status is 503']: (r) => r.status === 503})) {
        taskQueueFullRate.add(true);
        successfulRequestRate.add(false);
    } else {
        fail('unexpected response '.concat(strip(JSON.stringify(response, null), '\"')));
    }
}

function testTeardown() {
    //wait for the process queue to be cleared
    sleep(10);

    const response = startNrc200({'eNodeBIds':[29], 'distance':600, 'freqPairs':{'5230':[177000]}});
    if (response.status !== 200)
        fail('queue is still full '.concat(strip(JSON.stringify(response, null), '\"')));
    monitoringId200Succeeded(response.body);
}

function generateRequest() {
    idIncrementCounter++;
    return {'eNodeBIds':[29 + idIncrementCounter], 'distance':600, 'freqPairs':{'5230':[177000]}};
}

module.exports = {
    testSetup,
    testTaskQueueIsFull,
    testTeardown
}