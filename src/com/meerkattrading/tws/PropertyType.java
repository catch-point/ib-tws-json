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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ib.client.ContractCondition;
import com.ib.client.ExecutionCondition;
import com.ib.client.HistogramEntry;
import com.ib.client.MarginCondition;
import com.ib.client.OperatorCondition;
import com.ib.client.OrderCondition;
import com.ib.client.PercentChangeCondition;
import com.ib.client.PriceCondition;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TimeCondition;
import com.ib.client.VolumeCondition;

/**
 * Determines the object properties for a Java type
 *
 * @author James Leigh
 *
 */
public class PropertyType {
	private static final Set<String> GETTERS = new LinkedHashSet<String>() {
		private static final long serialVersionUID = 2321538364094288074L;

		{
			add("public double com.ib.client.Bar.close()");
			add("public int com.ib.client.Bar.count()");
			add("public double com.ib.client.Bar.high()");
			add("public double com.ib.client.Bar.low()");
			add("public double com.ib.client.Bar.open()");
			add("public java.lang.String com.ib.client.Bar.time()");
			add("public long com.ib.client.Bar.volume()");
			add("public double com.ib.client.Bar.wap()");
			add("public double com.ib.client.HistoricalTick.price()");
			add("public long com.ib.client.HistoricalTick.size()");
			add("public long com.ib.client.HistoricalTick.time()");
			add("public double com.ib.client.HistoricalTickBidAsk.priceAsk()");
			add("public double com.ib.client.HistoricalTickBidAsk.priceBid()");
			add("public long com.ib.client.HistoricalTickBidAsk.sizeAsk()");
			add("public long com.ib.client.HistoricalTickBidAsk.sizeBid()");
			add("public com.ib.client.TickAttribBidAsk com.ib.client.HistoricalTickBidAsk.tickAttribBidAsk()");
			add("public long com.ib.client.HistoricalTickBidAsk.time()");
			add("public java.lang.String com.ib.client.HistoricalTickLast.exchange()");
			add("public double com.ib.client.HistoricalTickLast.price()");
			add("public long com.ib.client.HistoricalTickLast.size()");
			add("public java.lang.String com.ib.client.HistoricalTickLast.specialConditions()");
			add("public com.ib.client.TickAttribLast com.ib.client.HistoricalTickLast.tickAttribLast()");
			add("public long com.ib.client.HistoricalTickLast.time()");
			add("public com.ib.client.OrderConditionType com.ib.client.OrderCondition.type()");
			add("public java.lang.String com.ib.client.Order.getAlgoStrategy()");
			add("public java.lang.String com.ib.client.Order.getOrderType()");
			add("public int com.ib.client.Order.getOcaType()");
		}
	};
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
			List<Method> methods = getPublicMethodsOf((Class<?>) type);
			for (Method method : methods) {
				if (method.getName().startsWith("get") && GETTERS.contains(method.toString())) {
					String name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
					getters.put(name, method);
				} else if (method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE) {
					if (!getters.containsKey(method.getName())) {
						getters.put(method.getName(), method);
					}
				}
			}
			for (Method method : methods) {
				String name = method.getName();
				Method getter = getters.get(name);
				if (method.getParameterTypes().length == 1 && getter != null && method.getReturnType() == Void.TYPE) {
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
				String getter = giter.next();
				if (!setters.containsKey(getter) && !GETTERS.contains(getters.get(getter).toString())) {
					giter.remove();
				}
			}
			properties.putAll(getPropertiesFromGetters((Class<?>) type));
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
		if (getProperties().isEmpty())
			return getSimpleName();
		else
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
			return "{" + getComponentType().getSimpleName() + "}";
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
		if (defaultObject == null || "action".equals(property) || "orderType".equals(property)
				|| !getters.containsKey(property))
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

	private List<Method> getPublicMethodsOf(Class<?> type) {
		Method[] methods = type.getMethods();
		List<Method> list = new ArrayList<>(Arrays.asList(methods));
		Iterator<Method> iter = list.iterator();
		while (iter.hasNext()) {
			Method method = iter.next();
			if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().equals(Object.class)) {
				iter.remove();
			}
		}
		if (OrderCondition.class.equals(type)) {
			list.addAll(getPublicMethodsOf(ExecutionCondition.class));
			list.addAll(getPublicMethodsOf(OperatorCondition.class));
		}
		if (OperatorCondition.class.equals(type)) {
			list.addAll(getPublicMethodsOf(ContractCondition.class));
			list.addAll(getPublicMethodsOf(MarginCondition.class));
			list.addAll(getPublicMethodsOf(TimeCondition.class));
		}
		if (ContractCondition.class.equals(type)) {
			list.addAll(getPublicMethodsOf(PercentChangeCondition.class));
			list.addAll(getPublicMethodsOf(PriceCondition.class));
			list.addAll(getPublicMethodsOf(VolumeCondition.class));
		}
		return list;
	}

	private Map<String, PropertyType> getPropertiesFromGetters(Class<?> type) {
		Map<String, PropertyType> properties = new TreeMap<>();
		for (String name : getters.keySet()) {
			properties.put(name, new PropertyType(getters.get(name).getGenericReturnType()));
		}
		if (TagValue.class.isAssignableFrom(type)) {
			properties.put("tag", new PropertyType(String.class));
			properties.put("value", new PropertyType(String.class));
		}
		if (HistogramEntry.class.isAssignableFrom(type)) {
			properties.put("price", new PropertyType(Double.TYPE));
			properties.put("size", new PropertyType(Long.TYPE));
		}
		if (SoftDollarTier.class.isAssignableFrom(type)) {
			properties.put("dispalyName", new PropertyType(String.class));
		}
		return properties;
	}
}
