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
import java.util.logging.Logger;

import com.ib.client.EClient;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;

/**
 * Implements the actions that control TWS
 *
 * @author James Leigh
 *
 */
public class TwsSocketActions implements TwsActions {
	private final Logger logger = Logger.getLogger(TwsSocketActions.class.getName());
	private final TwsEvents events;
	private final Printer out;
	private final EJavaSignal signal = new EJavaSignal();
	private EClientSocket client;
	private Thread signalThread;
	private Map<String, Method> commands;
	private Map<Type, PropertyType> properties;
	private String tws_host;
	private int tws_port;

	public TwsSocketActions(Printer out) {
		this.out = out;
		EWrapper wrapper = EWrapperHandler.newInstance(out);
		this.client = new EClientSocket(wrapper, signal);
		this.events = TwsEventsHandler.newInstance(out);
	}

	protected void setRemoteAddress(String tws_host, int tws_port) {
		this.tws_host = tws_host;
		this.tws_port = tws_port;
	}

	public void sleep(Long ms) throws InterruptedException {
		Thread.sleep(ms == null ? 0 : ms);
	}

	public synchronized void eConnect(int clientId, boolean extraAuth)
			throws InterruptedException {
		if (getEClient().isConnected()) {
			getEClient().eDisconnect();
		}
		if (signalThread != null && signalThread.isAlive()) {
			signalThread.join();
		}
		if (tws_port > 0 && tws_host != null) {
			((EClientSocket) getEClient()).eConnect(tws_host, tws_port, clientId, extraAuth);
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
						logger.severe(e.getMessage());
						events.error(e.getMessage());
					}
				}
			});
			signalThread.start();
		} else {
			events.error("TWS API is not ready");
		}
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
