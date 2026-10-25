#!/usr/bin/env bash
# Uploads build outputs to a copyparty share with HTTP PUT (curl -T).
# Usage: SHARE_URL=https://host/share/<id>/ upload-to-share.sh <file>...
# Each file is stored as ganjoorak-<short sha>-<original name>.
set -uo pipefail

if [ -z "${SHARE_URL:-}" ]; then
  echo "::warning::SHARE_URL is not set; skipping upload"
  exit 0
fi
url="${SHARE_URL%/}/"
sha="${GITHUB_SHA:-local}"
prefix="ganjoorak-${sha:0:7}"

status=0
for file in "$@"; do
  [ -f "$file" ] || { echo "::warning::Missing $file"; status=1; continue; }
  name="$prefix-$(basename "$file")"
  echo "Uploading $name ($(du -h "$file" | cut -f1))"
  code=$(curl -sS -m 900 -o response.txt -w '%{http_code}' -T "$file" "$url$name") || code="000"
  echo "PUT -> HTTP $code"
  head -c 400 response.txt; echo
  case "$code" in
    2??) ;;
    *) echo "::warning::Upload of $name failed with HTTP $code"; status=1 ;;
  esac
done
rm -f response.txt
exit $status
