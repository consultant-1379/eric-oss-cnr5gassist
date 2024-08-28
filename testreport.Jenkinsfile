#!/usr/bin/env groovy

def defaultBobImage = 'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:1.7.0-87'
def bob = new BobCommand()
    .bobImage(defaultBobImage)
    .envVars([
        HOME: '${HOME}',
        AWS_HOME: '${AWS_HOME}',
        KUBE_HOME: '${KUBE_HOME}',
        KUBECONFIG: '${KUBECONFIG}',
        AWS_CONFIG_FILE: '${AWS_CONFIG_FILE}',
        AWS_SHARED_CREDENTIALS_FILE: '${AWS_SHARED_CREDENTIALS_FILE}',
        FUNCTIONAL_USER_USERNAME: '${FUNCTIONAL_USER_USERNAME}',
        FUNCTIONAL_USER_PASSWORD: '${FUNCTIONAL_USER_PASSWORD}',
        AETHER_ARM_TOKEN: '${AETHER_ARM_TOKEN}',
        CHART_NAME: '${CHART_NAME}',
        CHART_VERSION: '${CHART_VERSION}',
        CHART_REPO: '${CHART_REPO}'
    ])
    .needDockerSocket(true)
    .toString()

pipeline {
    agent {
        label env.SLAVE_LABEL
    }

    parameters {
        string(name: 'GERRIT_REFSPEC',
                defaultValue: 'refs/heads/master',
                description: 'Referencing to a commit by Gerrit RefSpec')
        string(name: 'CHART_VERSION',
                description: 'Version of the staged rApp')
        string(name: 'FUNCTIONAL_USER_SECRET',
                defaultValue: 'cloudman-user-creds',
                description: 'Jenkins secret ID for ARM Registry Credentials')
        string(name: 'SLAVE_LABEL',
                defaultValue: 'evo_docker_engine_gic_IDUN',
                description: 'Specify the slave label that you want the job to run on')
        string(name: 'KUBECONFIG',
                defaultValue: 'ossautoapp01_config',
                description: 'Kubeconfig file')
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
        booleanParam(name: 'RUN_CHAR_TESTS', defaultValue: true)
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
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Clean') {
            steps {
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ruleset2.0.yaml, test.Jenkinsfile'
                sh "rm -f .stages; ${bob} clean-stubs"
            }
        }
        stage('Init') {
            steps {
                withCredentials([
                  file(credentialsId: "${params.KUBECONFIG}", variable: 'KUBECONFIG'),
                  file(credentialsId: "${params.AWS_CREDENTIALS}", variable: 'AWS_SHARED_CREDENTIALS_FILE'),
                  file(credentialsId: "${params.AWS_CONFIG}", variable: 'AWS_CONFIG_FILE')
                ]) {
                    sh """#!/bin/bash
                    |mkdir -p ${env.AWS_HOME} ${env.KUBE_HOME}
                    |cp $KUBECONFIG ${env.KUBE_HOME}/config
                    |cp $AWS_CONFIG_FILE ${env.AWS_HOME}/config
                    |cp $AWS_SHARED_CREDENTIALS_FILE ${env.AWS_HOME}/credentials""".stripMargin()

                    retry(2) {
                        sh "${bob} 5gcnr-init-test"
                    }
                }
            }
        }
        stage('Characteristics and Robustness Tests') {
            when {
                expression { params.RUN_CHAR_TESTS }
            }
            steps {
                withCredentials([
                    file(credentialsId: "${params.KUBECONFIG}", variable: 'KUBECONFIG'),
                    file(credentialsId: "${params.AWS_CREDENTIALS}", variable: 'AWS_SHARED_CREDENTIALS_FILE'),
                    file(credentialsId: "${params.AWS_CONFIG}", variable: 'AWS_CONFIG_FILE')
                ]) {
                    sh "${bob} 5gcnr-char-test"
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'src/test/resources/utils/config/eric-oss-5gcnr_characteristic_report.json'
                }
            }
        }
        stage('Upload characteristic report'){
            steps {
                withCredentials([string(credentialsId: 'aether_arm_token', variable: 'AETHER_ARM_TOKEN')]) {
                    sh "${bob} upload-characteristics-report-dev"
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: 'build/yang-output/*.xml'
            }
        }
    }
}

def addHelmDRWarningIcon() {
    def val = readFile '.bob/var.helm-chart-check-report-warnings'
    if (val.trim().equals("true")) {
        echo "WARNING: One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html"
        manager.addWarningBadge("One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html")
    } else {
        echo "No Helm Design Rules have a WARNING state"
    }
}

// More about @Builder: http://mrhaki.blogspot.com/2014/05/groovy-goodness-use-builder-ast.html
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class BobCommand {
    def bobImage = 'bob.2.0:latest'
    def envVars = [:]
    def needDockerSocket = false

    String toString() {
        def env = envVars
                .collect({ entry -> "-e ${entry.key}=\"${entry.value}\"" })
                .join(' ')

        def cmd = """echo \"\\\"\$STAGE_NAME\\\" `date '+%s'`\" >> .stages;
            |docker run
            |--init
            |--rm
            |--workdir \${PWD}
            |--user \$(id -u):\$(id -g)
            |-v \${PWD}:\${PWD}
            |-v /etc/group:/etc/group:ro
            |-v /etc/passwd:/etc/passwd:ro
            |-v \${HOME}:\${HOME}
            |${needDockerSocket ? '-v /var/run/docker.sock:/var/run/docker.sock' : ''}
            |${env}
            |\$(for group in \$(id -G); do printf ' --group-add %s' "\$group"; done)
            |--group-add \$(stat -c '%g' /var/run/docker.sock)
            |${bobImage}
            |-r ./ruleset2.0.yaml
            |"""
        return cmd
                .stripMargin()           // remove indentation
                .replace('\n', ' ')      // join lines
                .replaceAll(/[ ]+/, ' ') // replace multiple spaces by one
    }
}