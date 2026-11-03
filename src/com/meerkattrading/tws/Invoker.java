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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.json.JsonString;
import javax.json.JsonValue;

import com.ib.client.Decimal;
import com.ib.client.EWrapper;

/**
 * Executes all the commands from the shell.
 *
 * Every public void method of {@link TwsActions} and the EClient is a command,
 * except the *ProtoBuf methods (see {@link ProtoBuf}).
 * A command name defined by {@link TwsActions} hides any EClient methods of the
 * same name. Other overloaded names keep every variant, and
 * {@link #resolve(String, List)} picks a variant for each call from the number
 * and JSON types of its arguments.
 *
 * @author James Leigh
 *
 */
public class Invoker {
	/** Orders variants by parameter count, then by signature, independent of reflection order */
	private static final Comparator<Method> VARIANT_ORDER = Comparator
			.comparingInt(Method::getParameterCount)
			.thenComparing(m -> Arrays.toString(m.getGenericParameterTypes()));
	private static final int EXACT = 0;
	private static final int CONVERT = 1;
	private static final int REJECT = -1;

	private final Logger logger = Logger.getLogger(Invoker.class.getName());
	private final Map<String, List<Method>> commands = new TreeMap<>();
	private final Map<Type, PropertyType> properties = new HashMap<>();
	private final Deserializer deserializer = new Deserializer();
	private final TwsSocketActions actions;

	/**
	 * A command resolved to one method variant with its deserialized arguments
	 */
	public static final class Call {
		private final Method method;
		private final Object[] args;

		Call(Method method, Object[] args) {
			this.method = method;
			this.args = args;
		}

		public Method getMethod() {
			return method;
		}

		public Object[] getArgs() {
			return args;
		}
	}

	public Invoker(Printer out) throws IOException {
		this(new TwsSocketActions(out), out);
	}

	public Invoker(TwsSocketActions client, Printer out) throws IOException {
		this.actions = client;
		for (Class<?> face : getAllInterfaces(getActions().getClass(), true)) {
			for (Method method : face.getMethods()) {
				if (isCommand(method)) {
					addCommand(method);
				}
			}
		}
		Set<String> actionNames = new LinkedHashSet<>(commands.keySet());
		for (Class<?> face : getAllInterfaces(getClient().getClass(), true)) {
			for (Method method : face.getMethods()) {
				if (isCommand(method)) {
					if (actionNames.contains(method.getName())) {
						logger.fine("Ignoring method " + method.toString());
					} else {
						addCommand(method);
					}
				}
			}
		}
		for (List<Method> variants : commands.values()) {
			variants.sort(VARIANT_ORDER);
			for (Method method : variants) {
				for (Type type : method.getGenericParameterTypes()) {
					addPropertyType(new PropertyType(type));
				}
			}
		}
		for (Method method : EWrapper.class.getDeclaredMethods()) {
			if (!ProtoBuf.hasMessageParameter(method)) {
				for (Type type : method.getGenericParameterTypes()) {
					addPropertyType(new PropertyType(type));
				}
			}
		}
		for (Method method : TwsEvents.class.getDeclaredMethods()) {
			for (Type type : method.getGenericParameterTypes()) {
				addPropertyType(new PropertyType(type));
			}
		}
		Map<String, Method> help = new TreeMap<>();
		for (Map.Entry<String, List<Method>> e : commands.entrySet()) {
			help.put(e.getKey(), getCanonical(e.getValue()));
		}
		actions.setHelpSchema(help, properties);
	}

	/**
	 * The parameter types of the canonical variant (the one shown by help)
	 */
	public PropertyType[] getParameterTypes(String command) throws NoSuchMethodException {
		if (commands.containsKey(command)) {
			return getParameterTypes(getCanonical(commands.get(command)));
		} else {
			throw new NoSuchMethodException(command);
		}
	}

