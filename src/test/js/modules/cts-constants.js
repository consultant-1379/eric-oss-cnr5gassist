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

import * as constants from './constants.js';

export const lat = 53.43;
export const lon = -7.9;
export const distance = 200;
export const enodebDist = 556;
export const gnodebDist = 390;
export const enodebOuterDist = 556;
export const gnbduDist = 390;
export const physicalCellIdentity = 138;
export const rGlobe = 6371;
export const gnbduId = 419396;
export const plmnId = 27211;
export const trackingAreaCode = 10496;
export const externalId = 'D790D3A202893DF5C171BADBE3FB65E3';

export const cnrTestENB = 'CNRTestENB';
export const cnrTestGNB = 'CNRTestGNB';
export const cnrTestNetwork = 'CNRTestNetwork';
export const ltecell = 'ltecell';
export const nrcell = 'nrcell';

export const slash = '/';
export const dash = '-';
export const qs = '?';
export const at = '&';
export const name = 'name=';
export const fsLteCells = 'fs.lteCells';
export const fsNrCells = 'fs.nrCells';
export const fsGeographicSite = 'fs.geographicSite';
export const fsLocatedAt = 'fs.locatedAt';
export const fsNrSectorCarriers = '?fs.nrSectorCarriers';
export const fsWirelessNetworks = '?fs.wirelessNetworks';
export const geoQuery = '?geographicSite.locatedAt.geospatialData.geoDistanceWithin=';

export const ctwEnodeb = 'ctw/eNodeB';
export const ctwLteCell = 'ctw/lteCell';
export const ctwGnbdu = 'ctw/gnbdu';
export const ctwGnbcucp = 'ctw/gnbcucp';
export const ctwNrCell = 'ctw/nrCell';
export const ctwNetFunctionCon = 'ctw/netFunctionCon';
export const ctgGeographicLocation = 'ctg/geographicLocation';
export const ctgGeographicSite = 'ctg/geographicSite';
export const oslAdvProcess = 'osl-adv/datasyncservice/process';
export const gsJsonHolder = 'gs/jsonHolder';

export const managedElement = 'ManagedElement=';
export const sectorCarrier = 'SectorCarrier=';
export const nrSectorCarrier = 'NRSectorCarrier=';
export const enodeBFunction = 'ENodeBFunction=';
export const gnbduFunction = 'GNBDUFunction=';
export const gnbduFunction1 = 'GNBDUFunction=1';
export const gnbcuCpFunction1 = 'GNBCUCPFunction=1';
export const ltecellDU = 'ltecellDU=';
export const nrCellDU = 'NRCellDU=';
export const enodeb = '/enodeb=';
export const gnbdu = '/gnbdu=';
export const gnbcuCp1 = '/gnbcucp=1';
export const netfunctioncon01 = '/netfunctioncon01';
export const irelandWestmeath = 'Ireland/Westmeath/';
export const location = "/Location";
export const ericssonEnmLratLocation = "/ericsson-enm-lrat:Location=";
export const ericssonEnmLratSite = "/ericsson-enm-lrat:Site=";
export const site = "/Site";
export const geoSiteSectorCarrier = '/geoSite:'.concat(sectorCarrier);
export const geoLocationSectorCarrier = '/geoLocation:'.concat(sectorCarrier);
export const geoLocationNrSectorCarrier = '/geoLocation:'.concat(nrSectorCarrier);
export const erienmnrmGnbduSectorCarrier = '/erienmnrmgnbdu:'.concat(sectorCarrier);
export const geoSiteNrSectorCarrier = '/geoSite:'.concat(nrSectorCarrier);
export const erienmnrmGnbduNrSectorCarrier = '/erienmnrmgnbdu:'.concat(nrSectorCarrier);
export const erienmnrmcomtopManagedElement = '/erienmnrmcomtop:'.concat(managedElement);
export const ericssonEnmManagedElement = '/ericsson-enm-ComTop:'.concat(managedElement);
export const ew345ManagedElement = 'ew345/_3gpp-common-managed-element:'.concat(managedElement);
export const u3gppENodeBFunction = '/_3gpp-nr-nrm-enodebfunction:'.concat(enodeBFunction);
export const erienmnrmGnbduFunction = '/erienmnrmgnbdu:'.concat(gnbduFunction);
export const erienmnrmGnbduFunction1 = '/erienmnrmgnbdu:'.concat(gnbduFunction1);
export const erienmnrmGnbcuCpFunction1 = '/erienmnrmgnbcucp:'.concat(gnbcuCpFunction1);
export const u3gppLtecellDU = '/_3gpp-LTE-LTEm-ltecelldu:'.concat(ltecellDU);
export const ericssonEnmNrCellDU = '/ericsson-enm-GNBDU:'.concat(nrCellDU);

export const reconcile = 'reconcile';
export const lookUp = 'lookUp';
export const operating = 'operating';
export const deleteAction = 'delete';
export const region = 'Region';
export const f1cGnbcucpGnbdu = 'F1C_gnbcucp_gnbdu';
export const geospatialCoords = 'GeospatialCoords';
export const point = 'Point';
export const enabled = 'ENABLED';
export const unlocked = 'UNLOCKED'

export const fddEarfcnDls = [
    [5230, 23230], [900, 18900], [2100, 23230], [5230, 18900], [5230, 23230], [2100, 18900], [900, 23230], [5230, 20100]
];

export const downlinkEARFCN = [26500, 26500, 5230, 5230];

export const wirelessNetwork = {
    '$type': 'ctw/wirelessNetwork',
    '$action': 'reconcile',
    '$refId': 'CNRTestNetwork',
    'name': 'CNRTestNetwork',
    'mcc': 128,
    'mnc': 49,
    'country': 'Ireland',
    'operatorName': 'CNR Test Operator',
    'status': 'operating'
};

export const ctsIdMapping = __ENV.STAGING_LEVEL === 'PRODUCT' ? true :
    __ENV.CTS_ID_MAPPING ? true : false;

export const sessionIdAccess = __ENV.STAGING_LEVEL === 'PRODUCT' ? true :
    __ENV.SESSION_ID_ACCESS === 'true' ? true : false;

export const ctsUrl = __ENV.STAGING_LEVEL === 'PRODUCT' ? constants.ingressUrl :
    __ENV.CTS_URL ? __ENV.CTS_URL : '';

export const ctsRestUri = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/oss-core-ws/rest' :
    __ENV.CTS_REST_URI ? __ENV.CTS_REST_URI : '';

export const dataSyncUri = ctsRestUri.concat('/osl-adv/datasync/process');
export const enodebUri = ctsRestUri.concat('/ctw/enodeb');
export const ltecellUri = ctsRestUri.concat('/ctw/ltecell');
export const gnbduUri = ctsRestUri.concat('/ctw/gnbdu');
export const nrcellUri = ctsRestUri.concat('/ctw/nrcell');
export const nrSectorCarrierUri = ctsRestUri.concat('/ctw/nrsectorcarrier');
export const geographicSiteUri = ctsRestUri.concat('/ctg/geographicsite');
export const geographicLocationUri = ctsRestUri.concat('/ctg/geographiclocation');
export const taskCount = 'Task/count';