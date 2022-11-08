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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Finds the JRE, VM args, TWS jars, and IB TWS API jar and launches a JVM
 *
 * @author James Leigh
 *
 */
public class Launcher {
	private static final String HEADER = "where [options] include:";
	private static final String FOOTER = "Please report issues at https://github.com/jamesrdf/ib-tws-shell/issues";
	private static final String TWS_API_JAR = "TwsApi.jar";
	private static final File[] tws_path_search = new File[] { new File("C:\\Jts\\ibgateway"), new File("C:\\Jts"),
			new File(new File(System.getProperty("user.home"), "Jts"), "ibgateway"),
			new File(System.getProperty("user.home"), "Jts"),
			new File(System.getProperty("user.home"), "Applications") };
	private static final File[] tws_api_path_search = new File[] { new File("C:\\TWS API"),
			new File(System.getProperty("user.home"), "IBJts"), new File(System.getProperty("user.home"), "Jts"),
			new File(System.getProperty("user.home"), "Downloads"),
			new File(System.getProperty("user.home"), "Download"), new File(System.getProperty("user.home"), "lib"),
			new File(System.getProperty("user.home"), "libs"),
			new File(System.getProperty("java.class.path").split("path.separator")[0]).getParentFile() };

	public static void main(String[] args) throws Throwable {
		Options options = new Options();
		options.addOption(null, "tws-version", true, "The major version number of the installed TWS software");
		options.addOption(null, "tws-path", true,
				"Location of Jts/ibgateway/Trader Workstation/IB Gateway folder to use");
		options.addOption(null, "tws-settings-path", true, "Where TWS will read/store settings");
		options.addOption(null, "tws-api-path", true, "The TwsApi directory to be searched for TwsApi.jar");
		options.addOption(null, "tws-api-jar", true, "The TwsApi.jar filename");
		options.addOption(null, "java-home", true, "The location of the jre to launch");
		options.addOption(null, "no-prompt", false, "Don't prompt for input");
		options.addOption("s", "silence", false, "Don't log to stderr");
		options.addOption("i", "interactive", false, "Enter interactive mode after executing a script file");
		options.addOption("h", "help", false, "This message");
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println(exp.getMessage());
			System.exit(1);
			return;
		}
		if (cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ib-tws-shell [options] [scirpt files..]", HEADER, options, FOOTER);
			System.exit(0);
			return;
		}
		List<String> shell_args = new ArrayList<>();
		if (cmd.hasOption("tws-settings-path")) {
			shell_args.add("--tws-settings-path");
			shell_args.add(cmd.getOptionValue("tws-settings-path"));
		}
		if (cmd.hasOption("no-prompt")) {
			shell_args.add("--no-prompt");
		}
		if (cmd.hasOption("silence")) {
			shell_args.add("--silence");
		}
		if (cmd.hasOption("interactive")) {
			shell_args.add("interactive");
		}
		String java = getJavaExe(cmd);
		Collection<String> vm_args = getJVMOptions(cmd);
		String cp;
		try {
			cp = getClassPath(cmd);
		} catch (Exception err) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ib-tws-shell [options] [scirpt files..]", HEADER, options, err.getMessage());
			System.exit(1);
			return;
		}
		List<String> command = new ArrayList<>();
		command.add(java);
		command.addAll(vm_args);
		command.add("-cp");
		command.add(cp);
		command.add(Launcher.class.getPackage().getName() + ".Shell");
		command.addAll(shell_args);
		Process shell = new ProcessBuilder(command).redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT)
				.redirectError(Redirect.INHERIT).start();
		try {
			System.exit(shell.waitFor());
		} catch (InterruptedException e) {
			shell.destroy();
			System.exit(shell.waitFor());
		}
	}

	/**
	 * Locates the java executable
	 */
	private static String getJavaExe(CommandLine cmd) throws IOException {
		String javaHome = getJavaRuntimeEnvironment(cmd);
		if (javaHome == null || !new File(javaHome).isDirectory())
			System.err.println("Cannot found JRE trying changing --java-home=...");
		return new File(new File(javaHome, "bin"), "java").getPath();
	}

	/**
	 * Locals the JRE on the system by looking in the usual TWS locations.
	 */
	private static String getJavaRuntimeEnvironment(CommandLine cmd) throws IOException {
		if (cmd.hasOption("java-home"))
			return cmd.getOptionValue("java-home");
		File install4j = getInstall4j(cmd);
		if (install4j == null || !install4j.isDirectory())
			return System.getProperty("java.home");
		String[] jre_search = new String[] {
				new File(new File(new File(new File(install4j, "jre.bundle"), "Contents"), "Home"), "jre").getPath(),
				readFile(new File(install4j, "pref_jre.cfg")), readFile(new File(install4j, "inst_jre.cfg")) };
		for (String jre : jre_search) {
			if (jre != null && new File(jre).isDirectory())
				return jre;
		}
		return null;
	}

	/**
	 * Locates the .install4j folders of a TWS install
	 */
	private static File getInstall4j(CommandLine cmd) {
		for (File jts_path : getJtsPathSearch(cmd)) {
			String version = getJtsVersion(jts_path, cmd);
			File[] search = version != null
					? new File[] { new File(new File(jts_path, version), ".install4j"),
							new File(new File(new File(jts_path, "ibgateway"), version), ".install4j"),
							new File(new File(jts_path, "IB Gateway " + version), ".install4j"),
							new File(new File(jts_path, "Trader Workstation " + version), ".install4j"),
							new File(jts_path, ".install4j"), new File(new File(jts_path, "ibgateway"), ".install4j"),
							new File(new File(jts_path, "IB Gateway"), ".install4j"),
							new File(new File(jts_path, "Trader Workstation"), ".install4j") }
					: new File[] { new File(jts_path, ".install4j"),
							new File(new File(jts_path, "ibgateway"), ".install4j"),
							new File(new File(jts_path, "IB Gateway"), ".install4j"),
							new File(new File(jts_path, "Trader Workstation"), ".install4j") };
			for (File i4j : search) {
				if (i4j.isDirectory())
					return i4j;
			}
		}
		if (!cmd.hasOption("tws-path")) {
			System.err.println("Could not find .install4j try --tws-path=...");
		} else if (!cmd.hasOption("tws-version")) {
			System.err.println("Could not find .install4j try --tws-version=...");
		} else {
			System.err.println("Could not find .install4j");
		}
		return null;
	}

	private static String readFile(File file) throws IOException {
		if (file == null || !file.canRead())
			return null;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			return reader.readLine();
		} finally {
			reader.close();
		}
	}

	private static Collection<String> getJVMOptions(CommandLine cmd) throws IOException {
		List<String> opts = getVMOptions(cmd);
		List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
		Set<String> set = new LinkedHashSet<>(opts.size() + args.size());
		set.addAll(args);
		set.addAll(opts);
		return set;
	}

	/**
	 * Reads the JVM arguments in the TWS install directory
	 */
	private static List<String> getVMOptions(CommandLine cmd) throws IOException {
		File vmoptions = getVMOptionsFile(cmd);
		if (vmoptions == null || !vmoptions.isFile())
			return Collections.emptyList();
		List<String> opts = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(vmoptions));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() > 0 && line.charAt(0) != '#') {
					opts.add(line);
				}
			}
		} finally {
			reader.close();
		}
		return opts;
	}

	/**
	 * Finds the vmoptions file in the TWS install directory
	 */
	private static File getVMOptionsFile(CommandLine cmd) {
		Pattern regex = Pattern.compile("gateway", Pattern.CASE_INSENSITIVE);
		for (File jts_path : getJtsPathSearch(cmd)) {
			String version = getJtsVersion(jts_path, cmd);
			boolean isGateway = regex.matcher(jts_path.getPath()).find();
			File[] search = version != null
					? new File[] {
							new File(new File(jts_path, version), isGateway ? "ibgateway.vmoptions" : "tws.vmoptions"),
							new File(jts_path, isGateway ? "ibgateway.vmoptions" : "tws.vmoptions"),
							new File(new File(new File(jts_path, "ibgateway"), version), "ibgateway.vmoptions"),
							new File(new File(jts_path, "IB Gateway " + version), "ibgateway.vmoptions"),
							new File(new File(jts_path, "Trader Workstation " + version), "tws.vmoptions"),
							new File(new File(System.getProperty("user.home"), "Jts"),
									(isGateway ? "ibgateway-" : "tws-") + version + ".vmoptions"),
							new File(new File(jts_path, "ibgateway"), "ibgateway.vmoptions"),
							new File(new File(jts_path, "IB Gateway"), "ibgateway.vmoptions"),
							new File(new File(jts_path, "Trader Workstation"), "tws.vmoptions"),
							new File(new File(System.getProperty("user.home"), "Jts"),
									isGateway ? "ibgateway.vmoptions" : "tws.vmoptions") }
					: new File[] { new File(jts_path, isGateway ? "ibgateway.vmoptions" : "tws.vmoptions"),
							new File(new File(System.getProperty("user.home"), "Jts"),
									(isGateway ? "ibgateway-" : "tws-") + version + ".vmoptions"),
							new File(new File(jts_path, "ibgateway"), "ibgateway.vmoptions"),
							new File(new File(jts_path, "IB Gateway"), "ibgateway.vmoptions"),
							new File(new File(jts_path, "Trader Workstation"), "tws.vmoptions"),
							new File(new File(System.getProperty("user.home"), "Jts"),
									isGateway ? "ibgateway.vmoptions" : "tws.vmoptions") };
			for (File vmoptions : search) {
				if (vmoptions.isFile())
					return vmoptions;
			}
		}
		if (!cmd.hasOption("tws-path")) {
			System.err.println("Could not find vmoptions try --tws-path=...");
		} else if (!cmd.hasOption("tws-version")) {
			System.err.println("Could not find vmoptions try --tws-version=...");
		} else {
			System.err.println("Could not find vmoptions");
		}
		return null;
	}

	/**
	 * Creates the Java Class Path
	 */
	private static String getClassPath(CommandLine cmd) throws MalformedURLException {
		String[] classpath = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		List<File> jars = new ArrayList<>();
		Collection<File> jts_jars = getJtsJars(cmd);
		if (!jts_jars.isEmpty()) {
			jars.addAll(jts_jars);
		}
		File tws_api_jar = getTwsApiJar(cmd);
		if (tws_api_jar != null) {
			jars.add(tws_api_jar);
		} else {
			throw new IllegalArgumentException("Could not find TwsApi.jar try --tws-api-jar=...");
		}
		for (int i = 0; i < classpath.length; i++) {
			jars.add(new File(classpath[i]));
		}
		StringBuilder sb = new StringBuilder();
		for (File jar : jars) {
			if (sb.length() > 0) {
				sb.append(System.getProperty("path.separator"));
			}
			sb.append(jar.getPath());
		}
		return sb.toString();
	}

	/**
	 * Identify all the jars needed to launch TWS
	 */
	private static Collection<File> getJtsJars(CommandLine cmd) throws MalformedURLException {
		File jars_dir = getJtsJarsDir(cmd);
		if (jars_dir == null)
			return Collections.emptyList();
		Map<String, File> jars = new LinkedHashMap<>();
		for (String jar : listNumerically(jars_dir)) {
			String key = jar.replaceFirst("[^a-z0-9][0-9].*$", "");
			if (!jars.containsKey(key)) {
				jars.put(key, new File(jars_dir, jar));
			}
		}
		return jars.values();
	}

	/**
	 * Finds the jars directory in the TWS install
	 */
	private static File getJtsJarsDir(CommandLine cmd) {
		for (File jts_path : getJtsPathSearch(cmd)) {
			String version = getJtsVersion(jts_path, cmd);
			File[] search = version != null
					? new File[] { new File(new File(jts_path, version), "jars"),
							new File(new File(new File(jts_path, "ibgateway"), version), "jars"),
							new File(new File(jts_path, "IB Gateway " + version), "jars"),
							new File(new File(jts_path, "Trader Workstation " + version), "jars"),
							new File(jts_path, "jars"), new File(new File(jts_path, "ibgateway"), "jars"),
							new File(new File(jts_path, "IB Gateway"), "jars"),
							new File(new File(jts_path, "Trader Workstation"), "jars") }
					: new File[] { new File(jts_path, "jars"), new File(new File(jts_path, "ibgateway"), "jars"),
							new File(new File(jts_path, "IB Gateway"), "jars"),
							new File(new File(jts_path, "Trader Workstation"), "jars") };
			for (File jars : search) {
				if (jars.isDirectory())
					return jars;
			}
		}
		if (!cmd.hasOption("tws-path")) {
			System.err.println("Could not find jars try --tws-path=...");
		} else if (!cmd.hasOption("tws-version")) {
			System.err.println("Could not find jars try --tws-version=...");
		} else {
			System.err.println("Could not find jars");
		}
		return null;
	}

	/**
	 * List of common TWS install locations
	 */
	private static File[] getJtsPathSearch(CommandLine cmd) {
		File[] jts_search_path = cmd.hasOption("tws-path") ? new File[] { new File(cmd.getOptionValue("tws-path")) }
				: tws_path_search;
		return jts_search_path;
	}

	/**
	 * Looks for installed TWS versions on the system
	 */
	private static String getJtsVersion(File jts_path, CommandLine cmd) {
		if (cmd.hasOption("tws-version"))
			return cmd.getOptionValue("tws-version");
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
	 * Finds the TwsApi.jar file
	 */
	private static File getTwsApiJar(CommandLine cmd) {
		if (cmd.hasOption("tws-api-jar"))
			return new File(cmd.getOptionValue("tws-api-jar"));
		if (cmd.hasOption("tws-api-path"))
			return searchFor(new File(cmd.getOptionValue("tws-api-path")), TWS_API_JAR);
		for (File dir : tws_api_path_search) {
			if (dir.isDirectory()) {
				File found = searchFor(dir, TWS_API_JAR);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * Searches the folder for filename
	 */
	private static File searchFor(File folder, String filename) {
		for (String ls : listNumerically(folder)) {
			if (ls.equalsIgnoreCase(filename)) {
				return new File(folder, ls);
			} else if (new File(folder, ls).isDirectory()) {
				File found = searchFor(new File(folder, ls), filename);
				if (found != null) {
					return found;
				}
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
