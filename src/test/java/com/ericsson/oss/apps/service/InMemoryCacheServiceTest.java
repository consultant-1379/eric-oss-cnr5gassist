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

package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.util.NrcUtil;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.ericsson.oss.apps.util.TestDefaults.GUTRA_NETWORK_EXTERNAL_ID;
import static com.ericsson.oss.apps.util.TestDefaults.SCTP_ENDPOINT_EXTERNAL_ID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"nrc.cache.max-size=3", "nrc.cache.expiry-time=1"})
class InMemoryCacheServiceTest {
    @Autowired
    InMemoryCacheService cacheService;

    @BeforeEach
    void resetCache() {
        cacheService.clear();
    }

    @Test
    void getData() {
        Optional data = cacheService.get("hi");
        assertTrue(data.isEmpty());
        cacheService.add("hi", "Mock Object");
        Optional possibleStrData = cacheService.get("hi");
        if (possibleStrData.isPresent()) {
            assertEquals("Mock Object", possibleStrData.get());
        } else {
            fail("No object with key \"hi\" found");
        }
    }

    @Test
    void isCacheFull() {
        cacheService.add("data1", 2);
        cacheService.add("data2", 0.2f);
        cacheService.add("data3", "str");
        cacheService.add("data1", "str");
        assertTrue(cacheService.isFull());
    }

    @Test
    void setData() {
        cacheService.add("hi", "Mock Object");
        Optional possibleStrData = cacheService.get("hi");
        if (possibleStrData.isPresent()) {
            assertEquals("Mock Object", possibleStrData.get());
        } else {
            fail("No object with key \"hi\" found");
        }
    }

    @Test
    void isValid() {
        assertFalse(cacheService.isValid("hi"));
        cacheService.add("hi", "Mock Object");
        Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> !cacheService.isValid("hi"));
        assertFalse(cacheService.isValid("hi"));
    }

    @Test
    void removeKey() {
        cacheService.add("data1", 2);
        cacheService.add("data2", 0.2f);
        assertEquals(2, cacheService.count());

        cacheService.remove("data1");
        assertEquals(1, cacheService.count());
        assertTrue(cacheService.get("data1").isEmpty());
        assertEquals(0.2f, cacheService.get("data2").get());
    }

    @Test
    void removeKeys() {
        String key1 = NrcUtil.externalIdKey(SCTP_ENDPOINT_EXTERNAL_ID);
        String key2 = NrcUtil.externalIdKey(GUTRA_NETWORK_EXTERNAL_ID);
        cacheService.add("prefix1, " + key1 + ", postfix1", 1);
        cacheService.add("prefix2, " + key1 + ", postfix2", 2);
        cacheService.add("prefix3, " + key2 + ", postfix3", 3);
        assertEquals(3, cacheService.count());
        assertEquals(2, cacheService.count(key1));
        assertEquals(1, cacheService.count(key2));

        cacheService.remove(key1);
        assertEquals(1, cacheService.count());
        assertEquals(0, cacheService.count(key1));
        assertEquals(1, cacheService.count(key2));
    }

    @Test
    void clearCache() {
        cacheService.add("data1", 2);
        cacheService.add("data2", 0.2f);
        cacheService.add("data3", "str");
        cacheService.add("data1", "str");
        cacheService.clear();
        assertTrue(cacheService.get("data1").isEmpty());
        assertTrue(cacheService.get("data2").isEmpty());
        assertTrue(cacheService.get("data3").isEmpty());
    }

    @Test
    void isCacheFullAndHasExpiredValue() {
        cacheService.add("data1", 2);
        LocalDateTime currDate = LocalDateTime.now();
        Awaitility.await().atMost(150, TimeUnit.MILLISECONDS).until(() -> !currDate.equals(LocalDateTime.now()));

        cacheService.add("data2", 0.2f);
        cacheService.add("data3", "str");
        assertTrue(cacheService.isFull());

        //full and nothing is expired
        cacheService.add("data4", "str");
        assertTrue(cacheService.isFull());

        assertFalse(cacheService.get("data4").isEmpty());
        assertTrue(cacheService.get("data1").isEmpty());
        //full and one data expired
        Awaitility.await().atMost(3, TimeUnit.MINUTES).until(() -> !cacheService.isValid("data2"));
        cacheService.add("data5", "str1");
        assertEquals("str1", cacheService.get("data5").get());
    }
}