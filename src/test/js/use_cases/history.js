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

import {check, sleep} from 'k6';
import {strip} from '../modules/common.js';
import {startNrc, monitoringId200, monitoring} from '../modules/nrc-common.js';

function postRequest(idList, requestBody) {
    let response = startNrc(requestBody);

    if (response.status === 200 || response.status === 208) {
        idList.push(strip(response.body, '"'));

        if (response.status === 200) {
            sleep(2.5);
        }
    }

    return idList;
}

function sendRequests(idList, numOfReq) {
    let startEnodeBid = 1;

    for (let i = 0; i < numOfReq; i++) {
        let enodeBid = startEnodeBid + i;
        let reqBody = {"eNodeBIds":[enodeBid],"distance":600,"freqPairs":{"5230":[177000]}};
        idList = postRequest(idList, reqBody);
    }

    return idList;
}

function checkInMonitoring(idList, id, counter) {

    if (idList.includes(id)) {
        counter += 1;
    }

    return counter;
}

function history() {
    let reqNum = 20;
    let idList = [];

    idList = sendRequests(idList, reqNum);
    let numOfIds = idList.length;

    let minReqNum = reqNum / 2;
    check(numOfIds, {['Accepted requests > ' + minReqNum]: (l) => l > minReqNum });

    idList.forEach((id) => monitoringId200(id));
    let monList = JSON.parse(monitoring().body);

    let counter = 0;
    monList.forEach((le) => counter = checkInMonitoring(idList, strip(le['id'], '"'), counter));
    check(numOfIds, {['Have ids in monitoring ' + numOfIds]: (l) => l === counter });
}

module.exports = {
    history
}