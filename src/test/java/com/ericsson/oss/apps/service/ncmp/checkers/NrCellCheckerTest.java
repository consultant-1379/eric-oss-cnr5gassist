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

package com.ericsson.oss.apps.service.ncmp.checkers;

import com.ericsson.oss.apps.service.ncmp.handlers.ExternalCellHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class NrCellCheckerTest {

    @Mock
    private ExternalCellHandler externalCellHandler;
    @InjectMocks
    private NrCellChecker nrCellChecker;

    @Test
    void withLocalCellIdValue() {
        Mockito.when(externalCellHandler.read(any(), any())).thenReturn(Optional.of(EXTERNAL_CELL_EXTERNAL_ID));
        boolean actual = nrCellChecker.withLocalCellIdValue(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, NR_CELL_111);
        assertTrue(actual);
    }

    @Test
    void withLocalCellIdNull() {
        assertTrue(nrCellChecker.withLocalCellIdNull(NR_CELL_222));
    }
}