	/**
	 * Picks the method variant that best fits the given JSON arguments and
	 * deserializes them. Variants taking exactly the given number of arguments
	 * are preferred; otherwise, if a variant needs more primitive values,
	 * {@link MoreInputExpected} is thrown so the input can continue on the next
	 * line; otherwise variants whose missing trailing arguments are all
	 * non-primitive (passed as null) are used. Remaining variants are ranked by
	 * how well each JSON value fits each parameter type, and the first variant
	 * whose arguments deserialize is used.
	 */
	public Call resolve(String command, List<String> jsonArgs) throws NoSuchMethodException, MoreInputExpected,
			IllegalAccessException, InvocationTargetException {
		List<Method> variants = commands.get(command);
		if (variants == null) {
			throw new NoSuchMethodException(command);
		}
		int given = jsonArgs.size();
		int maxParams = 0;
		int moreExpected = Integer.MAX_VALUE;
		List<Method> exact = new ArrayList<>();
		List<Method> partial = new ArrayList<>();
		for (Method method : variants) {
			PropertyType[] types = getParameterTypes(method);
			maxParams = Math.max(maxParams, types.length);
			if (given > types.length) {
				continue;
			}
			boolean primitiveMissing = false;
			for (int i = given; i < types.length; i++) {
				primitiveMissing |= types[i].isPrimitive();
			}
			if (primitiveMissing) {
				moreExpected = Math.min(moreExpected, types.length - given);
			} else if (given == types.length) {
				exact.add(method);
			} else {
				partial.add(method);
			}
		}
		List<Method> candidates;
		if (!exact.isEmpty()) {
			candidates = exact;
		} else if (moreExpected < Integer.MAX_VALUE) {
			throw new MoreInputExpected("Expecting " + moreExpected + " more value(s)");
		} else if (!partial.isEmpty()) {
			candidates = partial;
		} else {
			throw new IllegalArgumentException("Expected " + (given - maxParams) + " less value(s)");
		}
		if (candidates.size() == 1) {
			Method method = candidates.get(0);
			return new Call(method, deserialize(method, jsonArgs));
		}
		List<Method> ranked = rank(candidates, jsonArgs);
		RuntimeException runtime = null;
		InvocationTargetException target = null;
		IllegalAccessException access = null;
		for (Method method : ranked) {
			try {
				return new Call(method, deserialize(method, jsonArgs));
			} catch (RuntimeException e) {
				runtime = runtime == null ? e : runtime;
			} catch (InvocationTargetException e) {
				target = target == null ? e : target;
			} catch (IllegalAccessException e) {
				access = access == null ? e : access;
			}
		}
		if (runtime != null)
			throw runtime;
		if (target != null)
			throw target;
		throw access;
	}

