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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Stand alone client using the Read-eval-print loop and standard-in/out
 *
 * @author James Leigh
 *
 */
public class Shell {
	private static final String JSON_API_EXTENSION_REF = "JSON API Extension See https://github.com/jamesrdf/ib-tws-json";
	private static final String HEADER = "where [options] include:";
	private static final String FOOTER = "Please report issues at https://github.com/jamesrdf/ib-tws-json/issues";
	private static final File[] tws_path_search = new File[] { new File("C:\\Jts\\ibgateway"), new File("C:\\Jts"),
			new File(new File(System.getProperty("user.home"), "Jts"), "ibgateway"),
			new File(System.getProperty("user.home"), "Jts"),
			new File(System.getProperty("user.home"), "Applications") };

	/**
	 * Called when TwsApi.jar is in the classpath
	 */
	public static void main(String[] args) throws Throwable {
		CommandLine cmd = parseCommandLine(args);
		boolean launch = cmd.hasOption("launch") || args.length == 0;
		boolean extension = cmd.hasOption("uninstall") || cmd.hasOption("install");
		boolean attach = extension || launch;
		if (attach) {
			File vmoptions = getVMOptionsFile(cmd);
			if (vmoptions == null && extension) {
				System.err.println("Could not find TWS");
				System.exit(1);
			}
			File jar = findThisJar();
			if (jar == null && extension) {
				System.err.println("Could not find JAR file");
				System.exit(1);
			}
			if (vmoptions != null && jar != null) {
				modifyJtsVMOptions(vmoptions, jar, cmd);
			}
		}
		Process p = launch ? launchJts(cmd) : null;
		boolean interactive = cmd.hasOption("interactive") || cmd.hasOption("no-prompt") || !attach;
		if (interactive || cmd.getArgs().length > 0) {
			String host = cmd.getOptionValue("tws-api-host", "localhost");
			if (!cmd.hasOption("tws-api-port")) {
				System.err.println("Parameter missing --tws-api-port=...");
			} else {
				int port = Integer.parseInt(cmd.getOptionValue("tws-api-port", "7497"));
				if (p != null) {
					waitForLocalServer(p, port);
				}
				boolean prompt = !cmd.hasOption("no-prompt");
				Interpreter interpreter = new Interpreter(prompt);
				interpreter.setRemoteAddress(host, port);
				for (String arg : cmd.getArgs()) {
					FileInputStream in = new FileInputStream(arg);
					try {
						interpreter.repl(in);
					} finally {
						in.close();
					}
				}
				if (interactive) {
					interpreter.repl();
				}
				interpreter.exit();
			}
		}
		if ((interactive || cmd.getArgs().length > 0) && !cmd.hasOption("tws-api-port")) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}

