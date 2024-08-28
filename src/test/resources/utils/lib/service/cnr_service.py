#!/usr/bin/env python3
#
# COPYRIGHT Ericsson 2021
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# -*- coding: utf-8 -*-
"""
This module covers 5G CNR rApp REST API client functions
For more information check this link:
https://adp.ericsson.se/marketplace/5g-cnr-assist/documentation/1.1.7-1/dpi/api-documentation
"""
import json
import logging
import logging.handlers
import time
from lib.service.commons import POST, GET
from lib.service.eiap_api_gw import EIAP, EIAPConfig

class CNR():
    """!@class CTS
        @brief Class representing an EAIP API rest call object
        @details This is a class that represents EIAP API rest call object with
        specific functions to initiate API calls and process responses
    """
    SUCCEEDED    = "Succeeded"
    COMPLETED    = "Completed"
    FAILED       = "Failed"

    def __init__(self, cfg, user, apigw = EIAPConfig.APIGW,
                     eiap = None, endpoint_tag = EIAPConfig.CNR):
        '''
        @brief Object constructor
        :param: cfg: the configuration containing the needed parameters
        :param: apigw: the prefix used for identifying the proper host connection params
        :param: user: the prefix used for identifying the proper user params
        :param: eiap: the EIAP objects already instantiated
        :param: endpoint_tag: the endpoint tag for identifying service endpoints
        '''
        if eiap:
            self.eiap = eiap
        else:
            self.eiap = EIAP(cfg, apigw, user)
        self.host = cfg.get_host(apigw)
        endpoints = cfg.cfg['generic_endpoints'][endpoint_tag]
        self.monitor_url  = f'https://{self.host}'+endpoints['monitor']
        self.startnrc_url = f'https://{self.host}'+endpoints['startNrc']
        self.health_url   = f'https://{self.host}'+endpoints['health']

        self.header = {"Content-Type": "application/json"}
        self.header["Accept"] = "application/json"


    def start_nrc(self, payload, refresh_auth = False):
        '''
        @brief Call CNR REST endpoint to start NRC request
        :param: payload: the payload containing the request
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        return self.eiap.eiap_rest_call(POST, self.startnrc_url, inputs=json.dumps(payload),
                    refresh_auth=refresh_auth)

    def get_req_status(self, req_id, status_strings):
        '''
        @brief Call CNR REST endpoint to query the request status
        :param: req_id: the id of the request
        :param: status_string: the status strings to be checked (array)
        '''
        result = None
        resp = self.get_req_result(req_id)
        if resp and isinstance(resp, dict):
            if resp.get('process'):
                if resp['process'].get('nrcStatus','') in status_strings and \
                   resp['process'].get('enmUpdateStatus','') in status_strings:
                    result = resp
                logging.debug(
                        "Returned answer:\n NRC task id: %s processing status: %s",
                        req_id, result)
        return result, resp

    def get_req_result(self, req_id, refresh_auth = False):
        '''
        @brief Call CNR REST endpoint to query the request result
        :param: req_id: the id of the request
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        return self.eiap.eiap_rest_call(GET, f'{self.monitor_url}/{req_id}',
                refresh_auth=refresh_auth)

    def check_health(self, refresh_auth = False):
        '''
        @brief Call CNR REST health endpoint to check health
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        result = None
        resp = self.eiap.eiap_rest_call(GET, self.health_url,refresh_auth=refresh_auth)
        if resp and isinstance(resp, (dict, list)):
            result = resp
        return result

    def wait_request_result(self, req_id, wait=100):
        '''
        @brief Call CNR REST endpoint to check task processing result
        :param: req_id: the id of the NRC task
        :param: wait: the number of seconds to wait for status
        '''
        logging.info(
            "Waiting for request [%s] to finish ...", req_id)
        counter = 0
        status = None
        resp = None
        while counter<wait and not status:
            counter+=1
            time.sleep(1)
            status, resp = self.get_req_status(req_id, [self.SUCCEEDED, self.FAILED, self.COMPLETED])
            if status:
                logging.info("NRC task [%s] execution result is %s", req_id, status)
        if not status:
            logging.info("Timeout exceeded while waiting for NRC task status.")
            if resp:
                logging.info("Last response was:\n%s",json.dumps(resp, indent = 2))
        return status

    def wait_health_status(self, status, wait=100):
        '''
        @brief Call CNR REST endpoint to check health status
        :param: status: the status string to be checked
        :param: wait: the number of seconds to wait for status
        '''
        counter = 0
        alive = False
        result = None
        while counter<wait and not alive:
            counter+=1
            time.sleep(1)
            resp = self.check_health()
            if resp:
                alive = resp['status'] in status
                result = resp
        if not alive:
            logging.info("Timeout exceeded while waiting for CNR health status.")
        return result
