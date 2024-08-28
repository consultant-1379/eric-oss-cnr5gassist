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
import {
    health200,
    startNrc200,
    startNrc400,
    monitoring200,
    monitoringId} from '../modules/nrc-common.js';
import {check, group} from 'k6';

function startNrc() {
     startNrc200({'eNodeBIds': [29]});
     startNrc400({'eNodeBIds': []});
}

function monitoring() {
    const response = monitoring200();
    check(response, {
        'NRC history length > 0': (r) => r.body && JSON.parse(r.body).length > 0,
    });
}

function monitoringById() {
    const response = monitoringId('11111111-1111-1111-1111-111111111111');
    check(response, {
        'endpoint exists': (r) => r.status > 0,
    });
}

module.exports = {
    startNrc,
    monitoring,
    monitoringById
}