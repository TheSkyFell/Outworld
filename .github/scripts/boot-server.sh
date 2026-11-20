#!/usr/bin/env bash
# Boots a dedicated server with Outworld Core and Industry on one loader and checks that:
# - it reaches "Done", with both mods started and their content registered
# - Core's server events fire and /outworld answers (asked over RCON)
# - Core's config file was written
# - machines sleep and wake: a test machine placed at 0 100 0 sleeps; a neighbour change starts a
#   100-tick job, but with no energy it sleeps again
# - energy moves through the loaders' energy APIs: placing a test battery (10,000 FE) next to it wakes
#   it; it pulls 1,000 FE through the loader lookup and does exactly 100 ticks of work in batches of
#   4 ticks, then sleeps; the battery is left with 9,000 FE
# - items move through the loaders' item APIs: in a column at 0 _ 4, a chest of 8 iron ingots sits on
#   a test crate, over a hopper, over a second crate. The crates pull from above and push below from
#   common code; the hopper reaches the crates through the loader. All 8 ingots end in the bottom crate.
# - fluid moves through the loaders' fluid APIs: in a column at 0 _ 8, a full water cauldron sits
#   under two test tanks, which pull from below and push above. The bucket of water ends in the top
#   tank, 81,000 droplets, and the cauldron is left empty.
# - afterwards all five machines are asleep
# - /outworld perf reports the server tick, the machines section and the test machine's batches,
#   asked while the test machine is working
# - multiblocks form from data: a test controller facing east at 4 100 12, on the front of a 3x3x3
#   iron shell with an air core (the test_frame structure, turned a quarter), forms; it unforms when
#   a shell block is removed or the core is filled, and forms again when they're put back, three
#   times in all; turning the controller north unforms it and turning it back east forms it again;
#   27 positions are watched for changes
# - gas moves through Core's gas storage: a test gas tank holding 4 buckets of hydrogen at 0 100 10,
#   with a second one on top, ends with all 4,000 mB in the top tank
# - the crusher runs recipes from data: on a creative battery at 8 100 2, a crusher given 3 raw iron
#   turns it into 6 crushed iron; no Outworld data file fails to load
# - energy flows through cables: a combustion generator at 10 100 6 burning one coal (from its data
#   recipe) feeds three cables at 11..13 100 6, which a crusher at 14 100 6 draws on to turn 2 raw iron
#   into 4 crushed iron
# - a solar panel at 10 100 9, set to noon, charges an empty test battery next to it
# - the electric furnace smelts with vanilla recipes: on a creative battery at 6 100 14, given 2 crushed
#   iron, it makes 2 iron ingots
# - the ore washer takes water through the loaders' fluid APIs: over a test tank and a water cauldron
#   at 8 _ 14, it washes 2 crushed copper into 3 purified copper
# - the electrolyzer splits water into gases: over a test tank and a water cauldron at 10 _ 14, it
#   fills the test gas tank on top of it with hydrogen or oxygen. Both machines share a creative
#   battery at 9 101 14
# - pipes and tanks: water pumped up from a cauldron at 12 _ 12 into two fluid pipes reaches an ore
#   washer at their end, which washes 2 crushed gold into 3 purified gold; an electrolyzer at 12 101 15
#   fills a gas tank block through a gas pipe; the cable from the power test charges an empty battery
#   under it; the grids' contents are saved with the world when the server stops
# - the stress test runs: 500 test machines working nonstop, each on a creative battery, built at
#   64 100 64. Under load Outworld must stay within its 5 ms per tick budget with all 500 machines
#   awake; once quieted, every machine sleeps and Outworld costs (next to) nothing. The report is kept
#   as an artifact and summarised on the run's page.
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

