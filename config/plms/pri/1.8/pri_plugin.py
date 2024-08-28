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

import logging
import os

import yaml

import argparse_utils
from prilib import PriException
from prilib.plugin_common import prepare_env, get_document_data
from prilib.auth import PetAuthData
from prilib.pet import run_pet
from prilib.pra_email_generator import generate_email_body
import logutil

LOGGER = logutil.get_logger(__name__)


###############################################################################
def enable_debug():
    """
    Enable debugging in the plugin

    PRI plugins have to export this function
    """
    LOGGER.setLevel(logging.DEBUG)


###############################################################################
def get_eridoc_document_data(common_args, args):
    """
    Retrive eridoc document information from dpraf report

    PRI plugins have to export this function

    Returns:
        product_number(String): product number in eridoc
        product_name(String): product name in eridoc
        revision(String): product revision in eridoc
        semantic_version(String): product semantic version
    """
    return get_document_data(common_args, args)


###############################################################################
def get_parser():
    """
    Return the parser for this configuration

    PRI plugins have to export this function

    Returns:
        Plugin-specific parser object
    """
    parser_config = os.path.realpath(os.path.join(os.path.dirname(__file__),
                                                  "inputs.yaml"))
    if not os.path.isfile(parser_config):
        raise PriException(f"Input config file {parser_config} is missing!")
    with open(parser_config, 'r') as inputs:
        data = yaml.safe_load(inputs)
    return argparse_utils.get_parser(data)


###############################################################################
def get_auth(config_path):
    """
    Get the authentication secret file for PET
    """
    auth_data = {
        "git_collector": {
            "username": os.environ.get("GERRIT_USERNAME"),
            "password": os.environ.get("GERRIT_PASSWORD")
        },
        "jira_collector": {
            "username": os.environ.get("JIRA_USERNAME"),
            "password": os.environ.get("JIRA_PASSWORD")
        }
    }
    return PetAuthData(config_path, auth_data)


###############################################################################
def generate(eridoc_info, common_args, args):
    """
    Generate the PRI dorcument

    PRI plugins have to export this function

    Parameters:
        eridoc_info(dictionary): eridoc document information
        common_args(namedtuple): common arguments to PRI script
        args(namedtuple): plugin-specific arguments

    Returns:
        Path to generated PRI document
    """
    prepare_env(os.path.dirname(__file__), eridoc_info, common_args, args)
    LOGGER.debug("Env preparation completed, Starting PET")
    run_pet(os.path.dirname(__file__), get_auth, common_args)
    LOGGER.debug("PRI generation completed")

    return os.path.join(common_args.output_dir, "pri.pdf")


###############################################################################
def generate_release_mail(common_args):
    """
    Generates the PRA release mail
    """
    LOGGER.debug("Generate PRA release mail")
    generate_email_body(common_args.output_dir, os.path.dirname(__file__))
