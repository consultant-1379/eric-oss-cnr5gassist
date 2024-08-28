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
This python program contains a set of functions that helps to interact with EIAP
REST interfaces for rApp specific tasks - onboarding, termination, calling NCMP
and CTS interface functions, CNR rApp REST functions etc.
The functions are allowing collecting pod characteristic timings (startup,
 upgrade, rollback, service outage) for a pod specified in the start-up parameters,
 do an rApp upgrade, query RAN network elements, and more.
It uses EIAP API Gateway to interact with the system.

Usage: eiap_utils.py [OPTIONS] COMMAND [ARGS]...

Options:
  --version                   Show the version and exit.
  -c, --config-file PATH      Specify the generic configuration file
  -k, --cfg TEXT              Specify the configuration section from config
                              file
  -kc, --kubecfg TEXT         Override kube config location set from the
                              configuration file
  -n, --namespace TEXT        Override namespace set form configuration file
  -cd, --cluster-domain TEXT  Override cluster host domain set from
                              configuration file
  -ih, --iam-host TEXT        Override IAM host set from configuration file
  -ah, --apigw-host TEXT      Override API GW host set from configuration file
  -cu, --cts-user TEXT        Override CTS user set from configuration file
  -nu, --ncmp-user TEXT       Override NCMP user set from configuration file
  -up, --user-pass TEXT       Override users generic password set from
                              configuration file
  -d, --debug                 Set logging level to debug
  -ssv, --skip-ssl-verify     Skip SSL key verification on all used REST
                              interfaces
  -h, --help                  Show this message and exit.

