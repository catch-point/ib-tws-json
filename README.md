# ib-tws-json
JSON API Extension for Interactive Brokers' Trader Workstation and Gateway

Introduction
------------

The TWS API is a simple yet powerful interface through which Interactive Brokers clients can automate their trading strategies, request market data and monitor their account balance and portfolio in real time.

This project extends Interactive Brokers' Trader Workstation (TWS) and IB Gateway with a JSON API, so the TWS API can be used without an official client library. This is particularly useful for programming languages that do not have an official TWS API client, such as Node.js with https://github.com/catch-point/ib-tws-node

Unlike other client libraries, this project runs inside the TWS Desktop or Gateway (in the same JVM as TWS) and offers a text-based API that any language with a TCP socket can use.

To learn how it works, see [ARCHITECTURE.md](ARCHITECTURE.md).

Requirements
------------

* **TWS or IB Gateway.** The TWS API is an interface to TWS or IB Gateway, so a running instance of one of these programs is required. They can be downloaded here: https://www.interactivebrokers.com/en/index.php?f=14099#tws-software
* **The TWS API Java client (`TwsApi.jar`).** It is not bundled with this project. IBKR distributes it as an installer for each platform:
  * Windows: https://download2.interactivebrokers.com/installers/tws-api/stable/tws-api-stable-windows-x64.exe
  * macOS (Apple Silicon): https://download2.interactivebrokers.com/installers/tws-api/stable/tws-api-stable-macos-arm.dmg
  * macOS (Intel): https://download2.interactivebrokers.com/installers/tws-api/stable/tws-api-stable-macosx-x64.dmg
  * Linux: https://download2.interactivebrokers.com/installers/tws-api/stable/tws-api-stable-linux-x64.sh

  This project searches for `TwsApi.jar` in `C:\TWS API`, `~/IBJts`, `~/Jts`, `~/Downloads` and a few other common folders. If you install it elsewhere, use `--tws-api-jar` or `--tws-api-path` (see [Options](#options)).
* **A working knowledge of the TWS API.** See https://interactivebrokers.github.io/tws-api/

This version is built and tested with **TWS API 10.50.02** and **Java 17**, the Java version TWS currently bundles.

The TWS API and its samples are licensed by Interactive Brokers under the GNU GPL version 3 or later. This project is licensed under the GNU GPL version 3 (see [LICENSE](LICENSE)).

Note that you do not need an IBKR account to try this out, as you can use IBKR's Free Trial offer, for which there is a link at the top of the homepage on their website.

Installation
------------

Run the release JAR by double clicking it, or from a terminal run:
```
java -jar ib-tws-json.jar
```

This searches for TWS/Gateway, installs the extension, and launches TWS/Gateway. If several versions of TWS exist, or it is installed in an alternative location, run `java -jar ib-tws-json.jar --help` to see how to specify the install location.

The extension is installed by adding a `-javaagent` line to TWS's `tws.vmoptions` (or `ibgateway.vmoptions`) file, so it starts whenever TWS starts.

Configuration
-------------

By default, the JSON API listens on the TWS API port plus 100. For example, if the TWS API is configured to run on port 7497, the JSON API will be on port 7597. To use a different offset, use `--json-api-port-offset`. To use a fixed port, use `--json-api-port` when installing.

### Security

The JSON API has **no authentication**, and any client that can connect to it can trade on the logged-in account. By default it only listens on the loopback interface (`127.0.0.1`), so only programs on the same machine can connect. Only use `--json-api-inet` to listen on another network interface on a network you trust, or behind a firewall or SSH tunnel.

Stand Alone
-----------

The JSON API can also be run stand alone (without installing it into TWS), using the `--interactive` argument and the TWS API port. This is useful to explore and debug the API when developing a client:

```
java -jar ib-tws-json.jar --interactive --tws-api-port 7497
```

Commands are read from stdin and events are written to stdout. Use `--no-prompt` when piping commands in. Command files can also be given as arguments.

Uninstall
---------

From a terminal run `java -jar ib-tws-json.jar --uninstall`

Options
-------

| Option | Description |
|--------|-------------|
| `-h`, `--help` | Show the options and exit |
| `-v`, `--version` | Print the current version and exit |
| `--install` | Install the extension into TWS |
| `--uninstall` | Uninstall the extension from TWS |
| `--launch` | Install the extension and start TWS before exiting (the default when no options are given) |
| `-i`, `--interactive` | Enter interactive (stand alone) client mode |
| `--no-prompt` | Don't show a prompt in interactive mode |
| `--tws-api-path <dir>` | The folder to search for `TwsApi.jar` |
| `-j`, `--tws-api-jar <file>` | The `TwsApi.jar` file |
| `--tws-api-host <host>` | Host name or IP address running TWS (stand alone mode, default `localhost`) |
| `-p`, `--tws-api-port <port>` | Port the TWS API is running on |
| `--json-api-port <port>` | Fixed port for the JSON API to listen on |
| `--json-api-port-offset <n>` | JSON API port offset from the TWS API port (default 100) |
| `--json-api-inet <address>` | Network interface for the JSON API to listen on (default loopback) |
| `--jts-exe-name <name>` | The primary launch file name installed by the TWS software |
| `--jts-install-dir <dir>` | Location of the Jts, ibgateway, Trader Workstation or IB Gateway folder to use |
| `--jts-config-dir <dir>` | Where TWS reads and stores its settings. Port settings given with this option apply only to that TWS configuration |

JSON API
--------

The JSON API mirrors the TWS API's actions (`EClient` methods) and events (`EWrapper` callbacks) in text form. Most `EClient` methods are available as commands. See https://interactivebrokers.github.io/tws-api/classIBApi_1_1EClient.html and https://interactivebrokers.github.io/tws-api/interfaceIBApi_1_1EWrapper.html

* **Commands** are one line: the `EClient` method name followed by the parameters as JSON values, separated by whitespace.
* **Events** are one line: the `EWrapper` method name followed by the parameters as JSON values, separated by tabs.

### Example session

These are commands and real output from the stand alone mode with a TWS running:

```
> isConnected
isConnected	false
> eConnect 0 false
connectAck
managedAccounts	"U23004070"
nextValidId	1
error	-1	"1793765279677"	2104	"Market data farm connection is OK:usfarm.nj"	""
error	-1	"1793765279820"	2104	"Market data farm connection is OK:usfuture"	""
error	-1	"1793765279821"	2104	"Market data farm connection is OK:cafarm"	""
error	-1	"1793765279821"	2104	"Market data farm connection is OK:usfarm"	""
error	-1	"1793765279822"	2106	"HMDS data farm connection is OK:ushmds"	""
error	-1	"1793765279822"	2158	"Sec-def data farm connection is OK:secdefil"	""
> isConnected
isConnected	true
> reqContractDetails 1 {"symbol":"AAPL","secType":"STK","exchange":"SMART","currency":"USD"}
contractDetails	1	{"aggGroup":1,"category":"Computers","contract":{"conid":265598,"currency":"USD","exchange":"SMART","localSymbol":"AAPL","primaryExch":"NASDAQ","secType":"STK","strike":"0.0","symbol":"AAPL","tradingClass":"NMS"},"industry":"Technology","ineligibilityReasonList":[],"liquidHours":"20261103:0930-20261103:1600;20261104:0930-20261104:1600;20261105:0930-20261105:1600;20261106:0930-20261106:1600","longName":"APPLE INC","marketName":"NMS","marketRuleIds":"4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563,4563","minSize":"0.0001","minTick":"0.01","orderTypes":"ACTIVETIM,AD,ADDONT,ADJUST,ALERT,ALGO,ALLOC,AON,AVGCOST,BASKET,BENCHPX,CASHQTY,COND,CONDORDER,DARKONLY,DARKPOLL,DAY,DEACT,DEACTDIS,DEACTEOD,DIS,DUR,GAT,GTC,GTD,GTT,HID,IBKRATS,ICE,IMB,IOC,LIT,LMT,LOC,MIDPX,MIT,MKT,MOC,MTL,NGCOMB,NODARK,NONALGO,OCA,OPG,OPGREROUT,PEGBENCH,PEGMID,POSTATS,POSTONLY,PREOPGRTH,PRICECHK,REL,REL2MID,RELPCTOFS,RPI,RTH,SCALE,SCALEODD,SCALERST,SIZECHK,SNAPMID,SNAPMKT,SNAPREL,STP,STPLMT,SWEEP,TRAIL,TRAILLIT,TRAILLMT,TRAILMIT,WHATIF","priceMagnifier":1,"secIdList":[{"tag":"ISIN","value":"US0378331005"}],"sizeIncrement":"0.0001","stockType":"COMMON","subcategory":"Computers","suggestedSizeIncrement":"40","timeZoneId":"US/Eastern","tradingHours":"20261103:0400-20261103:2000;20261104:0400-20261104:2000;20261105:0400-20261105:2000;20261106:0400-20261106:2000","validExchanges":"SMART,AMEX,NYSE,CBOE,PHLX,ISE,CHX,ARCA,NASDAQ,DRCTEDGE,BEX,BATS,EDGEA,BYX,IEX,EDGX,FOXRIVER,PEARL,NYSENAT,LTSE,MEMX,IBEOS,OVERNIGHT,TPLUS0,PSX,T24X,TXSE"}
contractDetailsEnd	1
> bogus
error	"bogus?"
> exit
connectionClosed
```

When connected, `reqContractDetails` is answered with `contractDetails` events and a final `contractDetailsEnd	1`, just like the `EWrapper` callbacks.

### Message format

* **Parameters are positional,** in the same order as the Java method. Use `help "<command>"` to list them.
* **Objects** such as `Contract` and `Order` are JSON objects using the Java client's field names. Fields that are left out keep their defaults. Use `help "<Type>"` to list a type's fields and defaults.
* **Numbers:** `int` values are written as JSON numbers. `long`, `double` and `Decimal` values are written as JSON strings, so no precision is lost in clients such as JavaScript. Both numbers and numeric strings are accepted as input.
* **Enums** are written by name, such as `"GROUPS"`. As input, an enum also accepts its TWS API numeric code (for example `1` for `GROUPS`).
* **Missing parameters:** trailing parameters that are objects, strings or lists can be left out, and are passed as `null`. If a number or boolean parameter is missing, the command continues on the next line, so long commands can span several lines.
* **Strings** must be JSON strings in double quotes, for example `requestFA "GROUPS"`.
* **Overloaded methods:** when the TWS API has several versions of a method, the version is chosen from the number and types of the values given. For example, `requestFA 1` and `requestFA "GROUPS"` both work.
* **Errors** are reported as `error` events: either TWS's `error` callback, or a message about the command, such as `error "bogus?"` for an unknown command.
* The TWS API's `*ProtoBuf` methods are not available. Each has a regular equivalent, except IB's configuration API (`reqConfigProtoBuf`, `updateConfigProtoBuf`).

A few additional actions are provided below.

#### help

Displays a list of available action commands. If an action, event or type name is given (in double quotes), it displays its parameters or fields, ending with a `helpEnd` event. For example:

```
help "reqContractDetails"
help	"reqContractDetails"	"arg0"	"int"
help	"reqContractDetails"	"arg1"	"Contract"
helpEnd	"reqContractDetails"
```

For a type, each line lists a field, its type and its default value, such as `help	"Contract"	"symbol"	"String"	null`. Parameter names are shown as `arg0`, `arg1`, … because the TWS API jar does not include them.

#### sleep

Causes the server to pause the given number of milliseconds before processing the next command.

#### eConnect

This should be run before any EClient commands. Unlike EClient's version, it only takes the clientId and extraAuth parameters:

* clientId
* true for extra authentication, false for normal authentication

#### eDisconnect

Disconnect the TWS API. This can be used to change clientId by calling eConnect afterwards.

#### exit

Disconnects the JSON client

#### serverVersion

Issue a "serverVersion" event response with the Host's version. Some of the API functionality might not be available in older Hosts and therefore it is essential to keep the TWS/Gateway and TWS API up to date.

#### isConnected

Issue a "isConnected" event response.

#### connectedHost

Issue a "connectedHost" event response.

#### isUseV100Plus

Issue a "isUseV100Plus" event response, which is enabled by default.

#### optionalCapabilities

Provide a string value recognized by the API or issues a "optionalCapabilities" event response of those values.

#### getTwsConnectionTime

Issue a "getTwsConnectionTime" event response with the time the connection was established.

Building from Source
--------------------

```
./build.sh
```

or `./gradlew build`. The build:

* uses **JDK 17**, selected by a Java toolchain and `gradle/gradle-daemon-jvm.properties`, so the compiled classes match the Java version TWS bundles. If JDK 17 is not installed, Gradle downloads one.
* downloads IBKR's TWS API installer for the build machine's platform and runs it unattended into `build/dependencies/twsapi`. To choose a platform, use `-PtwsApiPlatform=windows-x64`, `macos-arm`, `macosx-x64` or `linux-x64`.
* writes the extension to `build/libs/ib-tws-json-<version>.jar`.

The tests run without TWS, by replaying recorded calls through the JSON layer. See [ARCHITECTURE.md](ARCHITECTURE.md#testing) for what each test covers, how to regenerate the test fixtures, and how to upgrade to a new TWS API release.