	/**
	 * Parses args and prints a help message if needed
	 */
	static CommandLine parseCommandLine(String[] args) {
		Options options = new Options();
		options.addOption("h",  "help", false, "This message");
		options.addOption("v",  "version", false, "Print the current version and exit");
		options.addOption(null, "install", false, "Installs the TWS JSON extension from TWS");
		options.addOption(null, "uninstall", false, "Uninstalls the TWS JSON extension from TWS");
		options.addOption(null, "launch", false, "Installs the TWS JSON extension and starts TWS before exiting");
		options.addOption("i",  "interactive", false, "Enter interactive client mode");
		options.addOption(null, "no-prompt", false, "Don't prompt for input when in interactive mode");
		options.addOption(null, "tws-api-path", true, "The TwsApi directory to be searched for TwsApi.jar");
		options.addOption("j",  "tws-api-jar", true, "The TwsApi.jar filename");
		options.addOption(null, "tws-api-host", true, "Hostname or IP running TWS");
		options.addOption("p",  "tws-api-port", true, "Port TWS API is running on");
		options.addOption(null, "json-api-port", true, "Server port for TWS JSON API to listen on");
		options.addOption(null, "json-api-port-offset", true, "Server JSON port offset from tws-api-port");
		options.addOption(null, "json-api-inet", true, "Server local network interface to listen on for TWS JSON API");
		options.addOption(null, "jts-exe-name", true, "The primary launch filename installed by TWS software");
		options.addOption(null, "jts-install-dir", true,
				"Location of Jts/ibgateway/Trader Workstation/IB Gateway folder to use");
		options.addOption(null, "jts-config-dir", true, "Where TWS will read/store settings");
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println(exp.getMessage());
			System.exit(1);
			return null;
		}
		if (cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ib-tws-json [options] [scirpt files..]", HEADER, options, FOOTER);
			System.exit(0);
			return null;
		}
		if (cmd.hasOption("version")) {
			String version = Shell.class.getPackage().getImplementationVersion();
			System.out.println(version != null ? version : "devel");
			System.exit(0);
			return null;
		}
		return cmd;
	}

	/**
	 * Uninstall/install this extension in tws.vmoptions file
	 */
	private static void modifyJtsVMOptions(File vmoptions, File jar, CommandLine cmd)
			throws FileNotFoundException, IOException {
		List<String> lines = readLines(vmoptions);
		if (cmd.hasOption("uninstall")) {
			int udix = findIndex(lines, jar.getName().replaceFirst("[-0-9.]+(-[A-Za-z]+)?.(jar|JAR)$", ""));
			if (udix > 0) {
				lines.remove(udix);
			}
			if (udix > 2) {
				if (lines.get(udix - 1).startsWith("#") && lines.get(udix - 1).contains("http")) {
					lines.remove(udix - 1);
				}
				if (lines.get(udix - 2).length() < 1) {
					lines.remove(udix - 2);
				}
			}
		}
		if (cmd.hasOption("install") || !cmd.hasOption("uninstall")) {
			int idx = findIndex(lines, jar.getName());
			Properties existing = idx >= 0 ? parseAgentArg(lines.get(idx)) : new Properties();
			Properties props = saveSettings(cmd, existing);
			CharSequence agentArgs = formatAgentArgs(props);
			String agent = "-javaagent:" + jar.getAbsolutePath() + "=" + agentArgs;
			if (idx > 0) {
				lines.set(idx, agent);
			} else {
				lines.add("");
				lines.add("# " + JSON_API_EXTENSION_REF);
				lines.add(agent);
			}
		}
		writeLines(lines, vmoptions);
	}

	/**
	 * Launches IBKR TWS in a separate process VM
	 */
	private static Process launchJts(CommandLine cmd) throws IOException {
		String exec = getTwsExecFile(cmd).getAbsolutePath();
		String ibDir = cmd.hasOption("jts-config-dir") ? cmd.getOptionValue("jts-config-dir")
				: System.getProperty("jtsConfigDir") != null ? System.getProperty("jtsConfigDir") : null;
		String[] command = ibDir == null ? new String[] { exec }
				: new String[] { exec, ibDir, "-J-DjtsConfigDir=" + ibDir };
		return new ProcessBuilder(Arrays.asList(command)).inheritIO().start();
	}

	/**
	 * Waits until a server is listening on the given port
	 */
	private static boolean waitForLocalServer(Process p, int port) throws InterruptedException {
		boolean waiting = false;
		int ms = 1000;
		Thread.sleep(ms);
		while (p.isAlive()) {
			try {
				new ServerSocket(port).close();
				ms += ms;
				if (ms <= 0 || ms >= 10 * 60 * 60) {
					return false;
				}
				if (!waiting) {
					waiting = true;
					System.err.println("Waiting for JTS to startup...");
				}
				Thread.sleep(ms);
			} catch (IOException e) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the vmoptions file in the TWS install directory
	 */
	private static File getVMOptionsFile(CommandLine cmd) {
		File exec = getTwsExecFile(cmd);
		if (exec == null) {
			return null;
		}
		return new File(exec.getParent(), exec.getName() + ".vmoptions");
	}

	/**
	 * Searches this ClassLoader's URLs to find the JAR with this class in it.
	 */
	private static File findThisJar() {
		ClassLoader cl = Shell.class.getClassLoader();
		String target_entry = Shell.class.getName().replace(".", "/") + ".class";
		return findJar(cl, target_entry);
	}

	/**
	 * Reads all the lines in the given file
	 */
	private static List<String> readLines(File file) throws FileNotFoundException, IOException {
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		} finally {
			reader.close();
		}
		return lines;
	}

	/**
	 * Find the index of haystack that contains needle
	 */
	private static int findIndex(List<String> haystack, String needle) {
		for (int i = 0, n = haystack.size(); i < n; i++) {
			if (haystack.get(i) != null && haystack.get(i).contains(needle))
				return i;
		}
		return -1;
	}

	/**
	 * Parses a javaagent string using {@link Agent#parseAgentArg(String)}
	 */
	private static Properties parseAgentArg(String arg) {
		int idx = arg.indexOf("-javaagent:");
		if (idx >= 0) {
			return Agent.parseAgentArg(arg.substring(arg.indexOf('=', idx) + 1));
		} else {
			return null;
		}
	}

	/**
	 * Formats an agent arg string for use with {@link Agent#parseAgentArg(String)
	 */
	private static Properties saveSettings(CommandLine cmd, Properties existing) {
		Properties props = existing;
		File apiJar = Launcher.getTwsApiJar(cmd.getOptionValue("tws-api-jar"), cmd.getOptionValue("tws-api-path"));
		if (apiJar != null) {
			props.put("tws-api-jar", apiJar.getAbsolutePath());
		}
		if (cmd.hasOption("jts-config-dir")) {
			String jtsConfigDir = cmd.getOptionValue("jts-config-dir");
			JsonObject obj = decodeObject(existing.getProperty(jtsConfigDir, "{}"));
			JsonObjectBuilder object = Json.createObjectBuilder(obj);
			if (cmd.hasOption("tws-api-port")) {
				object.remove("tws-api-port").add("tws-api-port", Integer.parseInt(cmd.getOptionValue("tws-api-port")));
			}
			if (cmd.hasOption("json-api-port")) {
				object.remove("json-api-port").add("json-api-port", Integer.parseInt(cmd.getOptionValue("json-api-port")));
			} else if (cmd.hasOption("json-api-port-offset")) {
				object.remove("json-api-port-offset").add("json-api-port-offset",
						Integer.parseInt(cmd.getOptionValue("json-api-port-offset")));
			}
			if (cmd.hasOption("json-api-inet")) {
				object.remove("json-api-inet").add("json-api-inet", cmd.getOptionValue("json-api-inet"));
			}
			props.put(jtsConfigDir, encodeObject(object.build()));
		} else {
			if (cmd.hasOption("tws-api-port")) {
				props.put("tws-api-port", cmd.getOptionValue("tws-api-port"));
			}
			if (cmd.hasOption("json-api-port")) {
				props.put("json-api-port", cmd.getOptionValue("json-api-port"));
			} else if (cmd.hasOption("json-api-port-offset")) {
				props.put("json-api-port-offset", cmd.getOptionValue("json-api-port-offset"));
			}
			if (cmd.hasOption("json-api-inet")) {
				props.put("json-api-inet", cmd.getOptionValue("json-api-inet"));
			}
		}
		return props;
	}

	/**
	 * Formats an agent arg string for use with {@link Agent#parseAgentArg(String)
	 */
	private static CharSequence formatAgentArgs(Properties props) {
		StringBuilder sb = new StringBuilder();
		for (Object key : props.keySet()) {
			sb.append(encodeQuotedString(key.toString()));
			sb.append(":");
			if ("tws-api-jar".equals(key) || "json-api-inet".equals(key)) {
				sb.append(encodeQuotedString(props.get(key).toString()));
			} else if ("tws-api-port".equals(key) || "json-api-port".equals(key) || "json-api-port-offset".equals(key)) {
				sb.append(props.get(key));
			} else {
				// jts-config-dir JSON
				sb.append(props.get(key));
			}
			sb.append(",");

		}
		if (sb.length() > 0) {
			// remove the last comma
			sb.setLength(sb.length() - 1);
		}
		return sb;
	}

	/**
	 * Finds the executable file in the TWS install directory
	 */
	private static File getTwsExecFile(CommandLine cmd) {
		Pattern regex = Pattern.compile("gateway", Pattern.CASE_INSENSITIVE);
		for (File jts_path : getJtsPathSearch(cmd)) {
			String version = getJtsVersion(jts_path, cmd);
			boolean isGateway = regex.matcher(jts_path.getPath()).find();
			String exe = cmd.hasOption("jts-exe-name") ? cmd.getOptionValue("jts-exe-name")
					: isGateway ? "ibgateway" : "tws";
			String vmoptions = exe + ".vmoptions";
			File[] search = version != null
					? new File[] { new File(new File(jts_path, version), vmoptions), new File(jts_path, vmoptions),
							new File(new File(new File(jts_path, "ibgateway"), version), vmoptions),
							new File(new File(jts_path, "IB Gateway " + version), vmoptions),
							new File(new File(jts_path, "Trader Workstation " + version), vmoptions),
							new File(new File(System.getProperty("user.home"), "Jts"),
									cmd.hasOption("jts-exe-name") ? vmoptions
											: (isGateway ? "ibgateway-" : "tws-") + version + ".vmoptions"),
							new File(new File(jts_path, "ibgateway"), vmoptions),
							new File(new File(jts_path, "IB Gateway"), vmoptions),
							new File(new File(jts_path, "Trader Workstation"), vmoptions),
							new File(new File(System.getProperty("user.home"), "Jts"), vmoptions) }
					: new File[] { new File(jts_path, vmoptions),
							new File(new File(System.getProperty("user.home"), "Jts"),
									cmd.hasOption("jts-exe-name") ? vmoptions
											: (isGateway ? "ibgateway-" : "tws-") + version + ".vmoptions"),
							new File(new File(jts_path, "ibgateway"), vmoptions),
							new File(new File(jts_path, "IB Gateway"), vmoptions),
							new File(new File(jts_path, "Trader Workstation"), vmoptions),
							new File(new File(System.getProperty("user.home"), "Jts"), vmoptions) };
			for (File vmoption : search) {
				String dir = vmoption.getParent();
				String name = vmoption.getName();
				File exec = new File(dir, name.substring(0, name.length() - ".vmoptions".length()));
				if (vmoption.isFile() && exec.isFile())
					return exec;
			}
		}
		if (!cmd.hasOption("jts-install-dir")) {
			System.err.println("Could not find TWS try --jts-install-dir=...");
		} else if (!cmd.hasOption("jts-exe-name")) {
			System.err.println("Could not find TWS try --jts-exe-name=...");
		} else {
			System.err.println("Could not find TWS");
		}
		return null;
	}

	/**
	 * Writes lines into the given file
	 */
	private static void writeLines(List<String> lines, File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		try {
			for (String line : lines) {
				writer.write(line);
				writer.write(System.lineSeparator());
			}
		} finally {
			writer.close();
		}
	}

	/**
	 * Searches the JARs in the given ClassLoader (and it's parent(s)) to find the
	 * target_entry
	 */
	private static File findJar(ClassLoader cl, String target_entry) {
		if (cl instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) cl).getURLs();
			for (URL url : urls) {
				try {
					File file = Paths.get(url.toURI()).toFile();
					if (file.isFile()) {
						JarFile jar = new JarFile(file);
						try {
							Enumeration<JarEntry> e = jar.entries();
							while (e.hasMoreElements()) {
								JarEntry entry = e.nextElement();
								if (entry.getName().equals(target_entry)) {
									return file;
								}
							}
						} finally {
							jar.close();
						}
					}
				} catch (URISyntaxException e) {
					// ignore
				} catch (IOException e) {
					// ignore
				}
			}
		} else if (cl == null) {
			return null;
		}
		return findJar(cl.getParent(), target_entry);
	}

	/**
	 * Encodes a java.lang.String into a JSON string representation
	 */
	private static String encodeQuotedString(CharSequence value) {
		StringWriter writer = new StringWriter();
		JsonGenerator generator = Json.createGenerator(writer);
		generator.write(value.toString());
		generator.close();
		return writer.toString();
	}

	/**
	 * Parses a JSON object
	 */
	private static JsonObject decodeObject(CharSequence value) {
		JsonParser parser = Json.createParser(new StringReader(value.toString()));
		parser.next();
		return parser.getObject();
	}

	/**
	 * Serializes a JSON object
	 */
	private static String encodeObject(JsonObject obj) {
		StringWriter writer = new StringWriter();
		JsonGenerator generator = Json.createGenerator(writer);
		generator.write(obj);
		generator.close();
		return writer.toString();
	}

	/**
	 * List of common TWS install locations
	 */
	private static File[] getJtsPathSearch(CommandLine cmd) {
		File[] jts_search_path = cmd.hasOption("jts-install-dir")
				? new File[] { new File(cmd.getOptionValue("jts-install-dir")) }
				: tws_path_search;
		return jts_search_path;
	}

	/**
	 * Looks for installed TWS versions on the system
	 */
	private static String getJtsVersion(File jts_path, CommandLine cmd) {
		if (cmd.hasOption("jts-install-dir") && cmd.hasOption("jts-exe-name"))
			return null;
		Pattern regex = Pattern.compile("^(IB Gateway |Trader Workstation |ibgateway-|tws-)?([0-9]+)(\\.vmoptions)?$");
		for (String ls : listNumerically(jts_path)) {
			File dir = new File(jts_path, ls);
			Matcher m = regex.matcher(ls);
			if (dir.exists() && m.find()) {
				return m.group(2);
			}
		}
		return null;
	}

	/**
	 * Lists the contents of the directory with the highest version numbers first
	 */
	private static String[] listNumerically(File dir) {
		Pattern digits = Pattern.compile("^[0-9]+$");
		Set<String> list = new TreeSet<String>(new Comparator<String>() {

			@Override
			public int compare(String arg0, String arg1) {
				String[] split0 = arg0.split("[^a-z0-9]+");
				String[] split1 = arg1.split("[^a-z0-9]+");
				for (int i = 0; i < split0.length && i < split1.length; i++) {
					if (digits.matcher(split0[i]).find() && digits.matcher(split1[i]).find()) {
						int cmp = new BigInteger(split0[i]).compareTo(new BigInteger(split1[i]));
						if (cmp != 0) {
							return cmp;
						}
					} else if (split0[i].compareTo(split1[i]) != 0) {
						return split0[i].compareTo(split1[i]);
					}
				}
				return arg0.compareTo(arg1);
			}
		}.reversed());
		String[] ls = dir.list();
		if (ls == null)
			return new String[0];
		list.addAll(Arrays.asList(ls));
		return list.toArray(new String[list.size()]);
	}
}
