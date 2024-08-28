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
import com.ericsson.oss.apps.model.ncmp.*;
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
public class NrCellCUReaderTest {

    @Mock
    private NcmpService ncmpService;
    @InjectMocks
    private NrCellCUReader nrCellCUReader;

    private static final String ID = "1";

    @Test
    public void readNrCellCUTest() {
        NcmpObject<NrCellCU> nrCellCUObject = toNcmpObject(NrCellCU.builder().nRCellCUId(ID)
            .cellLocalId(11L).pSCellCapable(false).build());
        Mockito.when(ncmpService.getResourcesWithOptions(any(ExternalId.class),
            eq(NrCellCU.builder().cellLocalId(25L)
                .build()), eq(NrCellCU.class)))
            .thenReturn(Collections.singletonList(nrCellCUObject));

        Assertions.assertEquals(Optional.of(nrCellCUObject), nrCellCUReader.read(MANAGED_ELEMENT_EXTERNAL_ID,
            NR_CELL_111.getLocalCellIdNci()));
    }

}
