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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertyType {
	private final Logger logger = Logger.getLogger(PropertyType.class.getName());
	private final Type type;
	private final Map<String, Method> getters = new TreeMap<>();
	private final Map<String, Method> setters = new TreeMap<>();
	private final Map<String, PropertyType> properties = new TreeMap<>();
	private PropertyType keyType;
	private PropertyType componentType;
	private Object defaultObject;

	public PropertyType(Type type) {
		this.type = type;
		if (isArray()) {
			componentType = new PropertyType(((Class<?>) type).getComponentType());
		} else if (isList() && type instanceof ParameterizedType) {
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[0]);
		} else if (isSet() && type instanceof ParameterizedType) {
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[0]);
		} else if (isMap() && type instanceof ParameterizedType) {
			keyType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[0]);
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[1]);
		} else if (isEntry() && type instanceof ParameterizedType) {
			keyType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[0]);
			componentType = new PropertyType(((ParameterizedType) type).getActualTypeArguments()[1]);
		} else if (isList() || isSet()) {
			componentType = new PropertyType(Object.class);
		} else if (isMap() || isEntry()) {
			keyType = new PropertyType(Object.class);
			componentType = new PropertyType(Object.class);
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
				if (method.getParameterTypes().length == 1 && getter != null && method.getReturnType() == Void.TYPE
						&& Modifier.isPublic(method.getModifiers())) {
					if (getter.getReturnType() == method.getParameterTypes()[0]) {
						setters.put(name, method);
					} else if (!setters.containsKey(name) && getter.getReturnType().isEnum()
							&& Integer.TYPE.equals(method.getParameterTypes()[0])) {
						setters.put(name, method);
					}
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
			if (!Modifier.isAbstract(((Class<?>) type).getModifiers())) {
				for (Constructor<?> c : ((Class<?>) type).getConstructors()) {
					if (c.getParameterTypes().length == 0 && Modifier.isPublic(c.getModifiers())) {
						try {
							defaultObject = c.newInstance();
						} catch (Exception e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
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
		} else if (isList() || isArray() || isSet()) {
			return "[" + getComponentType().getSimpleName() + "]";
		} else if (isMap()) {
			return "{String:" + getComponentType().getSimpleName() + "}";
		} else if (isEntry()) {
			return "{key:String,value:" + getComponentType().getSimpleName() + "}";
		} else {
			return type.getTypeName();
		}
	}

	public boolean isString() {
		return String.class.equals(type);
	}

	public boolean isPrimitive() {
		return type instanceof Class && ((Class<?>) type).isPrimitive();
	}

	public boolean isEnum() {
		return type instanceof Class && ((Class<?>) type).isEnum();
	}

	public boolean isList() {
		return List.class.equals(type)
				|| type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(List.class);
	}

	public boolean isArray() {
		return type instanceof Class && ((Class<?>) type).isArray();
	}

	public boolean isMap() {
		return Map.class.equals(type)
				|| type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(Map.class);
	}

	public boolean isEntry() {
		return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(Map.Entry.class);
	}

	public boolean isSet() {
		return Set.class.equals(type)
				|| type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(Set.class);
	}

	public PropertyType getKeyType() {
		return keyType;
	}

	public PropertyType getComponentType() {
		return componentType;
	}

	public Map<String, PropertyType> getProperties() {
		return properties;
	}

	public Object getDefaultValue(String property) throws IllegalAccessException, InvocationTargetException {
		if (defaultObject == null || !getters.containsKey(property))
			return null;
		return getGetterMethod(property).invoke(defaultObject);
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
