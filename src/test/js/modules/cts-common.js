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
import encoding from 'k6/encoding';
import * as constants from './constants.js';
import * as ctsConstants from './cts-constants.js';
import {getSessionId, httpGet, httpPost, parseJson, checkResponse, runSummary} from './common.js';

function reconcileEnodeb(nodeId) {
    return {
        '$type': ctsConstants.ctwEnodeb,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId,
        'externalId': ctsConstants.ew345ManagedElement + nodeId + ctsConstants.u3gppENodeBFunction + nodeId,
        'status': ctsConstants.operating,
        'operationalState': ctsConstants.enabled,
        'administrativeState': ctsConstants.unlocked,
        '$wirelessNetworks': [
            ctsConstants.cnrTestNetwork
        ]
    };
}

function reconcileGeographicLocation(lat, lon, options = {}) {
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const cellId = options['cellId'] ? options['cellId'] : 1;
    const refId = options['refId'] ? options['refId'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationSectorCarrier + cellId;
    const name = options['name'] ? options['name'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationSectorCarrier + cellId;
    const externalId = options['externalId'] ? options['externalId'] :
        ctsConstants.ew345ManagedElement + nodeId + ctsConstants.geoLocationSectorCarrier + cellId;
    return {
        '$type': ctsConstants.ctgGeographicLocation,
        '$action': ctsConstants.reconcile,
        '$refId': refId,
        'name': name,
        'externalId': externalId,
        'status': ctsConstants.operating,
        'type': ctsConstants.geospatialCoords,
        'geospatialData': {
            'type': ctsConstants.point,
            'coordinates': [
                lon, lat
            ]
        }
    };
}

function reconcileGeographicSite(options = {}) {
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const cellId = options['cellId'] ? options['cellId'] : 1;
    const refId = options['refId'] ? options['refId'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteSectorCarrier + cellId;
    const name = options['name'] ? options['name'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteSectorCarrier + cellId;
    const externalId = options['externalId'] ? options['externalId'] :
        ctsConstants.ew345ManagedElement + nodeId + ctsConstants.geoSiteSectorCarrier + cellId;
    const locatedAt = options['locatedAt'] ? options['locatedAt'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationSectorCarrier + cellId;
    return {
        '$type': ctsConstants.ctgGeographicSite,
        '$action': ctsConstants.reconcile,
        '$refId': refId,
        'name': name,
        'externalId': externalId,
        'status': ctsConstants.operating,
        'type': ctsConstants.region,
        '$locatedAt': [
            locatedAt
        ]
    };
}

function reconcileLteCell(nodeId, cellLocalId, fddEarfcnDl, fddEarfcnUl) {
    return {
        '$type': ctsConstants.ctwLteCell,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId + ctsConstants.slash + cellLocalId,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId + ctsConstants.slash + cellLocalId,
        'externalId': ctsConstants.ew345ManagedElement + ctsConstants.ManagedElement + nodeId +
            ctsConstants.u3gppENodeBFunction + nodeId + ctsConstants.u3gppLtecellDU + cellLocalId,
        'type': 'FDD',
        'FDDearfcnDl': fddEarfcnDl,
        'FDDearfcnUl': fddEarfcnUl,
        'cellLocalId': cellLocalId.toString(),
        'status': ctsConstants.operating,
        '$wirelessNetworks': [
            ctsConstants.cnrTestNetwork
        ],
        '$eNodeB': [
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId
        ],
        '$geographicSite': [
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteSectorCarrier + cellLocalId
        ]
    };
}

function reconcileExistingLteCell(lteCell, options = {}) {
    let data = {
        '$type': ctsConstants.ctwLteCell,
        '$action': ctsConstants.reconcile,
        '$refId': lteCell.name,
        'name': lteCell.name,
        'externalId': lteCell.externalId,
        'FDDearfcnDl': lteCell.FDDearfcnDl,
        'FDDearfcnUl': lteCell.FDDearfcnUl,
        'cellLocalId': lteCell.cellLocalId,
        'status': ctsConstants.operating
    };

    if (options['wirelessNetworks']) data['$wirelessNetworks'] = [ options['wirelessNetworks'] ];
    if (options['gnbdu']) data['$gnbdu'] = [ options['gnbdu'] ];
    if (options['geographicSite']) data['$geographicSite'] = [ options['geographicSite'] ];

    return data;
}

function jsonHolder(json) {
    return {
        'type': ctsConstants.oslAdvProcess,
        'jsonHolder': {
            'type': ctsConstants.gsJsonHolder,
            'json': json
        }
    };
}

function deleteEnodeb(nodeId) {
    return {
        '$type': ctsConstants.ctwEnodeb,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId
    }
}

function deleteGeographicLocation(nodeId, carrier) {
    return {
        '$type': ctsConstants.ctgGeographicLocation,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationSectorCarrier + carrier,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationSectorCarrier + carrier
    }
}

function deleteGeographicSite(nodeId, carrier) {
    return {
        '$type': ctsConstants.ctgGeographicSite,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteSectorCarrier + carrier,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteSectorCarrier + carrier
    }
}

function deleteLteCell(nodeId, carrier) {
    return {
        '$type': ctsConstants.ctwLteCell,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId + ctsConstants.slash + carrier,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.enodeb + nodeId + ctsConstants.slash + carrier
    }
}

function reconcileGnbdu(nodeId) {
    return {
        '$type': ctsConstants.ctwGnbdu,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId,
        'externalId': ctsConstants.externalId + ctsConstants.ericssonEnmManagedElement + nodeId +
            ctsConstants.erienmnrmGnbduFunction + nodeId,
        'status': ctsConstants.operating,
        'administrativeState': ctsConstants.unlocked,
        'operationalState': ctsConstants.enabled,
        'gnbduId': ctsConstants.gnbduId,
        '$wirelessNetworks': [
            ctsConstants.cnrTestNetwork
        ]
    };
}

function reconcileGnbcuCp(nodeId) {
    return {
        '$type': ctsConstants.ctwGnbcucp,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbcuCp1,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbcuCp1,
        'externalId': ctsConstants.externalId + ctsConstants.ericssonEnmManagedElement + nodeId +
            ctsConstants.erienmnrmGnbcuCpFunction1,
        'plmnId': ctsConstants.plmnId,
        'status': ctsConstants.operating,
        '$wirelessNetworks': [
            ctsConstants.cnrTestNetwork
        ]
    };
}

function reconcileNetFunctionCon(nodeId) {
    return {
        '$type': ctsConstants.ctwNetFunctionCon,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.netfunctioncon01,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.netfunctioncon01,
        'externalId': ctsConstants.externalId + ctsConstants.ericssonEnmManagedElement + nodeId,
        'type': ctsConstants.f1cGnbcucpGnbdu,
        'status': ctsConstants.operating,
        '$wirelessNetFunctions': [
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId,
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbcuCp1
        ]
    };
}

function reconcileGnodebGeographicLocation(nodeId, nrSectorCarrier, lat, lon) {
    return {
        '$type': ctsConstants.ctgGeographicLocation,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + nrSectorCarrier,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + nrSectorCarrier,
        'externalId': ctsConstants.externalId + ctsConstants.ericssonEnmManagedElement + nodeId +
            ctsConstants.erienmnrmGnbduNrSectorCarrier + nrSectorCarrier,
        'status': ctsConstants.operating,
        'type': ctsConstants.geospatialCoords,
        'geospatialData': {
            'type': ctsConstants.point,
            'coordinates': [
                lat, lon
            ]
        }
    };
}

function lookUpNrCell(cellName) {
    return {
        '$type': ctsConstants.ctwNrCell,
        '$action': ctsConstants.lookUp,
        '$refId': cellName,
        'name': cellName
    };
}

function reconcileNrCell(nodeId, cellLocalId, downlinkEARFCN, physicalCellIdentity) {
    return {
        '$type': ctsConstants.ctwNrCell,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId + ctsConstants.slash + cellLocalId,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId + ctsConstants.slash + cellLocalId,
        'externalId': ctsConstants.externalId + ctsConstants.ericssonEnmManagedElement + nodeId +
            ctsConstants.erienmnrmGnbduFunction1 + ctsConstants.ericssonEnmNrCellDU + nodeId + ctsConstants.dash + cellLocalId,
        'downlinkEARFCN': downlinkEARFCN,
        'localCellIdNci': cellLocalId,
        'physicalCellIdentity': physicalCellIdentity,
        'trackingAreaCode': ctsConstants.trackingAreaCode,
        'status': ctsConstants.operating,
        '$wirelessNetworks': [
            ctsConstants.cnrTestNetwork
        ],
        '$gnbdu': [
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId
        ],
        '$geographicSite': [
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteNrSectorCarrier + cellLocalId
        ]
    };
}

function reconcileExistingNrCell(nrCell, options = {}) {
    let data = {
        '$type': ctsConstants.ctwNrCell,
        '$action': ctsConstants.reconcile,
        '$refId': nrCell.name,
        'name': nrCell.name,
        'externalId': nrCell.externalId,
        'downlinkEARFCN': nrCell.downlinkEARFCN,
        'localCellIdNci': nrCell.localCellIdNci,
        'physicalCellIdentity': nrCell.physicalCellIdentity,
        'trackingAreaCode': nrCell.trackingAreaCode,
        'status': ctsConstants.operating
    };

    if (options['wirelessNetworks']) data['$wirelessNetworks'] = [ options['wirelessNetworks'] ];
    if (options['gnbdu']) data['$gnbdu'] = [ options['gnbdu'] ];
    if (options['geographicSite']) data['$geographicSite'] = [ options['geographicSite'] ];

    return data;
}

function reconcileGnodebGeographicSite(nodeId, nrSectorCarrier) {
    return {
        '$type': ctsConstants.ctgGeographicSite,
        '$action': ctsConstants.reconcile,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteNrSectorCarrier + nrSectorCarrier,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoSiteNrSectorCarrier + nrSectorCarrier,
        'externalId': ctsConstants.externalId + ctsConstants.ericssonEnmManagedElement + nodeId +
            ctsConstants.erienmnrmGnbduNrSectorCarrier + nrSectorCarrier,
        'type': ctsConstants.region,
        'status': ctsConstants.operating,
        '$locatedAt': [
            ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + nrSectorCarrier
        ]
    };
}

function deleteGnbdu(nodeId) {
    return {
        '$type': ctsConstants.ctwGnbdu,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId
    }
}

function deleteGnbcuCp(nodeId) {
    return {
        '$type': ctsConstants.ctwGnbcucp,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbcuCp1,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbcuCp1
    }
}

function deleteNetFunctionCon(nodeId) {
    return {
        '$type': ctsConstants.ctwNetFunctionCon,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.netfunctioncon01,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.netfunctioncon01
    }
}

function deleteGndbuGeographicLocation(options = {}) {
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const cellId = options['cellId'] ? options['cellId'] : 1;
    const refId = options['refId'] ? options['refId'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + cellId;
    const name = options['name'] ? options['name'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + cellId;
    return {
        '$type': ctsConstants.ctgGeographicLocation,
        '$action': ctsConstants.deleteAction,
        '$refId': refId,
        'name': name
    }
}

function deleteGndbuGeographicSite(options = {}) {
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const cellId = options['cellId'] ? options['cellId'] : 1;
    const refId = options['refId'] ? options['refId'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + cellId;
    const name = options['name'] ? options['name'] :
        ctsConstants.irelandWestmeath + nodeId + ctsConstants.geoLocationNrSectorCarrier + cellId;
    return {
        '$type': ctsConstants.ctgGeographicSite,
        '$action': ctsConstants.deleteAction,
        '$refId': refId,
        'name': name
    }
}

function deleteGndbuNrCell(nodeId, carrier) {
    return {
        '$type': ctsConstants.ctwNrCell,
        '$action': ctsConstants.deleteAction,
        '$refId': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId + ctsConstants.slash + carrier,
        'name': ctsConstants.irelandWestmeath + nodeId + ctsConstants.gnbdu + nodeId + ctsConstants.slash + carrier
    }
}

function getCellIds(options) {
    const cellId = options['cellId'] ? options['cellId'] : 1;
    let cellIds = options['cellIds'] ? options['cellIds'] : [cellId];
    if (options['cellCount']) {
        cellIds = [];
        for (let i = 0; i < options['cellCount']; i++) {
            cellIds.push(cellId + i);
        }
    }
    return cellIds;
}

function createEnodeb(nodeId, lon, options = {}) {
    const geographicData = [];
    const lteCells = [];
    const cellIds = getCellIds(options);
    const enodebDist = options['enodebDist'] ? options['enodebDist'] : ctsConstants.enodebDist;
    let coords = coordinatesFromVectorOffset(90, enodebDist, ctsConstants.lat, lon);
    for (let i = 0; i < cellIds.length; i++) {
        const cellId = cellIds[i];
        const geographicLocation = reconcileGeographicLocation(coords[0], coords[1], {'nodeId': nodeId, 'cellId': cellId});
        const k = (cellId) % ctsConstants.fddEarfcnDls.length;
        const fddEarfcnDl = options['FDDearfcnDl'] ? options['FDDearfcnDl'] : ctsConstants.fddEarfcnDls[k][0];
        const fddEarfcnUl = options['FDDearfcnUl'] ? options['FDDearfcnUl'] : ctsConstants.fddEarfcnDls[k][1];
        if (i === 0) geographicData.push(reconcileEnodeb(nodeId));
        if (i % 2 === 1) coords = coordinatesFromVectorOffset(90, enodebDist, coords[0], coords[1]);
        geographicData.push(geographicLocation);
        geographicData.push(reconcileGeographicSite({'nodeId': nodeId, 'cellId': cellId}));
        lteCells.push(reconcileLteCell(nodeId, cellId, fddEarfcnDl, fddEarfcnUl));
    }
    const data = geographicData.concat(lteCells);
    return Object.values({data});
}

function cleanupEnodeb(nodeId, options = {}) {
    const items = [];
    const lteCells = [];
    const cellIds = getCellIds(options);
    for (let i = 0; i < cellIds.length; i++) {
        const cellId = cellIds[i];
        if (i === 0) items.push(deleteEnodeb(nodeId));
        items.push(deleteGeographicLocation(nodeId, cellId));
        items.push(deleteGeographicSite(nodeId, cellId));
        lteCells.push(deleteLteCell(nodeId, cellId));
    }
    const data = items.concat(lteCells);
    return Object.values({data});
}

function createEnodebCtsData(options = {}) {
    const items = [];
    let jsonData;
    let wirelessNetworkData = [];
    let lon = ctsConstants.lon;
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const nodeCount = options['nodeCount'] ? options['nodeCount'] : 1;
    const cellCount = options['cellCount'] ? options['cellCount'] : 1;
    const enodebOuterDist = options['enodebOuterDist'] ? options['enodebOuterDist'] : ctsConstants.enodebOuterDist;
    for (let i = 0; i < nodeCount; i++) {
        items.push(createEnodeb(ctsConstants.cnrTestENB.concat(nodeId + i), lon, options))
        if (i > 0) jsonData = jsonData.concat(items[i][0]);
        else jsonData = items[0][0];
        lon = coordinatesFromVectorOffset(90, enodebOuterDist * ~~(cellCount / 2), ctsConstants.lat, lon)[1];
    }
    wirelessNetworkData.push(ctsConstants.wirelessNetwork);
    wirelessNetworkData = wirelessNetworkData.concat(jsonData);
    return jsonHolder(wirelessNetworkData);
}

function cleanupEnodebCtsData(options = {}) {
    const items = [];
    let jsonData;
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const nodeCount = options['nodeCount'] ? options['nodeCount'] : 1;
    for (let i = 0; i < nodeCount; i++) {
        items.push(cleanupEnodeb(ctsConstants.cnrTestENB.concat(nodeId + i), options));
        if (i > 0) jsonData = jsonData.concat(items[i][0]);
        else jsonData = items[0][0];
    }
    return jsonHolder(jsonData);
}

function createGndbu(nodeId, currentLon, options = {}) {
    const data = [];
    const lteCells = [];
    let physicalCellIdentity = ctsConstants.physicalCellIdentity;
    const gnodebDist = options['gnodebDist'] ? options['gnodebDist'] : ctsConstants.gnodebDist;
    let coordinates = coordinatesFromVectorOffset(90, gnodebDist, ctsConstants.lat, currentLon);
    const cellIds = getCellIds(options);
    for (let i = 0; i < cellIds.length; i++) {
        const cellId = cellIds[i];
        const downlinkEARFCN = options['downlinkEARFCN'] ? options['downlinkEARFCN'] :
            ctsConstants.downlinkEARFCN[(cellId) % ctsConstants.downlinkEARFCN.length];
        if (i === 0) {
            data.push(reconcileGnbdu(nodeId));
            data.push(reconcileGnbcuCp(nodeId));
            data.push(reconcileNetFunctionCon(nodeId));
        }
        data.push(reconcileGnodebGeographicLocation(nodeId, cellId, coordinates[0], coordinates[1]));
        data.push(reconcileGnodebGeographicSite(nodeId, cellId));
        lteCells.push(reconcileNrCell(nodeId, cellId, downlinkEARFCN, physicalCellIdentity));
        coordinates = coordinatesFromVectorOffset(90, gnodebDist, coordinates[0], coordinates[1]);
        physicalCellIdentity++;
    }
    const jsonData = data.concat(lteCells);
    return Object.values({jsonData});
}

function createGndbuCtsData(options = {}) {
    const items = [];
    let jsonData;
    let wirelessNetworkData = [];
    let lon = ctsConstants.lon;
    const nodeCount = options['nodeCount'] ? options['nodeCount'] : 1;
    const cellCount = options['cellCount'] ? options['cellCount'] : 1;
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const gnbduDist = options['gnbduDist'] ? options['gnbduDist'] : ctsConstants.gnbduDist;
    for (let i = 0; i < nodeCount; i++) {
        let cnrTestGNB = ctsConstants.cnrTestGNB.concat(nodeId + i);
        items.push(createGndbu(cnrTestGNB, lon, options))
        if (i > 0) jsonData = jsonData.concat(items[i][0]);
        else jsonData = items[0][0];
        lon = coordinatesFromVectorOffset(90, gnbduDist * ~~(cellCount), ctsConstants.lat, lon)[1];
    }
    wirelessNetworkData.push(ctsConstants.wirelessNetwork);
    wirelessNetworkData = wirelessNetworkData.concat(jsonData);
    return jsonHolder(wirelessNetworkData);
}

function createLteCellGeoLocations(options = {}) {
    const items = [];
    if (options['id']) {
        const lat = options['lat'] ? options['lat'] : ctsConstants.lat;
        const lon = options['lon'] ? options['lon'] : ctsConstants.lon;
        const distance = options['distance'] ? options['distance'] : ctsConstants.distance;
        const enodeb = getEnodeb({'id': options['id'], 'fs': ctsConstants.fsLteCells});
        for (let i = 0; i < enodeb.lteCells.length; i++) {
            const cellName = enodeb.lteCells[i].value.name;
            const cellExternalId = enodeb.lteCells[i].value.externalId;
            let siteName = cellName.concat(ctsConstants.site).concat(i + 1);
            let siteExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratSite).concat(i + 1);
            let locationName = cellName.concat(ctsConstants.location).concat(i + 1);
            let locationExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratLocation).concat(i + 1);
            let coords = [];

            try {
                const locationData = getCellLocation(getLtecell, {'id': enodeb.lteCells[i].value.id})
                siteName = locationData['geographicSite'].name;
                siteExternalId = locationData['geographicSite'].externalId;
                locationName = locationData['geographicLocation'].name;
                locationExternalId = locationData['geographicLocation'].externalId;
                coords = locationData['coordinates'];
            } catch (error) {}

            if (options['lat'] || options['lon'] || !coords || !coords[0] || !coords[1]) {
                coords = coordinatesFromVectorOffset(90, Math.random() * distance, lat, lon);
            }

            items.push(reconcileGeographicLocation(coords[0], coords[1],
                {'refId': locationName, 'name': locationName, 'externalId': locationExternalId}));
            items.push(reconcileGeographicSite(
                {'refId': siteName, 'name': siteName, 'externalId': siteExternalId, 'locatedAt': locationName}));
            items.push(reconcileExistingLteCell(enodeb.lteCells[i].value, {'geographicSite': siteName}));
        }
    }
    return items;
}

function cleanupLteCellGeoLocations(options = {}) {
    const items = [];
    if (options['id']) {
        const enodeb = getEnodeb({'id': options['id'], 'fs': ctsConstants.fsLteCells});
        for (let i = 0; i < enodeb.lteCells.length; i++) {
            const cellName = enodeb.lteCells[i].value.name;
            const cellExternalId = enodeb.lteCells[i].value.externalId;
            let siteName = cellName.concat(ctsConstants.site).concat(i + 1);
            let siteExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratSite).concat(i + 1);
            let locationName = cellName.concat(ctsConstants.location).concat(i + 1);
            let locationExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratLocation).concat(i + 1);

            try {
                const locationData = getCellLocation(getLtecell, {'id': enodeb.lteCells[i].value.id})
                siteName = locationData['geographicSite'].name;
                siteExternalId = locationData['geographicSite'].externalId;
                locationName = locationData['geographicLocation'].name;
                locationExternalId = locationData['geographicLocation'].externalId;
            } catch (error) {}

            items.push(deleteGndbuGeographicLocation({'refId': locationName, 'name': locationName}));
            items.push(deleteGndbuGeographicSite({'refId': siteName, 'name': siteName}));
        }
    }
    return items;
}

function createNrCellGeoLocations(options = {}) {
    const items = [];
    if (options['id']) {
        const lat = options['lat'] ? options['lat'] : ctsConstants.lat;
        const lon = options['lon'] ? options['lon'] : ctsConstants.lon;
        const distance = options['distance'] ? options['distance'] : ctsConstants.distance;
        const gnbdu = getGnbdu({'id': options['id'], 'fs': ctsConstants.fsNrCells});
        for (let i = 0; i < gnbdu.nrCells.length; i++) {
            const cellName = gnbdu.nrCells[i].value.name;
            const cellExternalId = gnbdu.nrCells[i].value.externalId;
            let siteName = cellName.concat(ctsConstants.site).concat(i + 1);
            let siteExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratSite).concat(i + 1);
            let locationName = cellName.concat(ctsConstants.location).concat(i + 1);
            let locationExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratLocation).concat(i + 1);
            let coords = [];

            try {
                const locationData = getCellLocation(getNrcell, {'id': gnbdu.nrCells[i].value.id})
                siteName = locationData['geographicSite'].name;
                siteExternalId = locationData['geographicSite'].externalId;
                locationName = locationData['geographicLocation'].name;
                locationExternalId = locationData['geographicLocation'].externalId;
                coords = locationData['coordinates'];
            } catch (error) {}

            if (options['lat'] || options['lon'] || !coords || !coords[0] || !coords[1]) {
                coords = coordinatesFromVectorOffset(90, Math.random() * distance, lat, lon);
            }

            items.push(reconcileGeographicLocation(coords[0], coords[1],
                {'refId': locationName, 'name': locationName, 'externalId': locationExternalId}));
            items.push(reconcileGeographicSite(
                {'refId': siteName, 'name': siteName, 'externalId': siteExternalId, 'locatedAt': locationName}));
            items.push(reconcileExistingNrCell(gnbdu.nrCells[i].value, {'geographicSite': siteName}));
        }
    }
    return items;
}

function cleanupNrCellGeoLocations(options = {}) {
    const items = [];
    if (options['id']) {
        const gnbdu = getGnbdu({'id': options['id'], 'fs': ctsConstants.fsNrCells});
        for (let i = 0; i < gnbdu.nrCells.length; i++) {
            const cellName = gnbdu.nrCells[i].value.name;
            const cellExternalId = gnbdu.nrCells[i].value.externalId;
            let siteName = cellName.concat(ctsConstants.site).concat(i + 1);
            let siteExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratSite).concat(i + 1);
            let locationName = cellName.concat(ctsConstants.location).concat(i + 1);
            let locationExternalId = cellExternalId.concat(ctsConstants.ericssonEnmLratLocation).concat(i + 1);

            try {
                const locationData = getCellLocation(getNrcell, {'id': gnbdu.nrCells[i].value.id})
                siteName = locationData['geographicSite'].name;
                siteExternalId = locationData['geographicSite'].externalId;
                locationName = locationData['geographicLocation'].name;
                locationExternalId = locationData['geographicLocation'].externalId;
            } catch (error) {}

            items.push(deleteGndbuGeographicLocation({'refId': locationName, 'name': locationName}));
            items.push(deleteGndbuGeographicSite({'refId': siteName, 'name': siteName}));
        }
    }
    return items;
}

function cleanupGndbu(nodeId, options = {}) {
    const gndbus = [];
    const lteCells = [];
    const cellIds = getCellIds(options);
    for (let i = 0; i < cellIds.length; i++) {
        const cellId = cellIds[i];
        if (i === 0) {
            gndbus.push(deleteGnbdu(nodeId));
            gndbus.push(deleteGnbcuCp(nodeId));
            gndbus.push(deleteNetFunctionCon(nodeId));
        }
        gndbus.push(deleteGndbuGeographicLocation({'nodeId': nodeId, 'cellId': cellId}));
        gndbus.push(deleteGndbuGeographicSite({'nodeId': nodeId, 'cellId': cellId}));
        lteCells.push(deleteGndbuNrCell(nodeId, cellId));
    }
    const data = gndbus.concat(lteCells);
    return Object.values({data});
}

function cleanupGndbuCtsData(options = {}) {
    const items = [];
    let jsonData;
    const nodeId = options['nodeId'] ? options['nodeId'] : 1;
    const nodeCount = options['nodeCount'] ? options['nodeCount'] : 1;
    for (let i = 0; i < nodeCount; i++) {
        let cnrTestGNB = ctsConstants.cnrTestGNB.concat(nodeId + i);
        items.push(cleanupGndbu(cnrTestGNB, options));
        if (i > 0) jsonData = jsonData.concat(items[i][0]);
        else jsonData = items[0][0];
    }
    return jsonHolder(jsonData);
}

function getSessionParams() {
    const encodedCredentials = encoding.b64encode('sysadm:');
    return ctsConstants.sessionIdAccess ?
        {
            headers: {
                'Cookie': 'JSESSIONID='.concat(getSessionId()),
                'Content-Type': 'application/json'
            }
        } :
        {
            headers: {
                'Content-Type': 'application/json',
                'GS-Database-Name': 'eai_install',
                'GS-Database-Host-Name': 'localhost',
                'Authorization': `Basic ${encodedCredentials}`
            }
        };
}

function doDataSync(request, options = {}) {
    return httpPost(ctsConstants.ctsUrl, ctsConstants.dataSyncUri, request, getSessionParams(), options);
}

function handleDataSyncSummary(data) {
    let items = [];
    let jsonData = '';
    data.root_group.groups.forEach(function(group) {
        group.checks.forEach(function(check, index) {
            if (check.name.includes('jsonData')) {
                const jsonString = JSON.stringify(check.name, null).replace('\"jsonData ', '').replace(/\\/g, '');
                if (jsonString.includes('{"jsonData":""}')) {
                    jsonData = jsonString.replace('\"}}}\"', '\"}}}').replace('{\"jsonData\":\"\"}', JSON.stringify(items));
                } else {
                    items = items.concat(JSON.parse(jsonString.replace('\"}]\"', '\"}]').replace('\"]}]\"', '\"]}]')));
                }
                check.name = 'store json data';
                check.path = '';
                return;
            }
        });
    });

    const options = {
        'stdout': runSummary(data)
    };
    if (__ENV.OUT) options[__ENV.OUT] = JSON.stringify(JSON.parse(jsonData), null, 4);
    return options;
}

function httpGetWithCheck(uri, options = {}) {
    const response = httpGet(ctsConstants.ctsUrl, uri, getSessionParams(), options);
    checkResponse(response, options);
    return response;
}

function getManagedObjectByUri(uri, options = {}) {
    const response = httpGetWithCheck(uri, options);
    const body = parseJson(response.body);
    return response.status === 200 && body ? body : {};
}

function getManagedObject(uri, options = {}) {
    let filter = '';
    if (options['fdn']) {
        filter = ctsConstants.name.concat(options['fdn']);
        if (options['cellId']) {
            filter = ctsConstants.slash.concat(options['cellId']);
        }
        else if (options['fsNrSectorCarriers']) {
            filter = filter.concat(options['fsNrSectorCarriers']);
        }
        else if (options['fsWirelessNetworks']) {
            filter = filter.concat(options['fsWirelessNetworks']);
        }
        else if (options['fsLteCells']) {
            filter = filter.concat(options['fsLteCells']);
        }
        else if (options['fsNrCells']) {
            filter = filter.concat(options['fsNrCells']);
        }
    }
    else if (options['coordinate'] && options['distance']) {
        filter = ctsConstants.geoQuery.concat(encodeURIComponent(createGeoQueryBody(options['coordinate'][0], options['coordinate'][1], options['distance'])));
    }
    else if (options['ltecell']) {
        filter = ctsConstants.slash.concat((options['ltecell'].concat(ctsConstants.qs)).concat(ctsConstants.fsGeographicSite));
    }
    else if (options['geographicSiteId']) {
        filter = ctsConstants.slash.concat((options['geographicSiteId'].concat(ctsConstants.qs)).concat(ctsConstants.fsLocatedAt));
    }
    else if (options['nrcell']) {
        filter = ctsConstants.slash.concat((options['nrcell'].concat(ctsConstants.qs)).concat(ctsConstants.fsGeographicSite));
    }
    else if (options['fs']) {
        filter = filter.length === 0 ? options['fs'] : filter.concat(ctsConstants.at).concat(options['fs']);
    }
    const result = getManagedObjectByUri(uri
        .concat(options['id'] ? ctsConstants.slash.concat(options['id']) : '')
        .concat(filter.length === 0 ? '' :
            (options['nrcell'] || options['ltecell'] || options['geographicSiteId'] || options['cellId']) ? filter : ctsConstants.qs.concat(filter)),
        options);

    return (result[0] && !options['list'] && !options['celltype']) ? result[0] : result;
}

function getManagedObjectByInternalId(uri, nodeType, options = {}) {
    const cnrTest = nodeType === ctsConstants.enodeb ? ctsConstants.cnrTestENB : ctsConstants.cnrTestGNB;
    const response = getManagedObjectByUri(
        uri
            .concat(ctsConstants.qs)
            .concat(ctsConstants.name)
            .concat(ctsConstants.irelandWestmeath).concat(cnrTest).concat(options['nodeId'])
            .concat(nodeType).concat(cnrTest).concat(options['nodeId'])
            .concat(options['cellId'] ? ctsConstants.slash.concat(options['cellId']) : ''),
        options);

    if (response && response[0]) {
        if (options['fs']) {
            options['id'] = response[0].id;
            return getManagedObject(uri, options);
        } else {
            return response[0];
        }
    } else {
        return {};
    }
}

function getEnodeb(options = {}) {
    let response;
    if (options['id'] || options['fdn']) {
        response = getManagedObject(ctsConstants.enodebUri, options);
    } else if (options['nodeId']) {
        response = getManagedObjectByInternalId(ctsConstants.enodebUri, ctsConstants.enodeb, options);
    }
    return response ? response : {};
}

function getLtecell(options = {}) {
    let response;
    if (options['id'] || options['fdn']) {
        response = getManagedObject(ctsConstants.ltecellUri, options);
    } else if (options['nodeId'] && options['cellId']) {
        response = getManagedObjectByInternalId(ctsConstants.ltecellUri, ctsConstants.enodeb, options);
    }
    return response ? response : {};
}

function getAllCells(options = {}) {
    let response;
    if (options['type'] === 'ltecell') {
        let fsLteCells = ctsConstants.at.concat((ctsConstants.fsLteCells));
        options.fsLteCells = options.fsLteCells || fsLteCells;
        response = getManagedObject(ctsConstants.enodebUri, options);
    } else if (options['type'] === 'nrcell') {
        let fsNrCells = ctsConstants.at.concat((ctsConstants.fsNrCells));
        options.fsNrCells = options.fsNrCells || fsNrCells;
        response = getManagedObject(ctsConstants.gnbduUri, options);
    }
    return response ? response : {};
}

function getGnbdu(options = {}) {
    let response;
    if (options['id'] || options['fdn']) {
        response = getManagedObject(ctsConstants.gnbduUri, options);
    } else if (options['nodeId']) {
        response = getManagedObjectByInternalId(ctsConstants.gnbduUri, ctsConstants.gnbdu, options);
    }
    return response ? response : {};
}

function getNrcell(options = {}) {
    let response;
    if (options['id'] || options['fdn']) {
        response = getManagedObject(ctsConstants.nrcellUri, options);
    } else if (options['nodeId'] && options['cellId']) {
        response = getManagedObjectByInternalId(ctsConstants.nrcellUri, ctsConstants.gnbdu, options);
    }
    return response ? response : {};
}

function getCellGeographicSite(options = {}) {
    let response;
    if (options['ltecell']) {
        response = getManagedObject(ctsConstants.ltecellUri, options);
    }
    else if (options['nrcell']) {
        response = getManagedObject(ctsConstants.nrcellUri, options);
    }
    return response ? response : {};
}

function getGeographicsite(options = {}) {
    let response;
    if (options['geographicSiteId']) {
        response = getManagedObject(ctsConstants.geographicSiteUri, options);
    }
    return response ? response : {};
}

function getGeoQuery(options = {}) {
    let response;
    if (options['celltype'] === 'ltecell') {
        response = getManagedObject(ctsConstants.ltecellUri, options);
    }
    else if (options['celltype'] === 'nrcell') {
        response = getManagedObject(ctsConstants.nrcellUri, options);
    }
    return response ? response : {};
}

function getNrSectorCarriers(options = {}) {
    let response;
    let fsNrSectorCarriers = ctsConstants.at.concat((ctsConstants.fsNrSectorCarriers).substring(1));
    options.fsNrSectorCarriers = options.fsNrSectorCarriers || fsNrSectorCarriers;
    response = getManagedObject(ctsConstants.gnbduUri, options);
    return response ? response : {};
}

function getWirelessNetworks(options = {}) {
    let response;
    let fsWirelessNetworks = ctsConstants.at.concat((ctsConstants.fsWirelessNetworks).substring(1));
    options.fsWirelessNetworks = options.fsWirelessNetworks || fsWirelessNetworks;
    response = getManagedObject(ctsConstants.gnbduUri, options);
    return response ? response : {};
}

function createGeoQueryBody(x, y, distance) {
    return (
       "{'center': {'type': 'Point','coordinates': [" + x + "," + y + "]}, 'distance': " + distance +"}"
    );
}

function getEnodebCount(options = {}) {
    const response = httpGetWithCheck(ctsConstants.enodebUri.concat(ctsConstants.taskCount), options);
    return response.status === 200 && response.body ? response.body : 0;
}

function getLtecellCount(options = {}) {
    const response = httpGetWithCheck(ctsConstants.ltecellUri.concat(ctsConstants.taskCount), options);
    return response.status === 200 && response.body ? response.body : 0;
}

function getGnbduCount(options = {}) {
    const response = httpGetWithCheck(ctsConstants.gnbduUri.concat(ctsConstants.taskCount), options);
    return response.status === 200 && response.body ? response.body : 0;
}

function getNrcellCount(options = {}) {
    const response = httpGetWithCheck(ctsConstants.nrcellUri.concat(ctsConstants.taskCount), options);
    return response.status === 200 && response.body ? response.body : 0;
}

function getNrSectorCarrier(options = {}) {
    return getManagedObject(ctsConstants.nrSectorCarrierUri, options);
}

function getGeographicSite(options = {}) {
    return getManagedObject(ctsConstants.geographicSiteUri, options);
}

function getGeographicLocation(options = {}) {
    return getManagedObject(ctsConstants.geographicLocationUri, options);
}

function getCellLocation(getCellFunc, options = {}) {
    const data = {};
    try {
        let getCellOptions = {'fs': ctsConstants.fsGeographicSite};
        if (options['id']) getCellOptions['id'] = options['id'];
        if (options['nodeId']) getCellOptions['nodeId'] = options['nodeId'];
        if (options['cellId']) getCellOptions['cellId'] = options['cellId'];
        data['cell'] = getCellFunc(getCellOptions);
        data['geographicSite'] = getGeographicSite({'id': data['cell'].geographicSite[0].value.id, 'fs': ctsConstants.fsLocatedAt});
        data['geographicLocation'] = getGeographicLocation({'id': data['geographicSite'].locatedAt[0].value.id});
        data['coordinates'] = geographicLocation.geospatialData.coordinates;
    } catch (error) {}
    return data;
}

function checkIdExists(getIdFunc, options) {
    let existsId = false;
    if (options['id']) {
        check(getIdFunc(options), {
            [`ID ${options['id']} exists in CTS`]: (r) => r && r.id && (existsId = true)
        });
    } else if (options['nodeId'] || options['cellId']) {
        const checkedId = options['cellId'] ? options['cellId'] : options['nodeId'];
        check(getIdFunc(options), {
            [`ID ${checkedId} exists in CTS`]: (r) => r && r.id && (existsId = true)
        });
    } else {
        check(false, {
            [`ID exists in CTS: invalid option`]: (r) => false
        });
    }
    return existsId;
}

function checkIdNotExists(getIdFunc, options) {
    const checkedId = options['cellId'] ? options['cellId'] : options['nodeId'];
    check(getIdFunc(options), {
        [`ID ${checkedId} does not exist in CTS`]: (r) => !r || !r[0]
    });
}

Number.prototype.toRad = function () {
    return this * Math.PI / 180;
}

Number.prototype.toDeg = function () {
    return this * 180 / Math.PI;
}

function coordinatesFromVectorOffset(bearing, offset, lat, lon) {
    //Destination point along great-circle given distance and bearing from start point
    offset = offset / (ctsConstants.rGlobe * 1000);
    bearing = bearing.toRad();

    let lat1 = lat.toRad();
    let lon1 = lon.toRad();

    let lat2 = Math.asin(Math.sin(lat1) * Math.cos(offset) +
        Math.cos(lat1) * Math.sin(offset) * Math.cos(bearing));

    let lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(offset) *
        Math.cos(lat1),
        Math.cos(offset) - Math.sin(lat1) *
        Math.sin(lat2));

    if (isNaN(lat2) || isNaN(lon2)) return null;

    return Array(lat2.toDeg(), lon2.toDeg());
}

