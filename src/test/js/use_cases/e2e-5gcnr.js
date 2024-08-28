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
import * as common from '../modules/common.js';
import * as ctsConstants from '../modules/cts-constants.js';
import * as nrcCommon from '../modules/nrc-common.js';
import * as ctsCommon from '../modules/cts-common.js';
import * as ncmpCommon from '../modules/ncmp-common.js'
import { e2eObjects } from '../modules/e2e-constants.js';

function getId(getIdFunc, options) {
    if (ctsConstants.ctsIdMapping) {
        return getIdFunc(options) ? getIdFunc(options).id : options['fdn'];
    } else {
        return options['cellId'] ? options['cellId'] : options['fdn'];
    }
}

function healthCheck() {
    group('5G CNR health check included in the system status check', function() {
        nrcCommon.health200({'valid': true, 'includes': 'status:UP'});
    });
}

function fetchNrcells(fdn, type) {
    const nrCells = [];
    const nrcellsOfGnbdu = ctsCommon.getAllCells({'fdn': fdn, 'type': type, 'valid': true});
    if (nrcellsOfGnbdu.nrCells) {
        for (var key in Object.keys(nrcellsOfGnbdu.nrCells)) {
            nrCells.push(nrcellsOfGnbdu.nrCells[key].value.id);
        }
    }
    return nrCells;
}

function isValidEnodeb(enodeb) {
    check(enodeb, {
        'Valid enodeb object': (r) => r && r.id
    });
    return enodeb && enodeb.id
}

function isValidLTEcell(ltecell) {
    check(ltecell, {
        'Valid lteCell object': (r) => r && r.id && r.FDDearfcnDl
    });
    return ltecell && ltecell.id
}

function isValidNRSectorCarrier(nrsectorcarrier) {
    check(nrsectorcarrier, {
        'Valid nrSectorCarrier object': (r) => r && r.id && r.arfcnDL
    });
    return nrsectorcarrier && nrsectorcarrier.id
}

function singleEnodebAndGnbduWithNoFreq() {
    group('Single enodeb with single gnbdu found with no freq filter', function() {
        if (isValidEnodeb(e2eObjects.enodeb1)) {
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': 150}).body;
            const monitoringIdResponse = nrcCommon.monitoringId200Succeeded(requestId);
            nrcCommon.checkMonitoringIdResponse(monitoringIdResponse, {
                'allNrcNeighbors[?].eNodeBId': e2eObjects.enodeb1.id,
                'allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId': e2eObjects.gnbdu1.id,
                'allNrcNeighbors[?].gNodeBDUs[?].nrCellIds': fetchNrcells(e2eObjects.gnbdu1.name, ctsConstants.nrcell)
            });
        }
    });
}

function singleEnodebAndGnbduWithMultipleFreq() {
    group('Single enodeb with single gnbdu found with freq filter (multiple frequencies)', function() {
        if (isValidEnodeb(e2eObjects.enodeb1) && isValidLTEcell(e2eObjects.ltecell1) && isValidNRSectorCarrier(e2eObjects.nrSectorCarrier1)) {
            var freqPairs = {};
            freqPairs[e2eObjects.ltecell1.FDDearfcnDl] = [e2eObjects.nrSectorCarrier1.arfcnDL, 2076665];
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': 150, 'freqPairs': freqPairs}).body;
            const monitoringIdResponse = nrcCommon.monitoringId200Succeeded(requestId);
            nrcCommon.checkMonitoringIdResponse(monitoringIdResponse, {
                'allNrcNeighbors[?].eNodeBId': e2eObjects.enodeb1.id,
                'allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId': e2eObjects.gnbdu1.id,
                'allNrcNeighbors[?].gNodeBDUs[?].nrCellIds': fetchNrcells(e2eObjects.gnbdu1.name, ctsConstants.nrcell)
            });
        }
    });
}

function externalGNodeBFunAndTermPointToGNB() {
    group('Single enodeb with single gnbdu found with no freq filter with ExternalGNodeBFunction and TermPointToGNB created - X2 link', function() {
        if (isValidEnodeb(e2eObjects.enodeb1)) {
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': 120}).body;
            nrcCommon.monitoringId200Succeeded(requestId);
            ncmpCommon.getExternalGUtranCell({
                'mo': e2eObjects.enodeb1,
                'includes': ['erienmnrmlrat:ExternalGUtranCell', 'absTimeOffset:0']});
            ncmpCommon.getTermPointToGNB({
                'mo': e2eObjects.enodeb1,
                'includes': ['erienmnrmlrat:TermPointToGNB', 'termPointToGNBId:1']});
        }
    });
}

