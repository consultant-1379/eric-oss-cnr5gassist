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

import com.ericsson.oss.apps.api.model.ErrorResponse;
import com.ericsson.oss.apps.service.MetricService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ericsson.oss.apps.util.Constants.MetricConstants.*;

@Slf4j
@ControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MetricService metricService;

    @NonNull
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> customizeStatusExceptionHandling(
        @NonNull ResponseStatusException ex,
        WebRequest request
    ) {
        Map<String, Object> body = new HashMap<>();

        if (ex.getReason() != null && !Objects.equals(ex.getReason(), "")) {
            body.put("message", (ex.getReason()));
        }
        return handleExceptionInternal(ex, body, HttpHeaders.EMPTY, ex.getStatusCode(), request);
    }

    @NonNull
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", (ex.getMessage()));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @NonNull
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("errors", (
                ex.getBindingResult().getFieldErrors().stream()
                    .collect(Collectors.groupingBy(FieldError::getField, Collectors.mapping(
                        FieldError::getDefaultMessage, Collectors.toList())))
            )
        );
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @NonNull
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body,
                                                             HttpHeaders headers, @NonNull HttpStatusCode status,
                                                             @NonNull WebRequest request) {
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute("javax.servlet.error.exception", ex, 0);
        }
        String requestUri = ((ServletWebRequest) request).getRequest().getRequestURI();
        if (status.is4xxClientError()) {
            if (requestUri.contains("/api/v1/nrc/startNrc")) {
                metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, START_NRC, METHOD, POST,
                    HTTP_STATUS, String.valueOf(status.value()));
            } else if (requestUri.contains("/api/v1/nrc/monitoring")) {
                metricService.increment(NRC_HTTP_REQUESTS, ENDPOINT, MONITORING, METHOD, GET,
                    HTTP_STATUS, String.valueOf(status.value()));
            }
        }
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp((LocalDateTime.now()).atOffset(ZoneOffset.UTC))
            .error(((HttpStatus) status).getReasonPhrase())
            .status(status.value())
            .path(requestUri)
            .body(body)
            .build();
        return new ResponseEntity(errorResponse, headers, status);
    }
}