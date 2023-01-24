/*
 * Copyright (c) 2023 James Leigh
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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Called via AspectJ to creates a new {@link Server} for each open {@link ServerSocket}
 * 
 * @author James Leigh
 *
 */
@Aspect
public class ServerSocketHandler {
	private static Integer port;
	private static int portOffset = 100;
	private static InetAddress inet = InetAddress.getLoopbackAddress();
	private static Integer twsPort;
	private static ConcurrentHashMap<Integer, Server> servers = new ConcurrentHashMap<>();

	/**
	 * Set the only port {@link Server} should listen on
	 */
	public static void setPort(Integer port) {
		ServerSocketHandler.port = port;
	}

	/**
	 * Have {@link Server} listen this number of ports higher than the initial {@link ServerSocket}
	 */
	public static void setPortOffset(Integer portOffset) {
		ServerSocketHandler.portOffset = portOffset;
	}

	/**
	 * Set the only {@link InetAddress} that should be bound by the {@link Server}
	 */
	public static void setInet(InetAddress inet) {
		ServerSocketHandler.inet = inet;
	}

	/**
	 * Set the only TWS API port that should be used by the {@link Worker}
	 */
	public static void setTwsPort(Integer twsPort) {
		ServerSocketHandler.twsPort = twsPort;
	}

	/**
	 * Starts the {@link Server}, if only one
	 */
	public static void initialize() {
		if (port != null) {
			servers.putIfAbsent(port, new Server(inet, port));
			Server server = servers.get(port);
			server.start();
		}
	}

	/**
	 * Called through AspectJ when a new {@link ServerSocket} is created
	 */
	@AfterReturning(pointcut = "call(java.net.ServerSocket.new(..)) && this(caller)", returning = "srv")
	public void create(ServerSocket srv, Object caller) {
		String pkg = caller.getClass().getPackage().getName();
		if (srv.isBound() && !pkg.contains(".rpc.") && !pkg.contains(Server.class.getPackage().getName())) {
			startServer(srv);
		}
	}

	/**
	 * Called through AspectJ when a {@link ServerSocket} is bound
	 */
	@AfterReturning("call(void java.net.ServerSocket.bind(..)) && target(srv) && this(caller)")
	public void bind(ServerSocket srv, Object caller) {
		String pkg = caller.getClass().getPackage().getName();
		if (srv.isBound() && !pkg.contains(".rpc.") && !pkg.contains(Server.class.getPackage().getName())) {
			startServer(srv);
		}
	}

	/**
	 * Called through AspectJ when a {@link ServerSocket} is closed
	 */
	@Before("call(void java.net.ServerSocket.close()) && target(srv)")
	public void close(ServerSocket srv) {
		stopServer(srv);
	}

	/**
	 * Called when a new {@link ServerSocket} needs a new {@link Server}
	 */
	private void startServer(ServerSocket srv) {
		if (twsPort == null || twsPort.equals(srv.getLocalPort())) {
			int json_port = port != null ? port : portOffset + srv.getLocalPort();
			servers.putIfAbsent(json_port, new Server(inet, json_port));
			Server server = servers.get(json_port);
			server.setRemote(srv.getInetAddress(), srv.getLocalPort());
			server.start();
		}
	}

	/**
	 * Closes the {@link Server} when using an offset a {@link ServerSocket} is closed 
	 */
	private void stopServer(ServerSocket srv) {
		if (port == null) {
			int json_port = portOffset + srv.getLocalPort();
			Server server = servers.remove(json_port);
			if (server != null) {
				server.stop();
			}
		}
	}
}
