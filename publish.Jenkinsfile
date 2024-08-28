#!/usr/bin/env groovy

def defaultBobImage = 'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:1.7.0-55'
def bob_mimer = 'python3 bob/bob2.0/bob.py -r ruleset2.0.yaml'
def bob = new BobCommand()
    .bobImage(defaultBobImage)
    .envVars([
        HOME:'${HOME}',
        ISO_VERSION:'${ISO_VERSION}',
        RELEASE:'${RELEASE}',
        SONAR_HOST_URL:'${SONAR_HOST_URL}',
        SONAR_AUTH_TOKEN:'${SONAR_AUTH_TOKEN}',
        GERRIT_CHANGE_NUMBER:'${GERRIT_CHANGE_NUMBER}',
        KUBECONFIG:'${KUBECONFIG}',
        K8S_NAMESPACE: '${K8S_NAMESPACE}',
        USER:'${USER}',
        SELI_ARTIFACTORY_REPO_USER:'${CREDENTIALS_SELI_ARTIFACTORY_USR}',
        SELI_ARTIFACTORY_REPO_PASS:'${CREDENTIALS_SELI_ARTIFACTORY_PSW}',
        SERO_ARTIFACTORY_REPO_USER:'${CREDENTIALS_SERO_ARTIFACTORY_USR}',
        SERO_ARTIFACTORY_REPO_PASS:'${CREDENTIALS_SERO_ARTIFACTORY_PSW}',
        MAVEN_CLI_OPTS: '${MAVEN_CLI_OPTS}',
        OPEN_API_SPEC_DIRECTORY: '${OPEN_API_SPEC_DIRECTORY}',
        XRAY_USER:'${CREDENTIALS_XRAY_SELI_ARTIFACTORY_USR}',
        XRAY_APIKEY:'${CREDENTIALS_XRAY_SELI_ARTIFACTORY_PSW}',
        FOSSA_API_KEY: '${CREDENTIALS_FOSSA_API_KEY}',
        BAZAAR_TOKEN:'${BAZAAR_TOKEN}',
        DEFENSICS_HOME:'${DEFENSICS_HOME}',
        GERRIT_USERNAME: '${CREDENTIALS_GERRIT_USR}',
        GERRIT_PASSWORD: '${CREDENTIALS_GERRIT_PSW}',
        ERIDOC_USERNAME:'${ERIDOC_USERNAME}',
        ERIDOC_PASSWORD:'${ERIDOC_PASSWORD}',
        VHUB_API_TOKEN: '${VHUB_API_TOKEN}',
        ADP_PORTAL_API_KEY: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJtc2lkIjoiNjI3MTFjZjYyOGJiNTkwMDkxMjRlZWNmIiwiaW52YWxfc2VjcmV0IjoiYzg0cTZqOGR3OSJ9._Nz9HXsG7BpcnAy4PyE2WD02vy10fulHcwHRuvSKSQI',
    ])
    .needDockerSocket(true)
    .toString()

def LOCKABLE_RESOURCE_LABEL = "kaas"

