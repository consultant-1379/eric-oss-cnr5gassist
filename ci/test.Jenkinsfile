#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('Update to latest Base Image') {
    if (params.IS_BASE_IMAGE_UPDATE ){
        sh "${bob} -r ${ci-ruleset} common-base-update"
    }
}

stage('Apply version'){
    sh "${bob} -r ${ci-ruleset} apply-version"
}