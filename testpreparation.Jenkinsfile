#!/usr/bin/env groovy

def eiap_utils = 'cd src/test/resources/utils/; python3 eiap_utils.py -ssv'

pipeline {
    agent {
        label env.SLAVE_LABEL
    }

    parameters {
        string(name: 'GERRIT_REFSPEC',
                defaultValue: 'refs/heads/master',
                description: 'Referencing to a commit by Gerrit RefSpec')
        string(name: 'CHART_VERSION',
                defaultValue: 'latest',
                description: 'Version of the staged rApp - not used')
        string(name: 'FUNCTIONAL_USER_SECRET',
                defaultValue: 'cloudman-user-creds',
                description: 'Jenkins secret ID for ARM Registry Credentials')
        string(name: 'SLAVE_LABEL',
                defaultValue: 'evo_docker_engine_gic_IDUN',
                description: 'Specify the slave label that you want the job to run on')
        string(name: 'KUBECONFIG',
                defaultValue: 'ossautoapp01_config',
                description: 'Kubeconfig file')
        string(name: 'APIGW_HOST',
                defaultValue: '',
                description: 'The EIAP API GW host FQDN')
        string(name: 'AWS_CREDENTIALS',
                defaultValue: 'ossautoapp01_aws_credentials',
                description: 'AWS Shared credentials file')
        string(name: 'AWS_CONFIG',
                defaultValue: 'ossautoapp01_aws_config',
                description: 'AWS config file')
        string(name: 'CHART_REPO',
                defaultValue: "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm",
                description: 'The repo. of the chart which will be tested in prod. staging')
        string(name: 'CHART_NAME',
                defaultValue: "eric-oss-5gcnr",
                description: 'The name of the chart which will be tested in prod. staging')
        booleanParam(name: 'FORCE_UPDATE', defaultValue: false)
        booleanParam(name: 'DEBUG', defaultValue: false)
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '40', artifactNumToKeepStr: '40', artifactDaysToKeepStr: '14'))
    }

    environment {
        AWS_HOME = "${env.WORKSPACE}/.aws/"
        KUBE_HOME = "${env.WORKSPACE}/.kube"
        KUBECONFIG = "${env.WORKSPACE}/.kube/config"
        CHART_REPO = "${CHART_REPO}"
        CHART_NAME = "${CHART_NAME}"
        APIGW_FQDN = "${(APIGW_HOST=='') ? '': '-ih '+APIGW_HOST }"
        DEBUG_FLAG = "${(DEBUG=='true') ? '--debug' : '' }"
        FORCEUPDATE = "${(FORCE_UPDATE=='true') ? '--force-update' : '' }"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Clean') {
            steps {
                sh "rm -rf ./.aws ./.bob/ ./.kube/ ./.cache/ ./charts/*/charts/"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ruleset2.0.yaml, testpreparation.Jenkinsfile'
                sh 'pip3 install -r src/test/resources/utils/requirements --user'
            }
        }
        stage('Init and ENM connection check') {
            steps {
                retry(2) {
                    sh "${eiap_utils} \$APIGW_FQDN \$DEBUG_FLAG -ik check-enm-connections"
                }
           }
        }
        stage('Data init in ENM with NCMP') {
            steps {
                sh "${eiap_utils} \$APIGW_FQDN \$DEBUG_FLAG -ik init-staging-test-data-ncmp \$FORCEUPDATE"
            }
        }
        stage('Geolocation data init in CTS') {
            steps {
                sh "${eiap_utils} \$APIGW_FQDN \$DEBUG_FLAG -ik init-staging-test-geodata-cts \$FORCEUPDATE"
            }
        }
        stage('Cleaning External cells on test eNodeBs') {
            steps {
                sh "${eiap_utils} \$APIGW_FQDN \$DEBUG_FLAG -ik cleanup-external-cells"
            }
        }
        stage('Cleaning test gNodeB nodes') {
            steps {
                sh "${eiap_utils} \$APIGW_FQDN \$DEBUG_FLAG -ik cleanup-gnbdus"
            }
        }
    }
}

