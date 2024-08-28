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
package com.ericsson.oss.apps.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@UtilityClass
public class StreamTools {
    public static <T> Stream<T> collectionToStream(Collection<T> collection) {
        return Stream.ofNullable(collection).flatMap(Collection::stream).filter(Objects::nonNull);
    }
}
