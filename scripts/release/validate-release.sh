#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION_FILE="${ROOT_DIR}/version.properties"
TAG="${1:-${GITHUB_REF_NAME:-}}"

fail() {
    printf 'release validation failed: %s\n' "$1" >&2
    exit 1
}

[[ -n "$TAG" ]] || fail 'a version tag is required (pass vX.Y.Z or set GITHUB_REF_NAME)'
[[ "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "tag must use vX.Y.Z format: ${TAG}"
[[ -f "$VERSION_FILE" ]] || fail "missing ${VERSION_FILE}"

parse_version_file() {
    local file="$1"
    local version_name=''
    local version_code=''
    local line key value

    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        [[ "$line" =~ ^[[:space:]]*$ ]] && continue
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        [[ "$line" =~ ^(VERSION_NAME|VERSION_CODE)=([^=[:space:]]+)$ ]] || \
            fail "invalid line in ${file}"

        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        case "$key" in
            VERSION_NAME)
                [[ -z "$version_name" ]] || fail "duplicate VERSION_NAME in ${file}"
                version_name="$value"
                ;;
            VERSION_CODE)
                [[ -z "$version_code" ]] || fail "duplicate VERSION_CODE in ${file}"
                version_code="$value"
                ;;
        esac
    done < "$file"

    [[ "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || \
        fail "VERSION_NAME must use X.Y.Z format in ${file}"
    [[ "$version_code" =~ ^[1-9][0-9]*$ ]] || \
        fail "VERSION_CODE must be a positive integer in ${file}"

    VERSION_NAME_VALUE="$version_name"
    VERSION_CODE_VALUE="$version_code"
}

is_greater_integer() {
    local left="$1"
    local right="$2"
    local left_length="${#left}"
    local right_length="${#right}"

    if (( left_length != right_length )); then
        (( left_length > right_length ))
    else
        [[ "$left" > "$right" ]]
    fi
}

parse_version_file "$VERSION_FILE"
current_version_name="$VERSION_NAME_VALUE"
current_version_code="$VERSION_CODE_VALUE"
tag_version="${TAG#v}"
[[ "$tag_version" == "$current_version_name" ]] || \
    fail "tag ${TAG} does not match VERSION_NAME=${current_version_name}"

current_commit="$(git -C "$ROOT_DIR" rev-parse --verify HEAD^{commit})" || \
    fail 'cannot resolve HEAD'
tag_commit="$(git -C "$ROOT_DIR" rev-list -n 1 "$TAG" 2>/dev/null)" || \
    fail "cannot resolve tag ${TAG}"
[[ "$tag_commit" == "$current_commit" ]] || \
    fail "tag ${TAG} does not point to HEAD"

main_commit="$(git -C "$ROOT_DIR" rev-parse --verify refs/remotes/origin/main^{commit} 2>/dev/null)" || \
    fail 'origin/main is unavailable; fetch the remote main branch before validation'
[[ "$current_commit" == "$main_commit" ]] || \
    fail 'HEAD must equal origin/main before a release tag is published'

mapfile -t semver_tags < <(
    git -C "$ROOT_DIR" tag --list 'v*' |
        awk '/^v[0-9]+\.[0-9]+\.[0-9]+$/' |
        sort -V
)

highest_other_tag=''
for candidate in "${semver_tags[@]}"; do
    [[ "$candidate" == "$TAG" ]] && continue
    highest_other_tag="$candidate"
done

if [[ -n "$highest_other_tag" ]]; then
    highest_tag="$(printf '%s\n%s\n' "$TAG" "$highest_other_tag" | sort -V | tail -n 1)"
    [[ "$highest_tag" == "$TAG" ]] || \
        fail "tag ${TAG} must be greater than existing semver tag ${highest_other_tag}"
fi

previous_file=''
if ((${#semver_tags[@]} > 0)); then
    previous_file="$(mktemp)"
    trap 'rm -f "$previous_file"' EXIT
fi

for candidate in "${semver_tags[@]}"; do
    [[ "$candidate" == "$TAG" ]] && continue
    git -C "$ROOT_DIR" show "${candidate}:version.properties" > "$previous_file" 2>/dev/null || \
        fail "${candidate} does not contain version.properties"
    parse_version_file "$previous_file"
    previous_version_code="$VERSION_CODE_VALUE"
    is_greater_integer "$current_version_code" "$previous_version_code" || \
        fail "VERSION_CODE=${current_version_code} must be greater than ${candidate} VERSION_CODE=${previous_version_code}"
done

printf 'validated tag=%s versionName=%s versionCode=%s commit=%s' \
    "$TAG" "$current_version_name" "$current_version_code" "$current_commit"
[[ -n "$highest_other_tag" ]] && printf ' previousTag=%s' "$highest_other_tag"
printf '\n'

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
        printf 'tag=%s\n' "$TAG"
        printf 'version_name=%s\n' "$current_version_name"
        printf 'version_code=%s\n' "$current_version_code"
        printf 'commit=%s\n' "$current_commit"
        printf 'previous_tag=%s\n' "$highest_other_tag"
    } >> "$GITHUB_OUTPUT"
fi
