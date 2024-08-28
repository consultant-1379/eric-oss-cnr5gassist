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
This module covers EIAP Network CM Proxy Service (NCMP) API client functions
For more information check these links:
"""
import logging
import json
from lib.service.commons import GET, DELETE, POST, PATCH, key_find
from lib.service.eiap_api_gw import EIAP, EIAPConfig

ENODEB_FUNCTION     = 'ericsson-enm-Lrat:ENodeBFunction'
GNBDU_FUNCTION      = 'ericsson-enm-GNBDU:GNBDUFunction'
NRSECTORCARRIER     = 'ericsson-enm-GNBDU:NRSectorCarrier'
EXTGNODEB_FUNCTION  = 'ericsson-enm-Lrat:ExternalGNodeBFunction'
TERMPOINTTOGNB      = 'ericsson-enm-Lrat:TermPointToGNB'
EXTENODEB_FUNCTION  = 'ericsson-enm-Lrat:ExternalENodeBFunction'
GUTRANW             = 'erienmnrmlrat:GUtraNetwork'
GUTRANSSFREQ        = 'erienmnrmlrat:GUtranSyncSignalFrequency'
GNBCUCP_FUNCTION    = 'erienmnrmgnbcucp:GNBCUCPFunction'
GNBCUUP_FUNCTION    = 'erienmnrmgnbcuup:GNBCUUPFunction'
TRANSPORT           = 'erienmnrmcomtop:Transport'
ROUTER              = 'erienmnrmrtnl3router:Router'
IP_INTERFACE        = 'erienmnrmrtnl3interfaceipv4:InterfaceIPv4'
IP_ADDRESS          = 'erienmnrmrtnl3interfaceipv4:AddressIPv4'
ENDPOINT            = 'erienmnrmgnbcucp:EndpointResource'
SCTP_PROFILE        = 'erienmnrmrtnsctpprofile:SctpProfile'
SCTP_ENDPOINT       = 'erienmnrmrtnsctp:SctpEndpoint'
LOCAL_SCTP_ENDPOINT = 'erienmnrmgnbcucp:LocalSctpEndpoint'
NRCELLDU            = 'erienmnrmgnbdu:NRCellDU'
NRCELLCU            = 'erienmnrmgnbdu:NRCellCU'
NRSECTORCARRIER     = 'erienmnrmgnbdu:NRSectorCarrier'
RES_PARTITIONS      = 'erienmnrmgnbcucp:ResourcePartitions'
RES_PARTITION       = 'erienmnrmgnbcucp:ResourcePartition'
RES_PARTITION_MEMBER= 'erienmnrmgnbcucp:ResourcePartitionMember'

GNBDU_PLMNID = 'dUpLMNId'
A_PLMNIDLIST = 'pLMNIdList'
A_ARFCNDL    = 'arfcnDL'
A_ARFCN      = 'arfcn'
A_GUTRANSSFR = 'gUtranSyncSignalFrequencyId'
ADMIN_STATE  = 'administrativeState'
A_CELLLOCID  = 'cellLocalId'

class MO():
    """
    Generic ENM MO
    """
    def __init__(self, name, oid, parent = None):
        self.oid = oid
        self.name = name
        if name:
            self.shortname = name.split(':')[-1]
        else:
            self.shortname = None
        self.parent = parent

    def get_resname(self):
        result = ""
        if self.name:
            result = f"/{self.name}={self.oid}"
        if self.parent and isinstance(self.parent, MO):
            result = f"{self.parent.get_resname()}{result}"
        return result

    def get_parentresname(self):
        result = "/"
        if self.parent:
            result = f"{self.parent.get_resname()}"
        return result

    def get_refname(self):
        result = ""
        if self.shortname:
            result = f"{self.shortname}={self.oid}"
        if self.parent and isinstance(self.parent, MO):
            result = f"{self.parent.get_refname()},{result}"
        return result

    def get_payload(self, attr = None):
        pld = {'id':self.oid}
        if attr:
            pld['attributes'] = attr
        return json.dumps({ self.name: [pld]})


def get_resource(field, item_id=1):
    result = ''
    if field:
        result = f"/{field}={item_id}"
    return result

def get_resources(fields_list, item_id=1):
    result = ''
    for field in fields_list:
        result+= f"/{field}={item_id}"
    return result

def get_fields(fields_list, joiner = ';'):
    return f"fields={joiner.join(fields_list)}"

def pack_field_and_attributes(field, attributes):
    return f"{field}/attributes({';'.join(attributes)})"

def get_attribute(obj, field):
    """@brief Extracts field attribute from the obj dict object
    """
    result = None
    if isinstance(obj, dict) and 'attributes' in obj:
        if field in obj['attributes']:
            result = obj['attributes'][field]
    return result

def generate_string_object(obj_name, obj_id, obj_attr):
    """@brief Creates a dict structure with the parameters given
    """
    value = json.dumps({"id": obj_id, "attributes": obj_attr})
    return f'{{ "{obj_name}": [{value}]}}'

class NCMP():
    """!@class NCMP
        @brief Class representing an EAIP API rest call object
        @details This is a class that represents EIAP API rest call object with
        specific functions to initiate API calls and process responses
    """

    def __init__(self, cfg, user, apigw = EIAPConfig.APIGW,
                     eiap = None, endpoint_tag = EIAPConfig.NCMP):
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
        self.endpoints = cfg.cfg['generic_endpoints'][endpoint_tag]
        self.resqry_url= f'https://{self.host}'+ self.endpoints['res_qry']
        self.rescreate_url= f'https://{self.host}'+ self.endpoints['res_create']
        self.node_opt       = self.endpoints['xt_opt']
        self.termpoint_opt  = self.endpoints['tp_opt']
        self.gutranwk_opt   = self.endpoints['gu_opt']
        self.nrcellcu_opt   = self.endpoints['cu_opt']

        self.header = {"Content-Type": "application/json"}
        self.header["Accept"] = "application/json"

    def get_resource(self, cmhandle, resource, options = None, refresh_auth = False):
        '''
        @brief Call EIAP NCMP endpoint to query data
        :param: cmhandle: the object cmhandle
        :param: resource: the resource to be used for query
        :param: options: the options to be used for resource query
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        link = self.resqry_url.format(cmhandle=cmhandle)
        payload = {'resourceIdentifier': resource}
        if options:
            payload['options'] = options
        result = None
        logging.debug("Calling %s with payload value: %s", link, json.dumps(payload, indent=4))
        resp = self.eiap.eiap_rest_call(GET, link, params=payload, refresh_auth=refresh_auth)
        if resp:
            if 'message' in resp:
                logging.warning('NCMP resource request failed with: %s', resp['message'])
            else:
                result = resp
        return result

    def patch_resource(self, cmhandle, resource, payload, refresh_auth = False):
        '''
        @brief Call EIAP NCMP endpoint to patch resource
        :param: cmhandle: the object cmhandle
        :param: resource: the resource to be patched in ENM
        :param: payload: the payload to be used for patching
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        result = False
        link = self.rescreate_url.format(cmhandle=cmhandle)
        params = {'resourceIdentifier': resource}
        resp = self.eiap.eiap_rest_call(PATCH, link, inputs=payload, params=params, refresh_auth=refresh_auth)
        if resp:
            if 'message' in resp:
                logging.warning('NCMP resource patching request failed with: %s', resp['message'])
            else:
                result = True
        return result

    def delete_resource(self, cmhandle, resource, options = None, refresh_auth = False):
        '''
        @brief Call EIAP NCMP endpoint to delete resource
        :param: cmhandle: the object cmhandle
        :param: resource: the resource to be deleted from ENM
        :param: options: the options to be used for deletion
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        result = False
        link = self.rescreate_url.format(cmhandle=cmhandle)
        logging.info('Deleting resource: %s', resource)
        payload = {'resourceIdentifier': resource}
        if options:
            payload['options'] = options
        resp = self.eiap.eiap_rest_call(DELETE, link, params=payload, refresh_auth=refresh_auth)
        if resp:
            if 'message' in resp:
                logging.warning('NCMP resource delete request failed with: %s', resp['message'])
            else:
                result = True
        return result

    def create_resource(self, cmhandle, resource, payload, refresh_auth = False):
        '''
        @brief Call EIAP NCMP endpoint to create a resource
        :param: cmhandle: the object cmhandle
        :param: resource: the resource to be extended in ENM
        :param: payload: the resource json to be created in ENM
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        result = False
        link = self.rescreate_url.format(cmhandle=cmhandle)+f'?resourceIdentifier={resource}'
        self.header = {"Content-Type": "application/yang-data+json"}
        resp = self.eiap.eiap_rest_call(POST, link, inputs=payload, refresh_auth=refresh_auth)
        if resp:
            if 'message' in resp:
                logging.warning('NCMP resource create request failed with: %s', resp['message'])
            else:
                result = True
        return result

    def create_object(self, node, nmo, attr):
        '''
        @brief Call EIAP NCMP endpoint to create a resource verifying its existence before
        :param: nmo: the node MO object to be created
        :param: attr: the payload with attributes for creation
        '''
        objects = self.get_mo_ids(node, nmo)
        if str(nmo.oid) in objects:
            logging.warning("%s %s object is already present. Creation skipped.",
            nmo.shortname, nmo.oid)
        else:
            payload = nmo.get_payload(attr)
            logging.info("Creating the %s MO %s", nmo.shortname, nmo.oid)
            logging.debug("Creating the %s MO under %s with payload:\n%s",
                nmo.shortname, nmo.get_parentresname(), payload)
            self.create_resource(node.cmhandle, nmo.get_parentresname(), payload)

    def patch_object(self, node, nmo, attr):
        '''
        @brief Call EIAP NCMP endpoint to patch a resource
        :param: nmo: the node MO object to be patched
        :param: attr: the payload with attributes for patching
        '''
        payload = nmo.get_payload(attr)
        logging.info("Patching the %s MO : %s", nmo.shortname, nmo.oid)
        logging.debug("Patching the %s MO under %s with payload:\n%s", nmo.shortname, nmo.get_parentresname(), payload)
        self.patch_resource(node.cmhandle, nmo.get_parentresname(), payload)

    def check_gnodeb_in_enm(self, node):
        '''
        @brief Call EIAP NCMP endpoint to query gnodeb data
        :param: node: the node to be queried
        :return: list of cell local ids
        '''
        result = []
        if node:
            logging.debug("Collecting details about node %s: %s", node.cmhandle, node.shortname)
            plmnidlist = []
            gnbdu_plmnid = None
            gnbdu_mo = MO(GNBDU_FUNCTION, 1)
            nmo = MO(NRCELLDU, 1, gnbdu_mo)
            resp = self.get_resource(node.cmhandle, nmo.get_parentresname(), get_fields([
                    f'attributes({GNBDU_PLMNID})',
                    pack_field_and_attributes(NRCELLDU, [A_PLMNIDLIST, A_CELLLOCID])
                   ]))
            if resp and GNBDU_FUNCTION in resp and isinstance(resp[GNBDU_FUNCTION], list):
                gnbdu_plmnid = resp[GNBDU_FUNCTION][0].get('attributes',{}).get(GNBDU_PLMNID,'')
                if NRCELLDU in resp[GNBDU_FUNCTION][0]:
                    for item in resp[GNBDU_FUNCTION][0][NRCELLDU]:
                        attr = get_attribute(item, A_PLMNIDLIST)
                        if attr not in plmnidlist:
                            plmnidlist.append(attr)
                        cellid = get_attribute(item, A_CELLLOCID)
                        result.append(cellid)
            logging.info("Node %s: %s >>\n GNBDUFunction PLMN id: %s\n NRCellDU PLMN ID list: %s",
                          node.cmhandle, node.shortname, gnbdu_plmnid, plmnidlist)
            node.plmnids = plmnidlist
        return result

    def get_gnodeb_details(self, nodes):
        '''
        @brief Call EIAP NCMP endpoint to query gnodeb data
        :param: nodes: the nodes to be queried
        '''
        if not nodes:
            return
        for node in nodes.values():
            logging.debug("Collecting details about node %s", node)
            arfcn_dl = []
            plmnidlist = []
            resp = self.get_resource(node.cmhandle, get_resource(GNBDU_FUNCTION), get_fields([
                    pack_field_and_attributes(NRCELLDU,[A_PLMNIDLIST]),
                    pack_field_and_attributes(NRSECTORCARRIER, [A_ARFCNDL])
                   ]))
            if resp and GNBDU_FUNCTION in resp and isinstance(resp[GNBDU_FUNCTION], list):
                if NRCELLDU in resp[GNBDU_FUNCTION][0]:
                    for item in resp[GNBDU_FUNCTION][0][NRCELLDU]:
                        attr = get_attribute(item, A_PLMNIDLIST)
                        if attr not in plmnidlist:
                            plmnidlist.append(attr)
                if NRSECTORCARRIER in resp[GNBDU_FUNCTION][0]:
                    for item in resp[GNBDU_FUNCTION][0][NRSECTORCARRIER]:
                        attr = get_attribute(item, A_ARFCNDL)
                        if attr not in arfcn_dl:
                            arfcn_dl.append(attr)
            logging.info("Node %s >> DL frequency: %s , PLMN ID list: %s", node.cmhandle,
                arfcn_dl, plmnidlist)
            node.plmnids = plmnidlist
            node.frequencies = arfcn_dl

    def get_gutran_sycnfreq(self, node):
        '''
        @brief Call EIAP NCMP endpoint to query enodeb data gUtranSyncSignalFrequency
        :param: node: the node to be queried
        '''
        result = None
        if node:
            logging.debug("Collecting gUtranSyncSignalFrequency id for node %s", node)
            gutrassfreq = []
            resp = self.get_resource(node.cmhandle, get_resource(ENODEB_FUNCTION), get_fields([
                              GUTRANW, pack_field_and_attributes(GUTRANSSFREQ,
                              [A_ARFCN,A_GUTRANSSFR])],'/'))
            if resp:
                data = key_find(resp,GUTRANSSFREQ)
                if data:
                    for item in data:
                        attr = get_attribute(item, A_GUTRANSSFR)
                        if attr and attr not in gutrassfreq:
                            gutrassfreq.append(attr)
                logging.info("Node %s >> GUtraN SS frequencies: %s", node.cmhandle,
                    gutrassfreq)
            result = gutrassfreq
        return result

    def get_object_ids(self, node, object_type):
        '''
        @brief Call EIAP NCMP endpoint to query node object
        :param: node: the node to be queried
        :param: object_type: the object type to be queried
        '''
        result = None
        if node:
            logging.info("Collecting %s ids for node %s", object_type, node.shortname)
            objects = []
            resp = self.get_resource(node.cmhandle, '', get_fields([object_type]))
            logging.info(json.dumps(resp, indent = 4))
            resp = key_find(resp, object_type)
            if resp:
                for item in resp:
                    objects.append(item.get('id',''))
            logging.info("%s %s objects found", len(objects), object_type)
            result = objects
        return result

    def get_mo_ids(self, node, nmo):
        '''
        @brief Call EIAP NCMP endpoint to query node object
        :param: node: the node to be queried
        :param: nmo: the node MO object to be queried
        '''
        result = None
        if node:
            logging.info("Collecting %s ids", nmo.shortname)
            objects = []
            resp = self.get_resource(node.cmhandle, nmo.get_parentresname(), get_fields([nmo.name]))
            logging.debug(json.dumps(resp, indent = 4))
            resp = key_find(resp, nmo.name)
            if resp:
                for item in resp:
                    objects.append(item.get('id',''))
            logging.debug("%s %s objects found on %s", objects, nmo.shortname, node.shortname)
            result = objects
        return result

    def check_enodeb_function(self, node):
        '''
        @brief Call EIAP NCMP endpoint to query enodeb data
        :param: nodes: the nodes to be queried
        '''
        resp = None
        if node:
            logging.debug("Collecting GUtraNW for node %s", node)
            resp = self.get_resource(node.cmhandle, get_resource(ENODEB_FUNCTION),
                        get_fields([GUTRANW]))
        return resp

    def get_gutranwk(self, nodes):
        '''
        @brief Call EIAP NCMP endpoint to query GUtraNWK data
        :param: nodes: the nodes to be queried
        '''
        if nodes:
            for node in nodes.values():
                logging.debug("Collecting GUTRA network for node %s", node)
                self.get_resource(node.cmhandle, self.gutranwk_opt)

    def get_termpointgnb(self, nodes):
        '''
        @brief Call EIAP NCMP endpoint to query TermPointGNBDU data
        :param: nodes: the nodes to be queried
        '''
        if nodes:
            for node in nodes.values():
                logging.debug("Collecting details about TermPointGNBDU for node %s", node)
                self.get_resource(node.cmhandle, self.termpoint_opt)

    def get_nrcellcu(self, nodes):
        '''
        @brief Call EIAP NCMP endpoint to query NRCellCU data
        :param: nodes: the nodes to be queried
        '''
        if nodes:
            for node in nodes.values():
                logging.debug("Collecting NRCellCU details for node %s", node)
                self.get_resource(node.cmhandle, self.nrcellcu_opt)

    def get_function(self, node, function, field):
        '''
        @brief Call EIAP NCMP endpoint to query resource data
        :param: node: the node to be queried
        '''
        result = None
        if node:
            logging.debug("Collecting %s function details for node %s", function, node)
            resp = self.get_resource(node.cmhandle, get_resource(function), options=get_fields([field]))
            if resp:
                result = key_find(resp, field)
        return result

    def delete_function(self, node, resource):
        '''
        @brief Call EIAP NCMP endpoint to delete resource data
        :param: node: the node to be queried
        '''
        result = None
        if node:
            logging.debug("Deleting resource for node %s with this definition: %s",
                 node, json.dumps(resource))
            result = self.delete_resource(node.cmhandle, resource)
        return result

    def populate_gnodeb(self, node, frequency, do_gnbcucp = True,
                        do_gnbdu = True, do_transport = True):
        '''
        @brief Call EIAP NCMP endpoint to create a set of resources for GNBDU node
        :param: node: the node to be modified
        :param: frequency: the frequency base to be used
        :param: do_gnbcucp: flag to enable/disable GNBCUCP MO modifications
        :param: do_gnbdu: flag to enable/disable GNBDU MO modifications
        :param: do_transport: flag to enable/disable TRANSPORT MO modifications
        '''
        if node:
            logging.info("Processing node %s : %s ...",node.cmhandle, node.shortname)

            transport_mo = MO(TRANSPORT, 1)
            router_mo = MO(ROUTER, "VR_INNER", transport_mo)
            ipintf_mo = MO(IP_INTERFACE,"NRAT_CP", router_mo)
            ipaddr_mo = MO(IP_ADDRESS,"IPCNR_1", ipintf_mo)
            sctppr_mo = MO(SCTP_PROFILE,"NRAT", transport_mo)
            sctpep_mo = MO(SCTP_ENDPOINT,"NRAT", transport_mo)

            if do_transport:
                ip_addr = f"10.55.168.{node.name.rsplit('/',1)[0][-1]}/29"

                self.create_object(node, router_mo, {"routerId": router_mo.oid})
                self.create_object(node, ipintf_mo, {"interfaceIPv4Id": ipintf_mo.oid})
                self.create_object(node, ipaddr_mo, {"addressIPv4Id": ipaddr_mo.oid, "address": ip_addr})

                self.create_object(node, sctppr_mo, {"sctpProfileId": sctppr_mo.oid})
                self.create_object(node, sctpep_mo, {"sctpEndpointId": sctpep_mo.oid,
                     "localIpAddress": [f"{node.longfdn},{ipaddr_mo.get_refname()}"],
                     "sctpProfile": f"{node.longfdn},{sctppr_mo.get_refname()}",
                     "portNumber": 333})

            gnbcucp_mo    = MO(GNBCUCP_FUNCTION, 1)
            gnbcuup_mo    = MO(GNBCUUP_FUNCTION, 1)
            endp_mo       = MO(ENDPOINT, 1, gnbcucp_mo)
            lclsctpep_mo  = MO(LOCAL_SCTP_ENDPOINT, "VR_INNER", endp_mo)
            respartmbr_mo = MO(RES_PARTITION_MEMBER, 1, MO(RES_PARTITION, 1, MO(RES_PARTITIONS, 1, gnbcucp_mo)))

            if do_gnbcucp:
                self.create_object(node, endp_mo, {"endpointResourceId": endp_mo.oid,
                          "userLabel": "1"})
                self.create_object(node, lclsctpep_mo, {"localSctpEndpointId": lclsctpep_mo.oid,
                          "interfaceUsed": "X2",
                          "sctpEndpointRef": f"{node.longfdn},{sctpep_mo.get_refname()}"})
                if node.plmnids:
                    self.patch_object(node, gnbcucp_mo, {"pLMNId": node.plmnids[0]})
                    self.patch_object(node, gnbcuup_mo, {"pLMNIdList": node.plmnids})
                    self.patch_object(node, respartmbr_mo, {"pLMNIdList": node.plmnids,
                         "endpointResourceRef": f"{node.longfdn},{endp_mo.get_refname()}"})

            gnbdu_mo      = MO(GNBDU_FUNCTION, 1)
            if do_gnbdu:
                for i in range(1, 5):
                    nrsectcarr_mo = MO(NRSECTORCARRIER, f'{i}', gnbdu_mo)
                    nrcelldu_mo   = MO(NRCELLDU, f"{node.shortname}-{i}", gnbdu_mo)
                    self.patch_object(node, nrsectcarr_mo, {"arfcnDL": frequency, "arfcnUL": 0})
                    if node.plmnids:
                        self.patch_object(node, nrcelldu_mo, {"nRSectorCarrierRef": [
                            f"{node.longfdn},{nrsectcarr_mo.get_refname()}"],
                            "pLMNIdList": node.plmnids})

    def populate_gnodeb_multiple_plmn(self, node, frequency, do_gnbcucp = True,
                        do_gnbdu = True, do_transport = True):
        '''
        @brief Call EIAP NCMP endpoint to create a set of resources for GNBDU node
        :param: node: the node to be modified
        :param: frequency: the frequency base to be used
        :param: do_gnbcucp: flag to enable/disable GNBCUCP MO modifications
        :param: do_gnbdu: flag to enable/disable GNBDU MO modifications
        :param: do_transport: flag to enable/disable TRANSPORT MO modifications
        '''
        if node:
            logging.info("Processing node %s : %s ...",node.cmhandle, node.shortname)

            transport_mo = MO(TRANSPORT, 1)
            gnbcucp_mo   = MO(GNBCUCP_FUNCTION, 1)
            gnbcuup_mo   = MO(GNBCUUP_FUNCTION, 1)
            gnbdu_mo     = MO(GNBDU_FUNCTION, 1)
            sctppr_mo    = MO(SCTP_PROFILE,"NRAT", transport_mo)
            self.create_object(node, sctppr_mo, {"sctpProfileId": sctppr_mo.oid})

            resparts_mo = MO(RES_PARTITIONS, 1, gnbcucp_mo)
            respart_mo = MO(RES_PARTITION, 1, resparts_mo)

            if do_gnbcucp:
                self.create_object(node, resparts_mo, {"resourcePartitionsId": resparts_mo.oid})
                self.create_object(node, respart_mo, {"resourcePartitionId": respart_mo.oid})
                if node.plmnids:
                    self.patch_object(node, gnbcucp_mo, {"pLMNId": node.plmnids[0]})
                    self.patch_object(node, gnbcuup_mo, {"pLMNIdList": node.plmnids})

            for i in range(1,4):
                ip_addr = f"10.55.{node.name.rsplit('/',1)[0][-2]}.{i}/29"
                router_mo = MO(ROUTER, f"S{i}", transport_mo)
                ipintf_mo = MO(IP_INTERFACE, f"TN_A_S{i}", router_mo)
                ipaddr_mo = MO(IP_ADDRESS,f"IPCNR_{i}", ipintf_mo)
                sctpep_mo = MO(SCTP_ENDPOINT,f"NRAT_{i}", transport_mo)

                if do_transport:
                    self.create_object(node, router_mo, {"routerId": router_mo.oid})
                    self.create_object(node, ipintf_mo, {"interfaceIPv4Id": ipintf_mo.oid})
                    self.create_object(node, ipaddr_mo, {"addressIPv4Id": ipaddr_mo.oid, "address": ip_addr})
                    self.create_object(node, sctpep_mo, {"sctpEndpointId": sctpep_mo.oid,
                         "localIpAddress": [f"{node.longfdn},{ipaddr_mo.get_refname()}"],
                         "sctpProfile": f"{node.longfdn},{sctppr_mo.get_refname()}",
                         "portNumber": 333})

                endp_mo       = MO(ENDPOINT, f"PLMN{i}", gnbcucp_mo)
                lclsctpep_mo  = MO(LOCAL_SCTP_ENDPOINT, f"PLMN{i}", endp_mo)
                respartmbr_mo = MO(RES_PARTITION_MEMBER, f"PLMN_{i}", respart_mo)

                if do_gnbcucp:
                    self.create_object(node, endp_mo, {"endpointResourceId": endp_mo.oid,
                              "userLabel": endp_mo.oid})
                    self.create_object(node, lclsctpep_mo, {"localSctpEndpointId": lclsctpep_mo.oid,
                              "interfaceUsed": "X2",
                              "sctpEndpointRef": f"{node.longfdn},{sctpep_mo.get_refname()}"})
                    if node.plmnids:
                        self.create_object(node, respartmbr_mo, {"pLMNIdList": [node.plmnids[i-1]],
                              "endpointResourceRef": f"{node.longfdn},{endp_mo.get_refname()}",
                              "resourcePartitionMemberId": respartmbr_mo.oid})
                        self.patch_object(node, respartmbr_mo, {"pLMNIdList": [node.plmnids[i-1]],
                              "endpointResourceRef": f"{node.longfdn},{endp_mo.get_refname()}"})

            if do_gnbdu:
#                self.patch_object(node, gnbdu_mo, {"dUpLMNId": plmnid})
                for i in range(1, 5):
                    nrsectcarr_mo = MO(NRSECTORCARRIER, f'{i}', gnbdu_mo)
                    nrcelldu_mo   = MO(NRCELLDU, f'{node.shortname}-{i}', gnbdu_mo)
                    self.patch_object(node, nrsectcarr_mo, {"arfcnDL": frequency, "arfcnUL": 0})
                    if node.plmnids:
                        self.patch_object(node, nrcelldu_mo, {"nRSectorCarrierRef": [
                            f"{node.longfdn},{nrsectcarr_mo.get_refname()}"],
                            "pLMNIdList": node.plmnids})

    def create_test_gutranw_with_sync_frequencies(self, node, frequency):
        '''
        @brief Call EIAP NCMP endpoint to create a GUTRANW resource with a set
               of frequency parameters
        :param: node: the node to be modified
        :param: frequency: the frequency base to be used
        '''
        if node:
            logging.info("Processing node %s ...",node)
            enodebf_mo = MO(ENODEB_FUNCTION, 1)
            gutranw_mo = MO(GUTRANW, 1, enodebf_mo)
            gutssfr_mo = MO(GUTRANSSFREQ,"", gutranw_mo)
#            gutranw = self.get_mo_ids(node, GUTRANW)
            self.create_object(node, gutranw_mo, {"gUtraNetworkId": gutranw_mo.oid,"userLabel": "1"})

            freq_guard=1666
            smtc_scs=120
            smtc_periodicity=20
            smtc_offset=0
            smtc_duration=1
            gutranssfreq_objs = self.get_mo_ids(node, gutssfr_mo)
            for i in range(10):
                attr = {}
                freq = frequency + i*freq_guard
                attr["gUtranSyncSignalFrequencyId"] = freq
                attr["userLabel"] = attr["gUtranSyncSignalFrequencyId"]
#                attr["gUtranSyncSignalFrequencyId"] = \
#                     f'{freq}-{smtc_scs}-{smtc_periodicity}-{smtc_offset}-{smtc_duration}'
                if str(attr["userLabel"]) in gutranssfreq_objs:
                    logging.info("GUtranSyncSignalFrequency %s object already present. Creation skipped.",
                    attr["userLabel"])
                else:
                    attr["smtcScs"] = smtc_scs
                    attr["smtcPeriodicity"] = smtc_periodicity
                    attr["smtcOffset"] = smtc_offset
                    attr["smtcDuration"] = smtc_duration
                    attr["arfcn"] = freq
                    gutssfr_mo = MO(GUTRANSSFREQ,attr["userLabel"], gutranw_mo)
                    self.create_object(node, gutssfr_mo, attr)
