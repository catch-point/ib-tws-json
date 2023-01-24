package com.meerkattrading.tws;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class TestAgent {

	@Test
	public void testEmptyArg() throws IOException {
		assertEquivalent("", "");
	}

	@Test
	public void testTwsApiJarArg() throws IOException {
		assertEquivalent("tws-api-jar:file.jar", "tws-api-jar:file.jar");
	}

	@Test
	public void testTwsApiPathArg() throws IOException {
		assertEquivalent("tws-api-path:dir/", "tws-api-path:dir/");
	}

	@Test
	public void testTwsApiJarPathArg() throws IOException {
		assertEquivalent("tws-api-jar:file.jar\ntws-api-path:dir/", "tws-api-jar:file.jar,tws-api-path:dir/");
	}

	@Test
	public void testDoubleQuoted() throws IOException {
		assertEquivalent("tws-api-path:dir with spaces/", "\"tws-api-path\":\"dir with spaces/\"");
	}

	@Test
	public void testObject() throws IOException {
		assertEquivalent("config:{\"prop\":\"value\"}", "config:{\"prop\":\"value\"}");
	}


	@Test
	public void testDoubleQuotedBackslash() throws IOException {
		assertEquivalent("tws-api-path:dir\twith \\\"spaces\\\"/", "tws-api-path:\"dir\\twith \\\"spaces\\\"/\"");
	}

	private static void assertEquivalent(String properties_format, String arg_format) throws IOException {
		Assert.assertEquals(readProperties(properties_format), Agent.parseAgentArg(arg_format));
	}

	private static Properties readProperties(String input) throws IOException {
		Properties props = new Properties();
		props.load(new StringReader(input));
		return props;
	}
}
