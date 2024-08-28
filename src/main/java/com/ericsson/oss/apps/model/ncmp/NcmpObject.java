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
package com.ericsson.oss.apps.model.ncmp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.SEMI_COLON;

@Data
@Builder
@AllArgsConstructor
public class NcmpObject<A extends NcmpAttribute> {
    @NonNull
    private String id;
    @NonNull
    private A attributes;

    public static <A extends NcmpAttribute> String getName(Class<A> valueType) {
        return Optional.ofNullable(valueType.getAnnotation(Type.class))
            .map(Type::name).orElse(valueType.getSimpleName());
    }

    public static <A extends NcmpAttribute> String getParentResourceIdentifier(Class<A> valueType) {
        Optional<String> id = Optional.ofNullable(valueType.getAnnotation(ParentResourceIdentifier.class))
            .map(ParentResourceIdentifier::name);
        if (id.isPresent()) {
            if (!id.get().endsWith("/")) {
                return id.get().concat("/");
            }
            return id.get();
        }
        return "";
    }

    public static <A extends NcmpAttribute> String getAttributeNames(Class<A> valueType) {
        return Arrays.stream(valueType.getDeclaredFields())
            .map(f -> Optional.ofNullable(f.getAnnotation(JsonProperty.class))
                .map(JsonProperty::value).orElse(f.getName()))
            .collect(Collectors.joining(SEMI_COLON));
    }

    @JsonIgnore
    public static <A extends NcmpAttribute> String getResourceIdentifier(Class<A> valueType) {
        return getParentResourceIdentifier(valueType).concat(getName(valueType));
    }

    @JsonIgnore
    public String getName() {
        return getName(attributes.getClass());
    }
}
