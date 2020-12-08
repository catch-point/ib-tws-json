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

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Printer {
	private final PrintStream out;
	private final Serializer serializer = new Serializer();
	private final Map<Type, PropertyType> types = new HashMap<>();

	public Printer(PrintStream out) {
		this.out = out;
	}

	public void println(String command, String arg) {
		try {
			println(command, new Object[] { arg });
		} catch (IllegalAccessException cause) {
			throw new AssertionError("unexpected", cause);
		} catch (InvocationTargetException cause) {
			throw new AssertionError("unexpected", cause);
		}
	}

	public void println(String command, Number arg) {
		try {
			println(command, new Object[] { arg });
		} catch (IllegalAccessException cause) {
			throw new AssertionError("unexpected", cause);
		} catch (InvocationTargetException cause) {
			throw new AssertionError("unexpected", cause);
		}
	}

	public void println(String command, Boolean arg) {
		try {
			println(command, new Object[] { arg });
		} catch (IllegalAccessException cause) {
			throw new AssertionError("unexpected", cause);
		} catch (InvocationTargetException cause) {
			throw new AssertionError("unexpected", cause);
		}
	}

	public void println(String command, Object... args) throws IllegalAccessException, InvocationTargetException {
		if (args == null || args.length < 1) {
			out.println(command);
		} else {
			String[] json = new String[args.length];
			for (int i = 0; i < args.length; i++) {
				json[i] = serializer.serialize(args[i], getPropertyType(args[i].getClass()));
			}
			StringBuilder sb = new StringBuilder();
			sb.append(command);
			for (String str : json) {
				sb.append('\t').append(str);
			}
			out.println(sb.toString());
		}
	}

	public void println(String command, Type[] types, Object... args)
			throws IllegalAccessException, InvocationTargetException {
		if (args == null || args.length < 1) {
			out.println(command);
		} else {
			String[] json = new String[args.length];
			for (int i = 0; i < args.length; i++) {
				json[i] = serializer.serialize(args[i], getPropertyType(types[i]));
			}
			StringBuilder sb = new StringBuilder();
			sb.append(command);
			for (String str : json) {
				sb.append('\t').append(str);
			}
			out.println(sb.toString());
		}
	}

	public synchronized PropertyType getPropertyType(Type type) {
		if (!types.containsKey(type)) {
			types.put(type, new PropertyType(type));
		}
		return types.get(type);
	}
}
