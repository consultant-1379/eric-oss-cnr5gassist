#!/bin/bash
#
# COPYRIGHT Ericsson 2021
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

sleep 30

KUBECONFIG=$1
NAMESPACE=$2
REPORT_PATH=$3
job="eric-oss-5gcnr-test-k6.main";
rm -rf test
mkdir test

retries="12";
pod=$(kubectl --kubeconfig ${KUBECONFIG} get pod -n ${NAMESPACE} -l job-name=${job} --template '{{range .items}}{{.metadata.name}}{{end}}')
echo "pod: ${pod}"

while [ $retries -ge 0 ]
do
    kubectl --kubeconfig ${KUBECONFIG} cp ${NAMESPACE}/${pod}:/tmp/k6-test-results.html ${REPORT_PATH}/test/k6-test-results.html
    kubectl --kubeconfig ${KUBECONFIG} cp ${NAMESPACE}/${pod}:/tmp/summary.json ${REPORT_PATH}/test/summary.json
    if [[ -f ${REPORT_PATH}/test/k6-test-results.html && -f ${REPORT_PATH}/test/summary.json ]] ;
    then
        echo report copied
        kubectl --namespace ${NAMESPACE} --kubeconfig ${KUBECONFIG} logs ${pod} > ${REPORT_PATH}/${job}.log
        cat ${REPORT_PATH}/${job}.log
        break
    elif [[ "$retries" -eq "0" ]]
    then
        echo no report file available
        kubectl --namespace ${NAMESPACE} --kubeconfig ${KUBECONFIG} logs ${pod} > ${REPORT_PATH}/${job}.log
        cat ${REPORT_PATH}/${job}.log
        exit 1
    else
        let "retries-=1"
        echo report not available, Retries left = $retries :: Sleeping for 25 seconds
        sleep 25
    fi
done