Commands:
  check-cts                      !@brief Initialize ENM node data using...
  check-enm-connections          !@brief Check ENM connection in the...
  cleanup-external-cells         !@brief Cleaning up test eNodeB nodes...
  cleanup-gnbdu-nrcelldu         !@brief Cleaning up GNBDU nodes from...
  cleanup-gnbdus                 !@brief Cleaning up GNBDU nodes from...
  delete-app                     !@brief Delete a deployed rApp :param...
  get-characteristics            !@brief Get pod characterisistics in a...
  proxy-service                  !@brief Proxy service for robustness test...
  get-enodeb-x2                  !@brief Getting enodeb TermPointToGNB...
  get-instantiated-apps          !@brief List instantiated apps querying...
  get-ncmp-resource              !@brief Getting NCMP resource data for a...
  get-nodes                      !@brief Getting enodeb or gnbdu nodes...
  get-onboarded-apps             !@brief List succesfully onboarded apps...
  init-staging-test-data-ncmp    !@brief Initialize ENM node data using...
  init-staging-test-geodata-cts  !@brief Initialize geolocation for test...
  instantiate-app                !@brief Instantiating onboarded app with...
  load-test                      !@brief Load testing the application...
  onboard-app                    !@brief Onboarding csar with App Manager...
  query-nrc                      !@brief Query CNR NRC request status...
  robustness-test-scaling        !@brief Testing scalability robustness...
  robustness-test-sigterm        !@brief Testing robustness of the...
  run-cnr-helper                 !@brief Running 5G CNR NRC algorithm and...
  run-nrc                        !@brief Running 5G CNR NRC algorithm and...
  start-nrc                      !@brief Start CNR NRC request and...
  terminate-app                  !@brief Terminate a deployed rApp :param...
  upgrade-app                    !@brief Upgrade a deployed rApp with...
  """
import sys
import os
import datetime
import copy
import json
import logging
import logging.handlers
import time
import urllib3
import click
import lib.service.rta_service as rtas
import lib.service.cts_service as ctss
import lib.service.cnr_service as cnrs
import lib.service.ncmp_service as ncmps
import lib.service.app_manager as appmgrs
from lib.service.commons import SETTINGS, DELIM, DEFAULT_LOG_FORMAT, load_config_file, save_config_file
from lib.service.eiap_api_gw import EIAP, EIAPConfig
import lib.service.kube_client as kubes
sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)),'lib'))

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])


######################################## main function ####################################
@click.group(context_settings=CONTEXT_SETTINGS)
@click.version_option(version='1.0.0')
@click.option('--config-file', '-c', help='Specify the generic configuration file',
      type=click.Path(exists=True), default=f'{os.path.dirname(os.path.abspath(__file__))}/config/eiap_utils.cfg')
@click.option('--cfg', '-k', default='default', help='Specify the configuration section from config file')
@click.option('--kubecfg', '-kc', help='Override kube config location set from the configuration file')
@click.option('--ignorekubecfg', '-ik', is_flag=True, help='Flag to enable/disable use of kubeconfig', default=False)
@click.option('--namespace', '-n', help='Override namespace set form configuration file')
@click.option('--cluster-domain', '-cd', help='Override cluster host domain set from configuration file')
@click.option('--iam-host', '-ih', help='Override IAM host set from configuration file')
@click.option('--apigw-host', '-ah', help='Override API GW host set from configuration file')
@click.option('--cts-user', '-cu', help='Override CTS user set from configuration file')
@click.option('--ncmp-user', '-nu', help='Override NCMP user set from configuration file')
@click.option('--user-pass', '-up', help='Override users generic password set from configuration file')
@click.option('--debug', '-d', is_flag=True, help='Set logging level to debug')
@click.option('--skip-ssl-verify', '-ssv', is_flag=True,
              help='Skip SSL key verification on all used REST interfaces')
@click.pass_context
def main(ctx, config_file, cfg, kubecfg, ignorekubecfg, namespace, cluster_domain, iam_host,
    apigw_host, cts_user, ncmp_user, user_pass, debug, skip_ssl_verify):
    """
    Main function to initialize logging and kube connection
    :param ctx: the click module context
    :param config_file: the configuration file to be used
    :param cfg: the configuration section in the configuration file
    :param kubecfg: Override kube config location set from the configuration file
    :param ignorekubecfg: Flag to enable/disable use of kubeconfig
    :param namespace: Override namespace set from the configuration file
    :param cluster_domain: Override cluster host domain set from the configuration file
    :param iam_host: Override IAM host set from the configuration file
    :param apigw_host: Override API GW host set from the configuration file
    :param cts_user: Override CTS user set from the configuration file
    :param ncmp_user: Override NCMP user set from the configuration file
    :param user_pass: Override users generic password set from the configuration file
    :param debug: flag to turn on debug mode
    :param skip_ssl_verify: flag to disable SSL certificate verification during https connect
    """

    SETTINGS["verify_certificates"] = not skip_ssl_verify
    if skip_ssl_verify:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    logfile = f"{os.path.dirname(os.path.abspath(__file__))}/data/log/eiap_utils.log"
    file_handler = logging.handlers.TimedRotatingFileHandler(logfile, 'D', 1, 5)
    console_handler = logging.StreamHandler()
    logging.basicConfig(format=DEFAULT_LOG_FORMAT,
                    level=logging.INFO,
                    handlers=[console_handler, file_handler])
    if debug:
        logging.getLogger().setLevel(logging.DEBUG)
        logging.debug("Debug mode enabled")

    logging.info("Starting application")
    config = load_config_file(config_file, yamltype=True)
    if not config or cfg not in config:
        logging.error("No configuration %s found in the %s file",
                      cfg, config_file)
        sys.exit(1)
    elif not ignorekubecfg and ('kubeconfig' not in config[cfg] or 'namespace' not in config[cfg]):
        logging.error(
            "No kube configuration found in the %s file %s section", config_file, cfg)
        sys.exit(1)
    else:
        ctx.obj = {}
        if not ignorekubecfg: 
            if not kubecfg:
                kubecfg = config[cfg].get('kubeconfig', None)
            ctx.obj['kube'] = kubes.KubernetesClient(kubecfg)
        else:
            ctx.obj['kube'] = None
        eiap_cfg = EIAPConfig(config[cfg])
        if not cluster_domain:
            if iam_host and '.' in iam_host:
                cluster_domain = iam_host.replace(iam_host.split('.')[0],'',1)
                iam_host   = iam_host.split('.')[0]
            if apigw_host and '.' in apigw_host:
                cluster_domain = apigw_host.replace(apigw_host.split('.')[0],'',1)
                apigw_host = apigw_host.split('.')[0]
        eiap_cfg.set_parameter(eiap_cfg.NS, namespace)
        eiap_cfg.set_parameter(eiap_cfg.DOMAIN, cluster_domain)
        eiap_cfg.set_parameter(eiap_cfg.IAM, iam_host, eiap_cfg.HOSTS)
        eiap_cfg.set_parameter(eiap_cfg.APIGW, apigw_host, eiap_cfg.HOSTS)
        eiap_cfg.set_parameter(eiap_cfg.CTS, cts_user, eiap_cfg.USERS)
        eiap_cfg.set_parameter(eiap_cfg.NCMP, ncmp_user, eiap_cfg.USERS)
        eiap_cfg.set_parameter(eiap_cfg.GPASS, user_pass)
        eiap_cfg.set_parameter('generic_endpoints', config['generic_endpoints'])
        for key in config:
            if 'test_data' in key:
                eiap_cfg.set_parameter(key, config[key])
        ctx.obj['cfg'] = eiap_cfg

######################### Onboarding csar with App Manager #####################################
@main.command()
@click.pass_context
@click.option('--csar-file', '-c', help='The csar file to be onboarded')
def onboard_app(ctx, csar_file):
    """!@brief Onboarding csar with App Manager
    :param ctx: the click module context
    :param csar-file: The csar file to be onboarded
    """
    cfg = ctx.obj['cfg']
    logging.info("\n%s Starting app onboarding with App Manager using %s csar file %s",
                 DELIM, csar_file, DELIM)
    appm = appmgrs.AppMGR(cfg, 'appmgr')
    resp = appm.onboard_csar_to_am(csar_file)
    if resp:
        if isinstance(resp, dict):
            logging.debug('Returned answer:\n %s', json.dumps(resp, indent=4))
            logging.info(
                "Returned answer:\n ID: %s -> %s-%s onboarding status: %s",
                 resp['id'], resp['name'], resp['version'], resp['status'])

        resp = appm.check_oapp_status( resp['id'], ['ONBOARDED', 'FAILED'])
        if resp:
            logging.info(
                'Returned answer:\n %s', json.dumps(resp, indent=4))
            if resp['status'] in ['ONBOARDED']:
                logging.info("%s onboarded with id: %s", csar_file, resp['id'])


######################### Start CNR NRC request and monitor the request status ##################################
@main.command()
@click.pass_context
@click.option('--payload-file', '-p', help='The NRC request payload json file')
@click.option('--cell-id', '-c', help='The enodeb cell id to start request for')
@click.option('--distance', '-d', help='The distance to be checked in meters', default=300)
@click.option('--frequency-filter', '-ff', help='The frequency filter dictionary')
def start_nrc(ctx, payload_file, cell_id, distance, frequency_filter):
    """!@brief Start CNR NRC request and monitor the request status
    :param ctx: the click module context
    :param payload_file: The NRC request json payload file
    :param cell_id: The node id to run the NRC request for
    :param distance: The distance to run the NRC request for
    :param frequency_filter: The frequency filter dictionary to be used
    """
    cfg = ctx.obj['cfg']
    eiap = EIAP(cfg, cfg.APIGW, 'cnr')
    cnr  = cnrs.CNR(cfg, 'cnr', eiap = eiap)
    req_id = None
    if cell_id:
        cell_id = str(cell_id).split(',')
    if payload_file:
        logging.info("\n%s Starting NRC request with %s payload file %s",
                     DELIM, payload_file, DELIM)
        req_id = cnr.start_nrc(load_config_file(payload_file))
    elif frequency_filter:
        freq_pairs = {}
        for freq in frequency_filter.split(':')[0].split(','):
            freq_pairs[freq]= frequency_filter.split(':')[1].split(',')
        logging.info(
        "\n%s Starting NRC request for cell id [ %s ] within a %sm circle with frequency filter %s %s",
              DELIM, cell_id, distance, freq_pairs, DELIM)
        payload = {'eNodeBIds': cell_id, "distance": distance, "freqPairs": freq_pairs}
        req_id = cnr.start_nrc(payload)
    else:
        logging.info("\n%s Starting NRC request for cell id [ %s ] within a %sm circle %s",
                    DELIM, cell_id, distance, DELIM)
        req_id = cnr.start_nrc({'eNodeBIds': cell_id, "distance": distance})
    if req_id:
        logging.info('Request stored with id: [ %s ]', req_id)
        resp = cnr.wait_request_result(req_id, 75)
        if resp:
            grouping = resp.get("grouping")
            if not (grouping and len(grouping)>0):
                return
            logging.info(
                "\n%s Getting the cmHandle for cell: %s %s",DELIM, cell_id, DELIM)
            cts  = ctss.CTS(cfg, 'cts', eiap = eiap)
            ncmp = ncmps.NCMP(cfg, 'ncmp', eiap = eiap)
            node, cells = cts.get_cell_function('enodeb', cell_id)
            if not (cells and isinstance(cells, dict)):
                return
            logging.info(
                "\n%s Getting the TermPointToGNB for cmHandle: %s %s",DELIM, node.cmhandle, DELIM)
            time.sleep(5)
            resource = ncmp.get_resource(node.cmhandle, eiap.endpoints['ncmp_x2_params'])
            if resource and isinstance(resource, (dict, list)):
                if node.cmhandle:
                    print(json.dumps(resource, indent=4))
            else:
                logging.info("No TermPointToGNB found for %s.", node.cmhandle)
                if resource:
                    logging.info("The response was this: %s.", resource.text)

######################### Query CNR NRC request status through monitoring endpoint ##################################
@main.command()
@click.pass_context
@click.option('--monitor-id', '-m', help='The NRC request monitoring id')
def query_nrc(ctx, monitor_id):
    """!@brief Query CNR NRC request status through monitoring endpoint
    :param ctx: the click module context
    :param monitor_id: The NRC request monitoring id
    """
    cfg = ctx.obj['cfg']
    cnr  = cnrs.CNR(cfg, 'cnr')
    if monitor_id:
        logging.info('Querying request stored with id: [ %s ]', monitor_id)
        resp = cnr.wait_request_result(monitor_id, 2)
        if resp:
            grouping = resp.get("allNrcNeighbors")
            print(json.dumps(grouping, indent =4))
            enmupdates = resp.get("enmUpdates", {})
            print(json.dumps(enmupdates, indent =4))
    else:
        logging.info('Querying request ids from CNR')
        resp = cnr.get_req_result('')
        if resp:
            print(json.dumps(resp, indent =4))

######################### Instantiating app with App Manager #####################################
@main.command()
@click.pass_context
@click.option('--app-id', '-i', help='The app id to be instantiated')
@click.option('--payload-file', '-p', help='The site_values payload json file name')
def instantiate_app(ctx, app_id, payload_file):
    """!@brief Instantiating onboarded app with App Manager
    :param ctx: the click module context
    :param app_id: the onboarded application id
    :param payload_file: the site values payload file
    """
    logging.info(
        "\n%s Starting app instantiation with App Manager using app ID: %s and file as payload %s %s",
         DELIM, app_id, payload_file, DELIM)
    appm = appmgrs.AppMGR(ctx.obj['cfg'], 'appmgr')

    result = appm.get_onboarded_app(app_id)
    if result and result.get('mode') and "DISABLED" in result.get('mode'):
        appm.enable_onboarded_app(app_id)
    appm.instantiate_with_am(app_id, load_config_file(payload_file), wait = 80)


######################### Getting the list of instantiated apps #####################################
@main.command()
@click.pass_context
@click.option('--app-name', '-n', help='The app name to be checked')
@click.option('--list-all', '-a', is_flag=True, help='List all statuses')
def get_instantiated_apps(ctx, app_name, list_all):
    """!@brief List instantiated apps querying App LCM
    :param ctx: the click module context
    :param app_name: the application name to be checked
    :param list_all: list all records in any status
    """
    logging.info("\n%s Getting the list of instantiated apps %s",DELIM, DELIM)
    appm = appmgrs.AppMGR(ctx.obj['cfg'], 'appmgr')

    resp = appm.get_instantiated_app_byname(app_name, list_all)
    for item in resp:
        logging.info("  [%s]  ID: %s -> Onboard id: %s instantiation status: %s",
                item['name'], item['id'], item['appOnBoardingAppId'], item['healthStatus'])


###################### Getting the list of successfully onboarded apps ##############################
@main.command()
@click.pass_context
@click.option('--list-all', '-a', is_flag=True, help='List all statuses')
def get_onboarded_apps(ctx, list_all):
    """!@brief List succesfully onboarded apps querying App Onboarding
    :param ctx: the click module context
    :param list_all: list all records in any status
    """
    logging.info("\n%s Getting the list of onboarded apps %s",DELIM, DELIM)
    appm = appmgrs.AppMGR(ctx.obj['cfg'], 'appmgr')

    resp = appm.get_onboarded_app("")
    if resp and isinstance(resp, list):
        for item in resp:
            if "ONBOARDED" in item['status'] or list_all:
                logging.info(
                    "    Onboarding ID: %s -> %s-%s: %s",
                    item['id'], item['name'], item['version'], item['status'])
    else:
        logging.info("No application found in the onboarding database ...")


######################### Getting the list of enodeb and gnbdu cells #####################################
@main.command()
@click.pass_context
@click.option('--cmhandle', '-c', help='The cmhandle to be queried')
@click.option('--resource', '-r', help='The resource to be queried')
def get_ncmp_resource(ctx, cmhandle, resource):
    """!@brief Getting NCMP resource data for a cmHandle
    :param ctx: the click module context
    :param cmhandle: the ENM object cmhandle id
    :param resource: the resource to be queried
    """
    logging.info("\n%s Getting the NCMP resource for cmHandle: %s %s",DELIM, cmhandle, DELIM)
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')

    resource = ncmp.get_resource(cmhandle, resource)
    if resource and isinstance(resource, (dict, list)):
        if cmhandle:
            print(json.dumps(resource, indent=4))
    else:
        logging.info("No resource found for %s: %s", cmhandle, resource)


######################### Getting the list of TermPointToGNB for cmhandle ###############################
@main.command()
@click.pass_context
@click.option('--cmhandle', '-c', help='The cmhandle to be queried')
@click.option('--gnodebid', '-g', help='The gNodeB id to be queried')
def get_enodeb_x2(ctx, cmhandle, gnodebid):
    """!@brief Getting enodeb TermPointToGNB data for a cmHandle
    :param ctx: the click module context
    :param cmhandle: the ENM object cmhandle id
    :param gnodebid: the GNodeB id to be queried
    """
    logging.info("\n%s Getting the TermPointToGNB for cmHandle: %s %s",DELIM, cmhandle, DELIM)
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    params = ncmp.endpoints['x2_res']
    if gnodebid:
        params = ncmp.endpoints['xt_res'].format(gnodebid=gnodebid)
    resource = ncmp.get_resource(cmhandle, params)
    if resource and isinstance(resource, (dict, list)):
        if cmhandle:
            print(json.dumps(resource, indent=4))
    else:
        logging.info("No TermPointToGNB found for %s.", cmhandle)
        if resource:
            logging.info("The response was this: %s.", resource.text)


######################### Getting the list of enodeb and gnbdu cells #####################################
@main.command()
@click.pass_context
@click.option('--node-type', '-t', help='The cell type to be queried')
@click.option('--node-id', '-n', help='The cell id to be queried')
@click.option('--max-nodes', '-m', default=50, help='The maximum number of nodes shown')
def get_nodes(ctx, node_type, node_id, max_nodes):
    """!@brief Getting enodeb or gnbdu nodes from CTS
    :param ctx: the click module context
    :param node_type: the queried node type
    :param node_id: the queried node id
    :param max_nodes: the maximum number of nodes shown
    """
    logging.info("\n%s Getting node/cell list from CTS %s",DELIM, DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')

    if node_id:
        node = cts.get_enm_node_with_cells(node_id, node_type)
        if node and node.cells:
            logging.info("Nodes returned:\n %s", node)
            for cell_id, cell in node.cells.items():
                logging.info("    child cell %s %4s-> %4s", cell.otype, cell_id,
                    str(cell).replace(node.name, ''))
    else:
        nodes = cts.get_enm_objects(node_type)
        logging.info("Nodes found %s", len(nodes) if nodes else nodes)
        if nodes:
            if len(nodes)>max_nodes:
                logging.info("Showing first %s nodes only.", max_nodes)
            for i in range(min(len(nodes),max_nodes)):
                print(f"{nodes[i]}")


######################### Run CNR logic #####################################
@main.command()
@click.pass_context
@click.option('--distance', '-d', help='The distance to be analysed')
@click.option('--enodeb-id', '-e', help='The enodeb id to be queried')
def run_nrc(ctx, enodeb_id, distance):
    """!@brief Running 5G CNR NRC algorithm and external cell creation
    :param ctx: the click module context
    :param enodeb_id: The node id to run the NRC request for
    :param distance: The distance to run the NRC request for
    """

    logging.info("\n%s Running CNR logic with cell id %s within %s m distance %s",
                 DELIM, enodeb_id, distance, DELIM)
    cts  = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')

    enodeb = cts.get_enm_node_with_cells(enodeb_id)
    if enodeb:
        cts.get_geolocation(enodeb)
        nrcells = cts.get_cells_around(enodeb, 'nrcell', distance)
        enodeb.neighbors = cts.assign_parents(nrcells)
        cts.get_plmnids(enodeb.neighbors)
        if enodeb.neighbors:
            for gid, gnodeb in enodeb.neighbors.items():
                logging.info("gNodeB nodes identified in %sm range around eNodeB [%s]: %s -> [%s]",
                             distance, enodeb.name, gid, [str(x) for x in gnodeb.cells.values()])
        logging.info("\n%s Collecting information for external cell creation %s",
                     DELIM, DELIM)
        ncmp.get_gnodeb_details(enodeb.neighbors)
        for gid, gnodeb in enodeb.neighbors.items():
            ncmp.create_object(enodeb, ncmps.EXTGNODEB_FUNCTION, gnodeb.shortname,
                           {'userLabel': gnodeb.shortname, 'externalGNodeBFunctionId': gnodeb.shortname,
                            "gnodeBId": int(gnodeb.shortname[-5]),
                            "gNodeBPlmnId": {'mncLength':'2' ,'mcc': '228', 'mnc': '49'}},
                           [[ncmps.ENODEB_FUNCTION,1],[ncmps.GUTRANW,1]])
#        ncmp.get_termpointgnb(enodeb.neighbors)
#        ncmp.get_nrcellcu(enodeb.neighbors)
    sys.exit(0)
    if enodeb and enodeb.neighbors:
        for node in enodeb.neighbors:
            logging.info('Checking eNodeB [%s] with cmHandle [%s] x2 links toward gNodeB [%s]',
                         node.oid, node.cmhandle, node.name)
            resource = ncmp.get_resource(node, ncmp.endpoints['x2_params'])
            if resource and isinstance(resource, (dict, list)):
                if node.cmhandle:
                    print(json.dumps(resource, indent=4))
            else:
                logging.info("No TermPointToGNB found for %s.", node.cmhandle)
                if resource:
                    logging.info("The response was this: %s.", resource.text)


######################### CNR helper #####################################
@main.command()
@click.pass_context
@click.option('--gnodeb-id', '-g', help='The gnodeb id to be queried')
@click.option('--enodeb-id', '-e', help='The gnodeb id to be queried')
def run_cnr_helper(ctx, gnodeb_id, enodeb_id):
    """!@brief Running 5G CNR NRC algorithm and external cell creation
    :param ctx: the click module context
    :param gnodeb_id: The GNodeB id to test with
    :param enodeb_id: The ENodeB id to test with
    """

    logging.info("\n%s Running CNR logic helper functions for node id [%s] %s",
                 DELIM, gnodeb_id, DELIM)
    cts  = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp', eiap=cts.eiap)

    if gnodeb_id:
        gnbdu = {gnodeb_id: cts.get_enm_node_with_cells(gnodeb_id, 'gnbdu')}
#        print(json.dumps(ncmp.get_resource(gnbdu[gnodeb_id].cmhandle,
#               ncmps.RES_GNBDU_FUNCTION), indent=4))
#        print(json.dumps(ncmp.get_resource(gnbdu[gnodeb_id].cmhandle,
#               ncmps.RES_GNBDU_FUNCTION+ncmps.RES_NRSECTORCARRIER), indent=4))
        print(json.dumps(ncmp.get_gnodeb_details(gnbdu), indent=4))
    if enodeb_id:
        enodeb = {enodeb_id: cts.get_enm_node_with_cells(enodeb_id, 'enodeb')}
        print(json.dumps(enodeb, indent=4))


#    ncmp.get_gnodeb_details(gnbdu)
#    ncmp.get_gutranwk(gnbdu)
#    ncmp.get_termpointgnb(gnbdu)
#    ncmp.get_nrcellcu(gnbdu)

######################### Get ADP characteristics function #####################################
@main.command()
@click.pass_context
@click.option('--app-name', '-n',
              help='The deployed app name to get characteristics for', default=r'eric-oss-5gcnr')
@click.option('--container-name', '-c',
              help='The deployed pod container name to get characteristics for', default=r'eric-oss-cnr5gassist')
@click.option('--payload-file', '-p', help='The site_values payload json file name')
@click.option('--template-file', '-t', help='The json template file to generate report from',
    type=click.Path(exists=True),
    default=f'{os.path.dirname(os.path.abspath(__file__))}/config/5GCNR_characteristic_report_template.json')
@click.option('--output-file', '-o', help='The json report',
    type=click.Path(exists=False),
    default=f'{os.path.dirname(os.path.abspath(__file__))}/config/eric-oss-5gcnr_characteristic_report.json')
def get_characteristics(ctx, app_name, container_name, payload_file, template_file, output_file):
    """!@brief Get pod characterisistics in a kube environment
    :param ctx: the click module context
    :param app_name: The deployed app name to get characteristics for
    :param container_name: The deployed app container name to get characteristics for
    :param payload_file: The site_values payload json file name
    :param template_file: The template used for characteristic report generation
    :param output_file: The output file name used for characteristic report generation
    """
    def update_characteristic_json(charact_data, section_id, timestart, timeend, msg):
        """ Updating the json with the seconds elapsed
        :param charact_data: the json stucture that has to be updated
        :param ID: the ID of section that has to be updated
        """
        if timestart and timeend:
            for item in charact_data['results']:
                if item.get('id')==section_id:
                    item['duration'] = (timeend-timestart).total_seconds()
            logging.info(msg, (timeend-timestart).total_seconds())
        else:
            logging.info('Not enough data for filling out duration for section %s', section_id)

    cfg = ctx.obj['cfg']
    # getting init time
    charact_json = load_config_file(template_file)
    charact_data = charact_json['ADP-Microservice-Characteristics-Report']
    charact_data['test-environment']['cluster'] = ctx.obj['kube'].get_cluster_name()
    logging.info("Using kube cluster %s", ctx.obj['kube'].get_cluster_name())
    logging.info("\n%s Getting pod description %s", DELIM, DELIM)
    pods = ctx.obj['kube'].get_pod_data(cfg.cfg[cfg.NS], app_name)
    if not pods:
        logging.info('No pods found for %s.', app_name)
        sys.exit(0)
    pod_data = pods[0].metadata
    charact_data['service']['name']    = pod_data.annotations['ericsson.com/product-name']
    charact_data['service']['version'] = pod_data.annotations['ericsson.com/product-revision']
    charact_data['resource-configuration'][0]['resources'] = []
    for pod in pods:
        podinfo = {'pod': pod.metadata.name, 'instances': len(pod.spec.containers), 'containers': []}
        for container in pod.spec.containers:
            podinfo['containers'].append(
                {'name': container.name,
                 'cpu-req': container.resources.requests['cpu'],
                 'cpu-limit': container.resources.limits['cpu'],
                 'mem-req': container.resources.requests['memory'],
                 'mem-limit': container.resources.limits['memory'],
                 "optional-container-metadata": {}
                 })
        charact_data['resource-configuration'][0]['resources'].append(podinfo)
        logging.info('This is what we get:\n %s', json.dumps(charact_data['resource-configuration'], indent=4))

    # getting init time
    logging.info("\n%s Collecting initialization time %s", DELIM, DELIM)
    pod_name, timestart, timeend = ctx.obj['kube'].get_pod_log_startup_timestamps(
              cfg.cfg[cfg.NS], app_name, container_name,
             'Starting CoreApplication',
             'Completed initialization' )
    update_characteristic_json(charact_data,0, timestart, timeend, '  Initialization time: %s')

    # getting startup time after pod is restarted
    logging.info("\n%s %s pod delete and restart time %s", DELIM, app_name, DELIM)
    if pod_name:
        ctx.obj['kube'].core_v1.delete_namespaced_pod(pod_name, cfg.cfg[cfg.NS])
        logging.info("  Waiting 80 seconds")
        time.sleep(80)
    pod_name, timestart, timeend = ctx.obj['kube'].get_pod_startup_timestamps(cfg.cfg[cfg.NS], app_name)
    update_characteristic_json(charact_data,1, timestart, timeend, "Pod restart time: %s")

    # getting pod rollback outage and startup times
    logging.info(
        "\n%s Upgrade/rollback release and test availability of service during it using app manager %s",
        DELIM, DELIM)
    cnr  = cnrs.CNR(cfg, 'cnr')
    timeend = None
    cnr.wait_health_status('UP',10)
    timestart = datetime.datetime.now()
    logging.info("Outage started at %s", timestart)
    appm = appmgrs.AppMGR(cfg, 'appmgr')
    resp = appm.get_instantiated_app_byname(app_name)
    if resp:
        inst = resp[0]
        if payload_file:
            payload = load_config_file(payload_file)
        else:
            payload = { "appId": "", "additionalParameters": json.loads(inst["additionalParameters"])}
        logging.info('Starting rollback/upgrade for app %s with id: %s',
                     inst['name'],inst['appOnBoardingAppId'])
        appm.terminate_instantiated_app(inst['id'], 60)
        resp = appm.instantiate_with_am(inst['appOnBoardingAppId'], payload, wait = 100)
        if resp:
            cnr.check_health(refresh_auth = True)
            status = cnr.wait_health_status('UP',100)
            if status:
                timeend = datetime.datetime.now()
                logging.info("Outage ended at %s", timeend)
            update_characteristic_json(charact_data, 2, timestart, timeend,
                                       'Container service outage time during upgrade: %s')
    else:
        logging.warning('App reinstantiation failed - no running %s app found', app_name)
    charact_json['ADP-Microservice-Characteristics-Report'] = charact_data
    save_config_file(output_file, charact_json)


######################### Proxy service for robustness test #####################################
@main.command()
@click.pass_context
@click.option('--app-name', '-n',
              help='The deployed app name to run the proxy service for', default=r'eric-oss-5gcnr')
@click.option('--container-name', '-c',
              help='The deployed pod container name to get characteristics for', default=r'eric-oss-cnr5gassist')
def proxy_service(ctx, app_name, container_name):
    from flask import Flask
    import signal

    app = Flask('__main__')

    @app.route('/health', methods=['GET'])
    def health():
        return json.dumps({'running': True}), 200, {'ContentType':'application/json'}

    @app.route('/restart_5gcnr', methods=['POST'])
    def restart_5gcnr():
        cfg = ctx.obj['cfg']
        pod_name, _, _ = ctx.obj['kube'].get_pod_log_startup_timestamps(
                  cfg.cfg[cfg.NS], app_name, container_name,
                 'Starting CoreApplication',
                 'Completed initialization' )
        resp_value = False
        resp_code  = 400
        if pod_name:
            logging.info("\nThe %s pod is up and running, trying to delete it", pod_name)
            ctx.obj['kube'].core_v1.delete_namespaced_pod(pod_name, cfg.cfg[cfg.NS])
            resp_value = True
            resp_code  = 200
        else:
            logging.info("\nCannot find running pod for %s", app_name)
        return json.dumps({'success': resp_value}), resp_code, {'ContentType':'application/json'}

    @app.route('/shutdown', methods=['POST'])
    def shutdown():
        logging.info("\nShuting down the proxy server...")
        sig = getattr(signal, 'SIGKILL', signal.SIGTERM)
        os.kill(os.getpid(), sig)

    app.run(host='0.0.0.0', port=8085)


######################### Load test wih 3 endpoints #####################################
@main.command()
@click.pass_context
@click.option('--app-name', '-n',
              help='The deployed app name to perform the load test for', default=r'eric-oss-5gcnr')
@click.option('--period', '-p', help='Load test period in seconds')
@click.option('--payload-file', '-r', help='The NRC payload json file name')
def load_test(ctx, app_name, period, payload_file):
    """!@brief Load testing the application
    :param ctx: the click module context
    :param app_name: The deployed app name to perform the load test for
    :param period: the load testing period in seconds
    :param payload_file: the payload file used for testing
    """
    def print_pod_usage(ctx, namespace, app_name):
        pod_usage = ctx.obj['kube'].get_pod_usage(namespace, app_name)
        for pod in pod_usage:
            for container in pod['containers']:
                logging.info('%s container usage in %s pod: CPU: %10s ; Memory: %s',
                     container['name'], pod['metadata']['name'],
                     container['usage']['cpu'], container['usage']['memory'])

    cnr = cnrs.CNR(ctx.obj['cfg'], 'cnr')

    # collection throughput and latency information on the service
    logging.info(
        "\n%s Collecting throughput and latency information on 5G CNR for %s seconds %s",
        DELIM, period, DELIM)
    logging.info(
        "\n%s by sending in NCR requests then querying results, both in a loop %s",
        DELIM, DELIM)
    cfg = ctx.obj['cfg']

    print_pod_usage(ctx, cfg.cfg[cfg.NS], app_name)
    timestart = datetime.datetime.now()
    timeend   = None
    period = int(period)
    periodexpired = False
    main_counter = 0
    monitor_counter = 0
    while not periodexpired:
        resp = cnr.start_nrc(load_config_file(payload_file))
        if resp:
            main_counter+=1
        resp = cnr.get_req_result('')
        if resp:
            monitor_counter+=1
        timeend = datetime.datetime.now()
        periodexpired = (timeend-timestart).total_seconds() > period
    logging.info("Elapsed time: %.2fs, main requests: %d, monitoring requests: %d, average: %.2f/s",
                 (timeend-timestart).total_seconds(), main_counter, monitor_counter,
                 (main_counter+monitor_counter)/(timeend-timestart).total_seconds())
    print_pod_usage(ctx, cfg.cfg[cfg.NS], app_name)

######################### Robustness test SIGTERM #####################################
@main.command()
@click.pass_context
@click.option('--app-name', '-n',
              help='The deployed app name to get robustness test for', default=r'eric-oss-5gcnr')
@click.option('--payload-file', '-r', help='The NRC payload json file name')
@click.option('--timeout-period', '-t', default=5, help='Liveliness timeout period in seconds')
def robustness_test_sigterm(ctx, app_name, timeout_period, payload_file):
    """!@brief Testing robustness of the application by:
         testing the liveliness and readiness probe functionality
         testing service restart during NRC request processing
    :param ctx: the click module context
    :param app_name: The deployed app name to get robustness test for
    :param timeout_period: the liveliness testing timout period in seconds
    :param payload_file: the payload file used for testing
    """
    def check_nrc(payload):
        logging.info('Starting NCR request')
        req_id = cnr.start_nrc(payload)
        if not req_id or len(req_id)>100:
            logging.warning("NRC request failed")
            sys.exit(1)
        else:
            logging.info('NRC request has been started with ID: %s', req_id)

    cfg = ctx.obj['cfg']
    cnr = cnrs.CNR(cfg, 'cnr')

    # collection throughput and latency information on the service
    logging.info(
    "\n%s Collecting liveliness and readiness test results during service restart for 5G CNR %s",
        DELIM, DELIM)
    logging.info(
        "\n%s by sending in NCR requests then sending SIGKILL to service %s",
        DELIM, DELIM)
    logging.info("\n%s Getting running pod description %s", DELIM, DELIM)
    pods = ctx.obj['kube'].get_pod_data(cfg.cfg[cfg.NS], app_name)
    if not pods:
        logging.info('No pods found for %s.', app_name)
        sys.exit(0)
    pod_name = pods[0].metadata.name
    timestart = datetime.datetime.now()
    timeend   = None
    check_nrc(load_config_file(payload_file))
    # terminating pod and checking for availability after it
    logging.info("\n%s Deleting %s pod and checking avaliablity %s", DELIM, app_name, DELIM)
    if pod_name:
        ctx.obj['kube'].core_v1.delete_namespaced_pod(pod_name, cfg.cfg[cfg.NS])
#    cnr.wait_request_result(req_id, timeout_period)
    resp = cnr.check_health(refresh_auth = True)
    if resp:
        timeend = datetime.datetime.now()
        logging.info("The application has responded in %s seconds after SIGTERM!",
            (timeend-timestart).total_seconds())
        if (timeend-timestart).total_seconds()<timeout_period:
            sys.exit(1)
    else:
        logging.info(
         "The application has been restarted instantly (e.g. not responding right after SIGTERM.")

######################### Robustness test SCALING #####################################
@main.command()
@click.pass_context
@click.option('--app-name', '-n',
              help='The deployed app name to get robustness test for', default=r'eric-oss-5gcnr')
@click.option('--payload-file', '-r', help='The NRC payload json file name')
@click.option('--instance-number', '-n', default=3, help='The number of instances to scale up to')
def robustness_test_scaling(ctx, app_name, instance_number, payload_file):
    """!@brief Testing scalability robustness of the application by:
         scaling up the pod numbers
         sending in NRC request
         scaling down the pod numbers
         sending in NRC request
    :param ctx: the click module context
    :param app_name: The deployed app name to get robustness test for
    :param instance_number: the number of instances to scale up to
    :param payload_file: the payload file used for testing
    """

    def check_nrc(payload):
        logging.info('Starting NCR request')
        req_id = cnr.start_nrc(payload)
        if not req_id or len(req_id)>100:
            logging.warning("NRC request failed")
            sys.exit(1)
        else:
            logging.info('NRC request has been started with ID: %s', req_id)

    def list_pods(cfg, app_name):
        logging.info("\n%s Getting running pod description for %s %s", DELIM, app_name, DELIM)
        pods = ctx.obj['kube'].get_pod_data(cfg.cfg[cfg.NS], app_name)
        if not pods:
            logging.info('No pods found for %s.', app_name)
            sys.exit(0)
        else:
            logging.info('Found %s running pods for %s.', len(pods), app_name)
        for pod in pods:
            logging.info('%s pod %s', app_name, pod.metadata.name)
        return pods

    cfg = ctx.obj['cfg']
    cnr = cnrs.CNR(cfg, 'cnr')

    # collection throughput and latency information on the service
    logging.info(
    "\n%s Testing scale-up and down of % service and sending in service request in between %s",
        DELIM, app_name, DELIM)
    pods = list_pods(app_name)
    check_nrc(load_config_file(payload_file))
    # scaling up pod and checking for availability after it
    logging.info("\n%s Scaling up %s deployment and checking functionality %s",
        DELIM, app_name, DELIM)
    if pods:
        ctx.obj['kube'].scale_deployment(app_name, cfg.cfg[cfg.NS], instance_number)
    time.sleep(70)
    list_pods(app_name)
    check_nrc(load_config_file(payload_file))
    logging.info("\n%s Scaling down %s deployment and checking functionality %s",
        DELIM, app_name, DELIM)
    if pods:
        ctx.obj['kube'].scale_deployment(app_name, cfg.cfg[cfg.NS], 1)
    time.sleep(30)
    list_pods(app_name)
    check_nrc(load_config_file(payload_file))

############################# Upgrade app function #######################################

@main.command()
@click.pass_context
@click.option('--app-name', '-n',
              help='The instantiated app name to upgrade', default=r'eric-oss-5gcnr')
@click.option('--oapp-version', '-v', help='The onboarded app version to upgrade to')
@click.option('--payload-file', '-p', help='The site_values payload json file name')
@click.option('--use-lcmupgrade', '-u', is_flag=True,
               help='Use the lcm upgrade feature vs terminate/instantiate')
def upgrade_app(ctx, app_name, oapp_version, payload_file, use_lcmupgrade):
    """!@brief Upgrade a deployed rApp with other onboarded version
    :param ctx: the click module context
    :param app_name: The instantiated app_name to be upgraded
    :param oapp_version: The onboarded app version to upgrade tp,
    :param payload_file: The site_values payload json file name
    :param use_lcmupgrade: Use the lcm upgrade feature vs terminate/instantiate
    @return: Prints out measured characteristics to stdout
    """

    cfg = ctx.obj['cfg']
    logging.info(
        "\n%s Upgrade/rollback release and test availability of service during it using app manager %s",
        DELIM, DELIM)
    cnr = cnrs.CNR(cfg,'cnr')

    timeend = None
    cnr.check_health()
    appm = appmgrs.AppMGR(cfg, 'appmgr')
    resp = appm.get_instantiated_app_byname(app_name)
    if resp:
        inst = resp[0]
        upgrade_id = inst['appOnBoardingAppId']
        instance_id = inst['id']
        if oapp_version:
            resp = appm.get_onboarded_app_byname(app_name, oapp_version)
            if resp:
                for item in resp:
                    if oapp_version in item['version']:
                        upgrade_id = item['id']
        if payload_file:
            payload = load_config_file(payload_file)
        else:
            payload = { "appId": "", "additionalParameters": json.loads(inst["additionalParameters"])}
        logging.info('Starting rollback/upgrade for app %s with id: %s',
                     inst['name'],inst['appOnBoardingAppId'])
        timestart = datetime.datetime.now()
        logging.info("Outage started at %s", timestart)
        if not use_lcmupgrade:
            appm.terminate_instantiated_app(inst['id'], 60)
        else:
            payload['appInstanceId'] = instance_id
            payload['appOnBoardingAppId'] = upgrade_id
#            payload.pop('appId', None)
        print(json.dumps(payload, indent=4))
        resp = appm.instantiate_with_am(upgrade_id, payload, upgrade = use_lcmupgrade, wait = 80)
        if resp:
            logging.info("Waiting for health endpoint to become available ...")
            cnr.check_health(refresh_auth = True)
            status = cnr.wait_health_status('UP',50)
            if status:
                timeend = datetime.datetime.now()
                logging.info("Outage ended at %s", timeend)
            if timestart and timeend:
                logging.info('\nContainer service outage time during upgrade: %s', timeend-timestart)
            elif not timeend:
                logging.error("Container service outage time evaluation failed")
            else:
                logging.error("No container service outage was measured")
    else:
        logging.warning('App reinstantiation failed - no running %s app found', app_name)


############################# Terminate instantiated app #######################################

@main.command()
@click.pass_context
@click.option('--instantiated-app-id', '-i', help='The instantiated app id to upgrade')
def terminate_app(ctx, instantiated_app_id):
    """!@brief Terminate a deployed rApp
    :param ctx: the click module context
    :param instantiated_app_id: The instantiated app id to be terminated,
    """
    logging.info("\n%s Terminating instantiated app with id: %s %s",
                 DELIM, instantiated_app_id, DELIM)
    appmgr = appmgrs.AppMGR(ctx.obj['cfg'], 'appmgr')
    appmgr.terminate_instantiated_app(instantiated_app_id, 60)

############################# Delete onboarded app #######################################
@main.command()
@click.pass_context
@click.option('--onboarded-app-id', '-i', help='The onboarded app id to be deleted')
def delete_app(ctx, onboarded_app_id):
    """!@brief Delete a deployed rApp
    :param ctx: the click module context
    :param onboarded_app_id: The onboarded app id to be deleted
    """
    logging.info("\n%s Deleting onboarded app with id: %s %s",
                 DELIM, onboarded_app_id, DELIM)
    appmgr = appmgrs.AppMGR(ctx.obj['cfg'], 'appmgr')
    appmgr.delete_onboarded_app(onboarded_app_id, 60)

#################### Get and check node list from CTS for test data #####################################
def check_test_nodes(ctx, cts, test_data):
    """!@brief Collect and check test node data from CTS based on test data in configuration
    :param ctx: the click module context
    :param cts: the CTS connection object
    :param test_data: the test data set to be used from config
    """
    logging.info("Collecting test node data from CTS")
    enodebs = copy.copy(ctx.obj['cfg'].cfg[test_data]['nodes_4G'])
    gnodebs = copy.copy(ctx.obj['cfg'].cfg[test_data]['nodes_5G'])
    cts_enodebs = cts.get_enm_objects('enodeb')
    cts_gnodebs = cts.get_enm_objects('gnbdu')
    plmns_single = ctx.obj['cfg'].cfg[test_data].get('plmnids',{}).get('single',[])
    plmns_multi  = ctx.obj['cfg'].cfg[test_data].get('plmnids',{}).get('multi',[])
    test_enodebs = {}
    test_gnodebs = {}
    if cts_enodebs and cts_gnodebs:
        for node in cts_enodebs:
            if node.cmhandle in enodebs:
                node.coordinates = enodebs[node.cmhandle]['coordinates']
                node.distance    = enodebs[node.cmhandle]['distance']
                node.plmnids     = plmns_single
                test_enodebs[node.shortname] = node
                enodebs.pop(node.cmhandle)
        for node in cts_gnodebs:
            if node.cmhandle in gnodebs:
                node.frequency   = gnodebs[node.cmhandle]['frequency']
                node.coordinates = gnodebs[node.cmhandle]['coordinates']
                node.distance    = gnodebs[node.cmhandle]['distance']
                node.multiPLMN   = gnodebs[node.cmhandle].get('multiPLMN', False)
                if node.multiPLMN:
                    node.plmnids = plmns_multi
                else:
                    node.plmnids = plmns_single
                test_gnodebs[node.shortname] = node
                gnodebs.pop(node.cmhandle)
    return test_enodebs, test_gnodebs, enodebs, gnodebs

######################### Get node list from CTS for test data #####################################
def get_test_nodes(ctx, cts, test_data):
    """!@brief Wrapper around test node data check against CTS based on test data in configuration
    :param ctx: the click module context
    :param cts: the CTS connection object
    :param test_data: the test data set to be used from config
    """
    test_enodebs, test_gnodebs, _, _ = check_test_nodes(ctx, cts, test_data)
    return test_enodebs, test_gnodebs

#################### Get test node cmhandles from configuration #####################################
def get_test_node_handles(ctx, test_data):
    """!@brief Collect test nodes cm handles and return them in an array
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    """
    ret  = list(ctx.obj['cfg'].cfg[test_data]['nodes_4G'].keys())
    ret += list(ctx.obj['cfg'].cfg[test_data]['nodes_5G'].keys())
    return ret

######################### Check ENM connections #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
def check_enm_connections(ctx, test_data):
    """!@brief Check ENM connection in the cluster by checking NCMP calls, then
       verifying the test node data existence in NCMP and CTS
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    """
    logging.info("\n%s Checking CTS and NCMP/ENM connection on host %s %s",
                 DELIM, ctx.obj['cfg'].get_host('apigw'), DELIM)
    data_ready = True
    do_rta_sync= False
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    _, gnodebs, miss_enodebs, miss_gnodebs = check_test_nodes(ctx, cts, test_data)
    if miss_enodebs or miss_gnodebs:
        logging.info('Missing nodes:\n  LTE: %s\n  5G: %s',
                 [v['name'].rsplit('/')[-2] for k, v in miss_enodebs.items()],
                 [v['name'].rsplit('/')[-2] for k, v in miss_gnodebs.items()])
        logging.error('Data verification failed! Node data is missing from CTS. Exiting ...')
        data_ready = False
        sys.exit(1)
    else:
        logging.info('CTS connection verification is successful, test node data is present!')

    if data_ready:
        ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
        node = list(gnodebs.values())[1]
        nodecellids = ncmp.check_gnodeb_in_enm(node)
        cts_node = cts.get_enm_node_with_cells(node.oid, 'gnbdu')
        if nodecellids:
            logging.info('ENM/NCMP connection verification is successful, test node data is accessible!')
            for cell in cts_node.cells.values():
                if str(cell.localid) not in nodecellids:
                    do_rta_sync = True
            if do_rta_sync:
                logging.info("It seems the cell ids are not properly synchronized to CTS. Initiating RTA sync. -> %s", nodecellids)
        else:
            logging.error('ENM connection/data verification failed! Node data is missing from ENM.')
            data_ready = False

    if do_rta_sync:
        rta = rtas.RTA(ctx.obj['cfg'], 'rta')
        logging.info("Triggering RTA sync toward CTS for the test nodes ...")
        rta.trigger_discovery(get_test_node_handles(ctx, test_data))
        time.sleep(10)    # sleep 10s to leave room for sync

    if not data_ready:
        sys.exit(1)

######################### Initialize ENM node data using NCMP #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
@click.option('--force-update', '-f', is_flag=True,
               help='Force update of test data, even if it is already set')
def init_staging_test_data_ncmp(ctx, test_data, force_update):
    """!@brief  Initialize ENM node data using NCMP
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    :param force_update: Force update of test data, even if it is already set
    """
    logging.info("\n%s Checking and configuring ENM nodes using NCMP on host %s %s",
                 DELIM, ctx.obj['cfg'].get_host('apigw'), DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    enodebs, gnodebs = get_test_nodes(ctx, cts, test_data)

    if not force_update and ncmp.get_gutran_sycnfreq(list(enodebs.values())[0]):
        logging.info("The ENM test nodes seems to be already configured. Stopping here.")
    else:
        for _, node in enodebs.items():
            ncmp.create_test_gutranw_with_sync_frequencies(node, 2073333)
        for _, node in gnodebs.items():
            if node.multiPLMN:
                ncmp.populate_gnodeb_multiple_plmn(node, node.frequency,
                        do_gnbdu = True, do_gnbcucp = True, do_transport = True)
            else:
                ncmp.populate_gnodeb(node, node.frequency,
                        do_gnbdu = True, do_gnbcucp = True, do_transport = True)

######################### Initialize geolocation for test data in CTS #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
@click.option('--use-random-angle', '-ra', is_flag=True, help='Set locations around a circle')
@click.option('--force-update', '-f', is_flag=True,
               help='Force update of test data, even if it is already set')
def init_staging_test_geodata_cts(ctx, test_data, force_update, use_random_angle):
    """!@brief  Initialize geolocation for test data in CTS by generating a datasync
       json input, and calling CTS datasync endpoint. It takes locations from the config ...
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    :param use_random_angle: set cell site locations around a circle instead of a line
    :param force_update: Force update of test data, even if it is already set
    """
    logging.info("\n%s Setting geolocation data in CTS for test data on host %s %s",
                 DELIM, ctx.obj['cfg'].get_host('apigw'), DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    enodebs, gnodebs = get_test_nodes(ctx, cts, test_data)

    test_node = cts.get_enm_node_with_cells(list(enodebs.values())[0].oid)
    if not force_update:
        cts.get_geolocation(test_node)
    if not force_update and test_node.locations:
        logging.info("Found %s location(s) set for node %s", test_node.locations,
                      test_node)
        logging.info("The CTS test cells geolocation seems to be already configured. Stopping here.")
    else:
        for _, enode in enodebs.items():
            enodeb = cts.get_enm_node_with_cells(enode.oid)
            counter = 0
            if enodeb:
                for _, cell in enodeb.cells.items():
                    counter+=1
                    cts.create_and_set_location(cell, enode.distance, enode.coordinates,
                        counter, use_random_angle)
        for _, gnode in gnodebs.items():
            gnodeb = cts.get_enm_node_with_cells(gnode.oid, 'gnbdu')
            print(str(gnodeb))
            counter = 0
            if gnodeb:
                for _, cell in gnodeb.cells.items():
                    counter+=1
                    cts.create_and_set_location(cell, gnode.distance, gnode.coordinates,
                        counter, use_random_angle)


######################### Cleanup ExternalGNodeB data from test nodes #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
@click.option('--filter-id', '-f', help='Filter for removing X2 links')
@click.option('--list-only', '-l', is_flag=True, help='List the X2 links only and not delete them')
def cleanup_external_cells(ctx, test_data, filter_id, list_only):
    """!@brief Cleaning up test eNodeB nodes from external cells
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    :param filter_id: filter to remove X2 links for selected 5G node ids only
    :param list_only: list the X2 links only and not delete them
    """
    logging.info("\n%s Cleaning up X2 links from test eNodeB nodes on host %s %s",
                 DELIM, ctx.obj['cfg'].get_host('apigw'), DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    enodebs = get_test_nodes(ctx, cts, test_data)[0]

    enodebf_mo = ncmps.MO(ncmps.ENODEB_FUNCTION, 1)
    gutranw_mo = ncmps.MO(ncmps.GUTRANW, 1, enodebf_mo)
    extgnbf_mo = ncmps.MO(ncmps.EXTGNODEB_FUNCTION, 1, gutranw_mo)
    trmpgnb_mo = ncmps.MO(ncmps.TERMPOINTTOGNB, 1, extgnbf_mo)

    for _, enode in enodebs.items():
        logging.info('Processing LTE node %s ...',enode.shortname)
        x2links = ncmp.get_mo_ids(enode, extgnbf_mo)
        if x2links:
            for node_id in x2links:
                if filter_id and filter_id != node_id:
                    break
                extgnbf_mo = ncmps.MO(ncmps.EXTGNODEB_FUNCTION, node_id, gutranw_mo)
                trmpgnb_mo = ncmps.MO(ncmps.TERMPOINTTOGNB, 1, extgnbf_mo)
                trmpgnd    = ncmp.get_mo_ids(enode, trmpgnb_mo)
                if list_only:
                    logging.info("X2 link %s: -> TermPointToGNB list %s", node_id, trmpgnd)
                    break
                for trmpgnd_id in trmpgnd:
                    logging.info('Locking TermPointToGNB with id %s for node %s',trmpgnd_id, node_id)
                    ncmp.patch_object(enode, trmpgnb_mo, {ncmps.ADMIN_STATE: 'LOCKED'})
                logging.info('X2 link to %s -> deleting it', node_id)
                result = ncmp.delete_resource(enode.cmhandle, extgnbf_mo.get_resname())
                if not result:
                    logging.warning("  !!! X2 link %d deletion failed", node_id)
        else:
            logging.info("Node %s: -> No X2 links found, nothing cleaned", enode.shortname)

#################### Cleanup GUtransyncsignalFrequency MO data from test nodes #######################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
@click.option('--filter-id', '-f', help='Filter for removing MOs', default=r'1')
@click.option('--list-only', '-l', is_flag=True, help='List the MOs only and not delete them')
def cleanup_gutransyncsignalfreq(ctx, test_data, filter_id, list_only):
    """!@brief Cleaning up test eNodeB nodes from GUtransyncsignalFrequency invalid objects
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    :param filter_id: filter to remove GUtransyncsignalFrequency for selected ids only
    :param list_only: list the MOs only and not delete them
    """
    logging.info("\n%s Cleaning up GUtransyncsignalFrequency MO with id %s from test eNodeB nodes on host %s %s",
                 DELIM, filter_id, ctx.obj['cfg'].get_host('apigw'), DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    enodebs = get_test_nodes(ctx, cts, test_data)[0]

    enodebf_mo = ncmps.MO(ncmps.ENODEB_FUNCTION, 1)
    gutranw_mo = ncmps.MO(ncmps.GUTRANW, 1, enodebf_mo)
    gssf_mo    = ncmps.MO(ncmps.GUTRANSSFREQ, filter_id, gutranw_mo)
    for _, enode in enodebs.items():
        gssf_ids = ncmp.get_mo_ids(enode, gssf_mo)
        if gssf_ids and (list_only or filter_id in gssf_ids):
            logging.info("Node: %s -> Found GUtransyncsignalFrequency MOs %s",
                        enode.shortname, gssf_ids)
            if not list_only:
                result = ncmp.delete_resource(enode.cmhandle, gssf_mo.get_resname())
                if result:
                    logging.info("GUtransyncsignalFrequency %s on node %s was deleted",
                        filter_id, enode.shortname)

######################### Cleanup GNodeB data from test nodes #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
@click.option('--filter-id', '-f', help='Filter for removing MOs', default=r'1')
@click.option('--list-only', '-l', is_flag=True, help='List the MOs only and not delete them')
def cleanup_gnbdus(ctx, test_data, filter_id, list_only):
    """!@brief Cleaning up GNBDU nodes from invalid configuration
    :param ctx: vthe click module context
    :param test_data: the test data set to be used from config
    :param filter_id: filter to remove MOs for selected ids only
    :param list_only: list the MOs only and not delete them
    """
    logging.info("\n%s Cleaning up test GNBDU nodes from invalid configuration on host %s %s",
                 DELIM, ctx.obj['cfg'].get_host('apigw'), DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    gnodebs = get_test_nodes(ctx, cts, test_data)[1]

    gnbcucp_mo = ncmps.MO(ncmps.GNBCUCP_FUNCTION, 1)
    resparts_mo= ncmps.MO(ncmps.RES_PARTITIONS, 1, gnbcucp_mo)
    respart_mo = ncmps.MO(ncmps.RES_PARTITION, 1, resparts_mo)
    respartmbr_mo = ncmps.MO(ncmps.RES_PARTITION_MEMBER, filter_id, respart_mo)

    # cleaning up unneeded ResourcePartitionMember
    for _, gnode in gnodebs.items():
        if not gnode.multiPLMN:
            continue
        rpm_ids = ncmp.get_mo_ids(gnode, respartmbr_mo)
        if rpm_ids and (list_only or filter_id in rpm_ids):
            logging.info("Node: %s -> Found ResourcePartitionMember MOs %s",
                        gnode.shortname, rpm_ids)
            if not list_only:
                result = ncmp.delete_resource(gnode.cmhandle, respartmbr_mo.get_resname())
                if result:
                    logging.info("ResourcePartitionMember %s on node %s was deleted",
                        filter_id, gnode.shortname)

######################### Cleanup NRCellDU MO data from test nodes #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
@click.option('--filter-id', '-f', help='Filter for removing MOs', default=r'1')
@click.option('--list-only', '-l', is_flag=True, help='List the MOs only and not delete them')
def cleanup_gnbdu_nrcelldu(ctx, test_data, filter_id, list_only):
    """!@brief Cleaning up GNBDU nodes from invalid NRCellDU objects configuration
    :param ctx: the click module context
    :param test_data: the test data set to be used from config
    :param filter_id: filter to remove MOs for selected ids only
    :param list_only: list the MOs only and not delete them
    """
    logging.info("\n%s Cleaning up test GNBDU nodes from invalid NRCellDU configuration on host %s %s",
                 DELIM, ctx.obj['cfg'].get_host('apigw'), DELIM)
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    gnodebs = get_test_nodes(ctx, cts, test_data)[1]

    gnbdu_mo = ncmps.MO(ncmps.GNBDU_FUNCTION, 1)
    nrcelldu_mo = ncmps.MO(ncmps.NRCELLDU, filter_id, gnbdu_mo)

    # cleaning up NRCELLDU objects
    for _, gnode in gnodebs.items():
        nrcelldu_ids = ncmp.get_mo_ids(gnode, nrcelldu_mo)
        if nrcelldu_ids and (list_only or filter_id in nrcelldu_ids):
            logging.info("Node: %s -> Found NRCELLDU MOs %s",
                        gnode.shortname, nrcelldu_ids)
            if not list_only:
                result = ncmp.delete_resource(gnode.cmhandle, nrcelldu_mo.get_resname())
                if result:
                    logging.info("NRCELLDU %s on node %s was deleted",
                        filter_id, gnode.shortname)

######################### Print out test data for 5G nodes ###################################
@main.command()
@click.pass_context
@click.option('--node-index', '-i', help='Index of test node', default=1)
@click.option('--node-type', '-t', help='Type of node', default='gnbdu')
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
def list_node_data(ctx, node_type, node_index, test_data):
    """!@brief Listing the nodes data for the 5G test nodes
    :param ctx: the click module context
    :param node_type: the test node type
    :param node_index: the test node index
    :param test_data: the test data set to be used from config
    """
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    ncmp = ncmps.NCMP(ctx.obj['cfg'], 'ncmp')
    enodebs, gnodebs = get_test_nodes(ctx, cts, test_data)
    if 'gnbdu' == node_type:
        nodes = gnodebs
    else:
        nodes = enodebs

    node = list(dict(sorted(nodes.items())).values())[node_index-1]

    logging.info("\n  Listing test node configuration for node:\n%s\n%s",
                 node.longfdn,node.cmhandle)

    nrsectcrf = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.NRSECTORCARRIER]),['*']).replace('fields=','')
    nrcellduf = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.NRCELLDU]),['*']).replace('fields=','')
    result = ncmp.get_resource(node.cmhandle, ncmps.GNBDU_FUNCTION, f"fields={nrsectcrf};{nrcellduf}")
    if result and ncmps.GNBDU_FUNCTION in result:
        gnbduf = result[ncmps.GNBDU_FUNCTION][0]
        for nmo in gnbduf.get(ncmps.NRCELLDU,[]):
            print(f"NRCellDU: {nmo['id']} -> cellLocationId: {nmo['attributes']['cellLocalId']}; nrsc_ref:{nmo['attributes']['nRSectorCarrierRef'][0].split(',')[-1]}")
        for nmo in gnbduf.get(ncmps.NRSECTORCARRIER,[]):
            print(f"NRSectorCarrier: {nmo['id']} -> arfcnDL: {nmo['attributes']['arfcnDL']}; arfcnUL: {nmo['attributes']['arfcnUL']}; reservedBy:{nmo['attributes']['reservedBy'][0].split(',')[-1]}")

    respartmbr = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.RES_PARTITION_MEMBER]),['*']).replace('fields=','')
    lclsctpep  = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.LOCAL_SCTP_ENDPOINT]),['*']).replace('fields=','')
    endp_res   = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.ENDPOINT]),['*']).replace('fields=','')
    result = ncmp.get_resource(node.cmhandle, ncmps.GNBCUCP_FUNCTION, f"fields={respartmbr};{lclsctpep};{endp_res}")
#    print(json.dumps(result, indent=4))
    if result and ncmps.GNBCUCP_FUNCTION in result:
        gnbcucpf = result[ncmps.GNBCUCP_FUNCTION][0]
        for nmo in gnbcucpf.get('ResourcePartitions',[])[0].get('ResourcePartition',[])[0].get(ncmps.RES_PARTITION_MEMBER,[]):
            print(f"RES_PARTITION_MEMBER: {nmo['id']} -> pLMNIdList: {nmo['attributes']['pLMNIdList']}; ref:{nmo['attributes']['endpointResourceRef']}")
        for nmo in gnbcucpf.get(ncmps.ENDPOINT,[]):
#            print(gnbcucpf[ncmps.ENDPOINT])
            print(f"ENDPOINT: {nmo['id']} -> reservedBy:{nmo['attributes']['reservedBy'][0].split(',')[-1]}")
            lcl_endp = nmo[ncmps.LOCAL_SCTP_ENDPOINT]
            for lep in lcl_endp:
#            print(gnbcucpf[ncmps.LOCAL_SCTP_ENDPOINT])
                print(f"LOCAL_SCTP_ENDPOINT: {lep['id']} -> {lep['attributes']['interfaceUsed']} : sctpEndpointRef: {lep['attributes']['sctpEndpointRef'].split(',')[-1]}")

    sctp_prof = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.SCTP_PROFILE]),['*']).replace('fields=','')
    sctp_endp = ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.SCTP_ENDPOINT]),['*']).replace('fields=','')
    ip_address= ncmps.pack_field_and_attributes(ncmps.get_fields([ncmps.IP_ADDRESS]),['*']).replace('fields=','')
    result = ncmp.get_resource(node.cmhandle, ncmps.TRANSPORT, f"fields={sctp_prof};{sctp_endp};{ip_address}")
#    print(json.dumps(result, indent=4))
    if result and ncmps.TRANSPORT in result:
        transportf = result[ncmps.TRANSPORT][0]
        for nmo in transportf.get(ncmps.SCTP_PROFILE,[]):
            print(f"SCTP_PROFILE: {nmo['id']} -> sctpProfileId: {nmo['attributes']['sctpProfileId']}; reservedBy:{nmo['attributes']['reservedBy'][0].split(',')[-1]}")
        for nmo in transportf.get(ncmps.SCTP_ENDPOINT,[]):
            print(f"SctpEndpoint: {nmo['id']} -> sctpProfile: {nmo['attributes']['sctpProfile'].split(',')[-1]}; localIpAddress:{nmo['attributes']['localIpAddress'][0].split(',')[-1]}")
        for nmo in transportf.get('Router',[]):
#            print(mo)
            intf = nmo['InterfaceIPv4'][0]
            ipaddr = intf[ncmps.IP_ADDRESS][0]
            print(f"Router: {nmo['id']} -> IP_intf: {intf['id']}; IP_addr: {ipaddr['id']}: {ipaddr['attributes']['address']}")

######################### Test CNR new connections #####################################
@main.command()
@click.pass_context
@click.option('--test-data', '-td', help='Test data set', default=r'staging_test_data')
def check_cnr_end2end(ctx, test_data):
    """!@brief  Do a sequence of CNR tests to check the end2end test results
    :param ctx: the click module context
    """
    cts = ctss.CTS(ctx.obj['cfg'], 'cts')
    enodebs = get_test_nodes(ctx, cts, test_data)[0]
    ctx.forward(start_nrc)
    ctx.invoke(start_nrc, payload_file = None, cell_id=enodebs['LTE06dg2ERBS00011'].oid,
               distance=150, frequency_filter=None)
    ctx.invoke(start_nrc, payload_file = None, cell_id=enodebs['LTE06dg2ERBS00011'].oid,
               distance=150, frequency_filter='4:2076665,2073333')
    ctx.invoke(start_nrc, payload_file = None, cell_id=enodebs['LTE06dg2ERBS00011'].oid,
               distance=120, frequency_filter=None)
    ctx.invoke(start_nrc, payload_file = None, cell_id=f"{enodebs['LTE06dg2ERBS00011'].oid},{enodebs['LTE06dg2ERBS00012'].oid}",
               distance=500, frequency_filter='3,4:2088327,2074999')
    ctx.invoke(start_nrc, payload_file = None, cell_id=f"{enodebs['LTE06dg2ERBS00011'].oid}",
               distance=500, frequency_filter='3:2074999')
    ctx.invoke(start_nrc, payload_file = None, cell_id=f"{enodebs['LTE06dg2ERBS00011'].oid}",
               distance=500, frequency_filter=None)
    ctx.invoke(start_nrc, payload_file = None, cell_id=enodebs['LTE06dg2ERBS00012'].oid,
               distance=600, frequency_filter=None)
    ctx.invoke(start_nrc, payload_file = None, cell_id=enodebs['LTE06dg2ERBS00011'].oid,
               distance=150, frequency_filter=None)


if __name__ == "__main__":
    main()
    # Say bye
    print("All Done! Bye, for now.")
