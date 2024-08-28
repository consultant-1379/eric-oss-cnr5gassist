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
This module covers Kubernetes API functions
"""
import datetime
import logging
import logging.handlers
from kubernetes import client, config
from lib.service.commons import STAMPFMT


class KubernetesClient:
    """!@class KubernetesClient
        @brief Class representing a Kubernetes API client
        @details This is a class that represents Kubernetes API client
    """

    def __init__(self, kubernetes_admin_conf):
        """@brief Kubernetes API client init
        """

        config.load_kube_config(config_file=kubernetes_admin_conf)
        self.cluster_name = config.list_kube_config_contexts(
                                  config_file=kubernetes_admin_conf)[0][0]['name']
        self.core_v1 = client.CoreV1Api()
        self.api_client = client.AppsV1Api(client.ApiClient())
        self.co_client  = client.CustomObjectsApi()
        self.config  = client.configuration
#        self.ext_v1  = client.
#        self.apps_v1 = client.AppsV1beta2Api()

    def get_pod_startup_timestamps(self, namespace, app_name):
        """@brief Log running pod startup time
        :param app_name: The deployed pod name to scale the instances for
        :param namespace: The cluster namespace where the pod is deployed
        """
        timestart = None
        timeend   = None
        pod_name  = None
        resp = self.core_v1.list_namespaced_pod( namespace=namespace,
               label_selector=f'app.kubernetes.io/name={app_name}')
        if resp and resp.items:
            for i in resp.items:
                logging.debug("Found pod %s in namespace %s --> %s",
                i.metadata.name, i.metadata.namespace, i.status.pod_ip)
            pod_name = resp.items[0].metadata.name
            logging.debug('Getting timestamps for pod %s', pod_name)
            for item in resp.items[0].status.conditions:
                if 'Initialized' in item.type:
                    timestart = item.last_transition_time
                    logging.info('     Container start time: %s', timestart)
                if 'Ready' == item.type:
                    timeend = item.last_transition_time
                    logging.info('     Container ready time: %s', timeend)
            if "Running" in resp.items[0].status.phase:
                logging.info('     Container restart time: %s', timeend-timestart)
        return pod_name, timestart, timeend

    def get_pod_data(self, namespace, app_name):
        """@brief Get running pod descriptions
        :param app_name: The deployed pod name to get data for
        :param namespace: The cluster namespace where the pod is deployed
        """
        pods  = None
        resp = self.core_v1.list_namespaced_pod( namespace=namespace,
               label_selector=f'app.kubernetes.io/name={app_name}')
        if resp and resp.items:
            for i in resp.items:
                logging.debug("Found pod %s in namespace %s --> %s",
                i.metadata.name, i.metadata.namespace, i.status.pod_ip)
            pods = resp.items
        return pods

    def get_pod_usage(self, namespace, app_name):
        """@brief Get running pod memory and cpu usage
        :param app_name: The deployed pod name to get usage data for
        :param namespace: The cluster namespace where the pod is deployed
        """
        pod_usage = []
        resp = self.co_client.list_namespaced_custom_object(group="metrics.k8s.io",
               version="v1beta1", namespace=namespace,
               plural='pods')
        if resp and 'items' in resp:
            for i in resp['items']:
                logging.debug("Found pod %s in namespace %s with %s container(s).",
                i['metadata']['name'], i['metadata']['namespace'], len(i['containers']))
                if app_name in i['metadata']['name'] and namespace in i['metadata']['namespace']:
                    pod_usage.append(i)
        return pod_usage

    def get_cluster_name(self):
        """@brief Returning configured cluster name
        """
        return self.cluster_name

    def scale_deployment(self, app_name, namespace, pod_instances):
        """@brief Scale the pod instances to the number given
        :param pod_instances: the pod instance number to scale to
        :param app_name: The deployed pod name to scale the instances for
        :param namespace: The cluster namespace where the pod is deployed
        """
        resp = self.api_client.patch_namespaced_deployment_scale(
               app_name, namespace,
               [{'op': 'replace', 'path': '/spec/replicas', 'value': pod_instances}])
        return resp

    def get_pod_log_startup_timestamps(self, namespace, app_name, container, start_str, end_str):
        """@brief Get pod startup timestamps from pod log
        :param app_name: The deployed pod name to scale the instances for
        :param namespace: The cluster namespace where the pod is deployed
        :param container: The container name of the pod to list the log for
        :param start_str: The log start string to list log from
        :param end_str: The log end string to list log to
        """
        timestart = None
        timeend   = None
        pod_name  = None
        resp = self.core_v1.list_namespaced_pod( namespace=namespace,
               label_selector=f'app.kubernetes.io/name={app_name}')
        if resp and resp.items:
            for i in resp.items:
                logging.debug("Found pod %s in namespace %s --> %s",
                i.metadata.name, i.metadata.namespace, i.status.pod_ip)
            pod_name = resp.items[0].metadata.name
            logging.debug('Getting log for pod %s', pod_name)
            loglines = self.core_v1.read_namespaced_pod_log(
                name=pod_name, namespace=namespace, container=container).split('\n')
            for line in loglines:
                if start_str in line:
                    timestart = datetime.datetime.strptime(line[0:23], STAMPFMT)
                    logging.info("\n  Startline: %s", line)
                if end_str in line:
                    timeend = datetime.datetime.strptime(line[0:23], STAMPFMT)
                    logging.info("\n  Endline:   %s", line)
        return pod_name, timestart, timeend
