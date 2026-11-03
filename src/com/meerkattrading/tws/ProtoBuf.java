/*
 * Copyright (c) 2026 James Leigh
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

/**
 * Identifies the TWS API's *ProtoBuf methods, which take or deliver a protobuf
 * message in place of individual parameters. They are hidden from the JSON API:
 * protobuf messages cannot be built from or written as JSON by
 * {@link Deserializer} and {@link Serializer}, and most protobuf events are
 * followed by their regular EWrapper event carrying the same data. The
 * configuration events ({@code configResponseProtoBuf} and
 * {@code updateConfigResponseProtoBuf}) have no regular equivalent, so the
 * configuration API remains unavailable over JSON.
 */
final class ProtoBuf {
	private static final String MESSAGE_LITE = "com.google.protobuf.MessageLite";

	private ProtoBuf() {
	}

	/**
	 * If any parameter of this method is a protobuf message
	 */
	static boolean hasMessageParameter(Method method) {
		for (Class<?> type : method.getParameterTypes()) {
			if (isMessage(type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * If this type implements com.google.protobuf.MessageLite, compared by name
	 * so this works without protobuf on the class path
	 */
	static boolean isMessage(Class<?> type) {
		if (type == null || type.isPrimitive()) {
			return false;
		} else if (MESSAGE_LITE.equals(type.getName())) {
			return true;
		}
		for (Class<?> face : type.getInterfaces()) {
			if (isMessage(face)) {
				return true;
			}
		}
		return isMessage(type.getSuperclass());
	}
}
