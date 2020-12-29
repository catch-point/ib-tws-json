# ib-tws-shell Commands
Read–eval–print loop for Interactive Broker's Trader Workstation and Gateway

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
