Below is a detailed low-level design for integrating payments with your existing **Token Quota, Subscription, and Billing** setup, keeping things **provider-agnostic** (Stripe/Razorpay, etc.) and following industry standards.

---

## 1. High-level goals (constraints you already have)

* Account tiers: `FREE`, `PRO` (extendable).
* Token quota per tier (already in `PlanConfig`).
* Token usage already tracked per user (`UserTokenQuota`, `UserTokenUsage`).
* Requirement: add **paid subscriptions** that:

  * Upgrade/downgrade tier.
  * Control quota (`monthlyLimit`, `isActive`).
  * Integrate with external payment provider via webhooks.

Payment model assumed:

* **Recurring subscription** (monthly/annual), not one-off top-ups.

---

## 2. Core domain model (DB entities)

### 2.1 `BillingPlan` (local mirror of monetization plans)

* Purpose: stable, versioned view of what users can subscribe to.
* Fields:

  * `id` (PK)
  * `code` (e.g. `PRO_MONTHLY`, `PRO_YEARLY`)
  * `accountTier` (`AccountTier.PRO`)
  * `billingPeriod` (`MONTHLY`, `YEARLY`)
  * `currency` (`INR`, `USD`, …)
  * `priceAmount` (BigDecimal)
  * `providerPlanId` (string – Stripe price ID / Razorpay plan ID)
  * `active` (boolean)
* Source of truth:

  * Values come from config/YAML initially but persisted in DB for referential integrity and history.

### 2.2 `Subscription`

* Purpose: track one active subscription per user (for now).
* Fields:

  * `id` (PK)
  * `user` (FK to `User`)
  * `billingPlan` (FK to `BillingPlan`)
  * `providerSubscriptionId` (string)
  * `status` (`ACTIVE`, `INCOMPLETE`, `PAST_DUE`, `CANCELED`, `EXPIRED`)
  * `startAt` (Instant)
  * `currentPeriodStart` (Instant)
  * `currentPeriodEnd` (Instant)
  * `cancelAtPeriodEnd` (boolean)
  * `canceledAt` (Instant, nullable)
  * `createdAt`, `updatedAt`
* Relationship to quota:

  * `Subscription` drives `UserTokenQuota.tier` and `monthlyLimit`.

### 2.3 `PaymentTransaction` (optional but recommended)

* Purpose: audit each payment attempt.
* Fields:

  * `id`
  * `user` (FK)
  * `subscription` (FK)
  * `providerPaymentId`
  * `amount`
  * `currency`
  * `status` (`PENDING`, `SUCCEEDED`, `FAILED`, `REFUNDED`)
  * `type` (`INITIAL`, `RENEWAL`, `REFUND`)
  * `occurredAt`
  * `rawPayload` (JSON or TEXT)

### 2.4 `PaymentProviderEventLog`

* Purpose: idempotency + debugging of webhooks.
* Fields:

  * `id`
  * `providerEventId`
  * `eventType`
  * `receivedAt`
  * `processed` (boolean)
  * `processedAt`
  * `rawPayload` (JSON/TEXT)

---

## 3. Service layer design

### 3.1 `PlanCatalogService`

* Responsibilities:

  * Resolve available plans for UI:

    * `getActivePlansForDisplay()`
    * `getPlanByCode(String code)`
  * Map from `AccountTier` + billing period → `BillingPlan`.
  * Bridge between config (`PlanConfig`) and DB (`BillingPlan`).

### 3.2 `SubscriptionService`

* Responsibilities (core business logic):

  * `startSubscription(userId, planCode)`

    * Validate plan.
    * Create local `Subscription` in `INCOMPLETE`/`PENDING` state.
    * Ask `PaymentOrchestrationService` to create checkout session.
  * `activateSubscriptionFromWebhook(providerSubId, ...)`

    * Mark subscription `ACTIVE`.
    * Set `currentPeriodStart`/`currentPeriodEnd`.
    * Call `QuotaManagementService.updateUserTier(...)`.
  * `handleRenewal(providerSubId, newPeriodEnd, paymentInfo)`
  * `cancelAtPeriodEnd(userId)`
  * `cancelImmediately(userId)` (admin use).

### 3.3 `PaymentOrchestrationService` (payment-provider-agnostic façade)

