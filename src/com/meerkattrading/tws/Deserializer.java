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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;

import com.ib.client.Bar;
import com.ib.client.ContractCondition;
import com.ib.client.ExecutionCondition;
import com.ib.client.HistogramEntry;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.MarginCondition;
import com.ib.client.OperatorCondition;
import com.ib.client.OrderCondition;
import com.ib.client.OrderConditionType;
import com.ib.client.PercentChangeCondition;
import com.ib.client.PriceCondition;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.ib.client.TimeCondition;
import com.ib.client.VolumeCondition;

/**
 * Takes the JSON values from stdin command and converts them into Java Object.
 * 
 * @author James Leigh
 *
 */
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
		} else if (ptype.isArray()) {
			return jsonToArrayOf(obj, ptype.getComponentType());
		} else if (ptype.isSet()) {
			return jsonToSetOf(obj, ptype.getComponentType());
		} else if (ptype.isMap()) {
			return jsonToMapOf(obj, ptype.getComponentType());
		} else if (ptype.isEntry()) {
			return jsonToEntry(obj, ptype.getComponentType());
		} else {
			Class<?> type = (Class<?>) ptype.getJavaType();
			if (type == Boolean.TYPE || type == Boolean.class) {
				return jsonToBoolean(obj);
			} else if (type.isAssignableFrom(Object.class)) {
				return jsonToObject(obj);
			} else if (type.isAssignableFrom(String.class)) {
				return jsonToString(obj);
			} else if (type == Character.TYPE || type.isAssignableFrom(Character.class)) {
				return jsonToCharacter(obj);
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
			} else if (type.isAssignableFrom(HistogramEntry.class)) {
				return jsonToHistogramEntry(obj);
			} else if (type.isAssignableFrom(Bar.class)) {
				return jsonToBar(obj);
			} else if (type.isAssignableFrom(HistoricalTick.class)) {
				return jsonToHistoricalTick(obj);
			} else if (type.isAssignableFrom(HistoricalTickBidAsk.class)) {
				return jsonToHistoricalTickBidAsk(obj);
			} else if (type.isAssignableFrom(TickAttribBidAsk.class)) {
				return jsonToTickAttribBidAsk(obj);
			} else if (type.isAssignableFrom(HistoricalTickLast.class)) {
				return jsonToHistoricalTickLast(obj);
			} else if (type.isAssignableFrom(TickAttribLast.class)) {
				return jsonToTickAttribLast(obj);
			} else {
				return jsonToJavaObject(obj, ptype);
			}
		}
	}

	private List<?> jsonToListOf(JsonValue obj, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null)
			return null;
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

	private Object[] jsonToArrayOf(JsonValue obj, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		List<?> list = jsonToListOf(obj, type);
		Object[] array = (Object[]) Array.newInstance((Class<?>) type.getJavaType(), list.size());
		return list.toArray(array);
	}

	private Set<?> jsonToSetOf(JsonValue obj, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		else
			return new LinkedHashSet<Object>(jsonToListOf(obj, type));
	}

	private Map<?, ?> jsonToMapOf(JsonValue obj, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		Map<Object, Object> map = new LinkedHashMap<>();
		for (String key : obj.asJsonObject().keySet()) {
			Object k = jsonToJava(Json.createValue(key), type.getKeyType());
			Object v = jsonToJava(obj.asJsonObject().get(key), type);
			map.put(k, v);
		}
		return map;
	}

	private Map.Entry<String, ?> jsonToEntry(JsonValue obj, PropertyType type)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		String key = obj.asJsonObject().getString("key");
		JsonValue value = obj.asJsonObject().get("value");
		return new AbstractMap.SimpleEntry<String, Object>(key, jsonToJava(value, type));
	}

	private boolean jsonToBoolean(JsonValue obj) {
		if (obj == null)
			return false;
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
		if (obj == null)
			return null;
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
		if (obj == null)
			return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		case STRING:
			return ((JsonString) obj).getString();
		default:
			return obj.toString();
		}
	}

	private Character jsonToCharacter(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		return jsonToString(obj).charAt(0);
	}

	private Number jsonToNumber(JsonValue obj) {
		if (obj == null)
			return 0;
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
		if (obj == null)
			return null;
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
		if (obj == null)
			return null;
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
		if (obj == null)
			return 0;
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
		if (obj == null)
			return 0;
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
		if (obj == null)
			return 0;
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
		if (obj == null)
			return null;
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
		if (obj == null)
			return null;
		switch (obj.getValueType()) {
		case NULL:
			return null;
		default:
			return Arrays.asList(jsonToObjectArray(obj));
		}
	}

	private Object jsonToEnum(JsonValue obj, Class<?> enum_type) {
		if (obj == null)
			return null;
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
		oc.conjunctionConnection(jsonToBoolean(o.get("conjunctionConnection")));
		switch (type) {
		case Execution:
			((ExecutionCondition) oc).exchange(o.getString("exchange"));
			((ExecutionCondition) oc).secType(o.getString("secType"));
			((ExecutionCondition) oc).symbol(o.getString("symbol"));
			return oc;
		case Margin:
			((OperatorCondition) oc).isMore(jsonToBoolean(o.get("isMore")));
			((MarginCondition) oc).percent(jsonToInteger(o.get("percent")));
			return oc;
		case PercentChange:
			((OperatorCondition) oc).isMore(jsonToBoolean(o.get("isMore")));
			((ContractCondition) oc).conId(jsonToInteger(o.get("conId")));
			((ContractCondition) oc).exchange(o.getString("exchange"));
			((PercentChangeCondition) oc).changePercent(jsonToDouble(o.get("changePercent")));
			return oc;
		case Price:
			((OperatorCondition) oc).isMore(jsonToBoolean(o.get("isMore")));
			((ContractCondition) oc).conId(jsonToInteger(o.get("conId")));
			((ContractCondition) oc).exchange(o.getString("exchange"));
			((PriceCondition) oc).price(jsonToDouble(o.get("price")));
			((PriceCondition) oc).triggerMethod(jsonToInteger(o.get("triggerMethod")));
			return oc;
		case Time:
			((OperatorCondition) oc).isMore(jsonToBoolean(o.get("isMore")));
			((TimeCondition) oc).time(o.getString("time"));
			return oc;
		case Volume:
			((OperatorCondition) oc).isMore(jsonToBoolean(o.get("isMore")));
			((ContractCondition) oc).conId(jsonToInteger(o.get("conId")));
			((ContractCondition) oc).exchange(o.getString("exchange"));
			((VolumeCondition) oc).volume(jsonToInteger(o.get("volume")));
			return oc;
		}
		throw new AssertionError("Unhandled OrderConditionType " + type);
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
		return new SoftDollarTier(jsonToString(o.get("name")), jsonToString(o.get("value")),
				jsonToString(o.containsKey("displayName") ? o.get("displayName") : o.get("name")));
	}

	private HistogramEntry jsonToHistogramEntry(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		return new HistogramEntry(jsonToDouble(o.get("price")), jsonToLong(o.get("size")));
	}

	private Bar jsonToBar(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		String time = o.getString("time");
		double open = jsonToDouble(o.get("open"));
		double high = jsonToDouble(o.get("high"));
		double low = jsonToDouble(o.get("low"));
		double close = jsonToDouble(o.get("close"));
		long volume = jsonToLong(o.get("volume"));
		int count = jsonToInteger(o.get("count"));
		double wap = jsonToDouble(o.get("wap"));
		return new Bar(time, open, high, low, close, volume, count, wap);
	}

	private HistoricalTick jsonToHistoricalTick(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		long time = jsonToLong(o.get("time"));
		double price = jsonToDouble(o.get("price"));
		long size = jsonToLong(o.get("size"));
		return new HistoricalTick(time, price, size);
	}

	private HistoricalTickBidAsk jsonToHistoricalTickBidAsk(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		long time = jsonToLong(o.get("time"));
		TickAttribBidAsk attrib = jsonToTickAttribBidAsk(o.get("tickAttribBidAsk"));
		double priceBid = jsonToDouble(o.get("priceBid"));
		double priceAsk = jsonToDouble(o.get("priceAsk"));
		long sizeBid = jsonToLong(o.get("sizeBid"));
		long sizeAsk = jsonToLong(o.get("sizeAsk"));
		return new HistoricalTickBidAsk(time, attrib, priceBid, priceAsk, sizeBid, sizeAsk);
	}

	private TickAttribBidAsk jsonToTickAttribBidAsk(JsonValue value) {
		if (value == null || value.getValueType() == ValueType.NULL)
			return null;
		JsonObject obj = value.asJsonObject();
		TickAttribBidAsk o = new TickAttribBidAsk();
		o.askPastHigh(jsonToBoolean(obj.get("askPastHigh")));
		o.bidPastLow(jsonToBoolean(obj.get("bidPastLow")));
		return o;
	}

	private HistoricalTickLast jsonToHistoricalTickLast(JsonValue obj) {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		JsonObject o = obj.asJsonObject();
		long time = jsonToLong(o.get("time"));
		TickAttribLast attrib = jsonToTickAttribLast(o.get("tickAttribLast"));
		double price = jsonToDouble(o.get("price"));
		long size = jsonToLong(o.get("size"));
		return new HistoricalTickLast(time, attrib, price, size, o.getString("exchange"),
				o.getString("specialConditions"));
	}

	private TickAttribLast jsonToTickAttribLast(JsonValue value) {
		if (value == null || value.getValueType() == ValueType.NULL)
			return null;
		JsonObject obj = value.asJsonObject();
		TickAttribLast o = new TickAttribLast();
		o.pastLimit(jsonToBoolean(obj.get("pastLimit")));
		o.unreported(jsonToBoolean(obj.get("unreported")));
		return o;
	}

	private Object jsonToJavaObject(JsonValue obj, PropertyType ptype)
			throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
		if (obj == null || obj.getValueType() == ValueType.NULL)
			return null;
		if (obj.getValueType() != ValueType.OBJECT)
			throw new IllegalArgumentException("Expected " + obj + " to be an object");
		try {
			Class<?> ctype = (Class<?>) ptype.getJavaType();
			Constructor<?> method = ctype.getConstructor();
			Object object = method.newInstance();
			Map<String, PropertyType> properties = ptype.getProperties();
			for (String key : properties.keySet()) {
				if (obj.asJsonObject().containsKey(key)) {
					Object value = jsonToJava(obj.asJsonObject().get(key), properties.get(key));
					Method setter = ptype.getSetterMethod(key);
					if (value != null && value instanceof Enum<?>
							&& Integer.TYPE.equals(setter.getParameterTypes()[0])) {
						setter.invoke(object, ((Enum<?>) value).ordinal());
					} else {
						setter.invoke(object, value);
					}
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
