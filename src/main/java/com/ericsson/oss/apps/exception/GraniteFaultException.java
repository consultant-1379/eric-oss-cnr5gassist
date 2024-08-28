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

package com.ericsson.oss.apps.exception;

import com.ericsson.oss.apps.client.cts.model.GraniteFaultStack;

public class GraniteFaultException extends RuntimeException {
    public final transient GraniteFaultStack graniteFaultStack;

    public GraniteFaultException(GraniteFaultStack faultStack) {
        super(faultStack.getMessageText());
        graniteFaultStack = faultStack;
    }
}