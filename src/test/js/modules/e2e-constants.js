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
import * as ctsCommon from './cts-common.js';

export const netwpfx = "Europe/Ireland/"
export const netsimpfx = "NETSimW/"
export const enoden1 = "LTE416dg2ERBS00001"
export const enoden2 = "LTE416dg2ERBS00002"
export const enoden3 = "LTE31dg2ERBS00013"

export const gnoden1 = "NR01gNodeBRadio00011"
export const gnoden2 = "NR01gNodeBRadio00018"
export const gnoden3 = "NR01gNodeBRadio00019"
export const gnoden4 = "NR01gNodeBRadio00020"


export const enodebFdn1 = netwpfx + netsimpfx + enoden1 + "/1"
export const enodebFdn2 = netwpfx + netsimpfx + enoden2 + "/1"
export const enodebFdn3 = netwpfx + netsimpfx + enoden3 + "/1"

export const ltecellFdn1= netwpfx + netsimpfx + enoden1 + "/1/"+ enoden1 + "-1"
export const ltecellFdn2= netwpfx + netsimpfx + enoden2 + "/1/"+ enoden2 + "-1"

export const gnbduFdn1 = netwpfx + gnoden1 + "/" + gnoden1 + "/1"
export const gnbduFdn2 = netwpfx + gnoden2 + "/" + gnoden2 + "/1"
export const gnbduFdn3 = netwpfx + gnoden3 + "/" + gnoden3 + "/1"
export const gnbduFdn4 = netwpfx + gnoden4 + "/" + gnoden4 + "/1"

export const nrcellFdn1 = netwpfx + gnoden1 + "/" + gnoden1 + "/1/" + gnoden1 + "-1"
export const nrSectorCarrierFdn1 = netwpfx + gnoden1 + "/" + gnoden1 + "/1/1"
export const nrSectorCarrierFdn3 = netwpfx + gnoden3 + "/" + gnoden3 + "/1/1"

export const e2eObjects = {}

export const nodeRadius = 40
export const nodeLat = 53.34198
export const enodeb1Lon = -6.2867
export const gnodeb1Lon = -6.2887


export function initE2eObjects() {
    e2eObjects.enodeb1 = ctsCommon.getEnodeb({'fdn': enodebFdn1});
    e2eObjects.enodeb2 = ctsCommon.getEnodeb({'fdn': enodebFdn2});
    e2eObjects.enodeb3 = ctsCommon.getEnodeb({'fdn': enodebFdn3});

    e2eObjects.ltecell1 = ctsCommon.getLtecell({'fdn': ltecellFdn1});
    e2eObjects.ltecell2 = ctsCommon.getLtecell({'fdn': ltecellFdn2});

    e2eObjects.gnbdu1 = ctsCommon.getGnbdu({'fdn': gnbduFdn1});
    e2eObjects.gnbdu2 = ctsCommon.getGnbdu({'fdn': gnbduFdn2});
    e2eObjects.gnbdu3 = ctsCommon.getGnbdu({'fdn': gnbduFdn3});
    e2eObjects.gnbdu4 = ctsCommon.getGnbdu({'fdn': gnbduFdn4});

    e2eObjects.nrcell1 = ctsCommon.getNrcell({'fdn': nrcellFdn1});
    e2eObjects.nrSectorCarrier1 = ctsCommon.getNrSectorCarrier({'fdn': nrSectorCarrierFdn1});
    e2eObjects.nrSectorCarrier3 = ctsCommon.getNrSectorCarrier({'fdn': nrSectorCarrierFdn3});
}