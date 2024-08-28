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

import * as common from './common.js';
import * as ncmpConstants from './ncmp-constants.js';

function getCmHandle(options = {}) {
    if (options['mo']) {
        return getCmHandle({'externalId': options['mo'].externalId})
    } else if (options['externalId'] && options['externalId'].includes('/')) {
        return options['externalId'].split('/')[0];
    } else {
        return '';
    }
}

function getResourceIdentifier(options = {}) {
    if (options['mo'] && options['mo'].externalId && options['mo'].externalId.indexOf('/') > 0) {
        return options['mo'].externalId.substr(options['mo'].externalId.indexOf('/'));
    } else {
        return '';
    }
}

function getSessionParams() {
    return {
        headers: {
            'Content-Type': 'application/yang-data+json',
            'Cookie': 'JSESSIONID='.concat(common.getSessionId())
        }
    };
}

function checkResponse(response, options) {
    common.checkResponse(response, options);
}

function getManagedObject(path, options = {}) {
    const uri = ncmpConstants.ncmpBasePath
        .concat(getCmHandle(options))
        .concat(ncmpConstants.resourceIdentifier)
        .concat(path);
    const response = common.httpGet(ncmpConstants.ncmpUrl, uri, getSessionParams(), options);
    options['status'] = 200;
    checkResponse(response, options);
    const body = common.parseJson(response.body);
    return response.status === 200 && body[0] ? body[0] : {};
}

function getENodeBFunction(options = {}) {
    return getManagedObject(ncmpConstants.eNodeBFunction, options);
}

function getGNBDUFunction(options = {}) {
    return getManagedObject(ncmpConstants.gNBDUFunction, options);
}

function getGUtraNetwork(options = {}) {
    return getManagedObject(
        ncmpConstants.eNodeBFunction
        .concat(ncmpConstants.gUtraNetwork), options);
}

function getExternalGNodeBFunction(options = {}) {
    return getManagedObject(
        ncmpConstants.eNodeBFunction
        .concat(ncmpConstants.gUtraNetwork)
        .concat(ncmpConstants.externalGNodeBFunctionFilter), options);
}

function getGUtranSyncSignalFrequency(options = {}) {
    return getManagedObject(
        ncmpConstants.eNodeBFunction
        .concat(ncmpConstants.gUtraNetwork)
        .concat(ncmpConstants.gUtranSyncSignalFrequencyFilter), options);
}

function getDnPrefix(options = {}) {
    return getManagedObject(ncmpConstants.dnPrefixFilter, options);
}

function getExternalGUtranCell(options = {}) {
    return getManagedObject(
        ncmpConstants.eNodeBFunction
        .concat(ncmpConstants.gUtraNetwork)
        .concat(ncmpConstants.externalGNodeBFunction)
        .concat(ncmpConstants.externalGUtranCellFilter), options);
}

function getTermPointToGNB(options = {}) {
    return getManagedObject(ncmpConstants.termPointToGNBFilter, options);
}

function getNRCellCU(options = {}) {
    return getManagedObject(ncmpConstants.nRCellCUFilter, options);
}

function getNRSectorCarrier(options = {}) {
    return getManagedObject(
        getResourceIdentifier(options)
        .concat(ncmpConstants.nRSectorCarrierFilter), options);
}

module.exports = {
    getENodeBFunction,
    getGNBDUFunction,
    getGUtraNetwork,
    getExternalGNodeBFunction,
    getGUtranSyncSignalFrequency,
    getDnPrefix,
    getExternalGUtranCell,
    getTermPointToGNB,
    getNRCellCU,
    getNRSectorCarrier
}