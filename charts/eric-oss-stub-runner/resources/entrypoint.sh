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


repo="https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-dev-local"
ids="com.ericsson.oss.apps.stubs:eric-oss-cnr5gassist:+:stubs"
tmp_dir="./extracted/"
dest="./mappings/"

rm -rf "${tmp_dir}" "${dest}"
rm -f "${jar_name}"

while getopts r:i:d: flag
do
  case "${flag}" in
    r) repo=${OPTARG};;
    i) ids=${OPTARG};;
    d) dest=${OPTARG};;
    *) usage
        exit 1;;
  esac
done

IFS=':' read -r -a array <<< "${ids}"

group="${array[0]//.//}"
artifact="${array[1]}"

if [[ "${array[2]}" =~ ^([[:digit:]]+)\.\+ ]]; then
  version="((${BASH_REMATCH[1]})\.([0-9]+)\.([0-9]+)(-.*)?\/)"
elif [[ "${array[2]}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.\+ ]]; then
  version="((${BASH_REMATCH[1]})\.(${BASH_REMATCH[2]})\.([0-9]+)(-.*)?\/)"
elif [[ "${array[2]}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)(-.*)? ]]; then
  version="((${BASH_REMATCH[1]})\.(${BASH_REMATCH[2]})\.(${BASH_REMATCH[3]})(-.*)?\/)"
else
  version="(([0-9]+)\.([0-9]+)\.([0-9]+)(-.*)?\/)"
fi

echo "Artifact: ${artifact}"
echo "Group: ${group}"
echo "Version: ${version}"

latest_version=$(curl -u "$REPO_USERNAME:$REPO_PASSWORD" "${repo}/${group}/${artifact}/" |
  sed -rn "s/<a href=\"${version}\">\1<\/a>/\2 \3 \4 \1/p" |
  sort -k1n -k2n -k3n |
  tail -1 |
  cut -d' ' -f 4 |
  rev | cut -c 2- | rev)

echo "Latest available version: ${latest_version}"

if (( ${#array[@]} == 4 )); then
  jar_name="${artifact}-${latest_version}-${array[3]}.jar"
else
  jar_name="${artifact}-${latest_version}.jar"
fi

echo "Jar Name: ${jar_name}"

wget -q --user "$REPO_USERNAME" --password "$REPO_PASSWORD" -N "${repo}/${group}/${artifact}/${latest_version}/${jar_name}"
unzip -d "${tmp_dir}" "${jar_name}"
rm "${jar_name}"

mkdir -p "${dest}"
find "${tmp_dir}" -path "*/mappings/*.json" -type f -exec cp -r {} "${dest}" \;
rm -rf "${tmp_dir}"
