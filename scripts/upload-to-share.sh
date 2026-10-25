#!/usr/bin/env bash
# Uploads build outputs to the share link in $SHARE_URL.
# Usage: SHARE_URL=https://host/share/<id>/ upload-to-share.sh <file>...
set -uo pipefail

if [ -z "${SHARE_URL:-}" ]; then
  echo "::warning::SHARE_URL is not set; skipping upload"
  exit 0
fi
url="${SHARE_URL%/}/"
work="$(mktemp -d)"

echo "Probing $url"
code=$(curl -sS -L -m 30 -o "$work/page.html" -w '%{http_code}' "$url") || {
  echo "::warning::Share host is unreachable from this runner"
  exit 1
}
echo "GET -> HTTP $code"
# Print the parts of the page that reveal how it accepts uploads (title, forms, API paths).
grep -oiE '<title>[^<]*|<form[^>]*>|<input[^>]*>|(action|href|src|url|endpoint)[=:] *"[^"]*(upload|api|share)[^"]*"|/api/[A-Za-z0-9_/.-]+' "$work/page.html" \
  | sort -u | head -60

status=0
for file in "$@"; do
  [ -f "$file" ] || { echo "::warning::Missing $file"; status=1; continue; }
  echo "Uploading $(basename "$file") ($(du -h "$file" | cut -f1))"
  code=$(curl -sS -L -m 900 -o "$work/response.txt" -w '%{http_code}' -F "file=@$file" "$url") || code="000"
  echo "POST -> HTTP $code"
  head -c 600 "$work/response.txt"; echo
  case "$code" in
    2??) ;;
    *) echo "::warning::Upload of $(basename "$file") failed with HTTP $code"; status=1 ;;
  esac
done
exit $status
