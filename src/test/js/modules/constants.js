/*
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
 */

export const get = 'Get: ';
export const post = 'Post: ';
export const healthUri = '/actuator/health';
export const prometheusUri = '/actuator/prometheus';
export const metricsUri = '/actuator/metrics';
export const nrcBaseUri = '/api/v1/nrc';
export const monitoringUri = nrcBaseUri.concat('/monitoring');
export const startNrcUri = nrcBaseUri.concat('/startNrc');

export const defaultTimeout = 60;
export const defaultSleepTime = 5;
export const maxRetry = 10;

export const defaultE2EOptions = {
    duration: '30m',
    vus: 1,
    iterations: 1,
    thresholds: {
        checks: ['rate == 1.0']
    }
};

export const ingressSchema = __ENV.INGRESS_SCHEMA ? __ENV.INGRESS_SCHEMA : 'https';
export const ingressHost = __ENV.APIGW_HOST ? __ENV.APIGW_HOST : 'th.stsvp1eic26.stsoss.sero.gic.ericsson.se';
export const ingressUrl = ingressSchema.concat('://').concat(ingressHost);
export const ingressLoginUri = '/auth/v1/login';
export const ingressLoginUser = __ENV.INGRESS_USER ? __ENV.INGRESS_USER : 'cnr-user';
export const ingressLoginPassword = __ENV.INGRESS_PASSWORD ? __ENV.INGRESS_PASSWORD : 'idunEr!css0n';
export const ingressLoginParams = {
    headers: {
        'X-Login': ingressLoginUser,
        'X-password': ingressLoginPassword,
        'X-tenant': 'master',
        'Content-Type': 'application/x-www-form-urlencoded'
    }
};

export const sessionIdAccess = __ENV.STAGING_LEVEL === 'PRODUCT' ? true :
    __ENV.SESSION_ID_ACCESS === 'true' ? true : false;

export const serviceSchema = __ENV.SERVICE_SCHEMA ? __ENV.SERVICE_SCHEMA : 'http';
export const serviceHost = __ENV.SERVICE_HOST ? __ENV.SERVICE_HOST : 'localhost';
export const servicePort = __ENV.SERVICE_PORT ? __ENV.SERVICE_PORT : '8080';

export const serviceUrl = __ENV.STAGING_LEVEL === 'PRODUCT' ? ingressUrl :
    __ENV.SERVICE_URL ? __ENV.SERVICE_URL : serviceSchema.concat('://').concat(serviceHost).concat(':').concat(servicePort);

export const serviceRestUri = __ENV.STAGING_LEVEL === 'PRODUCT' ? '/cnr' :
    __ENV.SERVICE_REST_URI ? __ENV.SERVICE_REST_URI : '';

export const nrcRequestCountMetric = '/5gcnr_nrc_request_count';
export const monitoringRequestCountMetric = '/5gcnr_monitoring_request_count';