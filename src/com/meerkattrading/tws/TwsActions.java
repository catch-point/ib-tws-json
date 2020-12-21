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

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import ibcalpha.ibc.IbcException;

/**
 * Actions to control TWS
 *
 * @author James Leigh
 *
 */
public interface TwsActions {

	public void login(TradingMode mode, Base64LoginManager credentials, TraderWorkstationSettings settings)
			throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			IOException, InterruptedException;

	public void sleep(Long ms) throws InterruptedException;

	public void enableAPI(Integer portNumber, Boolean readOnly)
			throws InterruptedException, IbcException, ExecutionException, IOException;

	public void saveSettings();

	public void reconnectData();

	public void reconnectAccount();

	public void eConnect(String host, int port, int clientId, boolean extraAuth) throws InterruptedException;

	public void eDisconnect();

	public void exit() throws EOFException;

	public void serverVersion() throws IOException;

	public void isConnected() throws IOException;

	public void connectedHost() throws IOException;

	public void isUseV100Plus() throws IOException;

	public void optionalCapabilities(String val) throws IOException;

	public void faMsgTypeName(int type) throws IOException;

	public void getTwsConnectionTime() throws IOException;

	public void help(String name) throws IllegalAccessException, InvocationTargetException, IOException;

}
