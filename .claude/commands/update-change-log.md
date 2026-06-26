Review the current functional changes and update the change log in readme.txt under the
"** x.x.x - TBD" section.

## CONTEXT

`readme.txt` (project root) holds a hand-maintained change log. Entries are grouped by
release, newest first, in this format:

```
** 2.2.1 - TBD
- A bullet describing a functional change merged to develop for the next release.

** 2.2.0 - 11/20/24
- A shipped change...
```

The top-most "** <version> - TBD" block lists functional changes that have been merged
to `develop` but not yet released. The `- TBD` placeholder is replaced with a date only
when the release is cut (do NOT replace it).

## INPUTS

- (optional) An explicit description of the change. If omitted, derive it from the diff.
- (optional) A git ref/range to review. Defaults to changes on the current branch that
  are not yet on `develop` (`git diff develop...HEAD`), plus any uncommitted changes.

## INSTRUCTIONS

1. Determine what changed:
   - Run `git diff develop...HEAD --stat` and `git status` to see committed (branch) and
     uncommitted changes. Read the relevant diffs to understand behavior, not just files.
   - IGNORE non-functional changes: build/tooling/IDE config (`.vscode/`, `.gitignore`),
     tests, formatting, comments, wiki/docs. The change log records FUNCTIONAL changes
     visible to users (features, fixes, config options, behavior changes).
   - If there are no functional changes, say so and make no edits.
2. Open `readme.txt` and locate the top-most "** <version> - TBD" block.
   - If a TBD block exists, you will add to / amend it.
   - If the newest block is already dated (a release was just cut), create a NEW TBD
     block above it. Use the project version for the next release: read the root
     `pom.xml` `<version>` (e.g. `2.2.1-SNAPSHOT`) and strip `-SNAPSHOT`, giving
     `** 2.2.1 - TBD`.
3. Write or update bullet(s):
   - One concise bullet per distinct functional change. Start each with `- `.
   - Lead with what changed and the user-visible effect; mention the relevant
     class/component and any new system property, config attribute, or default.
   - Match the existing voice: present/past tense, plain prose, wrapped to ~80 cols
     and indented to align continuation lines as seen in existing entries.
   - Before adding, check whether an existing TBD bullet already covers this change.
     If so, MODIFY that bullet to stay accurate rather than adding a duplicate.
   - Reference an issue/wiki URL only if one clearly applies (match existing style).
4. Preserve everything else in the file exactly — only touch the TBD block.
5. Show the user the final TBD block and a one-line summary of what you added/changed.

## OUTPUT FORMAT

### Changes reviewed
- **Range:** <ref/range or "working tree">
- **Functional changes found:** <short list, or "none">
- **Ignored as non-functional:** <short list, e.g. .vscode/, tests>

### Change log update
- **Section:** `** <version> - TBD` (created new / amended existing)
- **Action:** added N bullet(s) / modified existing bullet

### Resulting TBD block
```
** <version> - TBD
- ...
```
