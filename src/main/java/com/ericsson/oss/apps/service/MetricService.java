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

package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.config.MetricProperties;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.*;
import io.micrometer.core.lang.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricService {

    private final MeterRegistry meterRegistry;
    private final MetricProperties metricProperties;
    private final Map<String, Timer.Sample> timedTasks = new ConcurrentHashMap<>();

    public void increment(String metricName, String... tags) {
        increment(metricName, 1, tags);
    }

    public void increment(String metricName, double value, String... tags) {
        Optional<Counter> counter = findMetric(Counter.class, metricName, tags);
        if (!counter.isPresent()) {
            Optional result = registerMetric(Counter.builder(metricName), metricName, tags);
            counter = result.isPresent() ? Optional.of((Counter) result.get()) : Optional.empty();
        }

        if (counter.isPresent()) {
            counter.get().increment(value);
        }
    }

    public <T> void createGauge(String metricName, @Nullable T object, ToDoubleFunction<T> function, String... tags) {
        if (!findGauge(metricName, tags).isPresent()) {
            registerMetric(Gauge.builder(metricName, object, function), metricName, tags);
        }
    }

    public void startTimer(String keyId, String metricName, String... tags) {
        if (!findTimer(metricName, tags).isPresent()) {
            registerMetric(Timer.builder(metricName), metricName, tags);
        }

        timedTasks.put(metricName + keyId, Timer.start(meterRegistry));
    }

    public void stopTimer(String keyId, String metricName, String... tags) {
        Optional<Timer> timer = findTimer(metricName, tags);
        if (timer.isPresent()) {
            timedTasks.get(metricName + keyId).stop(timer.get());
            timedTasks.remove(metricName + keyId);
        }
    }

    public void clearTimedTasks() {
        timedTasks.clear();
    }

    public void stopLastTaskTimer(String metricName) {
        String lastKey = null;
        for (String key : timedTasks.keySet()) {
            if (key.contains(metricName)) {
                lastKey = key.replace(metricName, "");
            }
        }
        if (lastKey != null) {
            stopTimer(lastKey, metricName);
            //remove the remaining timer samples, because they won't be used
            for (String key : timedTasks.keySet()) {
                if (key.contains(metricName)) {
                    timedTasks.remove(key);
                }
            }
        }
    }

    public Optional<Counter> findCounter(String metricName, String... tags) {
        return findMetric(Counter.class, metricName, tags);
    }

    public Optional<Gauge> findGauge(String metricName, String... tags) {
        return findMetric(Gauge.class, metricName, tags);
    }

    public Optional<Timer> findTimer(String metricName, String... tags) {
        return findMetric(Timer.class, metricName, tags);
    }

    private <M extends Meter> Optional<M> findMetric(Class<M> meterClass, String metricName, String... tags) {
        Set<Tag> tagSet = new HashSet<>();
        tagSet.add(Tag.of(UNIQUE_APP_ID, metricProperties.getUniqueAppId()));
        tagSet.add(Tag.of(INSTANCE_ID, metricProperties.getInstance()));
        for (int i = 0; i < tags.length - 1; i += 2) {
            tagSet.add(Tag.of(tags[i], tags[i + 1]));
        }

        Set<String> keySet = new HashSet<>(Arrays.asList(UNIQUE_APP_ID, INSTANCE_ID));
        keySet.addAll(REQUIRED_TAG_KEYS.getOrDefault(metricName, Collections.emptyList()));

        List<M> resultList = meterRegistry.getMeters().stream()
            .filter(meterClass::isInstance)
            .map(meterClass::cast)
            .filter(m -> metricName.equals(m.getId().getName()))
            .filter(m -> tagSet.equals(new HashSet<>(m.getId().getTags())))
            .filter(m -> keySet.equals(new HashSet<>(
                m.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toList()))))
            .collect(Collectors.toList());

        if (resultList.size() > 1) {
            log.warn("Multiple metrics found for {} {}", metricName, tags);
        }

        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
    }

    private Optional<Object> registerMetric(Object builder, String metricName, String... tags) {
        try {
            builder = builder.getClass().getMethod(TAG, String.class, String.class)
                    .invoke(builder, UNIQUE_APP_ID, metricProperties.getUniqueAppId());
            builder = builder.getClass().getMethod(TAG, String.class, String.class)
                    .invoke(builder, INSTANCE_ID, metricProperties.getInstance());
            for (int i = 0; i < tags.length - 1; i += 2) {
                builder = builder.getClass().getMethod(TAG, String.class, String.class)
                        .invoke(builder, tags[i], tags[i + 1]);
            }
            builder = builder.getClass().getMethod(DESCRIPTION, String.class)
                    .invoke(builder, METRIC_DESCRIPTIONS.get(metricName));
            return Optional.of(builder.getClass().getMethod(REGISTER, MeterRegistry.class).invoke(builder, meterRegistry));
        } catch (Exception e) {
            log.error("Metric method invoke error", e);
        }
        return Optional.empty();
    }
}
