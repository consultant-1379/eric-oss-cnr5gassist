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
import com.ericsson.oss.apps.client.ApiClient;
import com.ericsson.oss.apps.client.cts.model.*;
import com.ericsson.oss.apps.exception.EnmAdapterOverloadedException;
import com.ericsson.oss.apps.exception.GraniteFaultException;
import com.ericsson.oss.apps.model.GeoQueryObject;
import com.ericsson.oss.apps.model.PciConflict;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.nrc.PciConflictDetector;
import com.ericsson.oss.apps.util.CtsUtils;
import com.ericsson.oss.apps.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.oss.apps.util.Constants.CTS;
import static com.ericsson.oss.apps.util.Constants.LoggingConstants.*;
import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;
import static com.ericsson.oss.apps.util.Constants.NCMP;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Slf4j
@Aspect
@Configuration
@AllArgsConstructor
public class ClientAspects {

    private static final Predicate<Object[]> isApiGatewayCall = args ->
            args != null && args.length > 0 && args[0].equals("/auth/v1/login");

    @Autowired
    private ObjectMapper objectMapper;
    @Resource(name = "restTemplateRetry")
    private RetryTemplate restTemplateRetry;

    private final MetricService metricService;
    private static final List<String> OBJECT_METHODS = Arrays.stream(Object.class.getDeclaredMethods())
        .map(Method::getName).collect(toList());