	public Object invoke(Call call) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = call.getMethod();
		Object client = getClient();
		Object actions = getActions();
		if (method.getDeclaringClass().isInstance(client)) {
			return method.invoke(client, call.getArgs());
		} else if (method.getDeclaringClass().isInstance(actions)) {
			return method.invoke(actions, call.getArgs());
		} else {
			throw new AssertionError("Unknown command: " + method.getName());
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

	private static boolean isCommand(Method method) {
		return method.getReturnType() == Void.TYPE && Modifier.isPublic(method.getModifiers())
				&& !Modifier.isStatic(method.getModifiers()) && !method.isBridge()
				&& !Object.class.equals(method.getDeclaringClass()) && !ProtoBuf.hasMessageParameter(method);
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

	/**
	 * Adds a variant, unless one with the same erased parameter types is already
	 * known (such as an override, or a proxy class's raw re-declaration of an
	 * interface method), keeping the first, which has the generic parameter types
	 */
	private void addCommand(Method method) {
		List<Method> variants = commands.computeIfAbsent(method.getName(), k -> new ArrayList<>());
		for (Method existing : variants) {
			if (Arrays.equals(existing.getParameterTypes(), method.getParameterTypes())) {
				return;
			}
		}
		variants.add(method);
	}

	/**
	 * The variant help describes: the most parameters, then the fewest primitive
	 * parameters (an enum parameter also accepts its name or numeric code, so it
	 * is the more general variant), then {@link #VARIANT_ORDER}.
	 */
	private static Method getCanonical(List<Method> variants) {
		Method best = null;
		for (Method method : variants) {
			if (best == null || method.getParameterCount() > best.getParameterCount()
					|| method.getParameterCount() == best.getParameterCount()
							&& countPrimitives(method) < countPrimitives(best)) {
				best = method;
			}
		}
		return best;
	}

	private static int countPrimitives(Method method) {
		int count = 0;
		for (Class<?> type : method.getParameterTypes()) {
			if (type.isPrimitive()) {
				count++;
			}
		}
		return count;
	}

	private PropertyType[] getParameterTypes(Method method) {
		Type[] types = method.getGenericParameterTypes();
		PropertyType[] ptypes = new PropertyType[types.length];
		for (int i = 0; i < types.length; i++) {
			ptypes[i] = properties.get(types[i]);
		}
		return ptypes;
	}

	private Object[] deserialize(Method method, List<String> jsonArgs)
			throws IllegalAccessException, InvocationTargetException {
		PropertyType[] types = getParameterTypes(method);
		Object[] args = new Object[types.length];
		for (int i = 0; i < args.length; i++) {
			String json = i < jsonArgs.size() ? jsonArgs.get(i) : "null";
			args[i] = deserializer.deserialize(json, types[i]);
		}
		return args;
	}

	/**
	 * Orders candidates by how well the JSON arguments fit: variants that reject
	 * an argument go last, then fewest conversions, then {@link #VARIANT_ORDER}
	 */
	private List<Method> rank(List<Method> candidates, List<String> jsonArgs) {
		JsonValue[] values = new JsonValue[jsonArgs.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = deserializer.parse(jsonArgs.get(i));
		}
		Map<Method, Integer> scores = new HashMap<>();
		for (Method method : candidates) {
			PropertyType[] types = getParameterTypes(method);
			int score = 0;
			for (int i = 0; i < values.length; i++) {
				int fit = fitness(values[i], types[i]);
				if (fit == REJECT) {
					score = Integer.MAX_VALUE;
					break;
				}
				score += fit;
			}
			scores.put(method, score);
		}
		List<Method> ranked = new ArrayList<>(candidates);
		ranked.sort(Comparator.<Method>comparingInt(scores::get).thenComparing(VARIANT_ORDER));
		return ranked;
	}

	/**
	 * How well a JSON value fits a parameter type: {@link #EXACT},
	 * {@link #CONVERT} (the Deserializer would have to coerce it), or
	 * {@link #REJECT}
	 */
	private static int fitness(JsonValue value, PropertyType ptype) {
		JsonValue.ValueType json = value.getValueType();
		if (ptype.isList() || ptype.isArray() || ptype.isSet()) {
			return json == JsonValue.ValueType.ARRAY || json == JsonValue.ValueType.NULL ? EXACT : REJECT;
		} else if (ptype.isMap() || ptype.isEntry()) {
			return json == JsonValue.ValueType.OBJECT || json == JsonValue.ValueType.NULL ? EXACT : REJECT;
		} else if (!(ptype.getJavaType() instanceof Class<?>)) {
			return CONVERT;
		}
		Class<?> type = (Class<?>) ptype.getJavaType();
		boolean bool = type == Boolean.TYPE || type == Boolean.class;
		boolean text = type == String.class || type == Character.TYPE || type == Character.class;
		boolean numeric = isNumeric(type);
		if (type == Object.class) {
			return EXACT;
		}
		switch (json) {
		case NULL:
			return type.isPrimitive() ? CONVERT : EXACT;
		case TRUE:
		case FALSE:
			return bool ? EXACT : text || numeric ? CONVERT : REJECT;
		case NUMBER:
			return numeric ? EXACT : type.isEnum() || text ? CONVERT : REJECT;
		case STRING:
			if (text) {
				return EXACT;
			} else if (type.isEnum()) {
				return isEnumName(type, ((JsonString) value).getString()) ? EXACT : CONVERT;
			} else {
				return numeric || bool ? CONVERT : REJECT;
			}
		case OBJECT:
			return bool || text || numeric || type.isEnum() ? REJECT : EXACT;
		case ARRAY:
		default:
			return REJECT;
		}
	}

	private static boolean isNumeric(Class<?> type) {
		return type.isPrimitive() && type != Boolean.TYPE && type != Character.TYPE
				|| Number.class.isAssignableFrom(type) || Decimal.class.equals(type);
	}

	private static boolean isEnumName(Class<?> type, String name) {
		for (Object constant : type.getEnumConstants()) {
			if (((Enum<?>) constant).name().equals(name)) {
				return true;
			}
		}
		return false;
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
