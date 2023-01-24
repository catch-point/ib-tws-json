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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;

/**
 * Finds the TwsApi.jar and launches a {@link Shell} in a separate {@link URLClassLoader}
 *
 * @author James Leigh
 *
 */
public class Launcher {
	private static final String TWS_API_JAR = "TwsApi.jar";
	private static final File[] tws_api_path_search = new File[] { new File("C:\\TWS API"),
			new File(System.getProperty("user.home"), "IBJts"), new File(System.getProperty("user.home"), "Jts"),
			new File(System.getProperty("user.home"), "Downloads"),
			new File(System.getProperty("user.home"), "Download"), new File(System.getProperty("user.home"), "lib"),
			new File(System.getProperty("user.home"), "libs"),
			new File(System.getProperty("java.class.path").split("path.separator")[0]).getParentFile() };

	/**
	 * Primary entry point to search for TwsApi.jar and launch {@link Shell}
	 */
	public static void main(String[] args) throws Throwable {
		CommandLine cmd = Shell.parseCommandLine(args);
		String jar = cmd.getOptionValue("tws-api-jar");
		String path = cmd.getOptionValue("tws-api-path");
		URL[] cp = getClassPath(jar, path);
		URLClassLoader cl = new URLClassLoader(cp, ClassLoader.getSystemClassLoader().getParent());
		Thread.currentThread().setContextClassLoader(cl);
		try {
			Class<?> Shell = cl.loadClass(Launcher.class.getPackage().getName() + ".Shell");
			Method main = Shell.getMethod("main", String[].class);
			main.invoke(null, new Object[] {args});
		} catch (InvocationTargetException exec) {
			throw exec.getCause();
		} finally {
			cl.close();
		}
	}

	/**
	 * Creates the Java Class Path
	 */
	static URL[] getClassPath(String jar, String path) throws MalformedURLException {
		List<URL> jars = new ArrayList<>();
		File tws_api_jar = getTwsApiJar(jar, path);
		if (tws_api_jar != null) {
			jars.add(tws_api_jar.toURI().toURL());
		} else {
			throw new IllegalArgumentException("Could not find TwsApi.jar try --tws-api-jar=...");
		}
		ClassLoader[] loaders = new ClassLoader[] { Launcher.class.getClassLoader(),
				Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader() };
		for (ClassLoader cl : loaders) {
			if (cl instanceof URLClassLoader) {
				URL[] sys = ((URLClassLoader)cl).getURLs();
				for (URL url : sys) {
					jars.add(url);
				}
			}
		}
		String[] classpath = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		for (int i = 0; i < classpath.length; i++) {
			jars.add(new File(classpath[i]).toURI().toURL());
		}
		URL[] cp = new URL[jars.size()];
		for (int i=0; i<cp.length; i++) {
			cp[i] = jars.get(i);
		}
		return cp;
	}

	/**
	 * Finds the TwsApi.jar file
	 */
	static File getTwsApiJar(String jar, String path) {
		if (jar != null) {
			return new File(jar);
		}
		if (path != null) {
			return searchFor(new File(path), TWS_API_JAR);
		}
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
