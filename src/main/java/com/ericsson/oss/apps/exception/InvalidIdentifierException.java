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

import lombok.Getter;

@Getter
public class InvalidIdentifierException extends RuntimeException {
    private final String identifier;

    public InvalidIdentifierException(String identifier) {
        this(identifier, null);
    }

    public InvalidIdentifierException(String identifier, Throwable cause) {
        super(String.format("Identifier: %s is invalid", identifier), cause);
        this.identifier = identifier;
    }
}
