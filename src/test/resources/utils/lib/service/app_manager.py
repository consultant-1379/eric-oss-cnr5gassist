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
This module covers EIAP APP Manager REST API client functions
For more information check these links:
https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/tor/idun-sdk/latest/services/app-onboarding/introduction.html
https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/tor/idun-sdk/latest/services/app-lcm/introduction.html
"""
import os
import json
import logging
import logging.handlers
import time
import copy
from requests_toolbelt import MultipartEncoder, MultipartEncoderMonitor
from tqdm import tqdm
from lib.service.commons import POST, GET, PUT, DELETE
from lib.service.eiap_api_gw import EIAP, EIAPConfig

class AppMGR():
    """!@class AppMGR
        @brief Class representing an EAIP API rest call object
        @details This is a class that represents EIAP API rest call object with
        specific functions to initiate API calls and process responses
    """

    INSTANTIATED = "INSTANTIATED"
    TERMINATED   = "TERMINATED"
    ONBOARDED    = "ONBOARDED"
    FAILED       = "FAILED"
    DELETED      = "DELETED"


    def __init__(self, cfg, user, appmgr = EIAPConfig.APPMGR):
        '''
        @brief Object constructor
        :param: cfg: the configuration containing the needed parameters
        :param: appmgr: the prefix used for identifying the proper host connection params
        :param: user: the prefix used for identifying the proper user params
        '''
        self.eiap = EIAP(cfg, appmgr, user)
        self.host = cfg.get_host(appmgr)
        endpoints = cfg.cfg['generic_endpoints'][EIAPConfig.APPMGR]
        self.onboard_url = f'https://{self.host}'+endpoints['onboarding']
        self.appinst_url  = f'https://{self.host}'+endpoints['instantiation']
        self.applcm_url  = f'https://{self.host}'+endpoints['applcm']

        self.header = {"Content-Type": "application/json"}
        self.header["Accept"] = "application/json"

    def onboard_csar_to_am(self, csar_file):
        '''
        @brief Call App Manager onboarding function for uploading csar
        :param: csar_file: the csar file to be uploaded
        '''

        result = None
        total_size = os.path.getsize(csar_file)
        with tqdm(desc=csar_file, total=total_size, unit='B',
                     unit_scale=True, unit_divisor=1024) as progress_bar:
#        with tqdm(desc=csar_file, total=total_size, unit='B', unit_scale=True, mininterval=2,
#                  bar_format="{desc}: {percentage:10.0f}%\n", unit_divisor=1024) as progress_bar:
            with open(csar_file, 'rb') as cfile:
                mec_fields = {'file': (csar_file, cfile)}
                mec = MultipartEncoder(fields=mec_fields)
                mem = MultipartEncoderMonitor(
                    mec, lambda monitor: progress_bar.update(monitor.bytes_read - progress_bar.n))
                header = copy.copy(self.header)
                header['Content-Type'] = mem.content_type
                result = self.eiap.eiap_rest_call(POST, self.onboard_url, header=header, inputs=mem)
        return result

    def get_onboarded_app(self, app_id, refresh_auth = False):
        '''
        @brief Call App Manager onboarding function for getting onboarded status info
        :param: app_id: the id of the onboarded app
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        return self.eiap.eiap_rest_call(GET, f'{self.onboard_url}/{app_id}',
            refresh_auth=refresh_auth)

    def get_onboarded_app_byname(self, app_name, version = None):
        '''
        @brief Call App Manager onboarding function for getting onboarded status info
        :param: app_name: the id of the onboarded app
        :param: version: the app version to check
        '''
        result = []
        oapps = self.get_onboarded_app('')
        for item in oapps:
            if not app_name or app_name in item['name']:
                if not version or (version and version in item['version']):
                    result.append(item)
        return result

    def enable_onboarded_app(self, app_id, refresh_auth = False):
        '''
        @brief Call App Manager onboarding function for enabling onboarded app
        :param: app_id: the id of the onboarded app
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        logging.info("Enabling onboarded app [%s]", app_id)
        return self.eiap.eiap_rest_call(PUT, f'{self.onboard_url}/{app_id}',
            inputs=json.dumps({"mode": "ENABLED"}) ,refresh_auth=refresh_auth)

    def disable_onboarded_app(self, app_id, refresh_auth = False):
        '''
        @brief Call App Manager onboarding function for disable onboarded app
        :param: app_id: the id of the onboarded app
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        logging.info("Disabling onboarded app [%s]", app_id)
        return self.eiap.eiap_rest_call(PUT, f'{self.onboard_url}/{app_id}',
            inputs=json.dumps({"mode": "DISABLED"}) ,refresh_auth=refresh_auth)

    def get_instantiated_app(self, app_id, refresh_auth = False):
        '''
        @brief Call App Manager LCM function for getting instantiation status info
        :param: app_id: the id of the instantiated app
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        return self.eiap.eiap_rest_call(GET, f'{self.appinst_url}/{app_id}',
            refresh_auth=refresh_auth)

    def get_instantiated_apps(self, health_status = None):
        '''
        @brief Call App Manager LCM function for getting instantiation list based on status
        :param: health_status: the status of the instantiated app
        '''
        result = []
        resp = self.get_instantiated_app("")
        if resp and isinstance(resp, dict) and "appInstances" in resp:
            logging.debug(json.dumps(resp, indent = 2))
            result = [x for x in resp["appInstances"] if not health_status or x['healthStatus'] == health_status]
        return result

    def get_instantiated_app_byname(self, app_name, list_all = False):
        '''
        @brief Call App Manager LCM and onboarding function for getting instantiation status info
        :param: app_name: the rApp name of the instantiated app
        :param list_all: list all records in any status
        '''
        result = []
        status_filter = None
        if not list_all:
            status_filter = self.INSTANTIATED
        iapps = self.get_instantiated_apps(status_filter)
        for item in iapps:
            resp = self.get_onboarded_app(item['appOnBoardingAppId'])
            if resp and (not app_name or app_name in resp['name']):
                item['name'] = f"{resp['name']}-{resp['version']}"
                result.append(item)
            else:
                item['name'] = "No oboarding record"
                result.append(item)
        return result

    def get_onboarded_app_instance(self, onboarded_app_id):
        '''
        @brief Call App Manager LCM function for getting instantiation status info
        :param: onboarded_app_id: the ondoarded_app)id the instance is searched for
        '''
        resp = self.get_instantiated_apps(self.INSTANTIATED)
        return [x for x in resp if x['appOnBoardingAppId'] == onboarded_app_id]

    def terminate_onboarded_app_instance(self, onboarded_app_id, wait=100):
        '''
        @brief Call App Manager LCM function for terminating an instantiated app instance
        :param: onboarded_app_id: the onboarding id of the instantiated app
        :param: wait: the number of seconds to wait for status
        '''
        result = None
        result = self.get_onboarded_app_instance(onboarded_app_id)
        if len(result)>1:
            logging.warning("There is more than one instantiated app for onboarding id [%s]!",
                            onboarded_app_id)
            self.terminate_instantiated_app(result[0], wait)
        elif len(result)==1:
            self.terminate_instantiated_app(result[0], wait)
        else:
            logging.warning("There is no instantiated app for onboarding id [%s]!",
                            onboarded_app_id)

    def terminate_instantiated_app(self, app_id, wait=100):
        '''
        @brief Call App Manager LCM function for terminating and instantiated app instance
        :param: app_id: the id of the instantiated app
        :param: wait: the number of seconds to wait for status
        '''
        logging.info(
            "Terminating current app [%s] instance in app manager", app_id)
        if self.check_iapp_status(app_id, [self.INSTANTIATED], 1):
            self.eiap.eiap_rest_call(PUT, f'{self.appinst_url}/{app_id}')
            if self.check_iapp_status(app_id, [self.TERMINATED], wait):
                logging.info("App [%s] instance has been terminated", app_id)
            else:
                logging.info("App [%s] termination failed", app_id)
        else:
            logging.warning(
                "The application instance [%s] is not in (%s) state, termination skipped",
                app_id, self.INSTANTIATED)

    def delete_onboarded_app(self, app_id, wait=100):
        '''
        @brief Call App Manager LCM function for deleting an onboraded app
        :param: app_id: the id of the onboarded app
        :param: wait: the number of seconds to wait for status
        '''
        logging.info(
            "Deleting onboarded app [%s] in app manager", app_id)
        if self.check_oapp_status(app_id, [self.ONBOARDED], 1):
            self.disable_onboarded_app(app_id)
            self.eiap.eiap_rest_call(DELETE, f'{self.applcm_url}/apps/{app_id}')
            if self.check_oapp_status(app_id, [self.DELETED], wait):
                logging.info("Onboarded ap [%s] has been deleted", app_id)
            else:
                logging.info("Onboarded app [%s] deletion failed", app_id)
        else:
            logging.warning(
                "The application [%s] is not in (%s) state, deletion skipped",
                app_id, self.ONBOARDED)

    def instantiate_with_am(self, app_id, payload, upgrade = False, wait=100):
        '''
        @brief Call App Manager LCM function for instantiation of an app
        :param: app_id: the id of the instantiated app
        :param: payload: the json payload to be used for instantiation
        :param: upgrade: selector parameter to indicate upgrade of running instance
        :param: wait: the number of seconds to wait for instantiation result
        '''
        result = None
        if payload:
            payload['appId'] = app_id
            logging.info("Instantiating app with onboarding id [%s] in app manager", app_id)
            method = POST
            if upgrade:
                method = PUT
            resp = self.eiap.eiap_rest_call(method, self.appinst_url, inputs=json.dumps(payload))
            if resp and isinstance(resp, dict):
                if not resp.get('id'):
                    logging.debug(
                        'Returned answer:\n %s', json.dumps(resp, indent=4))
                    if resp.get('detail'):
                        logging.info(
                            'Returned answer:\n %s', resp.get('detail'))
                instantiated_app_id = resp['id']
                logging.info(
                    "Returned answer:\n ID: %s -> Onboard id: %s instantiation status: %s",
                    instantiated_app_id, resp['appOnBoardingAppId'], resp['healthStatus'])
                resp = self.check_iapp_status(
                      instantiated_app_id, [self.INSTANTIATED, self.FAILED, self.TERMINATED], wait)
                if resp:
                    logging.info("App with onboarding id [%s] has been instantiated with id [%s]",
                    app_id, instantiated_app_id)
                    result = resp
            else:
                logging.info("Instantiating of app with onboarding id [%s] has failed", app_id)
        return result

    def check_iapp_status(self, instantiated_app_id, status_strings, wait=100):
        '''
        @brief Call App Manager LCM function for checking instantiated app status
        :param: app_id: the id of the instantiated app
        :param: status_string: the status strings to be checked (array)
        :param: wait: the number of seconds to wait for status
        '''
        result = None
        counter = 0
        statusmatch = False
        while counter<wait and not statusmatch:
            counter+=1
            time.sleep(1)
            resp = self.get_instantiated_app(instantiated_app_id)
            if resp and isinstance(resp, dict):
                statusmatch = resp['healthStatus'] in status_strings
                if statusmatch:
                    result = resp
                    logging.info(
                        "Returned answer:\n ID: %s -> Onboard id: %s instantiation status: %s",
                        resp['id'], resp['appOnBoardingAppId'], resp['healthStatus'])
        if not statusmatch:
            logging.info("Timeout exceeded while waiting for app [%s] status to be in %s.",
                         instantiated_app_id, status_strings)
        return result

    def check_oapp_status(self, onboarded_app_id, status_strings, wait=100):
        '''
        @brief Call App Manager Onboarding function for checking onboarded app status
        :param: app_id: the id of the onboarded app
        :param: status_string: the status strings to be checked (array)
        :param: wait: the number of seconds to wait for status
        '''
        result = None
        counter = 0
        statusmatch = False
        while counter<wait and not statusmatch:
            counter+=1
            time.sleep(1)
            resp = self.get_onboarded_app(onboarded_app_id)
            if resp and isinstance(resp, dict):
                statusmatch = resp['status'] in status_strings
                if statusmatch:
                    result = resp
        if not statusmatch:
            logging.info("Timeout exceeded while waiting for app [%s] status to be in %s.",
                         onboarded_app_id, status_strings)
        return result
