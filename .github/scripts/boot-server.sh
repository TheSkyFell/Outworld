#!/usr/bin/env bash
# Boots a dedicated server with Outworld Core and Industry on one loader and checks that:
# - it reaches "Done", with both mods started and their content registered
# - Core's server events fire and /outworld answers (asked over RCON)
# - Core's config file was written
# - machines sleep and wake: a test machine placed at 0 100 0 sleeps; a neighbour change starts a
#   100-tick job, which it works through in batches of 4 ticks and then sleeps again
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
# The server must not pause for having no players, or game time (and the machines) would stop.
cat > "$run_dir/server.properties" <<EOF
level-type=minecraft\:flat
online-mode=false
pause-when-empty-seconds=-1
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
placed_machines=""
placed_work=""
job_machines=""
done_work=""
done_machines=""
if [[ "$result" == "done" ]]; then
    version_reply="$(rcon outworld version 2>&1 || true)"

    # Machines: place one, let it settle, then give it a job through a neighbour change.
    rcon forceload add 0 0 > /dev/null 2>&1 || true
    rcon setblock 0 100 0 outworld_industry:test_block > /dev/null 2>&1 || true
    sleep 3
    placed_machines="$(rcon outworld machines 2>&1 || true)"
    placed_work="$(rcon data get block 0 100 0 work_done 2>&1 || true)"
    rcon setblock 1 100 0 minecraft:stone > /dev/null 2>&1 || true
    sleep 1
    job_machines="$(rcon outworld machines 2>&1 || true)"
    for _ in $(seq 1 30); do
        sleep 1
        done_work="$(rcon data get block 0 100 0 work_done 2>&1 || true)"
        [[ "$done_work" == *": 100" ]] && break
    done
    sleep 1
    done_machines="$(rcon outworld machines 2>&1 || true)"

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

batches() { [[ "$1" =~ ([0-9]+)\ work\ batches ]] && echo "${BASH_REMATCH[1]}" || echo -1; }
if [[ "$placed_machines" == *"1 loaded, 0 awake"* && "$placed_work" == *": 0" ]]; then
    pass "placed machine checked for work, then slept (${placed_machines})"
else
    fail "placed machine sleeps (machines: ${placed_machines:-none}; work: ${placed_work:-none})"
fi
if [[ "$job_machines" == *"1 loaded, 1 awake"* ]]; then
    pass "neighbour change woke the machine"
else
    fail "neighbour change wakes the machine (machines: ${job_machines:-none})"
fi
job_batches=$(( $(batches "$done_machines") - $(batches "$placed_machines") ))
if [[ "$done_work" == *": 100" && "$done_machines" == *"1 loaded, 0 awake"* ]] && (( job_batches >= 25 && job_batches <= 26 )); then
    pass "100 ticks of work done in ${job_batches} batches, then asleep"
else
    fail "job of 100 ticks in 25-26 batches, then asleep (work: ${done_work:-none}; machines: ${done_machines:-none}; batches for the job: ${job_batches})"
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
    echo "Machines after placing: ${placed_machines:-none} | work: ${placed_work:-none}"
    echo "Machines during the job: ${job_machines:-none}"
    echo "Machines after the job: ${done_machines:-none} | work: ${done_work:-none}"
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
