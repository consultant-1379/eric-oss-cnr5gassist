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

############################################################################
# Script to automatically change the common base image to the latest version
############################################################################

echo "Querying for the latest OS base version..."
common_base_version=$(curl -u $1:$2 -X POST https://arm.epk.ericsson.se/artifactory/api/search/aql -H "content-type: text/plain" -d 'items.find({ "repo": {"$eq":"docker-v2-global-local"}, "path": {"$match" : "proj-ldc/common_base_os_release/*"}}).sort({"$desc": ["created"]}).limit(1)' 2>/dev/null | grep path | sed -e 's_.*\/\(.*\)".*_\1_')
[ -z "$common_base_version" ] && echo "could not find OS base version" && exit 1
echo "Found OS base version: $common_base_version"
echo "$common_base_version" > ".bob/var.latest-os-base-version"
exit 0