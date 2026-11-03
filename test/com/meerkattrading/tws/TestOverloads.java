package com.meerkattrading.tws;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import com.ib.client.Types.FADataType;

import junit.framework.TestCase;

/**
 * Overloaded EClient methods are all reachable as JSON commands: the variant is
 * picked from the number and JSON types of the arguments. Enum parameters
 * accept their name, or a number/numeric string matched to the enum's
 * id()/val()/value()/code() when it has one, otherwise to its ordinal.
 */
public class TestOverloads extends TestCase {
	private ByteArrayOutputStream out;
	private Printer printer;
	private Invoker invoker;

	@Override
	protected void setUp() throws Exception {
		out = new ByteArrayOutputStream();
		printer = new Printer(out);
		invoker = new Invoker(printer);
	}

	private Invoker.Call resolve(String command, String... jsonArgs) throws Exception {
		return invoker.resolve(command, Arrays.asList(jsonArgs));
	}

	private static List<Class<?>> params(Invoker.Call call) {
		return Arrays.asList(call.getMethod().getParameterTypes());
	}

	public void testRequestFAWithNumberUsesIntVariant() throws Exception {
		Invoker.Call call = resolve("requestFA", "1");
		assertEquals(Arrays.asList(int.class), params(call));
		assertEquals(1, call.getArgs()[0]);
	}

	public void testRequestFAWithNameUsesEnumVariant() throws Exception {
		Invoker.Call call = resolve("requestFA", "\"GROUPS\"");
		assertEquals(Arrays.asList(FADataType.class), params(call));
		assertEquals(FADataType.GROUPS, call.getArgs()[0]);
	}

	public void testRequestFAWithBareNameUsesEnumVariant() throws Exception {
		Invoker.Call call = resolve("requestFA", "GROUPS");
		assertEquals(FADataType.GROUPS, call.getArgs()[0]);
	}

	public void testReplaceFAWithNumberUsesIntVariant() throws Exception {
		Invoker.Call call = resolve("replaceFA", "1000", "1", "\"<xml/>\"");
		assertEquals(Arrays.asList(int.class, int.class, String.class), params(call));
		assertEquals(1, call.getArgs()[1]);
	}

	public void testReplaceFAWithNameUsesEnumVariant() throws Exception {
		Invoker.Call call = resolve("replaceFA", "1000", "\"GROUPS\"", "\"<xml/>\"");
		assertEquals(Arrays.asList(int.class, FADataType.class, String.class), params(call));
		assertEquals(FADataType.GROUPS, call.getArgs()[1]);
		assertEquals("<xml/>", call.getArgs()[2]);
	}

	public void testIncompleteOverloadStillExpectsMoreInput() throws Exception {
		try {
			resolve("replaceFA", "1000");
			fail("Expected MoreInputExpected");
		} catch (MoreInputExpected e) {
			assertEquals("Expecting 2 more value(s)", e.getMessage());
		}
	}

	public void testTooManyValues() throws Exception {
		try {
			resolve("requestFA", "1", "2");
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals("Expected 1 less value(s)", e.getMessage());
		}
	}

	public void testTwsActionsHideEClientOverloads() throws Exception {
		Invoker.Call call = resolve("eConnect", "0", "false");
		assertTrue(TwsActions.class.isAssignableFrom(call.getMethod().getDeclaringClass()));
		assertEquals(Arrays.asList(int.class, boolean.class), params(call));
	}

	public void testEnumAcceptsNumericCode() throws Exception {
		// FADataType codes are GROUPS(1), ALIASES(3), matching requestFA(int)
		PropertyType type = new PropertyType(FADataType.class);
		Deserializer deserializer = new Deserializer();
		assertEquals(FADataType.GROUPS, deserializer.deserialize("1", type));
		assertEquals(FADataType.GROUPS, deserializer.deserialize("\"1\"", type));
		assertEquals(FADataType.ALIASES, deserializer.deserialize("3", type));
		assertEquals(FADataType.GROUPS, deserializer.deserialize("\"GROUPS\"", type));
		try {
			deserializer.deserialize("2", type);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("with id 2"));
		}
	}

	public void testEnumWithoutCodeAcceptsOrdinal() throws Exception {
		PropertyType type = new PropertyType(com.ib.client.Types.Action.class);
		Deserializer deserializer = new Deserializer();
		assertEquals(com.ib.client.Types.Action.values()[1], deserializer.deserialize("1", type));
		try {
			deserializer.deserialize("99", type);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("ordinal 99"));
		}
	}

	public void testHelpDescribesEnumVariant() throws Exception {
		invoker.invoke(resolve("help", "\"replaceFA\""));
		printer.flush();
		String help = out.toString();
		assertTrue(help, help.contains("help\t\"replaceFA\"\t\"arg1\"\t\"FADataType\""));
		assertTrue(help, help.contains("helpEnd\t\"replaceFA\""));
	}

	public void testHelpDescribesLongestEventVariant() throws Exception {
		invoker.invoke(resolve("help", "\"error\""));
		printer.flush();
		String help = out.toString();
		assertTrue(help, help.contains("help\t\"error\"\t\"arg4\""));
	}
}
