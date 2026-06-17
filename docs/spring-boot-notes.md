# Spring Boot Project Structure Notes

## Big Picture

A Spring Boot app is usually separated into layers. Each layer has a different job.

```text
Browser / User
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

## Controller

The controller handles web requests.

Its job is to:

* Receive requests from the browser or API
* Read request parameters
* Call services or repositories
* Add data to the `Model`
* Return the page name or API response

Example:

```java
@GetMapping("/transactions/compare")
public String compareTransactions(...) {
    model.addAttribute("hasComparison", true);
    return "transaction-compare";
}
```

The controller should not contain too much business logic.

## Service

The service handles business logic.

Its job is to:

* Process data
* Calculate results
* Apply app rules
* Build insights
* Handle important app behavior

Examples in SpendSense:

* Parse bank statements
* Detect duplicate uploads
* Compare statement cycles
* Build key insight text
* Later: call AI to summarize spending

A service keeps the controller cleaner.

## Repository

The repository talks to the database.

Its job is to:

* Fetch data
* Save data
* Run database queries

Example:

```java
transactionRepository.findByStatementImport_Id(cycleId);
```

The repository should focus on database access, not business meaning.

## Thymeleaf / HTML

Thymeleaf displays data from the controller.

Example:

```html
<div th:text="${cycleBTotalDollars}"></div>
```

The controller sends `cycleBTotalDollars` to the page using:

```java
model.addAttribute("cycleBTotalDollars", value);
```

Thymeleaf then replaces the HTML with the real value.

## Simple Rule

Ask this when deciding where code belongs:

```text
Is it about routing/page requests?
→ Controller

Is it about calculations, rules, or business meaning?
→ Service

Is it about database fetching/saving?
→ Repository

Is it about display?
→ HTML / Thymeleaf
```

## SpendSense Example

For the comparison page:

```text
Browser opens /transactions/compare
    ↓
TransactionCompareController receives cycleAId and cycleBId
    ↓
Controller gets cycle and transaction data
    ↓
Comparison logic calculates totals and differences
    ↓
ComparisonInsightService builds readable insight sentences
    ↓
Controller sends data to Thymeleaf
    ↓
transaction-compare.html displays the page
```

## Why Services Matter

Services make the app easier to maintain.

Instead of putting all logic in the controller, we move reusable logic into services.

This helps because:

* Controllers stay smaller
* Business logic is easier to test
* Future AI features can be added cleanly
* Code is easier to understand later

For AI features, the controller should not call AI directly. A better structure is:

```text
TransactionCompareController
    ↓
ComparisonInsightService
    ↓
AiInsightService
```

The controller should only call the service and display the result.
