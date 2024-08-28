#!/usr/bin/env python3
#
# COPYRIGHT Ericsson 2023
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
This module covers EIAP Runtime Topology Adapter Service (RTA) API client functions
For more information check these links:
...
"""
import json
import logging
import logging.handlers
from lib.service.commons import POST
from lib.service.eiap_api_gw import EIAP, EIAPConfig


class RTA():
    """!@class RTA
        @brief Class representing an EAIP RTA API rest call object
        @details This is a class that represents EIAP RTA API rest call object with
        specific functions to initiate API calls and process responses
    """

    def __init__(self, cfg, user, apigw = EIAPConfig.GASGW,
                     eiap = None, endpoint_tag = EIAPConfig.RTA):
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
        endp_pfx  = cfg.get_endpoint_prefix(endpoint_tag)
        self.discovery_url = f'https://{self.host}'+ endp_pfx+ endpoints['discovery']

        self.header = {"Content-Type": "application/json"}
        self.header["Accept"] = "*/*"

    def trigger_discovery(self, cmhandles):
        '''
        @brief Call EIAP RTA endpoint to start CTS sync of ENM objects (and cm handles)
        :param: cmhandles: the list of cmhandles to be synchronized
        '''
        link = f'{self.discovery_url}'
        logging.info("Calling %s", link)
        payload = { "cmHandles": cmhandles}
        logging.info("Payload used: %s", json.dumps(payload))
        result = self.eiap.eiap_rest_call(POST, link, inputs = json.dumps(payload))
        if result:
            logging.info("Trigger request returned: %s", result.text)
        else:
            logging.info("Trigger toward %s has failed", link)
