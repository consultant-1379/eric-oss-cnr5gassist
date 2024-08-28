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

FROM armdocker.rnd.ericsson.se/proj-eric-oss-drop/k6-base-image:latest
ADD src/test/js /k6-test
COPY ci/scripts/run_k6_end2end_dev.sh .
ENTRYPOINT ["/bin/sh", "run_k6_end2end_dev.sh"]
