#!/usr/bin/env bash
# Prints whether the runner can reach each Maven repository the build uses, so a dependency
# failure in CI shows whether a repository was down or unreachable.
set -uo pipefail

check() {
    local name="$1" url="$2"
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$url")
    echo "Repository check: ${name}: HTTP ${code} (${url})"
}

check "Maven Central" "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.10.1/asm-9.10.1.pom"
check "Fabric" "https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.18.1/fabric-loom-1.18.1.pom"
check "NeoForged" "https://maven.neoforged.net/releases/net/neoforged/neoforge/26.3.0.113/neoforge-26.3.0.113.pom"
check "Gradle Plugin Portal" "https://plugins.gradle.org/m2/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/2.0.147/net.neoforged.moddev.gradle.plugin-2.0.147.pom"
exit 0
