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

"""!@file munin_service.py
    @details library for handling Munin REST API Client functions for CI scripts (forked from RelAuto repository)
    https://gerrit.ericsson.se/a/RelAuto/relauto
"""
import logging
import re
import collections
import json
import time
import copy
import getpass
import urllib3
import requests
from lib.service.commons import POST, GET, PUT, PATCH, HEAD


urllib3.disable_warnings()


# Schema version supports product structure model version 2.0
SCHEMA_VERSION = '10.0.0'
MUNIN_ENV = {'PROD': 'https://munin.internal.ericsson.com/api/v1',
             'TEST': 'https://munin-sandbox.internal.ericsson.com/api/v1'}
MIMER_AUTHN_ENV = {'TEST': 'https://mimer-sandbox.internal.ericsson.com/authn/api/v2/refresh-token',
                   'PROD': 'https://mimer.internal.ericsson.com/authn/api/v2/refresh-token' }
PRODUCT               = '/products/%s'
PRODUCT_VERSION       = '/products/%s/versions/%s'
SVL_REPORT_REQ        = '/products/%s/versions/%s/reports'

# Supply Origin
SO_ERICSSON = 'ERICSSON'
SO_FOSS     = 'FOSS'
SO_COTS     = 'COTS'
# Artifact Category
AC_ABSTRACT   = 'Abstract'
AC_DERIVATIVE = 'Derivative'
AC_SOURCE     = 'Source'
# Statuses
IN_WORK        = 'InWork'
COMPLETED      = 'Completed'
AUTO_COMPLETED = 'AutoCompleted'
# Linking
LINKINGS = ['Dynamic', 'Static', 'Classpath', 'Not Linked']
ENCRYPTION_PURPOSE_OPTIONS = ['Data confidentiality, OAM only',
                              'Data confidentiality, not only OAM',
                              'Other than data confidentiality']
# Munin structure attribute names
MUNIN_TID      = 'muninTargetIdentifier'
PRIM_TID       = 'primTargetIdentifier'
M_PROD_RSTATE  = 'productRstate'
M_PROD_VER_L   = 'productVersionLabel'
M_PROD_VER     = 'productVersion'
M_PROD_NR      = 'productNumber'
M_REF_PROD_NR  = 'referenceProductNumber'

MAX_RETRIES = 20
SETTINGS = {"verify_certificates": True}

MUNIN_USES_FOSS_OBJECT = { \
     MUNIN_TID: { "productVersionLabel": None, "productNumber": None }, \
     "systemOfRecord": "Munin", \
     "fossUsage": { \
       "obligationFulfillment": None,\
       "fossUsageStatus": "InWork",   \
       "usageDescription": None,  \
       "fossLicenses": []  \
     } \
    }
PRIM_USES_FOSS_OBJECT = { \
     PRIM_TID: { "productRstate": None, "referenceProductNumber": None , "supplyOrigin": "FOSS" }, \
     "systemOfRecord": "PRIM", \
     "fossUsage": { \
       "obligationFulfillment": None,\
       "fossUsageStatus": "InWork",   \
       "usageDescription": None,  \
       "fossLicenses": []  \
     } \
    }
PRIM_INCLUDE_OBJECT = { \
     PRIM_TID: { "productRstate": None, "referenceProductNumber": None }, \
     "systemOfRecord": "PRIM", \
    }

MUNIN_INCLUDE_OBJECT = { \
     MUNIN_TID: { "productVersionLabel": None, "productNumber": None }, \
     "systemOfRecord": "Munin", \
    }

###############################################################################
class MuninRestError(Exception):
    """
    Signal a http error when request failed
    """
    def __init__(self, http_status, err_message):
        Exception.__init__(self, err_message)
        self.status = http_status
        self.message = err_message

    def __str__(self):
        return (f"Munin server returned HTTP status [{self.status}]\n"
                f"With error message [{self.message}]")


