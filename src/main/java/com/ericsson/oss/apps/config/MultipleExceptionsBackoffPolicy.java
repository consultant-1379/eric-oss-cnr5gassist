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

import org.springframework.classify.Classifier;
import org.springframework.classify.SubclassClassifier;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetrySynchronizationManager;

import java.util.Map;

public class MultipleExceptionsBackoffPolicy extends FixedBackOffPolicy {
    private Classifier<Throwable, Long> classifier;

    public MultipleExceptionsBackoffPolicy(final Map<Class<? extends Throwable>, Long> throwableBackoffMap) {
        classifier = new SubclassClassifier<>(throwableBackoffMap, 5_000L);
    }

    @Override
    protected void doBackOff() {
        Long backoff = classifier.classify(RetrySynchronizationManager.getContext().getLastThrowable());
        setBackOffPeriod(backoff);
        super.doBackOff();
    }
}
