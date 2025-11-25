Answering each point with concrete design patterns and how they fit your existing quota/subscription module.

---

## 1. Price changes for same “plan name” (grandfathered users)

Goal:

* User A: PRO at X (old price).
* New users: PRO at X-100 (discounted).
* No loss of consistency in history.

### Design

* Distinguish between:

  * **Product** = logical plan (PRO, PROFESSIONAL).
  * **Price** = specific amount + billing period.

* Entities:

  * `BillingProduct`

    * `id`
    * `code` (`PRO`, `PROFESSIONAL`)
    * `displayName`
    * `defaultTier` (`AccountTier.PRO`)
    * `active` (boolean)

  * `BillingPrice`

    * `id`
    * `product` (FK → `BillingProduct`)
    * `version` (int, e.g., 1, 2,…)
    * `billingPeriod` (`MONTHLY`, `YEARLY`)
    * `amount`
    * `currency`
    * `providerPriceId`
    * `active` (boolean)
    * **Business rule**: never update `amount` once used; create new row instead.

  * `Subscription`

    * References **`BillingPrice`**, not just product.
    * This “locks in” the exact price for that user.

### How it works

* When you “discount” PRO:

  * Create new `BillingPrice` row for PRO with `amount = X-100`, `version = 2`.
  * Mark old `BillingPrice` as `active=false` (for new signups).
  * Existing subscriptions keep pointing to old `BillingPrice` (`version 1`), so:

    * Their renewals stay at price X.
    * Analytics can still distinguish old vs new pricing.

* Pros:

  * Perfect consistency for existing subscriptions.
  * Full pricing history.

* Cons:

  * More rows in `BillingPrice`.

* Alternative:

  * Store `priceVersion` directly on `Subscription` and keep only one product-level row.

---

## 2. Failure, cancel, autopay, upgrade, downgrade

### 2.1 States

Define subscription status state machine:

* `INCOMPLETE`
* `ACTIVE`
* `PAST_DUE` (payment failed, grace period)
* `CANCELED`
* `EXPIRED` (fixed-term campaigns)
* `TRIALING` (if you add free trials)

### 2.2 Payment failure

* Source of truth: webhook `INVOICE_PAYMENT_FAILED` / `PAYMENT_FAILED`.

* Logic:

  * Set `Subscription.status = PAST_DUE`.
  * Option A:

    * Keep quota active during small grace period (e.g., 3 days).
  * Option B:

    * Immediately downgrade quota to FREE (hard).

* LLD:

  * `SubscriptionService.handlePaymentFailure(providerSubscriptionId, eventData)`:

    * Load `Subscription`.
    * Update status to `PAST_DUE`.
    * Possibly set `gracePeriodUntil`.
    * Optionally call `QuotaManagementService.updateUserTier(userId, FREE, freeLimit)` when grace ends (via scheduler).

### 2.3 Cancel like Netflix (“cancel next month”)

* UI: user clicks “Cancel”.
* Backend:

  * `SubscriptionService.cancelAtPeriodEnd(userId)`:

    * Set `cancelAtPeriodEnd = true` locally.
    * Call provider to set `cancel_at_period_end = true`.
* When provider actually cancels at end of period:

  * Webhook `SUBSCRIPTION_CANCELED`:

    * Set `status = CANCELED`, `canceledAt = now`.
    * Downgrade:

      * `QuotaManagementService.updateUserTier(userId, FREE, freeLimit)`.

### 2.4 Autopay

* Autopay is entirely provider-driven:

  * Your backend never “pulls” money; it only reacts to:

    * `INVOICE_PAYMENT_SUCCEEDED` (renewal success).
    * `INVOICE_PAYMENT_FAILED` (failure).
* On success:

  * Extend `currentPeriodEnd`.
  * Optionally reset quota (`resetDate`, `currentMonthlyUsage`) to match new period.

### 2.5 Upgrade (PRO → PROFESSIONAL)

* Flow:

  * UI:

    * user selects PROFESSIONAL.
    * Call `POST /billing/checkout` with new product/price.

  * Backend:

    * `SubscriptionService.startUpgrade(userId, newPriceId)`:

      * Option A: use provider’s built-in **proration**.
      * Option B: end current subscription at period end, start new one.

  * Webhook:

    * `SUBSCRIPTION_UPDATED` or `INVOICE_PAYMENT_SUCCEEDED`:

      * Detect upgrade type.
      * Update `Subscription.billingPrice` to new `BillingPrice`.
      * Update quota:

        * `QuotaManagementService.updateUserTier(userId, PROFESSIONAL, professionalLimit)`.
        * Optionally reset or boost current monthly limit.

* Policy:

  * Most SaaS: upgrade is immediate (user gets new higher quota instantly).

### 2.6 Downgrade (PROFESSIONAL → PRO)

* Common approach:

  * Effective only at **next billing period**.

* Flow:

  * UI:

    * user selects PRO.
    * `SubscriptionService.scheduleDowngrade(userId, newPriceId)`:

      * Store `pendingPriceId` in `Subscription` (or `nextBillingPrice`).
  * On next renewal event:

    * Switch `billingPrice` from PROFESSIONAL to PRO.
    * Update quota for next cycle.

* Pros:

  * User gets full value of high tier till period end.

* Cons:

  * More state to track (`pendingPriceId`).

---

## 3. Refund processing

### Why you may need refunds

* Payment provider errors or duplicate charges.
* Technical issue: user charged but could not use service (e.g., quota misconfiguration).
* Regulatory or policy reasons (chargeback mitigation, consumer complaints).

### Design

