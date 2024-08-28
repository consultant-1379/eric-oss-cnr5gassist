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

# Usage fucntion to show how to use the script
function usage() {
  echo "Usage: $0 [-t <token>] [-f <file>] [-a <artifactory>] [-l <local_path>]"
  echo "Download a file from Artifactory"
  echo ""
  echo "Options:"
  echo "-t, --token       The access token to use for Artifactory (required)"
  echo "-f, --file        The name of the file to download (required)"
  echo "-a, --artifactory The base URL of the Artifactory instance (required)"
  echo "-l, --local       The local path to download the file to (required)"
  echo ""
  echo "Example usage:"
  echo "$0 -t **** -f myfile.txt -a https://my.artifactory.com/artifactory -l /path/to/local/file.txt"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--token)
            token=$2
            shift 2
            ;;
        -f|--file)
            file=$2
            shift 2
            ;;
        -a|--artifactory)
            artifactory=$2
            shift 2
            ;;
        -l|--local)
            local_path=$2
            shift 2
            ;;
        *)
            echo "Invalid argument: $1"
            usage
            exit 1
            ;;
    esac
done

# Check if all required arguments are provided
if [[ -z $token || -z $file || -z $artifactory || -z $local_path ]]; then
    echo "Missing required arguments"
    exit 1
fi

# Check if the local path to store the downloaded file exists
if [ ! -d "$(dirname "$local_path")" ]; then
    echo "Local file path $(dirname "$local_path") does not exist"
    exit 1
fi

# Construct the ARM URL for the file
url="${artifactory}/documents/${file}"

if [ ${#token} -eq 64 ]; then echo "Using ARM Reference token"
    token_headers="Authorization: Bearer ${token}"
else
    token_headers="X-JFrog-Art-Api: ${token}"
fi

# Send a request to check if the file exists in ARM and extract the HTTP response code
response=$(curl -G -H "${token_headers}" "${url}" --write-out "%{http_code}" --output /dev/null)

if [ "$response" == "200" ]; then
    curl -o "${local_path}/$file" -H "${token_headers}" "${url}"
    if [ $? -eq 0 ]; then
        echo "File $file downloaded successfully to $local_path"
    else
        echo "Failed to download $file to $local_path"
    fi
elif [ "$response" == "401" ]; then
    echo "Invalid token"
    exit 1
elif [ "$response" == "404" ]; then
    echo "File ${file} does not exist in Artifactory"
    exit 1
else
    echo "Unexpected response code: $response"
    exit 1
fi


