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
package com.ericsson.oss.apps.service.ncmp.handlers;

import com.ericsson.oss.apps.model.ExternalId;
import com.ericsson.oss.apps.model.ncmp.AdditionalPLMNInfo;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.service.NcmpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class AdditionalPLMNIdReaderTest {

    @Mock
    private NcmpService ncmpService;

    @InjectMocks
    private AdditionalPLMNInfoReader additionalPLMNInfoReader;

    @Test
    public void readAdditionalPLMNIdTest() {
        NcmpObject<AdditionalPLMNInfo> nrCellCUObject = toNcmpObject(AdditionalPLMNInfo.builder()
            .build());
        Mockito.when(ncmpService.getResources(any(ExternalId.class), eq(AdditionalPLMNInfo.class)))
            .thenReturn(Collections.singletonList(nrCellCUObject));

        Assertions.assertEquals(nrCellCUObject, additionalPLMNInfoReader.read(MANAGED_ELEMENT_EXTERNAL_ID).get(0));
    }
}