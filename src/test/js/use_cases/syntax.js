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

import {startNrc200,
        startNrc400,
        monitoringId200Succeeded,
        monitoringId200Failed} from '../modules/nrc-common.js';
import {check} from 'k6';

function nrcSucceeded() {
    const response = startNrc200({'eNodeBIds':[29],'distance':200});
    monitoringId200Succeeded(response.body);
}

function noEnodebField() {
    startNrc400({'distance':200,'freqPairs':{'5230':[177000]}});
}

function emptyEnodebList() {
    startNrc400({'eNodeBIds':[],'distance':200,'freqPairs':{'5230':[177000]}});
}

function enodebList500() {
    const nrcRequest = {'eNodeBIds':[],'distance':200,'freqPairs':{'5230':[177000]}}
    for (let i = 0; i < 500; i++) nrcRequest.eNodeBIds.push(10000 + i);
    startNrc200(nrcRequest);

    nrcRequest.eNodeBIds.push(10500);
    startNrc400(nrcRequest);
}

function enodebValueSyntaxError() {
    startNrc400({'eNodeBIds':['xxx',29],'distance':200,'freqPairs':{'5230':[177000]}});
}

function freqPairsKeySyntaxError() {
    // Note: This shouldn't fail because the 'freqPairs' dict key is used as a filter.
    const response = startNrc200({'eNodeBIds':[29],'distance':200,'freqPairs':{'xxx':[177000]}});
    monitoringId200Succeeded(response.body);
}

function distanceNameTypo() {
    // Note: This shouldn't fail because the 'distance' field is optional.
    const response = startNrc200({'eNodeBIds':[29],'distanceX':200,'freqPairs':{'5230':[177000]}});
    monitoringId200Succeeded(response.body);
}

function negativeDistance() {
    startNrc400({'eNodeBIds':[29],'distance':-200,'freqPairs':{'5230':[177000]}});
}

function tooBigDistance() {
    startNrc400({'eNodeBIds':[29],'distance':99999999999999999999999999999999,'freqPairs':{'5230':[177000]}});
}

module.exports = {
    nrcSucceeded,
    noEnodebField,
    emptyEnodebList,
    enodebList500,
    enodebValueSyntaxError,
    freqPairsKeySyntaxError,
    distanceNameTypo,
    negativeDistance,
    tooBigDistance
}