function multipleEnodebAndGnbduWithFreq() {
    group('Multiple enodebs with multiple gnbdus found with freq filter', function() {
        if (isValidEnodeb(e2eObjects.enodeb1) && isValidEnodeb(e2eObjects.enodeb2) &&
            isValidLTEcell(e2eObjects.ltecell1) && isValidNRSectorCarrier(e2eObjects.nrSectorCarrier1) &&
            isValidLTEcell(e2eObjects.ltecell2) && isValidNRSectorCarrier(e2eObjects.nrSectorCarrier3)) {
            var freqPairs = {};
            freqPairs[e2eObjects.ltecell1.FDDearfcnDl] = [e2eObjects.nrSectorCarrier1.arfcnDL, e2eObjects.nrSectorCarrier3.arfcnDL];
            freqPairs[e2eObjects.ltecell2.FDDearfcnDl] = [e2eObjects.nrSectorCarrier1.arfcnDL, e2eObjects.nrSectorCarrier3.arfcnDL];
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id, e2eObjects.enodeb2.id], 'distance': 500, 'freqPairs': freqPairs}).body;
            const nrCells1 = fetchNrcells(e2eObjects.gnbdu1.name, ctsConstants.nrcell);
            const nrCells3 = fetchNrcells(e2eObjects.gnbdu3.name, ctsConstants.nrcell);
            nrcCommon.monitoringId200Succeeded(requestId, {
                'includes': JSON.stringify([{
                        'eNodeBId': e2eObjects.enodeb1.id,
                        'gNodeBDUs': [{'gNodeBDUId': e2eObjects.gnbdu1.id, 'nrCellIds': nrCells1}]
                    }, {
                        'eNodeBId': e2eObjects.enodeb2.id,
                        'gNodeBDUs': [{'gNodeBDUId': e2eObjects.gnbdu3.id, 'nrCellIds': nrCells3}]
                    }], null)
            });
        }
    });
}

function multiplePlmnIds() {
    group('Single enodeb with single gnbdu found with no freq filter with multiple PLMN IDs', function() {
        // See the "Configure ENM" confluence page where the NR01gNodeBRadio00020 (e2eObjects.gnbdu2) is Multi PLMN: Yes
        if (isValidEnodeb(e2eObjects.enodeb2)) {
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb2.id], 'distance': 600}).body;
            const monitoringIdResponse = nrcCommon.monitoringId200Succeeded(requestId, {'valid': true, 'timeout': 120});
            nrcCommon.checkMonitoringIdResponse(monitoringIdResponse, {
                'allNrcNeighbors[?].eNodeBId': e2eObjects.enodeb2.id
            });
            nrcCommon.checkMonitoringIdResponse(monitoringIdResponse, {
                'allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId': e2eObjects.gnbdu2.id,
                'allNrcNeighbors[?].gNodeBDUs[?].nrCellIds': fetchNrcells(e2eObjects.gnbdu2.name, ctsConstants.nrcell)
            });
            nrcCommon.checkMonitoringIdResponse(monitoringIdResponse, {
                'allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId': e2eObjects.gnbdu3.id,
                'allNrcNeighbors[?].gNodeBDUs[?].nrCellIds': fetchNrcells(e2eObjects.gnbdu3.name, ctsConstants.nrcell)
            });
            nrcCommon.checkMonitoringIdResponse(monitoringIdResponse, {
                'allNrcNeighbors[?].gNodeBDUs[?].gNodeBDUId': e2eObjects.gnbdu4.id,
                'allNrcNeighbors[?].gNodeBDUs[?].nrCellIds': fetchNrcells(e2eObjects.gnbdu4.name, ctsConstants.nrcell)
            });
        }
    });
}