class Munin():
    """!@class Munin
        @brief Class representing a Munin API rest call object
        @details This is a class that represents Munin API rest call object with specific function to initiate
        API calls and process responses
    """

    def __init__(self, refresh_token, signum = None, env='PROD'):

        self.header = { "Content-Type": "application/json" }
        self.header["Accept"] = "application/json"
        if not signum:
            self.signum = getpass.getuser()
        else:
            self.signum = signum
        self.header["X-On-Behalf-Of"] = self.signum
        self.env = env
        self.etag = None
        self.refresh_token = refresh_token
        self.base_url = MUNIN_ENV.get(env)
        self.auth_url = MIMER_AUTHN_ENV.get(env)
        self.refresh_auth()

    def munin_rest_call(self, method, function, inputs = None):
        '''
        @brief Call a Munin REST function
        :method: the call method, POST or GET
        :param: function: the fucntion to be called
        :param: inputs: the json object for call
        '''
        resp = None
        url = f"{self.base_url}{function}"
        try:
            resp = get_raw_url(method, url, self.header, inputs)
        except MuninRestError as exc:
            logging.warning("Munin rest call failed with: %s", exc)
        return resp

    def refresh_auth(self):
        '''
        @brief Refreshes the auth token
        '''
        data = { 'token': self.refresh_token }
        logging.info("Refreshing authorization token for: %s", self.signum)
        result = get_raw_url(POST, self.auth_url, self.header, data)
        if result.ok and result.code == 'OK':
            self.refresh_token = result.data.get('refresh_token')
            self.access_token = result.data.get('access_token')
            self.header["Authorization"] = f"Bearer {self.access_token}"
            logging.info("Authorization refreshed")
        else:
            self.access_token = None
            if result.ok:
                logging.info("Autorization failed: %s", result.messages)
            else:
                logging.info("Autorization failed: %s", result)


    def search_product(self, prod_nr, prod_ver = None):
        '''
        @brief Munin product search
        :param: prod_nr: the product number to be checked
        :param: prod_ver: the product version to be checked (optional)
        '''
        resp = None
        response = self.munin_rest_call( GET, PRODUCT % prod_nr)
        if response and response.ok:
            versions = response.data.get('productVersions')
            if prod_ver:
                for item in versions:
                    label = item.get(M_PROD_VER_L)
                    if is_the_same_version(label, prod_ver):
                        resp = label
            else:
                resp = versions
        return resp

    def foss_deep_search(self, munin_prod_nr, prim_prod_nr, version, secondary_version = None):
        '''
        @brief Munin foss product search with deep search(when no matching version is found,
               PRIM product is searched in each FOSS version
        :param: munin_prod_nr: the product number to be checked
        :param: prim_prod_nr: the product number to be checked
        :param: version: the product version to be checked (mostly semver pattern search)
        :param: secondary_version: the freeform product version to be checked - exact match needed
        '''
        resp = None
        foss_version = self.search_product(munin_prod_nr, version)
        if not foss_version and secondary_version and len(secondary_version)>1:
            foss_version = self.search_product(munin_prod_nr,secondary_version)
        if not foss_version:
            foss_versions = self.search_product(munin_prod_nr)
            if foss_versions:
                logging.info("Performing deep search (in %s versions) for: %s %s -> %s",
                             len(foss_versions), munin_prod_nr, version, prim_prod_nr)
                for item in foss_versions:
                    label = item.get(M_PROD_VER_L)
                    foss = self.get_product(munin_prod_nr,label)
                    if foss and foss.get('sourcing') and foss['sourcing'].get(PRIM_TID):
                        if prim_prod_nr == foss['sourcing'][PRIM_TID][M_REF_PROD_NR]:
                            resp = label
        else:
            resp = foss_version
        return resp

    def get_product(self, prod_nr, prod_ver):
        '''
        @brief Munin product get function for a registered Munin product
        :param: prod_nr: the product number
        :param: prod_ver: the product version
        '''
        response = self.munin_rest_call( GET, PRODUCT_VERSION % (prod_nr, prod_ver))
        if response and response.ok:
            logging.info("%s for %s / %s", response.messages, prod_nr, prod_ver)
            if response.etag:
                self.etag = response.etag
            return response.data
        return None

    def patch_product(self, prod_nr, prod_ver, patch_struct):
        '''
        @brief Munin product patching, aca changing parts of registered Munin product
        :param: prod_nr: the product number to be patched
        :param: prod_ver: the product version to be patched
        :param: patch_struct: the patch structure to be updated with
        '''
        if self.etag:
            self.header["If-Match"] = self.etag
        response = self.munin_rest_call( PATCH, PRODUCT_VERSION % (prod_nr, prod_ver),
                                         { 'content': patch_struct })
        if response and response.ok:
            logging.info("%s for %s / %s", response.messages, prod_nr, prod_ver)
            if response.etag:
                self.etag = response.etag

    def update_product(self, prod_nr, prod_ver, prod_struct):
        '''
        @brief Munin product update, aca changing parts of registered Munin product
        :param: prod_nr: the product number to be patched
        :param: prod_ver: the product version to be patched
        :param: prod_struct: the patch structure to be updated with
        '''
        if self.etag:
            self.header["If-Match"] = self.etag
        response = self.munin_rest_call( PUT, PRODUCT_VERSION % (prod_nr, prod_ver), prod_struct)
        if response and response.ok:
            logging.info("%s for %s / %s", response.messages, prod_nr, prod_ver)
            if response.etag:
                self.etag = response.etag

    def get_svl_report(self, prod_nr, prod_ver, outfile):
        '''
        @brief Get SVL report from Munin for a specific product
        :param: prod_nr: the product number
        :param: prod_ver: the product version
        '''
        ret = None
        report_payload = { "reportType": "SVLinput", "reportFileType": 'xlsx'}
        response = self.munin_rest_call( POST, SVL_REPORT_REQ % (prod_nr, prod_ver), report_payload)
        if response:
            if response.ok:
                location = response.location
                logging.info("%s: %s for %s v%s from %s", response.status_code,
                              response.messages, prod_nr, prod_ver, location)
                response = self.munin_rest_call( HEAD, location, {})
                wait = 100
                while response and response.status_code != 201 and wait>0:
                    response = self.munin_rest_call( HEAD, location, {})
                    time.sleep(1)
                    wait-=1
                logging.info("%s: %s for %s v%s from %s", response.status_code,
                              response.messages, prod_nr, prod_ver, location)
                if response.status_code == 201:
                    self.header["Accept"] = "*/*"
                    response = self.munin_rest_call( GET, location, {})
                    if response and response.status_code == 200:
                        with open(outfile, 'wb') as downdoc:
                            downdoc.write(response.content)
                    logging.info("Downloaded %s for %s v%s from %s",
                                  outfile, prod_nr, prod_ver, location)
                ret = response
            else:
                logging.warning("Error response received: %s", response.messages)

        return ret



