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

package com.ericsson.oss.apps.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Constants {
    public static final String EQUAL = "=";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String QUOTE = "\"";
    public static final String BACKSLASH = "/";
    public static final String SEMI_COLON = ";";
    public static final String NULL = "null";
    public static final String SQUARE_BRACKET_OPEN = "[";
    public static final String SQUARE_BRACKET_CLOSE = "]";

    public static final String CTS = "cts";
    public static final String NCMP = "ncmp";

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String LOGIN = "login";
    public static final String PASSWORD = "password";
    public static final String TENANT = "tenant";
    public static final String SESSION_KEY = "JSESSIONID";
    public static final String TASK_QUEUE_IS_FULL = "TASK QUEUE IS FULL";

    public static final String LOADED = "LOADED";

    //QueryBuilderService param names
    public static final String TYPE = "type";
    public static final String POINT = "Point";
    public static final String COORDINATES = "coordinates";
    public static final String GEO_FREQUENCY = "downlinkEARFCN.in";
    public static final String GEO_QUERY_FILTER = "geographicSite.locatedAt.geospatialData.geoDistanceWithin";
    public static final String GEO_TYPE = "geographicSite.locatedAt.type.eq";

    //NCMP objects fields
    public static final String EXTERNAL_ID = "externalId";
    public static final String APPLICATION_YANG_DATA_JSON = "application/yang-data+json";
    public static final String ATTRIBUTES = "attributes";
    //GNBDUFunction
    public static final String DU_PLMN_ID = "dUpLMNId";
    //ExternalGNodeBFunction
    public static final String EXTERNAL_GNODEB_PLMN_ID = "gNodeBPlmnId";
    public static final String MCC = "mcc";
    public static final String MNC = "mnc";
    public static final String MNC_LENGTH = "mncLength";
    public static final String GNODEB_ID = "gNodeBId";
    public static final String GNODEB_ID_LENGTH = "gNodeBIdLength";
    public static final String EXTERNAL_GNODEB_FUNCTION_ID = "externalGNodeBFunctionId";
    //ExternalGuTranCell
    public static final String GUTRANSYNC_SIGNAL_FREQUENCY = "gUtranSyncSignalFrequencyRef";
    public static final String NRTAC = "nRTAC";
    public static final String EXTERNAL_GUTRAN_CELL_ID = "externalGUtranCellId";
    public static final String TRUE = "true";
    public static final String EXTERNAL_GUTRAN_CELL_PLMN_ID_LIST = "plmnIdList";
    //TermPointToGNB
    public static final String UNLOCKED = "UNLOCKED";
    public static final String GUTRA_NETWORK_ID = "gUtraNetworkId";
    //ResourcePartitionMember
    public static final String PLMN_ID_LIST = "pLMNIdList";
    //Additional log info counter names
    public static final String NRC_FOUND_NEIGHBOURING_NODES_COUNT = "nrc_found_neighbouring_nodes_count";
    public static final String NRC_FOUND_NEIGHBOURING_CELLS_COUNT = "nrc_found_neighbouring_cells_count";
    public static final String NCMP_MISSING_NEIGHBOURS_COUNT = "ncmp_missing_neighbours_count";
    public static final String NCMP_CREATED_OBJECT_COUNT = "ncmp_created_object_count";
    public static final String NCMP_EXTERNALGNODEBFUNCTIONS_OBJECT_COUNT = "ncmp_externalgnodebfunctions_object_count";
    public static final String NCMP_TERMPOINTTOGNB_OBJECT_COUNT = "ncmp_termpointtognb_object_count";
    public static final String NCMP_EXTERNALGUTRANCELL_OBJECT_COUNT = "ncmp_externalgutrancell_object_count";

    @UtilityClass
    public static final class MetricConstants {
        //prefix
        public static final String SERVICE_PREFIX = "5gcnr";

        //metric names
        public static final String NRC_THREADQUEUE_SIZE = SERVICE_PREFIX + "_threadqueue_size";
        public static final String NRC_THREADQUEUE_PENDING_SIZE = SERVICE_PREFIX + "_threadqueue_pending_size";
        public static final String NRC_THREADQUEUE_ONGOING_SIZE = SERVICE_PREFIX + "_threadqueue_ongoing_size";
        public static final String NRC_THREAD_QUEUE_FULL_COUNT = SERVICE_PREFIX + "_threadqueue_full_count";
        public static final String NRC_THREAD_QUEUE_IDLE_DURATION_SECONDS = SERVICE_PREFIX + "_threadqueue_idle_duration_seconds";
        public static final String NRC_HISTORY_HTTP_REQUEST_DURATION_SECONDS = SERVICE_PREFIX + "_history_http_request_duration_seconds";
        public static final String NRC_HTTP_REQUESTS = SERVICE_PREFIX + "_nrc_http_requests";
        public static final String NRC_REQUEST_SIZE = SERVICE_PREFIX + "_nrc_request_size";
        public static final String NRC_PROCESS_HTTP_REQUEST_DURATION_SECONDS = SERVICE_PREFIX + "_nrc_process_http_request_duration_seconds";
        public static final String NRC_REQUEST_COUNT = SERVICE_PREFIX + "_nrc_request_count";
        public static final String NRC_FOUND_NEIGHBOURING_NODES_COUNT = SERVICE_PREFIX + "_nrc_found_neighbouring_nodes_count";
        public static final String NRC_FOUND_NEIGHBOURING_CELLS_COUNT = SERVICE_PREFIX + "_nrc_found_neighbouring_cells_count";
        public static final String CTS_HTTP_REQUESTS = SERVICE_PREFIX + "_cts_http_requests";
        public static final String NCMP_HTTP_REQUESTS = SERVICE_PREFIX + "_ncmp_http_requests";
        public static final String NCMP_OBJECT_COUNT = SERVICE_PREFIX + "_ncmp_object_count";
        public static final String NCMP_MISSING_NEIGHBOURS_COUNT = SERVICE_PREFIX + "_ncmp_missing_neighbours_count";
        public static final String CTS_PROCESSING_HTTP_REQUEST_DURATION_SECONDS = SERVICE_PREFIX + "_cts_processing_http_request_duration_seconds";
        public static final String NCMP_PROCESSING_HTTP_REQUEST_DURATION_SECONDS = SERVICE_PREFIX + "_ncmp_processing_http_request_duration_seconds";
        public static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS = SERVICE_PREFIX + "_apigateway_sessionid_http_requests";
        public static final String APIGATEWAY_PROCESSING_HTTP_REQUEST_DURATION_SECONDS = SERVICE_PREFIX + "_apigateway_sessionid_processing_seconds";
        public static final String CACHE_SERVED_OBJECTS_REQUESTS_COUNT = SERVICE_PREFIX + "_cache_served_objects_requests_count";
        public static final String CACHE_SIZE = SERVICE_PREFIX + "_cache_size";
        public static final String RETRY_HTTP_REQUESTS = SERVICE_PREFIX + "_retry_http_requests";
        public static final String ENM_ADAPTER_OVERLOAD_RETRY_COUNT = SERVICE_PREFIX + "_enm_retry_overload_count";

        //tag Keys
        public static final String NRC_STATUS = "nrc_status";
        public static final String HTTP_STATUS = "http_status";
        public static final String ENDPOINT = "endpoint";
        public static final String METHOD = "method";
        public static final String UNIQUE_APP_ID = "uniqueAppId";
        public static final String INSTANCE_ID = "instance";
        public static final String SERVICE = "service";
        public static final String TAG = "tag";
        public static final String CTS_TAG = SQUARE_BRACKET_OPEN + SERVICE + EQUAL + CTS + SQUARE_BRACKET_CLOSE;
        public static final String NCMP_TAG = SQUARE_BRACKET_OPEN + SERVICE + EQUAL + NCMP + SQUARE_BRACKET_CLOSE;
        public static final String DESCRIPTION = "description";
        public static final String REGISTER = "register";
        public static final String NCMP_OBJECT = "ncmp_object";
        public static final String TERMPOINTTOGNB = "termpointtognb";
        public static final String EXTERNALGUTRANCELL = "externalgutrancell";
        public static final String EXTERNALGNODEBFUNCTIONS = "externalgnodebfunctions";
        public static final String OBJECT_TYPE = "object_type";
        public static final String CAUSE = "cause";

        //tag values
        public static final String POST = "post";
        public static final String GET = "get";
        public static final String START_NRC = "startNrc";
        public static final String MONITORING = "monitoring";
        public static final String MONITORING_BY_ID = MONITORING + "/{id}";
        public static final String CONGESTION = "congestion";

        //metric description
        public static final Map<String, String> METRIC_DESCRIPTIONS =
            Map.ofEntries(
                Map.entry(NRC_THREAD_QUEUE_FULL_COUNT, "The number of rejected NRC requests because of thread queue full."),
                Map.entry(NRC_HISTORY_HTTP_REQUEST_DURATION_SECONDS, "The time elapsed between the insertion and " +
                    "removal of an NRC request in history."),
                Map.entry(NRC_REQUEST_SIZE, "The number of EnodeB IDs in the recently received NRC request."),
                Map.entry(NRC_PROCESS_HTTP_REQUEST_DURATION_SECONDS, "The time elapsed between request " +
                    "and completion of the just finished NRC request."),
                Map.entry(NRC_REQUEST_COUNT, "The number of finished nrc requests."),
                Map.entry(CTS_HTTP_REQUESTS, "The number of CTS requests."),
                Map.entry(NRC_HTTP_REQUESTS, "The number of Nrc requests; the number of startNrc and monitoring endpoints http calls."),
                Map.entry(NCMP_HTTP_REQUESTS, "The number of NCMP requests."),
                Map.entry(CTS_PROCESSING_HTTP_REQUEST_DURATION_SECONDS, "Time spent in CTS."),
                Map.entry(NCMP_PROCESSING_HTTP_REQUEST_DURATION_SECONDS, "Time spent in NCMP."),
                Map.entry(NCMP_OBJECT_COUNT, "The number of NCMP created objects."),
                Map.entry(NCMP_MISSING_NEIGHBOURS_COUNT, "The number of missing neighbors."),
                Map.entry(NRC_THREAD_QUEUE_IDLE_DURATION_SECONDS, "The time elapsed when the current NRC request is successful until the new NRC request."),
                Map.entry(NRC_FOUND_NEIGHBOURING_CELLS_COUNT, "The number of NRC neighboring cells."),
                Map.entry(NRC_FOUND_NEIGHBOURING_NODES_COUNT, "The number of NRC neighboring nodes."),
                Map.entry(NRC_THREADQUEUE_SIZE, "The number of NRC tasks in same time that are waiting for completion."),
                Map.entry(NRC_THREADQUEUE_PENDING_SIZE, "The number of pending NRC tasks in same time that are waiting to be in ongoing phase."),
                Map.entry(NRC_THREADQUEUE_ONGOING_SIZE, "The number of ongoing NRC tasks in same time that are waiting to finalized."),
                Map.entry(APIGATEWAY_SESSIONID_HTTP_REQUESTS, "The number of session ID requested from the API Gateway."),
                Map.entry(APIGATEWAY_PROCESSING_HTTP_REQUEST_DURATION_SECONDS, "Time spent in the API Gateway."),
                Map.entry(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, "The number of requests served from the cache."),
                Map.entry(CACHE_SIZE, "The number of objects that are currently in the cache."),
                Map.entry(RETRY_HTTP_REQUESTS, "The number of CTS and NCMP requests retries performed."),
                Map.entry(ENM_ADAPTER_OVERLOAD_RETRY_COUNT, "The number of CTS and NCMP retries requests when ENM " +
                    "adapter is overloaded.")
            );

        public static final Map<String, List<String>> REQUIRED_TAG_KEYS =
            Map.ofEntries(
                Map.entry(NRC_HTTP_REQUESTS, Arrays.asList(ENDPOINT, METHOD, HTTP_STATUS)),
                Map.entry(CTS_HTTP_REQUESTS, Arrays.asList(HTTP_STATUS)),
                Map.entry(NCMP_HTTP_REQUESTS, Arrays.asList(HTTP_STATUS)),
                Map.entry(APIGATEWAY_SESSIONID_HTTP_REQUESTS, Arrays.asList(HTTP_STATUS)),
                Map.entry(CACHE_SERVED_OBJECTS_REQUESTS_COUNT, Arrays.asList(SERVICE)),
                Map.entry(CACHE_SIZE, Arrays.asList(SERVICE)),
                Map.entry(RETRY_HTTP_REQUESTS, Arrays.asList(OBJECT_TYPE))
            );
    }

    @UtilityClass
    public static final class LoggingConstants {
        public static final String FACILITY_KEY = "facility";
        public static final String SUBJECT_KEY = "subject";
        public static final String RESP_MESSAGE_KEY = "resp_message";
        public static final String RESP_CODE_KEY = "resp_code";
        public static final String AUDIT_LOG = "log audit";
        public static final String NON_AUDIT_LOG = "security/authorization messages";
        public enum SupportedLogLevel {INFO, DEBUG, WARNING, ERROR, CRITICAL}
    }
}
