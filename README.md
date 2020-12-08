# tws-shell
Read–eval–print loop for TWS API

Introduction
------------

The TWS API is a simple yet powerful interface through which Interactive Broker clients can automate their trading strategies, request market data and monitor your account balance and portfolio in real time.

This project serializes Interactive Broker's Java Client to stdin/stdout so that it can be used by other programs. This is particularly useful for programing languages that do not have an official TWS API Client.

Requirements
------------

Users must agree to the terms of the Interactive Broker license, download their software and Java Client API.
See https://www.interactivebrokers.com/en/index.php?f=14099#tws-software

* The TWS API is an interface to TWS or IB Gateway, and as such requires network connectivity to a running instance of one of these programs. They can be downloaded here: https://www.interactivebrokers.com/en/index.php?f=14099#tws-software
* To obtain the TWS API source and sample code, download the API Components here: http://interactivebrokers.github.io
* A working knowledge of the API programming language.
* This project makes use of gradle build tool. See https://gradle.org/

Usage
-----

Pass EClient calls on one line to stdin. Prefix with the method name, followed by each parameter prefixed by one or more spaces. Every parameter must be encoded in JSON.

Calls to EWrapper are serialized to stdout. Prefixed by the method name, followed by each parameter prefixed by a tab character. Every parameter is serialized as JSON.

The command "help" is available to show a list of EClient+ calls and if passed the name of a method or type, it will show more.

Type "exit" to quit.

For example consider the calls below.

```
java -cp TwsApi.jar:jline.jar:javax.json-1.1.jar:javax.json-api-1.1.jar:tws-shell.jar com.meerkattrading.tws.Shell << EOF
connect "localhost" 7496 0
reqIds -1
reqAllOpenOrders
reqAutoOpenOrders true
reqOpenOrders
placeOrder 282 { "symbol":"IBKR", "secType": "STK", "exchange": "ISLAND"} { "action":"SELL", "orderType":"LMT", "totalQuantity": 1, "lmtPrice": 50}
disconnect
EOF
```
