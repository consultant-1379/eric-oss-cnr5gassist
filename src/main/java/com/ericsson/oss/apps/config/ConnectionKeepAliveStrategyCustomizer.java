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


import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

public class ConnectionKeepAliveStrategyCustomizer implements ConnectionKeepAliveStrategy {

    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 20 * 1000;

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        BasicHeaderElementIterator it = new BasicHeaderElementIterator
            (response.headerIterator(HttpHeaders.KEEP_ALIVE));

        while (it.hasNext()) {
            HeaderElement he = it.next();
            String param = he.getName();
            String value = he.getValue();

            if (value != null && param.equalsIgnoreCase("timeout")) {
                return TimeValue.ofMilliseconds(Long.parseLong(value) * 1000);
            }
        }
        return TimeValue.ofMilliseconds(DEFAULT_KEEP_ALIVE_TIME_MILLIS);
    }
}
