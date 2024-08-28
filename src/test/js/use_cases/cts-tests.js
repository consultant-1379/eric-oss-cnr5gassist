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
import * as ctsConstants from '../modules/cts-constants.js';
import { roundNumber } from '../modules/common.js';
import { coordinatesFromVectorOffset, createEnodebCtsData, cleanupEnodebCtsData, createGndbuCtsData, cleanupGndbuCtsData, distanceFromCoordinates } from '../modules/cts-common.js';
import { check } from 'k6';

function createEnodebCtsDataTest() {
    const enodebOuterDist = 450;
    const enodebDist = 150;
    let result = createEnodebCtsData({'nodeCount': 10, 'cellCount': 8, 'enodebOuterDist': enodebOuterDist, 'enodebDist': enodebDist});
    check(result, {
        'jsonHolder type': (r) => r.jsonHolder.type === 'gs/jsonHolder',
        'json array length is 251': (r) => r.jsonHolder.json.length === 251,
        '0th type value is ctw/wirelessNetwork': (r) => r.jsonHolder.json[0].$type === 'ctw/wirelessNetwork',
        '0th action value is reconcile': (r) => r.jsonHolder.json[0].$action === 'reconcile',
        '0th refId value is CNRTestNetwork': (r) => r.jsonHolder.json[0].$refId === 'CNRTestNetwork',
        '1st type value is ctw/wirelessNetwork': (r) => r.jsonHolder.json[1].$type === 'ctw/eNodeB',
        '1st action value is reconcile': (r) => r.jsonHolder.json[1].$action === 'reconcile',
        '1st refId value is CNRTestENB1': (r) => r.jsonHolder.json[1].$refId === 'Ireland/Westmeath/CNRTestENB1/enodeb=CNRTestENB1',
        '2nd type value is ctw/wirelessNetwork': (r) => r.jsonHolder.json[2].$type === 'ctg/geographicLocation',
        '2nd action value is reconcile': (r) => r.jsonHolder.json[2].$action === 'reconcile',
        '2nd refId value is CNRTestENB1/geoLocation:SectorCarrier=1': (r) => r.jsonHolder.json[2].$refId === 'Ireland/Westmeath/CNRTestENB1/geoLocation:SectorCarrier=1',
        '2nd geolocation value is (53.43, -7.9)': (r) =>
            roundNumber(r.jsonHolder.json[2].geospatialData.coordinates[1], 2) === ctsConstants.lat &&
            roundNumber(r.jsonHolder.json[2].geospatialData.coordinates[0], 1) === ctsConstants.lon,
        '27th geolocation is (53.43, -7.880)': (r) =>
            roundNumber(r.jsonHolder.json[27].geospatialData.coordinates[1], 2) === ctsConstants.lat &&
            roundNumber(r.jsonHolder.json[27].geospatialData.coordinates[0], 4) ===
            roundNumber(coordinatesFromVectorOffset(90, enodebOuterDist * (8 / 2) + enodebDist, ctsConstants.lat, ctsConstants.lon)[1], 4),
        'distance check between #2 and #27': (r) =>
            distanceFromCoordinates(r.jsonHolder.json[2].geospatialData.coordinates[1], r.jsonHolder.json[27].geospatialData.coordinates[1],
                r.jsonHolder.json[2].geospatialData.coordinates[0], r.jsonHolder.json[27].geospatialData.coordinates[0]) < (enodebOuterDist + 1) * (8 / 2)
    });
}

function createGndbuCtsDataTest() {
    const gnbduDist = 325;
    const gnodebDist = 125;
    let result = createGndbuCtsData({'nodeCount': 10, 'cellCount': 4, 'gnbduDist': gnbduDist, 'gnodebDist': gnodebDist});
    check(result, {
        'jsonHolder type': (r) => r.jsonHolder.type === 'gs/jsonHolder',
        '0th type value is ctw/wirelessNetwork': (r) => r.jsonHolder.json[0].$type === 'ctw/wirelessNetwork',
        '0th action value is reconcile': (r) => r.jsonHolder.json[0].$action === 'reconcile',
        '1st $type value is ctw/gnbdu': (r) => r.jsonHolder.json[1].$type === 'ctw/gnbdu',
        '2nd $type value is ctw/gnbcucp': (r) => r.jsonHolder.json[2].$type === 'ctw/gnbcucp',
        '2nd $type value is ctw/netFunctionCon': (r) => r.jsonHolder.json[3].$type === 'ctw/netFunctionCon',
        '3rd $type value is ctg/geographicLocation': (r) => r.jsonHolder.json[4].$type === 'ctg/geographicLocation',
        '3rd geolocation value is (53.43, -7.9)': (r) =>
            roundNumber(r.jsonHolder.json[4].geospatialData.coordinates[0], 2) === ctsConstants.lat &&
            roundNumber(r.jsonHolder.json[4].geospatialData.coordinates[1], 1) === ctsConstants.lon,
        '4th $type value is ctg/geographicSite': (r) => r.jsonHolder.json[5].$type === 'ctg/geographicSite',
        '5th geolocation value is (53.43, -7.8965)': (r) =>
            roundNumber(r.jsonHolder.json[6].geospatialData.coordinates[0], 2) === ctsConstants.lat &&
            roundNumber(r.jsonHolder.json[6].geospatialData.coordinates[1], 4) ===
            roundNumber(coordinatesFromVectorOffset(90, gnodebDist * 2, ctsConstants.lat, ctsConstants.lon)[1], 4),
        'distance check between 3rd and 5th': (r) =>
            distanceFromCoordinates(r.jsonHolder.json[4].geospatialData.coordinates[1], r.jsonHolder.json[6].geospatialData.coordinates[1],
                r.jsonHolder.json[4].geospatialData.coordinates[0], r.jsonHolder.json[6].geospatialData.coordinates[0]) < gnbduDist + 1,
        '12th $type value is ctw/nrCell': (r) => r.jsonHolder.json[12].$type === 'ctw/nrCell',
        '12th downlinkEARFCN value is 26500': (r) => r.jsonHolder.json[12].downlinkEARFCN === ctsConstants.downlinkEARFCN[0],
        '13th physicalCelIdentity value is 139': (r) => r.jsonHolder.json[13].physicalCellIdentity === ctsConstants.physicalCellIdentity + 1,
    });
}

