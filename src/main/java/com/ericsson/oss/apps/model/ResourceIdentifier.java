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

import com.ericsson.oss.apps.exception.InvalidIdentifierException;
import lombok.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.*;

@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ResourceIdentifier {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("/(([\\w\\-.]+):)?([\\w\\-.]+)(\\[@?id=([\\w\\-]+)]|=([\\w\\-]+))");

    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Node {

        private static Predicate<Node> isNameEqual(String name) {
            return n -> name.equals(n.getName());
        }

        private static Predicate<Node> isNamespaceEqual(String namespace) {
            return n -> namespace.equals(n.getNamespace());
        }

        private final String namespace;
        @NonNull
        private final String name;
        @NonNull
        private final String value;

        public String toString() {
            return getFullName() + EQUAL + value;
        }

        public String getFullName() {
            StringBuilder builder = new StringBuilder();
            if (namespace != null) {
                builder.append(namespace).append(COLON);
            }
            builder.append(name);
            return builder.toString();
        }
    }

    public ResourceIdentifier() {
        this(Collections.emptyList());
    }

    @Getter(value = AccessLevel.PROTECTED)
    private final List<Node> nodes;

    public static ResourceIdentifier of(final String resourceIdentifier) {
        Matcher matcher = IDENTIFIER_PATTERN.matcher(resourceIdentifier);
        int length = 0;
        LinkedList<Node> nodes = new LinkedList<>();

        while (matcher.find()) {
            length += matcher.group(0).length();
            String id = matcher.group(5) != null ? matcher.group(5) : matcher.group(6);
            nodes.add(new Node(matcher.group(2), matcher.group(3), id));

            if (resourceIdentifier.length() == length) {
                return new ResourceIdentifier(Collections.unmodifiableList(nodes));
            }
        }

        throw new InvalidIdentifierException(resourceIdentifier);
    }

    public ResourceIdentifier add(final Node node) {
        return new ResourceIdentifier(Stream.concat(nodes.stream(), Stream.of(node))
            .collect(Collectors.toUnmodifiableList()));
    }

    public ResourceIdentifier addAll(final ResourceIdentifier resourceIdentifier) {
        return new ResourceIdentifier(Stream.of(nodes, resourceIdentifier.getNodes())
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList()));
    }

    public Node getFirst() {
        return nodes.get(0);
    }

    public Node getLast() {
        return nodes.get(nodes.size() - 1);
    }

    @Override
    public String toString() {
        return nodes.stream()
            .map(Node::toString)
            .collect(Collectors.joining(BACKSLASH, BACKSLASH, ""));
    }

    public Fdn toFdn() {
        return new Fdn(nodes.stream()
            .map(node -> Map.entry(node.getName(), node.getValue()))
            .collect(Collectors.toUnmodifiableList()));
    }

    public ResourceIdentifier getParent() {
        return new ResourceIdentifier(Collections.unmodifiableList(nodes.subList(0, nodes.size() - 1)));
    }

    public ResourceIdentifier getRoot() {
        return new ResourceIdentifier(Collections.unmodifiableList(nodes.subList(0, 1)));
    }

    public Optional<ResourceIdentifier> getAncestorIdByName(String name) {
        return getAncestorId(Node.isNameEqual(name));
    }

    public Optional<ResourceIdentifier> getAncestorIdByFullName(String namespace, String name) {
        return getAncestorId(Node.isNamespaceEqual(namespace)
            .and(Node.isNameEqual(name)));
    }

    private Optional<ResourceIdentifier> getAncestorId(Predicate<Node> predicate) {
        LinkedList<Node> ancestorId = new LinkedList<>();

        for (Node node : nodes) {
            ancestorId.add(node);
            if (predicate.test(node)) {
                return Optional.of(new ResourceIdentifier(Collections.unmodifiableList(ancestorId)));
            }
        }

        return Optional.empty();
    }
}
