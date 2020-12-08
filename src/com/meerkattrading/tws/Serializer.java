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

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import com.ib.client.ContractCondition;
import com.ib.client.ExecutionCondition;
import com.ib.client.MarginCondition;
import com.ib.client.OperatorCondition;
import com.ib.client.OrderCondition;
import com.ib.client.PercentChangeCondition;
import com.ib.client.PriceCondition;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TimeCondition;
import com.ib.client.VolumeCondition;

public class Serializer {
	private final JsonWriterFactory factory = Json.createWriterFactory(Collections.emptyMap());

	public String serialize(Object object, PropertyType type) throws IllegalAccessException, InvocationTargetException {
		JsonValue value = objectToJsonValue(object, type);
		StringWriter writer = new StringWriter();
		JsonWriter jwriter = factory.createWriter(writer);
		jwriter.write(value);
		return writer.toString();
	}

	private JsonValue objectToJsonValue(Object object, PropertyType type)
			throws IllegalAccessException, InvocationTargetException {
		if (object == null) {
			return JsonValue.NULL;
		} else if (object instanceof Boolean && (((Boolean) object).booleanValue())) {
			return JsonValue.TRUE;
		} else if (object instanceof Boolean && !(((Boolean) object).booleanValue())) {
			return JsonValue.FALSE;
		} else if (object instanceof BigDecimal) {
			return Json.createValue((BigDecimal) object);
		} else if (object instanceof BigInteger) {
			return Json.createValue((BigInteger) object);
		} else if (object instanceof Double) {
			return Json.createValue((Double) object);
		} else if (object instanceof Integer) {
			return Json.createValue((Integer) object);
		} else if (object instanceof Long) {
			return Json.createValue((Long) object);
		} else if (object instanceof String) {
			return stringToJson((String) object);
		} else if (object instanceof Map.Entry<?, ?>) {
			return entryToJson((Map.Entry<?, ?>) object, type);
		} else if (type.isArray()) {
			return arrayToJson((Object[]) object, type);
		} else if (type.isList() || type.isSet()) {
			return collectionToJson((Collection<?>) object, type);
		} else if (type.isMap()) {
			return mapToJson((Map<?, ?>) object, type);
		} else if (type.getJavaType() instanceof Class<?> && ((Class<?>) type.getJavaType()).isEnum()) {
			return enumToJson(object);
		} else if (object instanceof OrderCondition) {
			return orderConditionToJson((OrderCondition) object);
		} else if (object instanceof TagValue) {
			return tagValueToJson((TagValue) object);
		} else if (object instanceof SoftDollarTier) {
			return softDollarTierToJson((SoftDollarTier) object);
		} else if (object instanceof Exception) {
			return exceptionToJson((Exception) object);
		} else {
			return javaObjectToJson(object, type);
		}
	}

	private JsonValue stringToJson(String object) {
		if (object == null) {
			return JsonValue.NULL;
		} else {
			return Json.createValue(object);
		}
	}

	private JsonValue entryToJson(Map.Entry<?, ?> entry, PropertyType type)
			throws IllegalAccessException, InvocationTargetException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("key", stringToJson(entry.getKey().toString()));
		builder.add("value", objectToJsonValue(entry.getKey().toString(), type.getComponentType()));
		return builder.build();
	}

	private JsonValue arrayToJson(Object[] object, PropertyType type)
			throws IllegalAccessException, InvocationTargetException {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Object obj : object) {
			builder.add(objectToJsonValue(obj, type.getComponentType()));
		}
		return builder.build();
	}

	private JsonValue collectionToJson(Collection<?> object, PropertyType type)
			throws IllegalAccessException, InvocationTargetException {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Object obj : object) {
			builder.add(objectToJsonValue(obj, type.getComponentType()));
		}
		return builder.build();
	}

	private JsonValue mapToJson(Map<?, ?> object, PropertyType type)
			throws IllegalAccessException, InvocationTargetException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Map.Entry<?, ?> entry : object.entrySet()) {
			builder.add(entry.getKey().toString(), objectToJsonValue(entry.getValue(), type.getComponentType()));
		}
		return builder.build();
	}

	private JsonValue enumToJson(Object object) {
		return stringToJson(object.toString());
	}

	private JsonValue orderConditionToJson(OrderCondition oc) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("conjunctionConnection", oc.conjunctionConnection());
		if (oc instanceof ExecutionCondition) {
			builder.add("exchange", ((ExecutionCondition) oc).exchange());
			builder.add("secType", ((ExecutionCondition) oc).secType());
			builder.add("symbol", ((ExecutionCondition) oc).symbol());
		}
		if (oc instanceof OperatorCondition) {
			builder.add("isMore", ((OperatorCondition) oc).isMore());
		}
		if (oc instanceof MarginCondition) {
			builder.add("percent", ((MarginCondition) oc).percent());
		}
		if (oc instanceof ContractCondition) {
			builder.add("conId", ((ContractCondition) oc).conId());
			builder.add("exchange", ((ContractCondition) oc).exchange());
		}
		if (oc instanceof PercentChangeCondition) {
			builder.add("changePercent", ((PercentChangeCondition) oc).changePercent());
		}
		if (oc instanceof PriceCondition) {
			builder.add("price", ((PriceCondition) oc).price());
			builder.add("triggerMethod", ((PriceCondition) oc).triggerMethod());
		}
		if (oc instanceof TimeCondition) {
			builder.add("time", ((TimeCondition) oc).time());
		}
		if (oc instanceof VolumeCondition) {
			builder.add("volume", ((VolumeCondition) oc).volume());
		}
		return builder.build();
	}

	private JsonValue tagValueToJson(TagValue object) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("tag", object.m_tag);
		builder.add("value", object.m_value);
		return builder.build();
	}

	private JsonValue softDollarTierToJson(SoftDollarTier object) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("name", object.name());
		builder.add("value", object.value());
		builder.add("displayName", object.toString());
		return builder.build();
	}

	private JsonValue exceptionToJson(Exception exc) {
		return stringToJson(exc.getMessage());
	}

	private JsonValue javaObjectToJson(Object object, PropertyType type)
			throws IllegalAccessException, InvocationTargetException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		Map<String, PropertyType> properties = type.getProperties();
		for (String key : properties.keySet()) {
			Method getter = type.getGetterMethod(key);
			Object value = getter.invoke(object);
			JsonValue jsonValue = objectToJsonValue(value, properties.get(key));
			if (value != null && value != Boolean.FALSE) {
				builder.add(key, jsonValue);
			}
		}
		return builder.build();
	}

}
