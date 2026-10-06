# E-commerce Order Management

A Java 17 / Spring Boot modular monolith for catalog, customer carts, multi-warehouse inventory, checkout, fulfillment, discounts, returns, and refunds. The API uses Spring MVC, Spring Data JPA, Spring Security, Bean Validation, and H2 by default, with a PostgreSQL runtime profile.

## Run Locally

Prerequisites: JDK 17 and Maven 3.9 or later.

```bash
mvn spring-boot:run
mvn test
```

The default H2 database is file-backed at `./data/orders`. The H2 console is available at `/h2-console` (JDBC URL `jdbc:h2:file:./data/orders`, user `sa`, blank password). For an in-memory test database, use the test profile through `mvn test`.

For PostgreSQL, provide `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`, then run with `--spring.profiles.active=postgres`. Schema generation uses Hibernate `update` for this assignment; a production deployment should use versioned Flyway/Liquibase migrations.

The app seeds two sample products, two warehouses, and stock on first start. Demo accounts use HTTP Basic:

| Role | Username | Default password |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Customer | `customer` | `customer123` |
| Warehouse staff | `warehouse` | `warehouse123` |

Set `ADMIN_PASSWORD`, `CUSTOMER_PASSWORD`, and `WAREHOUSE_PASSWORD` to override these values. Demo users are in-memory and intended only for local evaluation.

## Architecture

The code is organized by business capability (`catalog`, `cart`, `inventory`, `orders`, `payment`, `promotion`, `returns`, and `events`). Controllers own HTTP mapping and role checks; services own use cases and transaction boundaries; Spring Data repositories isolate persistence. `PaymentGateway` and `TaxCalculator` are interfaces so integrations and tax policy can be replaced without rewriting checkout.

Checkout locks the customer's cart and then locks each product's active warehouse-stock rows in a stable product-ID / warehouse-ID order. It confirms aggregate availability before allocating units, increments `reserved`, captures payment through the adapter, creates a price-snapshotted order and payment record, then clears the cart in one database transaction. Any validation, stock, or simulated payment failure rolls the database transaction back. Inventory allocation spans warehouses in stable warehouse-ID order; an order line retains its exact allocations. Shipping and return approval lock the relevant stock rows in ID order as well, with a version column as a lost-update backstop. Shipping converts reservations into shipped stock, while an approved return restores those units and refunds the captured amount.

The fulfillment state machine accepts only `PLACED -> CONFIRMED -> PACKED -> SHIPPED -> DELIVERED -> RETURNED`. Warehouse staff can request a transition, but invalid jumps are rejected by the domain enum. Customer order queries are scoped to the authenticated username.

Checkout publishes domain events that run asynchronously only after commit. The listener persists fulfillment-routing, notification, and audit records; an admin can inspect them at `GET /api/admin/orders/{orderId}/operations`. This is deliberately an in-process, bounded-executor pipeline. Events can be lost if the process stops after checkout commits but before async work completes; a durable outbox and retry policy are production follow-ups, outside this single-process assignment scope.

## Assumptions and Scope

- Prices use USD; currency conversion and multiple currencies are not modeled.
- Tax is a configurable flat rate (`app.tax-rate`, default `0.08`), calculated on the post-discount item subtotal. Shipping fees and jurisdiction-specific tax rules are omitted.
- A checkout uses at most one global discount code. Discounts may be percentage or fixed amount, can expire, and may have a global redemption limit. Discount use is locked and committed with checkout.
- Inventory quantities represent physical on-hand units. `available = onHand - reserved`; admin adjustments cannot set on-hand below reservations. A reservation remains until shipment or an approved return. Cancellation, reservation expiry, backorders, and partial shipment are not modeled.
- Returns are full-order returns requested after delivery. An admin approves or declines; approval performs a full refund and restocks the originally allocated warehouse quantities. Partial returns and return windows are omitted.
- Payment is represented by a `PaymentGateway` port and a local simulator. Any token except `decline` is approved. The simulator does not move money. A real provider call cannot be made atomic with a relational database transaction; production use needs idempotency keys and a durable payment/outbox workflow to resolve provider/database failures.
- Product and order quantities use a single base unit; bundles, variants, and catalog media are omitted.
- Roles and demo identities are in-memory, with HTTP Basic for assessment simplicity. Customer accounts, password reset, OAuth, and persistent identity management are out of scope.
- Async downstream operations run after the checkout response can complete, and their records are persisted separately. The in-memory executor is not a durable queue.

These choices keep the evaluated flows locally runnable while leaving clear adapter and policy seams for production additions.

## REST API

All routes require HTTP Basic authentication. `/api/catalog/**` is available to authenticated roles; other access is role-restricted.

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/catalog/categories` | Any authenticated role | Browse categories |
| `GET` | `/api/catalog/products?categoryId={id}` | Any authenticated role | Browse products |
| `GET` | `/api/catalog/products/{id}` | Any authenticated role | Product details |
| `POST` | `/api/admin/categories` | Admin | Create category |
| `PUT` | `/api/admin/categories/{id}` | Admin | Update category |
| `POST` | `/api/admin/products` | Admin | Create product |
| `PUT`, `DELETE` | `/api/admin/products/{id}` | Admin | Update or deactivate product |
| `POST` | `/api/admin/warehouses` | Admin | Create warehouse |
| `PUT` | `/api/admin/inventory/products/{productId}/warehouses/{warehouseId}` | Admin | Set on-hand stock |
| `POST` | `/api/admin/discounts` | Admin | Create discount |
| `PUT` | `/api/admin/discounts/{id}/deactivate` | Admin | Deactivate discount |
| `GET`, `POST` | `/api/cart`, `/api/cart/items` | Customer | Read cart and add product quantity |
| `DELETE` | `/api/cart/items/{productId}` | Customer | Remove cart line |
| `POST` | `/api/orders/checkout` | Customer | Validate cart, reserve stock, charge test payment, create order |
| `GET` | `/api/orders`, `/api/orders/{orderId}` | Customer | List or track own orders |
| `POST` | `/api/orders/{orderId}/returns` | Customer | Request a full return |
| `GET` | `/api/warehouse/warehouses` | Warehouse | List warehouses |
| `GET` | `/api/warehouse/warehouses/{warehouseId}/stock` | Warehouse | Inspect warehouse inventory |
| `PATCH` | `/api/warehouse/orders/{orderId}/status` | Warehouse | Advance fulfillment state (JSON enum string) |
| `PATCH` | `/api/admin/returns/{returnId}` | Admin | Approve or decline a return |
| `GET` | `/api/admin/orders/{orderId}/operations` | Admin | Read async audit/notification/routing records |

Example checkout:

```bash
curl -u customer:customer123 -H 'Content-Type: application/json' \
  -d '{"shippingAddress":"12 Market Street","discountCode":"WELCOME10","paymentToken":"test-card"}' \
  http://localhost:8080/api/orders/checkout
```

Use `"paymentToken":"decline"` to exercise the failure path. Validation/business-rule errors return a structured `400`, missing resources return `404`, duplicate unique values return `409`, and role failures return `403`.

## Tests

`OrderFlowIntegrationTest` exercises authenticated HTTP flows, payment-decline rollback, reservation across two warehouses, lifecycle progression, return approval, refund status, and inventory restoration. `OrderStatusTest` covers allowed and rejected domain transitions. Run all tests with `mvn test`.

## Development Artifacts

- `AGENTS.md`: repository-specific implementation and review guidance.
- `.github/skills/order-management/SKILL.md`: domain skill used to keep future changes aligned with the order/inventory invariants.
- `docs/video-outline.md`: concise recording outline for the assignment walkthrough.

