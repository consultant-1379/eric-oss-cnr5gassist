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

###########################################################################
# Script to run 5GCNR rApp k6 end2end test on the precodereview environment
###########################################################################

echo "###########################################################"
echo "Executing k6 tests..."
echo

COMMAND="k6 run --insecure-skip-tls-verify --quiet --http-debug=full --verbose /k6-test/k6.main.js"

echo "Command being run: $COMMAND"
$COMMAND

exit_status=$?
echo "k6 execution result code: $exit_status (non-zero means a problem occurred, otherwise successful execution)"
echo "###########################################################"
echo "Process complete"
sleep 30
[ $exit_status -ne 0 ] && { exit 1; }
echo "SUCCESS"
exit 0

