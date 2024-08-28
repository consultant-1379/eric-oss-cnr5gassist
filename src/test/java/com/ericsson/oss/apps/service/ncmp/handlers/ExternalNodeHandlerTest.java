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
import com.ericsson.oss.apps.model.ncmp.ExternalGNodeBFunction;
import com.ericsson.oss.apps.model.ncmp.GUtraNetwork;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.NcmpService;
import com.ericsson.oss.apps.service.ncmp.EnmUpdateMonitorService;
import com.ericsson.oss.apps.util.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ExternalNodeHandlerTest {

    private static final NcmpObject<ExternalGNodeBFunction> NCMP_OBJECT = toNcmpObject(NCMP_OBJECT_ID_NR45, EXTERNAL_G_NODE_B_FUNCTION_ATTRIBUTES);

    @Mock
    private NcmpService ncmpService;
    @Mock
    private MetricService metricService;
    @Mock
    private EnmUpdateMonitorService enmUpdateMonitorService;
    @InjectMocks
    private ExternalNodeHandler externalNodeHandler;

    @Test
    public void readExternalGNodeBFunction() {
        Mockito.when(ncmpService.getResourcesWithOptions(eq(GUTRA_NETWORK_EXTERNAL_ID), any(), eq(ExternalGNodeBFunction.class)))
            .thenReturn(List.of(NCMP_OBJECT));

        Assertions.assertEquals(Optional.of(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), externalNodeHandler.read(GUTRA_NETWORK_EXTERNAL_ID, GNBDU, GNBDU_FUNCTION));
    }

    @Test
    public void createExternalGNodeBFunctionUnderGUtraNetwork() {
        Assertions.assertEquals(Optional.of(NCMP_OBJECT), externalNodeHandler.create(GUTRA_NETWORK_EXTERNAL_ID, GNBDU, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));

        Mockito.verify(ncmpService, Mockito.times(1)).createResource(eq(GUTRA_NETWORK_EXTERNAL_ID), any());
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_OBJECT_COUNT), eq(Constants.MetricConstants.NCMP_OBJECT), eq(EXTERNALGNODEBFUNCTIONS));
    }

    @Test
    public void createGUtraNetworkThenExternalGNodeBFunction() {
        Mockito.when(ncmpService.getResource(any(ExternalId.class), eq(GUtraNetwork.class)))
            .thenReturn(Optional.empty());
        Mockito.when(ncmpService.createResource(or(eq(GUTRA_NETWORK_EXTERNAL_ID), eq(ENODEB_EXTERNAL_ID)), any()))
            .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        Assertions.assertEquals(Optional.of(NCMP_OBJECT), externalNodeHandler.create(GUTRA_NETWORK_EXTERNAL_ID, GNBDU, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));

        Mockito.verify(ncmpService, Mockito.times(1)).createResource(eq(GUTRA_NETWORK_EXTERNAL_ID), any());
        Mockito.verify(ncmpService, Mockito.times(1)).createResource(eq(ENODEB_EXTERNAL_ID), any());
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_OBJECT_COUNT), eq(Constants.MetricConstants.NCMP_OBJECT), eq(EXTERNALGNODEBFUNCTIONS));
    }

    @Test
    public void createExternalGNodeBFunctionFailed() {
        Mockito.when(ncmpService.getResource(any(ExternalId.class), eq(GUtraNetwork.class)))
            .thenReturn(Optional.of(toNcmpObject(G_UTRA_NETWORK)));
        Mockito.when(ncmpService.createResource(eq(GUTRA_NETWORK_EXTERNAL_ID), any()))
            .thenThrow(RestClientException.class);

        Assertions.assertEquals(Optional.empty(), externalNodeHandler.create(GUTRA_NETWORK_EXTERNAL_ID, GNBDU, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
        assertDoesNotThrow(() -> externalNodeHandler.create(GUTRA_NETWORK_EXTERNAL_ID, GNBDU, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
    }

    @Test
    public void createExternalGNodeBFunctionWithInvalidGnbdu() {
        Assertions.assertEquals(Optional.empty(), externalNodeHandler.create(GUTRA_NETWORK_EXTERNAL_ID, GNBDU_NO_WIRELESS_NETWORKS, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
        assertDoesNotThrow(() -> externalNodeHandler.create(GUTRA_NETWORK_EXTERNAL_ID, GNBDU_NO_WIRELESS_NETWORKS, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
    }
}
