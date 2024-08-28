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

package com.ericsson.oss.apps.config.client;

import com.ericsson.oss.apps.client.gw.GatewayServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.ericsson.oss.apps.util.Constants.EQUAL;
import static com.ericsson.oss.apps.util.Constants.SESSION_KEY;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(value = StaticAuthenticationInterceptor.class)
@ConditionalOnExpression("!'${gateway.auth.login:}'.isEmpty() || !'${gateway.auth.password:}'.isEmpty()")
public class DynamicAuthenticationInterceptor extends AuthenticationInterceptor {

    private final GatewayServiceApi gatewayServiceApi;

    @PostConstruct
    private void init() {
        refreshSession();
    }

    @Scheduled(initialDelayString = "${gateway.auth.refresh-period}000",
            fixedRateString = "${gateway.auth.refresh-period}000")
    void refreshSession() {
        session.set(SESSION_KEY + EQUAL + gatewayServiceApi.login());
    }
}
