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

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public class CacheData<T> {
    private final T data;
    private final LocalDateTime timestamp;
    private final int expiryInSeconds;

    public CacheData(T data, int expiryInSeconds) {
        this.data = data;
        this.expiryInSeconds = expiryInSeconds;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isExpired() {
        return ChronoUnit.SECONDS.between(timestamp, LocalDateTime.now()) > expiryInSeconds;
    }
}