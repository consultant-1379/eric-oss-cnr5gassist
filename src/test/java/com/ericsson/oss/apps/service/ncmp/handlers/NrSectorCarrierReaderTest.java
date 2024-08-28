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

import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.SectorCarrier;
import com.ericsson.oss.apps.service.NcmpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class NrSectorCarrierReaderTest {

    @Mock
    private NcmpService ncmpService;
    @InjectMocks
    private NrSectorCarrierReader nrSectorCarrierReader;

    private static final String ID = "1";
    private static final int ESS_PAIR_ID = 1;

    @Test
    public void readNrSectorCarrierTest() {
        NcmpObject<SectorCarrier> nrSectorCarrierObject =
            toNcmpObject(SectorCarrier.builder().nRSectorCarrierId(ID).essScPairId(ESS_PAIR_ID).build());
        Mockito.when(ncmpService.getResource(eq(NR_SECTOR_CARRIER_EXTERNAL_ID), eq(SectorCarrier.class)))
            .thenReturn(Optional.ofNullable(nrSectorCarrierObject));

        Assertions.assertEquals(Optional.of(nrSectorCarrierObject),
            nrSectorCarrierReader.read(NR_SECTOR_CARRIER_EXTERNAL_ID));
    }
}
