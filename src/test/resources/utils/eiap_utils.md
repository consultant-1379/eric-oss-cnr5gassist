# Ericsson Intelligent Automation Platform (EIAP) utilities script package

## __Overview__

This document is summarizing the utility functions created in form of a Python script package, for working with EAIP platform services.  
The package was created by team Aether, to support their work with 5G CNR rApp application, it's development and related demo sessions.  

The application consists of a main script (eiap_utils.py) that contains all the high level functions, and a set of libraries to work with 
services of EIAP. This is the folder structure of the package:  
|___config  
|___data  
|   |___log  
|___lib  
    |___service  

The service folder contains libraries for the following EIAP services:
  - App Manager
  - CTS service
  - NCMP service
  - 5G CNR rApp service

The package is using the Python *[Click](https://pypi.org/project/click/)* for command line argument parsing, and therefore 
the command line structure is tied to that.  

## __Generic configuration__

The script is using a generic configuration, common for all the commands. It is located in the /config directory (eiap_utils.cfg)  
The configuration file has a yaml structure and contains:  
  - *Basic cluster environment access related parameters*
  - *Test data configuration*

You can find an example configuration file content below:  
```
generic_endpoints:
  apigw_login: /auth/v1/login
  cnr:
    monitor: /cnr/api/v1/nrc/monitoring
    startNrc: /cnr/api/v1/nrc/startNrc
    health: /cnr/actuator/health
  appmgr:
    onboarding: /app-manager/onboarding/v1/apps
    instantiation: /app-manager/lcm/app-lcm/v1/app-instances
    applcm: /app-manager/lcm/app-lcm/v1
  cts:
    ctw: /oss-core-ws/rest/ctw
    ctg: /oss-core-ws/rest/ctg
    datasync: /oss-core-ws/rest/osl-adv/datasync/process
    geoqry: 'geographicSite.locatedAt.geospatialData.geoDistanceWithin={{center:{{type:Point,coordinates:[{},{}]}},distance:{}}}'
  ncmp:
    res_qry: /ncmp/v1/ch/{cmhandle}/data/ds/ncmp-datastore:passthrough-operational
    res_create: /ncmp/v1/ch/{cmhandle}/data/ds/ncmp-datastore:passthrough-running
default:
  kubeconfig: /local/persistent_docker/mount/workspace/AutoApp-5G-CNR-Test/.kube/config
  namespace: idunossautoapp01
  domain: .662502336946.eu-west-1.ac.ericsson.se
  generic_pass: 
cnis_n12:
  kubeconfig: ~/.kube/cnis_n12_a
  namespace: idunossautoapp01
  domain: .a.cnis-idun-n12-services-2.sero.gic.ericsson.se
  generic_pass: 
staging_test_data:
  nodes_4G:
    868505FE0E8E3C8E6E07107037BFA1CE:
      name: Europe/Ireland/NETSimW/LTE416dg2ERBS00001/1
      coordinates: [-6.2867, 53.34198]
      frequency: 3
      distance: 40
    F22728495FB9CD89F81E71C90DE6C833:
      name: Europe/Ireland/NETSimW/LTE416dg2ERBS00002/1
      coordinates: [-6.2627, 53.34198]
      frequency: 4
      distance: 40
  nodes_5G:
    395221E080CCF0FD1924103B15873814:
      name: Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1
      coordinates: [-6.2887, 53.34198]
      frequency: 2073333
      distance: 40
    87968772573A59E96B623AB065876231:
      name: Europe/Ireland/NR01gNodeBRadio00002/NR01gNodeBRadio00002/1
      coordinates: [-6.2817, 53.34198]
      frequency: 2074999
      distance: 40
```

## __Cluster configuration__

The script is used to communicate and interact with an EIAP (Kubernetes) cluster. The cluster is configured as a structure in the 
configuration file.  
The generic parameters of the cluster configuration are listed below:  
```
<cluster configuration name>:
  kubeconfig: <the location of kube configuration to connect the cluster>
  namespace: <EIAP service namespace in the cluster>
  domain: <domain part of FQDN assigned to the EIAP cluster>
  generic_pass: <a generic password (base64) to be used for all users connecting EAIP services>
  endpointprefix:
    cts: <endpoint prefix used for CTS service - default is empty>
    ncmp: <endpoint prefix used for NCMP service - default is empty>
    appmgr: <endpoint prefix used for App Manager service - default is empty>
    cnr: <endpoint prefix used for CNR service - default is empty>
  users:
    cts: <user name used for CTS service access - default is "cts-user">
    ncmp: <user name used for NCMP service access - default is "cps-user">
    appmgr: <user name used for App Manager service access - default is "kcadmin">
    cnr: <user name used for CNR service access - default is "cnr-user">
  pass:
    cts: <password for user used for CTS service access - default is the generic_pass>
    ncmp: <password for user for NCMP service access - default is the generic_pass>
    appmgr: <password for user for App Manager service access - default is the generic_pass>
    cnr: <password for user for CNR service access - default is the generic_pass>
  hosts:
    appmgr: <service host name used for App Manager service access - default is "appmgr">
    apigw: <service host name used for API GW service access - default is "th">
    iam: <service host name used for IAM service access - default is "th">
```

## __Logging__

The script is logging the activities it performs to the log files configured (/data/log/eiap_utils.log) and to the console.  
The log files are rotated daily. 


## __Generic script usage and main parameters__

The generic script usage pattern is the following 
```
    Usage: eiap_utils.py [MAIN_OPTIONS] <COMMAND> [ARGS]  
    Main Options:  
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
```

The commands usable are presented in the following chapters, and also execution examples are given. 
The majority of functions are using EAIP API GW for accessing the services, and the proper authentication for accessing 
those resources is done in the background (i.e. getting the session id, and using it for the calls).  

## __1. App Manager related functions__

### __1.1. rApp onboarding__

The script can be used for rApp onboarding providing the right csar file.  
The command line for it is:  

```
Usage: eiap_utils.py -k <cluster configuration> onboard-app [OPTIONS]
Options:
  -c, --csar-file TEXT  The csar file to be onboarded
  -h, --help            Show this message and exit.

Example:
eiap_utils.py -k ossautoapp -ssv onboard-app -c eric-oss-5gcnr-1.1.68-0.csar 
```

The script will call App Onboarding REST API, will upload the file, and wait for the onboarding status of the rApp, and finally 
prints out the onboarding result.  

The onboarded applications can be listed with the following command:  
```
Usage: eiap_utils.py -k <cluster configuration> get-onboarded-apps [OPTIONS]
Options:
  -a, --list-all  List all statuses
  -h, --help      Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv get-onboarded-apps -a
```

This provides a brief list of onboarded apps, but it doesn't show the json reply details from the App Manager


### __1.2. rApp instantiation__

An onboarded rApp can be instantiated by providing the onboarding id.  
```
Usage: eiap_utils.py -k <cluster configuration> instantiate-app [OPTIONS]
Options:
  -i, --app-id TEXT        The onboarded app id to be instantiated
  -p, --payload-file TEXT  The site_values payload json file name
  -h, --help               Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv instantiate-app -i 1 -p cnr_app_init.json
```
The payload file has a json content, and it provides additional parameters for rApp instantiation 
(similar to site values used in case of helm install). This is an example of such a file:
```
{
  "appId": "x",
  "additionalParameters": {
    "global.hosts.iam": "iam.662502336946.eu-west-1.ac.ericsson.se",
    "global.hosts.pf":  "th.662502336946.eu-west-1.ac.ericsson.se",
    "global.serviceMesh.enabled": "true",
    "global.serviceMesh.ingress.enabled": "true",
    "global.security.tls.enabled": "false",
    "namespace": "idunossautoapp01",
    "integration.policyFramework.credentials.login": "cnr-user",
    "integration.policyFramework.credentials.password": "change_me",
    "integration.policyFramework.enabled": true,
    "integration.policyFramework.hooks.enabled": true
  }
}
```

The script will call the App LCM REST API, will pass the parameters given, and will wait and show the result of the 
rApp instantiation.  

The list of instantiated apps can be checked with the following command:  
```
Usage: eiap_utils.py -k <cluster configuration> get-instantiated-apps [OPTIONS]
Options:
  -a, --list-all  List all statuses
  -h, --help      Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv get-instantiated-apps -a
```

This provides a brief list of instantiated apps, but it doesn't show the json reply details from the App LCM. 


### __1.3. rApp instance termination__  

An instantiated rApp can be terminated (stopped) by providing the instantiation id.  
```
Usage: eiap_utils.py -k <cluster configuration> terminate-app [OPTIONS]
Options:
  -i, --instantiated-app-id TEXT  The instantiated app id to be terminated
  -h, --help                      Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv terminate-app -i 2
```

The script will call the App LCM REST API termination function and will wait and show the result of the 
rApp termination process.  


### __1.4. rApp deletion__  

An onboarded rApp can be deleted (removed from the system) by providing the onboarded app id.  
```
Usage: eiap_utils.py -k <cluster configuration> delete-app [OPTIONS]
Options:
  -i, --onboarded-app-id TEXT  The onboarded app id to be deleted
  -h, --help                      Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv delete-app -i 1
```

The script will call the App LCM REST API delete function and will wait and show the result of the 
rApp delete process.  


### __1.5. rApp upgrade__  

An instantiated rApp can be upgraded to another version by providing information about the app to be upgraded.  
The command to be used is this:  
```
Usage: eiap_utils.py -k <cluster configuration> upgrade-app [OPTIONS]
Options:
  -n, --app-name TEXT      The instantiated app name to upgrade
  -v, --oapp-version TEXT  The onboarded app version to upgrade to
  -p, --payload-file TEXT  The site_values payload json file name
  -u, --use-lcmupgrade     Use the lcm upgrade feature vs
                           terminate/instantiate
  -h, --help               Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv upgrade-app -n eric-oss-5gcnr -v "1.1.97-1" -u 
```

The script will call the App LCM REST API to terminate/instatiate/upgrade function and will wait and show the result of the 
rApp upgrade process.  

Using flag -u means that the upgarde is not done with termination and instantiation afterwards, but use the App LCM 
built-in functionality for upgrade (that will result in almost no downtime).  


## __2. 5G CNR functionality testing functions__  

The functions listed below are helping interacting with 5G CNR rApp API, and also executing partial 5G CNR use cases directly 
using CTS and NCMP API interfaces.  


### __2.1. Start 5G CNR NRC function__  

A 5G CNR task can be started with the following command:  
```
Usage: eiap_utils.py -k <cluster configuration> start-nrc [OPTIONS]
Options:
  -p, --payload-file TEXT  The NRC request payload json file
  -c, --cell-id TEXT       The enodeb cell id to start request for
  -d, --distance INTEGER   The distance to be checked in meters
  -h, --help               Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv start-nrc -c 120 -d 400
```
The command assumes that the 5G CNR is already deployed on the selected platform, it doesn't do an instantiation of 5G CNR.  
The command calls the 5G CNR API and starts an NRC task, waits for the feedback task ID, starts a monitoring request, waits 
for finalizing the task and presents the task result.  


### __2.2. Query 5G CNR NRC task__  

A 5G CNR NRC task status can be queried using the ID of the task:  
```
Usage: eiap_utils.py -k <cluster configuration> query-nrc [OPTIONS]
Options:
  -m, --monitor-id TEXT  The NRC request monitoring id
  -h, --help             Show this message and exit.
Example:
eiap_utils.py -k manalab_c -ssv query-nrc -m ce5e8b7f-4c45-4f4a-b115-fa659081a827
```
The command calls the 5G CNR API, starts a monitoring request, waits 
for finalizing the task and presents the task result.  


### __2.3. Run partial 5G CNR NRC use case directly__  

The reason of this function is to run through the use case implemented in 5G CNR, and execute the query functions through 
direct calls toward the CTS and NCMP interfaces. The command used is this:  
```
Usage: eiap_utils.py -k <cluster configuration> run-nrc [OPTIONS]
Options:
  -d, --distance TEXT   The distance to be analysed
  -e, --enodeb-id TEXT  The enodeb id to be queried
  -h, --help            Show this message and exit.
Example:
eiap_utils.py -k manalab_c -ssv run-nrc -e 36 -d 200
```


## __3. Test data checking and initialization support for 5G CNE end2end tests__

There are a few commands implemented to support 5G CNR end2end test cases by providing check, initialization and cleanup 
functions for the test nodes configured in the eiap_utils script configuration file. The configuration file contains 
4G(LTE) and 5G(NR) nodes definitions, that are used by the commands.  
This is a typical definition structure:
```
<test data group name>_test_data:
  nodes_4G:
    <cm_handle>:
      name: <node FDN>
      coordinates: [<longitude>, <latitude>]
      frequency: <node frequency>
      distance: <node radius>
  nodes_5G:
    <cm_handle>:
      name: <node FDN>
      coordinates: [<longitude>, <latitude>]
      frequency: <node frequency>
      distance: <node radius>
```
and this is an example configuration:
```
staging_test_data:
  nodes_4G:
    868505FE0E8E3C8E6E07107037BFA1CE:
      name: Europe/Ireland/NETSimW/LTE416dg2ERBS00001/1
      coordinates: [-6.2867, 53.34198]
      frequency: 3
      distance: 40
    F22728495FB9CD89F81E71C90DE6C833:
      name: Europe/Ireland/NETSimW/LTE416dg2ERBS00002/1
      coordinates: [-6.2627, 53.34198]
      frequency: 4
      distance: 40
  nodes_5G:
    395221E080CCF0FD1924103B15873814:
      name: Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1
      coordinates: [-6.2887, 53.34198]
      frequency: 2073333
      distance: 40
    87968772573A59E96B623AB065876231:
      name: Europe/Ireland/NR01gNodeBRadio00002/NR01gNodeBRadio00002/1
      coordinates: [-6.2817, 53.34198]
      frequency: 2074999
      distance: 40
```


### __3.1. ENM connectivity testing__  

This function goes through the test data in the configuration and verifies if at least one of the node cm-handles can be 
queried from ENM through NCMP interface (checking the PLMN ID list). If it fails, the script exits with a non-zero 
exit value.  
```
Usage: eiap_utils.py -k <cluster configuration> check-enm-connections [OPTIONS]
Options:
  -td, --test-data TEXT  Test data set
  -h, --help             Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv check-enm-connections -td staging_test_data
```
The command is used in the auto app staging pipeline for validating ENM connection.  


### __3.2. Test data initialization in NCMP__  

This function goes through the test data in the configuration, verifies if 4G and 5G test nodes are present in CTS, then verifies 
if test data is properly configured for LTE nodes in the ENM (by checking GUtranSyncSignal settings), and if those are not
configured, but present, it will start a configuration process for all the test nodes.  
The logic of test data preparation is documented in the confluence pages of team Aether.  
The command used for that is this  
```
Usage: eiap_utils.py -k <cluster configuration> init-staging-test-data-ncmp [OPTIONS]
Options:
  -td, --test-data TEXT  Test data set
  -f, --force-update     Force update of test data, even if it is already set
  -h, --help             Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv init-staging-test-data-ncmp
```
The command is used in the auto app staging pipeline for configuring the nodes in ENM automatically.  


### __3.3. Test data geolocation initialization in CTS__  

This function goes through the test data in the configuration, verifies if 4G and 5G test nodes are present in CTS, then 
verifies if geolocation test data is properly configured for for one LTE node, and if that is not
configured, but nodes are present present, it will start a geolocation configuration process for all the test nodes.  
The sites and locations of node cells are created in CTS with its datasync API, and the location coordinates are randomly 
assigned around the node given location using the configured radius.  
The command used for that is this  
```
Usage: eiap_utils.py -k <cluster configuration> init-staging-test-geodata-cts [OPTIONS]
Options:
  -td, --test-data TEXT    Test data set
  -ra, --use-random-angle  Set locations around a circle
  -f, --force-update       Force update of test data, even if it is already
                           set
  -h, --help               Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv init-staging-test-geodata-cts
```
The command is used in the auto app staging pipeline for setting locations automatically in CTS, for the test data, 
if those are missing.  


### __3.4. Clean-up external cells (X2 links)__  

This function goes through the test LTE nodes in the configuration, verifies if external cells (X2 links) are present 
and deletes them, to prepare the nodes for a new test NRC procedure.  
The command used for that is this  
```
Usage: eiap_utils.py -k <cluster configuration> cleanup-external-cells [OPTIONS]
Options:
  -td, --test-data TEXT  Test data set
  -f, --filter-id TEXT   Filter for removing X2 links (e.g. 5G node name)
  -l, --list-only        List the X2 links only and not delete them
  -h, --help             Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv cleanup-external-cells -f NR01gNodeBRadio00007
```
The command is used in the auto app staging pipeline in pre-test preparation phase to clean up X2 links created 
during the end2end tests.  

There are two other commands implemented for node clean-up purposes, but those were used for handling misconfiguration 
cases only:
```
eiap_utils.py -k <cluster configuration> cleanup-gnbdus [OPTIONS]
eiap_utils.py -k <cluster configuration> cleanup-gnbdu-nrcelldu [OPTIONS]
```


## __4. CTS and NCMP query functions__

There are some helper command implemented to query some node data from CTS and NCMP.  

### __4.1. CTS nodes/cells query__  

This function invokes the CTS REST API to query node and cell MO details, and displays the results in a compact manner.  
The command used for that is this  
```
Usage: eiap_utils.py -k <cluster configuration> get_nodes [OPTIONS]
Options:
  -t, --node-type TEXT     The cell type to be queried (i.e gnbdu, enodeb, gnbcucp, nrcell, ltecell)
  -n, --node-id TEXT       The cell id to be queried
  -m, --max-nodes INTEGER  The maximum number of nodes shown
  -h, --help               Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv get-nodes -t gnbdu -m 200
```


### __4.2. NCMP resource/MO query__  

This function invokes the NCMP REST API to query node and cell MO details, and displays the result json.  
The command used for that is this  
```
Usage: eiap_utils.py -k <cluster configuration> get-ncmp-resource [OPTIONS]
Options:
  -c, --cmhandle TEXT  The cmhandle to be queried
  -r, --resource TEXT  The resource to be queried
  -h, --help           Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv get-ncmp-resource  -c 868505FE0E8E3C8E6E07107037BFA1CE -r "ericsson-enm-Lrat:ENodeBFunction"
```

## __5. Helper functions for 5G CNR test execution support__

The following functions are implemented for automated execution of some 5G CNR use cases related to commercial readiness 
preparation  
These functions are using Kubernetes API for getting information about the microservice status, so they need a properly 
configured kube configuration.  


### __5.1. Characteristic report generation__  

The command is executed in the staging environment for getting some 5G CNR microservice related characteristics 
parameters from the cluster where the 5G CNR microservice is deployed.  
The command used for that is this:
```
Usage: eiap_utils.py -k <cluster configuration> get-characteristics [OPTIONS]
Options:
  -n, --app-name TEXT        The deployed app name to get characteristics for
  -c, --container-name TEXT  The deployed pod container name to get
                             characteristics for
  -p, --payload-file TEXT    The site_values payload json file name
  -t, --template-file PATH   The json template file to generate report from
  -o, --output-file PATH     The json report
  -h, --help                 Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv get-characteristics
```

### __5.2. Robustness test with resource scaling__  

The command used for that is this:
```
Usage: eiap_utils.py -k <cluster configuration> robustness-test-scaling [OPTIONS]
Options:
  -n, --app-name TEXT            The deployed app name to get robustness test
                                 for
  -r, --payload-file TEXT        The NRC payload json file name
  -n, --instance-number INTEGER  The number of instances to scale up to
  -h, --help                     Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv robustness-test-scaling
```

### __5.3. Robustness test with SIGTERM invocation__  

The command used for that is this:
```
Usage: eiap_utils.py -k <cluster configuration> robustness-test-sigterm [OPTIONS]
Options:
  -n, --app-name TEXT           The deployed app name to get robustness test
                                for
  -r, --payload-file TEXT       The NRC payload json file name
  -t, --timeout-period INTEGER  Liveliness timeout period in seconds
  -h, --help                    Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv robustness-test-sigterm
```

### __5.3. Limited load test__  

This command triggers the execution of 5G CNR start-Nrc and monitor function for a predefined period.
The command used for that is this:
```
Usage: eiap_utils.py -k <cluster configuration> load-test [OPTIONS]
Options:
  -n, --app-name TEXT      The deployed app name to perform the load test for
  -p, --period TEXT        Load test period in seconds
  -r, --payload-file TEXT  The NRC payload json file name
  -h, --help               Show this message and exit.
Example:
eiap_utils.py -k ossautoapp -ssv load-test
```


## __Dependencies__

The script is using the following additional Python libraries:  
	- *[Click](https://pypi.org/project/click/)* for command line argument parsing.  
	- *[PyYAML](https://pypi.org/project/PyYAML/)* for yaml format handling  
	- *[requests](https://pypi.org/project/requests/)* for REST API interactions  
	- *[requests-toolbelt](https://pypi.org/project/requests-toolbelt/)* for multipart data uploading  
	- *[tqdm](https://pypi.org/project/tqdm/)* for multipart data uploading  
	- *[kubernetes](https://pypi.org/project/kubernetes/)* for interaction with Kubernetes clusters API  


## __Reference materials__

The script development considered the following documents for implementing its functions:  
	- *[App Manager Onboarding API](https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/tor/idun-sdk/latest/services/app-onboarding/developer-guide.html)* for rApp onboarding.  
	- *[App Manager LCM API](https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/sites/tor/idun-sdk/latest/services/app-lcm/developer-guide.html)* for rApp lifecycle management.  

