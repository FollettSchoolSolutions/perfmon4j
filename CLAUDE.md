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

Maintainers: update this file if the project adopts a specific provider or if integration details change.