def get_prim_object(prod_nr, rstate, template = None):
    '''
    @brief Prim object generator for Munin (default is include one)
    :param: prod_nr: the product number
    :param: rstate: the product rstate
    '''
    if not template:
        template = PRIM_INCLUDE_OBJECT
    record = copy.deepcopy(template)
    record[PRIM_TID][M_REF_PROD_NR] = prod_nr
    record[PRIM_TID][M_PROD_RSTATE] = rstate
    return record

def get_munin_object(prod_nr, version, template = None):
    '''
    @brief Munin object generator for Munin (default is include one)
    :param: prod_nr: the product number
    :param: version: the product version
    '''
    if not template:
        template = MUNIN_INCLUDE_OBJECT
    record = copy.deepcopy(template)
    record[MUNIN_TID][M_PROD_NR]    = prod_nr
    record[MUNIN_TID][M_PROD_VER_L] = version
    return record

def remove_from_includes(includes_list,  prod_nr, version = None):
    '''
    @brief Function to remove a product from Munin includes
    :param: includes_list: the includes list
    :param: prod_nr: the product number to be checked
    :param: version: the product version
    '''
    remove_list = []
    prod_nr = prod_nr.replace(' ','')
    if includes_list:
        for item in includes_list:
            item_2beremoved = None
            if item.get(PRIM_TID):
                if prod_nr and prod_nr in item[PRIM_TID][M_REF_PROD_NR].replace(' ',''):
                    item_2beremoved = item
                    if version and version not in item[PRIM_TID][M_PROD_RSTATE]:
                        item_2beremoved = None
            elif item.get(MUNIN_TID):
                if prod_nr and prod_nr in item[MUNIN_TID][M_PROD_NR].replace(' ',''):
                    item_2beremoved = item
                    if version and version not in item[MUNIN_TID][M_PROD_VER_L]:
                        item_2beremoved = None
            if item_2beremoved:
                remove_list.append(item_2beremoved)
    for item in remove_list:
        if item in includes_list:
            includes_list.remove(item)

