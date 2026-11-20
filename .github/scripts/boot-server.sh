#!/usr/bin/env bash
# Boots a dedicated server with Outworld Core and Industry on one loader, and checks that it reaches
# "Done" with both mods started, their content registered and no mixin failures.
#
# Usage: .github/scripts/boot-server.sh fabric|neoforge
set -uo pipefail

loader="${1:?usage: boot-server.sh fabric|neoforge}"
project=":industry-${loader}"
run_dir="industry/${loader}/runs/server"
log="boot-${loader}.log"
# Includes compiling and, on NeoForge, setting up the Minecraft sources on a cold cache.
timeout_seconds=1800

mkdir -p "$run_dir"
echo "eula=true" > "$run_dir/eula.txt"
# A flat world keeps the first boot quick.
cat > "$run_dir/server.properties" <<'EOF'
level-type=minecraft\:flat
online-mode=false
EOF

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

kill -TERM -- "-$gradle_pid" 2>/dev/null || true
sleep 5
kill -KILL -- "-$gradle_pid" 2>/dev/null || true

failures=0
check() {
    local description="$1" pattern="$2"
    if grep -q -E "$pattern" "$log"; then
        echo "  ok:      $description"
    else
        echo "  MISSING: $description"
        failures=$((failures + 1))
    fi
}

echo "Server boot on ${loader}: ${result} after ${elapsed}s"
check "server reached Done" 'Done \([0-9.,]+s\)!'
check "Outworld Core started" 'Outworld Core starting on'
check "Outworld Industry started" 'Outworld Industry starting on'
check "Industry blocks registered" 'Registered 1 entries in minecraft:block for outworld_industry'
check "Industry items registered" 'Registered 1 entries in minecraft:item for outworld_industry'

if grep -q -E 'FAILED during APPLY|was not applied' "$log"; then
    echo "  FAILED:  mixin errors in the log"
    failures=$((failures + 1))
fi

echo
echo "Outworld lines from the log:"
grep -E 'Outworld|outworld' "$log" | cut -c1-300 | head -n 40 || true

if (( failures > 0 )); then
    echo
    echo "Last 80 lines of the log:"
    tail -n 80 "$log" | cut -c1-300
    exit 1
fi
