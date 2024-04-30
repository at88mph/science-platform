#!/bin/sh

# Two variables are expected:
# REGISTRY_URL
#   The URL to the registry to use.
#   Example REGISTRY_URL=https://example.org/reg
#
# POSIX_MAPPER_URI
#   The IVO URI of the POSIX Mapper.  No absolute URLs.
#   Example POSIX_MAPPER_URI=ivo://example.org/posix-mapper
#
# OIDC_CLIENT_ID
#   The OIDC Client ID to authenticate with that supports Client Credentials (client_credentials).
#   Example OIDC_CLIENT_ID=my-oidc-client
#
# OIDC_CLIENT_SECRET
#   The OIDC Client Secret to authenticate with that supports Client Credentials (client_credentials).
#   Example OIDC_CLIENT_SECRET=my-super-secret-uuid
#
# OIDC_URL
#   The URL of the OIDC provider.  The /.well-known/openid-configuration will be appended to check it.
#   Example OIDC_URL=https://myoid.example.org
#

# It is expected that a process will have backed up the original /etc/passwd and /etc/groups files
# to the specified ORIGINAL_ files.

ORIGINAL_PASSWD_FILE="/etc-passwd/passwd-orig"
ORIGINAL_GROUP_FILE="/etc-group/group-orig"

PASSWD_FILE="/etc-passwd/passwd"
GROUP_FILE="/etc-group/group"

function tmp_file_name() {
    echo `hexdump -n 8 -v -e '/1 "%02X"' -e '/8 "\n"' /dev/urandom`
}

LOCAL_POSIX_MAPPER_CAPABILITIES_FILE="/tmp/$(tmp_file_name)-capabilities.xml"

if [[ -z "${POSIX_MAPPER_URI}" ]] ; then
    echo "Required environment variable POSIX_MAPPER_URI is not set."
    exit 1
elif [[ -z "${REGISTRY_URL}" && "${POSIX_MAPPER_URI}" != http* ]] ; then
    echo "Required environment variable REGISTRY_URL is not set if POSIX_MAPPER_URI is not an absolute URL."
    exit 2
elif [[ -z "${OIDC_CLIENT_ID}" || -z "${OIDC_CLIENT_SECRET}" || -z "${OIDC_URL}" ]] ; then
    if [[ -z "${LOGIN_USERNAME}" && -z "${LOGIN_PASSWORD}" ]] ; then
        echo "Required environment variables OIDC_CLIENT_ID, OIDC_CLIENT_SECRET, OIDC_URL for an OIDC Client with Client Credentials (client_credentials) enabled are not set."
        exit 3
    elif [[ -z "${LOGIN_URI}" ]] ; then
        echo "Required environment variables LOGIN_USERNAME, LOGIN_PASSWORD, and LOGIN_URI for legacy CADC-only basic authentication not set when OpenID Connect credentials are missing."
        exit 4
    else
        AC_AUTH=1
    fi
else
    OIDC_AUTH=1
    OIDC_TOKEN_ENDPOINT=`curl -SsL ${OIDC_URL}/.well-known/openid-configuration | awk -F \"token_endpoint\": '{print $2}' | awk -F , '{print $1}' | sed s/\"//g`
    echo "Using Registry at ${REGISTRY_URL}"
    echo "Using OpenID Connect to authenticate."
fi

function uri_to_capabilities_url() {
    URI="${1}"

    echo `curl -SsL ${REGISTRY_URL}/resource-caps | grep -E "${URI}\ *=" | awk -F = '{print $2}' | xargs`
}

function standard_uri_to_access_url() {
    STANDARD_URI="${1}"
    CAPABILITIES_FILE="${2}"
    LINE_NUMBER=`grep -n "${STANDARD_URI}" ${CAPABILITIES_FILE} | cut -f1 -d:`

    echo `tail -n +${LINE_NUMBER} ${CAPABILITIES_FILE} | grep accessURL -m 1 | awk -F \> '{print $2}' | awk -F \< '{print $1}'`
}

