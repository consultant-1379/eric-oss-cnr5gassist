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

import { group } from 'k6';
import { runSummary } from './modules/common.js';
import { htmlReport } from './resources/eric-k6-static-report-plugin.js';
import { defaultE2EOptions } from './modules/constants.js';

import { initE2eObjects } from './modules/e2e-constants.js';
import * as e2eGateway from './use_cases/e2e-gateway.js';
import * as e2eCts from './use_cases/e2e-cts.js';
import * as e2eNcmp from './use_cases/e2e-ncmp.js';
import * as e2e5gcnr from './use_cases/e2e-5gcnr.js';
import { happyPathTest, extraFreq } from './use_cases/e2e.js';

import { healthCheck } from './use_cases/health.js';
import { startNrc, monitoring, monitoringById } from './use_cases/tests.js';
import { testSetup, testTaskQueueIsFull, testTeardown } from './use_cases/queuefull.js';
import { monitoringInvalidId, monitoringNotFoundId } from './use_cases/monitoring.js';
import { createEnodebCtsDataTest, createGndbuCtsDataTest, cleanupEnodebCtsDataTest, cleanupGndbuCtsDataTest } from './use_cases/cts-tests.js';
import { history } from './use_cases/history.js';
import {
    nrcSucceeded,
    noEnodebField,
    emptyEnodebList,
    enodebList500,
    enodebValueSyntaxError,
    freqPairsKeySyntaxError,
    distanceNameTypo,
    negativeDistance,
    tooBigDistance
} from './use_cases/syntax.js';

export const options = defaultE2EOptions;

