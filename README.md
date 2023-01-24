# ib-tws-json
JSON API Extension for Interactive Broker's Trader Workstation and Gateway

Introduction
------------

The TWS API is a simple yet powerful interface through which Interactive Broker clients can automate their trading strategies, request market data and monitor your account balance and portfolio in real time.

This project extends Interactive Broker's Trader Workstation and Gateway by providing a JSON serialized API so that it can be used without an official client library. This is particularly useful for programming languages that do not have an official TWS API Client, such as https://github.com/jamesrdf/ib-tws-node

This project differentiates from other client libraries by extending the TWS Desktop or Gateway (in the same JVM running the TWS Client) to create a non-binary API that can be used without an official client library.

Requirements
------------

Users must agree to the terms of the Interactive Broker license, download their software and Java Client API.

* The TWS API is an interface to TWS or IB Gateway, and as such requires network connectivity to a running instance of one of these programs. They can be downloaded here: https://www.interactivebrokers.com/en/index.php?f=14099#tws-software
* To obtain the TWS API Java source and sample code download the API Components from here: http://interactivebrokers.github.io and save it to  to C:\TWS API or ~/IBJts
* A working knowledge of the API programming language.
* This project makes use of gradle build tool, which is needed to alter the source code. See https://gradle.org/

Note that you do not need an IBKR account to try this out, as you can use IBKR's Free Trial offer, for which there is a link at the top of the homepage on their website.

Installation
------------

Run the release JAR by double clicking it or from a terminal run:
```
java -jar ib-tws-json.jar
```

This will search for, install the extension, and run IBKR TWS/Gateway. If multiple versions of TWS existing, or it is installed in an alternative location, use the help message `--help` from a terminal to learn how to specify a install location.

Configuration
-------------

The default options will install the extension in the default location and launch TWS. By default this extension will listen a port offset by 100, for example if TWS API is configured to run on port 7497 then the JSON API will be on port 7547. To specify an alternative (and fixed) port use the `--json-port` option from a terminal when installing.

Stand Alone
-----------

The JSON API can be run stand alone (without installing) using the `--interactive` argument. This is useful to explore and debug the API when developing a client.

Unistall
--------

From a terminal run `java -jar ib-tws-json.jar --uninstall`

JSON API
--------

The JSON API mimics the TWS API (actions and events), but in a non-binary form. Most of the action commands of EClient are available in JSON API. See https://interactivebrokers.github.io/tws-api/classIBApi_1_1EClient.html

Each action is the EClient method name followed by whitespace separated JSON encodings of the parameters. Events are the EWrapper method name followed by tab separated JSON encodings of the parameters.

A few additional actions are provided below.

#### help

Displays a list of available action commands. If an action or event name is given (in double quotes), it will display the parameters to that command.

#### sleep

Causes the server to pause the given number of milliseconds before processing the next command.

#### eConnect

This should be run before any EClient commands. This differs from EClient and only takes the clientId and extraAuth parameters.

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

#### faMsgTypeName

Issue a "faMsgTypeName" event response converting 1, 2, or 3 into "GROUPS", "PROFILES", and "ALIASES" respectively.

#### getTwsConnectionTime

Issue a "getTwsConnectionTime" event response with the time the connection was established.