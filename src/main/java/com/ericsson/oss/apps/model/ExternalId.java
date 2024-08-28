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
import com.ericsson.oss.apps.model.ncmp.NcmpAttribute;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import lombok.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ericsson.oss.apps.util.Constants.COLON;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class ExternalId {
    private static final Pattern EXTERNAL_ID_PATTERN = Pattern.compile("^([\\w]+(?=/))(.+)$");


    @NonNull private final String cmHandle;
    @NonNull private final ResourceIdentifier resourceIdentifier;

    public static ExternalId of(final String externalId) {
        Matcher matcher = EXTERNAL_ID_PATTERN.matcher(externalId);
        if (matcher.find()) {
            try {
                return ExternalId.of(matcher.group(1), matcher.group(2));
            } catch (InvalidIdentifierException e) {
                throw new InvalidIdentifierException(externalId, e);
            }
        }
            throw new InvalidIdentifierException(externalId);
    }

    public static ExternalId of(final String cmHandle, final String resourceIdentifier) {
        return new ExternalId(cmHandle, ResourceIdentifier.of(resourceIdentifier));
    }

    public ExternalId of(final ResourceIdentifier resourceIdentifier) {
        return new ExternalId(cmHandle, resourceIdentifier);
    }

    public static ExternalId of(final String cmHandle, final ResourceIdentifier resourceIdentifier) {
        return new ExternalId(cmHandle, resourceIdentifier);
    }

    public ExternalId getParent() {
        return new ExternalId(cmHandle, resourceIdentifier.getParent());
    }

    public ExternalId getRoot(){
        return new ExternalId(cmHandle, resourceIdentifier.getRoot());
    }

    public <T extends NcmpAttribute> ExternalId add(final NcmpObject<T> object) {
        String[] name = object.getName().split(COLON);
        return new ExternalId(cmHandle, resourceIdentifier.add(new ResourceIdentifier.Node(name[0], name[1], object.getId())));
    }

    public Fdn toFdn() {
        return resourceIdentifier.toFdn();
    }

    @Override
    public String toString() {
        return cmHandle + resourceIdentifier;
    }
}
