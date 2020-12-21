package com.meerkattrading.tws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonWriterFactory;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestClientCalls extends TestCase {

	private static final String CLIENT_CALLS_JSON = "client-calls.json";

	public static TestSuite suite() {
		InputStream resource = TestClientCalls.class.getResourceAsStream(CLIENT_CALLS_JSON);
		JsonObject obj = Json.createReader(resource).readObject();
		TestSuite top = new TestSuite();
		for (String key : obj.keySet()) {
			JsonArray calls = obj.getJsonArray(key);
			TestSuite suite = new TestSuite(key);
			for (int i = 0, n = calls.size(); i < n; i++) {
				JsonArray call = calls.getJsonArray(i).asJsonArray();
				suite.addTest(new TestClientCalls(call.getString(0) + " " + key + " " + i));
			}
			top.addTest(suite);
		}
		return top;
	}

	private final JsonArray call;

	public TestClientCalls(String name) {
		super(name);
		String[] split = name.split(" ");
		InputStream resource = TestClientCalls.class.getResourceAsStream(CLIENT_CALLS_JSON);
		JsonObject obj = Json.createReader(resource).readObject();
		JsonArray calls = obj.getJsonArray(split[1]);
		int i = Integer.parseInt(split[2]);
		this.call = calls.getJsonArray(i).asJsonArray();
	}

	@Override
	protected void runTest() throws Throwable {
		String cmd = call.getString(0);
		String[] args = new String[call.size() - 1];
		JsonWriterFactory factory = Json.createWriterFactory(Collections.emptyMap());
		for (int i = 1, n = call.size(); i < n; i++) {
			StringWriter writer = new StringWriter();
			factory.createWriter(writer).write(call.get(i));
			args[i - 1] = writer.toString();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(cmd);
		for (String arg : args) {
			sb.append(' ').append(arg);
		}
		sb.append('\n');
		byte[] in_bytes = sb.toString().getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(in_bytes);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
		Shell shell = new Shell(in, out) {

			@Override
			protected Invoker getInvoker() throws IOException {
				return new Invoker(this.getPrinter()) {

					@Override
					protected IClient getClient() {
						ClassLoader cl = IClient.class.getClassLoader();
						return (IClient) Proxy.newProxyInstance(cl, new Class<?>[] { IClient.class },
								new InvocationHandler() {

									@Override
									public Object invoke(Object that, Method method, Object[] args) throws Throwable {
										Serializer serializer = new Serializer();
										writer.write(method.getName());
										Type[] types = method.getGenericParameterTypes();
										for (int i = 0; i < types.length; i++) {
											String json = serializer.serialize(args[i], new PropertyType(types[i]));
											writer.write(' ');
											writer.write(json);
										}
										writer.write('\n');
										return null;
									}
								});
					}
				};
			}

		};
		shell.repl();
		writer.close();
		byte[] out_bytes = out.toByteArray();
		assertEquals(new String(in_bytes), new String(out_bytes));
	}

}
