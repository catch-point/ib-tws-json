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
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.ib.client.EWrapper;

/**
 * Executes all the commands from the shell.
 *
 * @author James Leigh
 *
 */
public class Invoker {
	private final Logger logger = Logger.getLogger(Invoker.class.getName());
	private final Map<String, Method> commands = new TreeMap<>();
	private final Map<Type, PropertyType> properties = new HashMap<>();
	private final TwsSocketActions actions;

	public Invoker(Printer out) throws IOException {
		this(new TwsSocketActions(out), out);
	}

	public Invoker(TwsSocketActions client, Printer out) throws IOException {
		this.actions = client;
		for (Class<?> face : getAllInterfaces(getActions().getClass(), true)) {
			for (Method method : face.getMethods()) {
				if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())
						&& !Object.class.equals(method.getDeclaringClass())) {
					addCommand(method.getName(), method);
				}
			}
		}
		for (Class<?> face : getAllInterfaces(getClient().getClass(), true)) {
			for (Method method : face.getMethods()) {
				if (method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())
						&& !Object.class.equals(method.getDeclaringClass())) {
					addCommand(method.getName(), method);
				}
			}
		}
		for (Method method : commands.values()) {
			for (Type type : method.getGenericParameterTypes()) {
				addPropertyType(new PropertyType(type));
			}
		}
		for (Method method : EWrapper.class.getDeclaredMethods()) {
			for (Type type : method.getGenericParameterTypes()) {
				addPropertyType(new PropertyType(type));
			}
		}
		for (Method method : TwsEvents.class.getDeclaredMethods()) {
			for (Type type : method.getGenericParameterTypes()) {
				addPropertyType(new PropertyType(type));
			}
		}
		actions.setHelpSchema(commands, properties);
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
			Object client = getClient();
			Object actions = getActions();
			if (method.getDeclaringClass().isInstance(client)) {
				return method.invoke(client, args);
			} else if (method.getDeclaringClass().isInstance(actions)) {
				return method.invoke(actions, args);
			} else {
				throw new AssertionError("Unknown command: " + command);
			}
		} else {
			throw new NoSuchMethodException(command);
		}
	}

	public void exit() throws EOFException {
		actions.exit();
	}

	protected Object getActions() {
		return actions;
	}

	protected Object getClient() {
		return actions.getEClient();
	}

	private static Collection<Class<?>> getAllInterfaces(Class<?> type, boolean includeSelf) {
		Collection<Class<?>> array = new LinkedHashSet<Class<?>>();
		if (type == null)
			return array;
		array.addAll(getAllInterfaces(type.getSuperclass(), false));
		for (Class<?> face : type.getInterfaces()) {
			array.addAll(getAllInterfaces(face, true));
		}
		if (includeSelf) {
			array.add(type);
		}
		return array;
	}

	private void addCommand(String command, Method method) {
		if (!commands.containsKey(command)) {
			commands.put(command, method);
		} else if (method.getParameterCount() > commands.get(command).getParameterCount()
				&& method.getDeclaringClass().equals(commands.get(command).getDeclaringClass())) {
			logger.fine("Ignoring method " + commands.get(command).toString());
			commands.put(command, method);
		} else {
			logger.fine("Ignoring method " + method.toString());
		}
	}

	private void addPropertyType(PropertyType ptype) {
		if (ptype != null && !properties.containsKey(ptype.getJavaType())) {
			properties.put(ptype.getJavaType(), ptype);
			if (ptype.isList() || ptype.isSet() || ptype.isArray() || ptype.isEntry() || ptype.isMap()) {
				addPropertyType(ptype.getComponentType());
			} else {
				for (PropertyType pset : ptype.getProperties().values()) {
					addPropertyType(pset);
				}
			}
		}
	}
}
