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

import {check} from 'k6';
import * as constants from './constants.js';
import {
    httpGet,
    httpPost,
    getSessionId,
    waitingForResponse,
    strip,
    parseJson,
    logData,
    checkResponse,
    requestType
} from './common.js';

function nrcHttpGet(uri, options) {
    const response = httpGet(constants.serviceUrl, constants.serviceRestUri.concat(uri), getSessionParams(), options);
    nrcCheckResponse(response, options);
    return response;
}

function nrcHttpPost(uri, request, params, options) {
    const response = httpPost(constants.serviceUrl, constants.serviceRestUri.concat(uri), request, params, options);
    nrcCheckResponse(response, options);
    return response;
}

function getSessionParams() {
    return constants.sessionIdAccess ?
        {
            headers: {
                'Content-Type': 'application/json',
                'Cookie': 'JSESSIONID='.concat(getSessionId())
            }
        } : {};
}

function getStartNrcParams() {
    return constants.sessionIdAccess ?
        {
            headers: {
                'Cookie': 'JSESSIONID='.concat(getSessionId()),
                'Content-Type': 'application/json',
                'Connection': 'keep-alive',
                'Accept-Encoding' : 'gzip, deflate, br',
                'Accept' : '*/*'
            }
        } :
        {
            headers: {
                'Content-Type': 'application/json',
                'Connection': 'keep-alive',
                'Accept-Encoding' : 'gzip, deflate, br',
                'Accept' : '*/*'
            }
        };
}

function isNrcStatus(response, nrcStatus) {
    const body = parseJson(response.body);
    return body &&
           body.process &&
           body.process.nrcStatus &&
           body.process.nrcStatus === nrcStatus;
}

function nrcCheckResponse(response, options) {
    checkResponse(response, options);
    if (options['nrcStatus']) {
        check(response, {
            [requestType(options) + 'NRC process status is ' + options['nrcStatus']]:
                (r) => isNrcStatus(response, options['nrcStatus'])
        });
    }
}

function checkMonitoringIdResponse(response, options) {
    const body = parseJson(response.body);
    let result = true;
    let checkText = 'Monitoring ID response includes';

    if (options['allNrcNeighbors[?].eNodeBId']) {
        checkText = checkText.concat(' eNodeBId: '.concat(options['allNrcNeighbors[?].eNodeBId'])).concat(',');
        if (body && body.allNrcNeighbors && body.allNrcNeighbors[0] && body.allNrcNeighbors[0].eNodeBId) {
            let found = false;
            for (let i = 0; i < body.allNrcNeighbors.length && !found; i++) {
                if (body.allNrcNeighbors[i].eNodeBId === options['allNrcNeighbors[?].eNodeBId']) {
                    found = true;
                }
            }
            if (!found) {
                result = false;
            }
        }
    }

    if (options['allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId']) {
        checkText = checkText.concat(' gNodeBDUId: '.concat(options['allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId'])).concat(',');
        if (body && body.allNrcNeighbors && body.allNrcNeighbors[0] && body.allNrcNeighbors[0].eNodeBId &&
            body.allNrcNeighbors[0].gNodeBDUs && body.allNrcNeighbors[0].gNodeBDUs[0] &&
            body.allNrcNeighbors[0].gNodeBDUs[0].gNodeBDUId) {
            let found = false;
            for (let i = 0; i < body.allNrcNeighbors.length && !found; i++) {
                for (let j = 0; j < body.allNrcNeighbors[i].gNodeBDUs.length && !found; j++) {
                    if (body.allNrcNeighbors[i].gNodeBDUs[j].gNodeBDUId === options['allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId']) {
                        found = true;
                    }
                }
            }
            if (!found) {
                result = false;
            }
        }
    }

    if (options['allNrcNeighbors[?].gNodeBDUs[?].nrCellIds']) {
        checkText = checkText.concat(' nrCellIds: '.concat(options['allNrcNeighbors[?].gNodeBDUs[?].nrCellIds'])).concat(',');
        if (body && body.allNrcNeighbors && body.allNrcNeighbors[0] && body.allNrcNeighbors[0].eNodeBId &&
            body.allNrcNeighbors[0].gNodeBDUs && body.allNrcNeighbors[0].gNodeBDUs[0] &&
            body.allNrcNeighbors[0].gNodeBDUs[0].nrCellIds && body.allNrcNeighbors[0].gNodeBDUs[0].nrCellIds[0] &&
            options['allNrcNeighbors[?].gNodeBDUs[?].nrCellIds'][0]) {
            let found = false;
            for (let i = 0; i < body.allNrcNeighbors.length && !found; i++) {
                for (let j = 0; j < body.allNrcNeighbors[i].gNodeBDUs.length && !found; j++) {
                    if (body.allNrcNeighbors[i].gNodeBDUs[j].nrCellIds.every(k =>
                        options['allNrcNeighbors[?].gNodeBDUs[?].nrCellIds'].some(l => k == l))) {
                        found = true;
                    }
                }
            }
            if (!found) {
                result = false;
            }
        }
    }

    if (checkText.includes(',')) {
        checkText = checkText.slice(0, -1);
    }

    check(response, {
        [checkText]:
            (r) => result
    });
}

