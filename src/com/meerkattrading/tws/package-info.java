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
/**
 * A JSON API for the Interactive Brokers TWS API that runs inside the TWS or IB
 * Gateway JVM.
 *
 * <ul>
 * <li>Entry points: {@link com.meerkattrading.tws.Launcher} (command line),
 * {@link com.meerkattrading.tws.Agent} (-javaagent) and
 * {@link com.meerkattrading.tws.ServerSocketHandler} (AspectJ).</li>
 * <li>Connections: {@link com.meerkattrading.tws.Server} starts a
 * {@link com.meerkattrading.tws.Worker} per client, each running an
 * {@link com.meerkattrading.tws.Interpreter}.</li>
 * <li>Commands: {@link com.meerkattrading.tws.Invoker} turns EClient and
 * {@link com.meerkattrading.tws.TwsActions} methods into commands;
 * {@link com.meerkattrading.tws.Deserializer} converts JSON to Java.</li>
 * <li>Events: {@link com.meerkattrading.tws.EWrapperHandler} forwards EWrapper
 * callbacks to {@link com.meerkattrading.tws.Printer} and
 * {@link com.meerkattrading.tws.Serializer}.</li>
 * <li>Types: {@link com.meerkattrading.tws.PropertyType} describes IB classes
 * for both directions and for help.</li>
 * </ul>
 *
 * See ARCHITECTURE.md in the project root for the design, the message format,
 * the tests and how to upgrade to a new TWS API release.
 */
package com.meerkattrading.tws;