def is_in_the_list(munin_relation_list,  prod_nr, version = None):
    '''
    @brief Function to check if a product is included into the Munin relation list (includes, usesFoss, usesEncryption)
    :param: munin_relation_list: the Munin relation list object (enumeration)
    :param: prod_nr: the product number to be checked
    :param: version: the product version to be checked
    '''
    result = None
    prod_nr = prod_nr.replace(' ','')
    if munin_relation_list:
        for item in munin_relation_list:
            if item.get(PRIM_TID):
                if prod_nr and prod_nr in item[PRIM_TID][M_REF_PROD_NR].replace(' ',''):
                    if version:
                        result = is_the_same_version(item[PRIM_TID][M_PROD_RSTATE], version)
                    else:
                        result = True
            elif item.get(MUNIN_TID):
                if prod_nr and prod_nr in item[MUNIN_TID][M_PROD_NR].replace(' ',''):
                    if version:
                        result = is_the_same_version(item[MUNIN_TID][M_PROD_VER_L], version)
                    else:
                        result = True
    return result

def compare_lists(munin_relation_list,  munin_reference_relation_list):
    '''
    @brief Function to produce a diff of product/version lists
    :param: munin_relation_list: the Munin relation list object (enumeration) to compare
    :param: munin_reference_relation_list: the Munin relation list object (enumeration) to compare with
    '''
    majorv_re = re.compile(r"\d+")
    list2compare  = {}
    reference_list = {}
#        commons       = []
    added         = []
    removed       = []
    for mlist, tlist in [( munin_relation_list, list2compare ), ( munin_reference_relation_list, reference_list)]:
        for item in mlist:
            if item.get(PRIM_TID):
                pnr = item[PRIM_TID][M_REF_PROD_NR]
                if '/' in pnr:
                    pnp1, pnp2 = pnr.split('/')
                    if majorv_re.match(pnp1):
                        version = pnp1
                        pnr = pnp2
                    else:
                        version = pnp2
                        pnr = pnp1
                    version += item[PRIM_TID][M_PROD_RSTATE]
                else: version = item[PRIM_TID][M_PROD_RSTATE]
                tlist[pnr] = version
            elif item.get(MUNIN_TID):
                tlist[item[MUNIN_TID][M_PROD_NR]]= item[MUNIN_TID][M_PROD_VER_L]
        tlist = collections.OrderedDict(sorted(tlist.items()))
    added   = { k : list2compare[k] for k in set(list2compare) - set(reference_list) }
    removed = { k : reference_list[k] for k in set(reference_list) - set(list2compare) }
    changed = { k : f"{reference_list[k]} -> {list2compare[k]}" \
                for k in set(list2compare) - set(added) if reference_list[k] not in list2compare[k]}

    return added, removed, changed