* Responsibilities:

  * Public methods:

    * `createCheckoutSession(userId, billingPlanId, successUrl, cancelUrl)`

      * Returns `CheckoutSessionInfo` with redirect URL + provider session ID.
    * `cancelProviderSubscription(providerSubscriptionId)`
    * `syncSubscriptionState(providerSubscriptionId)` (optional).
  * Internals:

    * Delegates to `PaymentProviderClient` (Stripe/Razorpay specific).

### 3.4 `PaymentProviderClient` (port / adapter pattern)

* Interface:

  * `createSubscriptionSession(BillingPlan plan, User user, String successUrl, String cancelUrl)`
  * `cancelSubscription(String providerSubscriptionId)`
  * `parseWebhookPayload(String rawBody, String signatureHeader)` → `PaymentWebhookEvent`
* Implementation:

  * `StripePaymentProviderClient` or `RazorpayPaymentProviderClient`.
* This keeps your domain logic independent from specific SDKs.

### 3.5 `PaymentWebhookHandlerService`

* Responsibilities:

  * Entry point for webhook controller.
  * Idempotency via `PaymentProviderEventLog`.
  * For each event type:

    * `SUBSCRIPTION_CREATED` → `SubscriptionService.activateSubscriptionFromWebhook(...)`.
    * `INVOICE_PAYMENT_SUCCEEDED` (renewal) → `SubscriptionService.handleRenewal(...)`.
    * `INVOICE_PAYMENT_FAILED` → update subscription status to `PAST_DUE` + maybe downgrade quota.
    * `SUBSCRIPTION_CANCELED` → set `CANCELED` and downgrade quota.

---

## 4. Controllers

### 4.1 `BillingController` (user-facing)

Base path: `/api/v1/billing`.

* `POST /checkout`

  * Body: `{ planCode }`.
  * Flow:

    * Get current userId.
    * `PlanCatalogService.getPlanByCode(planCode)`.
    * `SubscriptionService.startSubscription(userId, planCode)` → returns `CheckoutSessionInfo`.
    * Response: `{ checkoutUrl, providerSessionId }`.
* `POST /cancel`

  * User-initiated cancel at period end:

    * `SubscriptionService.cancelAtPeriodEnd(userId)`.

### 4.2 `PaymentWebhookController` (provider → backend)

Base path: `/api/v1/billing/webhook/{provider}`.

* `POST /api/v1/billing/webhook/stripe`

  * Validate signature.
  * Parse into `PaymentWebhookEvent`.
  * Delegate to `PaymentWebhookHandlerService.handle(event)`.

---

## 5. Integration with Quota and Tier

### 5.1 Upgrade to PRO

Sequence:

1. User clicks “Upgrade to PRO”.
2. FE → `POST /api/v1/billing/checkout { planCode: "PRO_MONTHLY" }`.
3. `BillingController`:

   * Resolves plan.
   * Calls `SubscriptionService.startSubscription(userId, "PRO_MONTHLY")`:

     * Create `Subscription` row with:

       * `status = INCOMPLETE`
       * `billingPlan = PRO_MONTHLY`
       * `userId`
     * `PaymentOrchestrationService.createCheckoutSession(...)`:

       * Underlying provider creates session/checkout.
   * Return `checkoutUrl` to FE.
4. User completes payment on provider page.
5. Provider fires webhook `SUBSCRIPTION_CREATED` / `CHECKOUT_SESSION_COMPLETED`.
6. `PaymentWebhookController` → `PaymentWebhookHandlerService`:

   * Logs event in `PaymentProviderEventLog` (idempotency).
   * Maps to local user (via `customerId`/`metadata.userId`).
   * Calls `SubscriptionService.activateSubscriptionFromWebhook(...)`:

     * Find local `Subscription` by `providerSubscriptionId`.
     * Set:

       * `status = ACTIVE`
       * `currentPeriodStart`, `currentPeriodEnd`.
     * Call `QuotaManagementService.updateUserTier(userId, "PRO", newLimitFromPlan)`.
7. `QuotaManagementService.updateUserTier(...)`:

   * Load `UserTokenQuota` for user.
   * Set:

     * `tier = PRO`
     * `monthlyLimit = planLimits.monthlyTokenLimit`
     * optionally: if you want immediate reset on upgrade:

       * `currentMonthlyUsage = 0`
       * `resetDate = getNextMonthStart()` for PRO.

Policy decision:

* Common: upgrade applies **immediately**; quota increases right away, but reset date stays as per existing billing cycle OR aligned to provider’s `currentPeriodStart`.

