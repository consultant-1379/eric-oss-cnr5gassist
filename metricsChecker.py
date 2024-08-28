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

import os
import sys
import yaml
import random
import string
import unittest
import subprocess


# Helm Helpers
def evaluate_template(p, **kwargs):
    command = ['helm', 'template', p]
    if kwargs:
        command.extend(["--set", ",".join(map(lambda e: f"{e[0]}={str(e[1])}", kwargs.items()))])
    result = subprocess.run(command, stdout=subprocess.PIPE)
    return list(filter(lambda x: x is not None, yaml.safe_load_all(result.stdout)))


# Yaml Helpers
def recursive_search(container, keys):
    keys = keys.split('.', 1)
    key, keys = keys[0], keys[-1]
    if key in container:
        if key in ("labels", "annotations"):
            if keys in container[key]:
                return True, container[key][keys]
            else:
                return False, None
        elif key != keys:
            return recursive_search(container[key], keys)
        return True, container[key]
    return False, None


def get_value(container, keys):
    return recursive_search(container, keys)[1]


def contains_key(container, keys):
    return recursive_search(container, keys)[0]


# Random Generators
def get_random_string():
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for _ in range(random.randint(4, 10)))


def get_random_bool():
    return bool(random.randint(0, 1))


def get_random_bool_str():
    return str(get_random_bool()).lower()


def get_random_number():
    return random.randint(1000, 9999)


def get_random_number_str():
    return str(get_random_number())


class MetricsCheckerTests(unittest.TestCase):
    PATH = ""
    REQUIRED_VALUES = {
        "prometheus.path": "/actuator/prometheus",
        "prometheus.scrape": True
    }
    ALTERED_VALUES = {
        "prometheus.path": get_random_string(),
        "prometheus.scrape": get_random_bool_str(),
        "service.port": get_random_number_str()
    }
    ASSERTED_VALUES = {
        "metadata.annotations.prometheus.io/path": ALTERED_VALUES["prometheus.path"],
        "metadata.annotations.prometheus.io/scrape": ALTERED_VALUES["prometheus.scrape"],
        "metadata.annotations.prometheus.io/port": ALTERED_VALUES["service.port"]
    }

    @classmethod
    def setUpClass(cls) -> None:
        with open(os.path.join(cls.PATH, "values.yaml")) as f:
            cls.VALUES = yaml.safe_load(f)
        cls.CHARTS = evaluate_template(cls.PATH, **cls.ALTERED_VALUES)

    def assertItemProperties(self, item, **kwargs):
        for key, value in kwargs.items():
            self.assertEqual(get_value(item, key), value)

    def assertCertainKindItemsProperties(self, kind, **kwargs):
        for item in filter(lambda x: get_value(x, "kind") == kind, self.CHARTS):
            self.assertItemProperties(item, **kwargs)

    def test_values(self):
        self.assertItemProperties(self.VALUES, **self.REQUIRED_VALUES)

    def test_services(self):
        self.assertCertainKindItemsProperties("Service", **self.ASSERTED_VALUES)

    def test_deployments(self):
        asserted_values = dict(map(lambda x: ("spec.template." + x[0], x[1]), self.ASSERTED_VALUES.items()))
        asserted_values.update(self.ASSERTED_VALUES)
        self.assertCertainKindItemsProperties("Deployment", **asserted_values)


if __name__ == '__main__':
    if len(sys.argv) > 1:
        MetricsCheckerTests.PATH = sys.argv.pop()
    unittest.main()
