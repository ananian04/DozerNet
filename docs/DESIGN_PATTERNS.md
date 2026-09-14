# Design Patterns in DozerNet

The final evaluation requires at least one (preferably two) correctly applied design
patterns with clear justification. DozerNet uses the following.

## 1. Repository (DAO) pattern

**Where:** every module’s `*Repository` interfaces under
`com.dozernet.module*_*/repository` and `com.dozernet.common.*.*Repository`.

**Why:** separates persistence from business logic. Controllers and services never
talk to JDBC/SQL directly; they call typed repository methods
(e.g. `BookingRepository.findOverlapping(...)`).

**Benefit for viva:** easy to point at Spring Data JPA repositories as the
standard Repository/DAO implementation.

## 2. Strategy pattern — pricing

**Where:**
- `module6_payment/pricing/PricingStrategy` (interface)
- `StandardPricingStrategy` — days × daily rate
- `PricingSelector` — chooses the first applicable strategy, then layers on the
  district transport surcharge, 18% VAT and (for private machines) DozerNet's
  10% commission, returning a `PricingBreakdown`
- `TransportSurchargeCalculator` — deliberately *not* a strategy: the strategy
  decides the hire price, this adds haulage on top of whichever one applied

**Why:** invoice amounts can change without editing `PaymentService`. New
pricing rules (seasonal rates, long-hire discounts) are new strategy classes
with a lower `@Order`; nothing else changes.

**Used by:** `PaymentService.createInvoiceForApprovedBooking(...)` and
`generateInvoice(...)` via `PricingSelector.priceBreakdown(booking)`.

## 3. Observer pattern — notifications (Module 1 customer alerts)

**Ownership:** Customer Management (Module 1) owns the customer notification
inbox (`CustomerNotificationController`, `CustomerNotificationService`,
`templates/customer/notifications.html`). The shared Observer publisher stays in
`common.notification` so other modules can emit events without depending on Module 1.
Admin/owner/operator inboxes remain under `common.web.NotificationController`.

**Where:**
- `common/notification/NotificationObserver`
- `InAppNotificationObserver` — persists in-app alerts
- `EmailNotificationObserver` — logs email content to the console (demo-safe)
- `NotificationService` — subject that notifies all observers
- `module1_customer` — customer-facing inbox (list + mark-all-read)

**Why:** booking/payment/status changes should fan out to multiple channels
without the services knowing about UI or email details.

**Used by:** `BookingService`, `PaymentService`, `OperatorService`, `FleetService`, etc.

## 4. Policy object — cancellation refunds

**Where:** `module2_booking/CancellationPolicy`.

**Why:** the client's refund bands (full refund over 48 hours out, 20% charge
inside 48 hours, nothing once the hire has started) are a business rule, not a
payment mechanism. The policy decides *what fraction* comes back and module 6
moves the money, so each module owns one concern and the rule can be unit
tested against a fixed clock (`CancellationPolicyTest`).

**Used by:** `BookingService.cancelApproved(...)` &rarr;
`PaymentService.refundForCancellation(booking, fraction)`.

## 5. Factory / Spring IoC (supporting)

Spring’s application context acts as a factory for beans (services, strategies,
repositories). Strategies are injected as an ordered `List<PricingStrategy>` into
`PricingSelector`, which is a lightweight strategy-selection factory.

## Cross-cutting services in `common`

Two shared services are deliberately kept out of the six modules because every
module writes to them:

- **`common/audit`** — `AuditService.record(...)` appends to an append-only
  `AuditLog`. Booking approvals, listing approvals, operator verification,
  payments, refunds, maintenance and machine swaps all call it, and the admin
  reads the trail at `/admin/audit`.
- **`common/document`** — `DocumentService` stores NIC copies, licences,
  ownership proof and inspection photos against the account that uploaded them,
  so a customer who later registers as an owner reuses what is already on file.

---

For the viva: mention **Strategy** and **Observer** as the two primary applied
patterns, plus **Repository** as the persistence pattern used across all six modules.
