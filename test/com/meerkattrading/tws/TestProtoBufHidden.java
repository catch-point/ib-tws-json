package com.meerkattrading.tws;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;

import com.ib.client.Contract;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.protobuf.ContractProto;
import com.ib.client.protobuf.ExecutionDetailsProto;
import com.ib.client.protobuf.ExecutionProto;

import junit.framework.TestCase;

/**
 * The TWS API's *ProtoBuf methods are hidden from the JSON API: not commands,
 * not events, and not listed by help.
 */
public class TestProtoBufHidden extends TestCase {
	private ByteArrayOutputStream out;
	private Printer printer;
	private Invoker invoker;

	@Override
	protected void setUp() throws Exception {
		out = new ByteArrayOutputStream();
		printer = new Printer(out);
		invoker = new Invoker(printer);
	}

	private String help(String name) throws Exception {
		invoker.invoke(invoker.resolve("help", Arrays.asList("\"" + name + "\"")));
		printer.flush();
		String help = out.toString();
		out.reset();
		return help;
	}

	public void testProtoBufCommandsAreNotCommands() throws Exception {
		for (String command : new String[] { "reqMarketDataProtoBuf", "cancelContractDataProtoBuf",
				"placeOrderProtoBuf", "reqConfigProtoBuf", "updateConfigProtoBuf" }) {
			try {
				invoker.resolve(command, Collections.emptyList());
				fail("Expected " + command + " to be hidden");
			} catch (NoSuchMethodException e) {
				assertEquals(command, e.getMessage());
			}
		}
	}

	public void testRegularCommandsRemain() throws Exception {
		assertEquals("reqMktData", invoker.resolve("reqMktData", Arrays.asList("1", "{}", "\"\"", "false", "false")).getMethod().getName());
		assertEquals("cancelContractData",
				invoker.resolve("cancelContractData", Arrays.asList("1")).getMethod().getName());
	}

	public void testHelpListsNoProtoBufCommands() throws Exception {
		String help = help("EClient");
		assertTrue(help, help.contains("\"reqMktData\""));
		assertFalse(help, help.contains("ProtoBuf"));
	}

	public void testHelpListsNoProtoBufEvents() throws Exception {
		String help = help("EWrapper");
		assertTrue(help, help.contains("\"execDetails\""));
		assertFalse(help, help.contains("ProtoBuf"));
	}

	public void testHelpDoesNotDescribeProtoBufEvent() throws Exception {
		String help = help("execDetailsProtoBuf");
		assertEquals("error\t\"execDetailsProtoBuf?\"\n", help);
	}

	public void testProtoBufEventIsNotPrinted() throws Exception {
		EWrapper wrapper = EWrapperHandler.newInstance(printer);
		wrapper.execDetailsProtoBuf(ExecutionDetailsProto.ExecutionDetails.newBuilder().setReqId(7)
				.setContract(ContractProto.Contract.newBuilder().setSymbol("IBM").build())
				.setExecution(ExecutionProto.Execution.newBuilder().setExecId("0001").build()).build());
		printer.flush();
		assertEquals("", out.toString());
	}

	public void testRegularEventIsPrinted() throws Exception {
		EWrapper wrapper = EWrapperHandler.newInstance(printer);
		Contract contract = new Contract();
		contract.symbol("IBM");
		Execution execution = new Execution();
		execution.execId("0001");
		wrapper.execDetails(7, contract, execution);
		printer.flush();
		assertTrue(out.toString(), out.toString().startsWith("execDetails\t7\t{"));
	}
}
