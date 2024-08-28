/*******************************************************************************
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
 ******************************************************************************/

package com.ericsson.oss.apps.model;

import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.client.cts.model.ENodeB;
import com.ericsson.oss.apps.client.cts.model.Gnbdu;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class EnmUpdateContext {

    @NonNull
    private final NrcTask nrcTask;
    @NonNull
    private final ENodeB eNodeB;
    @NonNull
    private final Gnbdu gnbdu;
}