function distanceFromCoordinates(lat1, lat2, lon1, lon2) {
    //Haversine formula
    lon1 = lon1.toRad();
    lon2 = lon2.toRad();
    lat1 = lat1.toRad();
    lat2 = lat2.toRad();

    let dlon = lon2 - lon1;
    let dlat = lat2 - lat1;
    let a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

    let c = 2 * Math.asin(Math.sqrt(a));

    return (c * ctsConstants.rGlobe) * 1000;
}

module.exports = {
    createEnodebCtsData,
    cleanupEnodebCtsData,
    createGndbuCtsData,
    cleanupGndbuCtsData,
    createLteCellGeoLocations,
    cleanupLteCellGeoLocations,
    createNrCellGeoLocations,
    cleanupNrCellGeoLocations,
    jsonHolder,
    doDataSync,
    handleDataSyncSummary,
    getEnodeb,
    getGnbdu,
    getNrcell,
    getLtecell,
    getEnodebCount,
    getLtecellCount,
    getGnbduCount,
    getNrcellCount,
    getNrSectorCarrier,
    getGeographicSite,
    getGeographicLocation,
    getCellLocation,
    checkIdExists,
    checkIdNotExists,
    coordinatesFromVectorOffset,
    distanceFromCoordinates,
    getCellGeographicSite,
    getGeographicsite,
    getGeoQuery,
    getNrSectorCarriers,
    getWirelessNetworks,
    getAllCells
}