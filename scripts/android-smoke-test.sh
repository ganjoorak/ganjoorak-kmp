#!/usr/bin/env bash
# Installs each APK on the running emulator, launches it, and reports whether it stays up,
# any crash stack traces, and the text visible on screen.
set -u
pkg=net.ganjoorak.app
status=0
echo "--- what the APIs return from this runner"
for url in "https://api.ganjoor.net/api/ganjoor/poem/random" "https://echolalia.ir/wp-json/wp/v2/categories?slug=sher"; do
  curl -sS -m 20 -o /tmp/body -D - "$url" | grep -iE "^(HTTP|content-type|server)" || true
  head -c 200 /tmp/body; echo
done
echo "--- emulator network"
for i in $(seq 1 30); do
  adb shell ping -c 1 -W 2 8.8.8.8 >/dev/null 2>&1 && { echo "network up after ${i} tries"; break; }
  sleep 2
done
adb shell ping -c 1 -W 5 api.ganjoor.net || echo "api.ganjoor.net unreachable from the emulator"
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
    adb shell cat /sdcard/ui.xml | grep -oE '(text|content-desc)="[^"]+"' | head -60
  else
    echo "(screen dump unavailable)"
  fi
  echo "::endgroup::"
done
exit $status
