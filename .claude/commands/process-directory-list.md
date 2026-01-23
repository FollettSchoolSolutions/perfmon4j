# Process Directory List

Process the `directory-instructions.json` file and generate CLAUDE.md files for each directory by invoking the `/generate-instructions` command.

## Purpose

This command orchestrates the batch generation of CLAUDE.md files for multiple directories in the monorepo. It reads the directory list JSON file and systematically processes each directory item.

## Input

Reads from `directory-instructions.json` in the project root, which has this structure:

```json
{
  "items": [
    {
      "id": "1",
      "directory": "app/controllers",
      "output_file": "CLAUDE.md",
      "status": "pending",
      "analyzed_at": null
    }
  ]
}
```

## Steps

### 1. Read the Directory List
- Load `directory-instructions.json` from the current working directory
- Parse the JSON and extract the `items` array
- Display summary: "Found X directories to process"

### 2. Spawn Sub-Agents for Each Directory

**For each item in the `items` array where `status` is `"pending"`:**

#### Create Sub-Agents
Use the Task tool with `subagent_type: "general-purpose"` for each pending directory.

Each sub-agent receives this prompt:
```
Process directory "{directory_path}" from directory-instructions.json (ID: {id}):

Steps:
1. Read directory-instructions.json
2. Update item {id} to set status="in_progress"
3. Write the updated JSON back
4. Run: /generate-instructions {directory_path}
5. Read directory-instructions.json again
6. Update item {id} to set status="completed" and analyzed_at="{ISO timestamp}"
7. Write the final JSON back
8. If any errors occur, set status="error" instead

The /generate-instructions command will analyze the directory and create the CLAUDE.md file.
```

#### Launch All Agents in Parallel
**CRITICAL**: Spawn ALL sub-agents in a **single message** by making multiple Task tool calls:
```
Task(subagent_type="general-purpose", prompt="Process app/controllers...", description="Generate controllers CLAUDE.md")
Task(subagent_type="general-purpose", prompt="Process app/models...", description="Generate models CLAUDE.md")
Task(subagent_type="general-purpose", prompt="Process app/services...", description="Generate services CLAUDE.md")
... (one Task call for each pending directory)
```

This allows all directories to be processed simultaneously.

### 3. Monitor and Report Results
After all sub-agents have completed, display a summary:
```
✓ Completed: 6 directories
✗ Errors: 1 directory
⊘ Skipped: 1 directory (already completed)

Launched 7 sub-agents in parallel
```

## Usage Notes

- **Idempotent**: Running this command multiple times will only process `pending` items
- **Resumable**: If interrupted, re-run to continue from where it left off
- **Parallel Processing**: Spawns multiple sub-agents simultaneously, one per directory
- **Status tracking**: Each sub-agent updates the JSON file for its directory independently
- **Efficient**: All directories are processed concurrently for maximum speed

## Example Workflow

```bash
# 1. First create the directory list
/create-directory-list

# 2. Then process all directories
/process-directory-list

# 3. Check results
cat directory-instructions.json
```

## Important

- **Use the Task tool** to spawn sub-agents for parallel processing
- Each sub-agent should invoke `/generate-instructions` slash command directly - do NOT reimplement its logic
- Each directory gets its own dedicated sub-agent for independent processing
- Sub-agents must update the JSON file after their directory completes
- Preserve the JSON structure and formatting when updating the file. Do not add entries. 
- Launch ALL sub-agents in a SINGLE message with multiple Task tool calls for true parallelism