# Json-lib GeoServer Fork (`3.2.0-limit-fix`)

This branch is based on json-lib `3.2.0` and includes GeoServer compatibility and operational fixes used in previous `2.x` maintenance work.

## What this branch fixes

- Makes JSON nesting depth configurable (instead of hardcoded 20).
- Sets default max depth to `100` (backward-compatible with GeoServer expectations).
- Adds upper bound clamp to avoid OOM from very large `json.maxDepth` values.
- Preserves legacy single-quoted string normalization behavior on programmatic APIs.
- Preserves legacy number coercion behavior in numeric parsing.
- Updates `commons-lang3` dependency to `3.18.0`.

## Runtime Compatibility Flags

These are JVM system properties.

- `json.maxDepth`
  - Default: `100`
  - Behavior: maximum builder nesting depth.
  - Clamp: values above `10000` are clamped to `10000`.
  - Example: `-Djson.maxDepth=250`

- `json.compatibility.stripSingleQuotedStringValues`
  - Default: `true`
  - Behavior: when `true`, object-typed string values surrounded by single quotes are normalized by stripping outer quotes in APIs such as `JSONObject.element(...)`, `JSONObject.accumulate(...)`, and `JSONArray.element(Object, JsonConfig)`.
  - Public constant: `JSONObject.LEGACY_SINGLE_QUOTED_STRING_VALUES_PROPERTY`
  - Example: `-Djson.compatibility.stripSingleQuotedStringValues=false`

- `json.compatibility.legacyNumberCoercion`
  - Default: `true`
  - Behavior: keeps legacy numeric coercion semantics in `JSONTokener#createNumber`.
  - Example: `-Djson.compatibility.legacyNumberCoercion=false`

## Build and Test (CLI)

From repository root:

```bash
./gradlew clean build
```

Run all tests:

```bash
./gradlew :json-lib-core:test
```

Run a single test class:

```bash
./gradlew :json-lib-core:test --tests org.kordamp.json.TestJSONObject
```

Notes:

- Source/target compatibility are Java 8 (`1.8`) in `gradle.properties`.
- Building with newer JDKs (for example JDK 17) is supported by current project setup.
- License checks can be disabled with `-PdisableLicenseChecks=true` (already enabled in this fork's `gradle.properties`).

## Publish to GeoSolutions Maven

An automated script is provided:

`scripts/publish-geosolutions.sh`

It performs:

1. Local publish to staged Maven repo:
   - `:json-lib-core:publishMainPublicationToLocalReleaseRepository`
2. SFTP upload of artifacts + `maven-metadata.xml*` to GeoSolutions.
3. Idempotent remote directory creation using SFTP only (creates missing dirs, skips existing ones).

### Script options

- `--version <v>`: version to publish (default: `VERSION` file).
- `--key <path>`: private key (default: `~/.ssh/id_rsa_maven_tmp`).
- `--host <host>`: SFTP host (default: `maven.geo-solutions.it`).
- `--user <user>`: SFTP user (default: `maven`).
- `--remote-base <path>`: remote artifact base path (default: `/org/kordamp/json/json-lib-core`).
- `--skip-build`: skip Gradle publish, upload staged files only.
- `--dry-run`: print actions without uploading.
- `--no-sbom`: do not pass `-Pprofile=sbom`.
- `--no-reproducible`: do not pass `-PreproducibleBuild=true`.

### Typical usage

Preview:

```bash
scripts/publish-geosolutions.sh --dry-run
```

Build + upload:

```bash
scripts/publish-geosolutions.sh --key ~/.ssh/id_rsa_maven_tmp
```

Upload only (if already staged):

```bash
scripts/publish-geosolutions.sh --skip-build --key ~/.ssh/id_rsa_maven_tmp
```

If your server layout still uses the legacy path:

```bash
scripts/publish-geosolutions.sh \
  --skip-build \
  --key ~/.ssh/id_rsa_maven_tmp \
  --remote-base /net/sf/json-lib/json-lib
```
