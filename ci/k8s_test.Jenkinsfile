#!/usr/bin/env groovy
//wont run in parallel
def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('copy report') {
    sh "${bob} -r ${ci-ruleset} copy-report"
}