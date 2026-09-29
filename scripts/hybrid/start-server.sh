#!/usr/bin/env bash
# Foreground emulator-only process. Stop with Ctrl-C; the actual server may need
# `adb -s emulator-5554 shell kill -TERM <pid>` if the adb client disconnects.
set -euo pipefail
source "$(dirname "$0")/../android-env.sh"
actual_avd="$(adb -s "$ANDROID_SERIAL" emu avd name | head -n 1 | tr -d '\r')"
[[ "$actual_avd" == "$AVD_NAME" ]] || { echo 'Expected dedicated emulator.' >&2; exit 1; }
adb -s "$ANDROID_SERIAL" forward tcp:18089 tcp:18089
exec adb -s "$ANDROID_SERIAL" shell /data/local/tmp/notetaker-hybrid/llama-server-android \
    -m /data/local/tmp/notetaker-hybrid/Qwen3-0.6B-Q8_0.gguf \
    --host 127.0.0.1 --port 18089 -c 2048 -np 1 -t 4 -tb 4 \
    -b 128 -ub 128 --cache-ram 0 --no-context-shift --jinja --reasoning off --no-webui
