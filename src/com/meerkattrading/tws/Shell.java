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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {
	private final Deserializer deserializer = new Deserializer();
	private Controller controller;
	private Printer out;
	private Terminal terminal;
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
		JsonStringParser parser = new JsonStringParser();
		terminal = TerminalBuilder.builder().system(false).streams(in, log).build();
		reader = LineReaderBuilder.builder().terminal(terminal).parser(parser).build();
	}

	public void repl() throws InterruptedException, IOException {
		while (true) {
			try {
				try {
					eval(read());
				} catch (InvocationTargetException e) {
					try {
						throw e.getCause();
					} catch (RuntimeException cause) {
						throw cause;
					} catch (Throwable cause) {
						throw e;
					}
				}
			} catch (UserInterruptException e) {
				// Ignore
			} catch (EndOfFileException e) {
				break;
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				getPrinter().println("error", e.getMessage() == null ? e.toString() : e.getMessage());
			} finally {
				out.flush();
			}
		}
	}

	public ParsedLine read() {
		reader.readLine();
		return reader.getParsedLine();
	}

	public void eval(ParsedLine line)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		if (line != null) {
			List<String> split = line.words();
			String command = split.get(0);
			try {
				Controller controller = getController();
				PropertyType[] types = controller.getParameterTypes(command);
				Object[] args = new Object[types.length];
				for (int i = 0; i < args.length; i++) {
					String json = i + 1 < split.size() ? split.get(i + 1) : "null";
					args[i] = deserializer.deserialize(json, types[i]);
				}
				controller.invoke(command, args);
			} catch (NoSuchMethodException e) {
				getPrinter().println("error", command + "?");
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
