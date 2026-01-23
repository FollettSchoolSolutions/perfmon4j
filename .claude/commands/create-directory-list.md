# Create Directory List for CLAUDE.md Files

Interactively discover directories in the monorepo that should have CLAUDE.md files generated.

## Steps

### 1. Scan the Monorepo Structure
Examine the codebase to identify directories that would benefit from CLAUDE.md files. Look for:
- `packages/*` - Shared packages/libraries
- `apps/*` - Application directories
- `services/*` - Microservices
- `libs/*` - Shared libraries
- `modules/*` - Feature modules
- Any other top-level directories containing significant code

### 2. Propose Directories
Present a list of discovered directories with brief descriptions:
```
Proposed directories for CLAUDE.md files:

1. packages/api - REST API client library
2. packages/ui - Shared UI components
3. apps/web - Main web application
4. apps/mobile - React Native mobile app
5. services/auth - Authentication service
...
```

### 3. Iterate with User
Ask the user:
- Are there directories to **add** that I missed?
- Are there directories to **remove** from the list?
- Should any directories be **renamed** in the output?

Continue iterating until the user confirms the list is complete.

### 4. Generate JSON File
Once confirmed, create `directory-instructions.json` with this structure:

```json
{
  "items": [
    {
      "id": "1",
      "directory": "packages/api",
      "output_file": "CLAUDE.md",
      "status": "pending",
      "analyzed_at": null
    },
    {
      "id": "2",
      "directory": "packages/ui",
      "output_file": "CLAUDE.md",
      "status": "pending",
      "analyzed_at": null
    }
  ]
}
```

## Guidelines
- Only include directories with substantial code (skip config-only dirs)
- Skip directories that are clearly third-party or generated
- Include test directories only if they have unique patterns worth documenting
- The JSON file should be saved in the current working directory or a specified location

## Questions to Ask
1. "I found X directories. Should I include all of them or would you like to review the list first?"
2. "Are there any directories you'd like to exclude?"
3. "Are there any directories I missed that you'd like to add?"
4. "The output file names will be: [list]. Do these look correct?"
