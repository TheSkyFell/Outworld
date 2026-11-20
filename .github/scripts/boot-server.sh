#!/usr/bin/env bash
# Boots a dedicated server with Outworld Core and Industry on one loader and checks that:
# - it reaches "Done", with both mods started and their content registered
# - Core's server events fire and /outworld answers (asked over RCON)
# - Core's config file was written
# - there are no mixin failures
# Then it stops the server over RCON.
#
# Usage: .github/scripts/boot-server.sh fabric|neoforge
set -uo pipefail

loader="${1:?usage: boot-server.sh fabric|neoforge}"
project=":industry-${loader}"
run_dir="industry/${loader}/runs/server"
log="boot-${loader}.log"
rcon_port=25575
rcon_password="outworld-ci"
# Includes compiling and, on NeoForge, setting up the Minecraft sources on a cold cache.
timeout_seconds=1800

mkdir -p "$run_dir"
echo "eula=true" > "$run_dir/eula.txt"
# A flat world keeps the first boot quick. RCON lets this script run commands on the server.
cat > "$run_dir/server.properties" <<EOF
level-type=minecraft\:flat
online-mode=false
enable-rcon=true
rcon.port=${rcon_port}
rcon.password=${rcon_password}
EOF

rcon() {
    python3 .github/scripts/rcon.py 127.0.0.1 "$rcon_port" "$rcon_password" "$@"
}

# setsid puts Gradle, its daemon and the server in their own process group, so they can be stopped together.
setsid ./gradlew "${project}:runServer" --console=plain > "$log" 2>&1 < /dev/null &
gradle_pid=$!

result="timed out"
start=$SECONDS
while (( SECONDS - start < timeout_seconds )); do
    if grep -q -E 'Done \([0-9.,]+s\)!' "$log"; then
        result="done"
        break
    fi
    if ! kill -0 "$gradle_pid" 2>/dev/null; then
        result="exited before the server finished starting"
        break
    fi
    sleep 5
done
elapsed=$(( SECONDS - start ))

version_reply=""
if [[ "$result" == "done" ]]; then
    version_reply="$(rcon outworld version 2>&1 || true)"
    rcon stop > /dev/null 2>&1 || true
    for _ in $(seq 1 30); do
        kill -0 "$gradle_pid" 2>/dev/null || break
        sleep 2
    done
fi
kill -TERM -- "-$gradle_pid" 2>/dev/null || true
sleep 2
kill -KILL -- "-$gradle_pid" 2>/dev/null || true

failures=0
pass() { echo "  ok:      $1"; }
fail() { echo "  MISSING: $1"; failures=$((failures + 1)); }
check_log() {
    if grep -q -E "$2" "$log"; then pass "$1"; else fail "$1"; fi
}

echo "Server boot on ${loader}: ${result} after ${elapsed}s"
check_log "server reached Done" 'Done \([0-9.,]+s\)!'
check_log "Outworld Core started" 'Outworld Core starting on'
check_log "Outworld Industry started" 'Outworld Industry starting on'
check_log "Industry blocks registered" 'Registered 1 entries in minecraft:block for outworld_industry'
check_log "Industry items registered" 'Registered 1 entries in minecraft:item for outworld_industry'
check_log "Industry menus registered" 'Registered 1 entries in minecraft:menu for outworld_industry'
check_log "server-started event reached Core" 'Outworld is ready: .*outworld_industry'

if [[ "$version_reply" == *"outworld_industry"* ]]; then
    pass "/outworld version answered: ${version_reply}"
else
    fail "/outworld version over RCON (reply: ${version_reply:-none})"
fi

config="$run_dir/config/outworld/core.json"
if grep -q '"workIntervalTicks"' "$config" 2>/dev/null; then
    pass "config file written: $config"
else
    fail "config file $config"
fi

if grep -q -E 'FAILED during APPLY|was not applied' "$log"; then
    echo "  FAILED:  mixin errors in the log"
    failures=$((failures + 1))
fi

# Recorded in the log too, so failure reports on pull requests include them.
{
    echo "Boot checks: ${failures} failed. /outworld version reply: ${version_reply:-none}"
} >> "$log"

echo
echo "Outworld lines from the log:"
grep -E 'Outworld|outworld' "$log" | cut -c1-300 | head -n 40 || true

if (( failures > 0 )); then
    echo
    echo "Last 80 lines of the log:"
    tail -n 80 "$log" | cut -c1-300
    exit 1
fi
