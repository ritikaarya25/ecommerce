---
name: order-management-domain
description: Preserve order, payment, and multi-warehouse inventory invariants when extending this Spring Boot application.
---

# Order Management Domain Skill

Use this skill when changing catalog, cart, inventory, checkout, payment, fulfillment, discounts, or returns.

## Required reasoning
- Identify which use case owns the transaction; checkout and return approval coordinate multiple repositories and must remain atomic at the database boundary.
- For inventory changes, state how `onHand`, `reserved`, and `available` change on checkout, shipment, and return. Lock rows for decisions based on available stock.
- Keep all stock locks ordered consistently to reduce deadlocks. Allocate by warehouse ID and preserve exact allocations on the order.
- Treat order status as a state machine and test rejected transitions, not only the happy path.
- Keep order prices as snapshots. Document how tax, discount, refund, and partial/whole return amounts are calculated.
- Route payment and tax policy through interfaces; do not pretend a remote payment provider participates atomically in a local database transaction.
- Publish customer notification, audit, and routing work after commit. State whether delivery is durable or best-effort.
- Enforce role authorization at the HTTP boundary and ownership at the service boundary.

## Validation checklist
- Add or update a focused unit test for domain decisions.
- Add or update an integration test for transaction rollback, concurrent inventory behavior, or security boundaries when relevant.
- Validate input and return consistent API errors.
- Update README assumptions and API documentation when behavior changes.