# Changelog

All notable changes to the JSON interface of ib-tws-json. Commands are the
lines a client sends (`EClient` methods and this project's own actions), and
events are the lines it receives (`EWrapper` callbacks and this project's own
replies). See [README.md](README.md#json-api) for the message format.

This file follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and
the project uses [Semantic Versioning](https://semver.org/).

## [2.1.0]

Built with TWS API 10.50.02.

### Added

- **Every version of an overloaded command can be used.** Before 2.1.0, only
  one version of each overloaded name was available, chosen by the order in
  which Java's reflection listed the methods. The version is now chosen for
  each call from the number and JSON types of the values given:
  - `requestFA 1` or `requestFA "GROUPS"`
  - `replaceFA 1000 1 "<xml>"` or `replaceFA 1000 "GROUPS" "<xml>"`
- **Enums accept numbers.** An enum parameter or field accepts its TWS API
  numeric code as well as its name, as a number or a numeric string. The code
  comes from the enum's `id()`, `val()`, `value()` or `code()` method (for
  example `FADataType`: GROUPS is `1`, ALIASES is `3`; `TriggerMethod`:
  LastOrBidAsk is `7`); enums without one use their position. A number with no
  matching constant is reported as an `error` event. Where a command also has
  an `int` version, such as `requestFA`, a number uses that version and is
  passed to TWS unchanged.

### Changed

- **`help` for overloaded names** now always describes the same version, in
  the same output format:
  - for a command, the version with the most parameters, preferring enum
    parameters over `int` ones. `help "requestFA"` now shows `FADataType`
    instead of `int` (both are accepted).
  - for an event, the version with the most parameters. `help "error"` now
    shows `error(int, long, int, String, String)` instead of `error(String)`.
    The `error` events themselves are unchanged.
- **An incomplete overloaded command waits for more input.** If no version of
  a command takes exactly the values given and a version needs more number or
  boolean values, the command now continues on the next line, as other
  commands already did. For example, `replaceFA 1000` now waits for the rest,
  where 2.0.0 called the enum version with `null` values.

### Removed

- **The TWS API's `*ProtoBuf` commands and events** (such as
  `placeOrderProtoBuf` and `orderStatusProtoBuf`). The commands could not be
  used, because their protobuf message parameter cannot be built from JSON, and
  the events had no usable content and duplicated the regular events that
  follow them. Sending one of these commands now gets `error "<command>?"`.
  IB's configuration API (`reqConfigProtoBuf`, `updateConfigProtoBuf`) has no
  regular equivalent and is not available.

### Other changes

These do not change the JSON interface.

- New [ARCHITECTURE.md](ARCHITECTURE.md) describing the design, the tests and
  how to upgrade to a new TWS API release, and an updated README with the
  message format, an example session, a security note and all command-line
  options.
- New tests: `TestOrderCalls` replays `orders-calls.json`, and
  `TestOverloads` covers overloads and enum codes.
- The fixture generator passes `FADataType.id()` instead of IBKR's sample's
  `ordinal()`, which no longer matches the TWS API code, and uses a fixed date,
  so regenerating the fixtures gives the same output every time.

## [2.0.0]

Built with **TWS API 10.50.02**, up from 10.37.02 in 1.2.0. Commands, events
and types come from the TWS API by reflection, so these changes come from
IBKR's own API changes.

No command or event that exists in both 1.2.0 and 2.0.0 changed its
parameters. **Upgrading from 1.0.0?** Version 1.0.1 already contained breaking
command and event signature changes (TWS API 10.19 to 10.37); see
[1.0.1](#101) below.

### Removed

These commands and events no longer exist. A client that sends a removed
command gets `error "<command>?"`.

- **Fundamental data** (removed from the TWS API by IBKR):
  - command `reqFundamentalData(int, Contract, String, [TagValue])`
  - command `cancelFundamentalData(int)`
  - event `fundamentalData(int, String)`
- **Connection redirects** (removed from the TWS API by IBKR):
  - command `allowRedirect(boolean)`
  - command `redirect(String)`

### Added

- **Commands** (new in the TWS API):
  - `cancelContractData(int)`: cancels a `reqContractDetails` request
  - `cancelHistoricalTicks(int)`: cancels a `reqHistoricalTicks` request
- **`Order` fields** (new in the TWS API): `allowPreOpen` (boolean),
  `conditionsIncludeOvernight` (boolean), `deactivate` (boolean),
  `hedgeMaxSize` (int), `ignoreOpenAuction` (boolean), `postOnly` (boolean),
  `ptOrderId` (int), `ptOrderType` (String), `seekPriceImprovement` (Boolean),
  `slOrderId` (int), `slOrderType` (String), `whatIfType` (int)
- **`ContractDetails` fields** (new in the TWS API): `eventContract1`,
  `eventContractDescription1`, `eventContractDescription2`, `settlementMethod`
  (String), and `lastPricePrecision`, `lastSizePrecision`, `minAlgoSize`
  (Decimal, written as strings)

### Changed

- **`Order.routeMarketableToBbo`** is now a `Boolean` that can be `null`
  (unset), instead of a `boolean` (changed in the TWS API).

### Other changes

These do not change the JSON interface.

- The build downloads IBKR's TWS API installer for Windows, macOS (Apple
  Silicon or Intel) or Linux and runs it unattended, as IBKR no longer
  publishes a zip archive. Use `-PtwsApiPlatform` to choose the platform.
- The test fixtures are regenerated from IBKR's 10.50 sample code
  (`./gradlew generateClientCalls generateOrderCalls`).

## [1.2.0]

Built with TWS API 10.37.02.

### Fixed

- **`reqWshEventData` works.** Its `WshEventData` parameter could not be built
  from JSON (it has no no-argument constructor), so the command always failed.
  It now accepts an object with `conId` or `filter`, plus `fillWatchlist`,
  `fillPortfolio`, `fillCompetitors`, `startDate`, `endDate` and
  `totalLimit`, and `help "WshEventData"` lists these fields (it listed none
  before).

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
