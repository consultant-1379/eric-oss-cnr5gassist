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

import com.ericsson.oss.apps.client.gw.GatewayServiceApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"gateway.auth.session=", "gateway.auth.login=username", "gateway.auth.password=password"})
public class DynamicAuthenticationTest {

    @MockBean
    private GatewayServiceApi gatewayServiceApi;

    @Test
    public void testLogin(){
        Mockito.verify(gatewayServiceApi, Mockito.times(1)).login();
    }
}
