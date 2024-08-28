#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"


stage('Upload Marketplace Documentation') {
  withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
                    string(credentialsId: 'ENMNA_ADP_PORTAL_API_KEY', variable: 'ADP_PORTAL_API_KEY')]) {
      // upload release version

    echo "Marketplace upload"
    sh "${bob} -r ${ci-ruleset} marketplace-upload-release"

  }

}
stage('create git-tag') {

  if ( params.PUBLISH ){
    withCredentials([usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')]){
      sh "${bob} -r ${ci-ruleset} create-git-tag"
    }
  }
}