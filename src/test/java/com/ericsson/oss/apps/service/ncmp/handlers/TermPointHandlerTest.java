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
import com.ericsson.oss.apps.model.ncmp.TermPointToGNB;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.NcmpService;
import com.ericsson.oss.apps.service.ncmp.EnmUpdateMonitorService;
import com.ericsson.oss.apps.service.ncmp.IpAddressFinder;
import com.ericsson.oss.apps.util.Constants;
import javassist.tools.rmi.ObjectNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class TermPointHandlerTest {

    private static final NcmpObject<TermPointToGNB> NCMP_OBJECT = toNcmpObject("1", TERM_POINT_TO_GNB);

    @Mock
    private NcmpService ncmpService;
    @Mock
    private MetricService metricService;
    @Mock
    private IpAddressFinder ipAddressFinder;
    @Mock
    private EnmUpdateMonitorService enmUpdateMonitorService;
    @InjectMocks
    private TermPointHandler termPointHandler;

    @Test
    void readTermPoint() {
        Mockito.when(ncmpService.getResources(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), eq(TermPointToGNB.class)))
            .thenReturn(List.of(NCMP_OBJECT));
        Assertions.assertEquals(Optional.of(TERM_POINT_EXTERNAL_ID), termPointHandler.read(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID));
    }

    @Test
    void createTermPointSuccessfulTest() throws ObjectNotFoundException {
        Mockito.when(ipAddressFinder.getIpForManagedElement(eq(GNODEB_MANAGED_ELEMENT_EXTERNAL_ID), eq(GNBDU_FUNCTION))).thenReturn(STRIPPED_IPv4_ADDRESS);
        Mockito.when(ncmpService.createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any())).thenReturn(new ResponseEntity<>(HttpStatus.CREATED));
        Mockito.doNothing().when(enmUpdateMonitorService).updateSucceeded(any(), any());
        Assertions.assertEquals(Optional.of(toNcmpObject("1", TERM_POINT_TO_GNB)), termPointHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, GNODEB_MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
        Mockito.verify(ipAddressFinder, Mockito.times(1)).getIpForManagedElement(eq(GNODEB_MANAGED_ELEMENT_EXTERNAL_ID), eq(GNBDU_FUNCTION));
        Mockito.verify(ncmpService, Mockito.times(1)).createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any());
        Mockito.verify(metricService, Mockito.times(1)).increment(eq(NCMP_OBJECT_COUNT), eq(Constants.MetricConstants.NCMP_OBJECT), eq(TERMPOINTTOGNB));
    }

    @Test
    void createTermPointNoIpAddressTest() throws ObjectNotFoundException {
        Mockito.when(ipAddressFinder.getIpForManagedElement(any(), any())).thenThrow(ObjectNotFoundException.class);
        Mockito.when(ncmpService.createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any())).thenReturn(new ResponseEntity<>(HttpStatus.CREATED));
        Mockito.doNothing().when(enmUpdateMonitorService).updateSucceeded(any(), any());
        Assertions.assertEquals(Optional.of(toNcmpObject("1", TermPointToGNB.builder().termPointToGNBId("1").build())), termPointHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, GNODEB_MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
        assertDoesNotThrow(() -> termPointHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, GNODEB_MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));

        Mockito.verify(ipAddressFinder, Mockito.times(2)).getIpForManagedElement(eq(GNODEB_MANAGED_ELEMENT_EXTERNAL_ID), eq(GNBDU_FUNCTION));
        Mockito.verify(ncmpService, Mockito.times(2)).createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any());
    }

    @Test
    void createTermPointFailTest() throws RestClientException, ObjectNotFoundException {
        Mockito.when(ipAddressFinder.getIpForManagedElement(any(), any())).thenThrow(ObjectNotFoundException.class);
        Mockito.when(ncmpService.createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any())).thenThrow(RestClientException.class);
        Mockito.doNothing().when(enmUpdateMonitorService).updateFailed(any(), any(),any());
        Assertions.assertEquals(Optional.empty(), termPointHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, GNODEB_MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));
        assertDoesNotThrow(() -> termPointHandler.create(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID, GNODEB_MANAGED_ELEMENT_EXTERNAL_ID, GNBDU_FUNCTION, ENM_UPDATE_CONTEXT));

        Mockito.verify(ipAddressFinder, Mockito.times(2)).getIpForManagedElement(eq(GNODEB_MANAGED_ELEMENT_EXTERNAL_ID), eq(GNBDU_FUNCTION));
        Mockito.verify(ncmpService, Mockito.times(2)).createResource(eq(EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID), any());
    }
}
