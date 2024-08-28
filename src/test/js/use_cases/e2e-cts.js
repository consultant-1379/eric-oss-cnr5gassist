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

import { group, check } from 'k6';
import * as constants from '../modules/constants.js';
import * as ctsConstants from '../modules/cts-constants.js';
import * as e2eConstants from '../modules/e2e-constants.js';
import { startNrc200, monitoringId200Succeeded } from '../modules/nrc-common.js';
import * as ctsCommon from '../modules/cts-common.js'
import { e2eObjects, nodeRadius, nodeLat, enodeb1Lon, gnodeb1Lon } from '../modules/e2e-constants.js';

function healthCheck() {
    group('CTS health check', function() {
        const enodebCount = ctsCommon.getEnodebCount({'status': 200, 'valid': true});
        check(enodebCount, {
            'The CTS is not empty': (r) => parseInt(r) > 0
        });
    });
}

function isInList(cell, name) {
    return cell.name === name;
}


function queryEnodebLtecells() {
    group('Query all the lteCells of enodeb1 and it is expected to have 6 different lteCells', function() {
        const lteCells = e2eObjects.enodeb1 ? ctsCommon.getAllCells({'fdn':  e2eObjects.enodeb1.name, 'type': ctsConstants.ltecell}) :  {};
        check(lteCells, {
            '0th enodeb type is ctw/enodeb': (r) => r.type === 'ctw/enodeb',
            '0th enodeb name is correct': (r) => r.name === e2eConstants.enodebFdn1,
            '0th lteCell type value is ctw/ltecell': (r) => r.lteCells && r.lteCells[0].value.type === 'ctw/ltecell',
            'lteCell nr 1 is in the response': (r) => r.lteCells.some(cell => cell.value.name.includes(e2eConstants.enoden1 + '-1')),
            'lteCell nr 2 is in the response': (r) => r.lteCells.some(cell => cell.value.name.includes(e2eConstants.enoden1 + '-2')),
            'lteCell nr 3 is in the response': (r) => r.lteCells.some(cell => cell.value.name.includes(e2eConstants.enoden1 + '-3')),
            'lteCell nr 6 is in the response': (r) => r.lteCells.some(cell => cell.value.name.includes(e2eConstants.enoden1 + '-6')),
        });
    });
}

function queryGnbdu() {
    group('Query all the nrCells of gnbdu1 and it is expected to have 4 different nrCells and 4 nrSectorCarriers', function() {
        const fdnId = e2eObjects.gnbdu1 ? e2eObjects.gnbdu1.name : '';
        const nrCells = ctsCommon.getAllCells({'fdn': fdnId, 'type': ctsConstants.nrcell});
        check(nrCells, {
            'gnodeb type is ctw/gnbdu': (r) => r.type === 'ctw/gnbdu',
            'gnodeb name value is correct': (r) => r.name === e2eConstants.gnbduFdn1,
            '0th nrCell type value is ctw/nrcell': (r) =>
                r.nrCells && r.nrCells[0].value.type === 'ctw/nrcell',
            'nrcell nr 1 is in the response': (r) => r.nrCells.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '-1')),
            'nrcell nr 2 is in the response': (r) => r.nrCells.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '-2')),
            'nrcell nr 3 is in the response': (r) => r.nrCells.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '-3')),
            'nrcell nr 4 is in the response': (r) => r.nrCells.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '-4')),
        });

        const nrSectorCarriers = ctsCommon.getNrSectorCarriers({'fdn': fdnId});
        check(nrSectorCarriers, {
            'gnodeb type is ctw/gnbdu': (r) => r.type === 'ctw/gnbdu',
            'nrSectorCarrier nr 1 is in the response': (r) => r.nrSectorCarriers.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '/1/1')),
            'nrSectorCarrier nr 2 is in the response': (r) => r.nrSectorCarriers.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '/1/2')),
            'nrSectorCarrier nr 3 is in the response': (r) => r.nrSectorCarriers.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '/1/3')),
            'nrSectorCarrier nr 4 is in the response': (r) => r.nrSectorCarriers.some(cell => cell.value.name.includes(e2eConstants.gnoden1 + '/1/4')),
        });

        const wirelessNetworks = ctsCommon.getWirelessNetworks({'fdn': fdnId});
        check(wirelessNetworks, {
            'gnodeb type is ctw/gnbdu': (r) => r.type === 'ctw/gnbdu',
            'gnodeb name value is correct': (r) => r.name ===  e2eConstants.gnbduFdn1,
            'wirelessNetwork name value is 128-49': (r) =>
                r.wirelessNetworks && r.wirelessNetworks[0].value.name === '128-49',
            'wirelessNetwork mcc value is 128': (r) =>
                r.wirelessNetworks && r.wirelessNetworks[0].value.mcc === 128,
            'wirelessNetwork mnc value is 49': (r) =>
                r.wirelessNetworks && r.wirelessNetworks[0].value.mnc === '49',
        });
    });
}
function queryLTECellsGeographics() {
    group('Query the geographicSite of the first ltecell of enodeb1 and use the obtained ID to get geographic location that has the coordinates that are used in geo gueries', function() {
        const lteCells = e2eObjects.enodeb1 ? ctsCommon.getAllCells({'fdn': e2eObjects.enodeb1.name, 'type': ctsConstants.ltecell}) : {};
        const ltecellId = lteCells.lteCells ? JSON.stringify(lteCells.lteCells[0].value.id) : '';
        const ltecellGeographicSite = ctsCommon.getCellGeographicSite({'ltecell': ltecellId});
        check(ltecellGeographicSite, {
            'Check geographic site is set': (r) => r.geographicSite
        });

        if (ltecellGeographicSite.geographicSite) {
            const ltecellGeographicSiteId = ltecellGeographicSite.geographicSite[0] ? JSON.stringify(ltecellGeographicSite.geographicSite[0].value.id) : '';
            const geographicSiteLocatedAt = ctsCommon.getGeographicsite({'geographicSiteId': ltecellGeographicSiteId});
            check(geographicSiteLocatedAt, {
                'Check locatedAt type value is ctg/geographiclocation': (r) =>
                    r.locatedAt && r.locatedAt[0].value.type === 'ctg/geographiclocation',
                'Check geospatialData type value is Point': (r) =>
                    r.locatedAt && r.locatedAt[0].value.geospatialData.type === 'Point',
                'Check coordinates are within the node radius': (r) =>
                    r.locatedAt && ctsCommon.distanceFromCoordinates(r.locatedAt[0].value.geospatialData.coordinates[1], nodeLat,
                        r.locatedAt[0].value.geospatialData.coordinates[0], enodeb1Lon) < (nodeRadius+1) * (1 / 1)
            });

            const coordinateX = geographicSiteLocatedAt.locatedAt ? parseFloat(JSON.stringify(geographicSiteLocatedAt.locatedAt[0].value.geospatialData.coordinates[0])) : '';
            const coordinateY = geographicSiteLocatedAt.locatedAt ? parseFloat(JSON.stringify(geographicSiteLocatedAt.locatedAt[0].value.geospatialData.coordinates[1])) : '';
            const coordinates = [coordinateX, coordinateY];
            const geoQuery = ctsCommon.getGeoQuery({'coordinate': coordinates, 'distance': ctsConstants.distance, 'celltype': ctsConstants.ltecell});
            check(geoQuery, {
                '0th type value is ctw/ltecell': (r) =>
                    r[0] && r[0].type === 'ctw/ltecell',
                'lteCell nr 1 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.enoden1 + '-1')),
                'lteCell nr 2 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.enoden1 + '-2')),
                'lteCell nr 3 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.enoden1 + '-3')),
                'lteCell nr 5 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.enoden1 + '-5')),
                'lteCell nr 6 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.enoden1 + '-6')),
            });
        }
    });
}

