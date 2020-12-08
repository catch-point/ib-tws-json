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

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;

import com.ib.client.ContractCondition;
import com.ib.client.ExecutionCondition;
import com.ib.client.MarginCondition;
import com.ib.client.OperatorCondition;
import com.ib.client.OrderCondition;
import com.ib.client.OrderConditionType;
import com.ib.client.PercentChangeCondition;
import com.ib.client.PriceCondition;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TimeCondition;
import com.ib.client.VolumeCondition;

public class Deserializer {
	public Object deserialize(String json, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		JsonValue obj = parse(json);
		return jsonToJava(obj, type);
	}

	private JsonValue parse(String json) {
		try {
			return Json.createReader(new StringReader(json)).readValue();
		} catch (JsonParsingException e) {
			return Json.createValue(json);
		}
	}

	private Object jsonToJava(JsonValue obj, PropertyType ptype)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (ptype.isList()) {
			return jsonToListOf(obj, ptype.getComponentType());
		} else {
			Class<?> type = (Class<?>) ptype.getJavaType();
			if (type == Boolean.TYPE || type == Boolean.class) {
				return jsonToBoolean(obj);
			} else if (type.isAssignableFrom(Object.class)) {
				return jsonToObject(obj);
			} else if (type.isAssignableFrom(String.class)) {
				return jsonToString(obj);
			} else if (type.isAssignableFrom(Number.class)) {
				return jsonToNumber(obj);
			} else if (type.isAssignableFrom(BigDecimal.class)) {
				return jsonToBigDecimal(obj);
			} else if (type.isAssignableFrom(BigInteger.class)) {
				return jsonToBigInteger(obj);
			} else if (type == Double.TYPE || type.isAssignableFrom(Double.class)) {
				return jsonToDouble(obj);
			} else if (type == Integer.TYPE || type.isAssignableFrom(Integer.class)) {
				return jsonToInteger(obj);
			} else if (type == Long.TYPE || type.isAssignableFrom(Long.class)) {
				return jsonToLong(obj);
			} else if (type.isAssignableFrom(Object[].class)) {
				return jsonToObjectArray(obj);
			} else if (type.isAssignableFrom(List.class)) {
				return jsonToList(obj);
			} else if (type.isEnum()) {
				return jsonToEnum(obj, type);
			} else if (type.isAssignableFrom(OrderCondition.class)) {
				return jsonToOrderCondition(obj);
			} else if (type.isAssignableFrom(TagValue.class)) {
				return jsonToTagValue(obj);
			} else if (type.isAssignableFrom(SoftDollarTier.class)) {
				return jsonToSoftDollarTier(obj);
			} else {
				return jsonToJavaObject(obj, ptype);
			}
		}
	}

	private List<?> jsonToListOf(JsonValue obj, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		case ARRAY:
			Object[] array = new Object[obj.asJsonArray().size()];
			for (int i = 0; i < array.length; i++) {
				array[i] = jsonToJava(obj.asJsonArray().get(i), type);
			}
			return Arrays.asList(array);
		default:
			Object[] ar = new Object[1];
			ar[0] = jsonToJava(obj, type);
			return Arrays.asList(ar);
		}
	}

	private boolean jsonToBoolean(JsonValue obj) {
		if (obj == null) return false;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return Boolean.FALSE;
		case STRING:
			return ((JsonString) obj).getString().length() > 0;
		default:
			return Boolean.TRUE;
		}
	}

	private Object jsonToObject(JsonValue obj) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		case FALSE:
			return Boolean.FALSE;
		case TRUE:
			return Boolean.TRUE;
		case STRING:
			return ((JsonString) obj).getString();
		case NUMBER:
			return ((JsonNumber) obj).numberValue();
		case ARRAY:
			Object[] array = new Object[obj.asJsonArray().size()];
			for (int i = 0; i < array.length; i++) {
				array[i] = jsonToObject(obj.asJsonArray().get(i));
			}
			return array;
		case OBJECT:
		default:
			return obj;
		}
	}

	private String jsonToString(JsonValue obj) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		case STRING:
			return ((JsonString) obj).getString();
		default:
			return obj.toString();
		}
	}

	private Number jsonToNumber(JsonValue obj) {
		if (obj == null) return 0;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return 0;
		case TRUE:
			return 1;
		case NUMBER:
			return ((JsonNumber) obj).numberValue();
		case STRING:
			return new BigDecimal(((JsonString) obj).getString());
		default:
			return new BigDecimal(obj.toString());
		}
	}

	private BigDecimal jsonToBigDecimal(JsonValue obj) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return BigDecimal.ZERO;
		case TRUE:
			return BigDecimal.ONE;
		case NUMBER:
			return ((JsonNumber) obj).bigDecimalValue();
		case STRING:
			return new BigDecimal(((JsonString) obj).getString());
		default:
			return new BigDecimal(obj.toString());
		}
	}

	private BigInteger jsonToBigInteger(JsonValue obj) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return BigInteger.ZERO;
		case TRUE:
			return BigInteger.ONE;
		case NUMBER:
			return ((JsonNumber) obj).bigIntegerValueExact();
		case STRING:
			return new BigInteger(((JsonString) obj).getString());
		default:
			return new BigInteger(obj.toString());
		}
	}

	private double jsonToDouble(JsonValue obj) {
		if (obj == null) return 0;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return 0;
		case TRUE:
			return 1;
		case NUMBER:
			return ((JsonNumber) obj).doubleValue();
		case STRING:
			return Double.valueOf(((JsonString) obj).getString());
		default:
			return Double.valueOf(obj.toString());
		}
	}

	private int jsonToInteger(JsonValue obj) {
		if (obj == null) return 0;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return 0;
		case TRUE:
			return 1;
		case NUMBER:
			return ((JsonNumber) obj).intValue();
		case STRING:
			return Integer.valueOf(((JsonString) obj).getString());
		default:
			return Integer.valueOf(obj.toString());
		}
	}

	private long jsonToLong(JsonValue obj) {
		if (obj == null) return 0;
		switch (obj.getValueType()) {
		case NULL:
		case FALSE:
			return 0;
		case TRUE:
			return 1;
		case NUMBER:
			return ((JsonNumber) obj).longValueExact();
		case STRING:
			return Long.valueOf(((JsonString) obj).getString());
		default:
			return Long.valueOf(obj.toString());
		}
	}

	private Object[] jsonToObjectArray(JsonValue obj) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		case ARRAY:
			Object[] array = new Object[obj.asJsonArray().size()];
			for (int i = 0; i < array.length; i++) {
				array[i] = jsonToObject(obj.asJsonArray().get(i));
			}
			return array;
		default:
			Object[] ar = new Object[1];
			ar[0] = jsonToObject(obj);
			return ar;
		}
	}

	private List<?> jsonToList(JsonValue obj) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		default:
			return Arrays.asList(jsonToObjectArray(obj));
		}
	}

	private Object jsonToEnum(JsonValue obj, Class<?> enum_type) {
		if (obj == null) return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		default:
			return Enum.valueOf((Class<? extends Enum>) enum_type, jsonToString(obj));
		}
	}

	private OrderCondition jsonToOrderCondition(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		OrderConditionType type = OrderConditionType.valueOf(o.getString("type"));
		OrderCondition oc = OrderCondition.create(type);
		oc.conjunctionConnection(o.getBoolean("conjunctionConnection"));
		switch (type) {
		case Execution:
			((ExecutionCondition) oc).exchange(o.getString("exchange"));
			((ExecutionCondition) oc).secType(o.getString("secType"));
			((ExecutionCondition) oc).symbol(o.getString("symbol"));
		case Margin:
			((OperatorCondition) oc).isMore(o.getBoolean("isMore"));
			((MarginCondition) oc).percent(o.getInt("percent"));
		case PercentChange:
			((OperatorCondition) oc).isMore(o.getBoolean("isMore"));
			((ContractCondition) oc).conId(o.getInt("conId"));
			((ContractCondition) oc).exchange(o.getString("exchange"));
			((PercentChangeCondition) oc).changePercent(o.getJsonNumber("changePercent").doubleValue());
		case Price:
			((OperatorCondition) oc).isMore(o.getBoolean("isMore"));
			((ContractCondition) oc).conId(o.getInt("conId"));
			((ContractCondition) oc).exchange(o.getString("exchange"));
			((PriceCondition) oc).price(o.getJsonNumber("price").doubleValue());
			((PriceCondition) oc).triggerMethod(o.getInt("triggerMethod"));
		case Time:
			((OperatorCondition) oc).isMore(o.getBoolean("isMore"));
			((TimeCondition) oc).time(o.getString("time"));
		case Volume:
			((OperatorCondition) oc).isMore(o.getBoolean("isMore"));
			((ContractCondition) oc).conId(o.getInt("conId"));
			((ContractCondition) oc).exchange(o.getString("exchange"));
			((VolumeCondition) oc).volume(o.getInt("volume"));
		}
		return oc;
	}

	private TagValue jsonToTagValue(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		return new TagValue(o.getString("tag"), o.getString("value"));
	}

	private SoftDollarTier jsonToSoftDollarTier(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		return new SoftDollarTier(o.getString("name"), o.getString("value"), o.getString("displayName"));
	}

	private Object jsonToJavaObject(JsonValue obj, PropertyType ptype)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		try {
			Class<?> ctype = (Class<?>) ptype.getJavaType();
			Constructor<?> method = ctype.getConstructor();
			Object object = method.newInstance();
			Map<String, PropertyType> properties = ptype.getProperties();
			for (String key : properties.keySet()) {
				if (obj.asJsonObject().containsKey(key)) {
					Object value = jsonToJava(obj.asJsonObject().get(key), properties.get(key));
					ptype.getSetterMethod(key).invoke(object, value);
				}
			}
			return object;
		} catch (InstantiationException e) {
			throw new InvocationTargetException(e);
		} catch (NoSuchMethodException e) {
			throw new InvocationTargetException(e);
		}
	}
}