function nrcStatusEndCondition(response, ellapsedTime, timeout, options) {
    const body = parseJson(response.body);
    if (body.process && body.process.status) {
        logData('POLL', {'nrcStatus': body.process.status});
    }
    return {
        finished:
            response.status !== 200 ||
            ellapsedTime > timeout ||
            !body ||
            !body.process ||
            !body.process.nrcStatus ||
            body.process.nrcStatus === options['nrcStatus'] ||
            body.process.nrcStatus === 'Succeeded' ||
            body.process.nrcStatus === 'Failed'
    };
}

function health(options = {}) {
    return nrcHttpGet(constants.healthUri, options);
}

function health200(options = {}) {
    options['status'] = 200;
    return health(options);
}

function prometheus(options = {}) {
    return nrcHttpGet(constants.prometheusUri, options);
}

function prometheus200(options = {}) {
    options['status'] = 200;
    return prometheus(options);
}

function metrics(metric, options = {}) {
    return nrcHttpGet(constants.metricsUri.concat(metric), options);
}

function metrics200(metric, options = {}) {
    options['status'] = 200;
    return metrics(metric, options);
}

function startNrc(nrcRequest, options = {}) {
    return nrcHttpPost(constants.startNrcUri, nrcRequest, getStartNrcParams(), options);
}

function startNrc200(nrcRequest, options = {}) {
    options['status'] = 200;
    return startNrc(nrcRequest, options);
}

function startNrc400(nrcRequest, options = {}) {
    options['status'] = 400;
    options['includes'] = 'Bad Request';
    return startNrc(nrcRequest, options);
}

function monitoring(options = {}) {
    return nrcHttpGet(constants.monitoringUri, options);
}

function monitoring200(options = {}) {
    options['status'] = 200;
    return monitoring(options);
}

function monitoringId(id, options = {}) {
    let uri = constants.monitoringUri.concat('/').concat(strip(id, '\"'));
    if (options['nrcStatus']) {
        let response = waitingForResponse(
            () => httpGet(constants.serviceUrl, constants.serviceRestUri.concat(uri), getSessionParams(), options),
            (response, ellapsedTime, timeout, options) => nrcStatusEndCondition(response, ellapsedTime, timeout, options),
            options['timeout'] ? options['timeout'] : constants.defaultTimeout,
            options['sleepTime'] ? options['sleepTime'] : constants.defaultSleepTime,
            options
        );
        nrcCheckResponse(response, options);
        return response;
    } else {
        return nrcHttpGet(uri, options);
    }
}

function monitoringId200(id, options = {}) {
    options['status'] = 200;
    return monitoringId(id, options);
}

function monitoringId200Succeeded(id, options = {}) {
    options['nrcStatus'] = 'Succeeded';
    return monitoringId200(id, options);
}

function monitoringId200Failed(id, options = {}) {
    options['nrcStatus'] = 'Failed';
    return monitoringId200(id, options);
}

function monitoringId400(id, options = {}) {
    options['status'] = 400;
    options['includes'] = 'Bad Request';
    return monitoringId(id, options);
}

function monitoringId404(id, options = {}) {
    options['status'] = 404;
    return monitoringId(id, options);
}

module.exports = {
    health,
    health200,
    prometheus,
    prometheus200,
    metrics,
    metrics200,
    startNrc,
    startNrc200,
    startNrc400,
    monitoring,
    monitoring200,
    monitoringId,
    monitoringId200,
    monitoringId200Succeeded,
    monitoringId200Failed,
    monitoringId400,
    monitoringId404,
    isNrcStatus,
    checkMonitoringIdResponse
}