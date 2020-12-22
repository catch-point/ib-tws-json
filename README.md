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

Additional commands are listed below.

### Additional Available Commands

#### help

The command `help` lists available commands in a `help` response. Other parameters string can be given to provide the schema available for those methods or types. `helpEnd` is sent it indicate the response is complete.

`help "EWrapper"` and `help "TwsEvents"` list the events sent from the shell based an activity in TWS.

#### login

This opens the TWS (or Gateway) software and logins into the application.

The first parameter must be "live" or "paper", the second provides the credentials in Base64 UTF-8 encoding. It has the following base64 UTF-8 string properties.

* FIXBase64UserName
* FIXBase64Password
* IBAPIBase64UserName
* IBAPIBase64Password

The third parameter to login is a JSON object with the properties from the table below.

| Property | Description |
| -------- | ------------|
|AcceptIncomingConnectionAction|Only needed if TWS is on a different server. The default value of "manual" means that the user must explicitly configure IBC to automatically accept API connections from unknown computers, but it is safest to set this to "reject" and to explicitly configure TWS to specify which IP addresses are allowed to connnect to the API.|
|AcceptNonBrokerageAccountWarning|If set to false, the user must manually accept this warning.|
|AllowBlindTrading|Unless this is set to true, attempts to place an order for a contract for which the account has no market data subscription, the user must manually accept a dialog warning against such blind trading.|
|DismissNSEComplianceNotice|If set to false, the user must manually dismiss this warning.|
|ExistingSessionDetectedAction| When a user logs on to an IBKR account for trading purposes by any means, the IBKR account server checks to see whether the account is already logged in elsewhere. If so, a dialog is displayed to both the users that enables them to determine what happens next. Read on below to see how this setting instructs TWS how to proceed.|
|FIX|Set to true if TWS should authenticate with Financial Information Exchange (FIX) protocol.|
|LogComponents|Use to identify window names that are opened by TWS. Can be "activate", "open", "openclose", or "never".|
|MinimizeMainWindow|Set to true to minimize TWS when it starts.|
|ReadOnlyLogin|If ReadOnlyLogin is set to true, and the user is enrolled in IB's account security programme, the user will not be asked to supply the security code, and login to TWS will occur automatically in read-only mode: in this mode, placing or managing orders is not allowed. Otherwise, if the user is enrolled in IB's account security programme, the user must supply the relevant security code to complete the login. If the user is not enrolled in IB's account security programme, this setting is ignored.|
|StoreSettingsOnServer|Set this to true to store a copy of the TWS settings on IB's servers as well as locally on your computer.  This enables you to run TWS on different computers with the same configuration, market data lines, etc.  Otherwise, running TWS on different computers will not share the same settings.|
|SuppressInfoMessages|Set to false to log more intermediate information about window states.|

##### ExistingSessionDetectedAction

When a user logs on to an IBKR account for trading purposes by any means, the
IBKR account server checks to see whether the account is already logged in
elsewhere. If so, a dialog is displayed to both the users that enables them
to determine what happens next. The `ExistingSessionDetectedAction` setting
instructs TWS how to proceed when it displays one of these dialogs:

  * If the existing TWS session is set to 'primary', the existing session
    continues and the new session is not permitted to proceed.

  * If the existing TWS session is set to 'primaryoverride', the existing
    session terminates and the new session is permitted to proceed.

  * If the new session is via TWS with
    `ExistingSessionDetectedAction=secondary', the new TWS exits so that the
    existing session is unaffected.

  * If the existing TWS session is set to 'manual', the user must handle the
    dialog.

The difference between `primary` and `primaryoverride` is that a
`primaryoverride` session can be taken over by a new `primary` or
`primaryoverride` session, but a `primary` session cannot be taken over by
any other session.

When set to 'primary', if another TWS session is started and manually told to
end the `primary` session, the `primary` session is automatically reconnected.

The default is 'manual'.

During a normal login process a "login" response is ussed with "TWO_FA_IN_PROGRESS" and later with "LOGGED_IN".

#### enableAPI

Open the Configuration dialogue and enables the API. The first parameter is the port number to listen on, the second parameter is true if the conneciton should be read-only, false otherwise. An "enableAPI" response is issued with the currently configured port number. However, TWS may need a little more time before it will be listening on the given port.

#### saveSettings

Saves the current settings.

#### sleep

Causes the shell to pause the given number of milliseconds before processing the next command.

#### reconnectData

Presses the Reconnect Date button in TWS.

#### reconnectAccount

Issues Ctl-Alt-R to TWS to recennect to the servers.

#### eConnect

This can be used with or without logging in to TWS (if another TWS instance is running). The parameters are as follows.

* host
* port number
* true for extra authentication, false for normal authentication

#### eDisconnect

Disconnect the client API. This can be used to change clientId by calling eConnect afterwards.

#### exit

Close everything down and exit the shell

#### serverVersion

Issue a "serverVersion" response with the Host's version. Some of the API functionality might not be available in older Hosts and therefore it is essential to keep the TWS/Gateway and TWS API up to date.

#### isConnected

Issue a "isConnected" response.

#### connectedHost

Issue a "connectedHost" response.

#### isUseV100Plus

Issue a "isUseV100Plus" response, which is enabled by default.

#### optionalCapabilities

Provide a string value recognized by the API or issues a "optionalCapabilities" response of those values.

#### faMsgTypeName

Issue a "faMsgTypeName" response converting 1, 2, or 3 into "GROUPS", "PROFILES", and "ALIASES" respectively.

#### getTwsConnectionTime

Issue a "getTwsConnectionTime" response with the time the connection was established.
