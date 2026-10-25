#!/usr/bin/env bash
# Installs each APK on the running emulator, launches it, and reports whether it stays up,
# any crash stack traces, and the text visible on screen.
set -u
pkg=net.ganjoorak.app
status=0
echo "--- what the APIs return from this runner"
for url in "https://api.ganjoor.net/api/ganjoor/poem/random" "https://echolalia.ir/wp-json/wp/v2/categories?slug=sher"; do
  rm -f /tmp/body
  curl -sS -m 20 -o /tmp/body -D - "$url" | grep -iE "^(HTTP|content-type|server)" || true
  [ -f /tmp/body ] && head -c 200 /tmp/body; echo
done
for apk in androidApp/build/outputs/apk/debug/androidApp-debug.apk androidApp/build/outputs/apk/release/androidApp-release.apk; do
  name=$(basename "$apk")
  echo "::group::$name"
  adb uninstall "$pkg" >/dev/null 2>&1
  if ! adb install -r "$apk"; then
    echo "::error::Could not install $name"
    status=1
    echo "::endgroup::"
    continue
  fi
  adb logcat -c
  adb shell am start -W -n "$pkg/.MainActivity"
  sleep 25
  if adb shell pidof "$pkg" >/dev/null; then
    echo "$name is running"
  else
    echo "::error::$name is not running 25s after launch"
    status=1
  fi
  echo "--- crash buffer"
  adb logcat -d -b crash
  echo "--- app log"
  adb logcat -d | grep -E "Ganjoorak:" | head -20
  echo "--- errors"
  adb logcat -d '*:E' | grep -iE "ganjoorak|AndroidRuntime|FATAL|Exception" | head -100
  echo "--- on screen"
  if adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; then
    adb shell cat /sdcard/ui.xml > /tmp/ui.xml
    grep -oE '(text|content-desc)="[^"]+"' /tmp/ui.xml | head -60
    grep -q "ممکن نشد" /tmp/ui.xml && echo "::warning::$name shows a loading error on the home screen"
  else
    echo "(screen dump unavailable)"
  fi
  echo "::endgroup::"
done
exit $status
