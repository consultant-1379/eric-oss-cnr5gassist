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

import com.ericsson.oss.apps.exception.EnmAdapterOverloadedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

@Slf4j
@Component
public class EnmAdapterOverloadedInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request,
        byte[] body,
        ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            String errorMessage = new String(FileCopyUtils.copyToByteArray(response.getBody()), "UTF-8");
            if (errorMessage.contains("Failed to communicate with ENM")) {
                log.debug("Enm is overloaded. It is unavailable temporarily, retry again in 50s.");
                throw new EnmAdapterOverloadedException("Enm is overloaded");
            }
        }
        return response;
    }
}