    @Pointcut("execution(* com.ericsson.oss.apps.client.ApiClient.invokeAPI(..))")
    private void apiClient() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("within(com.ericsson.oss.apps.service.CtsService)")
    private void ctsService() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.service.nrc.NeighbouringCellService.getFilteredNeighbourNrCellsWithAssoc(..))")
    private void getFilteredNeighbourNrCellsWithAssoc() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.service.CtsService.getLteCellWithGeographicSitesAssoc(..))")
    private void getLteCellWithGeographicSitesAssoc() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.service.CtsService.getGeographicSiteWithGeographicLocationsAssoc(..))")
    private void getGeographicSiteWithGeographicLocationsAssoc() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.service.CtsService.*(..))")
    private void allCtsMethods() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.service.NcmpService.*(..))")
    private void allNcmpMethods() {
        throw new UnsupportedOperationException();
    }

    @Pointcut("execution(* com.ericsson.oss.apps.client.gw.GatewayServiceApi.login(..))")
    private void apiGatewayLogin() {
        throw new UnsupportedOperationException();
    }

    @AfterReturning(pointcut = "getLteCellWithGeographicSitesAssoc()", returning = "lteCell")
    public void logMissingGeoSiteField(LteCell lteCell) {
        if (CtsUtils.getSites(lteCell).findAny().isEmpty()) {
            log.warn("The following {} id: {} has an empty GeographicSiteList field", lteCell.getType(), lteCell.getId());
        }
    }

    @AfterReturning(pointcut = "getGeographicSiteWithGeographicLocationsAssoc()", returning = "site")
    public void logMissingGeoLocation(GeographicSite site) {
        if (CtsUtils.getGeoPoints(site).findAny().isEmpty()) {
            log.warn("The following {} id: {} is missing GeoLocation information", site.getType(), site.getId());
        }
    }

    @AfterReturning(pointcut = "getFilteredNeighbourNrCellsWithAssoc()", returning = "filteredNrCells")
    public void logNeighbouringNrCellAnomalies(JoinPoint jp, List<NrCell> filteredNrCells) {
        logNoNeighbouringNrCells(jp, filteredNrCells);
        Map<Boolean, List<NrCell>> groupedNrCells = filteredNrCells.stream()
            .collect(groupingBy(x -> x.getDownlinkEARFCN() == null || x.getPhysicalCellIdentity() == null));
        logMissingPciRelatedNrCellFields(groupedNrCells.getOrDefault(true, Collections.emptyList()));
        logPciConflict(groupedNrCells.getOrDefault(false, Collections.emptyList()));
    }

    private void logNoNeighbouringNrCells(JoinPoint jp, List<NrCell> filteredNrCells) {
        Object[] args = jp.getArgs();
        NrcRequest nrcRequest = (NrcRequest) args[0];
        ENodeB eNodeB = (ENodeB) args[1];

        if (filteredNrCells.isEmpty()) {
            log.warn("No neighbouring NrCells were found for eNodeB(id={}) within: {} [m]", eNodeB.getId(), nrcRequest.getDistance());
        }
    }

    private void logMissingPciRelatedNrCellFields(List<NrCell> missingFieldNrCells) {
        List<Long> idsWithNull = missingFieldNrCells.stream().map(NrCell::getId).collect(toList());

        if (!idsWithNull.isEmpty()) {
            log.warn("The following NrCell id(s): {} have empty PCI or DownlinkEARFCN fields", idsWithNull);
        }
    }

    private void logPciConflict(List<NrCell> havingPciFieldNrCells) {
        Map<PciConflict, List<Long>> pciConflicts = PciConflictDetector.detectPCIConflicts(havingPciFieldNrCells);

        pciConflicts.forEach((key, value) ->
            log.warn("There are conflicting nrCells with the following ids: {} on the {} PhysicalCellIdentity and {} DownlinkEARFCN range",
                value, key.getPhysicalCellIdentity(), key.getDownlinkEARFCN())
        );
    }

    @Around("ctsService()")
    public Object handleCtsException(ProceedingJoinPoint pjp) throws Throwable {
        String taskTitle = pjp.getSignature().getName();
        log.info("CTS: {}, Args: {}", taskTitle, Arrays.deepToString(pjp.getArgs()));
        try {
            return pjp.proceed();
        } catch (HttpClientErrorException clientException) {
            log.error(String.format("CTS: Failed to %s", taskTitle));
            try {
                GraniteFaultStack faultStack = objectMapper.readValue(
                    clientException.getResponseBodyAsString(), GraniteFaultStack.class);
                throw new GraniteFaultException(faultStack);
            } catch (JsonProcessingException e) {
                log.error("CTS: ClientError deserialization failed");
                try {
                    log.error("JsonProcessingException raised : ", e);
                    throw clientException;
                } catch (Exception ex) {
                    log.info("Log failed printing JsonProcessingException and causes a NullPointerException error");
                    log.error("Log exception NPE : ", ex);
                    throw clientException;
                }
            }
        }
    }

    @Around("apiClient()")
    public Object handleRestTemplateRetry(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 6) {
            if (isApiGatewayCall.test(args)) {
                MDC.put(FACILITY_KEY, AUDIT_LOG);
                MDC.put(SUBJECT_KEY, SERVICE_PREFIX.toUpperCase(Locale.US));
            }
            log.info("apiClient.invokeAPI {}: path: {}{} params: {}", args[1], ((ApiClient) joinPoint.getTarget()).getBasePath(),
                args[0], Stream.of(args).skip(2).limit(4).collect(Collectors.toList()));
            if (isApiGatewayCall.test(args)) {
                MDC.remove(FACILITY_KEY);
                MDC.remove(SUBJECT_KEY);
            }
        }
        return restTemplateRetry.execute(retryContext -> joinPoint.proceed());
    }

    @AfterReturning(pointcut = "apiClient()", returning = "result")
    public void logResult(JoinPoint joinPoint, ResponseEntity result) {
        Object[] args = joinPoint.getArgs();
        if (isApiGatewayCall.test(args)) {
            MDC.put(FACILITY_KEY, AUDIT_LOG);
            MDC.put(SUBJECT_KEY, SERVICE_PREFIX.toUpperCase(Locale.US));
            MDC.put(RESP_MESSAGE_KEY, result.toString());
            MDC.put(RESP_CODE_KEY, result.getStatusCode().toString());
        }
        log.info("apiClient.invokeAPI {}: result: {} path: {}{} params: {} response: {}",
            args[1], result.getStatusCode(),
            ((ApiClient) joinPoint.getTarget()).getBasePath(), args[0],
            Stream.of(args).skip(2).limit(4).collect(Collectors.toList()),
            result.getBody() == null ? result.toString() : StringUtil.toJson(result.getBody()));
        if (isApiGatewayCall.test(args)) {
            MDC.remove(FACILITY_KEY);
            MDC.remove(SUBJECT_KEY);
            MDC.remove(RESP_MESSAGE_KEY);
            MDC.remove(RESP_CODE_KEY);
        }
    }

    @Before("allCtsMethods()")
    public void measureCtsRequestBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        if (!OBJECT_METHODS.contains(methodName) &&
            !(methodName.equals("getNrCellWithFilters") &&
            joinPoint.getArgs()[0].getClass().equals(GeoQueryObject.class))) {
            metricService.startTimer(String.valueOf(joinPoint.hashCode()), CTS_PROCESSING_HTTP_REQUEST_DURATION_SECONDS);
        }
    }

    @After("allCtsMethods()")
    public void measureCtsRequestAfter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        if (!OBJECT_METHODS.contains(methodName) &&
            !(methodName.equals("getNrCellWithFilters") &&
            joinPoint.getArgs()[0].getClass().equals(GeoQueryObject.class))) {
            metricService.stopTimer(String.valueOf(joinPoint.hashCode()), CTS_PROCESSING_HTTP_REQUEST_DURATION_SECONDS);
        }
    }

    @AfterReturning("allCtsMethods()")
    public void measureCtsRequestAfterReturning(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        if (!OBJECT_METHODS.contains(methodName)) {
            countHttpOks(CTS_HTTP_REQUESTS);
        }
    }

    @AfterThrowing(pointcut = "allCtsMethods()", throwing = "error")
    public void measureCtsRequestAfterThrowing(JoinPoint joinPoint, RestClientException error) {
        String methodName = joinPoint.getSignature().getName();
        if (!OBJECT_METHODS.contains(methodName)) {
            log.error("CTS request failed");
            countHttpErrors(CTS_HTTP_REQUESTS, error);
        }
    }

    @Before("allNcmpMethods()")
    public void measureNcmpRequestBefore(JoinPoint joinPoint) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            metricService.startTimer(String.valueOf(joinPoint.hashCode()), NCMP_PROCESSING_HTTP_REQUEST_DURATION_SECONDS);
        }
    }

    @After("allNcmpMethods()")
    public void measureNcmpRequestAfter(JoinPoint joinPoint) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            metricService.stopTimer(String.valueOf(joinPoint.hashCode()), NCMP_PROCESSING_HTTP_REQUEST_DURATION_SECONDS);
        }
    }

    @AfterReturning("allNcmpMethods()")
    public void measureNcmpRequestAfterReturning(JoinPoint joinPoint) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            countHttpOks(NCMP_HTTP_REQUESTS);
        }
    }

    @AfterThrowing(pointcut = "allNcmpMethods()", throwing = "error")
    public void measureNcmpRequestAfterThrowing(JoinPoint joinPoint, RestClientException error) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            log.error("NCMP request failed");
            countHttpErrors(NCMP_HTTP_REQUESTS, error);
        }
    }

    @Before("apiGatewayLogin()")
    public void measureApiGatewayLoginRequestBefore(JoinPoint joinPoint) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            metricService.startTimer(String.valueOf(joinPoint.hashCode()), APIGATEWAY_PROCESSING_HTTP_REQUEST_DURATION_SECONDS);
        }
    }

    @After("apiGatewayLogin()")
    public void measureApiGatewayLoginRequestAfter(JoinPoint joinPoint) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            metricService.stopTimer(String.valueOf(joinPoint.hashCode()), APIGATEWAY_PROCESSING_HTTP_REQUEST_DURATION_SECONDS);
        }
    }

    @AfterReturning("apiGatewayLogin()")
    public void measureApiGatewayLoginRequestAfterReturning(JoinPoint joinPoint) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            countHttpOks(APIGATEWAY_SESSIONID_HTTP_REQUESTS);
        }
    }

    @AfterThrowing(pointcut = "apiGatewayLogin()", throwing = "error")
    public void measureApiGatewayLoginRequestAfterThrowing(JoinPoint joinPoint, RestClientException error) {
        if (!OBJECT_METHODS.contains(joinPoint.getSignature().getName())) {
            log.error("ApiGateway get sessionId request failed");
            countHttpErrors(APIGATEWAY_SESSIONID_HTTP_REQUESTS, error);
        }
    }

    @AfterThrowing(pointcut = "apiClient()", throwing = "error")
    public void overLoadRetryMetric(JoinPoint joinPoint, Exception error) {
        if (error instanceof ResourceAccessException) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String objectType = ((String) args[0]).toLowerCase(Locale.US);
                if (objectType.contains("ctg") || objectType.contains("ctw")) {
                    metricService.increment(RETRY_HTTP_REQUESTS, OBJECT_TYPE, CTS);
                } else if (objectType.contains("ncmp")) {
                    metricService.increment(RETRY_HTTP_REQUESTS, OBJECT_TYPE, NCMP);
                }
            }
        } else if (error instanceof EnmAdapterOverloadedException) {
            metricService.increment(ENM_ADAPTER_OVERLOAD_RETRY_COUNT);
        }
    }

    private void countHttpOks(String metricName) {
        // The 2xx http response statuses are not differentiated
        metricService.increment(metricName, HTTP_STATUS, String.valueOf(HttpStatus.OK.value()));
    }

    private void countHttpErrors(String metricName, Exception error) {
        // The 3xx response statuses are not counted
        if (error instanceof HttpStatusCodeException) {
            // For handling 4xx and 5xx response statuses
            metricService.increment(metricName, HTTP_STATUS, String.valueOf(((HttpStatusCodeException) error)
                .getStatusCode().value()));
        }
    }
}
