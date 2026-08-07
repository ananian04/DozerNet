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
- `PricingSelector` — chooses the first applicable strategy

**Why:** invoice amounts can change without editing `PaymentService`. New
pricing rules are new strategy classes.

**Used by:** `PaymentService.generateInvoice(...)` via `PricingSelector.price(booking)`.

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

## 4. Factory / Spring IoC (supporting)

Spring’s application context acts as a factory for beans (services, strategies,
repositories). Strategies are injected as an ordered `List<PricingStrategy>` into
`PricingSelector`, which is a lightweight strategy-selection factory.

---

For the viva: mention **Strategy** and **Observer** as the two primary applied
patterns, plus **Repository** as the persistence pattern used across all six modules.
