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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.ericsson.oss.apps.util.Constants.EQUAL;
import static com.ericsson.oss.apps.util.Constants.SESSION_KEY;

@Component
@ConditionalOnExpression("!'${gateway.auth.session:}'.isEmpty()")
public class StaticAuthenticationInterceptor extends AuthenticationInterceptor {

    @Value("${gateway.auth.session}")
    private String sessionId;

    @PostConstruct
    private void init() {
        session.set(SESSION_KEY + EQUAL + sessionId);
    }
}
