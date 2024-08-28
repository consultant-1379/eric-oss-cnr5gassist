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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CacheDataTest {

    @Test
    void getData() {
        CacheData<String> data = new CacheData<>("someCache", 5);
        assertEquals("someCache", data.getData());
    }

    @Test
    void getTimeStamp() {
        LocalDateTime currentLocalDate = LocalDateTime.now();
        try (MockedStatic<LocalDateTime> topDateTimeUtilMock = Mockito.mockStatic(LocalDateTime.class)) {
            topDateTimeUtilMock.when(() -> LocalDateTime.now()).thenReturn(currentLocalDate);
            CacheData<String> data = new CacheData<>("someCache", 5);
            assertEquals(currentLocalDate, data.getTimestamp());
        }
    }

    @Test
    void isExpired() {
        CacheData<String> data = new CacheData<>("someCache", 1);
        assertFalse(data.isExpired());
        Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> data.isExpired());
        assertTrue(data.isExpired());
    }
}