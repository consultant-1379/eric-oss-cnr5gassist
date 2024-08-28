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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;


@Data
@Configuration
@ConfigurationProperties(prefix = "nrc")
public class NrcProperties {

    private int threadPoolSize;
    private int threadQueueSize;
    private int historySize;

    @PostConstruct
    private void init(){
        threadPoolSize = Math.max(threadPoolSize, 1);
        threadQueueSize = Math.max(threadQueueSize, 1);
        historySize = Math.max(historySize, threadPoolSize + threadQueueSize);
    }
}


