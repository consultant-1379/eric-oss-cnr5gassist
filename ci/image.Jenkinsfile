#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('Publish Images') {

    sh "${bob} -r ${ci-ruleset} publish-images"
}