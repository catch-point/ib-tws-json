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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Shell {
	private final Deserializer deserializer = new Deserializer();
	private Controller controller;
	private Printer out;
	private LineReader reader;

	public static void main(String[] args) throws InterruptedException, IOException {
		PrintStream out = System.out;
		System.setOut(System.err);
		Shell shell = new Shell(System.in, out, System.err);
		shell.repl();
		shell.exit();
	}

	public Shell(InputStream in, OutputStream out, OutputStream log) throws IOException {
		this.out = new Printer(new PrintWriter(out));
		controller = new Controller(this.getPrinter());
		reader = new LineReader(new BufferedReader(new InputStreamReader(in)));
	}

	public void repl() throws InterruptedException, IOException {
		while (true) {
			try {
				rep("");
			} catch (EOFException e) {
				break;
			} finally {
				out.flush();
			}
		}
	}

	private void rep(CharSequence prefix) throws IOException {
		try {
			ParsedInput input = reader.readLine(prefix);
			try {
				eval(input);
			} catch (MoreInputExpected e) {
				rep(input.getInput() + "\n");
			}
		} catch (SyntaxError | IllegalAccessException | InvocationTargetException | RuntimeException e) {
			getPrinter().println("error", e.getMessage() == null ? e.toString() : e.getMessage());
		}
	}

	public void eval(ParsedInput line)
			throws IllegalAccessException, InvocationTargetException, IOException, MoreInputExpected {
		if (line != null) {
			List<String> values = line.getParsedValues();
			String command = values.get(0);
			try {
				Controller controller = getController();
				PropertyType[] types = controller.getParameterTypes(command);
				if (values.size() < types.length + 1) {
					for (int i = values.size() - 1; i < types.length; i++) {
						if (types[i].isPrimitive()) {
							throw new MoreInputExpected(
									"Expecting " + (types.length - values.size()) + " more value(s)");
						}
					}
				} else if (values.size() > types.length + 1) {
					throw new IllegalArgumentException("Expected " + (values.size() - types.length) + " less value(s)");
				}
				Object[] args = new Object[types.length];
				for (int i = 0; i < args.length; i++) {
					String json = i + 1 < values.size() ? values.get(i + 1) : "null";
					args[i] = deserializer.deserialize(json, types[i]);
				}
				controller.invoke(command, args);
			} catch (NoSuchMethodException e) {
				getPrinter().println("error", command + "?");
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
		} else {
			getPrinter().println("error", "Each parameter must be in JSON format");
		}
	}

	public void exit() throws IOException {
		getController().eDisconnect();
	}

	protected Controller getController() throws IOException {
		return controller;
	}

	protected Printer getPrinter() throws IOException {
		return out;
	}
}
