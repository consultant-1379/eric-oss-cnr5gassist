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

package com.ericsson.oss.apps.config;

import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.client.cts.model.*;
import com.ericsson.oss.apps.exception.GraniteFaultException;
import com.ericsson.oss.apps.model.GeoPoint;
import com.ericsson.oss.apps.model.PciConflict;
import com.ericsson.oss.apps.service.nrc.PciConflictDetector;
import com.ericsson.oss.apps.util.CtsUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.TestDefaults.GEO_POINT;
import static com.ericsson.oss.apps.util.TestDefaults.LTE_CELL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ClientAspectsTest {

    private static final String TEXT = "TEXT";

    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RetryTemplate retryTemplate;
    @InjectMocks
    private ClientAspects clientAspects;

    @Test
    void pointCuts() {
        List.of("apiClient", "ctsService", "getFilteredNeighbourNrCellsWithAssoc",
            "getLteCellWithGeographicSitesAssoc", "getGeographicSiteWithGeographicLocationsAssoc").forEach(m -> {
            assertThrows(UnsupportedOperationException.class, () -> ReflectionTestUtils.invokeMethod(clientAspects, m));
        });
    }

    @Test
    void missingGeoSiteField() {
        try (MockedStatic<CtsUtils> utils = Mockito.mockStatic(CtsUtils.class)) {
            LteCell lteCell = Mockito.mock(LteCell.class);
            AtomicInteger times = new AtomicInteger();

            List.of(Stream.of(LTE_CELL), Stream.<LteCell>empty()).forEach(returnValue -> {
                utils.when(() -> CtsUtils.getSites(Mockito.any())).thenReturn(returnValue);
                clientAspects.logMissingGeoSiteField(lteCell);

                Mockito.verify(lteCell, Mockito.times(times.get())).getId();
                Mockito.verify(lteCell, Mockito.times(times.get())).getType();
                times.getAndIncrement();
            });
        }
    }

    @Test
    void missingGeoLocationField() {
        try (MockedStatic<CtsUtils> utils = Mockito.mockStatic(CtsUtils.class)) {
            GeographicSite site = Mockito.mock(GeographicSite.class);
            AtomicInteger times = new AtomicInteger();

            List.of(Stream.of(GEO_POINT), Stream.<GeoPoint>empty()).forEach(returnValue -> {
                utils.when(() -> CtsUtils.getGeoPoints(Mockito.any())).thenReturn(returnValue);
                clientAspects.logMissingGeoLocation(site);

                Mockito.verify(site, Mockito.times(times.get())).getId();
                Mockito.verify(site, Mockito.times(times.get())).getType();
                times.getAndIncrement();
            });
        }
    }

    @Test
    void restTemplateRetry() throws Throwable {
        Mockito.when(pjp.proceed()).thenReturn(TEXT);
        Mockito.when(retryTemplate.execute(Mockito.any())).thenAnswer(i -> {
            RetryCallback<Runnable, Throwable> retryCallback = i.getArgument(0);
            return retryCallback.doWithRetry(Mockito.mock(RetryContext.class));
        });

        assertEquals(TEXT, clientAspects.handleRestTemplateRetry(pjp));
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class NeighbouringNrCellAnomaliesTest {

        private final Consumer<NrCell> DOWNLINK_SETTER = n -> n.setDownlinkEARFCN(1);
        private final Consumer<NrCell> PCI_SETTER = n -> n.setPhysicalCellIdentity(2);
        private final List<Consumer<NrCell>> NRCELL_SETTERS = Arrays.asList(DOWNLINK_SETTER.andThen(PCI_SETTER),
            DOWNLINK_SETTER, PCI_SETTER, n -> {});

        @Mock
        private PciConflict pciConflict;
        @Mock
        private JoinPoint jp;
        @Mock
        private NrcRequest nrcRequest;
        @Mock
        private ENodeB eNodeB;

        @BeforeEach
        void setup() {
            Object[] args = {nrcRequest, eNodeB};
            Mockito.when(jp.getArgs()).thenReturn(args);
        }

        private List<NrCell> generateNrCells(int size) {
            return IntStream.range(0, size).mapToObj(i -> {
                NrCell nrCell = NrCell.builder().id((long) i).build();
                NRCELL_SETTERS.get(i % NRCELL_SETTERS.size()).accept(nrCell);
                return nrCell;
            }).collect(Collectors.toList());
        }

        @Test
        void noNeighbouringNrCells() {
            clientAspects.logNeighbouringNrCellAnomalies(jp, generateNrCells(0));

            Mockito.verify(nrcRequest, Mockito.times(1)).getDistance();
            Mockito.verify(eNodeB, Mockito.times(1)).getId();
        }

        @Test
        void pciConflict() {
            try (MockedStatic<PciConflictDetector> pciConflictDetector = Mockito.mockStatic(PciConflictDetector.class)) {
                List<NrCell> nrCells = generateNrCells(5);
                List<NrCell> pciConflictArgument = List.of(nrCells.get(0), nrCells.get(4));
                pciConflictDetector.when(() -> PciConflictDetector.detectPCIConflicts(Mockito.any()))
                    .thenReturn(Map.of(pciConflict, List.of(0, 4)));

                clientAspects.logNeighbouringNrCellAnomalies(jp, nrCells);
                pciConflictDetector.verify(() -> PciConflictDetector.detectPCIConflicts(Mockito.eq(pciConflictArgument)),
                    Mockito.times(1));

                Mockito.verify(pciConflict, Mockito.times(1)).getDownlinkEARFCN();
                Mockito.verify(pciConflict, Mockito.times(1)).getPhysicalCellIdentity();
            }
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CtsExceptionHandlingTest {

        private final HttpClientErrorException EXCEPTION = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        @Mock
        private Signature signature;
        @Mock
        private GraniteFaultStack graniteFaultStack;
        @Mock
        private JsonProcessingException jsonProcessingException;

        @BeforeEach
        void setup() {
            Mockito.when(pjp.getSignature()).thenReturn(signature);
            Mockito.when(signature.getName()).thenReturn(TEXT);
        }

        @Test
        void proceeding() throws Throwable {
            Mockito.when(pjp.proceed()).thenReturn(TEXT);
            assertEquals(TEXT, clientAspects.handleCtsException(pjp));
            Mockito.verify(pjp, Mockito.times(1)).proceed();
        }

        @Test
        void exceptionGranitFault() throws Throwable {
            Mockito.when(pjp.proceed()).thenThrow(EXCEPTION);
            Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(GraniteFaultStack.class)))
                .thenReturn(graniteFaultStack);

            assertThrows(GraniteFaultException.class, () -> clientAspects.handleCtsException(pjp));
        }

        @Test
        void exceptionJsonProcessing() throws Throwable {
            Mockito.when(pjp.proceed()).thenThrow(EXCEPTION);
            Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(GraniteFaultStack.class)))
                .thenThrow(jsonProcessingException);

            assertThrows(HttpClientErrorException.class, () -> clientAspects.handleCtsException(pjp));
        }
    }
}
