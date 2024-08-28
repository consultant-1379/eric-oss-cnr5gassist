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
This module covers EIAP Common Topology Service (CTS) API client functions
For more information check these links:
...
"""
import math
import random
import json
import copy
import logging
import logging.handlers
from lib.service.commons import GET, POST, ENMObject, Node, Cell, LTECell
from lib.service.eiap_api_gw import EIAP, EIAPConfig

child_map = {'enodeb': 'lteCells', 'gnbdu': 'nrCells',
             'ltecell': 'geographicSite', 'nrcell': 'gnbdu'}
parent_map = {'ltecell': 'eNodeB', 'nrcell': 'gnbdu'}
CTS_DATASYNC_ITEM = { \
    "$type": None, \
    "$action": "reconcile", \
    "$refId": None, \
    "name": None, \
    "externalId": None, \
    "status": "operating" \
    }
CTS_DATASYNC_OBJECT = {
    "type": "osl-adv/datasyncservice/process", \
    "jsonHolder": { \
        "type": "gs/jsonHolder", \
        "json": None \
       } \
    }

GEO_DATA_TYPE = "GeospatialCoords"
REGION_TYPE   = "Region"

def get_datasync_object(link_type, cell, item_id = None, obj_name = None , o_type = None):
    '''
    @brief CTS datasync object generator for CTS update
    :param: link_type: the CTS object type link
    :param: cell: the cell object
    :param: item_id: integer id for object naming
    :param: obj_name: created object name string
    :param: o_type: object type to be created or modified
    '''
    if obj_name:
        record_name   = f'{cell.name}/{obj_name}{item_id}'
        record_ext_id = f'{cell.externalId}/ericsson-enm-lrat:{obj_name}={item_id}'
    else:
        record_name   = f'{cell.name}'
        record_ext_id = f'{cell.externalId}'
    record = copy.deepcopy(CTS_DATASYNC_ITEM)
    record['$type']  = link_type
    record['$refId'] = record_name
    record['name']   = record_name
    record['externalId']  = record_ext_id
    if o_type:
        record['type'] = o_type
    return record

def coordinates_from_vector_offset(bearing, offset, coordinates):
    '''
    @brief generates a geographic point coordinates around the coordinates provided
    :param: bearing:
    :param: offset: the offset from the central point
    :param: coordinates: the central point coordinates
    '''
    #Destination point along great-circle given distance and bearing from start point
    r_globe = 6371
    offset = offset / (r_globe * 1000)
    bearing = bearing * math.pi / 180

    lon = coordinates[0] * math.pi / 180
    lat = coordinates[1] * math.pi / 180

    rlat = math.asin(math.sin(lat) * math.cos(offset) +
        math.cos(lat) * math.sin(offset) * math.cos(bearing))

    rlon = lon + math.atan2(math.sin(bearing) * math.sin(offset) *
        math.cos(lat),
        math.cos(offset) - math.sin(lat) *
        math.sin(rlat))

    return [rlon*180/math.pi, rlat*180/math.pi]



class CTS():
    """!@class CTS
        @brief Class representing an EAIP API rest call object
        @details This is a class that represents EIAP API rest call object with
        specific functions to initiate API calls and process responses
    """

    def __init__(self, cfg, user, apigw = EIAPConfig.APIGW,
                     eiap = None, endpoint_tag = EIAPConfig.CTS):
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
        self.ctw_url = f'https://{self.host}'+ endp_pfx+ endpoints['ctw']
        self.ctg_url = f'https://{self.host}'+ endp_pfx+ endpoints['ctg']
        self.datasync_url = f'https://{self.host}'+ endp_pfx+ endpoints['datasync']
        self.geo_qry = endpoints['geoqry']

        self.header = {"Content-Type": "application/json"}
        self.header["Accept"] = "application/json"

    def get_enm_objects(self, obj_type, params = None, refresh_auth = False):
        '''
        @brief Call EIAP CTS endpoint to query ENM objects (and cm handles)
        :param: obj_type: the cell type to be queried
        :param: params: the filtering parameters for the query
        :param: refresh_auth: the EIAP API GW token refresh flag
        '''
        link = f'{self.ctw_url}/{obj_type}'
        logging.info("Calling %s", link)
        result = self.eiap.eiap_rest_call(GET, link, params = params,
                      refresh_auth=refresh_auth)
        objects = []
        if result and isinstance(result, (list)):
            logging.debug("Result returned for %s:\n %s", link, json.dumps(result, indent=4))
            for item in result:
                objects.append(ENMObject(item))
        else:
            logging.debug("No %s node data found when calling %s", obj_type, link)
        return objects


    def get_cell_function(self, cell_type, cell_id, function = None,
        payload = None, host=None):
        '''
        @brief Call EIAP CTS endpoint to query cells (and cm handles)
        :param: cell_type: the cell type to be queried
        :param: cell_id: the cell id to be queried
        :param: function: the function to be queried
        :param: payload: additional payload for the search function
        :param: host: the host to be used for query instead the default one
        :param: use_name_search: flag to swith using name search instead CTS id
        '''
        srch_string = f'/{cell_id}'
        if function:
            srch_string = f'{srch_string}?fs.{function}'
        if host:
            link = f'{host}/{cell_type}{srch_string}'
        else:
            link = f'{self.ctw_url}/{cell_type}{srch_string}'
        result = self.eiap.eiap_rest_call(GET, link, inputs = payload)
        if result and isinstance(result, (list, dict)):
            logging.debug("Result returned for %s:\n %s", link, json.dumps(result, indent=4))
            if function:
                return result, result.get(function)
        else:
            logging.info("No %s cells/data found when calling %s", cell_type, link)
        return result, []

    def get_ltecell_geodata(self, cell_id):
        '''
        @brief Call EIAP CTS endpoint to query cells (and cm handles)
        :param: cell_id: the cell id to be queried
        '''
        result = []
        cell, sites = self.get_cell_function('ltecell', cell_id, 'geographicSite')
        cell = LTECell(cell)
        if sites:
            locations = []
            sites = [x['value'] for x in sites if x['mode'] == 'LOADED']
            if sites:
                locations = self.get_cell_function('geographicsite',
                                   sites[0]['id'], 'locatedAt', host = self.ctg_url)[1]
                if locations:
                    locations = [x['value'] for x in locations if x['mode'] == 'LOADED']
            if locations:
                result = locations[0]['geospatialData']['coordinates']
                logging.info("%s with id %s is located at: [%s,%s]",
                              cell.otype, cell.oid, result[0], result[1])
            else:
                logging.info('No location sites found for %s with id: [%s]',
                             cell.otype, cell.oid)
        return result

    def create_and_set_location(self, cell, distance, coordinates, item_id, use_random_angle = False):
        '''
        @brief Call EIAP CTS endpoint to query cells (and cm handles)
        :param: cell: the cell object to be queried
        :param: distance: the distance to be used as radius for location
        :param: coordinates: the coordinates to be used as central point
        :param: item_id: the id to be used for site or location id and name assignment
        :param: use_random_angle: flag to distribute the location sites around a circle instead of a line
        '''
        #creating the object
        logging.info("Creating and setting location for %s with id %s", cell.otype, cell.oid)
        location = get_datasync_object('ctg/geographicLocation',cell,item_id,'Location',GEO_DATA_TYPE)
        site     = get_datasync_object('ctg/geographicSite',    cell,item_id,'Site'    ,REGION_TYPE)
        if use_random_angle:
            angle = random.random()*360
        else:
            angle = 90
        c_coord = coordinates_from_vector_offset(angle, random.random()*distance, coordinates)
        location['geospatialData'] = { "type": "Point", "coordinates": c_coord }
        site['$locatedAt'] = [location['name']]
        if 'ltecell' in cell.otype:
            u_cell   = get_datasync_object('ctw/lteCell', cell)
            u_cell['$geographicSite'] = [site['name']]
            u_cell['FDDearfcnDl'] = 4
            u_cell['FDDearfcnUl'] = 18004
            u_cell['cellLocalId'] = item_id
            u_cell['status'] = "operating"
        else:
            u_cell   = get_datasync_object('ctw/nrCell', cell)
            u_cell['$geographicSite'] = [site['name']]
            u_cell['physicalCellIdentity'] = 0
            u_cell['trackingAreaCode'] = 999
            u_cell['localCellIdNci'] = item_id
        data2sync = copy.deepcopy(CTS_DATASYNC_OBJECT)
        data2sync['jsonHolder']['json'] = [location, site, u_cell]
        #calling CTS datasync endpoint
        result = None
        print(json.dumps(data2sync, indent = 4))
        resp = self.eiap.eiap_rest_call(POST, self.datasync_url, inputs = json.dumps(data2sync))
        if resp and isinstance(resp, (list, dict)):
            logging.debug("Geolocation datasync result is:\n %s", json.dumps(resp, indent=4))
            result = resp
        else:
            logging.info("Geolocation datasync for %s cell failed: %s", cell, resp)
        return result

    def get_geolocation(self, node):
        '''
        @brief Call EIAP CTS endpoint to query cells locattion
        :param: node: the node with cell dictionary with Cell objects
        '''
        locations = []
        if node and node.cells:
            for cell_id, cell in node.cells.items():
                logging.info("Getting geolocation for %s with id %s", cell.otype, cell_id)
                cell.location = self.get_ltecell_geodata(cell_id)
                if cell.location:
                    loc_value = cell.location[0]+cell.location[1]
                    if loc_value not in locations:
                        node.locations.append(cell.location)
                        locations.append(loc_value)


    def get_node_plmnids(self, node):
        '''
        @brief Call EIAP CTS endpoint to query node plmnids (wireless networks
        :param: node: the node (Node object) to be queried
        '''
        plmnids = {}
        netwks = self.get_cell_function(node.otype, node.oid, 'wirelessNetworks')[1]
        if netwks:
            netwks = [x['value'] for x in netwks if x['mode'] == 'LOADED']
            if netwks:
                plmnids = {f"{x['mcc']}{x['mnc']}": {'mcc': x['mcc'],
                           'mnc': x['mnc'],} for x in netwks}
            else:
                logging.warning('No wireless networks are defined for node id: [%s]', node.oid)
        return plmnids

    def get_plmnids(self, nodes):
        '''
        @brief Function to collect PLMN IDs from the nodes identified
        :param: nodes: the nodes list to collect PLMN IDs from
        '''
        for node in nodes.values():
            node.plmnids = self.get_node_plmnids(node)


    def get_cells_around(self, node, celltype, distance):
        '''
        @brief Call EIAP CTS endpoint to query nrcells around the LTE cells
        :param: node: the node with dictionary of locations to be queried
        :param: celltype: the cell type the function is searching for
        :param: distance: the radius in meters to be checked
        '''
        resp_cells = {}
        if node and node.locations:
            for location in node.locations:
                logging.info("Getting 5G neighbor cells %s meters around LTE node %s [%s]"\
                             "located at: Long. [%s] Lat. [%s]",
                             distance, node.name, node.oid, location[0], location[1])
                params = self.geo_qry.format(location[0], location[1], distance)
                print(f"Searching with params {params}")
                cells = self.get_enm_objects(celltype, params = params)
                if cells:
                    for cell in cells:
                        if cell.oid not in resp_cells:
                            cell.otype = celltype
                            resp_cells[cell.oid] = cell
        return resp_cells

    def assign_parents(self, cells):
        '''
        @brief Call EIAP CTS endpoint to query cells with parent nodes
        :param: cells: the dictionary of cells (nrCell or lteCell) to be queried
        '''
        resp_nodes = {}
        for cell_id, cell in cells.items():
            logging.info("Getting parent node for %s id [%s] ", cell.otype, cell.oid)
            parent_type = parent_map.get( cell.otype, '')
            nodes = self.get_cell_function(cell.otype, cell_id, parent_type)[1]
            if nodes:
                nodes = {x['value']['id']: Node(x['value']) for x in nodes if x['mode'] == 'LOADED'}
                resp_nodes.update(nodes)
                for gid in nodes.keys():
                    resp_nodes[gid].cells[cell.oid] = cell
                    resp_nodes[gid].otype = parent_type
        return resp_nodes

    def get_enm_node_with_cells(self, node_id, node_type = 'enodeb'):
        '''
        @brief Call EIAP CTS endpoint to query node with its cells
        :param: node_id: the node ID to be queried
        :param: node_type: the node type to be queried (enodeb, gnbdu)
        '''
        result = None
        child = child_map.get(node_type)
        if child:
            logging.info("Getting %s data for %s with id [%s]", child, node_type, node_id)
            fcell, childs = self.get_cell_function(node_type, node_id, child)
            if childs:
                result = Node(fcell)
                cells = {x['value']['id']: Cell(x['value'])
                         for x in childs if x['mode'] == 'LOADED'}
                if not cells:
                    logging.error('No %s are configured for %s [%s]', child, node_type, node_id)
                else:
                    result.cells = cells
        return result
