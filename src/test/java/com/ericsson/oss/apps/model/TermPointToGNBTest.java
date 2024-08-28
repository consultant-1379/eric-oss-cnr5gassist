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

import com.ericsson.oss.apps.model.ncmp.TermPointToGNB;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ericsson.oss.apps.util.TestDefaults.TERM_POINT_TO_GNB_ID;

public class TermPointToGNBTest {
    @Test
    void validIpAddressTest() {
        List.of("127.0.0.1", "19.117.63.126", "1.2.3.4", "255.255.253.0", "19.117.63.253")
            .forEach(item -> Assertions.assertEquals(item, TermPointToGNB.builder().termPointToGNBId(TERM_POINT_TO_GNB_ID).ipAddress(item).build().getIpAddress()));

        List.of("2001:db8:3333:4444:5555:6666:7777:8888",
                "2001:DB8:3333:4444:5555:6666:7777:8888",
                "2001:db8:3333:4444:CCCC:DDDD:EEEE:FFFF",
                "2001:db8:1::ab9:C0A8:102",
                "2001:0db8:0001:0000:0000:0ab9:C0A8:0102",
                "::",
                "2001:db8::",
                "2001:db8::1234:5678",
                "::1234:5678")
            .forEach(item -> Assertions.assertEquals(item, TermPointToGNB.builder().termPointToGNBId(TERM_POINT_TO_GNB_ID).ipAddress(item).build().getIpv6Address()));
    }

    @Test
    void invalidIpAddressTest() {
        List.of("invalid ip", "ZZZZ:db8:3333:4444:ZZZZ:6666:7777:ZZZZ")
            .forEach(item -> {
                TermPointToGNB termPoint = TermPointToGNB.builder().termPointToGNBId(TERM_POINT_TO_GNB_ID).ipAddress(item).build();
                Assertions.assertNull(termPoint.getIpAddress());
                Assertions.assertNull(termPoint.getIpv6Address());
            });
    }

    @Test
    void testConstructorTest() {
//        TermPointToGNB test = new TermPointToGNB(TERM_POINT_TO_GNB_ID, "1.1.1.1");
        TermPointToGNB test1 = TermPointToGNB.builder().termPointToGNBId(TERM_POINT_TO_GNB_ID).ipAddress("2001:db8:3333:4444:5555:6666:7777:8888").build();
//        System.out.println("Test: "+test.getAdministrativeState() +", class: "+test);
        System.out.println("Test1: "+test1.getAdministrativeState()+", class: "+test1);
    }
}
