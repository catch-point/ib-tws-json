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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * Used by {@link Server} to handle the remote clients in a separate thread
 * 
 * @author James Leigh
 *
 */
public class Worker implements Runnable {
	private final Logger logger = Logger.getLogger(Worker.class.getName());
	private final Interpreter interpreter;
	private Runnable onExit;

	/**
	 * Gives this worker an {@link Interpreter} to use with the client
	 */
	public Worker(InputStream in, OutputStream out) throws IOException {
		this.interpreter = new Interpreter(in, out);
	}

	public void setRemoteAddress(InetAddress host, int port) {
		interpreter.setRemoteAddress(host.getHostAddress(), port);
	}

	/**
	 * What procedure to call when the client exits
	 */
	public void onExit(Runnable onExit) {
		this.onExit = onExit;
	}

	/**
	 * Process the given input in this thread
	 */
	@Override
	public void run() {
		try {
			interpreter.repl();
		} catch (InterruptedException e) {
			logger.warning(e.getMessage());
		} catch (IOException e) {
			logger.warning(e.getMessage());
		} catch (RuntimeException e) {
			logger.severe(e.getMessage());
		} finally {
			onExit.run();
		}
	}

	/**
	 * Called to disconnect the client
	 */
	public void exit() {
		try {
			interpreter.exit();
		} catch (IOException e) {
			logger.warning(e.getMessage());
		}
	}

}
