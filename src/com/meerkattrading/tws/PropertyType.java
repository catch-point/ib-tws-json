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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PropertyType {
	private final Type type;
	private final Map<String, Method> getters = new TreeMap<>();
	private final Map<String, Method> setters = new TreeMap<>();
	private final Map<String, PropertyType> properties = new TreeMap<>();
	private PropertyType componentType;

	public PropertyType(Type type) {
		this.type = type;
		if (isArray()) {
			componentType = new PropertyType(((Class<?>)type).getComponentType());
		} else if (isList()) {
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[0]);
		} else if (isSet()) {
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[0]);
		} else if (isMap()) {
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[1]);
		} else if (isEntry()) {
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[1]);
		} else if (type instanceof Class<?>) {
			Method[] methods = ((Class<?>) type).getDeclaredMethods();
			for (Method method : methods) {
				if (method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE
						&& Modifier.isPublic(method.getModifiers())) {
					getters.put(method.getName(), method);
				}
			}
			for (Method method : ((Class<?>) type).getDeclaredMethods()) {
				String name = method.getName();
				Method getter = getters.get(name);
				if (method.getParameterTypes().length == 1 && getter != null
						&& getter.getReturnType() == method.getParameterTypes()[0]
						&& method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())) {
					setters.put(name, method);
				}
			}
			Iterator<String> giter = getters.keySet().iterator();
			while (giter.hasNext()) {
				if (!setters.containsKey(giter.next())) {
					giter.remove();
				}
			}
			for (String name : getters.keySet()) {
				properties.put(name, new PropertyType(getters.get(name).getGenericReturnType()));
			}
		} else {
			throw new AssertionError("Unhandled property type " + type);
		}
	}

	public String toString() {
		return getSimpleName() + getProperties().keySet().toString();
	}

	public Type getJavaType() {
		return type;
	}

	public String getSimpleName() {
		if (type instanceof Class) {
			return ((Class<?>) type).getSimpleName();
		} else if (isList()) {
			return "[" + getComponentType().getSimpleName() + "]";
		} else {
			return type.getTypeName();
		}
	}

	public boolean isList() {
		return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(List.class);
	}

	public boolean isArray() {
		return type instanceof Class && ((Class<?>) type).isArray();
	}

	public boolean isMap() {
		return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(Map.class);
	}

	public boolean isEntry() {
		return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(Map.Entry.class);
	}

	public boolean isSet() {
		return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(Set.class);
	}

	public PropertyType getComponentType() {
		return componentType;
	}

	public Map<String, PropertyType> getProperties() {
		return properties;
	}

	public Method getGetterMethod(String property) {
		return getters.get(property);
	}

	public Method getSetterMethod(String property) {
		return setters.get(property);
	}

	public Object[] getValues() {
		if (!(type instanceof Class) || !((Class<?>) type).isEnum())
			return null;
		return ((Class<?>) type).getEnumConstants();
	}
}
