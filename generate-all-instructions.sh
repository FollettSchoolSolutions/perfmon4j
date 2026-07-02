#!/bin/bash

# Generate Instructions Script
# Systematically runs the generate-instructions prompt for each directory in the JSON file.
# Supports both GitHub Copilot CLI and Claude Code CLI.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INPUT_FILE="$SCRIPT_DIR/directory-instructions.json"
CLI_TOOL="copilot"

while [[ $# -gt 0 ]]; do
    case $1 in
        --cli=*) CLI_TOOL="${1#*=}"; shift ;;
        --input=*) INPUT_FILE="${1#*=}"; shift ;;
        -h|--help)
            echo "Usage: $0 [--cli=copilot|claude] [--input=FILE]"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

if [[ "$CLI_TOOL" == "copilot" ]]; then
    PROMPT_FILE="$SCRIPT_DIR/.github/prompts/generate-instructions.prompt.md"
else
    PROMPT_FILE="$SCRIPT_DIR/.claude/commands/generate-instructions.md"
fi

PROMPT_TEMPLATE=$(cat "$PROMPT_FILE")

pending_count=$(jq '[.items[] | select(.status == "pending")] | length' "$INPUT_FILE")
echo "Found $pending_count pending items (using $CLI_TOOL)"

if [[ "$pending_count" -eq 0 ]]; then
    exit 0
fi

if [[ "$CLI_TOOL" == "copilot" ]]; then
    mkdir -p .github/instructions
fi

item_index=0
total_items=$(jq '.items | length' "$INPUT_FILE")

while [[ $item_index -lt $total_items ]]; do
    status=$(jq -r ".items[$item_index].status" "$INPUT_FILE")

    if [[ "$status" != "pending" ]]; then
        ((item_index++))
        continue
    fi

    item_id=$(jq -r ".items[$item_index].id" "$INPUT_FILE")
    item_directory=$(jq -r ".items[$item_index].directory" "$INPUT_FILE")
    item_output_file=$(jq -r ".items[$item_index].output_file" "$INPUT_FILE")

    if [[ "$CLI_TOOL" == "copilot" ]]; then
        output_path=".github/instructions/$item_output_file"
    else
        output_path="$item_directory/CLAUDE.md"
    fi

    echo "[$item_id] $item_directory -> $output_path"

    TEMP_PROMPT_FILE=$(mktemp)
    cat > "$TEMP_PROMPT_FILE" << EOF
$PROMPT_TEMPLATE

---

## Target Directory
Analyze this directory: \`$item_directory\`

## Output File
Create the instructions file at: \`$output_path\`
EOF

    cli_exit_code=0

    if [[ "$CLI_TOOL" == "copilot" ]]; then
        copilot -p "$(cat "$TEMP_PROMPT_FILE")" \
            --allow-tool 'shell(cat)' \
            --allow-tool 'shell(ls)' \
            --allow-tool 'write' \
            > /dev/null 2>&1 || cli_exit_code=$?
    else
        claude -p "$(cat "$TEMP_PROMPT_FILE")" \
            --allowedTools "Read,Write,Glob,Grep" \
            > /dev/null 2>&1 || cli_exit_code=$?
    fi

    rm -f "$TEMP_PROMPT_FILE"

    if [[ $cli_exit_code -eq 0 ]] && [[ -f "$output_path" ]]; then
        echo "  Done"
        tmp_file=$(mktemp)
        jq ".items[$item_index].status = \"done\" | .items[$item_index].analyzed_at = \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"" "$INPUT_FILE" > "$tmp_file"
        mv "$tmp_file" "$INPUT_FILE"
    else
        echo "  Error"
        tmp_file=$(mktemp)
        jq ".items[$item_index].status = \"error\" | .items[$item_index].analyzed_at = \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"" "$INPUT_FILE" > "$tmp_file"
        mv "$tmp_file" "$INPUT_FILE"
    fi

    ((item_index++))
    sleep 1
done

echo ""
done_count=$(jq '[.items[] | select(.status == "done")] | length' "$INPUT_FILE")
error_count=$(jq '[.items[] | select(.status == "error")] | length' "$INPUT_FILE")
pending_count=$(jq '[.items[] | select(.status == "pending")] | length' "$INPUT_FILE")

echo "Done: $done_count | Errors: $error_count | Pending: $pending_count"
