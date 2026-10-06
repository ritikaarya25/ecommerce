# Repository Agent Guidance

## Project shape
- Maintain a Spring Boot modular monolith organized by business capability under `com.acme.orders`.
- Keep HTTP concerns in `api`, use cases and transaction boundaries in services, persistence details in repositories, and domain invariants close to domain types.
- Prefer constructor injection and narrow interfaces for replaceable policies/integrations (for example `PaymentGateway` and `TaxCalculator`).

## Invariants
- Checkout must run in one database transaction and must not clear a cart, capture recorded payment, or retain inventory reservations on a failed attempt.
- Lock cart state and inventory rows; acquire stock locks in stable product/warehouse order. Availability is `onHand - reserved` and must never be negative.
- Keep order price snapshots and exact warehouse reservation allocations; use those allocations during ship/return accounting.
- Validate order transitions through `OrderStatus`; do not duplicate a looser transition graph in controllers.
- Keep customer order access scoped to the authenticated customer and keep admin/warehouse operations role-protected.
- Trigger downstream side effects after commit. Keep external-payment limitations and asynchronous delivery guarantees documented.

## Change and verification practices
- Add focused unit tests for domain rules and integration tests for transactional or authorization behavior.
- Run `mvn test` before submitting. Use Java 17 or newer and Maven 3.9 or newer.
- Update `README.md` when changing an assumption, API, role, persistence profile, or local workflow.
- Do not add deployment, frontend, CI, or distributed infrastructure unless the assignment scope changes.