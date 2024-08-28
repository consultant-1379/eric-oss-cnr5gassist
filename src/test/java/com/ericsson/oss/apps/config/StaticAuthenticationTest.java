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

package com.ericsson.oss.apps.config;

import com.ericsson.oss.apps.config.client.StaticAuthenticationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {"gateway.auth.session=token"})
public class StaticAuthenticationTest {

    @Autowired
    private ApplicationContext appContext;

    @Test
    public void testBean(){
        List<String> beans = Arrays.asList(appContext.getBeanNamesForType(StaticAuthenticationInterceptor.class));
        assertFalse(beans.isEmpty());
    }
}