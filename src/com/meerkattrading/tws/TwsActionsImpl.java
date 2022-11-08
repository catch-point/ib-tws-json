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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ib.client.EClient;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;

import ibcalpha.ibc.DefaultMainWindowManager;
import ibcalpha.ibc.ErrorCodes;
import ibcalpha.ibc.IbcException;
import ibcalpha.ibc.LoginManager;
import ibcalpha.ibc.LoginManager.LoginState;
import ibcalpha.ibc.MainWindowManager;
import ibcalpha.ibc.Settings;
import ibcalpha.ibc.TWSManager;
import ibcalpha.ibc.TradingModeManager;

/**
 * Implements the actions that control TWS
 *
 * @author James Leigh
 *
 */
public class TwsActionsImpl implements TwsActions {
	private final Logger logger = Logger.getLogger(TwsActionsImpl.class.getName());
	private final TwsEvents events;
	private final String ibDir;
	private final Printer out;
	private final EJavaSignal signal = new EJavaSignal();
	private EClientSocket client;
	private Thread signalThread;
	private Base64LoginManager login;
	private Thread loginThread;
	private Map<String, Method> commands;
	private Map<Type, PropertyType> properties;

	public TwsActionsImpl(String ibDir, Printer out) {
		this.ibDir = ibDir;
		this.out = out;
		EWrapper wrapper = EWrapperHandler.newInstance(out);
		this.client = new EClientSocket(wrapper, signal);
		this.events = TwsEventsHandler.newInstance(out);
	}

