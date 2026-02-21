# Scalable Document Summarizer Remediation Plan

## Current issues observed in code

1. **Over-compression across two summarization passes**
   - `SummaryGenerationServiceImpl` summarizes each chunk first and then summarizes those summaries again, both using the same requested target length. This compounds compression and shrinks outputs, especially for `LONG` and `VERY_LONG`. 
2. **Markdown quality mismatch in output contract**
   - `SummaryPromptBuilder` asks for JSON-only output and includes contradictory rules (e.g., references to `keyPoints` while schema does not include it). This leads to plain-text summary strings with inconsistent markdown structure.
3. **Single-model bottleneck and quota/rate-limit risk**
   - `SummaryLlmServiceImpl` currently uses one configured model for all stages. High chunk counts create bursty requests to one endpoint/model, increasing HTTP 429 probability.

---

## Target architecture

Move from a **single-lane two-pass summarizer** to a **multi-stage, quota-aware summarization pipeline**:

- **Stage A (map):** chunk-level extraction (cheap/fast model, higher concurrency).
- **Stage B (reduce tree):** hierarchical merges with explicit expansion budget and markdown-preserving merge prompts.
- **Stage C (final render):** single high-quality formatting pass (quality model) that enforces markdown outline and validates word count.
- **Resilience layer:** provider/model routing + backoff + queue + idempotent retries.
- **Governance layer:** dynamic token budgets, observability, and quality checks.

---

## Detailed remediation plan

## 1) Fix output sizing and over-compression

### 1.1 Introduce stage-specific length budgets
- Add a `SummaryBudgetPolicy` component that resolves:
  - target words per chunk summary (not equal to final output goal),
  - merge-pass target words,
  - final output target words.
- Example policy:
  - `SHORT`: final 120 words, chunk 80–120 words
  - `MEDIUM`: final 220–300, chunk 120–180
  - `LONG`: final 450–650, chunk 180–260
  - `VERY_LONG`: final 700–1100, chunk 220–320

### 1.2 Replace one-shot aggregation with hierarchical reduce
- Instead of summarizing N chunk summaries in one final prompt, merge in a tree (fan-in 4–8):
  - merge level 1: combine local groups,
  - merge level 2+: recursively combine until one summary remains.
- Benefits:
  - better retention of details,
  - lower per-request prompt size,
  - smoother token usage and fewer failures on large documents.

### 1.3 Preserve salient entities and facts through passes
- In chunk prompt output JSON, include fields such as:
  - `summaryMarkdown`,
  - `keyFacts[]`,
  - `entities[]`,
  - `openQuestions[]` (optional).
- Merge prompts should explicitly preserve and de-duplicate these fields before final prose rendering.

---

## 2) Produce consistent markdown summaries

### 2.1 Normalize JSON schema and remove contradictory prompt rules
- Define one strict schema in DTO and prompt text, e.g.:
  ```json
  {
    "summaryMarkdown": "string",
    "wordCount": 123,
    "keyPoints": ["..."]
  }
  ```
- Ensure every prompt references exactly these fields.

### 2.2 Add explicit markdown contract
- In prompts require:
  - title + short overview paragraph,
  - `## Key Points` section with bullets,
  - optional `## Risks/Actions` when present in source.
- Add rules:
  - no fenced code block wrapping entire response,
  - no plain-text-only body,
  - no heading level skips.

### 2.3 Add post-generation markdown sanity check
- Add a lightweight validator:
  - check for at least one heading + one bullet list,
  - verify `wordCount` is within threshold of computed words.
- On failed validation, run a cheap “format-fix” pass (not full re-summarization).

---

## 3) Eliminate single-model bottleneck and reduce 429s

### 3.1 Split models by stage
- Configure at least two model lanes:
  - `doc-summary.chunk.model` (fast/cheap),
  - `doc-summary.aggregate.model` (quality/final).
- Optionally add provider fallback chain per lane.

### 3.2 Add rate-limit-aware retries with jitter
- Replace fixed sleep retry with exponential backoff + jitter.
- Detect 429/5xx explicitly and retry only retryable failures.
- Respect `Retry-After` header when available.

### 3.3 Add bounded concurrency and queueing
- Process chunk summarization with bounded parallelism (e.g., 3–8 workers configurable).
- Introduce queue-backed async jobs for long docs:
  - `PENDING -> PROCESSING -> COMPLETED/FAILED` with progress metadata.
- Keep synchronous mode for small docs only.

### 3.4 Add idempotency and partial resume
- Persist intermediate stage outputs (`chunk summaries`, `merge level outputs`).
- On retry/restart, resume from latest completed stage instead of recomputing all chunks.

---

## 4) Token/cost governance improvements

### 4.1 Stage-level token allocation
- Reserve quota by stage before execution begins.
- Prevent starvation of final pass by holding a finalization reserve.

### 4.2 Dynamic `maxTokens` policy
- Move from static length-based caps to model-aware + stage-aware caps:
  - `cap = min(stageCap, remainingQuota - safetyMargin - promptTokens)`.
- Increase stage cap for aggregate/final passes for `LONG` and `VERY_LONG`.

### 4.3 Quality-vs-cost knobs
- Introduce per-plan controls:
  - max chunks processed,
  - max merge depth,
  - model tier allowed.

---

## 5) Observability and SLOs

- Emit metrics by stage/model:
  - p50/p95 latency,
  - tokens in/out,
  - retries, 429 count, fallback count,
  - markdown validation pass rate.
- Add tracing fields:
  - `documentId`, `summaryId`, `stage`, `attempt`, `model`.
- Define SLOs:
  - success rate >= 99% for small/medium docs,
  - 429-recovered success >= 95% with retries,
  - markdown-compliant output >= 99%.

---

## 6) Rollout plan (phased)

### Phase 0 (1–2 days): quick wins
- Fix prompt schema consistency.
- Add markdown output contract.
- Implement exponential backoff with jitter.
- Split chunk vs aggregate model config.

### Phase 1 (3–5 days): quality stability
- Add hierarchical merge strategy.
- Add stage-specific budget policy.
- Add markdown validator + repair pass.

### Phase 2 (1 week): scalability
- Add bounded parallel chunk workers.
- Add async queue mode for large docs.
- Persist intermediate artifacts for resume.

### Phase 3 (ongoing): optimization
- Add model fallback routing.
- Tune budgets via telemetry and A/B testing.
- Add product-level controls by subscription tier.

---

## 7) Suggested concrete code changes

- `SummaryPromptBuilder`
  - Introduce separate prompt methods per stage (`chunk`, `merge`, `finalRender`) with one shared response schema block.
- `SummaryGenerationServiceImpl`
  - Replace linear two-pass flow with orchestrated map/reduce pipeline.
  - Add stage-level token reservation and resume hooks.
- `SummaryLlmServiceImpl`
  - Support stage-specific model selection + fallback candidates.
  - Add retry strategy abstraction with jitter and retry classification.
- `application-*.yml`
  - Add per-stage model, concurrency, and retry settings.

---

## 8) Acceptance criteria

- `LONG` and `VERY_LONG` summaries consistently hit target word-range without major detail loss.
- Output is valid JSON + markdown-rich `summaryMarkdown` every time.
- Under load tests, system sustains chunk-heavy docs with minimal 429 failures and successful retry recovery.
- End-to-end cost and latency dashboards available by stage and model.