### 5.2 Renewal

1. Provider charges automatically on new period.
2. Webhook: `INVOICE_PAYMENT_SUCCEEDED`.
3. Handler:

   * Find `Subscription` by `providerSubscriptionId`.
   * Update:

     * `currentPeriodStart`/`currentPeriodEnd`.
     * `status = ACTIVE`.
   * Option A (simple): keep your **existing monthly quota reset scheduler** independent.
   * Option B (aligned with provider):

     * On each renewal, call `QuotaManagementService.resetQuota()` or directly:

       * `currentMonthlyUsage = 0`
       * `resetDate = currentPeriodEnd`.
   * This keeps quota and billing cycles tightly aligned.

### 5.3 Cancellation

* User cancel from your UI:

  * `/api/v1/billing/cancel`.
  * `SubscriptionService.cancelAtPeriodEnd(userId)`:

    * Notify provider: `PaymentOrchestrationService.cancelProviderSubscription(providerSubscriptionId)` with “cancel_at_period_end”.
    * Locally mark `cancelAtPeriodEnd = true` on `Subscription`.
* Provider cancellation webhook:

  * `SUBSCRIPTION_CANCELED`.
  * Handler:

    * Update `Subscription.status = CANCELED`, `canceledAt`.
    * Downgrade:

      * `QuotaManagementService.updateUserTier(userId, "FREE", freeLimit)`.

---

## 6. Token/Quota + Subscription alignment patterns

### Option A – Independent quota scheduler (what you have)

* Quota reset scheduler uses `resetDate` to reset usage.
* Subscription updates only:

  * `tier`
  * `monthlyLimit`.
* Pros:

  * Simpler implementation (billing and quota not tightly coupled at dates).
* Cons:

  * Edge cases when subscription starts mid-month; user may get partial month.

### Option B – Subscription-driven quota reset (tighter coupling)

* On subscription `ACTIVE` and each `RENEWAL`:

  * Set `UserTokenQuota.resetDate = subscription.currentPeriodEnd`.
  * Set `currentMonthlyUsage = 0`.
* Pros:

  * Quota matches billing period exactly.
* Cons:

  * Need to treat FREE tier separately (no subscription).

You can start with **A** and later move to **B** if needed.

---

## 7. Error handling, idempotency, security

* **Idempotency**:

  * All webhook handlers must:

    * Check if `providerEventId` already processed in `PaymentProviderEventLog`.
    * If yes → return 200 but do nothing.
  * Subscription updates must be resilient to duplicate events.

* **Security**:

  * Webhook:

    * Verify signature using provider secret.
    * Use fixed URL; do not expose publicly in docs with secrets.
  * `BillingController`:

    * Only allow authenticated users.
  * `UsageAdminController` / tier changes:

    * Restrict via method security (`@PreAuthorize("hasRole('ADMIN')")`).

* **Transaction boundaries**:

  * Webhook handling:

    * Wrap event processing in a single transactional method:

      * Save `PaymentProviderEventLog`.
      * Update `Subscription`.
      * Update `UserTokenQuota`.
    * Commit or rollback as one unit.

---

## 8. Pros, cons, alternatives

* Pros:

  * Clear separation:

    * Domain: `Subscription`, `BillingPlan`, `UserTokenQuota`.
    * Services: `PlanCatalogService`, `SubscriptionService`, `PaymentOrchestrationService`, `PaymentWebhookHandlerService`, `QuotaManagementService`, `UsageReportingService`.
  * Payment provider is abstracted behind `PaymentProviderClient`.
  * Webhook is single source of truth for subscription state (industry standard).
  * Easy to plug in multiple providers if needed.

* Cons:

  * More classes and layers (façade + adapters).
  * Requires careful mapping between provider IDs and local users/subscriptions.
  * Need to design migration if you later change provider.

* Alternatives:

  * Use provider’s **customer portal** for billing (Stripe Billing portal etc.) and keep your API surface smaller.
  * Store plans only in provider, with minimal `BillingPlan` mirror (just `providerPlanId` + `AccountTier`).

---

If you want next, I can:

* Define concrete Java entities for:

  * `BillingPlan`
  * `Subscription`
  * `PaymentProviderEventLog`
* Then design the `SubscriptionService` interface + method signatures and how it calls `QuotaManagementService.updateUserTier(...)`.
