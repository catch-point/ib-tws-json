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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.ib.client.EWrapper;

/**
 * Serializes all the TwsEvents events
 *
 * @author James Leigh
 *
 */
public class TwsEventsHandler implements InvocationHandler {
	private Printer out;

	public static TwsEvents newInstance(Printer out) {
		TwsEventsHandler handler = new TwsEventsHandler(out);
		ClassLoader cl = EWrapper.class.getClassLoader();
		return (TwsEvents) Proxy.newProxyInstance(cl, new Class<?>[] { TwsEvents.class }, handler);
	}

	public TwsEventsHandler(Printer out) {
		this.out = out;
	}

	@Override
	public Object invoke(Object that, Method method, Object[] args) throws Throwable {
		out.println(method.getName(), method.getGenericParameterTypes(), args);
		return null;
	}
}