def is_the_same_version(munin_version, sem_version):
    '''
    @brief Function to compare a semantic version with Munin freeform version
    :param: munin_version: the Munin version string
    :param: sem_version: regular semantic version
    '''
    # version check is not 100% sure because of hectic Munin FOSS versioning
    # first check fixed version or the 'v' version or z version cutting .0 from semver, then checking contains relation
    resp = None
    if not munin_version or not sem_version:
        resp = None
    elif sem_version == munin_version:
        resp = munin_version
    else:
        semver_re  = re.compile(r".*?(\d+\.\d\d*)(\.\d{1,5})?.*")
        if semver_re.match(munin_version) and semver_re.match(sem_version):
            mver = semver_re.match(munin_version).groups()
            sver = semver_re.match(sem_version).groups()
     #       print(f"{munin_version} vs {sem_version} :  {mv} vs {sv}")
            if mver[0] == sver[0]:
                if sver[1]:
                    if mver[1]:
                        if mver[1] == sver[1]:
                            resp = munin_version
                    elif sver[1] == '.0':
                        resp = munin_version
                elif mver[1] == '.0' or not mver[1]:
                    resp = munin_version
    return resp

def get_raw_url(method, url, headers=None, body=None, retries=0):
    '''
    Generic URL request module
    '''
    wait_time = 2
    while retries >= 0:
        logging.debug("Performing REST request, retries left [%d]",
                      retries)
        error = None
        try:
            response = requests.request(method=method,
                                        url = url ,
                                        headers = headers,
                                        data = json.dumps(body),
                                        verify=SETTINGS["verify_certificates"],
                                        timeout=100)

            content = MuninResult(response)
            logging.debug("Returned content: [%s]", content)
#            logging.debug("Response headers: [%s]", response.headers)
            if response.ok or response.status_code in [201, 202, 404]:
                return content

            # fail on authentication, resource not available, or forbidden
            # codes, or flow already exists
            if response.status_code in [403]:
                raise MuninRestError(response.status_code, "Access forbidden")
            if response.status_code in [503]:
                raise MuninRestError(response.status_code, "System is temporarily unavailable")
            if response.status_code in [400, 401, 404, 405, 406, 409, 422, 409, 500]:
                raise MuninRestError(response.status_code, content)
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
            raise MuninRestError(response.status_code, str(error))

        # try again, with back-off
        time.sleep(wait_time)
        retries -= 1
        if wait_time < 64:
            wait_time *= 2

class MuninResult:
    '''
    Result object from Munin
    '''
    def __init__(self, httpresponse):
        '''Example:
        { 'results': [
             { 'operation': 'GET_PRODUCT',
               'messages': [
                   'Successfully get all the versions of product metadata'
               ],
               'data': {
                   'productVersioningSchema': 'SemVer2.0.0',
                   '$schema': 'https://armstage1.rnd.ki.sw.ericsson.se/',
                   'name': 'Test',
                   'description': 'test',
                   'productNumber': 'apr20130'
               }
             }
          ]
        }
        '''
        if httpresponse.headers:
            self.etag = httpresponse.headers.get('ETag')
            self.location = httpresponse.headers.get('Location')
        self.headers = httpresponse.headers
        self.text = httpresponse.text
        self.content = httpresponse.content
        self.status_code = httpresponse.status_code
        logging.debug("Received: %s", self.text)
        try:
            self.content = httpresponse.json()
            if self.content and self.content.get('results'):
                result = self.content.get('results')[0]
                self.messages = result.get('messages')
                self.code = result.get('code')
                self.ok = not 'ERROR' in self.code
                self.data = result.get('data')
                self.operation = result.get('operation')
            else:
                self.ok = False
        except json.JSONDecodeError:
            # fallback to HTTP error message
            self.messages = ""
            self.ok = False

    def __str__(self):
        return json.dumps(self.text, indent=2)

def set_verify_certificates(value):
    """
    Set the verify certificates flag status
    """
    SETTINGS["verify_certificates"] = value
