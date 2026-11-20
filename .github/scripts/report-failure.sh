#!/usr/bin/env bash
# Reports the useful part of a failed CI log so it can be read without opening the Actions page:
# as annotations on the check run (always; readable through the checks API), and as a pull request
# comment when the run belongs to a pull request.
#
# Usage: .github/scripts/report-failure.sh "<check name>" <log file>
# Needs GH_TOKEN in the environment, and PR_NUMBER for the comment (empty on branch pushes).
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
        echo "<details><summary>First 20 lines</summary>"
        echo
        echo '```'
        head -n 20 "$log" | cut -c1-300
        echo '```'
        echo "</details>"
        echo
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

# One annotation per part. Workflow commands need %, CR and LF escaped.
annotate() {
    local part="$1"
    local text="$2"
    [[ -z "$text" ]] && return
    text="${text//'%'/'%25'}"
    text="${text//$'\r'/'%0D'}"
    text="${text//$'\n'/'%0A'}"
    echo "::error title=${title}: ${part}::${text}"
}
if [[ -f "$log" ]]; then
    annotate "errors" "$(grep -n -E 'What went wrong|error:|Exception|Caused by|FAILED|Could not|Unsupported|MISSING' "$log" | cut -c1-300 | head -n 60)"
    annotate "what went wrong" "$(grep -n -A 12 'What went wrong' "$log" | cut -c1-300 | head -n 40)"
    annotate "last lines" "$(tail -n 60 "$log" | cut -c1-300)"
fi

if [[ -n "${PR_NUMBER:-}" ]]; then
    gh pr comment "$PR_NUMBER" --repo "$GITHUB_REPOSITORY" --body-file "$comment"
fi
