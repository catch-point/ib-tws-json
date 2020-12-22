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
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ib.client.EWrapper;

/**
 * Serializes all the EWrapper events
 *
 * @author James Leigh
 *
 */
public class EWrapperHandler implements InvocationHandler {
	private final Logger logger = Logger.getLogger(EWrapperHandler.class.getName());
	private Printer out;

	public static EWrapper newInstance(Printer out) {
		EWrapperHandler handler = new EWrapperHandler(out);
		ClassLoader cl = EWrapper.class.getClassLoader();
		return (EWrapper) Proxy.newProxyInstance(cl, new Class<?>[] { EWrapper.class }, handler);
	}

	public EWrapperHandler(Printer out) {
		this.out = out;
	}

	@Override
	public Object invoke(Object that, Method method, Object[] args) throws Throwable {
		if ("error".equals(method.getName()) && args.length == 1 && args[0] instanceof Throwable) {
			Throwable ex = (Throwable) args[0];
			if (!(ex instanceof SocketException) || !"Socket closed".equals(ex.getMessage())) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}
		}
		out.println(method.getName(), method.getGenericParameterTypes(), args);
		return null;
	}
}
