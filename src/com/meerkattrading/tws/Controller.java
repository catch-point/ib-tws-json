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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.jline.reader.EndOfFileException;

import com.ib.client.EClient;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;

public class Controller {
	private final Logger logger = Logger.getLogger(Shell.class.getName());
	private final Map<String, Method> commands = new TreeMap<>();
	private final Map<String, PropertyType> properties = new TreeMap<>();
	private Printer out;
	private EJavaSignal signal = new EJavaSignal();
	private EWrapper ewrapper;
	private EClientSocket client;
	private Thread thread;

	public Controller(Printer out) throws IOException {
		this.out = out;
		ewrapper = EWrapperHandler.newInstance(out);
		client = new EClientSocket(ewrapper, signal);
		for (Method method : EClient.class.getDeclaredMethods()) {
			if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())) {
				addCommand(method.getName(), method);
			}
		}
		for (Method method : Controller.class.getDeclaredMethods()) {
			if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())) {
				addCommand(method.getName(), method);
			}
		}
		for (Method method : commands.values()) {
			for (Class<?> type : method.getParameterTypes()) {
				PropertyType propertySet = new PropertyType(type);
				addPropertySet(propertySet);
			}
		}
	}

	public synchronized void connect(String host, int port, int clientId, boolean extraAuth) {
		client.eConnect(host, port, clientId, extraAuth);
		final EReader reader = new EReader(client, signal);

		reader.start();
		// An additional thread is created in this program design to empty the messaging
		// queue
		thread = new Thread(() -> {
			while (client.isConnected()) {
				signal.waitForSignal();
				try {
					reader.processMsgs();
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
			}
		});
		thread.start();
	}

	public synchronized void disconnect() {
		if (client.isConnected()) {
			client.eDisconnect();
		}
	}

	public void eDisconnect() {
		disconnect();
	}

	public void exit() {
		throw new EndOfFileException("exit");
	}

	public void serverVersion() {
		out.println("serverVersion", client.serverVersion());
	}

	public void isConnected() {
		out.println("isConnected", client.isConnected());
	}

	public void connectedHost() {
		out.println("onnectedHost", client.connectedHost());
	}

	public void isUseV100Plus() {
		out.println("isUseV100Plus", client.isUseV100Plus());
	}

	public void optionalCapabilities() {
		out.println("optionalCapabilities", client.optionalCapabilities());
	}

	public void faMsgTypeName(int type) {
		out.println("faMsgTypeName", EClient.faMsgTypeName(type));
	}

	public void getTwsConnectionTime() {
		out.println("getTwsConnectionTime", client.getTwsConnectionTime());
	}

	public void help(String name) throws IllegalAccessException, InvocationTargetException {
		if (name == null || name.length() == 0) {
			for (String command : commands.keySet()) {
				out.println("help", command);
			}
		} else if (commands.containsKey(name)) {
			Method method = commands.get(name);
			Class<?>[] types = method.getParameterTypes();
			Object[] args = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				args[i] = types[i].getSimpleName();
			}
			out.println("help", args);
		} else if (properties.containsKey(name)) {
			PropertyType set = properties.get(name);
			Object[] values = set.getValues();
			if (values == null) {
				for (Entry<String, PropertyType> e : set.getProperties().entrySet()) {
					out.println("help", e.getKey(), e.getValue().getSimpleName());
				}
			} else {
				out.println("help", values);
			}
		} else {
			out.println("error", name + "?");
		}
	}

	public PropertyType[] getParameterTypes(String command) throws NoSuchMethodException {
		if (commands.containsKey(command)) {
			Method method = commands.get(command);
			Type[] types = method.getGenericParameterTypes();
			PropertyType[] ptypes = new PropertyType[types.length];
			for (int i = 0; i < types.length; i++) {
				if (types[i] instanceof Class<?>) {
					ptypes[i] = properties.get(((Class<?>) types[i]).getSimpleName());
				} else {
					assert types[i] instanceof ParameterizedType
							&& ((ParameterizedType) types[i]).getRawType() == List.class;
					Type ctype = ((ParameterizedType) types[i]).getActualTypeArguments()[0];
					assert ctype instanceof Class<?>;
					ptypes[i] = properties.get("[" + ((Class<?>) ctype).getSimpleName() + "]");
				}
			}
			return ptypes;
		} else {
			throw new NoSuchMethodException(command);
		}
	}

	public Object invoke(String command, Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		if (commands.containsKey(command)) {
			Method method = commands.get(command);
			if (method.getDeclaringClass().isInstance(this)) {
				return method.invoke(this, args);
			} else {
				return method.invoke(client, args);
			}
		} else {
			throw new NoSuchMethodException(command);
		}
	}

	private void addCommand(String command, Method method) {
		if (!commands.containsKey(command)) {
			commands.put(command, method);
		} else if (method.getParameterCount() > commands.get(command).getParameterCount()) {
			logger.fine("Ignoring method " + commands.get(command).toString());
			commands.put(command, method);
		} else {
			logger.fine("Ignoring method " + method.toString());
		}
	}

	private void addPropertySet(PropertyType propertySet) {
		if (!properties.containsKey(propertySet.getSimpleName())) {
			properties.put(propertySet.getSimpleName(), propertySet);
			if (propertySet.isList()) {
				addPropertySet(propertySet.getComponentType());
			} else {
				for (PropertyType pset : propertySet.getProperties().values()) {
					addPropertySet(pset);
				}
			}
		}
	}
}
