#!/usr/bin/env python3 -u
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

# TODO: refine search when multiple softwares are found (~ line 131)
# TODO: deal with double software entries

import os
import re
import csv
import json
import requests
import argparse
import logging
from functools import reduce

field_names = (
    "dependency",
    "name",
    "download_url",
    "version",
    "licenses",
    "bazaarurl",
    "prim_rstate"
)
error_field_names = ("name", "version")


class Util(object):
    args=None

    @staticmethod
    def map_to_dep_match(dep):
        return re.match(r"^\w+://((?P<group>[^:]+):(?P<name>[^:]+):(?P<version>[^:]+))$", dep)

    @staticmethod
    def filter_none(obj):
        return obj is not None

    @staticmethod
    def filter_relevant_data(key):
        relevant_keys = ('name', 'download_url', 'version', 'licenses', 'bazaarurl', 'prim', 'rstate')
        return key[0] in relevant_keys

    @staticmethod
    def map_to_dep_match_result(obj):
        return Dependency(**obj.groupdict())

    @staticmethod
    def map_to_json(obj):
        return json.loads(obj) if obj else None

    @staticmethod
    def extract_json_from_response(text):
        return tuple(filter(Util.filter_none, map(Util.map_to_json, text.splitlines())))


class Dependency(object):
    __slots__ = ('name', 'group', 'version', 'full_name', 'short_name')

    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            self.__setattr__(k, v)
        self.full_name = re.sub(r"[-._]", " ", self.name)
        self.short_name = self.full_name.split()[0]


class BaseRow(object):
    __slots__ = error_field_names

    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            self.__setattr__(k, v)

    @property
    def __dict__(self):
        return {s: getattr(self, s) for s in self.__slots__ if hasattr(self, s)}


class Row(BaseRow):
    __slots__ = field_names

    def __init__(self, **kwargs):
        super().__init__()
        for k in self.__slots__:
            self.__setattr__(k, kwargs.get(k))
        self.prim_rstate = self.prim_rstate or f"{kwargs.get('prim', '')} {kwargs.get('rstate', '')}".strip()


def extract_dependencies(path):
    with open(path) as file:
        json_file = json.load(file)
    if len(json_file['artifacts'])==0:
        return tuple()
    dependencies = set(reduce(lambda a, b: a + b,
                              map(lambda x: x['components'], json_file['artifacts'][0]['licenses'])))
    return tuple(map(Util.map_to_dep_match_result,
                     filter(Util.filter_none,
                            map(Util.map_to_dep_match, sorted(dependencies)))))


def get_bazaar_info(**kwargs):
    params = {
        "username": Util.args.username,
        "token": Util.args.token,
        "facility": "COMPONENT_QUERY",
    }
    params.update(kwargs)
    return requests.get("http://papi.internal.ericsson.com", params={"query": json.dumps(params)}, verify=False)


def get_prim_response(dependency):
    for name in (dependency.name, dependency.short_name):
        response = get_bazaar_info(name=name, version=dependency.version)
        json_responses = Util.extract_json_from_response(response.text)
        if not check_for_error(json_responses):
            return json_responses

    return tuple()


def get_prim(dependency):
    responses = get_prim_response(dependency)
    if check_for_error(responses):
        logging.warning(f'{dependency.name}:{dependency.version} not found on bazaar')
        return "error"

    def prim_filter1(element):
        __name = element['name']
        __version = element['version']
        return (re.search(dependency.full_name, __name, re.I) or __name.lower() == dependency.short_name) and \
               (__version.lower() == dependency.version.lower())

    def prim_filter2(element):
        __version = element['version']
        return __version.lower() == dependency.version.lower()

    for prim_filter in (prim_filter1, prim_filter2):
        filtered_responses = tuple(filter(prim_filter, responses))
        if len(filtered_responses) == 1:
            return filtered_responses[0]['prim']
    return 'error'


def get_data_from_prim(prim):
    response = get_bazaar_info(prim=prim).json()
    return dict(filter(Util.filter_relevant_data, response.items()))


def write_data(file, fieldnames, data, mode):
    with open(file, mode, newline="") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(map(vars, data))


def check_for_error(responses):
    return any(map(lambda x: "error" in x, responses))


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-o', '--output', default='.bob/dependencies_data.csv', type=str,
                        help='Output file for the dependencies data', dest='output_file')
    parser.add_argument('-e', '--error', default=".bob/dependencies_errors.csv", type=str,
                        help="the output file for dependencies errors", dest='error_file')
    parser.add_argument('-i', '--input', default='.bob/xray-reports/raw_xray_report.json', type=str,
                        help="The input file", dest='input_file')
    parser.add_argument('-l', '--line', default=None, type=int,
                        help="Look for information of only a specific line in the range of 1 to N")
    parser.add_argument('-m', '--mode', default='w', type=str, choices=['a', 'w'],
                        help='How to treat the output file: Append to the end or overWrite it')
    parser.add_argument('-v', '--verbose', default=False, help='show information on screen', action='store_true')
    parser.add_argument('--log', default='.bob/dependencies_log.txt', type=str, dest='log_file',
                        help="Set the log file destination")
    parser.add_argument('-t','--token', default=os.environ.get('BAZAAR_TOKEN'), type=str, dest='token',
                        help="Set the token")
    parser.add_argument('-u','--username', default=os.environ.get('BAZAAR_USER'), type=str, dest='username',
                        help="Set the username")

    return parser.parse_args()


def configure_logging(args):
    logging.basicConfig(format='%(asctime)s - %(levelname)s: %(msg)s')
    logging.getLogger("requests").propagate = False
    logging.getLogger("urllib3").propagate = False
    level = logging.DEBUG if args.verbose else logging.INFO

    logger = logging.getLogger()
    logger.handlers.clear()
    logger.setLevel(logging.DEBUG)
    fh = logging.FileHandler(args.log_file)
    fh.setLevel(logging.DEBUG)
    logger.addHandler(fh)
    ch = logging.StreamHandler()
    ch.setLevel(level)
    logger.addHandler(ch)


def main():
    args = parse_args()
    configure_logging(args)

    Util.args=args

    dependencies = extract_dependencies(args.input_file)
    if dependencies == tuple():
        write_data(args.output_file, field_names, [], args.mode)
        write_data(args.error_file, error_field_names, [], args.mode)
        logging.info(f"0 generated to the csv file. 0 Not found")
        return

    if args.line is not None:
        dependencies = [dependencies[args.line - 1]]

    data, error = [], []

    for dependency in dependencies:
        prim = get_prim(dependency)

        if prim == "error":
            if dependency.name.startswith("eric-"):
                logging.debug(f"{dependency.name}:{dependency.version} not added to the csv file because its a 2pp")
            else:
                logging.debug(f"{dependency.name}:{dependency.version} not added to the csv file")

            error.append(BaseRow(name=dependency.name, version=dependency.version))
            continue

        new_line = get_data_from_prim(prim)
        new_line["dependency"] = dependency.name
        new_line = Row(**new_line)
        data.append(new_line)

        logging.debug(f"{dependency.name}:{dependency.version} added to the csv file")

    write_data(args.output_file, field_names, data, args.mode)
    write_data(args.error_file, error_field_names, error, args.mode)

    logging.info(f"{len(data)} generated to the csv file. {len(error)} Not found")


if __name__ == "__main__":
    main()