* Add `Refund` model or handle via `PaymentTransaction`:

  * Option A – dedicated `Refund`:

    * `Refund`

      * `id`
      * `paymentTransaction` (FK)
      * `providerRefundId`
      * `amount`
      * `currency`
      * `status` (`REQUESTED`, `PROCESSING`, `SUCCEEDED`, `FAILED`)
      * `reason`
      * `createdAt`, `updatedAt`

  * Option B – reuse `PaymentTransaction` with `type = REFUND`.

### Flow

* Manual refund:

  * Admin or support portal → `RefundService.requestRefund(paymentTransactionId, amount, reason)`.
  * `RefundService`:

    * Call provider’s refund API.
    * Mark `Refund` as `PROCESSING`.
  * Webhook from provider:

    * `REFUND_SUCCEEDED`:

      * Mark refund as `SUCCEEDED`.
      * Optionally:

        * Cancel subscription or shorten period.
        * Adjust quota if refund is full-period refund.

* Automatic refund:

  * Policy for edge cases (e.g., payment succeeded but subscription activation failed):

    * On detection, trigger refund and cancel subscription.

### Interaction with quota

* Full-period refund:

  * Most consistent:

    * Cancel subscription & downgrade quota to FREE.
* Partial refund:

  * Usually keep subscription active (goodwill/partial compensation).

---

## 4. Reporting and analytics

### Data sources

* `Subscription`:

  * Active subs, churn, upgrades/downgrades.
* `BillingProduct` / `BillingPrice`:

  * Plan mix (which tiers generate revenue).
* `PaymentTransaction` (+ optional `Refund`):

  * Revenue, refunds, net revenue.
* `UserTokenUsage`, `UserTokenQuota`:

  * Usage vs quota per plan.

### Typical reports

* Revenue:

  * MRR, ARR = aggregate `PaymentTransaction` by `type = INITIAL/RENEWAL`.
* Churn:

  * Count of `Subscription.status` transitions to `CANCELED` per period.
* Plan distribution:

  * # users on FREE/PRO/PROFESSIONAL.
* Usage:

  * Tokens used per tier, per price version, per period.
* Campaign performance:

  * New signups for `BillingPrice` version used in a campaign period.

### LLD patterns

* Option A – query-only on OLTP DB:

  * Build repository methods:

    * `findRevenueByMonth(...)`
    * `findChurnByMonth(...)`
  * Use Spring Data projections / custom queries.
* Option B – pre-aggregated tables:

  * Nightly job:

    * Reads from `Subscription`/`PaymentTransaction`/`UserTokenUsage`.
    * Writes to `BillingAnalyticsDaily`:

      * `date`
      * `newSubscriptions`
      * `cancelledSubscriptions`
      * `mrr`
      * `tokensUsedByTier`, etc.
* Option C – export to external analytics (BigQuery, ClickHouse, etc).

---

## 5. Short-term plans / campaigns

Goal:

* Run “Black Friday – PRO at X-200 for first month only” or “3 months at 50% off”.

### Design

* `Promotion` entity:

  * `Promotion`

    * `id`
    * `code` (e.g. `BF2025`, `LAUNCH50`)
    * `discountType` (`PERCENT`, `FIXED_AMOUNT`)
    * `discountValue`
    * `appliesToProduct` (FK `BillingProduct` or `ALL`)
    * `appliesToBillingPeriod` (optional)
    * `startsAt`
    * `endsAt`
    * `maxRedemptions`
    * `perUserLimit` (e.g., 1)
    * `active` (boolean)

* `SubscriptionPromotion` (link table):

  * `id`
  * `subscription` (FK)
  * `promotion` (FK)
  * `appliedAt`
  * `appliesToPeriodStart`, `appliesToPeriodEnd` (if limited to first N periods)

### Application logic

* During checkout:

  * FE sends `planCode` + optional `promotionCode`.
  * `PlanCatalogService`:

    * Validates that `Promotion` is active, within date range, and applicable to product.
  * Price computation:

    * Base = `BillingPrice.amount`.
    * Discounted = `base - discount` (respecting min 0).
  * Provider:

    * Either:

      * Use provider’s coupon system (Stripe/Razorpay coupon).
      * Or create temporary provider price with discounted amount and link `Promotion`.

* Subscription:

  * Store:

    * `billingPrice` (base price).
    * `SubscriptionPromotion` for the campaign.
  * Webhook on renewal:

    * If promotion was “first month only”, do not re-apply discount for next period.

### Short-term plan vs regular plan

* You can either:

  * Create separate `BillingPrice` for campaign, or
  * Use `Promotion` + same base `BillingPrice`.

* Recommended:

  * Use `Promotion` for discount logic.
  * Keep `BillingPrice` as base catalog price.

---

## Summary mapping to your existing modules

* Quota:

  * `QuotaManagementService.updateUserTier(...)` called on:

    * Subscription activation (upgrade).
    * Renewal (if resetting quota per billing).
    * Cancellation / failure (downgrade to FREE).

* Usage:

  * `UsageRecordingService` unchanged; just respects updated `monthlyLimit`.

* New entities/services:

  * `BillingProduct`, `BillingPrice`, `Subscription`, `PaymentTransaction`, `PaymentProviderEventLog`, `Promotion`, `SubscriptionPromotion`.
  * `SubscriptionService`, `PlanCatalogService`, `PaymentOrchestrationService`, `PaymentProviderClient`, `PaymentWebhookHandlerService`.

If you want next, I can define the **Subscription + BillingProduct + BillingPrice entities and enums** in Java tailored to your existing `AccountTier` and YAML `monetization.plans` layout.