function queryNrCellsGeographics() {
    group('Query the geographicSite of the first nrcell of gnbdu1 and use the obtained ID to get geographic location that has the coordinates that are used in geo queries', function() {
        const nrCells = e2eObjects.gnbdu1 ? ctsCommon.getAllCells({'fdn': e2eObjects.gnbdu1.name, 'type': ctsConstants.nrcell}) : {};
        const nrCellId = nrCells.nrCells ? JSON.stringify(nrCells.nrCells[0].value.id) : '';
        const nrCellGeographicSite = ctsCommon.getCellGeographicSite({'nrcell': nrCellId});
        check(nrCellGeographicSite, {
            'Check geographic site is set': (r) => r.geographicSite
        });

        if (nrCellGeographicSite.geographicSite) {
            const nrCellGeographicSiteId = nrCellGeographicSite.geographicSite[0] ? JSON.stringify(nrCellGeographicSite.geographicSite[0].value.id) : '';
            const geographicSiteLocatedAt = ctsCommon.getGeographicsite({'geographicSiteId': nrCellGeographicSiteId});
            check(geographicSiteLocatedAt, {
                'Check locatedAt type value is ctg/geographiclocation': (r) =>
                    r.locatedAt && r.locatedAt[0].value.type === 'ctg/geographiclocation',
                'Check geospatialData type value is Point': (r) =>
                    r.locatedAt && r.locatedAt[0].value.geospatialData.type === 'Point',
                'Check coordinates are within the node radius': (r) =>
                    r.locatedAt && ctsCommon.distanceFromCoordinates(r.locatedAt[0].value.geospatialData.coordinates[1], nodeLat,
                        r.locatedAt[0].value.geospatialData.coordinates[0], gnodeb1Lon) < (nodeRadius+1) * (1 / 1)
            });

            const coordinateX = geographicSiteLocatedAt.locatedAt ? parseFloat(JSON.stringify(geographicSiteLocatedAt.locatedAt[0].value.geospatialData.coordinates[0])) : '';
            const coordinateY = geographicSiteLocatedAt.locatedAt ? parseFloat(JSON.stringify(geographicSiteLocatedAt.locatedAt[0].value.geospatialData.coordinates[1])) : '';
            const coordinates = [coordinateX, coordinateY];
            const geoQuery = ctsCommon.getGeoQuery({'coordinate': coordinates, 'distance': ctsConstants.distance, 'celltype': ctsConstants.nrcell});
            check(geoQuery, {
                'nrCell nr 1 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.gnoden1 + '-1')),
                'nrCell nr 2 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.gnoden1 + '-2')),
                'nrCell nr 3 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.gnoden1 + '-3')),
                'nrCell nr 4 is in the response': (r) => r.some(cell => cell.name.includes(e2eConstants.gnoden1 + '-4')),
            });
        }
    });
}

module.exports = {
    healthCheck,
    queryEnodebLtecells,
    queryGnbdu,
    queryLTECellsGeographics,
    queryNrCellsGeographics
}