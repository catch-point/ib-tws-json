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
		Printer out = new Printer(System.out);
		System.setOut(System.err);
		Shell shell = new Shell(out);
		shell.repl();
		shell.exit();
	}

	public Shell(Printer out) throws IOException {
		this.out = out;
		controller = new Controller(out);
		JsonStringParser parser = new JsonStringParser();
		terminal = TerminalBuilder.builder().system(false).streams(System.in, System.err).build();
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
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public ParsedLine read() {
		reader.readLine();
		return reader.getParsedLine();
	}

	public void eval(ParsedLine line)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (line != null) {
			List<String> split = line.words();
			String command = split.get(0);
			try {
				PropertyType[] types = controller.getParameterTypes(command);
				Object[] args = new Object[types.length];
				for (int i = 0; i < args.length; i++) {
					String json = i + 1 < split.size() ? split.get(i + 1) : "null";
					args[i] = deserializer.deserialize(json, types[i]);
				}
				controller.invoke(command, args);
			} catch (NoSuchMethodException e) {
				out.println("error", command + "?");
			}
		} else {
			out.println("error", "Each parameter must be in JSON format");
		}
	}

	public void exit() {
		controller.disconnect();
	}
}