export default function () {
    if (__ENV.STAGING_LEVEL === 'PRODUCT') {
        group('API GW access health check', function() {
            e2eGateway.healthCheck();
            e2eCts.healthCheck();
            e2eNcmp.healthCheck();
            e2e5gcnr.healthCheck();
        });

        initE2eObjects();

        group('CTS E2E tests', function() {
            e2eCts.queryEnodebLtecells();
            e2eCts.queryGnbdu();
            e2eCts.queryLTECellsGeographics();
            e2eCts.queryNrCellsGeographics();
        });

        group('NCMP E2E tests', function() {
            e2eNcmp.knownNodesTest();
        });

        group('5G CNR E2E tests', function() {
            e2e5gcnr.singleEnodebAndGnbduWithNoFreq();
            e2e5gcnr.singleEnodebAndGnbduWithMultipleFreq();
            e2e5gcnr.externalGNodeBFunAndTermPointToGNB();
            e2e5gcnr.multipleEnodebAndGnbduWithFreq();
            e2e5gcnr.multiplePlmnIds();
            e2e5gcnr.externalCellCreated();
            e2e5gcnr.singleEnodebWithInvalidFdn();
            e2e5gcnr.monitoringEndpointReachability();
            e2e5gcnr.actuatorEndpointReachability();
//            e2e5gcnr.metricsValidationsPerformance();
        });
    } else {
        group('System status', function() {
            group('Check the system status of the running service which should respond to status 200', function() {
                healthCheck();
            })
        });

        group('NRC requests', function() {
            group('Start two requests, the first one looks for potential neighbours of an existing enodeb and it is expected to have Succeeded status 200, and for the second one it has an empty enodeb list and it is expected to get in return a Bad Request 400', function() {
                startNrc();
            })
            group('Start a new NRC request with successful status as parameter and ensure the response body length is bigger than 0', function() {
                monitoring();
            });
            group('Invoke the monitoring endpoint with ID 11111111-1111-1111-1111-111111111111 and ensure the response status is bigger than 0 so we have existing endpoints', function() {
                monitoringById();
            });
        });

        group('CTS tests to identify potential neighbours with multiple frequencies', function() {
            group('Run an NRC request for a distance of 600 and 1 frequency pair and it is supposed to identify 1 potential neighbour of gnodeb with 3 NRC cells', function() {
              happyPathTest();
            })
            group('Extra frequency is the same as happy path test the only difference is in the frequency parameter which contains one more frequency so it is expected to have a 4G node connected with 5G node that has three cells', function() {
              extraFreq();
            })
        });

        group('Monitoring', function() {
            group('Invoke the monitoring endpoint with an malformed UUID, and it is expected to fail when trying to extract an ID from the UUID and return a bad request 400 as a result', function() {
                monitoringInvalidId();
            })
            group('Invoke the monitoring endpoint with a list of IDs containing one single ID. This ID does not correspond to any existing NRC job so the request so return not found request 404', function() {
                monitoringNotFoundId();
            })
        });

        group('Cts tests', function() {
            group('Load 500 ENodeBs with 4000 LteCells in cts, then ensures that values of ENodeBs,LteCells,GeographicSite,FdDearfcnDl and CNRTestENB345 are properly loaded in cts, and verifies that distance between cells are calculated as expected', function() {
                createEnodebCtsDataTest();
            })
            group('Load 1000 GNBs with 4000 NRCells in cts, then ensures that values of GNBs,NRCells,GeographicSite,downlinkEARFCN and CNRTestGNB123 are properly loaded in cts, and verifies that distance between cells are calculated as expected', function() {
                createGndbuCtsDataTest();
            })
            group('After the tests in product staging pipeline are done, as a last step, the pipeline should cleanup the enodeb data that we loaded in CTS', function() {
                cleanupEnodebCtsDataTest();
            })
            group('After the tests in product staging pipeline are done, as a last step, the pipeline should cleanup the gnodeb data that we loaded in CTS', function() {
                cleanupGndbuCtsDataTest();
            })
        });

        group('Queuefull', function() {
            group('Overload the thread queue by starting 10 NRC request in order to have a full queue', function() {
                testSetup();
            })
            group('Start a new NRC request after we overloaded the thread queue and ensure the case of thread queue is full is handled by returning service unavailable status 503', function() {
                testTaskQueueIsFull();
            })
            group('Wait for the process queue to be cleared then ensure that the queue is not full after starting a new NRC request with successful status 200', function() {
                testTeardown();
            })
        });

        group("Checking different syntax", function() {
            group("Start NRC request with valid enodeb ID (29) and distance (200) and it is expected to get in return a Succeeded request 200", function() {
                nrcSucceeded();
            })
            group("Start NRC request without eNodeBIds and since it is a mandatory field it is expected to get in return a Bad Request 400", function() {
                noEnodebField();
            });
            group("Start NRC request with an empty enodeb list and it is expected to get in return a Bad Request 400", function() {
                emptyEnodebList();
            });
            group("Start NRC request with more than 500 enodeb IDs and it is expected to get in return a Bad Request 400", function() {
                enodebList500();
            })
            group("Start NRC request with json syntax error exactly at Enodeb value in which we gave XXX and it is expected to get in return a Bad Request 400", function() {
                enodebValueSyntaxError();
            });
            group("Start NRC request with an invalid frequency pairs key and it is expected to get in return Succeeded status since freqPairs key is only used as a filter", function() {
                freqPairsKeySyntaxError();
            });
            group("Start NRC request with a typo in field name distanceX instead of distance and it is expected to get in return Succeeded status since distance field is optional", function() {
                distanceNameTypo();
            })
            group("Start NRC request with negative distance (-200) and it is expected to get in return a Bad Request 400", function() {
                negativeDistance();
            });
            group("Start NRC request with very big distance ( more than Integer.MAX_VALUE )and it is expected to get in return a Bad Request 400", function() {
                tooBigDistance();
            });
        });

        group('NRC requests history', function() {
            group('Post 20 different NRC requests to the startNrc endpoint and store it into a list of the accepted request IDs from the response body then ensure the number of accepted request IDs is greater than the thread queue size (10) and all the accepted request IDs are available in the NRC history and contained by the NRC process list', function() {
                history();
            })
        });
    }
}

export function handleSummary(data) {
    let result = {'stdout': runSummary(data)};
    const reportPath = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/doc/Test_Report/' : '/tmp/';
    result[reportPath.concat('k6-test-results.html')] = htmlReport(data);
    result[reportPath.concat('summary.json')] = JSON.stringify(data);
    return result;
}