#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <version> [aar_path]"
  exit 1
fi

VERSION="$1"
AAR_PATH="${2:-releases/bharatmaps-android-${VERSION}.aar}"

if [[ ! -f "$AAR_PATH" ]]; then
  echo "AAR not found: $AAR_PATH"
  exit 1
fi

GROUP_ID="com.bharatmaps"
ARTIFACT_ID="bharatmaps-android"
GROUP_PATH="${GROUP_ID//./\/}"
TARGET_DIR="com/${GROUP_PATH#com/}/${ARTIFACT_ID}/${VERSION}"
mkdir -p "$TARGET_DIR"

cp "$AAR_PATH" "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.aar"

cat > "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.pom" << POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>${GROUP_ID}</groupId>
  <artifactId>${ARTIFACT_ID}</artifactId>
  <version>${VERSION}</version>
  <packaging>aar</packaging>
  <name>BharatMaps Android SDK</name>
  <description>BharatMaps Android SDK binary artifact</description>
  <url>https://github.com/bharatmap/android-sdk</url>
  <licenses>
    <license>
      <name>Proprietary</name>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <dependencies>
    <dependency><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId><version>2.10.1</version></dependency>
    <dependency><groupId>androidx.annotation</groupId><artifactId>annotation</artifactId><version>1.7.1</version></dependency>
    <dependency><groupId>androidx.legacy</groupId><artifactId>legacy-support-v4</artifactId><version>1.0.0</version></dependency>
    <dependency><groupId>com.squareup.okhttp3</groupId><artifactId>okhttp</artifactId><version>4.12.0</version></dependency>
    <dependency><groupId>com.jakewharton.timber</groupId><artifactId>timber</artifactId><version>5.0.1</version></dependency>
    <dependency><groupId>androidx.interpolator</groupId><artifactId>interpolator</artifactId><version>1.0.0</version></dependency>
    <dependency><groupId>androidx.appcompat</groupId><artifactId>appcompat</artifactId><version>1.7.0</version></dependency>
    <dependency><groupId>androidx.core</groupId><artifactId>core-ktx</artifactId><version>1.13.1</version></dependency>
    <dependency><groupId>com.google.android.material</groupId><artifactId>material</artifactId><version>1.12.0</version></dependency>
    <dependency><groupId>androidx.recyclerview</groupId><artifactId>recyclerview</artifactId><version>1.3.2</version></dependency>
    <dependency><groupId>androidx.constraintlayout</groupId><artifactId>constraintlayout</artifactId><version>2.1.4</version></dependency>
    <dependency><groupId>androidx.cardview</groupId><artifactId>cardview</artifactId><version>1.0.0</version></dependency>
    <dependency><groupId>com.squareup.picasso</groupId><artifactId>picasso</artifactId><version>2.71828</version></dependency>
    <dependency><groupId>com.squareup.retrofit2</groupId><artifactId>retrofit</artifactId><version>2.9.0</version></dependency>
  </dependencies>
</project>
POM

META_PATH="com/${GROUP_PATH#com/}/${ARTIFACT_ID}/maven-metadata.xml"
if [[ -f "$META_PATH" ]]; then
  mapfile -t existing < <(grep -oE '<version>[^<]+' "$META_PATH" | sed 's/<version>//' | sort -V | uniq)
else
  existing=()
fi

has_version=false
for v in "${existing[@]:-}"; do
  if [[ "$v" == "$VERSION" ]]; then
    has_version=true
    break
  fi
done
if [[ "$has_version" == false ]]; then
  existing+=("$VERSION")
fi
IFS=$'\n' existing=($(printf "%s\n" "${existing[@]}" | sort -V | awk 'NF'))
unset IFS
latest="${existing[-1]}"
last_updated="$(date -u +%Y%m%d%H%M%S)"

{
  echo '<?xml version="1.0" encoding="UTF-8"?>'
  echo '<metadata>'
  echo "  <groupId>${GROUP_ID}</groupId>"
  echo "  <artifactId>${ARTIFACT_ID}</artifactId>"
  echo '  <versioning>'
  echo "    <latest>${latest}</latest>"
  echo "    <release>${latest}</release>"
  echo '    <versions>'
  for v in "${existing[@]}"; do
    echo "      <version>${v}</version>"
  done
  echo '    </versions>'
  echo "    <lastUpdated>${last_updated}</lastUpdated>"
  echo '  </versioning>'
  echo '</metadata>'
} > "$META_PATH"

while IFS= read -r -d '' file; do
  [[ "$file" == *.md5 || "$file" == *.sha1 || "$file" == *.sha256 || "$file" == *.sha512 ]] && continue
  md5 -q "$file" > "${file}.md5"
  shasum -a 1 "$file" | awk '{print $1}' > "${file}.sha1"
  shasum -a 256 "$file" | awk '{print $1}' > "${file}.sha256"
  shasum -a 512 "$file" | awk '{print $1}' > "${file}.sha512"
done < <(find "com/${GROUP_PATH#com/}/${ARTIFACT_ID}" -type f -print0)

cat > index.html << 'HTML'
<!doctype html><html><head><meta charset="utf-8"><title>BharatMaps Android Maven</title></head><body><h1>BharatMaps Android Maven Repository</h1></body></html>
HTML

echo "Published ${GROUP_ID}:${ARTIFACT_ID}:${VERSION}"
