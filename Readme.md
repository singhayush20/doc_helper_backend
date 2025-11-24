## TODOs for Quota Service

* [ ] Make quota consumption atomic

  * TODO: Replace `checkAndEnforceQuota + incrementUsage` with a single conditional `UPDATE` (`tryConsumeTokens(userId, tokensToUse, now)`) that checks `isActive`, `resetDate`, and `currentMonthlyUsage + tokensToUse <= monthlyLimit`.
  * Pros: Correct under concurrency, safe across multiple instances.
  * Cons: More complex JPQL/SQL.
  * Alternatives: Use optimistic locking (`@Version`) with retry or pessimistic locking (`SELECT ... FOR UPDATE`).

* [ ] Remove quota logic from `recordTokenUsage` that relies on non-atomic checks

  * TODO: Delete or limit external use of `checkAndEnforceQuota` and rely on `tryConsumeTokens` as the only enforcement.
  * Pros: Single source of truth for quota enforcement.
  * Cons: Need to refactor existing call sites.
  * Alternatives: Keep `checkAndEnforceQuota` only for read-only display, not for enforcement.

* [ ] Fix quota reset stale-entity behavior

  * TODO: After `resetQuota(quota)`, re-fetch quota from DB before computing usage, or move reset into atomic SQL (e.g., `resetIfNeeded(...)`).
  * Pros: Avoids using old `currentMonthlyUsage` or `resetDate` after reset.
  * Cons: Extra DB hit if done as separate fetch.
  * Alternatives: Add `billingPeriod` column and handle resets via period change instead of `resetDate` checks in service.

* [ ] Integrate reset logic with quota consumption

  * TODO: Add DB-side reset logic (`resetIfNeeded(userId, now)`) and call it immediately before `tryConsumeTokens`.
  * Pros: Month-boundary handling is consistent and race-free.
  * Cons: Extra update/check per first call after boundary.
  * Alternatives: Scheduled job that resets all quotas at period start, then `tryConsumeTokens` only checks usage and limit.

* [ ] Clarify transaction boundaries for mutation

  * TODO: Make only `recordTokenUsage`, `createDefaultQuota`, and future `updateUserTier` public mutation entrypoints; make `resetQuota`, `updateUserQuota`, `checkAndEnforceQuota` private/package-private or remove them.
  * Pros: Prevents misuse of low-level methods that bypass enforcement.
  * Cons: Requires adjusting other services/controllers if they call these methods directly.
  * Alternatives: Keep them public but add explicit JavaDoc and enforce use via code-review/discipline.

* [ ] Mark read-only methods as read-only or non-transactional

  * TODO: Add `@Transactional(readOnly = true)` (or none) to methods that only read: `getCurrentMonthUsage`, `getUserQuotaInfo`, `getDailyUsageSummaryForDays`, `getUsageBreakdown*`, `getTotalCost`.
  * Pros: Slight performance and clarity improvement.
  * Cons: Minimal impact; more annotations.
  * Alternatives: Leave as-is if your global transaction configuration is already optimized.

* [ ] Add idempotency to usage recording

  * TODO: Add a unique constraint on `(user_id, message_id, operation_type)` (or `(user_id, thread_id, message_id, operation_type)`) in `UserTokenUsage`.
  * TODO: In `recordTokenUsage`, handle `DataIntegrityViolationException` as “already recorded” or check existence before insert.
  * Pros: Prevents double-billing on retries.
  * Cons: Requires stable, unique `messageId` semantics from caller.
  * Alternatives: Use an explicit `idempotencyKey` column/table and key requests on that.

* [ ] Harden pricing config handling

  * TODO: In `calculateCost`, validate `pricingConfig.getInputCost(modelName)` / `getOutputCost(modelName)` and throw a clear exception or default safely if model is unknown.
  * Pros: Avoids silent wrong billing if model name misconfigured.
  * Cons: More failure paths; need mapping maintenance.
  * Alternatives: Hard-code a small set of allowed models and reject others.

* [ ] Consider increasing money precision if you expect high volume

  * TODO: Revisit `@Column(precision = 10, scale = 6)` for `estimatedCost`; consider `precision = 18` if large values are possible.
  * Pros: Future-proof for large-scale usage.
  * Cons: DB schema change, maybe overkill for small deployments.
  * Alternatives: Keep as-is and revisit after observing real usage ranges.

* [ ] Auto-create quota for new users (if desired)

  * TODO: Change `getCurrentUserQuota` to `getOrCreateCurrentUserQuota` and call `createDefaultQuota(userId)` when quota not found (if that matches your user lifecycle).
  * Pros: Fewer “quota not found” errors during normal onboarding.
  * Cons: Might accidentally create quota for logically “disabled” users if misused.
  * Alternatives: Keep strict behavior and ensure quota is created explicitly during user registration.

* [ ] Improve tier-based configuration

  * TODO: Move tier limits to a structured config (per `AccountTier`) or a separate `account_tier` table instead of only `billingConfig.getDefaultMonthlyLimit()`.
  * Pros: Easy to add paid tiers, different limits and features.
  * Cons: More configuration entities to manage.
  * Alternatives: Keep simple if you only plan 1–2 static tiers.

* [ ] Plan soft/hard limit semantics

  * TODO: Add fields like `allowOverage`, `hardLimit` to `UserTokenQuota` and adjust `tryConsumeTokens` logic if you want soft caps vs hard caps and pay-as-you-go behavior.
  * Pros: More flexible monetization (free tier hard stop, paid tier soft stop).
  * Cons: More complex business logic around quotas.
  * Alternatives: Keep strict hard limit for all tiers to start with.

* [ ] Enrich usage for analytics and provider-level costs

  * TODO: Add optional fields in `UserTokenUsage` for `modelProvider` and/or `billingCategory` to distinguish OpenAI/Anthropic/local, etc.
  * Pros: Easier cost attribution and optimization later.
  * Cons: Schema changes and more data to populate.
  * Alternatives: Derive provider from `modelName` conventionally (e.g., prefix) without extra columns.

* [ ] Add periodic reconciliation for quotas

  * TODO: Implement a background job that aggregates `UserTokenUsage` for each user and compares with `UserTokenQuota.currentMonthlyUsage` to detect inconsistencies.
  * Pros: Early detection of bugs, accounting correctness.
  * Cons: Extra operational complexity; requires scheduling.
  * Alternatives: Use on-demand reconciliation tools/scripts rather than a scheduled job.

* [ ] Clarify `updateUserTier` behavior

  * TODO: When implementing `updateUserTier`, decide whether changing tier mid-cycle resets usage, only changes limit going forward, or applies from next cycle. Implement that logic explicitly.
  * Pros: Predictable billing and user expectations.
  * Cons: More business rules to manage.
  * Alternatives: Restrict tier change to billing-boundary events (e.g., only change at next reset).

* [ ] Optional: Normalize time handling for billing

  * TODO: Consider storing an explicit `billingPeriod` (e.g., `YearMonth` or a string) in quota/usage summaries instead of only `resetDate`, and make all billing operations period-aware.
  * Pros: Cleaner reasoning about billing periods and historical changes.
  * Cons: Schema and query complexity increases.
  * Alternatives: Keep `resetDate`-based approach but rigorously test month-boundary edge cases with your chosen timezone.
