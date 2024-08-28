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

import { monitoringId400, monitoringId404 } from '../modules/nrc-common.js';

function monitoringInvalidId() {
    monitoringId400('invalid-UUID');
}

function monitoringNotFoundId() {
    monitoringId404('7b11af48-d117-4325-b957-75f9ab87ca4d');
}

module.exports = {
    monitoringInvalidId,
    monitoringNotFoundId
}
