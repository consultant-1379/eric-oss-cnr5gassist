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

import com.ericsson.oss.apps.config.client.AuthenticationInterceptor;
import com.ericsson.oss.apps.config.client.EnmAdapterOverloadedInterceptor;
import com.ericsson.oss.apps.config.client.InsecureRestTemplateCustomizer;
import com.ericsson.oss.apps.config.client.RestTemplateCustomizerImpl;
import com.ericsson.oss.apps.exception.EnmAdapterOverloadedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Configuration
public class RestTemplateConfiguration {

    @Value("${gateway.retry.maxAttempts}")
    private int maxAttempts;
    @Value("${gateway.retry.maxDelay}")
    private long maxDelay;
    @Value("${gateway.retry.maxDelayEnmOverload}")
    private long maxDelayEnmOverload;

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        return converter;
    }

    @Bean
    @ConditionalOnProperty(value = "gateway.insecure", havingValue = "false")
    public RestTemplateCustomizerImpl restTemplateCustomizer() {
        return new RestTemplateCustomizerImpl();
    }

    @Bean
    @ConditionalOnProperty(value = "gateway.insecure", havingValue = "true")
    public InsecureRestTemplateCustomizer requestFactoryCustomizer()
        throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return new InsecureRestTemplateCustomizer();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ObjectProvider<AuthenticationInterceptor> authenticationInterceptors) {
        RestTemplate restTemplate = builder.build();
        List<ClientHttpRequestInterceptor> existingInterceptors = restTemplate.getInterceptors();
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(existingInterceptors);
        Optional<AuthenticationInterceptor> authenticationInterceptor = authenticationInterceptors.orderedStream().findAny();
        if (authenticationInterceptor.isPresent()) {
            AuthenticationInterceptor interceptor = authenticationInterceptor.get();
            if (!existingInterceptors.contains(interceptor)) {
                interceptors.add(interceptor);
            }
        }
        interceptors.add(new EnmAdapterOverloadedInterceptor());

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    @Bean("restTemplateRetry")
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(makeExceptionClassifierRetryPolicy());
        Map<Class<? extends Throwable>, Long> exBackoffMap = Map.of(
            ResourceAccessException.class, maxDelay,
            EnmAdapterOverloadedException.class, maxDelayEnmOverload);
        retryTemplate.setBackOffPolicy(new MultipleExceptionsBackoffPolicy(exBackoffMap));
        return retryTemplate;
    }

    private RetryPolicy makeExceptionClassifierRetryPolicy() {
        final ExceptionClassifierRetryPolicy exceptionClassifierRetryPolicy = new ExceptionClassifierRetryPolicy();
        final Map<Class<? extends Throwable>, RetryPolicy> policyMap = new HashMap<>();

        policyMap.put(ResourceAccessException.class, new SimpleRetryPolicy(maxAttempts));
        policyMap.put(EnmAdapterOverloadedException.class, new SimpleRetryPolicy(maxAttempts));

        exceptionClassifierRetryPolicy.setPolicyMap(policyMap);

        return exceptionClassifierRetryPolicy;
    }
}
