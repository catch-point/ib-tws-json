/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.meerkattrading.tws;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.ib.client.Decimal;
import com.ib.client.ExecutionCondition;
import com.ib.client.MarginCondition;
import com.ib.client.Order;
import com.ib.client.OrderCondition;
import com.ib.client.OrderConditionType;
import com.ib.client.PercentChangeCondition;
import com.ib.client.PriceCondition;
import com.ib.client.TimeCondition;
import com.ib.client.VolumeCondition;

import samples.testbed.contracts.ContractSamples;
import samples.testbed.orders.OrderSamples;

public class TestbedOrderConditions {
	private static int orderId = 100;
	private static Map<String, JsonArrayBuilder> calls = new TreeMap<>();

	public static void main(String[] args) throws InterruptedException, IOException {
		final IClient client = (IClient) Proxy.newProxyInstance(IClient.class.getClassLoader(),  new Class<?>[] { IClient.class}, new InvocationHandler() {
			
			@Override
			public Object invoke(Object that, Method method, Object[] args) throws Throwable {
				StackTraceElement[] stack = new Throwable().getStackTrace();
				String group = stack[2].getMethodName();
				if (!calls.containsKey(group)) {
					calls.put(group, Json.createArrayBuilder());
				}
				Serializer serializer = new Serializer();
				JsonArrayBuilder array = Json.createArrayBuilder();
				array.add(Json.createValue(method.getName()));
				Type[] types = method.getGenericParameterTypes();
				for (int i=0;i<types.length;i++) {
					String json = serializer.serialize(args[i], new PropertyType(types[i]));
					JsonValue value = Json.createReader(new StringReader(json)).readValue();
					array.add(value);
				}
				JsonArray call = array.build();
				calls.get(group).add(call);
				return null;
			}
		});

		orders(client);

		JsonObjectBuilder object = Json.createObjectBuilder();
		for (String group : calls.keySet()) {
			object.add(group, calls.get(group).build());
		}
		JsonWriterFactory factory = Json.createWriterFactory(new HashMap<String,Boolean>(){{put(JsonGenerator.PRETTY_PRINTING, true);}});
		FileWriter writer = new FileWriter("/home/james/Projects/2020/tws-shell/develop/test/com/meerkattrading/tws/orders-calls.json");
		factory.createWriter(writer).write(object.build());
		writer.close();
	}

	private static void orders(IClient client) {
		int nextOrderId = 1000;
		int conId = 285191782;
		String exchange = "NYSE";
		boolean isMore = true;
		double price = 100.0;
		boolean isConjunction = true;
		String secType = "STK";
		String symbol = "GOOG";
		int percent = 50;
		double pctChange = 50.0;
		String time = "20201212 12:00:00";
		int volume = 1;
		Order mkt = OrderSamples.MarketOrder("BUY", Decimal.get(100));
        //Order will become active if conditioning criteria is met
        mkt.conditionsCancelOrder(true);
        mkt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
        mkt.conditions().add(OrderSamples.ExecutionCondition("EUR.USD", "CASH", "IDEALPRO", true));
        mkt.conditions().add(OrderSamples.MarginCondition(30, true, false));
        mkt.conditions().add(OrderSamples.PercentageChangeCondition(15.0, 208813720, "SMART", true, true));
        mkt.conditions().add(OrderSamples.TimeCondition("20160118 23:59:59", true, false));
        mkt.conditions().add(OrderSamples.VolumeCondition(208813720, "SMART", false, 100, true));
        client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), mkt);

        Order lmt = OrderSamples.LimitOrder("BUY", Decimal.get(100), 20);
        //The active order will be cancelled if conditioning criteria is met
        lmt.conditionsCancelOrder(true);
        lmt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
        client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), lmt);
        
        //Conditions have to be created via the OrderCondition.Create
        PriceCondition priceCondition = (PriceCondition)OrderCondition.create(OrderConditionType.Price);
        //When this contract...
        priceCondition.conId(conId);
        //traded on this exchange
        priceCondition.exchange(exchange);
        //has a price above/below
        priceCondition.isMore(isMore);
        //this quantity
        priceCondition.price(price);
        //AND | OR next condition (will be ignored if no more conditions are added)
        priceCondition.conjunctionConnection(isConjunction);
        lmt.conditions(Arrays.asList(priceCondition));
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), lmt);

        ExecutionCondition execCondition = (ExecutionCondition)OrderCondition.create(OrderConditionType.Execution);
        //When an execution on symbol
        execCondition.symbol(symbol);
        //at exchange
        execCondition.exchange(exchange);
        //for this secType
        execCondition.secType(secType);
        //AND | OR next condition (will be ignored if no more conditions are added)
        execCondition.conjunctionConnection(isConjunction);
        lmt.conditions(Arrays.asList(execCondition));
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), lmt);

        MarginCondition marginCondition = (MarginCondition)OrderCondition.create(OrderConditionType.Margin);
        //If margin is above/below
        marginCondition.isMore(isMore);
        //given percent
        marginCondition.percent(percent);
        //AND | OR next condition (will be ignored if no more conditions are added)
        marginCondition.conjunctionConnection(isConjunction);
        lmt.conditions(Arrays.asList(marginCondition));
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), lmt);

        PercentChangeCondition pctChangeCondition = (PercentChangeCondition)OrderCondition.create(OrderConditionType.PercentChange);
        //If there is a price percent change measured against last close price above or below...
        pctChangeCondition.isMore(isMore);
        //this amount...
        pctChangeCondition.changePercent(pctChange);
        //on this contract
        pctChangeCondition.conId(conId);
        //when traded on this exchange...
        pctChangeCondition.exchange(exchange);
        //AND | OR next condition (will be ignored if no more conditions are added)
        pctChangeCondition.conjunctionConnection(isConjunction);
        lmt.conditions(Arrays.asList(pctChangeCondition));
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), lmt);

        TimeCondition timeCondition = (TimeCondition)OrderCondition.create(OrderConditionType.Time);
        //Before or after...
        timeCondition.isMore(isMore);
        //this time...
        timeCondition.time(time);
        //AND | OR next condition (will be ignored if no more conditions are added)
        timeCondition.conjunctionConnection(isConjunction);
        lmt.conditions(Arrays.asList(timeCondition));
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), lmt);

        VolumeCondition volCon = (VolumeCondition)OrderCondition.create(OrderConditionType.Volume);
        //Whenever contract...
        volCon.conId(conId);
        //When traded at
        volCon.exchange(exchange);
        //reaches a volume higher/lower
        volCon.isMore(isMore);
        //than this...
        volCon.volume(volume);
        //AND | OR next condition (will be ignored if no more conditions are added)
        volCon.conjunctionConnection(isConjunction);
        lmt.conditions(Arrays.asList(volCon));
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), lmt);
	}
}