UID_STANDARD_URI="http://www.opencadc.org/std/posix#user-mapping-0.1"
GID_STANDARD_URI="http://www.opencadc.org/std/posix#group-mapping-0.1"
LOGIN_STANDARD_URI="ivo://ivoa.net/sso#tls-with-password"

if [[ "${POSIX_MAPPER_URI}" == http* ]] ; then
    # Remove trailing slashes
    TRIMMED_POSIX_MAPPER_URL=`echo "${POSIX_MAPPER_URI}" | tr -s \/ | sed 's/\(.*\)\/$/\1/'`
    POSIX_MAPPER_CAPABILITIES_URL="${TRIMMED_POSIX_MAPPER_URL}/capabilities"
else
    POSIX_MAPPER_CAPABILITIES_URL=$(uri_to_capabilities_url "${POSIX_MAPPER_URI}")
fi

echo "Using POSIX Mapper service at ${POSIX_MAPPER_CAPABILITIES_URL}"

if [[ "${OIDC_AUTH}" == "1" ]] ; then
    echo "Obtaining token from ${OIDC_TOKEN_ENDPOINT}"
    TOKEN=`curl -SsL -d "grant_type=client_credentials" -d "client_id=${OIDC_CLIENT_ID}" -d "client_secret=${OIDC_CLIENT_SECRET}" "${OIDC_TOKEN_ENDPOINT}" | awk -F \"access_token\": '{print $2}' | awk -F , '{print $1}' | sed s/\"//g`
    echo "Successfully obtained token ${TOKEN} with OIDC."
elif [[ "${AC_AUTH}" == "1" ]] ; then
    LOGIN_CAPABILITIES_URL=$(uri_to_capabilities_url "${LOGIN_URI}")
    LOCAL_LOGIN_CAPABILITIES_FILE="/tmp/$(tmp_file_name)-capabilities.xml"
    curl -SsL ${LOGIN_CAPABILITIES_URL} > ${LOCAL_LOGIN_CAPABILITIES_FILE}
    LOGIN_URL=$(standard_uri_to_access_url "${LOGIN_STANDARD_URI}" "${LOCAL_LOGIN_CAPABILITIES_FILE}")
    TOKEN=`curl -SsL -d "username=${LOGIN_USERNAME}" -d "password=${LOGIN_PASSWORD}" "${LOGIN_URL}"`
    echo "Successfully obtained token with AC."
else
    echo "No authentication method is set (missing LOGIN and OIDC)."
    exit 5
fi

# Dump the contents locally.
curl -SsL ${POSIX_MAPPER_CAPABILITIES_URL} > ${LOCAL_POSIX_MAPPER_CAPABILITIES_FILE}
echo "Got the latest capabilities at ${LOCAL_POSIX_MAPPER_CAPABILITIES_FILE}."

UID_URL=$(standard_uri_to_access_url "${UID_STANDARD_URI}" "${LOCAL_POSIX_MAPPER_CAPABILITIES_FILE}")
echo "Will obtain UID information from ${UID_URL}"

GID_URL=$(standard_uri_to_access_url "${GID_STANDARD_URI}" "${LOCAL_POSIX_MAPPER_CAPABILITIES_FILE}")
echo "Will obtain GID information from ${GID_URL}"

if [[ -z "${UID_URL}" || "X${UID_URL}" = "X" ]] ; then
    echo "No URL found for POSIX Mapper UID."
    exit 4
elif [[ -z "${GID_URL}" || "X${GID_URL}" = "X" ]] ; then
    echo "No URL found for POSIX Mapper GID."
    exit 5
else
    cp "${ORIGINAL_PASSWD_FILE}" "${PASSWD_FILE}"
    cp "${ORIGINAL_GROUP_FILE}" "${GROUP_FILE}"

    # Ensure they have home directories set.
    curl -SsL --header "authorization: bearer ${TOKEN}" "${UID_URL}" | sed 's/^\([a-z]*\):\(.*\):::$/\1:\2::\/home\/\1:/' >> "${PASSWD_FILE}"
    curl -SsL --header "authorization: bearer ${TOKEN}" "${GID_URL}" >> "${GROUP_FILE}"
fi
