#!/usr/bin/env bash
# Wrap Compose Desktop's jpackage "app-image" directory into a real Linux .AppImage.
# Prerequisite: ./gradlew :composeApp:packageAppImage
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BINARIES_DIR="${BINARIES_DIR:-$ROOT/composeApp/build/compose/binaries/main}"
APP_ROOT="${BINARIES_DIR}/app"
OUT_DIR="${BINARIES_DIR}/appimage"
VERSION="${APP_VERSION:-1.0.0}"
ARCH="$(uname -m)"
case "$ARCH" in
  x86_64|amd64) ARCH=x86_64 ;;
  aarch64|arm64) ARCH=aarch64 ;;
esac

if [[ ! -d "$APP_ROOT" ]]; then
  echo "error: missing jpackage app-image at $APP_ROOT — run ./gradlew :composeApp:packageAppImage first" >&2
  exit 1
fi

mapfile -t apps < <(find "$APP_ROOT" -mindepth 1 -maxdepth 1 -type d ! -name '.DS_Store' | sort)
if [[ ${#apps[@]} -ne 1 ]]; then
  echo "error: expected exactly one app directory under $APP_ROOT, found ${#apps[@]}" >&2
  exit 1
fi
app_dir="${apps[0]}"
app_name="$(basename "$app_dir")"
slug="$(printf '%s' "$app_name" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')"

launcher="$app_dir/bin/$app_name"
if [[ ! -x "$launcher" ]]; then
  # linux packageName may differ from folder name; try ganjoorak / Ganjoorak
  if [[ -x "$app_dir/bin/ganjoorak" ]]; then
    launcher="$app_dir/bin/ganjoorak"
  elif [[ -x "$app_dir/bin/Ganjoorak" ]]; then
    launcher="$app_dir/bin/Ganjoorak"
  else
    echo "error: launcher not found under $app_dir/bin" >&2
    ls -la "$app_dir/bin" >&2 || true
    exit 1
  fi
fi

icon=""
for candidate in \
  "$app_dir/lib/${app_name}.png" \
  "$app_dir/lib/ganjoorak.png" \
  "$app_dir/lib/Ganjoorak.png" \
  "$ROOT/composeApp/desktop-icons/icon.png"
do
  if [[ -f "$candidate" ]]; then icon="$candidate"; break; fi
done
if [[ -z "$icon" ]]; then
  echo "error: no icon found for AppImage" >&2
  exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
appdir="$OUT_DIR/${slug}.AppDir"
rm -rf "$appdir"
mkdir -p "$appdir"
cp -a "$app_dir"/. "$appdir/"

cat > "$appdir/AppRun" <<'APPRUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/bin/$(basename "$(ls -1 "$HERE/bin" | head -n1)")" "$@"
APPRUN
# Prefer the known launcher name when present
if [[ -x "$appdir/bin/$(basename "$launcher")" ]]; then
  cat > "$appdir/AppRun" <<APPRUN
#!/bin/sh
HERE="\$(dirname "\$(readlink -f "\$0")")"
exec "\$HERE/bin/$(basename "$launcher")" "\$@"
APPRUN
fi
chmod +x "$appdir/AppRun"

cp "$icon" "$appdir/${slug}.png"
cat > "$appdir/${slug}.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=${app_name}
Exec=AppRun
Icon=${slug}
Comment=Persian poetry reader for Ganjoor
Categories=Education;
Terminal=false
DESKTOP

tools="$(mktemp -d)"
trap 'rm -rf "$tools"' EXIT
curl -fsSL --retry 5 --retry-delay 2 --connect-timeout 10 --max-time 300 \
  -o "$tools/appimagetool" \
  "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH}.AppImage"
chmod +x "$tools/appimagetool"

output="$OUT_DIR/${slug}-${VERSION}-${ARCH}.AppImage"
# APPIMAGE_EXTRACT_AND_RUN avoids needing FUSE on CI runners
ARCH="$ARCH" APPIMAGE_EXTRACT_AND_RUN=1 "$tools/appimagetool" --no-appstream "$appdir" "$output"
rm -rf "$appdir"

echo "Built AppImage: $output ($(du -h "$output" | cut -f1))"
