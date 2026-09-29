#!/usr/bin/env bash
# Shared environment; source from the project scripts, no shell-profile edits needed.
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d '/Applications/Android Studio.app/Contents/jbr/Contents/Home' ]]; then
        export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
    elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
        JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
        export JAVA_HOME
    fi
fi
if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -f "$PROJECT_ROOT/local.properties" ]]; then
        ANDROID_HOME="$(sed -n 's/^sdk.dir=//p' "$PROJECT_ROOT/local.properties" | head -n 1)"
    fi
    if [[ -z "${ANDROID_HOME:-}" ]]; then
        if [[ -d "$HOME/Library/Android/sdk" ]]; then
            ANDROID_HOME="$HOME/Library/Android/sdk"
        elif [[ -d /opt/homebrew/share/android-commandlinetools ]]; then
            ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
        else
            ANDROID_HOME="$HOME/Android/Sdk"
        fi
    fi
fi
export ANDROID_HOME
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
AVD_NAME="${AVD_NAME:-NoteTaker_API_36}"
EMULATOR_PORT="${EMULATOR_PORT:-5554}"
export ANDROID_SERIAL="emulator-$EMULATOR_PORT"
