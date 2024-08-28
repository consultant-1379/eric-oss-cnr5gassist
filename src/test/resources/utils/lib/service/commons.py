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
This module contains contants and objects used by other libraries
"""
import logging
import json
import yaml

PUT     = 'PUT'
PATCH   = 'PATCH'
POST    = 'POST'
GET     = 'GET'
DELETE  = 'DELETE'
HEAD    = 'HEAD'
STAMPFMT = '%Y-%m-%d %H:%M:%S.%f'
DELIM = '------------------'
DEFAULT_LOG_FORMAT = ("[%(asctime)s:"
                      "%(funcName)-10.10s]: %(message)s")
SETTINGS = {"verify_certificates": True}

###############################################################################

def load_config_file(config_file, yamltype=False):
    """!@brief Function to load generic configuration or test report template file
    @return: dictionary structure with configuration parameters
    :param config_file: the configuration filename with path
    :param yamltype: configuration file type
    """
    result = None
    try:
        with open(config_file, encoding="utf8") as cfgfile:
            if not yamltype:
                result = json.load(cfgfile)
            else:
                result = yaml.load(cfgfile, Loader=yaml.FullLoader)
    except IOError as exc:
        if hasattr(exc, 'message'):
            msg = exc.message
        else:
            msg = exc
        print(
            f"A problem was encountered when reading configuration or template file: {config_file} -> {msg}")
    return result

def save_config_file(config_file, content, yamltype=False):
    """!@brief Function to save generic configuration or test report file
    @return: dictionary structure with configuration parameters
    :param config_file: the configuration filename with path
    :param content: the dictionary that has to be saved to the file
    :param yamltype: configuration file type
    """
    try:
        with open(config_file, 'w', encoding="utf8") as output_file:
            if yamltype:
                yaml.dump(content, output_file, default_style = None,
                                     default_flow_style = False, width=1000)
            else:
                json.dump(content, output_file, indent=4, sort_keys=True, default=str)
            logging.debug(" Content was saved to file %s.", config_file)
    except IOError as exc:
        if hasattr(exc, 'message'):
            msg = exc.message
        else:
            msg = exc
        logging.warning(
            "A problem was encountered when saving content to file: %s -> %s", config_file, msg)

###############################################################################

class ENMObject():
    """
    Generic ENM RAN object
    """
    oid        = None
    cmhandle   = None
    name       = None
    shortname  = None
    longfdn    = None
    externalId = None
    childs     = []
    otype      = None
    distance   = None

    def __init__(self, obj):
        if isinstance(obj, dict):
            self.oid = obj.get('id')
            self.cmhandle = obj.get('externalId').split('/')[0]
            self.externalId = obj.get('externalId')
            self.name = obj.get('name','')                     #.rsplit('/',1)[1]
            if '/' in self.name:
                fdnp = self.name.split('/')
                self.shortname = fdnp[-2]
                self.longfdn   = f'SubNetwork={fdnp[0]},SubNetwork={fdnp[1]},MeContext={fdnp[2]},ManagedElement={fdnp[3]}'
            self.otype = obj.get('type','').rsplit('/',1)[1]
        else:
            self.oid = obj

    def __str__(self):
        result = ""
        if self.cmhandle and self.oid and self.name:
            result = f"{self.oid:5} : {self.cmhandle:25} : {self.name}"
        return result

class Cell(ENMObject):
    """
    Generic ENM Cell object
    """
    frequency   = None
    location = []
    plmnids      = {}
    localid  = None

    def __init__(self, obj):
        if isinstance(obj, dict):
            ENMObject.__init__(self, obj)
            self.localid = obj.get('localCellIdNci')
        if isinstance(obj, ENMObject):
            self.oid = obj.oid
            self.cmhandle = obj.cmhandle
            self.name = obj.name

class NRCell(Cell):
    """
    Generic NR Cell object
    """
    otype = 'nrcell'
    nrcelldu = {}
    nrsectorcarrier = {}

    def __init__(self, obj):
        if isinstance(obj, dict):
            Cell.__init__(self, obj)
        if isinstance(obj, (Cell, ENMObject)):
            self.oid = obj.oid
            self.cmhandle = obj.cmhandle
            self.name = obj.name

class LTECell(Cell):
    """
    Generic LTE Cell object
    """
    otype = 'ltecell'

    def __init__(self, obj):
        if isinstance(obj, dict):
            Cell.__init__(self, obj)
        if isinstance(obj, (Cell, ENMObject)):
            self.oid = obj.oid
            self.cmhandle = obj.cmhandle
            self.name = obj.name


class Node(ENMObject):
    """
    Generic ENM Node object
    """
    cells    = {}
    locations = []
    neighbors= {}
    plmnids = []
    frequencies = []
    frequency = 0
    multiPLMN = False

    def __init__(self, obj):
        if isinstance(obj, dict):
            ENMObject.__init__(self, obj)
        if isinstance(obj, ENMObject):
            self.oid = obj.oid
            self.cmhandle = obj.cmhandle
            self.name = obj.name

class GNodeB(Node):
    """
    GNBDU node object
    """
    otype = 'gnbdu'

    def __init__(self, obj):
        if isinstance(obj, dict):
            Node.__init__(self, obj)
        if isinstance(obj, (Node, ENMObject)):
            self.oid = obj.oid
            self.cmhandle = obj.cmhandle
            self.name = obj.name

class ENodeB(Node):
    """
    ENODEB node object
    """
    otype = 'enodeb'
    extgnbf_exists = False    #ExternalGNodeBFunction existence
    gutranw_exists = False    #GutraNetwork existence
    termpgb_exists = False    #Termpoint2GNB existence

    def __init__(self, obj):
        if isinstance(obj, dict):
            Node.__init__(self, obj)
        if isinstance(obj, (Node, ENMObject)):
            self.oid = obj.oid
            self.cmhandle = obj.cmhandle
            self.name = obj.name

def key_find(dicto, key):
    """
    Routine to find a key in dictionary structure
    """
    ret = dicto
    if dicto and isinstance(dicto,dict):
        if key in dicto:
            ret = dicto[key]
        else:
            for val in dicto.values():
                if isinstance(val,dict):
                    item = key_find(val, key)
                    if item is not None:
                        ret = item
                elif isinstance(val,list):
                    for elem in val:
                        item = key_find(elem, key)
                        if item is not None:
                            ret = item
    return ret

def value_find(dicto, value):
    """
    Routine to find a value in dictionary structure
    """
    ret = dicto
    if dicto and isinstance(dicto,dict):
        if value in dicto.values():
            ret = value
        else:
            for val in dicto.values():
                if isinstance(val,dict):
                    item = value_find(val, value)
                    if item is not None:
                        ret = item
                elif isinstance(val,list):
                    for elem in val:
                        item = value_find(elem, value)
                        if item is not None:
                            ret = item
    return ret
