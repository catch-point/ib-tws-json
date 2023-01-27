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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;

/**
 * Finds the IBKR TWS API jar, appends it and this jar to the system class path,
 * then loads AspectJ
 *
 * @author James Leigh
 *
 */
public class Agent {
	private final static Logger logger = Logger.getLogger(Agent.class.getName());

	/**
	 * Start as a javaagent, adds TwsApi.jar to the class path, and loads aspectj weaver, for {@link ServerSocketHandler}
	 */
	public static void premain(String arg, Instrumentation inst) throws InterruptedException, IOException {
		Properties props = getAgentProperties(arg);
		appendToSystemClassLoader(props, inst);
		if (props.containsKey("json-api-port")) {
			startServer(props);
		} else {
			org.aspectj.weaver.loadtime.Agent.premain(arg, inst);
			initializeServerSocketHandler(props);
		}
	}

	/**
	 * Start as a javaagent, adds TwsApi.jar to the class path, and loads aspectj weaver, for {@link ServerSocketHandler}
	 */
	public static void agentmain(String arg, Instrumentation inst) throws InterruptedException, IOException {
		Properties props = getAgentProperties(arg);
		appendToSystemClassLoader(props, inst);
		if (props.containsKey("json-api-port")) {
			startServer(props);
		} else {
			org.aspectj.weaver.loadtime.Agent.agentmain(arg, inst);
			initializeServerSocketHandler(props);
		}
	}

	/**
	 * Adds TwsApi.jar to the class path
	 */
	private static void appendToSystemClassLoader(Properties props, Instrumentation inst) throws IOException {
		String jar = props.getProperty("tws-api-jar");
		String path = props.getProperty("tws-api-path");
		File tws_api_jar = Launcher.getTwsApiJar(jar, path);
		if (tws_api_jar != null) {
			inst.appendToSystemClassLoaderSearch(new JarFile(tws_api_jar));
		}
		if (Agent.class.getClassLoader() instanceof URLClassLoader) {
			URL[] sys = ((URLClassLoader) Agent.class.getClassLoader()).getURLs();
			for (URL url : sys) {
				try {
					File file = Paths.get(url.toURI()).toFile();
					if (file.isFile()) {
						inst.appendToSystemClassLoaderSearch(new JarFile(file));
					}
				} catch (URISyntaxException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Starts a JSON API server without using AspectJ to find the TWS API port
	 */
	private static void startServer(Properties props) throws UnknownHostException {
		int twsPort = Integer.parseInt(props.getProperty("tws-api-port", "0"));
		int jsonPort = Integer.parseInt(props.getProperty("json-api-port", "0"));
		int jsonPortOffset = Integer.parseInt(props.getProperty("json-api-port-offset", "100"));
		InetAddress inet = props.containsKey("json-api-inet") ? InetAddress.getByName(props.getProperty("json-api-inet")) : InetAddress.getLoopbackAddress();
		int local_port = jsonPort > 0 ? jsonPort : jsonPortOffset + twsPort;
		Server server = new Server(inet, local_port);
		if (twsPort > 0) {
			server.setRemote(InetAddress.getLoopbackAddress(), twsPort);
		}
		server.start();
	}

	/**
	 * Initializes {@link ServerSocketHandler} to wait for the TWS API port
	 */
	private static void initializeServerSocketHandler(Properties props) {
		ServerSocketHandler.setPortOffset(Integer.parseInt(props.getProperty("json-api-port-offset", "100")));
		int port = Integer.parseInt(props.getProperty("json-api-port", "0"));
		if (port > 0) {
			ServerSocketHandler.setPort(port);
		}
		if (props.containsKey("tws-api-port")) {
			ServerSocketHandler.setTwsPort(Integer.parseInt(props.getProperty("tws-api-port")));
		}
		try {
			if (props.containsKey("json-api-inet")) {
				ServerSocketHandler.setInet(InetAddress.getByName(props.getProperty("json-api-inet")));
			} else {
				ServerSocketHandler.setInet(InetAddress.getLoopbackAddress());
			}
		} catch (UnknownHostException e) {
			logger.warning(e.getMessage());
		}
		ServerSocketHandler.initialize();
	}

	/**
	 * Parses this agent (json-like) arg string and load the current jtsConfigDir settings
	 */
	private static Properties getAgentProperties(String arg) {
		Properties props = parseAgentArg(arg);
		String json = props.getProperty(findJtsConfigKey(props.keySet()), "{}");
		JsonObject object = decodeObject(json);
		for (Entry<String, JsonValue> e : object.entrySet()) {
			JsonValue val = e.getValue();
			if (val instanceof JsonString) {
				props.put(e.getKey(), ((JsonString)val).getString());
			} else if (val instanceof JsonNumber) {
				props.put(e.getKey(), ((JsonNumber)val).numberValue().toString());
			} else {
				props.put(e.getKey(), val.toString());
			}
		}
		return props;
	}

	private static String findJtsConfigKey(Set<Object> keySet) {
		String jtsConfigDir = System.getProperty("jtsConfigDir");
		if (jtsConfigDir == null) {
			return "null";
		} else if (keySet.contains(jtsConfigDir)) {
			return jtsConfigDir;
		} else {
			for (Object key : keySet) {
				if (new File(jtsConfigDir).equals(new File(key.toString()))) {
					return key.toString();
				}
			}
			return jtsConfigDir;
		}
	}

	/**
	 * Parses this agent (json-like) arg string into a Properties
	 */
	static Properties parseAgentArg(String arg) {
		Properties props = new Properties();
		Character quoted = null;
		boolean backslash = false;
		int objects = 0;
		String key = null;
		StringBuilder value = new StringBuilder();
		if (arg != null) {
			for (char ch : arg.toCharArray()) {
				if (quoted != null && backslash) {
					backslash = false;
					value.append(ch);
				} else if (quoted != null && ch == '\\') {
					backslash = true;
					value.append(ch);
				} else if (quoted != null && ch == quoted.charValue()) {
					quoted = null;
					value.append(ch);
					if (objects < 1) {
						// don't decode strings in nested objects
						value.replace(0, value.length(), decodeQuotedString(value));
					}
				} else if (quoted == null && ch == '"') {
					quoted = ch;
					value.append(ch);
				} else if (quoted == null && objects < 1 && ch == ':') {
					key = value.toString();
					value.setLength(0);
				} else if (quoted == null && key != null && ch == '{') {
					objects++;
					value.append(ch);
				} else if (quoted == null && key != null && ch == '}') {
					objects--;
					value.append(ch);
				} else if (quoted == null && key != null && objects > 0) {
					objects++;
					value.append(ch);
				} else if (quoted == null && key != null && ch == ',') {
					props.put(key, value.toString());
					key = null;
					value.setLength(0);
				} else if (quoted == null && Character.isWhitespace(ch)) {
					// ignore whitespace
				} else {
					value.append(ch);
				}
			}
			if (key != null) {
				props.put(key, value.toString());
			}
		}
		return props;
	}

	/**
	 * Parses JSON string into a java.lang.String
	 */
	private static String decodeQuotedString(CharSequence value) {
		JsonParser parser = Json.createParser(new StringReader(value.toString()));
		parser.next();
		return parser.getString();
	}

	/**
	 * Parses JSON object
	 */
	private static JsonObject decodeObject(CharSequence value) {
		JsonParser parser = Json.createParser(new StringReader(value.toString()));
		parser.next();
		return parser.getObject();
	}

}
