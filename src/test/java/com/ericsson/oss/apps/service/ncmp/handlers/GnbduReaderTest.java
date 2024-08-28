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
import com.ericsson.oss.apps.model.ncmp.GnbduFunction;
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

import static com.ericsson.oss.apps.util.TestDefaults.GNBDU;
import static com.ericsson.oss.apps.util.TestDefaults.toNcmpObject;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class GnbduReaderTest {

    @Mock
    private NcmpService ncmpService;
    @InjectMocks
    private GnbduReader gnbduReader;

    @Test
    public void readGnbduTest() {
        NcmpObject<GnbduFunction> gnbduObject = toNcmpObject(GnbduFunction.builder().gNBId(1L).gNBIdLength(3).build());
        Mockito.when(ncmpService.getResources(any(ExternalId.class), eq(GnbduFunction.class)))
            .thenReturn(Collections.singletonList(gnbduObject));

        Assertions.assertEquals(Optional.of(gnbduObject), gnbduReader.read(ExternalId.of(GNBDU.getExternalId())));
    }

}
