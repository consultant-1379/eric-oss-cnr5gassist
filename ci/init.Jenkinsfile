#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('name') {
    authorName = sh(returnStdout: true, script: 'git show -s --pretty=%an')
    currentBuild.displayName = currentBuild.displayName + ' / ' + authorName

}