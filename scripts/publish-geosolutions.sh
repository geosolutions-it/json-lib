#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/publish-geosolutions.sh [options]

Options:
  --version <v>          Version to publish. Defaults to content of ./VERSION.
  --key <path>           SSH private key path. Default: ~/.ssh/id_rsa_maven_tmp
  --host <host>          SFTP host. Default: maven.geo-solutions.it
  --user <user>          SFTP user. Default: maven
  --remote-base <path>   Remote artifact base path.
                         Default: /org/kordamp/json/json-lib-core
  --skip-build           Skip Gradle publish-to-local step, upload existing staged files only.
  --dry-run              Print actions and SFTP batch script without uploading.
  --no-sbom              Do not pass -Pprofile=sbom to Gradle.
  --no-reproducible      Do not pass -PreproducibleBuild=true to Gradle.
  -h, --help             Show this help.

Examples:
  scripts/publish-geosolutions.sh
  scripts/publish-geosolutions.sh --version 3.2.0-limit-fix
  scripts/publish-geosolutions.sh --remote-base /net/sf/json-lib/json-lib
  scripts/publish-geosolutions.sh --dry-run
EOF
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_VERSION="$(cat "$ROOT_DIR/VERSION" 2>/dev/null || true)"

VERSION="${DEFAULT_VERSION}"
KEY_PATH="${HOME}/.ssh/id_rsa_maven_tmp"
HOST="maven.geo-solutions.it"
USER_NAME="maven"
REMOTE_BASE="/org/kordamp/json/json-lib-core"
SKIP_BUILD="false"
DRY_RUN="false"
USE_SBOM="true"
USE_REPRODUCIBLE="true"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      VERSION="${2:-}"
      shift 2
      ;;
    --key)
      KEY_PATH="${2:-}"
      shift 2
      ;;
    --host)
      HOST="${2:-}"
      shift 2
      ;;
    --user)
      USER_NAME="${2:-}"
      shift 2
      ;;
    --remote-base)
      REMOTE_BASE="${2:-}"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD="true"
      shift
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --no-sbom)
      USE_SBOM="false"
      shift
      ;;
    --no-reproducible)
      USE_REPRODUCIBLE="false"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${VERSION}" ]]; then
  echo "Version not set. Provide --version or set ${ROOT_DIR}/VERSION." >&2
  exit 1
fi

if [[ "${DRY_RUN}" != "true" && ! -f "${KEY_PATH}" ]]; then
  echo "SSH key not found: ${KEY_PATH}" >&2
  exit 1
fi

STAGING_BASE="${ROOT_DIR}/build/repos/local/release/org/kordamp/json/json-lib-core"
LOCAL_VERSION_DIR="${STAGING_BASE}/${VERSION}"

if [[ "${SKIP_BUILD}" != "true" ]]; then
  echo "Publishing artifacts locally with Gradle for version ${VERSION}"
  echo "${VERSION}" > "${ROOT_DIR}/VERSION"
  export JRELEASER_PROJECT_VERSION="${VERSION}"

  GRADLE_ARGS=()
  if [[ "${USE_SBOM}" == "true" ]]; then
    GRADLE_ARGS+=("-Pprofile=sbom")
  fi
  if [[ "${USE_REPRODUCIBLE}" == "true" ]]; then
    GRADLE_ARGS+=("-PreproducibleBuild=true")
  fi

  (
    cd "${ROOT_DIR}"
    ./gradlew clean "${GRADLE_ARGS[@]}" :json-lib-core:publishMainPublicationToLocalReleaseRepository
  )
fi

if [[ ! -d "${LOCAL_VERSION_DIR}" ]]; then
  echo "Staged version directory not found: ${LOCAL_VERSION_DIR}" >&2
  exit 1
fi

if [[ ! -f "${LOCAL_VERSION_DIR}/json-lib-core-${VERSION}.pom" ]]; then
  echo "Expected POM not found in staged directory: ${LOCAL_VERSION_DIR}" >&2
  exit 1
fi

if [[ ! -f "${STAGING_BASE}/maven-metadata.xml" ]]; then
  echo "Expected metadata file not found: ${STAGING_BASE}/maven-metadata.xml" >&2
  exit 1
fi

SFTP_BATCH="$(mktemp)"
trap 'rm -f "${SFTP_BATCH}"' EXIT

normalize_remote_path() {
  local p="$1"
  # remove trailing slash except for root
  p="${p%/}"
  if [[ -z "${p}" ]]; then
    p="/"
  fi
  printf '%s\n' "${p}"
}

REMOTE_BASE="$(normalize_remote_path "${REMOTE_BASE}")"
REMOTE_VERSION_DIR="${REMOTE_BASE}/${VERSION}"

ensure_remote_directories() {
  local current=""
  local part=""
  local sftp_target="${USER_NAME}@${HOST}"
  IFS='/' read -r -a parts <<< "${REMOTE_VERSION_DIR#/}"
  for part in "${parts[@]}"; do
    [[ -z "${part}" ]] && continue
    current="${current}/${part}"

    # Check if directory exists.
    if sftp -q -b - -i "${KEY_PATH}" "${sftp_target}" >/dev/null 2>&1 <<EOF
ls ${current}
EOF
    then
      continue
    fi

    # Create only missing segment.
    if ! sftp -q -b - -i "${KEY_PATH}" "${sftp_target}" >/dev/null 2>&1 <<EOF
mkdir ${current}
EOF
    then
      echo "Failed to create remote directory with sftp: ${current}" >&2
      echo "Check remote base path and permissions: ${REMOTE_BASE}" >&2
      exit 1
    fi
  done
}

{
  printf 'put %s/* %s/\n' "${LOCAL_VERSION_DIR}" "${REMOTE_VERSION_DIR}"
  printf 'put %s/maven-metadata.xml* %s/\n' "${STAGING_BASE}" "${REMOTE_BASE}"
  printf 'ls %s\n' "${REMOTE_VERSION_DIR}"
  printf 'ls %s\n' "${REMOTE_BASE}"
} > "${SFTP_BATCH}"

echo "Prepared upload:"
echo "  local:  ${LOCAL_VERSION_DIR}"
echo "  remote: ${USER_NAME}@${HOST}:${REMOTE_VERSION_DIR}"
echo

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "[DRY RUN] SFTP batch content:"
  echo "[DRY RUN] Directory creation strategy:"
  echo "for each path segment under ${REMOTE_VERSION_DIR}: sftp ls, then sftp mkdir only if missing"
  echo
  cat "${SFTP_BATCH}"
  exit 0
fi

ensure_remote_directories
sftp -b "${SFTP_BATCH}" -i "${KEY_PATH}" "${USER_NAME}@${HOST}"

echo "Upload complete."
