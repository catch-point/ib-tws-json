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
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	private final Map<Type, PropertyType> properties = new HashMap<>();
	private final EJavaSignal signal = new EJavaSignal();
	private Printer out;
	private EClientSocket client;
	private Thread thread;

	public Controller(Printer out) throws IOException {
		this.out = out;
		EWrapper wrapper = EWrapperHandler.newInstance(out);
		this.client = new EClientSocket(wrapper, getSignal());
		for (Method method : Controller.class.getDeclaredMethods()) {
			if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())) {
				addCommand(method.getName(), method);
			}
		}
		Object client = getClient();
		for (Class<?> face : getAllInterfaces(client.getClass())) {
			for (Method method : face.getMethods()) {
				if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())
						&& !Object.class.equals(method.getDeclaringClass())) {
					addCommand(method.getName(), method);
				}
			}
		}
		for (Method method : client.getClass().getMethods()) {
			if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())
					&& !Object.class.equals(method.getDeclaringClass())) {
				addCommand(method.getName(), method);
			}
		}
		for (Method method : commands.values()) {
			for (Type type : method.getGenericParameterTypes()) {
				addPropertyType(new PropertyType(type));
			}
		}
	}

	private static Collection<Class<?>> getAllInterfaces(Class<?> type) {
		Collection<Class<?>> array = new HashSet<Class<?>>();
		if (type == null) return array;
		array.addAll(getAllInterfaces(type.getSuperclass()));
		array.addAll(Arrays.asList(type.getInterfaces()));
		for (Class<?> face : type.getInterfaces()) {
			array.addAll(getAllInterfaces(face));
		}
		return array;
	}

	public synchronized void eConnect(String host, int port, int clientId, boolean extraAuth) {
		if (getEClient().isConnected()) {
			getEClient().eDisconnect();
		}
		((EClientSocket) getEClient()).eConnect(host, port, clientId, extraAuth);
		final EReader reader = new EReader((EClientSocket) getEClient(), getSignal());

		reader.start();
		// An additional thread is created in this program design to empty the messaging
		// queue
		thread = new Thread(() -> {
			while (getEClient().isConnected()) {
				getSignal().waitForSignal();
				try {
					reader.processMsgs();
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
			}
		});
		thread.start();
	}

	public synchronized void eDisconnect() {
		if (getEClient().isConnected()) {
			getEClient().eDisconnect();
		}
	}

	public void exit() {
		throw new EndOfFileException("exit");
	}

	public void serverVersion() {
		out.println("serverVersion", getEClient().serverVersion());
	}

	public void isConnected() {
		out.println("isConnected", getEClient().isConnected());
	}

	public void connectedHost() {
		out.println("onnectedHost", getEClient().connectedHost());
	}

	public void isUseV100Plus() {
		out.println("isUseV100Plus", getEClient().isUseV100Plus());
	}

	public void optionalCapabilities() {
		out.println("optionalCapabilities", getEClient().optionalCapabilities());
	}

	public void faMsgTypeName(int type) {
		out.println("faMsgTypeName", EClient.faMsgTypeName(type));
	}

	public void getTwsConnectionTime() {
		out.println("getTwsConnectionTime", getEClient().getTwsConnectionTime());
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
		} else {
			for (Type type : properties.keySet()) {
				if (type.getTypeName().contains(name)) {
					PropertyType set = properties.get(type);
					Object[] values = set.getValues();
					if (values == null) {
						for (Entry<String, PropertyType> e : set.getProperties().entrySet()) {
							out.println("help", e.getKey(), e.getValue().getSimpleName());
						}
					} else {
						out.println("help", values);
					}
					return; // found
				}
			}
			out.println("error", name + "?");
		}
	}

	public PropertyType[] getParameterTypes(String command) throws NoSuchMethodException {
		if (commands.containsKey(command)) {
			Method method = commands.get(command);
			Type[] types = method.getGenericParameterTypes();
			PropertyType[] ptypes = new PropertyType[types.length];
			for (int i = 0; i < types.length; i++) {
				ptypes[i] = properties.get(types[i]);
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
				return method.invoke(getClient(), args);
			}
		} else {
			throw new NoSuchMethodException(command);
		}
	}

	protected EClient getEClient() {
		return (EClient) client;
	}

	protected Object getClient() {
		return client;
	}

	protected Object getWrapper() {
		return getEClient().wrapper();
	}

	protected EJavaSignal getSignal() {
		return signal;
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

	private void addPropertyType(PropertyType ptype) {
		if (ptype != null && !properties.containsKey(ptype.getJavaType())) {
			properties.put(ptype.getJavaType(), ptype);
			if (ptype.isList() || ptype.isSet() || ptype.isArray() || ptype.isEntry()
					|| ptype.isMap()) {
				addPropertyType(ptype.getComponentType());
			} else {
				for (PropertyType pset : ptype.getProperties().values()) {
					addPropertyType(pset);
				}
			}
		}
	}
}
