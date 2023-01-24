/*
 * Copyright (c) 2020-2023 James Leigh
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Read-eval-print loop
 *
 * @author James Leigh
 *
 */
public class Interpreter {
	private final Logger logger = Logger.getLogger(Interpreter.class.getName());
	private final Deserializer deserializer = new Deserializer();
	private final Invoker controller;
	private final Printer out;
	private final LineReader reader;
	private final TwsSocketActions client;

	public Interpreter(boolean prompt) throws IOException {
		Prompter prompter = prompt ? new Prompter(System.err) : new Prompter();
		reader = new LineReader(System.in, prompter);
		this.out = new Printer(prompter, System.out);
		this.client = new TwsSocketActions(this.getPrinter());
		controller = new Invoker(client, this.getPrinter());
	}

	public Interpreter(InputStream in, OutputStream out) throws IOException {
		reader = new LineReader(in);
		this.out = new Printer(out);
		this.client = new TwsSocketActions(this.getPrinter());
		controller = new Invoker(client, this.getPrinter());
	}

	public void setRemoteAddress(String host, int port) {
		client.setRemoteAddress(host, port);
	}

	public void exit() throws IOException {
		try {
			getInvoker().exit();
		} catch (EOFException e) {
			// expected
		}
	}

	public void repl(InputStream in) throws InterruptedException, IOException {
		repl(new LineReader(in));
	}

	public void repl() throws InterruptedException, IOException {
		repl(reader);
	}

	private void repl(LineReader reader) throws InterruptedException, IOException {
		while (true) {
			try {
				rep(reader, "");
			} catch (EOFException e) {
				break;
			} finally {
				out.flush();
			}
		}
	}

	private void rep(LineReader reader, CharSequence prefix) throws IOException {
		try {
			ParsedInput input = reader.readLine(prefix);
			try {
				try {
					if (input != null && !input.isEmpty()) {
						eval(input);
					}
				} catch (MoreInputExpected e) {
					String string = input.getInput().toString();
					if (string.trim().length() > 0) {
						rep(reader, input.getInput() + "\n");
					} else {
						rep(reader, "");
					}
				}
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				logger.warning("" + input.getInput());
				String msg = e.getMessage() == null ? e.toString() : e.getMessage();
				getPrinter().println("error", msg + " while evaluating " + input.getInput());
			}
		} catch (SyntaxError e) {
			logger.warning(e.getMessage());
			getPrinter().println("error", e.getMessage() == null ? e.toString() : e.getMessage());
		}
	}

	private void eval(ParsedInput line)
			throws IllegalAccessException, InvocationTargetException, IOException, MoreInputExpected {
		List<String> values = line.getParsedValues();
		String command = values.get(0);
		try {
			Invoker controller = getInvoker();
			PropertyType[] types = controller.getParameterTypes(command);
			if (values.size() < types.length + 1) {
				for (int i = values.size() - 1; i < types.length; i++) {
					if (types[i].isPrimitive()) {
						throw new MoreInputExpected(
								"Expecting " + (1 + types.length - values.size()) + " more value(s)");
					}
				}
			} else if (values.size() > types.length + 1) {
				throw new IllegalArgumentException("Expected " + (values.size() - types.length - 1) + " less value(s)");
			}
			Object[] args = new Object[types.length];
			for (int i = 0; i < args.length; i++) {
				String json = i + 1 < values.size() ? values.get(i + 1) : "null";
				args[i] = deserializer.deserialize(json, types[i]);
			}
			controller.invoke(command, args);
		} catch (NoSuchMethodException e) {
			if (command.length() > 0) {
				getPrinter().println("error", command + "?");
			}
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (RuntimeException | IllegalAccessException | InvocationTargetException | IOException
					| MoreInputExpected cause) {
				throw cause;
			} catch (Throwable cause) {
				throw e;
			}
		}
	}

	protected Invoker getInvoker() throws IOException {
		return controller;
	}

	protected Printer getPrinter() throws IOException {
		return out;
	}
}
