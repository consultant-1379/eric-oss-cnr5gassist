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
This module covers EIAP API GW functions
For more information check these links:
"""
import base64
import json
import logging
import logging.handlers
import time
import requests

from lib.service.commons import SETTINGS, POST, GET

MAX_RETRIES = 20

class EIAPRestError(Exception):
    """
    Signal a http error when request failed
    """

    def __init__(self, http_status, err_message):
        Exception.__init__(self, err_message)
        self.status = http_status
        self.message = err_message

    def __str__(self):
        return (f"EIAP server returned HTTP status [{self.status}]\n"
                f"With error message [{self.message}]")

class EIAPConfig():
    """
    Generic EIAP platform cluster configuration
    """
    DOMAIN = 'domain'
    NS     = 'namespace'
    HOSTS  = 'hosts'
    USERS  = 'users'
    PASSW  = 'pass'
    GPASS  = 'generic_pass'
    E_PFX  = 'endpointprefix'
    APPMGR = 'appmgr'
    APIGW  = 'apigw'
    GASGW  = 'gasgw'
    CNRGW  = 'cnrgw'
    IAM    = 'iam'
    NCMP   = 'ncmp'
    CTS    = 'cts'
    CNR    = 'cnr'
    RTA    = 'rta'
    cfg = {
       DOMAIN: "missing_domain", \
       NS: "missing_namespace", \
       GPASS: "empty", \
       HOSTS: { \
         APPMGR: 'appmgr',\
         APIGW:  'th',\
         GASGW:  'gas',\
         IAM:    'th',\
       }, \
       USERS: { \
         APPMGR: 'kcadmin',\
         NCMP:   'cps-user',\
         CTS:    'cts-user',\
         CNR:    'cnr-user',\
         RTA:    'rta-user',\
       }, \
       E_PFX: { \
         APPMGR: '',\
         NCMP:   '',\
         CTS:    '',\
         CNR:    '',\
       }, \
       PASSW: {} \
     }

    def __init__(self, config_map):
        '''
        Generic configuration initialization
        '''
        for param in [self.DOMAIN, self.NS, self.GPASS]:
            if param in config_map:
                self.cfg[param] = config_map[param]
        if self.HOSTS in config_map:
            for param in [self.APPMGR, self.APIGW, self.IAM]:
                if param in config_map[self.HOSTS]:
                    self.cfg[self.HOSTS][param] = config_map[self.HOSTS][param]
        if self.USERS in config_map:
            for param in [self.APPMGR, self.NCMP, self.CTS, self.CNR]:
                if param in config_map[self.USERS]:
                    self.cfg[self.USERS][param] = config_map[self.USERS][param]
        if self.PASSW in config_map:
            for param in [self.APPMGR, self.NCMP, self.CTS, self.CNR]:
                if param in config_map[self.PASSW]:
                    self.cfg[self.PASSW][param] = config_map[self.PASSW][param]
        if self.E_PFX in config_map:
            for param in [self.APPMGR, self.NCMP, self.CTS, self.CNR]:
                if param in config_map[self.E_PFX]:
                    self.cfg[self.E_PFX][param] = config_map[self.E_PFX][param]

    def get_host(self, host):
        '''
        Get host defined with <host> name
        '''
        return self.cfg[self.HOSTS].get(host,'host_not_set')+self.cfg[self.DOMAIN]

    def get_user(self, user):
        '''
        Get user defined with <user> name
        '''
        return self.cfg[self.USERS].get(user,'user_not_set')

    def get_pass(self, user):
        '''
        Get pass defined with <user> name
        '''
        return self.cfg[self.PASSW].get(user,
                   base64.b64decode(self.cfg.get(self.GPASS, 'bm90X3NldA')))

    def get_endpoint_prefix(self, service):
        '''
        Get user defined with <user> name
        '''
        return self.cfg[self.E_PFX].get(service, '')

    def set_parameter(self, parameter_key, value, group_key = None):
        '''
        Get user defined with <user> name
        '''
        if value:
            if group_key:
                if not self.cfg.get(group_key):
                    self.cfg[group_key] = {}
                self.cfg[group_key][parameter_key] = value
            else:
                self.cfg[parameter_key] = value



class EIAP():
    """!@class EIAP
        @brief Class representing an EAIP API GW object
        @details This is a class that represents EIAP API GW object with
        specific functions to authenticate with, initiate API calls and process responses
    """
    APIGW_LOGIN  = "apigw_login"

    def __init__(self, cfg, host, user, skip_auth = False):
        '''
        @brief Object constructor
        :param: cfg: the configuration containing the needed parameters
        :param: host: the host id used for identifying the proper connection params
        :param: user: the user id used for identifying the proper connection params
        :param: skip_auth: skip token request toward the auth API (no auth is required)
        '''

        self.skip_auth = skip_auth
        self.access_token = None
        self.user        = cfg.get_user(user)
        self.password    = cfg.get_pass(user)
        self.svc_host    = cfg.get_host(host)
        self.auth_host   = cfg.get_host(cfg.IAM)
        self.endpoints   = cfg.cfg["generic_endpoints"]

        self.header = {"Content-Type": "application/json"}
        self.header["Accept"] = "application/json"
        self.auth_header = {"X-tenant": "master"}
        self.auth_header["X-Login"]    = self.user
        self.auth_header["X-password"] = self.password
        logging.info('Getting token for user %s from %s...', self.user, self.auth_host)
        self.refresh_auth()

    def eiap_rest_call(self, method, url, header=None, inputs=None, files=None,
          params=None, nolog=False, refresh_auth = False):
        '''
        @brief Call an EIAP REST function
        :method: the call method, POST or GET
        :para.m: function: the function to be called
        :param: inputs: the json object for call
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        result = None
        resp = None
        if not self.skip_auth and (not self.access_token or refresh_auth):
            self.refresh_auth()
            if not self.access_token:
                logging.info("Cannot perform the request because of missing auth token")
                return None
        if header:
            self.header.update(header)
        try:
            resp = get_raw_url(method, url, self.header, body=inputs, content=files, params=params)
        except EIAPRestError as exc:
            if not nolog:
                logging.warning(
                    "EIAP rest call to url: %s failed with: %s", url, exc)
        if resp and resp.ok:
            try:
                content = resp.json()
                result = content
            except json.JSONDecodeError:
                result = resp
        return result

    def refresh_auth(self):
        '''
        @brief Refreshes the auth token
        '''
        if self.skip_auth:
            logging.debug('Skipping auth token request')
            return
        logging.debug("Refreshing authorization token for: %s", self.user)
        result = None
        try:
            result = get_raw_url(
                POST, f"https://{self.auth_host}{self.endpoints[self.APIGW_LOGIN]}",
                self.auth_header)
        except EIAPRestError as exc:
            logging.debug(
                "EIAP refresh authorization call failed with: %s", exc)

        if result and result.ok:
            self.access_token = result.text
            self.header["Cookie"] = f"JSESSIONID={self.access_token}"
            logging.debug("Authorization refreshed")
        else:
            self.access_token = None
            if result:
                if result.ok:
                    logging.debug("Autorization failed: %s", result.messages)
                else:
                    logging.debug("Autorization failed: %s", result)
            else:
                logging.info("Autorization failed")

    def check_endpoint(self, endpoint_id):
        '''
        @brief Call REST API endpoint
        :param: endpoint: the REST endpoint to be tested
        '''
        result = None
        resp = self.eiap_rest_call(self.svc_host, GET, self.endpoints[endpoint_id], nolog=True)
        if resp and isinstance(resp, (dict, list)):
            result = resp
        return result

    def invoke_endpoint(self, endpoint, payload):
        '''
        @brief Call rApp application REST endpoint with a payload
        :param: app_id: the id of the instantiated app
        '''
        result = None
        if payload:
            if isinstance(payload, dict):
                payload = json.dumps(payload)
            result = self.eiap_rest_call(
            self.svc_host, POST, endpoint, inputs=payload)
        return result

def get_raw_url(method, url, headers=None, body=None, content=None,
    params=None, retries=0):
    """
    Generic URL request module
    """
    wait_time = 2
    while retries >= 0:
        logging.debug("Performing REST request, retries left [%d]",
                      retries)
        error = None
        try:
            session = requests.session()
            response = session.request(method=method,
                                        url=url, headers=headers,
                                        data=body, params=params,
                                        files=content,
                                        verify=SETTINGS["verify_certificates"],
                                        timeout=100)
            if response.ok or response.status_code in [201]:
                return response
            # look if response contains a reason
            try:
                content = response.json()
                logging.debug("Returned content: [%s]", content)
                status = content.get('status',content.get('message','Unknown error'))
                error = f"{status}: {content.get('error',json.dumps(content))}"
            except json.JSONDecodeError:
                # fallback to HTTP error message
                error = response.text

            # fail on authentication, resource not available, or forbidden
            # codes, or flow already exists
            if response.status_code in [403]:
                raise EIAPRestError(response.status_code, "Access forbidden")
            if response.status_code in [503]:
                raise EIAPRestError(response.status_code, "System is temporarily unavailable")
            if response.status_code in [400, 401, 404, 409, 500]:
                raise EIAPRestError(response.status_code, error)
        except (requests.exceptions.Timeout,
                requests.exceptions.ConnectTimeout,
                requests.exceptions.ReadTimeout) as timeout:
            logging.error('Timeout happened: %s', timeout)
            error = timeout
        except requests.exceptions.SSLError as sslerror:
            logging.error('TLS Error happened: %s', sslerror)
            error = str(sslerror)
        except requests.exceptions.RequestException as exception:
            logging.error('Request exception happened: %s', exception)
            error = str(exception)
        if error and retries == 0:
            raise EIAPRestError("", str(error))

        # try again, with back-off
        time.sleep(wait_time)
        retries -= 1
        if wait_time < 64:
            wait_time *= 2


def set_verify_certificates(value):
    """
    Set the verify certificates flag status
    """
    SETTINGS["verify_certificates"] = value
