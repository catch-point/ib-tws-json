/*
 * Copyright (c) 2020 James Leigh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.meerkattrading.tws;

import ibcalpha.ibc.LoginManager.LoginState;

/**
 * Events fired from TWS
 *
 * @author James Leigh
 *
 */
public interface TwsEvents {

	void login(LoginState new_state);

	void enableAPI(int enableAPI);

	void error(String message);

	void helpEnd(String name);

	void help(String name, String value);

	void help(String type_name, Object value);

	void help(String method_name, String parameter, String type_name);

	void help(String name, String property, String type_name, Object default_value);

	void serverVersion(int serverVersion);

	void isConnected(boolean connected);

	void connectedHost(String connectedHost);

	void isUseV100Plus(boolean useV100Plus);

	void optionalCapabilities(String optionalCapabilities);

	void faMsgTypeName(String faMsgTypeName);

	void getTwsConnectionTime(String twsConnectionTime);

}
