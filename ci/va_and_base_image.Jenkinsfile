#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('extended VA') {

    if (params.LONG_VA_TESTS) {
        sh "${bob} -r ci/VA-ruleset2.0.yaml nmap-scan"
    }
    sh "${bob} -r ci/VA-ruleset2.0.yaml kubebench-scan"

}
stage('FUZZ'){

    sh 'mkdir -p $DEFENSICS_HOME'
    sh 'tar -xvzf $DEFENSICS_HOME_TAR -C $DEFENSICS_HOME'
    sh "${bob} -r ci/VA-ruleset2.0.yaml fuzz-test"
    sh "${bob} -r ci/VA-ruleset2.0.yaml get-defensics-dir"


    if (params.LONG_VA_TESTS){
        sh "${bob} -r ci/VA-ruleset2.0.yaml zap-test"
    }
}

stage('Commit latest Base Image') {

    if ( params.IS_BASE_IMAGE_UPDATE ){
        commitBaseImage()
    }
}