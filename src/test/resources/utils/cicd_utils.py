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
This python program contains a set of functions that helps to interact with E/// PLMS
systems over REST interfaces for executing CI related tasks

Usage: cicd_utils.py [OPTIONS] COMMAND [ARGS]...

Options:
  --version                   Show the version and exit.
  -c, --config-file PATH      Specify the generic configuration file
  -k, --cfg TEXT              Specify the configuration section from config
                              file
  -d, --debug                 Set logging level to debug
  -ssv, --skip-ssl-verify     Skip SSL key verification on all used REST
                              interfaces
  -h, --help                  Show this message and exit.

Commands:
  get_mimer_svl               !@brief Get SVL report from Mimer for the configured product...
  """
import sys
from os import path as ospath
import json
import logging
import logging.handlers
import urllib3
import yaml
import click
import lib.service.munin_service as munins
from lib.service.commons import SETTINGS, STAMPFMT, DELIM, DEFAULT_LOG_FORMAT, load_config_file, save_config_file

sys.path.append(ospath.join(ospath.dirname(ospath.abspath(__file__)),'lib'))

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])

######################################## main function ####################################
@click.group(context_settings=CONTEXT_SETTINGS)
@click.version_option(version='1.0.0')
@click.option('--config-file', '-c', help='Specify the generic configuration file',
      type=click.Path(exists=True), default=f'{ospath.dirname(ospath.abspath(__file__))}/config/eiap_utils.cfg')
@click.option('--cfg', '-k', default='default', help='Specify the configuration section from config file')
@click.option('--debug', '-d', is_flag=True, help='Set logging level to debug')
@click.option('--skip-ssl-verify', '-ssv', is_flag=True,
              help='Skip SSL key verification on all used REST interfaces')
@click.option('--auth_key', '-t', help='Authorization key/token for the function')
@click.option('--auth_user', '-u', help='Authorization user name/signum for the function')
@click.pass_context
def main(ctx, config_file, cfg, debug, skip_ssl_verify, auth_key, auth_user):
    """
    Main function to initialize logging and kube connection
    :param ctx: the click module context
    :param config_file: the configuration file to be used
    :param cfg: the configuration section in the configuration file
    :param debug: flag to turn on debug mode
    :param skip_ssl_verify: flag to disable SSL certificate verification during https connect
    :param auth_key: authorization key/token for functions
    :param auth_user: authorization user name/signum for functions
    """

    SETTINGS["verify_certificates"] = not skip_ssl_verify
    munins.SETTINGS["verify_certificates"] = not skip_ssl_verify
    if skip_ssl_verify:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    logfile = f"{ospath.dirname(ospath.abspath(__file__))}/data/log/eiap_utils.log"
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
    else:
        ctx.obj = {}
        ctx.obj['cfg'] = config
        ctx.obj['akey'] = auth_key
        ctx.obj['auser'] = auth_user

######################### Get SVL report from Mimer #####################################
@main.command()
@click.pass_context
@click.option('--product-number', '-pn', help='The Mimer product number', required=True)
@click.option('--product-version', '-pv', help='The Mimer product version', required=True)
@click.option('--output-file', '-o', help='The csv SVL report filename', required=True)
def get_mimer_svl(ctx, product_number, product_version, output_file):
    """!@brief Get SVL report from Mimer as xslx file
    :param ctx: the click module context
    :product_number: the Mimer product number
    :product_version: the Mimer product version
    :output_file: the file name/place for the downloaded xlsx file
    """
    logging.info("\n%s Getting the SVL report from Mimer for %s v%s %s",
                 DELIM, product_number, product_version, DELIM)
    munin = munins.Munin(ctx.obj['akey'], ctx.obj['auser'])
    resp = munin.get_svl_report(product_number, product_version, output_file)
    if not resp:
        logging.warning("Munin report download call failed, exiting with error ...")
        sys.exit(1)


if __name__ == "__main__":
    main()
    # Say bye
    print("All Done! Bye, for now.")
