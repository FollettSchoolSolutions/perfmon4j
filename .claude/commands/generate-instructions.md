# Generate CLAUDE.md File

Analyze a specific directory in the monorepo and create a CLAUDE.md file with context for Claude Code.

## Inputs
1. **Target Directory** (required) - The directory path to analyze (e.g., `packages/api`, `apps/web`)

## Output
Create a `CLAUDE.md` file in the target directory with all 7 sections populated.

## Steps
1. **Analyze the Directory** - Examine code structure, patterns, and conventions
2. **Generate CLAUDE.md** - Create the file with all sections
3. **Validate Content** - Ensure all mentioned code/patterns actually exist

## Sections to Include

### 1. Overview
- What this directory/package does
- Key concepts and domain terms
- Primary consumers/users of this code
- Integration points with other parts of the monorepo

### 2. Architecture & Patterns
- Folder structure within this directory
- Module boundaries and responsibilities
- Communication patterns (events, APIs, shared state)
- External service integrations

### 3. Stack Best Practices
- Language-specific idioms used here
- Framework patterns and conventions
- Dependency injection approach
- Error handling and validation patterns

### 4. Anti-Patterns
- Patterns to avoid in this directory
- Common mistakes to watch for
- Security pitfalls specific to this code

### 5. Data Models
- Key entities and their relationships
- DTOs and value objects
- Validation rules
- Database/storage patterns

### 6. Security & Configuration
- Environment variables used
- Secrets and sensitive data handling
- Authentication/authorization patterns
- API security considerations

### 7. Commands & Scripts
- Build commands for this directory
- Test commands
- Development scripts
- Deployment commands

## Guidelines
- Each section should be **concise** (aim for 5-10 bullet points per section)
- Use bullet points, not paragraphs
- Include specific file paths and code examples from THIS directory
- Only document patterns that actually exist in the code
- Skip sections that don't apply (but include the header with "N/A" note)

## Output Format
```markdown
# {DIR_NAME}

## Overview
- [specific bullet points]

## Architecture & Patterns
- [specific bullet points]

## Stack Best Practices
- [specific bullet points]

## Anti-Patterns
- [specific bullet points]

## Data Models
- [specific bullet points]

## Security & Configuration
- [specific bullet points]

## Commands & Scripts
- [specific bullet points]
```
