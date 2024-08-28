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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
@Slf4j
public class GatewayProperties {
    private String scheme;
    private String host;
    private String port;
    private Map<String, String> auth;
    private Map<String, Service> services;

    @Data
    public static class Service {
        private String url;
        private String basePath;
        private String headers;

        public Map<String, String> getHeadersAsMap() {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(StringUtils.isEmpty(headers) ? "{}" : headers, Map.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse headers", e);
                return new HashMap<>();
            }
        }
    }

    public String getUrl() {
        StringBuilder builder = new StringBuilder();
        if (scheme != null && !scheme.isEmpty()) {
            builder.append(scheme).append("://");
        }
        builder.append(host);
        if (port != null && !port.isEmpty()) {
            builder.append(":").append(port);
        }
        return builder.toString();
    }

    public String getBasePath(String serviceName) {
        Service service = getService(serviceName);
        return (StringUtils.isEmpty(service.getUrl()) ? getUrl() : service.getUrl()) + service.getBasePath();
    }

    public String getAuth(String name) {
        return auth.getOrDefault(name, null);
    }

    public Service getService(String name) {
        return services.getOrDefault(name, null);
    }
}
