# tws-shell
Read–eval–print loop for Interactive Broker's TWS API

Introduction
------------

The TWS API is a simple yet powerful interface through which Interactive Broker clients can automate their trading strategies, request market data and monitor your account balance and portfolio in real time.

This project serializes Interactive Broker's Java Client to stdin/stdout so that it can be used by other programs. This is particularly useful for programing languages that do not have an official TWS API Client.

Requirements
------------

Users must agree to the terms of the Interactive Broker license, download their software and Java Client API.
See https://www.interactivebrokers.com/en/index.php?f=14099#tws-software

* The TWS API is an interface to TWS or IB Gateway, and as such requires network connectivity to a running instance of one of these programs. They can be downloaded here: https://www.interactivebrokers.com/en/index.php?f=14099#tws-software
* To obtain the TWS API source and sample code to C:\TWS API or ~/IBJts, download the API Components from here: http://interactivebrokers.github.io
* A working knowledge of the API programming language.
* This project makes use of gradle build tool. See https://gradle.org/

TWS needs to operate in English so that the various dialogues can be recognised. You can set TWS's language by starting it manually (ie without passing a password) and selecting the language on the initial login dialog. TWS will remember this language setting when you subsequently use with the command.

Note that you do not need an IBKR account to try this out, as you can use IBKR's Free Trial offer, for which there is a link at the top of the homepage on their website.

Usage
-----

Pass EClient calls to stdin. Prefix with the method name, followed by each parameter prefixed by white spaces. Every parameter must be encoded in JSON. JSON values span multiple lines.

Calls to EWrapper are serialized to stdout, one per line. Prefixed by the method name, followed by each parameter prefixed by a tab character. Every parameter is serialized as JSON.

The command "help" is available to show a list of EClient+ calls and if passed the name of a method or type, it will show more.

Type "exit" to quit.

For example consider the auto script below that records the NetLiquidation of the account into a file.

```
java -jar build/libs/tws-shell*.jar >> NetLiquidation.tsv << EOF
open    "live"  {
    "AcceptIncomingConnectionAction":"reject",
    "AcceptNonBrokerageAccountWarning":true,
    "AllowBlindTrading":true,
    "DismissNSEComplianceNotice": true,
    "DismissPasswordExpiryWarning": true,
    "ExistingSessionDetectedAction": "secondary",
    "ExitAfterSecondFactorAuthenticationTimeout": false,
    "LogComponents": "never",
    "MinimizeMainWindow": false,
    "ReadOnlyLogin": true,
    "SecondFactorAuthenticationExitInterval":40,
    "ShowAllTrades":true,
    "StoreSettingsOnServer":false,
    "SuppressInfoMessages": true
}   {
    "IBAPIBase64UserName": "dXNlcm5hbWU=",
    "IBAPIBase64Password": "cGFzc3dvcmQ="
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
