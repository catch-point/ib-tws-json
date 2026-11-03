# Changelog

All notable changes to the JSON interface of ib-tws-json. Commands are the
lines a client sends (`EClient` methods and this project's own actions), and
events are the lines it receives (`EWrapper` callbacks and this project's own
replies). See [README.md](README.md#json-api) for the message format.

This file follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and
the project uses [Semantic Versioning](https://semver.org/).

## [1.1.2]

Built with TWS API 10.37.02. No changes to the JSON interface.

### Other changes

- The build uses the Gradle 8.13 wrapper (`./gradlew`) in place of a system
  Gradle 4, and `build.sh` runs it from any working directory.
- Java 17 is used to build, selected by a Java toolchain and
  `gradle/gradle-daemon-jvm.properties`, matching the JRE bundled with TWS. If
  JDK 17 is not installed, Gradle downloads one.

## [1.1.1]

Built with TWS API 10.37.02. No changes to the JSON interface.

## [1.1.0]

Built with TWS API 10.37.02. Upgraded AspectJ so the extension launches. No
changes to the JSON interface.

## [1.0.1]

Upgraded from TWS API 10.19.01 to **10.37.02**. Although the version number
only changed in its last digit, this release contained **breaking changes** to
command and event signatures. Clients written for 1.0.0 need the changes
below.

### Changed (breaking)

Parameters are positional, so a changed parameter list changes the meaning of
every value after it.

- **Commands:**

  | Command | 1.0.0 | 1.0.1 and later |
  |---------|-------|-----------------|
  | `cancelOrder` | `(int orderId, String manualOrderCancelTime)` | `(int orderId, OrderCancel orderCancel)` |
  | `reqGlobalCancel` | `()` | `(OrderCancel orderCancel)` |
  | `exerciseOptions` | `(int tickerId, Contract contract, int exerciseAction, int exerciseQuantity, String account, int override)` | `(…same six…, String manualOrderTime, String customerAccount, boolean professionalCustomer)` |

  - `cancelOrder 5 ""` is now rejected (`Expected "" to be an object`). Send
    the time in an object instead: `cancelOrder 5 {"manualOrderCancelTime":""}`.
    `OrderCancel` has the fields `manualOrderCancelTime` (String),
    `extOperator` (String) and `manualOrderIndicator` (int).
  - `reqGlobalCancel` with no value fails when connected, because the TWS API
    reads the missing `OrderCancel`. Send `reqGlobalCancel {}`.
  - `exerciseOptions` with the old six values waits for the three new values,
    so **the next command line is read as its continuation** and fails
    (usually with `Expected JSON value`). Add the three values, for example
    `… "" "" false`.
- **Events:**

  | Event | 1.0.0 | 1.0.1 and later |
  |-------|-------|-----------------|
  | `error` | `(int id, int errorCode, String errorMsg, String advancedOrderRejectJson)` | `(int id, long errorTime, int errorCode, String errorMsg, String advancedOrderRejectJson)` |
  | `orderStatus` | `permId` (6th value) is an `int` | `permId` is a `long` |
  | `commissionReport(CommissionReport)` | | renamed `commissionAndFeesReport(CommissionAndFeesReport)` |

  - `error` has a new second value, the error time in milliseconds, so the
    error code and message moved one position to the right.
  - `long` values are written as JSON strings, so `orderStatus`'s `permId`
    changed from a JSON number to a string (for example `"1234567890"`).
  - In the renamed report, the `commission` field is now `commissionAndFees`.
- **`FADataType`** no longer has `PROFILES`. `requestFA "PROFILES"` and
  `replaceFA … "PROFILES" …` are rejected.
- **`Order.faProfile`** no longer exists. Unknown fields in an object are
  ignored without an error, so an order sent with `faProfile` is placed
  **without** its FA profile.

### Removed

- This project's own `faMsgTypeName` command and event.

### Added

- Event `currentTimeInMillis(long)`.

## [1.0.0]

Built with TWS API 10.19.01.