function cleanupEnodebCtsDataTest() {
    let result = cleanupEnodebCtsData({'nodeCount': 10, 'cellCount': 8});
    check(result, {
        'jsonHolder type': (r) => r.jsonHolder.type === 'gs/jsonHolder',
        'json array length is 251': (r) => r.jsonHolder.json.length === 250,
        '0th action value is delete': (r) => r.jsonHolder.json[0].$action === 'delete',
        '0th type value is ctw/eNodeB': (r) => r.jsonHolder.json[0].$type === 'ctw/eNodeB',
        '1st type value is ctg/geographicLocation': (r) => r.jsonHolder.json[1].$type === 'ctg/geographicLocation',
        '1st name value is Ireland/Westmeath/CNRTestENB1/geoLocation:SectorCarrier=1': (r) => r.jsonHolder.json[1].name === 'Ireland/Westmeath/CNRTestENB1/geoLocation:SectorCarrier=1',
        '2nd type value is ctg/geographicSite': (r) => r.jsonHolder.json[2].$type === 'ctg/geographicSite',
        '3rd type value is ctg/geographicLocation': (r) => r.jsonHolder.json[3].$type === 'ctg/geographicLocation',
        '17th type value is ctw/lteCell': (r) => r.jsonHolder.json[17].$type === 'ctw/lteCell',
        '25th type value is ctw/eNodeB': (r) => r.jsonHolder.json[25].$type === 'ctw/eNodeB',
    });
}

function cleanupGndbuCtsDataTest() {
    let result = cleanupGndbuCtsData({'nodeCount': 10, 'cellCount': 4});
    check(result, {
        'jsonHolder type': (r) => r.jsonHolder.type === 'gs/jsonHolder',
        'json array length is 150': (r) => r.jsonHolder.json.length === 10 * 15,
        '0th action value is delete': (r) => r.jsonHolder.json[0].$action === 'delete',
        '0th $type value is ctw/gnbdu': (r) => r.jsonHolder.json[0].$type === 'ctw/gnbdu',
        '1st $type value is ctw/gnbcucp': (r) => r.jsonHolder.json[1].$type === 'ctw/gnbcucp',
        '2nd $type value is ctw/netFunctionCon': (r) => r.jsonHolder.json[2].$type === 'ctw/netFunctionCon',
        '3rd type value is ctg/geographicLocation': (r) => r.jsonHolder.json[3].$type === 'ctg/geographicLocation',
        '3rd name value is Ireland/Westmeath/CNRTestGNB1/geoLocation:NRSectorCarrier=1': (r) => r.jsonHolder.json[3].name === 'Ireland/Westmeath/CNRTestGNB1/geoLocation:NRSectorCarrier=1',
        '4th type value is ctg/geographicSite': (r) => r.jsonHolder.json[4].$type === 'ctg/geographicSite',
        '5th type value is ctg/geographicLocation': (r) => r.jsonHolder.json[5].$type === 'ctg/geographicLocation',
        '6th type value is ctg/geographicSite': (r) => r.jsonHolder.json[6].$type === 'ctg/geographicSite',
        '11th type value is ctw/nrCell': (r) => r.jsonHolder.json[11].$type === 'ctw/nrCell',
        '15th $type value is ctw/gnbdu': (r) => r.jsonHolder.json[15].$type === 'ctw/gnbdu',
        '15th name value is Ireland/Westmeath/CNRTestGNB2/gnbdu=CNRTestGNB2': (r) => r.jsonHolder.json[15].name === 'Ireland/Westmeath/CNRTestGNB2/gnbdu=CNRTestGNB2'
    });
}

module.exports = {
    createEnodebCtsDataTest,
    createGndbuCtsDataTest,
    cleanupEnodebCtsDataTest,
    cleanupGndbuCtsDataTest
}