# Runs a command every half second until its reply ends with the first argument (for up to 10
# seconds), then prints the last reply.
wait_for() {
    local expected="$1"
    shift
    local reply=""
    for _ in $(seq 1 20); do
        reply="$(rcon "$@" 2>&1 || true)"
        [[ "$reply" == *"$expected" ]] && break
        sleep 0.5
    done
    echo "$reply"
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
starved_machines=""
starved_work=""
job_machines=""
done_work=""
done_machines=""
battery_energy=""
perf_reply=""
mb_formed=""
mb_broken=""
mb_restored=""
mb_blocked=""
mb_cleared=""
mb_times=""
mb_turned=""
mb_turned_back=""
mb_machines=""
gas_top=""
crusher_output=""
crusher_state=""
power_output=""
power_state=""
solar_battery=""
furnace_output=""
washer_output=""
electrolyzer_gas=""
processing_state=""
pipe_washer_output=""
gas_grid=""
cable_battery=""
grids_state=""
gas_bottom=""
stress_start=""
stress_status=""
crate_count=""
crate_item=""
tank_bottom=""
tank_top=""
cauldron_empty=""
final_machines=""
if [[ "$result" == "done" ]]; then
    version_reply="$(rcon outworld version 2>&1 || true)"

    # Machines: place one and let it settle. A neighbour change gives it a job, but it has no energy.
    # Then a battery next to it wakes it, and it pulls the energy it needs.
    rcon forceload add 0 0 > /dev/null 2>&1 || true
    rcon setblock 0 100 0 outworld_industry:test_block > /dev/null 2>&1 || true
    sleep 3
    placed_machines="$(rcon outworld machines 2>&1 || true)"
    placed_work="$(rcon data get block 0 100 0 work_done 2>&1 || true)"
    rcon setblock 1 100 0 minecraft:stone > /dev/null 2>&1 || true
    sleep 2
    starved_machines="$(rcon outworld machines 2>&1 || true)"
    starved_work="$(rcon data get block 0 100 0 work_done 2>&1 || true)"
    rcon setblock -1 100 0 outworld_industry:test_battery > /dev/null 2>&1 || true
    sleep 1
    job_machines="$(rcon outworld machines 2>&1 || true)"
    perf_reply="$(rcon outworld perf 2>&1 || true)"
    for _ in $(seq 1 30); do
        sleep 1
        done_work="$(rcon data get block 0 100 0 work_done 2>&1 || true)"
        [[ "$done_work" == *": 100" ]] && break
    done
    sleep 1
    done_machines="$(rcon outworld machines 2>&1 || true)"
    battery_energy="$(rcon data get block -1 100 0 energy 2>&1 || true)"

    # Items: built bottom-up, so placing the full chest last wakes the top crate.
    rcon setblock 0 100 4 outworld_industry:test_crate > /dev/null 2>&1 || true
    rcon setblock 0 101 4 minecraft:hopper > /dev/null 2>&1 || true
    rcon setblock 0 102 4 outworld_industry:test_crate > /dev/null 2>&1 || true
    rcon setblock 0 103 4 'minecraft:chest{Items:[{Slot:0b,id:"minecraft:iron_ingot",count:8}]}' > /dev/null 2>&1 || true
    # Fluid: a full water cauldron under two tanks.
    rcon setblock 0 99 8 'minecraft:water_cauldron[level=3]' > /dev/null 2>&1 || true
    rcon setblock 0 100 8 outworld_industry:test_tank > /dev/null 2>&1 || true
    rcon setblock 0 101 8 outworld_industry:test_tank > /dev/null 2>&1 || true
    for _ in $(seq 1 30); do
        sleep 1
        crate_count="$(rcon data get block 0 100 4 Items[0].count 2>&1 || true)"
        tank_top="$(rcon data get block 0 101 8 tank.amount 2>&1 || true)"
        [[ "$crate_count" == *": 8" && "$tank_top" =~ :\ 81000L?$ ]] && break
    done
    sleep 1
    crate_item="$(rcon data get block 0 100 4 Items[0].id 2>&1 || true)"
    tank_bottom="$(rcon data get block 0 100 8 tank.amount 2>&1 || true)"
    cauldron_empty="$(rcon execute if block 0 99 8 minecraft:cauldron 2>&1 || true)"
    final_machines="$(rcon outworld machines 2>&1 || true)"

    # Multiblock: build the shell, hollow it, then put the controller on its east face.
    rcon fill 2 99 11 4 101 13 minecraft:iron_block > /dev/null 2>&1 || true
    rcon setblock 3 100 12 minecraft:air > /dev/null 2>&1 || true
    rcon setblock 4 100 12 'outworld_industry:test_multiblock[facing=east]' > /dev/null 2>&1 || true
    mb_formed="$(wait_for ': 1b' data get block 4 100 12 formed)"
    rcon setblock 2 101 13 minecraft:air > /dev/null 2>&1 || true
    mb_broken="$(wait_for ': 0b' data get block 4 100 12 formed)"
    rcon setblock 2 101 13 minecraft:iron_block > /dev/null 2>&1 || true
    mb_restored="$(wait_for ': 1b' data get block 4 100 12 formed)"
    rcon setblock 3 100 12 minecraft:stone > /dev/null 2>&1 || true
    mb_blocked="$(wait_for ': 0b' data get block 4 100 12 formed)"
    rcon setblock 3 100 12 minecraft:air > /dev/null 2>&1 || true
    mb_cleared="$(wait_for ': 1b' data get block 4 100 12 formed)"
    mb_times="$(rcon data get block 4 100 12 times_formed 2>&1 || true)"
    rcon setblock 4 100 12 'outworld_industry:test_multiblock[facing=north]' > /dev/null 2>&1 || true
    mb_turned="$(wait_for ': 0b' data get block 4 100 12 formed)"
    rcon setblock 4 100 12 'outworld_industry:test_multiblock[facing=east]' > /dev/null 2>&1 || true
    mb_turned_back="$(wait_for ': 1b' data get block 4 100 12 formed)"
    mb_machines="$(rcon outworld machines 2>&1 || true)"

    # Gas: a full tank under an empty one.
    rcon setblock 0 100 10 'outworld_industry:test_gas_tank{gas:{gas:"outworld:hydrogen",amount:4000L}}' > /dev/null 2>&1 || true
    rcon setblock 0 101 10 outworld_industry:test_gas_tank > /dev/null 2>&1 || true
    gas_top="$(wait_for ': 4000L' data get block 0 101 10 gas.amount)"
    gas_bottom="$(rcon data get block 0 100 10 gas.amount 2>&1 || true)"

    # Crusher: three raw iron, 100 ticks each, powered from a creative battery underneath.
    rcon setblock 8 100 2 'outworld_industry:test_battery{creative:1b}' > /dev/null 2>&1 || true
    rcon setblock 8 101 2 'outworld_industry:crusher{Items:[{Slot:0b,id:"minecraft:raw_iron",count:3}]}' > /dev/null 2>&1 || true
    for _ in $(seq 1 40); do
        sleep 1
        crusher_output="$(rcon data get block 8 101 2 'Items[{Slot:1b}]' 2>&1 || true)"
        [[ "$crusher_output" == *"crushed_iron"* && "$crusher_output" == *"count: 6"* ]] && break
    done
    crusher_state="$(rcon data get block 8 101 2 2>&1 | cut -c1-600 || true)"

    # Power: a combustion generator burning one coal, three cables, and a crusher with 2 raw iron.
    rcon setblock 10 100 6 'outworld_industry:combustion_generator{Items:[{Slot:0b,id:"minecraft:coal",count:1}]}' > /dev/null 2>&1 || true
    rcon fill 11 100 6 13 100 6 outworld_industry:cable > /dev/null 2>&1 || true
    rcon setblock 14 100 6 'outworld_industry:crusher{Items:[{Slot:0b,id:"minecraft:raw_iron",count:2}]}' > /dev/null 2>&1 || true
    for _ in $(seq 1 40); do
        sleep 1
        power_output="$(rcon data get block 14 100 6 'Items[{Slot:1b}]' 2>&1 || true)"
        [[ "$power_output" == *"crushed_iron"* && "$power_output" == *"count: 4"* ]] && break
    done
    power_state="generator: $(rcon data get block 10 100 6 2>&1 | cut -c1-400 || true) | crusher: $(rcon data get block 14 100 6 2>&1 | cut -c1-400 || true)"

    # Solar: an empty battery next to a panel at noon.
    rcon time set noon > /dev/null 2>&1 || true
    rcon setblock 11 100 9 'outworld_industry:test_battery{energy:0L}' > /dev/null 2>&1 || true
    rcon setblock 10 100 9 outworld_industry:solar_panel > /dev/null 2>&1 || true
    for _ in $(seq 1 20); do
        sleep 1
        solar_battery="$(rcon data get block 11 100 9 energy 2>&1 || true)"
        [[ "$solar_battery" =~ :\ [1-9][0-9]*L?$ ]] && break
    done

    # Processing: an electric furnace, an ore washer and an electrolyzer, each fed and powered.
    rcon setblock 6 100 14 'outworld_industry:test_battery{creative:1b}' > /dev/null 2>&1 || true
    rcon setblock 6 101 14 'outworld_industry:electric_furnace{Items:[{Slot:0b,id:"outworld_industry:crushed_iron",count:2}]}' > /dev/null 2>&1 || true
    rcon setblock 9 101 14 'outworld_industry:test_battery{creative:1b}' > /dev/null 2>&1 || true
    for x in 8 10; do
        rcon setblock "$x" 99 14 'minecraft:water_cauldron[level=3]' > /dev/null 2>&1 || true
        rcon setblock "$x" 100 14 outworld_industry:test_tank > /dev/null 2>&1 || true
    done
    rcon setblock 8 101 14 'outworld_industry:ore_washer{Items:[{Slot:0b,id:"outworld_industry:crushed_copper",count:2}]}' > /dev/null 2>&1 || true
    rcon setblock 10 101 14 outworld_industry:electrolyzer > /dev/null 2>&1 || true
    rcon setblock 10 102 14 outworld_industry:test_gas_tank > /dev/null 2>&1 || true
    for _ in $(seq 1 30); do
        sleep 1
        furnace_output="$(rcon data get block 6 101 14 'Items[{Slot:1b}]' 2>&1 || true)"
        washer_output="$(rcon data get block 8 101 14 'Items[{Slot:1b}]' 2>&1 || true)"
        electrolyzer_gas="$(rcon data get block 10 102 14 gas 2>&1 || true)"
        [[ "$furnace_output" == *"iron_ingot"* && "$furnace_output" == *"count: 2"* \
            && "$washer_output" == *"purified_copper"* && "$washer_output" == *"count: 3"* \
            && "$electrolyzer_gas" =~ amount:\ [1-9] ]] && break
    done
    processing_state="furnace: $(rcon data get block 6 101 14 2>&1 | cut -c1-300 || true) | washer: $(rcon data get block 8 101 14 2>&1 | cut -c1-400 || true) | electrolyzer: $(rcon data get block 10 101 14 2>&1 | cut -c1-500 || true)"

    # Pipes and tanks: water through fluid pipes to a washer, gas through a gas pipe into a tank block,
    # and an empty battery under the power test's cable.
    rcon setblock 12 99 6 'outworld_industry:test_battery{energy:0L}' > /dev/null 2>&1 || true
    rcon setblock 12 99 12 'minecraft:water_cauldron[level=3]' > /dev/null 2>&1 || true
    rcon setblock 12 100 12 outworld_industry:test_tank > /dev/null 2>&1 || true
    rcon setblock 14 100 12 'outworld_industry:test_battery{creative:1b}' > /dev/null 2>&1 || true
    rcon setblock 14 101 12 'outworld_industry:ore_washer{Items:[{Slot:0b,id:"outworld_industry:crushed_gold",count:2}]}' > /dev/null 2>&1 || true
    rcon fill 12 101 12 13 101 12 outworld_industry:fluid_pipe > /dev/null 2>&1 || true
    rcon setblock 12 99 15 'minecraft:water_cauldron[level=3]' > /dev/null 2>&1 || true
    rcon setblock 12 100 15 outworld_industry:test_tank > /dev/null 2>&1 || true
    rcon setblock 11 101 15 'outworld_industry:test_battery{creative:1b}' > /dev/null 2>&1 || true
    rcon setblock 13 101 15 outworld_industry:gas_pipe > /dev/null 2>&1 || true
    rcon setblock 14 101 15 outworld_industry:gas_tank > /dev/null 2>&1 || true
    rcon setblock 12 101 15 outworld_industry:electrolyzer > /dev/null 2>&1 || true
    for _ in $(seq 1 30); do
        sleep 1
        pipe_washer_output="$(rcon data get block 14 101 12 'Items[{Slot:1b}]' 2>&1 || true)"
        gas_grid="$(rcon outworld grid 14 101 15 2>&1 || true)"
        cable_battery="$(rcon data get block 12 99 6 energy 2>&1 || true)"
        [[ "$pipe_washer_output" == *"purified_gold"* && "$pipe_washer_output" == *"count: 3"* \
            && "$gas_grid" =~ outworld:(hydrogen|oxygen)\ [1-9] && "$cable_battery" =~ :\ [1-9][0-9]*L?$ ]] && break
    done
    grids_state="fluid pipe: $(rcon outworld grid 13 101 12 2>&1 || true) | washer: $(rcon data get block 14 101 12 2>&1 | cut -c1-300 || true) | cable: $(rcon outworld grid 12 100 6 2>&1 || true)"

    # Stress test: about 55 seconds of warm-up, load, settling and idle.
    stress_start="$(rcon outworld stress start machines 500 64 100 64 2>&1 || true)"
    for _ in $(seq 1 50); do
        sleep 3
        stress_status="$(rcon outworld stress status 2>&1 || true)"
        [[ "$stress_status" == *"Budget "* ]] && break
    done

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
failed_checks=()
pass() { echo "  ok:      $1"; }
fail() { echo "  MISSING: $1"; failures=$((failures + 1)); failed_checks+=("$1"); }
check_log() {
    if grep -q -E "$2" "$log"; then pass "$1"; else fail "$1"; fi
}

echo "Server boot on ${loader}: ${result} after ${elapsed}s"
check_log "server reached Done" 'Done \([0-9.,]+s\)!'
check_log "Outworld Core started" 'Outworld Core starting on'
check_log "Outworld Industry started" 'Outworld Industry starting on'
check_log "Industry blocks registered" 'Registered 17 entries in minecraft:block for outworld_industry'
check_log "Industry items registered" 'Registered 23 entries in minecraft:item for outworld_industry'
check_log "Industry block entity types registered" 'Registered 12 entries in minecraft:block_entity_type for outworld_industry'
check_log "Industry creative tab registered" 'Registered 1 entries in minecraft:creative_mode_tab for outworld_industry'
check_log "Industry menus registered" 'Registered 1 entries in minecraft:menu for outworld_industry'
check_log "Core's machine menu registered" 'Registered 1 entries in minecraft:menu for outworld( |$)'
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
if [[ "$starved_machines" == *"1 loaded, 0 awake"* && "$starved_work" == *": 0" ]]; then
    pass "job without energy: machine did no work and slept"
else
    fail "machine without energy sleeps (machines: ${starved_machines:-none}; work: ${starved_work:-none})"
fi
if [[ "$job_machines" == *"1 loaded, 1 awake"* ]]; then
    pass "battery placed next to it woke the machine"
else
    fail "battery placed next to it wakes the machine (machines: ${job_machines:-none})"
fi
job_batches=$(( $(batches "$done_machines") - $(batches "$starved_machines") ))
if [[ "$done_work" == *": 100" && "$done_machines" == *"1 loaded, 0 awake"* ]] && (( job_batches >= 25 && job_batches <= 26 )); then
    pass "100 ticks of work done in ${job_batches} batches, then asleep"
else
    fail "job of 100 ticks in 25-26 batches, then asleep (work: ${done_work:-none}; machines: ${done_machines:-none}; batches for the job: ${job_batches})"
fi
if [[ "$battery_energy" =~ :\ 9000L?$ ]]; then
    pass "1,000 FE moved through the loader's energy API (battery: ${battery_energy})"
else
    fail "battery left with 9000 FE (battery: ${battery_energy:-none})"
fi

if [[ "$crate_count" == *": 8" && "$crate_item" == *"iron_ingot"* ]]; then
    pass "8 iron ingots moved chest > crate > hopper > crate through the loader's item API"
else
    fail "8 iron ingots in the bottom crate (count: ${crate_count:-none}; item: ${crate_item:-none})"
fi
if [[ "$tank_top" =~ :\ 81000L?$ && "$tank_bottom" =~ :\ 0L?$ && "$cauldron_empty" == *"passed"* ]]; then
    pass "a bucket of water moved cauldron > tank > tank through the loader's fluid API"
else
    fail "81,000 droplets in the top tank, bottom tank and cauldron empty (top: ${tank_top:-none}; bottom: ${tank_bottom:-none}; cauldron: ${cauldron_empty:-none})"
fi
if [[ "$final_machines" == *"5 loaded, 0 awake"* ]]; then
    pass "all machines asleep once nothing moves (${final_machines})"
else
    fail "5 machines loaded, all asleep (machines: ${final_machines:-none})"
fi

if [[ "$perf_reply" == *"Server tick: "* && "$perf_reply" == *"outworld:machines: "* \
        && "$perf_reply" =~ outworld_industry:test_block:\ [0-9.]+\ ms\ average.*batches\ per\ tick ]]; then
    pass "/outworld perf reports the server tick, machines and the test machine's batches"
else
    fail "/outworld perf with server tick, machines and test_block batches (reply: ${perf_reply:-none})"
fi

if [[ "$mb_formed" == *": 1b" ]]; then
    pass "test multiblock formed from its data file, facing east"
else
    fail "test multiblock forms (formed: ${mb_formed:-none})"
fi
if [[ "$mb_broken" == *": 0b" && "$mb_restored" == *": 1b" ]]; then
    pass "removing a shell block unformed it; putting it back formed it again"
else
    fail "multiblock unforms and re-forms with a shell block (removed: ${mb_broken:-none}; replaced: ${mb_restored:-none})"
fi
if [[ "$mb_blocked" == *": 0b" && "$mb_cleared" == *": 1b" && "$mb_times" =~ :\ 3$ ]]; then
    pass "filling the air core unformed it; clearing it formed it again (formed 3 times)"
else
    fail "multiblock needs its air core, formed 3 times (filled: ${mb_blocked:-none}; cleared: ${mb_cleared:-none}; times: ${mb_times:-none})"
fi
if [[ "$mb_turned" == *": 0b" && "$mb_turned_back" == *": 1b" ]]; then
    pass "turning the controller north unformed it; turning it back east formed it again"
else
    fail "multiblock follows its controller's facing (north: ${mb_turned:-none}; east again: ${mb_turned_back:-none})"
fi
if [[ "$mb_machines" == *"27 multiblock positions watched"* ]]; then
    pass "27 positions watched for block changes"
else
    fail "27 multiblock positions watched (machines: ${mb_machines:-none})"
fi

if [[ "$gas_top" =~ :\ 4000L?$ && "$gas_bottom" =~ :\ 0L?$ ]]; then
    pass "4 buckets of hydrogen moved up through Core's gas storage"
else
    fail "4,000 mB of hydrogen in the top gas tank, none in the bottom one (top: ${gas_top:-none}; bottom: ${gas_bottom:-none})"
fi

if [[ "$crusher_output" == *"crushed_iron"* && "$crusher_output" == *"count: 6"* ]]; then
    pass "the crusher turned 3 raw iron into 6 crushed iron, from its data recipe"
else
    fail "6 crushed iron in the crusher's first output slot (slot: ${crusher_output:-none})"
fi
if [[ "$power_output" == *"crushed_iron"* && "$power_output" == *"count: 4"* ]]; then
    pass "a combustion generator burning coal powered a crusher through three cables (2 raw iron > 4 crushed)"
else
    fail "4 crushed iron from a crusher powered by a generator through cables (slot: ${power_output:-none})"
fi
if [[ "$solar_battery" =~ :\ [1-9][0-9]*L?$ ]]; then
    pass "a solar panel at noon charged the battery next to it (${solar_battery})"
else
    fail "solar panel charges an empty battery at noon (battery: ${solar_battery:-none})"
fi
if [[ "$furnace_output" == *"iron_ingot"* && "$furnace_output" == *"count: 2"* ]]; then
    pass "the electric furnace smelted 2 crushed iron into 2 iron ingots with the vanilla recipe"
else
    fail "2 iron ingots in the electric furnace's output slot (slot: ${furnace_output:-none})"
fi
if [[ "$washer_output" == *"purified_copper"* && "$washer_output" == *"count: 3"* ]]; then
    pass "the ore washer took water through the fluid API and washed 2 crushed copper into 3 purified copper"
else
    fail "3 purified copper in the ore washer's output slot (slot: ${washer_output:-none})"
fi
if [[ "$electrolyzer_gas" =~ amount:\ [1-9] && ( "$electrolyzer_gas" == *"outworld:hydrogen"* || "$electrolyzer_gas" == *"outworld:oxygen"* ) ]]; then
    pass "the electrolyzer split water into gas, collected above it (${electrolyzer_gas})"
else
    fail "hydrogen or oxygen from the electrolyzer in the gas tank above it (tank: ${electrolyzer_gas:-none})"
fi
if [[ "$pipe_washer_output" == *"purified_gold"* && "$pipe_washer_output" == *"count: 3"* ]]; then
    pass "water pumped into fluid pipes reached the ore washer at their end (3 purified gold)"
else
    fail "3 purified gold from an ore washer fed water through fluid pipes (slot: ${pipe_washer_output:-none})"
fi
if [[ "$gas_grid" =~ outworld:(hydrogen|oxygen)\ [1-9] ]]; then
    pass "the electrolyzer filled a gas tank block through a gas pipe (${gas_grid})"
else
    fail "hydrogen or oxygen in the gas tank block's grid (grid: ${gas_grid:-none})"
fi
if [[ "$cable_battery" =~ :\ [1-9][0-9]*L?$ ]]; then
    pass "the cable grid charged an empty battery next to it (${cable_battery})"
else
    fail "an empty battery under the cable charges from the grid (battery: ${cable_battery:-none})"
fi
saved_grids="$(find "$run_dir" -name 'gas_grids.dat' -o -name 'fluid_grids.dat' 2>/dev/null | sort | tr '\n' ' ')"
if [[ "$saved_grids" == *"fluid_grids.dat"* && "$saved_grids" == *"gas_grids.dat"* ]]; then
    pass "fluid and gas grid contents were saved with the world (${saved_grids})"
else
    fail "fluid_grids.dat and gas_grids.dat saved with the world (found: ${saved_grids:-none})"
fi
data_errors="$(grep -i -E "couldn't (parse|load)|parsing error|failed to (parse|load)" "$log" | grep -i 'outworld' | cut -c1-300 | head -n 5)"
if [[ -z "$data_errors" ]]; then
    pass "no Outworld data file failed to load"
else
    fail "Outworld data files that failed to load: ${data_errors}"
fi

stress_report="$run_dir/outworld-stress/machines.json"
stress_check="$(python3 - "$stress_report" <<'PY' 2>&1
import json, os, sys
try:
    report = json.load(open(sys.argv[1]))
except Exception as error:
    print(f"no report ({error})")
    sys.exit(1)
active, idle = report["active"], report["idle"]
problems = []
if not report["within_budget"]:
    problems.append(f"over budget: {active['outworld_ms']['avg']} ms")
if active["machines_awake"] < 500:
    problems.append(f"only {active['machines_awake']} machines awake under load")
if active["sections_ms"].get("outworld_industry:test_block", {}).get("avg", 0) <= 0:
    problems.append("no test machine batches measured under load")
if idle["machines_awake"] != 0:
    problems.append(f"{idle['machines_awake']} machines still awake when idle")
if idle["outworld_ms"]["avg"] > 0.05:
    problems.append(f"idle cost {idle['outworld_ms']['avg']} ms")
summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
if summary_file:
    with open(summary_file, "a") as out:
        out.write(f"### Stress test: {report['scenario']} x{report['count']} on {report['loader']}\n\n")
        out.write("| Phase | Outworld avg | p95 | max | Server tick avg | Machines awake |\n|---|---|---|---|---|---|\n")
        for name, phase in (("Under load", active), ("Idle", idle)):
            ow, st = phase["outworld_ms"], phase["server_tick_ms"]
            out.write(f"| {name} | {ow['avg']} ms | {ow['p95']} ms | {ow['max']} ms | {st['avg']} ms | {phase['machines_awake']} |\n")
        out.write(f"\nBudget {report['budget_ms']} ms per tick: {'within budget' if report['within_budget'] else '**over budget**'}\n\n")
print("; ".join(problems) if problems else "ok")
sys.exit(1 if problems else 0)
PY
)"
if [[ "$stress_status" == *"within budget"* && "$stress_check" == "ok" ]]; then
    pass "stress test: 500 busy machines within the 5 ms budget, all asleep and free once quiet"
