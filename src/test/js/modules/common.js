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

import http from 'k6/http';
import {group, check, sleep} from 'k6';
import {textSummary} from './k6-summary.js';
import * as constants from './constants.js';

let sessionId;

function runSummary(data) {
    logData('EVAL', data);
    return textSummary(data, {indent: ' '});
}

function httpGet(url, uri, params = {}, options = {}) {
    const timeout = options['timeout'] ? options['timeout'] : constants.defaultTimeout;
    params['timeout'] = timeout.toString().concat('s');
    logData('GET '.concat(url.concat(uri)), params);
    let response = {};
    let retryCount = 0;
    while (retryCount < constants.maxRetry) {
        response = http.get(url.concat(uri), params);
        if (response && response.status) {
            break;
        } else {
            retryCount += 1;
            logData('RETRY: '.concat(retryCount));
        }
    }
    options['requestType'] = constants.get;
    logData('GET RESPONSE', response);
    return response;
}

function httpPost(url, uri, request, params = {}, options = {}) {
    const timeout = options['timeout'] ? options['timeout'] : constants.defaultTimeout;
    params['timeout'] = timeout.toString().concat('s');
    logData('POST '.concat(url.concat(uri)), request);
    let response = {};
    let retryCount = 0;
    while (retryCount < constants.maxRetry) {
        response = http.post(url.concat(uri), JSON.stringify(request), params)
        if (response && response.body) {
            break;
        } else {
            retryCount += 1;
            logData('RETRY: '.concat(retryCount));
        }
    }
    options['requestType'] = constants.post;
    logData('POST RESPONSE', response);
    return response;
}

function getSessionId(options = {}) {
    if (!sessionId) {
        const response = httpPost(constants.ingressUrl, constants.ingressLoginUri, '', constants.ingressLoginParams, options);
        sessionId = response.status === 200 && response.body ? response.body : '';
        logData('JSESSIONID: '.concat(sessionId));
    }
    return sessionId;
}

function waitingForResponse(operation, endCondition, timeOut = constants.defaultTimeout, sleepTime = constants.defaultSleepTime, options = {}) {
    const startTime = new Date();
    let response;
    let retry = true;
    while (retry) {
        response = operation();
        retry = !endCondition(response, ((new Date()) - startTime) / 1000, timeOut, options).finished;
        if (retry) sleep(sleepTime);
    }
    return response;
}

function roundNumber(num, scale) {
    if (!("" + num).includes("e")) {
        return +(Math.round(num + "e+" + scale) + "e-" + scale);
    } else {
        const arr = ("" + num).split("e");
        let sig = "";
        if (+arr[1] + scale > 0) {
            sig = "+";
        }
        return +(Math.round(+arr[0] + "e" + sig + (+arr[1] + scale)) + "e-" + scale);
    }
}

function strip(trimmableString, trimmedCharacter) {
    return trimmableString.replace(new RegExp(trimmedCharacter, 'g'), '').replace(/\\/g, "");
}

function parseJson(data) {
    try {
        return JSON.parse(data);
    } catch(error) {
        logData('Json parse error: '.concat(error));
        return {};
    }
}

function logData(message, data = '') {
    console.log("<<");
    console.log(
        new Date().toISOString(),
        (typeof __ITER !== 'undefined' && typeof __VU !== 'undefined') ? `ITER:${__ITER} VU:${__VU} -` : '',
        message,
        strip(JSON.stringify(data, null), '\"')
    );
    console.log(">>");
}

function requestType(options) {
    return options['requestType'] ? options['requestType'] : '';
}

function isArray(object) {
    return object && object[0] && typeof object === 'object';
}

function responseBodyIncludes(response, body, text) {
    if ((response && response.body && response.body.includes(text)) ||
        (body && body.includes(text))) {
        return true;
    } else {
        return false;
    }
}

function checkResponse(response, options) {
    if (options['status']) {
        check(response, {
            [requestType(options) + 'response status is ' + options['status']]:
                (r) => r.status === options['status']
        });
    }

    if (options['valid']) {
        check(parseJson(response.body), {
            'The response body is valid non empty JSON': (r) => r && JSON.stringify(r) !== '{}'
        });
    }

    if (options['includes']) {
        const body = strip(JSON.stringify(parseJson(response.body), null), '\"');
        if (isArray(options['includes'])) {
            logData('INCLUDES: '.concat(strip(JSON.stringify(options['includes'], null), '\"')));
            let allIncluded = true;
            for (var i = 0; i < options['includes'].length; i++) {
                if (!responseBodyIncludes(response, body, options['includes'][i])) {
                    allIncluded = false;
                }
            }
            check(response, {
                [requestType(options) + 'response body contains ' + strip(JSON.stringify(options['includes'], null), '\"')]:
                    allIncluded
            });
        } else {
            logData('INCLUDES: '.concat(strip(options['includes'], '\"')));
            check(response, {
                [requestType(options) + 'response body contains ' + strip(options['includes'], '\"')]:
                    responseBodyIncludes(response, body, options['includes'])
            });
        }
    }
}

module.exports = {
    runSummary,
    httpGet,
    httpPost,
    getSessionId,
    waitingForResponse,
    strip,
    parseJson,
    logData,
    checkResponse,
    roundNumber,
    requestType
}