function externalCellCreated() {
    group('Single enodeb with single gnbdu found with no freq filter with external cell created already', function() {
        if (isValidEnodeb(e2eObjects.enodeb1)) {
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': 150}).body;

            if (__ENV.E2E_CLEANUP === 'YES') {
                // The CreateExternalGNodeBFunction is listed in the enmUpdates array only if it hasn't existed before
                nrcCommon.monitoringId200Succeeded(
                    requestId,
                    {'includes': 'CreateExternalGNodeBFunction'});
            } else {
                nrcCommon.monitoringId200Succeeded(requestId);
            }

            ncmpCommon.getExternalGUtranCell({
                'mo': e2eObjects.enodeb1,
                'includes': ['erienmnrmlrat:ExternalGUtranCell', 'absTimeOffset:0']});
        }
    });
}

function singleEnodebWithInvalidFdn() {
    group('Single enodeb with invalid FDN', function() {
        const invalidFdn = "Europe/Test/NR01gNodeGRadio222/NR01gNodeBRadio222/1"
        const enbId = getId(ctsCommon.getEnodeb, {'fdn': invalidFdn});
        const requestId = nrcCommon.startNrc200({'eNodeBIds': [enbId], 'distance': 400}).body;
        nrcCommon.monitoringId200Failed(requestId);
    });
}

function monitoringEndpointReachability() {
    group('Monitoring endpoint reachability', function() {
        if (isValidEnodeb(e2eObjects.enodeb1)) {
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': 150}).body;
            const monitoringResponse = nrcCommon.monitoringId200Succeeded(requestId);
            check(monitoringResponse, {
                'Monitoring status is 200': (r) => r.status === 200
            });
        }
    });
}

function actuatorEndpointReachability() {
    group('Actuator endpoint reachability', function() {
        const healthResponse = nrcCommon.health200();
        check(healthResponse, {
            'Health status is UP': (r) => r.json().status === 'UP',
        });

        const prometheusResponse = nrcCommon.prometheus200();
        check(JSON.stringify(prometheusResponse), {
            'Prometheus body is not empty': (r) => r.length > 0,
        });
    });
}

function metricsValidationsPerformance() {
    group('Performance metrics validations after the execution of the tests, checking the metrics presence and change', function() {
        let nrcMetric = common.parseJson(nrcCommon.metrics200(constants.nrcRequestCountMetric).body);
        check(nrcMetric, {
           'Nrc metric exists': (r) => r && r.measurements && r.measurements[0]
        });

        let monitoringMetric = common.parseJson(nrcCommon.metrics200(constants.monitoringRequestCountMetric).body);
        check(monitoringMetric, {
            'MonitoringCount metric exists': (r) => r && r.measurements && r.measurements[0]
        });

        if (nrcMetric && nrcMetric.measurements && nrcMetric.measurements[0] &&
            monitoringMetric && monitoringMetric.measurements && monitoringMetric.measurements[0]) {
            let nrcCount = nrcMetric.measurements[0].value;
            const requestId = nrcCommon.startNrc200({'eNodeBIds': [e2eObjects.enodeb1.id], 'distance': 150}).body;
            nrcMetric = common.parseJson(nrcCommon.metrics200(constants.nrcRequestCountMetric).body);
            check(nrcMetric, {
               'Nrc count requests metric is incremented by 1': (r) =>
                    r.measurements && r.measurements[0] && r.measurements[0].value === ++(nrcCount),
            });

            monitoringMetric = common.parseJson(nrcCommon.metrics200(constants.monitoringRequestCountMetric).body);
            let monitoringCount = monitoringMetric.measurements[0].value;
            nrcCommon.monitoringId200(requestId);
            monitoringMetric = common.parseJson(nrcCommon.metrics200(constants.monitoringRequestCountMetric).body);
            check(monitoringMetric, {
                'MonitoringCount count requests metric is incremented by 1': (r) =>
                    r.measurements && r.measurements[0] && r.measurements[0].value === ++(monitoringCount),
            });
        }
    });
}

module.exports = {
    healthCheck,
    singleEnodebAndGnbduWithNoFreq,
    singleEnodebAndGnbduWithMultipleFreq,
    externalGNodeBFunAndTermPointToGNB,
    multipleEnodebAndGnbduWithFreq,
    multiplePlmnIds,
    singleEnodebWithInvalidFdn,
    monitoringEndpointReachability,
    actuatorEndpointReachability,
    metricsValidationsPerformance,
    externalCellCreated
}