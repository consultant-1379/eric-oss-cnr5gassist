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
import * as common from '../modules/common.js'

function healthCheck() {
    group('Gateway health check', function() {
        check(common.getSessionId(), {
            'The JSESSIONID is not empty': (r) => r
        });
    });
}

module.exports = {
    healthCheck
}