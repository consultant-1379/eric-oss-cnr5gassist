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
import * as common from './common.js';

const eiapProxyUrl = __ENV.EIAP_PROXY_URL ? __ENV.EIAP_PROXY_URL : 'http://127.0.0.1:8085';
const healthUri = '/health';
const restart5gcnrUri = '/restart_5gcnr';
const shutdownUri = '/shutdown';

function healthCheck() {
    group('EIAP Proxy health check', function() {
        common.checkResponse(common.httpGet(eiapProxyUrl, healthUri), {'status': 200});
    });
}

function restart5gcnr() {
    group('Restart 5G CNR service', function() {
        common.checkResponse(common.httpPost(eiapProxyUrl, restart5gcnrUri), {'status': 200});
    });
}

function shutdown() {
    group('Shutdown EIAP Proxy', function() {
        common.httpPost(eiapProxyUrl, shutdownUri);
    });
}

module.exports = {
    healthCheck,
    restart5gcnr,
    shutdown
}