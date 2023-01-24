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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Listens on a TCP port and launches new {@Worker} threads to handle the client
 * 
 * @author James Leigh
 *
 */
public class Server implements Runnable {
	private final Logger logger = Logger.getLogger(Server.class.getName());
	private int local_port;
	private InetAddress local_inet;
	private int remote_port;
	private InetAddress remote_inet;
	private Thread thread;
	private ServerSocket serverSocket;
	private final ConcurrentHashMap<Worker, Thread> workers = new ConcurrentHashMap<>();

	/**
	 * Will listen on local_port and bind to local_inet and have {@link Worker} connect to tws API
	 */
	public Server(InetAddress local_inet, int local_port) {
		this.local_port = local_port;
		this.local_inet = local_inet;
	}

	/**
	 * Changes the {@link Worker}s to only connect to the give tws API
	 */
	public synchronized void setRemote(InetAddress remote_inet, int remote_port) {
		this.remote_port = remote_port;
		this.remote_inet = remote_inet;
		for (Worker worker : workers.keySet()) {
			worker.setRemoteAddress(remote_inet, local_port);
		}
	}

	/**
	 * Called when Server is ready to listen and run in it's own thread
	 */
	@Override
	public void run() {
		try {
			while (!serverSocket.isClosed()) {
				try {
					createWorker(serverSocket.accept());
				} catch (SocketException e) {
					if (!serverSocket.isClosed()) {
						logger.warning(e.getMessage());
					}
				} catch (IOException e) {
					logger.warning(e.getMessage());
				}
			}
		} catch (RuntimeException e) {
			logger.severe(e.getMessage());
		}
	}

	/**
	 * Start a new Thread running this server
	 */
	public synchronized void start() {
		if (thread == null) {
			try {
				thread = new Thread(this);
				serverSocket = new ServerSocket(local_port, 1, local_inet);
				thread.start();
			} catch (IOException e) {
				logger.warning(e.getMessage());
			}
		}
	}

	/**
	 * Stops this server and closes it's thread
	 */
	public synchronized void stop() {
		if (serverSocket != null) {
			try {
				for (Worker worker : workers.keySet()) {
					worker.exit();
				}
				serverSocket.close();
			} catch (IOException e) {
				logger.warning(e.getMessage());
			}
		}
	}

	/**
	 * Called when a new remote client has opened a socket and needs a worker
	 */
	private synchronized void createWorker(Socket socket) throws IOException {
		Worker worker = new Worker(socket.getInputStream(), socket.getOutputStream());
		if (remote_inet != null && remote_port > 0) {
			worker.setRemoteAddress(remote_inet, remote_port);
		}
		Thread thread = new Thread(worker);
		workers.put(worker, thread);
		worker.onExit(() -> {
			try {
				workers.remove(worker);
				worker.exit();
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					logger.fine(e.getMessage());
				}
			}
		});
		thread.start();
	}

}
