# ib-tws-shell
Read–eval–print loop for Interactive Broker's Trader Workstation and Gateway

Introduction
------------

The TWS API is a simple yet powerful interface through which Interactive Broker clients can automate their trading strategies, request market data and monitor your account balance and portfolio in real time.

This project serializes Interactive Broker's Java Client to stdin/stdout so that it can be used by other programs. This is particularly useful for programming languages that do not have an official TWS API Client, such as https://github.com/jamesrdf/ib-tws-node

This project differentiates from other client libraries by integrating the TWS Desktop or Gateway into the same JVM running the TWS Client. Whereby providing a single interface to control Interactive Broker's Trader Workstation / Gateway.

Requirements
------------

Users must agree to the terms of the Interactive Broker license, download their software and Java Client API.

* The TWS API is an interface to TWS or IB Gateway, and as such requires network connectivity to a running instance of one of these programs. They can be downloaded here: https://www.interactivebrokers.com/en/index.php?f=14099#tws-software
* To obtain the TWS API source and sample code to C:\TWS API or ~/IBJts, download the API Components from here: http://interactivebrokers.github.io
* A working knowledge of the API programming language.
* This project makes use of gradle build tool. See https://gradle.org/

TWS needs to operate in English so that the various dialogues can be recognised. You can set TWS's language by starting it manually (ie without passing a password) and selecting the language on the initial login dialog. TWS will remember this language setting when you subsequently use it.

Note that you do not need an IBKR account to try this out, as you can use IBKR's Free Trial offer, for which there is a link at the top of the homepage on their website.

Launching
---------

The ib-tws-shell takes the following command line parameters.

### Command Line Parameters

| Parameter Name | Parameter Value |
|----------------|-----------------|
|java-home|The JRE that is used to launch TWS and ib-tws-shell. If none is provided, an install4j JRE is searched for in the tws-path that would have been installed by TWS. Note that TWS cannot be run with just any JRE and depends on features provided with the JRE that came with the install.|
|tws-api-jar|Points to the TwsApi.jar file that should be used when connecting to TWS. If none is provide it is searched for using tws-api-path.|
|tws-api-path|Where to look for the TwsApi.jar file (if tws-api-jar is not provided). If not provided, it will look in C:\\TWS API, ~/IBJts, and a few other places.|
|tws-path|The install location of TWS Desktop or Gateway. If using an offline version (or Gateway) this can point to the folder with the version number. When not provided, the system will look in the default location for Gateway and (if not found) TWS Desktop.|
|tws-settings-path|Every running instance must have a unique tws-settings-path, which defaults to `~/Jts`.|
|tws-version|If the tws-path is not provided this can help choose which TWS instance to launch. It is recommended to use an offline TWS install to give project contributors time to test new TWS releases.|
|silence|Don't log anything, just report API responses.|
|no-prompt|Don't print a friendly welcome message.|

Usage
-----

Pass EClient calls to stdin. Prefix with the method name, followed by each parameter prefixed by white spaces. Every parameter must be encoded in JSON. JSON values can span multiple lines.

Calls to EWrapper are serialized to stdout, one per line. Prefixed by the method name, followed by each parameter prefixed by a tab character. Every parameter is serialized as JSON.

The command "help" is available to show a list of EClient+ calls and if passed the name of a method or type, it will show more.

Type "exit" to quit.

For example consider the auto script below that records the NetLiquidation of the account into a file.

```
java -jar build/libs/tws-shell*.jar >> NetLiquidation.tsv << EOF
login    "live"   {
    "IBAPIBase64UserName": "dXNlcm5hbWU=",
    "IBAPIBase64Password": "cGFzc3dvcmQ="
}  {
    "AcceptIncomingConnectionAction":"reject",
    "AcceptNonBrokerageAccountWarning":true,
    "AllowBlindTrading":true,
    "DismissNSEComplianceNotice": true,
    "DismissPasswordExpiryWarning": true,
    "ExistingSessionDetectedAction": "secondary",
    "LogComponents": "never",
    "MinimizeMainWindow": false,
    "ReadOnlyLogin": true,
    "StoreSettingsOnServer":false,
    "SuppressInfoMessages": true
}
enableAPI   7496  true
sleep   2000
eConnect    "localhost" 7496    0  false
sleep   2000
reqCurrentTime
reqAccountSummary   1000    "All"   "NetLiquidation"
sleep   2000
exit
EOF
```

Commands
--------

Most of the commands available are in EClient. See https://interactivebrokers.github.io/tws-api/classIBApi_1_1EClient.html

Additional commands are documented in [commands.md](./commands.md)