else
    fail "stress test within budget, 500 awake under load, none awake and ~0 ms when idle (status: ${stress_status:-none}; check: ${stress_check:-none})"
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
    echo "Boot checks: ${failures} failed."
    for check in "${failed_checks[@]}"; do
        echo "  MISSING: ${check}"
    done
    echo "/outworld version reply: ${version_reply:-none}"
    echo "Machines after placing: ${placed_machines:-none} | work: ${placed_work:-none}"
    echo "Machines with a job but no energy: ${starved_machines:-none} | work: ${starved_work:-none}"
    echo "Machines after the battery was placed: ${job_machines:-none}"
    echo "Machines after the job: ${done_machines:-none} | work: ${done_work:-none} | battery: ${battery_energy:-none}"
    echo "Items: bottom crate count ${crate_count:-none}, item ${crate_item:-none}"
    echo "Fluid: top tank ${tank_top:-none} | bottom tank ${tank_bottom:-none} | cauldron empty: ${cauldron_empty:-none}"
    echo "Machines at the end: ${final_machines:-none}"
    echo "Multiblock: formed ${mb_formed:-none} | shell block removed ${mb_broken:-none} | replaced ${mb_restored:-none} | core filled ${mb_blocked:-none} | cleared ${mb_cleared:-none} | ${mb_times:-none}"
    echo "Multiblock turned north: ${mb_turned:-none} | back east: ${mb_turned_back:-none} | ${mb_machines:-none}"
    echo "Gas: top tank ${gas_top:-none} | bottom tank ${gas_bottom:-none}"
    echo "Crusher output slot: ${crusher_output:-none}"
    echo "Crusher: ${crusher_state:-none}"
    echo "Power: crusher output slot ${power_output:-none} | solar battery ${solar_battery:-none}"
    echo "Power blocks: ${power_state:-none}"
    echo "Processing: furnace slot ${furnace_output:-none} | washer slot ${washer_output:-none} | gas above electrolyzer ${electrolyzer_gas:-none}"
    echo "Processing blocks: ${processing_state:-none}"
    echo "Pipes: washer slot ${pipe_washer_output:-none} | gas tank ${gas_grid:-none} | battery under cable ${cable_battery:-none}"
    echo "Grids: ${grids_state:-none}"
    echo "Stress test start: ${stress_start:-none}"
    echo "Stress test result: ${stress_status:-none}"
    echo "/outworld perf while the test machine worked:"
    echo "${perf_reply:-none}"
} >> "$log"

# Shown on the run's page, and readable through the checks API.
if [[ -n "$stress_status" ]]; then
    stress_notice="${stress_status//'%'/'%25'}"
    echo "::notice title=Stress test (${loader})::${stress_notice//$'\n'/'%0A'}"
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
