#!/usr/bin/env bash
# Posts the useful part of a failed CI log as a pull request comment, so the failure can be read
# without opening the Actions page.
#
# Usage: .github/scripts/report-failure.sh "<check name>" <log file>
# Needs GH_TOKEN and PR_NUMBER in the environment.
set -uo pipefail

title="$1"
log="$2"
comment="$(mktemp)"

{
    echo "### CI failure: ${title}"
    echo
    echo "Run: ${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
    echo
    if [[ ! -f "$log" ]]; then
        echo "No log file was written; the step failed before it started. See the run above."
    else
        echo "<details><summary>Errors</summary>"
        echo
        echo '```'
        grep -n -E 'What went wrong|error:|Exception|Caused by|FAILED|Could not|Unsupported|MISSING' "$log" \
            | cut -c1-300 | head -n 80 || true
        grep -n -A 12 'What went wrong' "$log" | cut -c1-300 | head -n 60 || true
        echo '```'
        echo "</details>"
        echo
        echo "<details><summary>Last 120 lines</summary>"
        echo
        echo '```'
        tail -n 120 "$log" | cut -c1-300
        echo '```'
        echo "</details>"
    fi
} > "$comment"

gh pr comment "$PR_NUMBER" --repo "$GITHUB_REPOSITORY" --body-file "$comment"
