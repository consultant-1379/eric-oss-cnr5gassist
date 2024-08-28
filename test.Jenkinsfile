#!/usr/bin/env groovy

def eiap_utils = 'cd src/test/resources/utils/; python3 eiap_utils.py -ssv'
def bob = 'python3 bob/bob2.0/bob.py -r ruleset2.0.yaml'

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
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '40', artifactNumToKeepStr: '40', artifactDaysToKeepStr: '14'))
    }

    environment {
        AWS_HOME = "${env.WORKSPACE}/.aws/"
        KUBE_HOME = "${env.WORKSPACE}/.kube/"
        KUBECONFIG = "${env.WORKSPACE}/.kube/config"
        CHART_REPO = "${params.CHART_REPO}"
        CHART_NAME = "${params.CHART_NAME}"
        APIGW_FQDN = "${(params.APIGW_HOST=='') ? '': '-ih '+APIGW_HOST }"
        APIGW_HOST = "${params.APIGW_HOST}"
        DEBUG_FLAG = "${(params.DEBUG=='true') ? '--debug' : '' }"
        STAGING_LEVEL = "PRODUCT"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Clean') {
            steps {
                sh 'git submodule sync'
                sh 'git submodule update --init --recursive'
                sh "rm -rf ./.aws ./.bob/ ./.kube/ ./.cache/ ./charts/*/charts/"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ruleset2.0.yaml, test.Jenkinsfile'
                sh 'pip3 install -r src/test/resources/utils/requirements --user'
            }
        }
        stage('Init') {
            steps {
                sh "echo Empty init step"
            }
        }
        stage('K6 E2E Test') {
            steps {
                sh "${WORKSPACE}/ci/scripts/run_k6_end2end_staging.sh"
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/k6-test-results.html'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/summary.json'
                    publishHTML([allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: '',
                        reportFiles: 'doc/Test_Report/k6-test-results.html',
                        reportName: 'K6 Test Results',
                        reportTitles: ''])
                }
            }
        }
    }
}

