#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('settings.xml') {

    echo 'Inject settings.xml into workspace:'
    configFileProvider([configFile(fileId: "${env.SETTINGS_CONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}")]) {}
    archiveArtifacts allowEmptyArchive: true, artifacts: '${ci-ruleset}, publish.Jenkinsfile'

}