#!/usr/bin/env groovy
//is not the same as our original
def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('helm upgrade') {
    if (env.HELM_UPGRADE == "true") {
        echo "HELM_UPGRADE is set to true:"
        sh "${bob} -r ${ci-ruleset} helm-upgrade"
    }
}