pipeline {
    agent {
        node {
            label NODE_LABEL
        }
    }

    parameters {
        string(name: 'SETTINGS_CONFIG_FILE_NAME', defaultValue: 'maven.settings.oss.nexus')
        string(name: 'NODE_LABEL', defaultValue: 'GridEngine')
        string(name: 'GERRIT_REFSPEC', defaultValue: 'refs/heads/master')
        string(name: 'HELM_DR_CHECK_DISTRIBUTION_LIST', defaultValue: 'PDLEAMAETH@pdl.internal.ericsson.com',
            description: 'Distribution list of people to be notified if a "WARNING status" found in the Helm DR Check. Should include owner of the service')
        booleanParam(name: 'PUBLISH', defaultValue: true)
        booleanParam(name: 'RUN_PLMS_RELEASE', defaultValue: false)
        booleanParam(name: 'IS_BASE_IMAGE_UPDATE', defaultValue: false)
        booleanParam(name: 'LONG_VA_TESTS', defaultValue: false)
        booleanParam(name: 'LONG_VA_NMAP_SCAN', defaultValue: false)
        booleanParam(name: 'LONG_VA_ZAP_TEST', defaultValue: false)
        booleanParam(name: 'UPLOAD_VA_TESTS', defaultValue: false)
        booleanParam(name: 'ENABLE_FOSSA', defaultValue: true)
    }

    options {
        timestamps()
        timeout(time: 5, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '50'))
        parallelsAlwaysFailFast()
    }

    environment {
        RELEASE = "true"
        TEAM_NAME = "Aether"
        KUBECONFIG = "${WORKSPACE}/.kube/config"
        CREDENTIALS_SELI_ARTIFACTORY = credentials('SELI_ARTIFACTORY')
        CREDENTIALS_SERO_ARTIFACTORY = credentials('SERO_ARTIFACTORY')
        CREDENTIALS_FOSSA_API_KEY = "578d227de326b8c017ca4d277f7a2aaf"
        MAVEN_CLI_OPTS = "-Duser.home=${env.HOME} -B -s ${env.WORKSPACE}/settings.xml"
        OPEN_API_SPEC_DIRECTORY = "src/main/resources/v1"
        CREDENTIALS_XRAY_SELI_ARTIFACTORY = credentials('XRAY_SELI_ARTIFACTORY')
        DEFENSICS_HOME="${env.WORKSPACE}/.bob/defensics_home"
        RUN_VA = "true"
        CREDENTIALS_GERRIT = credentials('GERRIT_PASSWORD')
        VHUB_API_TOKEN = credentials('vhub-api-key-id')
        INIT_RULE = "${(RUN_PLMS_RELEASE=='true') ? 'init-release' : 'init-drop' }"
        VA_REPORT_HANDLING = "${(UPLOAD_VA_TESTS=='true' & PUBLISH=='true') ? 'upload' : 'fetch_vulnerability' }"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Clean') {
            steps {
                sh 'git submodule sync'
                sh 'git submodule update --init --recursive'
                echo 'Inject settings.xml into workspace:'
                configFileProvider([configFile(fileId: "${env.SETTINGS_CONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}")]) {}
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ruleset2.0.yaml, release.Jenkinsfile'
                sh "${bob_mimer} clean"
                sh 'pip3 install -r src/test/resources/utils/requirements --user'
            }
        }

        stage('Init') {
            steps {
                sh "${bob_mimer} ${INIT_RULE}"
                archiveArtifacts 'artifact.properties'
                script {
                    authorName = sh(returnStdout: true, script: 'git show -s --pretty=%an')
                    currentBuild.displayName = currentBuild.displayName + ' / ' + authorName
                }
            }
        }

        stage('Lint') {
            steps {
                parallel(
                    "lint markdown": {
                        sh "${bob_mimer} lint:markdownlint lint:vale"
                    },
                    "lint helm": {
                        sh "${bob_mimer} lint:helm"
                    },
                    "lint helm design rule checker": {
                        sh "${bob_mimer} lint:helm-chart-check"
                    },
                    "lint code": {
                        sh "${bob_mimer} lint:license-check"
                    },
                    "lint OpenAPI spec": {
                        sh "${bob_mimer} lint:oas-bth-linter"
                    },
                    "lint metrics": {
                        sh "${bob_mimer} lint:metrics-check"
                    }
                )
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/*bth-linter-output.html, **/design-rule-check-report.*'
                }
            }
        }

        stage('Generate') {
            when { expression { env.RUN_PLMS_RELEASE == "true"  } }
            steps {
                parallel(
                    "Open API Spec": {
                        sh "${bob} rest-2-html:check-has-open-api-been-modified"
                        script {
                            def val = readFile '.bob/var.has-openapi-spec-been-modified'
                            if (val.trim().equals("true")) {
                                sh "${bob_mimer} rest-2-html:zip-open-api-doc"
                                sh "${bob_mimer} rest-2-html:generate-html-output-files"

                                manager.addInfoBadge("OpenAPI spec has changed. HTML Output files will be published to the CPI library.")
                                archiveArtifacts artifacts: "rest_conversion_log.txt"
                            }
                        }
                    },
                    "Generate Docs": {
                        sh "${bob_mimer} generate-docs"
                        archiveArtifacts 'build/doc/**/*.*'
                        publishHTML (target: [
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'build/doc',
                            reportFiles: 'CTA_api.html',
                            reportName: 'REST API Documentation'
                        ])
                    },
                    "Generate preliminary PRI": {
                         withCredentials([usernamePassword(credentialsId: 'eridoc-user', usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD'),
                                          usernamePassword(credentialsId: 'eridoc-user', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                                          usernamePassword(credentialsId: 'eridoc-user', usernameVariable: 'JIRA_USERNAME', passwordVariable: 'JIRA_PASSWORD')
                                         ])
                         {
                             sh "${bob_mimer} generate-pri"
                             publishHTML (target: [
                                 allowMissing: false,
                                 alwaysLinkToLastBuild: false,
                                 keepAll: true,
                                 reportDir: 'build/pri',
                                 reportFiles: 'pri.html',
                                 reportName: "PRI"
                             ])
                             archiveArtifacts 'build/documents.yaml'
                             archiveArtifacts 'build/pri/pri_input.json'
                             archiveArtifacts 'build/pri/pri.html'
                             archiveArtifacts 'build/pri/pri.json'
                             archiveArtifacts 'build/pri/5GCNR_Assist_PRI.pdf'
                         }
                    }
                )
            }
        }

        stage('Build') {
            steps {
                sh "${bob_mimer} build"
            }
        }

        stage('Test') {
            steps {
                sh "${bob} test"
                archiveArtifacts '.bob/surefire-report.html'
                publishHTML (target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: '.bob',
                    reportFiles: 'surefire-report.html',
                    reportName: 'Junit Test'
                ])
            }
        }

        stage('SonarQube') {
            when {
                expression { env.SQ_ENABLED == "true" }
            }
            steps {
                withSonarQubeEnv("${env.SQ_SERVER}") {
                    sh "${bob_mimer} sonar-enterprise-release"
                }
            }
        }
        //this has to be removed for RUN_PLMS_RELEASE
        stage('Update to latest Base Image') {
            when {
                expression { params.IS_BASE_IMAGE_UPDATE }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY',
                                usernameVariable: 'SELI_ARTIFACTORY_REPO_USER',
                                passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                                sh "${bob_mimer} common-base-update"
                }
            }
        }

        stage('Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY',
                    usernameVariable: 'SELI_ARTIFACTORY_REPO_USER',
                    passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                     sh "${bob} apply-version"
                     sh "${bob} image"
                     sh "${bob} image-dr-check"
                     sh "${bob} publish-images"
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/image-design-rule-check-report*'
                }
            }
        }

        stage('Package') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY',
                    usernameVariable: 'SELI_ARTIFACTORY_REPO_USER',
                    passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                     sh "${bob_mimer} package"
                     sh "${bob_mimer} package-jars"
                }
            }
        }

        stage('K8S Resource Lock') {
            options {
                lock(label: LOCKABLE_RESOURCE_LABEL, variable: 'RESOURCE_NAME', quantity: 1)
            }
            environment {
                K8S_CLUSTER_ID = sh(script: "echo \${RESOURCE_NAME} | cut -d'_' -f1", returnStdout: true).trim()
                K8S_NAMESPACE = sh(script: "echo \${RESOURCE_NAME} | cut -d',' -f1 | cut -d'_' -f2", returnStdout: true).trim()
            }
            stages {
                stage('Helm Install') {
                    steps {
                        echo "Inject kubernetes config file (${env.K8S_CLUSTER_ID}) based on the Lockable Resource name: ${env.RESOURCE_NAME}"
                        configFileProvider([configFile(fileId: "${env.K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                        echo "The namespace (${env.K8S_NAMESPACE}) is reserved and locked based on the Lockable Resource name: ${env.RESOURCE_NAME}"

                        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                          sh "${bob} helm-dry-run"
                          sh "${bob} create-namespace"
                        }

                        script {
                            if (env.HELM_UPGRADE == "true") {
                                echo "HELM_UPGRADE is set to true:"
                                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                                  sh "${bob} helm-upgrade"
                                }
                            } else {
                                echo "HELM_UPGRADE is NOT set to true:"
                                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                                  sh "${bob} helm-install"
                                  if (params.LONG_VA_TESTS) {
                                      sh "${bob} helm-install"
                                  } else {
                                      sh "${bob} helm-install-long-va"
                                  }
                                }
                            }
                        }

                        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                          sh "${bob} healthcheck"
                        }
                    }
                    post {
                        always {
                          withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                            sh "${bob} kaas-info || true"
                          }
                          archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
                        }
                    }
                }

                stage('K8S Test') {
                    steps {
                        parallel(
                            "Preform Test": {
                               withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                                   sh "${bob} helm-test va-ruleset.init"
                               }
                            },
                            "Copy Test report": {
                                sh "${bob} copy-report"
                            }
                        )
                    }
                    post {
                        always {
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'test/k6-test-results.html'
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'test/summary.json'
                            publishHTML([allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: '',
                                reportFiles: 'test/k6-test-results.html',
                                reportName: 'K6 Test Results',
                                reportTitles: ''])
                        }
                    }
                }

                stage('Vulnerability Analysis') {
                    stages{
                        stage('Parallel'){
                            environment {
                                BAZAAR_TOKEN = credentials('BAZAAR_token')
                            }
                            when {
                                expression { env.RUN_VA == "true" }
                            }
                            steps {
                                parallel(
                                    "X-Ray": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml fetch-xray-report"

                                        archiveArtifacts '.bob/xray-reports/*xray_report.json'
                                        archiveArtifacts '.bob/dependencies*'
                                    },
                                    "Kubeaudit": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml kubeaudit-scan"
                                    },
                                    "Trivy": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml trivy-scan"
                                    },
                                    "Anchore-Grype": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml anchore-grype-scan"
                                    },
                                    /*"Kubehunter": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml kubehunter-scan"
                                    },*/
                                    "Kubesec": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml kubesec-scan"
                                    },
                                    "Hadolint": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml hadolint-scan"
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml evaluate-design-rule-check-resultcodes"
                                    },
                                    "Nmap": {
                                        script {
                                            if (params.LONG_VA_TESTS && params.LONG_VA_NMAP_SCAN) {
                                                sh "${bob} -r ci/VA-ruleset2.0.yaml nmap-scan"
                                            }
                                        }
                                    },
                                    "Kubebench": {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml kubebench-scan"
                                    }
                                )
                            }
                        }

                        stage('FUZZ'){
                            environment {
                                DEFENSICS_HOME_TAR = credentials('defensics_home')
                            }
                            steps{
                                sh 'mkdir -p $DEFENSICS_HOME'
                                sh 'tar -xvzf $DEFENSICS_HOME_TAR -C $DEFENSICS_HOME'
                                script {
                                    if (params.LONG_VA_TESTS || params.RUN_PLMS_RELEASE){
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml fuzz-test"
                                    }
                                }
                                sh "${bob} -r ci/VA-ruleset2.0.yaml get-defensics-dir"
                            }
                        }

                        stage('ZAP Scan'){
                            steps{
                                script{
                                    if (params.LONG_VA_TESTS && params.LONG_VA_ZAP_TEST){
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml zap-test"
                                    }
                                }
                            }
                        }

                        stage('Generate Vulnerability report V2.0'){
                            steps{
                                script {
                                    if (params.LONG_VA_TESTS) {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml va-report_long:${VA_REPORT_HANDLING}"
                                    } else if (params.RUN_PLMS_RELEASE) {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml va-report:${VA_REPORT_HANDLING}"
                                    } else {
                                        sh "${bob} -r ci/VA-ruleset2.0.yaml va-report_light:${VA_REPORT_HANDLING}"
                                    }
                                }
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts '.bob/va-reports/'
                        }
                    }
                }
            }
            post {
                unsuccessful {
                    sh "${bob} collect-k8s-logs || true"
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'k8s-logs/*'
                }
                cleanup {
                    sh "${bob} delete-namespace"
                }
            }
        }

        stage('Commit latest Base Image') {
            when {
                expression { params.IS_BASE_IMAGE_UPDATE }
            }
            steps {
                commitBaseImage()
            }
        }

        stage('Publish') {
            steps {
                script{
                    if(params.PUBLISH){
                        sh "${bob} publish"
                    }else{
                        currentBuild.result = "FAILURE"
                        echo "Failing due to PUBLISH being set to false"
                    }
                }
            }
        }

        stage ('FOSSA Analyze') {
            when {
                expression { params.ENABLE_FOSSA }
            }
            steps {
                sh "${bob} fossa-analyze"
            }
        }

        stage ('FOSSA Fetch Report') {
            when {
                expression { params.ENABLE_FOSSA }
            }
            steps {
                sh "${bob} fossa-scan-status-check"
                sh "${bob} fetch-fossa-report-attribution"
                archiveArtifacts '*fossa-report.json'
            }
        }

        stage ('FOSSA Dependency Validate') {
            when {
                expression { params.ENABLE_FOSSA }
            }
            steps {
                sh "${bob} dependency-validate"
            }
        }

        stage ('Generate License Agreement') {
            when {
                expression { params.ENABLE_FOSSA & params.RUN_PLMS_RELEASE }
            }
            steps {
                sh "${bob} license-agreement"
            }
        }

        stage('Munin Update') {
            when {
                expression { params.RUN_PLMS_RELEASE }
            }
            steps {
                withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                    sh "${bob_mimer} munin-update-version"
                }
            }
        }

        stage ('Generate SVL') {
            when {
                expression { params.RUN_PLMS_RELEASE }
            }
            steps {
                withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                    echo "Generate 3PP usage list file from Munin"
                    sh "${bob_mimer} generate-svl"
                }
            }
        }

        stage('Upload Marketplace Documentation') {
            when {
                expression { params.RUN_PLMS_RELEASE }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                    // upload release version
                    script {
                        echo "Marketplace upload"
                        sh "${bob_mimer} marketplace-upload-release"
                    }
                }
            }
        }

        stage('Upload CPI fragment') {
            when {
                expression { params.RUN_PLMS_RELEASE }
            }
            steps {
                sh "${bob} upload-cpi-fragment"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'build/yang-output/*.xml'
            }
        }

        stage('Upload EriDoc Documentation') {
            when {
                expression { params.RUN_PLMS_RELEASE }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'eridoc-user', usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD')])
                {
                   script {
                         echo "Upload EriDoc Documentation"
                         sh "${bob} eridoc-upload-documents:eridoc-upload"
                   }
                }
            }
        }

        stage('Structure Data') {
            when {
                expression { params.RUN_PLMS_RELEASE }
            }
            steps {
                sh "${bob} structure-data"
                archiveArtifacts 'build/structure-output/*.json'
            }
        }

        stage('create git-tag') {
            when {
                expression { params.PUBLISH & !params.RUN_PLMS_RELEASE }
            }
            steps {
              withCredentials([usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                {
                  sh "${bob} create-git-tag"
                }
            }
        }

        stage('Send Release Email') {
            when {
                expression { params.RUN_PLMS_RELEASE}
            }
            steps {
                script {
                    def exists = fileExists 'build/pri/pra_release_email.html'
                    if (exists) {
                        message = readFile('build/pri/pra_release_email.html')
                        VERSION = sh(returnStdout: true, script: 'cat .bob/var.semver').trim()
                        SERVICE_NAME = sh(returnStdout: true, script: 'cat .bob/var.service-name').trim()
                        mail body: message, subject:"$SERVICE_NAME 2.0 - $VERSION, PRA Release", to: 'PDLTEAMATH@pdl.internal.ericsson.com', mimeType: 'text/html'
                    } else {
                        echo 'No release contents found for notifying. Email sending aborted...'
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                if (!params.RUN_PLMS_RELEASE) {
                    bumpVersion()
                    sh "${bob} helm-chart-check-report-warnings"
                    sendHelmDRWarningEmail()
                    modifyBuildDescription()
                }
            }
        }
//        unsuccessful {
//           sendFailNotificationMail()
//        }
        cleanup {
            sh "${bob} delete-images"
        }
    }
}

def modifyBuildDescription() {

    def CHART_NAME = "eric-oss-5gcnr"
    def DOCKER_IMAGE_NAME = "eric-oss-cnr5gassist"

    def VERSION = readFile('.bob/var.version').trim()

    def CHART_DOWNLOAD_LINK = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/${CHART_NAME}/${CHART_NAME}-${VERSION}.tgz"
    def DOCKER_IMAGE_DOWNLOAD_LINK = "https://armdocker.rnd.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-drop/${CHART_NAME}/${VERSION}/"

    currentBuild.description = "Helm Chart: <a href=${CHART_DOWNLOAD_LINK}>${CHART_NAME}-${VERSION}.tgz</a><br>Docker Image: <a href=${DOCKER_IMAGE_DOWNLOAD_LINK}>${DOCKER_IMAGE_NAME}-${VERSION}</a><br>Gerrit: <a href=${env.GERRIT_CHANGE_URL}>${env.GERRIT_CHANGE_URL}</a> <br>"
}

def sendHelmDRWarningEmail() {
    def val = readFile '.bob/var.helm-chart-check-report-warnings'
    if (val.trim().equals("true")) {
        echo "WARNING: One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html"
        manager.addWarningBadge("One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html")
        echo "Sending an email to Helm Design Rule Check distribution list: ${env.HELM_DR_CHECK_DISTRIBUTION_LIST}"
        try {
            mail to: "${env.HELM_DR_CHECK_DISTRIBUTION_LIST}",
            from: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
            cc: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
            subject: "[${env.JOB_NAME}] One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html",
            body: "One or more Helm Design Rules have a WARNING state. <br><br>" +
            "Please review Gerrit and the Helm Design Rule Check Report: design-rule-check-report.html: <br><br>" +
            "&nbsp;&nbsp;<b>Gerrit master branch:</b> https://gerrit.ericsson.se/gitweb?p=${env.GERRIT_PROJECT}.git;a=shortlog;h=refs/heads/master <br>" +
            "&nbsp;&nbsp;<b>Helm Design Rule Check Report:</b> ${env.BUILD_URL}artifact/.bob/design-rule-check-report.html <br><br>" +
            "For more information on the Design Rules and ADP handling process please see: <br>" +
            "&nbsp;&nbsp; - <a href='https://confluence.lmera.ericsson.se/display/AA/Helm+Chart+Design+Rules+and+Guidelines'>Helm Design Rule Guide</a><br>" +
            "&nbsp;&nbsp; - <a href='https://confluence.lmera.ericsson.se/display/ACD/Design+Rule+Checker+-+How+DRs+are+checked'>More Details on Design Rule Checker</a><br>" +
            "&nbsp;&nbsp; - <a href='https://confluence.lmera.ericsson.se/display/AA/General+Helm+Chart+Structure'>General Helm Chart Structure</a><br><br>" +
            "<b>Note:</b> This mail was automatically sent as part of the following Jenkins job: ${env.BUILD_URL}",
            mimeType: 'text/html'
        } catch(Exception e) {
            echo "Email notification was not sent."
            print e
        }
    }
}

def sendFailNotificationMail() {
    echo "WARNING: Publish job execution failed! Please have someone from the team take a look!"
    manager.addWarningBadge("Publish job execution failed !!!")
    echo "Sending an email to Team Aether distribution list: ${env.HELM_DR_CHECK_DISTRIBUTION_LIST}"
    try {
        mail to: "${env.HELM_DR_CHECK_DISTRIBUTION_LIST}",
                from: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
                cc: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
                subject: "[${env.JOB_NAME}] Publish job execution failed !!!",
                body: "Publish job execution failed! <br><br>" +
                        "Please review your latest commit. <br><br>" +
                        "&nbsp;&nbsp;<b>Gerrit master branch:</b> https://gerrit.ericsson.se/gitweb?p=${env.GERRIT_PROJECT}.git;a=shortlog;h=refs/heads/master <br>" +
                        "&nbsp;&nbsp;<b>Failed job:</b> ${env.BUILD_URL} <br><br>" +
                        "<b>Note:</b> This mail was automatically sent as part of the following Jenkins job: ${env.BUILD_URL}",
                mimeType: 'text/html'
    } catch (Exception e) {
        echo "Failed job email notification was not sent."
        print e
    }
}
/*  increase pom & prefix version - patch number
    e.g.  1.0.0 -> 1.0.1/1.1.0/2.0.0
*/
def bumpVersion() {
    // COMMIT_VERSION value is set to patch if it is not specified by commit msg
    env.COMMIT_VERSION = sh(
        script: '''
            if (git log -1 | grep -q "\\[MINOR\\]"); then
                echo "minor";
            elif (git log -1 | grep -q "\\[MAJOR\\]"); then
                echo "major";
            else
                echo "patch";
            fi;
        ''',
        returnStdout: true
    ).trim()
    env.oldPatchVersionPrefix = readFile ".bob/var.version"
    env.VERSION_PREFIX_CURRENT = env.oldPatchVersionPrefix.trim()
    // increase patch number to version_prefix
    sh 'docker run --rm -v $PWD/VERSION_PREFIX:/app/VERSION -w /app --user $(id -u):$(id -g) armdocker.rnd.ericsson.se/proj-eric-oss-drop/utilities/bump ${COMMIT_VERSION}'
    env.versionPrefix = readFile "VERSION_PREFIX"
    env.newPatchVersionPrefix = env.versionPrefix.trim() + "-SNAPSHOT"
    env.VERSION_PREFIX_UPDATED = env.newPatchVersionPrefix.trim()

    echo "Version Prefix has been bumped from ${VERSION_PREFIX_CURRENT} to ${VERSION_PREFIX_UPDATED}"
    if (params.PUBLISH) {
        sh """
            sed -i '0,/${VERSION_PREFIX_CURRENT}/s//${VERSION_PREFIX_UPDATED}/' pom.xml
            echo "pom version has been bumped from ${VERSION_PREFIX_CURRENT} to ${VERSION_PREFIX_UPDATED}"
            git add pom.xml VERSION_PREFIX
            git commit -m "Automatically updating VERSION_PREFIX to ${versionPrefix}"
            git push origin HEAD:master
        """
    }
}

def commitBaseImage(){
    env.cbosVersion = readFile "OS_BASE_VERSION"
    if (params.PUBLISH) {
        sh """
            git add OS_BASE_VERSION
            git commit -m "[NO JIRA] updated OS base image to ${cbosVersion}"
            git push origin HEAD:master
        """
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

        def cmd = """\
            |docker run
            |--init
            |--rm
            |--workdir \${PWD}
            |--user \$(id -u):\$(id -g)
            |-v \${PWD}:\${PWD}
            |-v /etc/group:/etc/group:ro
            |-v /etc/passwd:/etc/passwd:ro
            |-v \${HOME}:\${HOME}
            |-v /proj/mvn/:/proj/mvn
            |${needDockerSocket ? '-v /var/run/docker.sock:/var/run/docker.sock' : ''}
            |${env}
            |\$(for group in \$(id -G); do printf ' --group-add %s' "\$group"; done)
            |--group-add \$(stat -c '%g' /var/run/docker.sock)
            |${bobImage}
            |"""
        return cmd
                .stripMargin()           // remove indentation
                .replace('\n', ' ')      // join lines
                .replaceAll(/[ ]+/, ' ') // replace multiple spaces by one
    }
}