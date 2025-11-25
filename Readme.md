## Payment and Plan Subscription Module

https://chatgpt.com/c/6925ac4f-ae00-8327-977b-bdb674a70b00

Key components required for a scalable, industry-standard subscription system for your **DocuHelper** project.

## Core Modules

* **Product & Plans Module**

  * Define plans (Free, Pro, Enterprise).
  * Store billing cycles (monthly, yearly).
  * Define limits: credits, features, rate limits.

* **Subscription Module**

  * Tracks active subscription per user.
  * Fields: `planId`, `status`, `startDate`, `endDate`, `autoRenew`, `cancelAtPeriodEnd`.
  * Handles upgrade/downgrade paths.

* **Payment Provider Module**

  * Integrates Stripe/Razorpay/PayPal.
  * Handles payment sessions, webhooks, invoices, refunds.

* **Entitlement Module**

  * Checks if the user is allowed to perform an action.
  * Connects subscription → features → enforcement logic.

* **Usage Tracking Module**

  * Tracks credits, tokens, API calls, workflow runs.
  * Required for fair use and advanced plans.

* **Webhook Processor**

  * Listens to payment provider events:

    * `payment_success`
    * `payment_failed`
    * `subscription_created`
    * `subscription_renewed`
    * `subscription_cancelled`

## Backend Architecture (Spring Boot)

* **Entities**

  * `Plan`
  * `Subscription`
  * `Transaction`
  * `PaymentProviderEvent`
  * `UsageRecord`

* **Services**

  * `SubscriptionService` (create/upgrade/downgrade)
  * `PaymentService` (initiate payment session)
  * `WebhookHandlerService` (handle payment provider callbacks)
  * `UsageService` (track usage)
  * `EntitlementService` (validate limits)

* **Controllers**

  * `/billing/checkout`
  * `/billing/subscriptions`
  * `/billing/webhook`
  * `/billing/usage`

## Key Workflows

### 1. User Upgrades to Pro

* User clicks “Upgrade to Pro”.
* Backend requests payment provider to create a checkout session.
* Redirect user to hosted payment page.
* Payment provider sends a webhook after success.
* Webhook handler:

  * Marks subscription active.
  * Updates end date.
  * Grants entitlements.
  * Logs transaction.

### 2. Subscription Renewal

* Provider charges user automatically.
* Sends `invoice.payment_succeeded`.
* Webhook:

  * Extends subscription period.
  * Refreshes usage limits (monthly credit reset).
  * Sends “renewal successful” event.

### 3. Cancel Subscription

* User clicks "Cancel".
* Mark `cancelAtPeriodEnd = true`.
* Subscription remains active till period end.

### 4. Usage Enforcement

* Each feature call (chat, document parsing, RAG retrieval) checks:

  * Does user have a valid plan?
  * Is usage below monthly limit?
* If limit exceeded:

  * Block API call.
  * Return `402 Subscription Required`.

## Data Model Overview (concise)

### `Plan`

* `id`
* `name`
* `price`
* `billingCycle`
* `limitsJson`

### `Subscription`

* `id`
* `userId`
* `planId`
* `status`
* `startAt`
* `endAt`
* `cancelAtPeriodEnd`

### `Transaction`

* `id`
* `userId`
* `amount`
* `status`
* `providerReferenceId`

## Industry Best Practices

* Use **webhooks as source of truth** instead of client callbacks.
* Do not trust client-side about payment success.
* Use **idempotency keys** when updating subscriptions.
* Use **grace periods** (1–2 days) for failed renewals.
* Store raw webhook payloads for audit and debugging.
* All billing logic must be **server-side only**.
* Always support **prorated upgrades** if required.

## Pros

* Scalable and follows SaaS standards.
* Works with any payment provider.
* Clean separation of subscription, billing, and entitlements.
* Supports future features (credits, usage analytics, RAG token limits).

## Cons

* Requires setup of webhooks.
* Requires SSL + public URL for testing webhooks.
* Billing logic must consider race conditions and retries.
* More tables and services to maintain.

## Alternatives

* Use **Stripe Billing** → easiest, most complete.
* Use **Razorpay Subscriptions** → best for India.
* Use **Paddle** → no GST handling required; global.
* Use **LemonSqueezy** → simple for indie devs.
* Use **self-built payment flow** (not recommended).

If you want, I can also:

* Provide **full database schema**.
* Provide **Spring Boot code templates** for each module.
* Provide **Flutter UI flow** for handling checkout.
* Provide **upgrade logic** with prorating.
