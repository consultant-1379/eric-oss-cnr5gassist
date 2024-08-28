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
package com.ericsson.oss.apps.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.*;

@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Fdn {

    public static final String ROOT_ELEMENT = "ManagedElement";
    public static final Map<String, String> MODULE_NAMESPACE_MAPPING = Map.of(
        ROOT_ELEMENT, "erienmnrmcomtop",
        "SctpEndpoint", "erienmnrmrtnsctp",
        "Router", "erienmnrmrtnl3router",
        "InterfaceIPv4", "erienmnrmrtnl3interfaceipv4",
        "InterfaceIPv6", "erienmnrmrtnl3interfaceipv6",
        "GNBCUCPFunction", "erienmnrmgnbcucp",
        "EndpointResource", "erienmnrmgnbcucp"
    );

    @Getter(value = AccessLevel.PROTECTED)
    private final List<Map.Entry<String, String>> mos;

    @JsonCreator
    public Fdn(String fdnString) {
        this((fdnString==null || fdnString.isEmpty()) ? new ArrayList<>() :
                Arrays.stream(fdnString.split(COMMA))
                    .map(item -> item.split(EQUAL))
                    .map(item -> Map.entry(item[0], item[1]))
                    .collect(Collectors.toUnmodifiableList()));
    }

    public static Fdn of(final String fdnString) {
        return new Fdn(fdnString);
    }

    public Fdn add(Map.Entry<String, String> mo) {
        return new Fdn(Stream.concat(mos.stream(), Stream.of(mo))
            .collect(Collectors.toUnmodifiableList()));
    }

    public Fdn addAll(Fdn fdn) {
        return new Fdn(Stream.of(mos, fdn.getMos()).flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList()));
    }

    public Map.Entry<String, String> getLast() {
        return mos.get(mos.size()-1);
    }

    @Override
    @JsonValue
    public String toString() {
        return mos.stream()
            .map(Object::toString)
            .collect(Collectors.joining(COMMA));
    }

    public ResourceIdentifier toResourceIdentifier() {
        AtomicReference<String> namespace = new AtomicReference<>(MODULE_NAMESPACE_MAPPING.getOrDefault(ROOT_ELEMENT, null));
        return new ResourceIdentifier(mos.stream()
            .dropWhile(e -> !ROOT_ELEMENT.equals(e.getKey()))
            .map(e -> {
                namespace.set(MODULE_NAMESPACE_MAPPING.getOrDefault(e.getKey(), namespace.get()));
                return new ResourceIdentifier.Node(namespace.get(), e.getKey(), e.getValue());
            })
            .collect(Collectors.toUnmodifiableList()));
    }
}