	public synchronized void login(TradingMode mode, Base64LoginManager credentials, TraderWorkstationSettings settings)
			throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			IOException, InterruptedException {
		if (TWSManager.isOpen()) {
			throw new IllegalStateException("TWS is already open");
		}
		login = credentials == null ? new Base64LoginManager() : credentials;
		boolean isGateway = TWSManager.class.getClassLoader().getResource("ibgateway/GWClient") != null;
		Settings.initialise(settings == null ? new TraderWorkstationSettings() : settings);
		LoginManager.initialise(login);
		MainWindowManager.initialise(new DefaultMainWindowManager(isGateway));
		TradingModeManager.initialise(new TradingModeManager() {

			@Override
			public String getTradingMode() {
				return mode == null ? TradingMode.live.name() : mode.name();
			}

			@Override
			public void logDiagnosticMessage() {
				// nothing to say
			}
		});
		loginThread = new Thread(() -> {
			LoginState old_state = LoginState.LOGGED_OUT;
			while (old_state != LoginState.LOGGED_IN) {
				synchronized (login) {
					try {
						login.wait();
						LoginState new_state = login.getLoginState();
						if (!new_state.equals(old_state)) {
							events.login(new_state);
							old_state = new_state;
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		});
		loginThread.start();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (isGateway) {
			TWSManager.start(cl.loadClass("ibgateway.GWClient"), ibDir);
		} else {
			TWSManager.start(cl.loadClass("jclient.LoginFrame"), ibDir);
		}
	}

	public void sleep(Long ms) throws InterruptedException {
		Thread.sleep(ms == null ? 0 : ms);
	}

	public synchronized void enableAPI(Integer portNumber, Boolean readOnly)
			throws InterruptedException, IbcException, ExecutionException, IOException {
		events.enableAPI(TWSManager.enableAPI(portNumber, readOnly));
	}

	public synchronized void saveSettings() {
		TWSManager.saveSettings();
	}

	public synchronized void reconnectData() {
		TWSManager.reconnectData();
	}

	public synchronized void reconnectAccount() {
		TWSManager.reconnectAccount();
	}

	public synchronized void eConnect(String host, int port, int clientId, boolean extraAuth)
			throws InterruptedException {
		if (getEClient().isConnected()) {
			getEClient().eDisconnect();
		}
		if (signalThread != null && signalThread.isAlive()) {
			signalThread.join();
		}
		((EClientSocket) getEClient()).eConnect(host, port, clientId, extraAuth);
		final EReader reader = new EReader((EClientSocket) getEClient(), signal);

		reader.start();
		// An additional thread is created in this program design to empty the messaging
		// queue
		signalThread = new Thread(() -> {
			while (getEClient().isConnected()) {
				signal.waitForSignal();
				try {
					reader.processMsgs();
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
					events.error(e.getMessage());
				}
			}
		});
		signalThread.start();
	}

	public synchronized void eDisconnect() {
		if (getEClient().isConnected()) {
			getEClient().eDisconnect();
		}
	}

	public void exit() throws EOFException {
		if (getEClient().isConnected()) {
			eDisconnect();
		}
		if (loginThread != null) {
			loginThread.interrupt();
		}
		if (login != null && TWSManager.isOpen()) {
			boolean isGateway = TWSManager.class.getClassLoader().getResource("ibgateway/GWClient") != null;
			TWSManager.stop(isGateway);
		} else if (login != null) {
			System.exit(ErrorCodes.ERROR_CODE_2FA_LOGIN_TIMED_OUT);
		}
		throw new EOFException("exit");
	}

	public void serverVersion() throws IOException {
		events.serverVersion(getEClient().serverVersion());
	}

	public void isConnected() throws IOException {
		events.isConnected(getEClient().isConnected());
	}

	public void connectedHost() throws IOException {
		events.connectedHost(getEClient().connectedHost());
	}

	public void isUseV100Plus() throws IOException {
		events.isUseV100Plus(getEClient().isUseV100Plus());
	}

	public void optionalCapabilities(String val) throws IOException {
		if (val == null) {
			events.optionalCapabilities(getEClient().optionalCapabilities());
		} else {
			getEClient().optionalCapabilities(val);
		}
	}

	public void faMsgTypeName(int type) throws IOException {
		events.faMsgTypeName(EClient.faMsgTypeName(type));
	}

	public void getTwsConnectionTime() throws IOException {
		events.getTwsConnectionTime(getEClient().getTwsConnectionTime());
	}

	public void help(String name) throws IllegalAccessException, InvocationTargetException, IOException {
		if (name == null || name.length() == 0) {
			events.help("actions", TwsActions.class.getSimpleName());
			events.help("actions", EClient.class.getSimpleName());
			events.help("actions", EClientSocket.class.getSimpleName());
			events.help("actions", "EClientMsgSink");
			events.help("events", EWrapper.class.getSimpleName());
			events.help("events", TwsEvents.class.getSimpleName());
			for (String command : commands.keySet()) {
				Method method = commands.get(command);
				events.help(method.getDeclaringClass().getSimpleName(), command);
			}
			events.helpEnd(name);
		} else if ("actions".equals(name)) {
			events.help("actions", TwsActions.class.getSimpleName());
			events.help("actions", EClient.class.getSimpleName());
			events.help("actions", EClientSocket.class.getSimpleName());
			events.help("actions", "EClientMsgSink");
			events.helpEnd(name);
		} else if ("events".equals(name)) {
			events.help("events", EWrapper.class.getSimpleName());
			events.help("events", TwsEvents.class.getSimpleName());
			events.helpEnd(name);
		} else if (TwsActions.class.getSimpleName().equals(name) || EClient.class.getSimpleName().equals(name)
				|| EClientSocket.class.getSimpleName().equals(name) || "EClientMsgSink".equals(name)) {
			for (String command : commands.keySet()) {
				Method method = commands.get(command);
				if (name.equals(method.getDeclaringClass().getSimpleName())) {
					events.help(name, command);
				}
			}
			events.helpEnd(name);
		} else if (EWrapper.class.getSimpleName().equals(name)) {
			for (Method method : EWrapper.class.getDeclaredMethods()) {
				if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())) {
					events.help(name, method.getName());
				}
			}
			events.helpEnd(name);
		} else if (TwsEvents.class.getSimpleName().equals(name)) {
			for (Method method : TwsEvents.class.getDeclaredMethods()) {
				if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())) {
					events.help(name, method.getName());
				}
			}
			events.helpEnd(name);
		} else if (commands.containsKey(name)) {
			Method method = commands.get(name);
			Type[] types = method.getGenericParameterTypes();
			Parameter[] params = method.getParameters();
			for (int i = 0; i < types.length; i++) {
				events.help(name, params[i].getName(), properties.get(types[i]).getSimpleName());
			}
			events.helpEnd(name);
		} else {
			for (PropertyType ptype : properties.values()) {
				if (ptype.getSimpleName().equals(name)) {
					Object[] values = ptype.getValues();
					if (values == null) {
						for (Entry<String, PropertyType> e : ptype.getProperties().entrySet()) {
							Object default_value = ptype.getDefaultValue(e.getKey());
							Type[] types = new Type[] { String.class, String.class, String.class, ptype.getJavaType() };
							out.println("help", types, name, e.getKey(), e.getValue().getSimpleName(), default_value);
						}
					} else {
						for (Object value : values) {
							out.println("help", new Type[] {String.class, ptype.getJavaType()}, name, value);
						}
					}
					events.helpEnd(name);
					return;
				}
			}
			for (Method method : EWrapper.class.getDeclaredMethods()) {
				if (method.getName().equals(name)) {
					Type[] types = method.getGenericParameterTypes();
					Parameter[] params = method.getParameters();
					for (int i = 0; i < types.length; i++) {
						events.help(name, params[i].getName(), properties.get(types[i]).getSimpleName());
					}
					events.helpEnd(name);
					return;
				}
			}
			for (Method method : TwsEvents.class.getDeclaredMethods()) {
				if (method.getName().equals(name)) {
					Type[] types = method.getGenericParameterTypes();
					Parameter[] params = method.getParameters();
					for (int i = 0; i < types.length; i++) {
						events.help(name, params[i].getName(), properties.get(types[i]).getSimpleName());
					}
					events.helpEnd(name);
					return;
				}
			}
			events.error(name + "?");
		}
	}

	protected EClient getEClient() {
		return client;
	}

	protected void setHelpSchema(Map<String, Method> commands, Map<Type, PropertyType> properties) {
		this.commands = commands;
		this.properties = properties;
	}

}
