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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The main entry point for the Read-eval-print loop
 *
 * @author James Leigh
 *
 */
public class Shell {
	private final Logger logger = Logger.getLogger(Shell.class.getName());
	private final Deserializer deserializer = new Deserializer();
	private Invoker controller;
	private Printer out;
	private LineReader reader;

	public static void main(String[] args) throws InterruptedException, IOException {
		Options options = new Options();
		options.addOption(null, "tws-settings-path", true, "Where TWS will read/store settings");
		options.addOption(null, "no-prompt", false, "Don't prompt for input");
		options.addOption("s", "silence", false, "Don't log to stderr");
		options.addOption("i", "interactive", false, "Enter interactive mode after executing a script file");
		options.addOption("h", "help", false, "This message");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println(exp.getMessage());
			System.exit(1);
			return;
		}
		if (cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ib-tws-shell [options] [scirpt files..]", options);
			System.exit(0);
			return;
		}
		String ibDir = cmd.hasOption("tws-settings-path") ? cmd.getOptionValue("tws-settings-path")
				: new File(System.getProperty("user.home"), "Jts").getPath();
		Shell shell = new Shell(ibDir, !cmd.hasOption("no-prompt"));
		if (cmd.hasOption("silence")) {
			PrintStream sink = new PrintStream(new OutputStream() {

				@Override
				public void write(int point) throws IOException {
					// n/a
				}
			});
			System.setOut(sink);
			System.setErr(sink);
		} else {
			System.setOut(System.err);
		}
		if (!cmd.hasOption("no-prompt")) {
			System.err
					.println("Welcome to ib-tws-shell! Type 'help' to see a list of commands or 'login' to open TWS.");
		}
		for (String script : cmd.getArgList()) {
			FileInputStream file = new FileInputStream(script);
			try {
				shell.repl(file);
			} finally {
				file.close();
			}
		}
		if (cmd.getArgList().isEmpty() || cmd.hasOption("interactive")) {
			shell.repl();
		}
		shell.exit();
	}

	public Shell(String ibDir, boolean prompt) throws IOException {
		Prompter prompter = prompt ? new Prompter(System.err) : new Prompter();
		reader = new LineReader(System.in, prompter);
		this.out = new Printer(prompter, System.out);
		controller = new Invoker(ibDir, this.getPrinter());
	}

	public Shell(InputStream in, OutputStream out) throws IOException {
		this(System.getProperty("user.dir"), in, out);
	}

	public Shell(String ibDir, InputStream in, OutputStream out) throws IOException {
		reader = new LineReader(in);
		this.out = new Printer(out);
		controller = new Invoker(ibDir, this.getPrinter());
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
				logger.log(Level.WARNING, "" + input.getInput(), e);
				String msg = e.getMessage() == null ? e.toString() : e.getMessage();
				getPrinter().println("error", msg + " while evaluating " + input.getInput());
			}
		} catch (SyntaxError e) {
			logger.log(Level.WARNING, e.getMessage(), e);
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
