# CLAUDE — Usage & Integration Guidelines

Purpose
-------
This document provides concise instructions for integrating with Claude-style LLM providers, plus usage recommendations, safety and privacy guidance, and example requests. It is intended as a quick-reference for contributors and maintainers.

Setup
-----
- Store your API key in an environment variable (recommended):

```
export CLAUDE_API_KEY="your_api_key_here"
```

- Use your provider's official client library when possible. When calling the HTTP API directly, set the required auth header (e.g., `Authorization: Bearer <key>` or `x-api-key`).

Quick curl example
------------------
The exact endpoint and parameters depend on your provider and chosen model. Example (replace endpoint and header as required by your provider):

```
curl https://api.example-llm.com/v1/complete \
  -H "Authorization: Bearer $CLAUDE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"claude-2","prompt":"Summarize the following...","max_tokens":300}'
```

Consistency
-----------
Consistency across the project matters: file/skill naming conventions, code style,
directory layout, changelog format, and documentation patterns should match what
already exists. Before adding something new, look at how similar things are already
named and structured, and follow that pattern.

If the user (or a request) breaks with an established convention — for example using
camelCase for a skill file when the existing ones are kebab-case — point it out and
recommend the consistent alternative rather than silently following along.

Prompting guidance
------------------
- Be explicit: include the role (system/instruction), desired format, length limits, and examples when useful.
- Use short, focused prompts for precise tasks; use multi-turn context when you need stateful interactions.
- Prefer constrained output formats (JSON, CSV, YAML) when parsing responses programmatically.
- Include an explicit instruction to refuse disallowed requests if you need the model to enforce policy.

Privacy & Safety
----------------
- Do not send secrets, private keys, or sensitive user data unless you have a clear privacy strategy and encryption in transit + access controls.
- Sanitize or redact personally identifiable information (PII) before sending if possible.
- Verify outputs for safety-critical actions; do not rely solely on model responses for authorization, billing, or other high-risk flows.

Cost & Rate Limits
------------------
- Be mindful of token/billing costs: batch requests and limit max tokens where appropriate.
- Implement exponential backoff and retries for transient errors; respect provider rate-limit headers.

Compliance & Licensing
---------------------
- Check and comply with the chosen provider's terms of service and acceptable use policies before production use.

Troubleshooting & Reporting
---------------------------
- If you encounter unexpected behavior or cost spikes, capture the request/response (with sensitive data redacted) and open an issue describing the model, endpoint, prompt, and timestamps.

Further reading
---------------
- Refer to your chosen provider's official docs for up-to-date endpoints, SDKs, and best practices.

Project Wiki Publishing
-----------------------
This project's documentation wiki lives in **two separate git repositories** that are kept in sync **manually**. A wiki change is not complete until it has been applied to both.

1. **Source copy — the `wiki/` folder in this repo.** Wiki pages are authored here as plain Markdown files (e.g. `wiki/Configuring-the-Java-Agent.md`) and committed to the `develop` branch like any other source file. This copy is browsable in the source tree but does **not** render on GitHub's Wiki tab.
2. **Published copy — the separate GitHub Wiki repo.** The Wiki tab is backed by a distinct repository, `git@github.com:FollettSchoolSolutions/perfmon4j.wiki.git` (default branch `master`). Only pages pushed here appear at `https://github.com/FollettSchoolSolutions/perfmon4j/wiki/<Page-Name>`.

To publish or update a wiki page:

```
# 1. Author/commit the page in the main repo's wiki/ folder (on develop)
git add wiki/<Page-Name>.md
git commit -m "..."
git push origin develop

# 2. Mirror it into the GitHub Wiki repo so it renders on the Wiki tab
git clone git@github.com:FollettSchoolSolutions/perfmon4j.wiki.git /tmp/p4j-wiki
cp wiki/<Page-Name>.md /tmp/p4j-wiki/
cd /tmp/p4j-wiki
git add <Page-Name>.md
git commit -m "..."
git push origin master
```

Notes:
- **Wiki URLs omit the `.md` extension** — GitHub renders pages, it does not serve raw `.md` files. The file `Configuring-the-Java-Agent.md` is reached at `.../wiki/Configuring-the-Java-Agent`.
- Because the two copies are synced by hand, they can drift. When editing an existing page, update **both** repos.
- `_Sidebar.md` (in the wiki repo) controls the wiki navigation sidebar but does not list every page; new pages still appear in the wiki's automatic "Pages" list without a sidebar entry.

Maintainers: update this file if the project adopts a specific provider or if integration details change.
