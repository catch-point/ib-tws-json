/*
 * Java TWS API Samples
 *
 * Copyright (C) 2013-2026  Interactive Brokers LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Adapted for ib-tws-json: the operation methods below are copied from IB's
 * samples/testbed/Testbed.java (TWS API 10.50) with EClientSocket replaced by
 * IClient and Thread.sleep replaced by a no-op. Running main() records every
 * call (round-tripped through Serializer/Deserializer) into client-calls.json,
 * which TestClientCalls replays. Regenerate with: ./gradlew generateClientCalls
 *
 * Intentional difference from IB's sample: financialAdvisorOperations passes
 * FADataType.id() where IB's sample passes FADataType.ordinal(). The ordinal
 * matched the TWS API code only while the enum was UNUSED, GROUPS, PROFILES,
 * ALIASES; it is now GROUPS(1), ALIASES(3), so ordinal() sends 0 or the wrong
 * type. Keep this change when copying a newer IB Testbed.
 *
 * Intentional difference from IB's sample: historicalDataRequests uses the fixed
 * FIXED_END_DATE_TIME where IB's sample uses six months before the current
 * time, so regenerating client-calls.json gives the same output every time.
 */

package com.meerkattrading.tws;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.ib.client.*;
import com.ib.client.Types.FADataType;

import junit.framework.TestCase;
import samples.testbed.advisor.FAMethodSamples;
import samples.testbed.contracts.ContractSamples;
import samples.testbed.orders.AvailableAlgoParams;
import samples.testbed.orders.OrderSamples;
import samples.testbed.scanner.ScannerSubscriptionSamples;

public class Testbed {
	private static int orderId = 100;
	/** End date for historicalDataRequests, in IB's yyyyMMdd-HH:mm:ss format (UTC) */
	private static final String FIXED_END_DATE_TIME = "20260503-11:00:02";
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
				Deserializer deserializer = new Deserializer();
				JsonArrayBuilder array = Json.createArrayBuilder();
				array.add(Json.createValue(method.getName()));
				Type[] types = method.getGenericParameterTypes();
				for (int i=0;i<types.length;i++) {
					PropertyType ptype = new PropertyType(types[i]);
					String json = serializer.serialize(args[i], ptype);
					Object trip = deserializer.deserialize(json, ptype);
					if (Order.class.equals(types[i])) {
						TestCase.assertEquals(((Order)args[i]).getAction(), ((Order)trip).getAction());
						TestCase.assertEquals(((Order)args[i]).getAlgoStrategy(), ((Order)trip).getAlgoStrategy());
						TestCase.assertEquals(((Order)args[i]).getDeltaNeutralOrderType(), ((Order)trip).getDeltaNeutralOrderType());
						TestCase.assertEquals(((Order)args[i]).getFaMethod(), ((Order)trip).getFaMethod());
						TestCase.assertEquals(((Order)args[i]).getHedgeType(), ((Order)trip).getHedgeType());
						TestCase.assertEquals(((Order)args[i]).getOrderType(), ((Order)trip).getOrderType());
						TestCase.assertEquals(((Order)args[i]).getReferencePriceType(), ((Order)trip).getReferencePriceType());
						TestCase.assertEquals(((Order)args[i]).getRule80A(), ((Order)trip).getRule80A());
						TestCase.assertEquals(((Order)args[i]).getTif(), ((Order)trip).getTif());
						TestCase.assertEquals(((Order)args[i]).getVolatilityType(), ((Order)trip).getVolatilityType());
					}
					if (Contract.class.equals(types[i])) {
						TestCase.assertEquals(((Contract)args[i]).getRight(), ((Contract)trip).getRight());
						TestCase.assertEquals(((Contract)args[i]).getSecIdType(), ((Contract)trip).getSecIdType());
						TestCase.assertEquals(((Contract)args[i]).getSecType(), ((Contract)trip).getSecType());
						List<ComboLeg> legs = ((Contract)args[i]).comboLegs();
						for (int j=0,n=legs.size(); j<n; j++) {
							ComboLeg left = ((Contract)args[i]).comboLegs().get(j);
							ComboLeg right = ((Contract)trip).comboLegs().get(j);
							TestCase.assertEquals(left.getAction(), right.getAction());
							TestCase.assertEquals(left.getOpenClose(), right.getOpenClose());
						}
					}
					JsonValue value = Json.createReader(new StringReader(json)).readValue();
					array.add(value);
				}
				JsonArray call = array.build();
				calls.get(group).add(call);
				return null;
			}
		});

		// Mirrors the operation list in IB's samples/testbed/Testbed.java (TWS API 10.50),
		// excluding the *ProtoBuf operations, which the JSON API cannot construct
		tickByTickOperations(client);
		tickDataOperations(client);
		tickOptionComputations(client);
		optionsOperations(client);
		orderOperations(client, getCurrentOrderId());
		contractOperations(client);
		hedgeSample(client, getCurrentOrderId());
		testAlgoSamples(client, getCurrentOrderId());
		bracketSample(client, getCurrentOrderId());
		bulletins(client);
		marketScanners(client);
		marketDataType(client);
		historicalDataRequests(client);
		accountOperations(client);
		newsOperations(client);
		marketDepthOperations(client);
		rerouteCFDOperations(client);
		marketRuleOperations(client);
		pnl(client);
		continuousFuturesOperations(client);
		pnlSingle(client);
		histogram(client);
		whatIfSamples(client, getCurrentOrderId());
		historicalTicks(client);
		financialAdvisorOperations(client);
		realTimeBars(client);
		wshCalendarOperations(client);
		miscellaneous(client);
		linkingOperations(client);
		ibkratsSample(client, getCurrentOrderId());

		JsonObjectBuilder object = Json.createObjectBuilder();
		for (String group : calls.keySet()) {
			object.add(group, calls.get(group).build());
		}
		JsonWriterFactory factory = Json.createWriterFactory(new HashMap<String,Boolean>(){{put(JsonGenerator.PRETTY_PRINTING, true);}});
		FileWriter writer = new FileWriter(args.length > 0 ? args[0] : "test/com/meerkattrading/tws/client-calls.json");
		factory.createWriter(writer).write(object.build());
		writer.close();
	}
	
	private static int getCurrentOrderId() {
		return ++orderId;
	}


	private static void histogram(IClient client) {
	    client.reqHistogramData(4002, ContractSamples.USStock(), false, "3 days");
        
        try {
            sleep(60000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        client.cancelHistogramData(4002);
	}
	
    private static void historicalTicks(IClient client) {
        //! [reqhistoricalticks]
        client.reqHistoricalTicks(18001, ContractSamples.USStockAtSmart(), "20220808 10:00:00 US/Eastern", null, 10, "TRADES", 1, true, null);
        client.reqHistoricalTicks(18002, ContractSamples.USStockAtSmart(), "20220808 10:00:00 US/Eastern", null, 10, "BID_ASK", 1, true, null);
        client.reqHistoricalTicks(18003, ContractSamples.USStockAtSmart(), "20220808 10:00:00 US/Eastern", null, 10, "MIDPOINT", 1, true, null);
        //! [reqhistoricalticks]

        //! [cancelhistoricalticks]
        client.cancelHistoricalTicks(18001);
        //! [cancelhistoricalticks]
    }

	private static void pnl(IClient client) throws InterruptedException {
		//! [reqpnl]
        client.reqPnL(17001, "DUD00029", "");
		//! [reqpnl]
        sleep(1000);
		//! [cancelpnl]
        client.cancelPnL(17001);        
		//! [cancelpnl]
    }
	
    private static void pnlSingle(IClient client) throws InterruptedException
    {
		//! [reqpnlsingle]
        client.reqPnLSingle(17001, "DUD00029", "", 268084);
		//! [reqpnlsingle]
        sleep(1000);
		//! [cancelpnlsingle]
        client.cancelPnLSingle(17001);
		//! [cancelpnlsingle]
    }

    private static void orderOperations(IClient client, int nextOrderId) throws InterruptedException {
		
		/*** Requesting the next valid id ***/
		//! [reqids]
        //The parameter is always ignored.
        client.reqIds(-1);
        //! [reqids]
        //sleep(1000);
        /*** Requesting all open orders ***/
        //! [reqallopenorders]
        client.reqAllOpenOrders();
        //! [reqallopenorders]
        //sleep(1000);
        /*** Taking over orders to be submitted via TWS ***/
        //! [reqautoopenorders]
        client.reqAutoOpenOrders(true);
        //! [reqautoopenorders]
        //sleep(1000);
        /*** Requesting this API client's orders ***/
        //! [reqopenorders]
        client.reqOpenOrders();
        //! [reqopenorders]
        //sleep(1000);
		
        /*** Placing/modifying an order - remember to ALWAYS increment the nextValidId after placing an order so it can be used for the next one! ***/
        //! [order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.LimitOrder("SELL", Decimal.ONE, 50));
        //! [order_submission]
        
		//! [place_midprice]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.Midprice("BUY", Decimal.ONE, 150));
        //! [place_midprice]
		
        //! [faorderoneaccount]
        Order faOrderOneAccount = OrderSamples.MarketOrder("BUY", Decimal.ONE_HUNDRED);
        // Specify the Account Number directly
        faOrderOneAccount.account("DU119915");
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), faOrderOneAccount);
        //! [faorderoneaccount]
        
        //! [faordergroup]
        Order faOrderGroup = OrderSamples.LimitOrder("BUY", Decimal.get(200), 10);
        faOrderGroup.faGroup("MyTestGroup1");
        faOrderGroup.faMethod("AvailableEquity");
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), faOrderGroup);
        //! [faordergroup]
        
        //! [faorderuserdefinedgroup]
        Order faOrderUserDefinedGroup = OrderSamples.LimitOrder("BUY", Decimal.get(200), 10);
        faOrderUserDefinedGroup.faGroup("MyTestProfile1");
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), faOrderUserDefinedGroup);
        //! [faorderuserdefinedgroup]
        
		//! [modelorder]
        Order modelOrder = OrderSamples.LimitOrder("BUY", Decimal.get(200), 100);
		modelOrder.account("DF12345");  // master FA account number
		modelOrder.modelCode("Technology"); // model for tech stocks first created in TWS
		client.placeOrder(nextOrderId++, ContractSamples.USStock(), modelOrder);
        //! [modelorder]
		
		//client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.PeggedToMarket("BUY", 10, 0.01));
		//client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), OrderSamples.MarketOrder("BUY", 10));
        //client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.Discretionary("SELL", 1, 45, 0.5));
		
        //! [reqexecutions]
        ExecutionFilter executionFilter1 = new ExecutionFilter();
        executionFilter1.lastNDays(7);
        client.reqExecutions(10001, executionFilter1);
        ExecutionFilter executionFilter2 = new ExecutionFilter();
        executionFilter2.specificDates(Arrays.asList(20250221, 20250220));
        client.reqExecutions(10002, executionFilter2);
        //! [reqexecutions]


		int cancelID = nextOrderId -1;
		//! [cancelorder]
		client.cancelOrder(cancelID, new OrderCancel("20240614-00:00:04"));
		//! [cancelorder]

		//! [reqglobalcancel]
		client.reqGlobalCancel(OrderSamples.OrderCancelEmpty());
		//! [reqglobalcancel]
		
        /*** Completed orders ***/
        //! [reqcompletedorders]
        client.reqCompletedOrders(false);
        //! [reqcompletedorders]

        //! [crypto_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.CryptoContract(), OrderSamples.LimitOrder("BUY", Decimal.parse("0.00001234"), 3370));
        //! [crypto_order_submission]

        //! [manual_order_time]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.LimitOrderWithManualOrderTime("BUY", Decimal.get(100), 111.11, "20220314-13:00:00"));
        //! [manual_order_time]
        
        //! [include_overnight]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.LimitOrderWithIncludeOvernight("BUY", Decimal.get(100), 110.11));
        //! [include_overnight]
        
        //! [manual_order_cancel_time]
        cancelID = nextOrderId - 1;
        client.cancelOrder(cancelID,  OrderSamples.OrderCancelWithManualTime("20220314-19:00:00"));
        //! [manual_order_cancel_time]

        //! [pegbest_up_to_mid_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), OrderSamples.PegBestUpToMidOrder("BUY", Decimal.get(100), 111.11, 100, 200, 0.02, 0.025));
        //! [pegbest_up_to_mid_order_submission]

        //! [pegbest_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), OrderSamples.PegBestOrder("BUY", Decimal.get(100), 111.11, 100, 200, 0.03));
        //! [pegbest_order_submission]

        //! [pegmid_order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), OrderSamples.PegMidOrder("BUY", Decimal.get(100), 111.11, 100, 0.02, 0.025));
        //! [pegmid_order_submission]

        //! [customer_account]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.LimitOrderWithCustomerAccount("BUY", Decimal.get(100), 111.11, "CustAcct"));
        //! [customer_account]

        //! [cme_tagging_fields]
        client.placeOrder(nextOrderId++, ContractSamples.SimpleFuture(), OrderSamples.LimitOrderWithCmeTaggingFields("BUY", Decimal.get(1), 5333, "ABCD", 1));
        sleep(5000);
        cancelID = nextOrderId - 1;
        client.cancelOrder(cancelID, OrderSamples.OrderCancelWithCmeTaggingFields("BCDE", 0));
        sleep(2000);
        client.placeOrder(nextOrderId++, ContractSamples.SimpleFuture(), OrderSamples.LimitOrderWithCmeTaggingFields("BUY", Decimal.get(1), 5444, "CDEF", 0));
        sleep(5000);
        client.reqGlobalCancel(OrderSamples.OrderCancelWithCmeTaggingFields("DEFG", 1));
        //! [cme_tagging_fields]

        //! [imbalance_only]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.LimitOnCloseOrderWithImbalance("BUY", Decimal.get(100), 44.44));
        //! [imbalance_only]

        //! [zero_strike_opt_order]
        client.placeOrder(nextOrderId++, ContractSamples.OptForecastxZeroStrike(), OrderSamples.LimitOrder("BUY", Decimal.get(1), 0.05));
        //! [zero_strike_opt_order]

        //! [limit_order_with_stop_loss_and_profit_taker]
        client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), OrderSamples.LimitOrderWithStopLossAndProfitTaker("BUY", Decimal.get(100), 40, nextOrderId++, nextOrderId++));
        //! [limit_order_with_stop_loss_and_profit_taker]

        sleep(10000);
        
    }
	
	private static void OcaSample(IClient client, int nextOrderId) {
		
		//OCA order
		//! [ocasubmit]
		List<Order> OcaOrders = new ArrayList<>();
		OcaOrders.add(OrderSamples.LimitOrder("BUY", Decimal.ONE, 10));
		OcaOrders.add(OrderSamples.LimitOrder("BUY", Decimal.ONE, 11));
		OcaOrders.add(OrderSamples.LimitOrder("BUY", Decimal.ONE, 12));
		OcaOrders = OrderSamples.OneCancelsAll("TestOCA_" + nextOrderId, OcaOrders, 2);
		for (Order o : OcaOrders) {
			
			client.placeOrder(nextOrderId++, ContractSamples.USStock(), o);
		}
		//! [ocasubmit]
		
	}
	
	private static void tickDataOperations(IClient client) throws InterruptedException {
		//! [reqmktdatatype]
		client.reqMarketDataType(4);
		//! [reqmktdatatype]
		
		/*** Requesting real time market data ***/
		//sleep(1000);
		//! [reqmktdata]
		client.reqMktData(1001, ContractSamples.StockComboContract(), "", false, false, null);
		//! [reqmktdata]

		//! [reqsmartcomponents]
		client.reqSmartComponents(1013, "a6");
		//! [reqsmartcomponents]

		//! [reqmktdata_snapshot]
		client.reqMktData(1003, ContractSamples.FutureComboContract(), "", true, false, null);
		//! [reqmktdata_snapshot]

		/* 
		//! [regulatorysnapshot] 
		// Each regulatory snapshot request incurs a 0.01 USD fee
		client.reqMktData(1014, ContractSamples.USStock(), "", false, true, null);
		//! [regulatorysnapshot]
		*/
		
		//! [reqmktdata_genticks]
		//Requesting RTVolume (Time & Sales) and shortable generic ticks
		client.reqMktData(1004, ContractSamples.USStockAtSmart(), "233,236", false, false, null);
		//! [reqmktdata_genticks]
		//! [reqmktdata_contractnews]
		// Without the API news subscription this will generate an "invalid tick type" error
		client.reqMktData(1005, ContractSamples.USStock(), "mdoff,292:BZ", false, false, null);
		client.reqMktData(1006, ContractSamples.USStock(), "mdoff,292:BT", false, false, null);
		client.reqMktData(1007, ContractSamples.USStock(), "mdoff,292:FLY", false, false, null);
		client.reqMktData(1008, ContractSamples.USStock(), "mdoff,292:DJ-RT", false, false, null);
		//! [reqmktdata_contractnews]
		//! [reqmktdata_broadtapenews]
		client.reqMktData(1009, ContractSamples.BTbroadtapeNewsFeed(), "mdoff,292", false, false, null);
		client.reqMktData(1010, ContractSamples.BZbroadtapeNewsFeed(), "mdoff,292", false, false, null);
		client.reqMktData(1011, ContractSamples.FLYbroadtapeNewsFeed(), "mdoff,292", false, false, null);
		//! [reqmktdata_broadtapenews]
		//! [reqoptiondatagenticks]
        //Requesting data for an option contract will return the greek values
        client.reqMktData(1002, ContractSamples.OptionWithLocalSymbol(), "", false, false, null);
        //! [reqoptiondatagenticks]
		//! [reqfuturesopeninterest]
        //Requesting data for a futures contract will return the futures open interest
        client.reqMktData(1014, ContractSamples.SimpleFuture(), "mdoff,588", false, false, null);
		//! [reqfuturesopeninterest]

		//! [reqmktdata_preopenbidask]
        //Requesting data for a futures contract will return the pre-open bid/ask flag
        client.reqMktData(1015, ContractSamples.SimpleFuture(), "", false, false, null);
        //! [reqmktData_preopenbidask]

        //! [reqavgoptvolume]
        //Requesting data for a stock will return the average option volume
        client.reqMktData(1016, ContractSamples.USStockAtSmart(), "mdoff,105", false, false, null);
        //! [reqavgoptvolume]
        
        //! [reqetfticks]
        client.reqMktData(1017, ContractSamples.etf(), "mdoff,577,614,623", false, false, null);
        //! [reqetfticks]
        
        //! [IPOPrice]
        client.reqMktData(1018, ContractSamples.StockWithIPOPrice(), "mdoff,586", false, false, null);
        //! [IPOPrice]

        //! [yieldbidask]
        client.reqMktData(1019, ContractSamples.Bond(), "", false, false, null);
        //! [yieldbidask]
        
		sleep(10000);
		//! [cancelmktdata]
		client.cancelMktData(1001);
		client.cancelMktData(1002);
		client.cancelMktData(1003);
		client.cancelMktData(1014);
		client.cancelMktData(1015);
		client.cancelMktData(1016);
		client.cancelMktData(1017);
        client.cancelMktData(1018);
        client.cancelMktData(1019);
		//! [cancelmktdata]
		
	}
	
	private static void tickOptionComputations(IClient client) throws InterruptedException {
		
		/*** Requesting real time market data ***/
		client.reqMarketDataType(4);
		
		//! [reqmktdata]
		client.reqMktData(2001, ContractSamples.OptionWithLocalSymbol(), "", false, false, null);
		//! [reqmktdata]
		
		sleep(10000);
		
		//! [cancelmktdata]
		client.cancelMktData(2001);
		//! [cancelmktdata]
	}
	
	private static void historicalDataRequests(IClient client) throws InterruptedException {
		
		/*** Requesting historical data ***/

		//! [reqHeadTimeStamp]
		client.reqHeadTimestamp(4003, ContractSamples.USStock(), "TRADES", 1, 1);
		//! [reqHeadTimeStamp]

		//! [cancelHeadTimestamp]
		client.cancelHeadTimestamp(4003);
		//! [cancelHeadTimestamp]
		
		//! [reqhistoricaldata]
		// IB's sample uses six months before now; a fixed date keeps client-calls.json stable
		String formatted = FIXED_END_DATE_TIME;
		client.reqHistoricalData(4001, ContractSamples.EurGbpFx(), formatted, "1 M", "1 day", "MIDPOINT", 1, 1, false, null);
		client.reqHistoricalData(4002, ContractSamples.EuropeanStock(), formatted, "10 D", "1 min", "TRADES", 1, 1, false, null);
        	client.reqHistoricalData(4003, ContractSamples.USStockAtSmart(), formatted, "1 M", "1 day", "SCHEDULE", 1, 1, false, null);
		sleep(2000);
		/*** Canceling historical data requests ***/
		client.cancelHistoricalData(4001);
        client.cancelHistoricalData(4002);
        client.cancelHistoricalData(4003);
		//! [reqhistoricaldata]

        //! [reqHistogramData]
		/*client.reqHistogramData(4004, ContractSamples.USStock(), false, "3 days");
        //! [reqHistogramData]
		sleep(5);
		
		//! [cancelHistogramData]
        client.cancelHistogramData(4004);*/
		//! [cancelHistogramData]
	}
	
	private static void realTimeBars(IClient client) throws InterruptedException {
		
		/*** Requesting real time bars ***/
        //! [reqrealtimebars]
        client.reqRealTimeBars(3001, ContractSamples.EurGbpFx(), 5, "MIDPOINT", true, null);
        //! [reqrealtimebars]
        sleep(2000);
        /*** Canceling real time bars ***/
        //! [cancelrealtimebars]
        client.cancelRealTimeBars(3001);
        //! [cancelrealtimebars]
		
	}
	
	private static void marketDepthOperations(IClient client) throws InterruptedException {
		
		/*** Requesting the Deep Book ***/

		//! [reqMktDepthExchanges]
		client.reqMktDepthExchanges();
		//! [reqMktDepthExchanges]

        //! [reqmarketdepth]
        client.reqMktDepth(2001, ContractSamples.EurGbpFx(), 5, false, null);
        //! [reqmarketdepth]
        sleep(2000);
        /*** Canceling the Deep Book request ***/
        //! [cancelmktdepth]
        client.cancelMktDepth(2001, false);
        //! [cancelmktdepth]

        //! [reqmarketdepth]
        client.reqMktDepth(2002, ContractSamples.EuropeanStock(), 5, true, null);
        //! [reqmarketdepth]
        sleep(5000);
        //! [cancelmktdepth]
        client.cancelMktDepth(2002, true);
        //! [cancelmktdepth]
	}
	
	private static void accountOperations(IClient client) throws InterruptedException {
		
        //client.reqAccountUpdatesMulti(9002, null, "EUstocks", true);
		//! [reqpositionsmulti]
        client.reqPositionsMulti(9003, "DU74649", "EUstocks");
      //! [reqpositionsmulti]
        sleep(10000);

        /*** Requesting managed accounts***/
        //! [reqmanagedaccts]
        client.reqManagedAccts();
        //! [reqmanagedaccts]

		/*** Requesting family codes***/
		//! [reqfamilycodes]
		client.reqFamilyCodes();
		//! [reqfamilycodes]

        /*** Requesting accounts' summary ***/
        sleep(2000);
        //! [reqaaccountsummary]
        client.reqAccountSummary(9001, "All", "AccountType,NetLiquidation,TotalCashValue,SettledCash,AccruedCash,BuyingPower,EquityWithLoanValue,PreviousEquityWithLoanValue,GrossPositionValue,ReqTEquity,ReqTMargin,SMA,InitMarginReq,MaintMarginReq,AvailableFunds,ExcessLiquidity,Cushion,FullInitMarginReq,FullMaintMarginReq,FullAvailableFunds,FullExcessLiquidity,LookAheadNextChange,LookAheadInitMarginReq ,LookAheadMaintMarginReq,LookAheadAvailableFunds,LookAheadExcessLiquidity,HighestSeverity,DayTradesRemaining,Leverage");
        //! [reqaaccountsummary]
        
        //! [reqaaccountsummaryledger]
        client.reqAccountSummary(9002, "All", "$LEDGER");
        //! [reqaaccountsummaryledger]
        sleep(2000);
        //! [reqaaccountsummaryledgercurrency]
        client.reqAccountSummary(9003, "All", "$LEDGER:EUR");
        //! [reqaaccountsummaryledgercurrency]
        sleep(2000);
        //! [reqaaccountsummaryledgerall]
        client.reqAccountSummary(9004, "All", "$LEDGER:ALL");
        //! [reqaaccountsummaryledgerall]
		
		//! [cancelaaccountsummary]
		client.cancelAccountSummary(9001);
		client.cancelAccountSummary(9002);
		client.cancelAccountSummary(9003);
		client.cancelAccountSummary(9004);
		//! [cancelaaccountsummary]
        
        /*** Subscribing to an account's information. Only one at a time! ***/
        sleep(2000);
        //! [reqaaccountupdates]
        client.reqAccountUpdates(true, "U150462");
        //! [reqaaccountupdates]
		sleep(2000);
		//! [cancelaaccountupdates]
		client.reqAccountUpdates(false, "U150462");
		//! [cancelaaccountupdates]
		
        //! [reqaaccountupdatesmulti]
        client.reqAccountUpdatesMulti(9002, "U150462", "EUstocks", true);
        //! [reqaaccountupdatesmulti]
        sleep(2000);
        /*** Requesting all accounts' positions. ***/
        //! [reqpositions]
        client.reqPositions();
        //! [reqpositions]
		sleep(2000);
		//! [cancelpositions]
		client.cancelPositions();
		//! [cancelpositions]
		
        /*** Requesting user info. ***/
        //! [requserinfo]
        client.reqUserInfo(0);
        //! [requserinfo]
    }

	private static void newsOperations(IClient client) throws InterruptedException {

		/*** Requesting news ticks ***/
		//! [reqNewsTicks]
		client.reqMktData(10001, ContractSamples.USStockAtSmart(), "mdoff,292", false, false, null);
		//! [reqNewsTicks]

		sleep(10000);

		/*** Canceling news ticks ***/
		//! [cancelNewsTicks]
		client.cancelMktData(10001);
		//! [cancelNewsTicks]

		sleep(2000);

		/*** Requesting news providers ***/
		//! [reqNewsProviders]
		client.reqNewsProviders();
		//! [reqNewsProviders]

		sleep(2000);
		
		/*** Requesting news article ***/
		//! [reqNewsArticle]
		client.reqNewsArticle(10002, "BZ", "BZ$04507322", null);
		//! [reqNewsArticle]

		sleep(5000);

		/*** Requesting historical news ***/
		//! [reqHistoricalNews]
		client.reqHistoricalNews(10003, 8314, "BZ+FLY", "", "", 10, null);
		//! [reqHistoricalNews]
	}

	private static void conditionSamples(IClient client, int nextOrderId) {
		
		//! [order_conditioning_activate]
		Order mkt = OrderSamples.MarketOrder("BUY", Decimal.ONE_HUNDRED);
		//Order will become active if conditioning criteria is met
		mkt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
		mkt.conditions().add(OrderSamples.ExecutionCondition("EUR.USD", "CASH", "IDEALPRO", true));
		mkt.conditions().add(OrderSamples.MarginCondition(30, true, false));
		mkt.conditions().add(OrderSamples.PercentageChangeCondition(15.0, 208813720, "SMART", true, true));
		mkt.conditions().add(OrderSamples.TimeCondition("20220909 10:00:00 US/Eastern", true, false));
		mkt.conditions().add(OrderSamples.VolumeCondition(208813720, "SMART", false, 100, true));
		client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), mkt);
		//! [order_conditioning_activate]
		
		//Conditions can make the order active or cancel it. Only LMT orders can be conditionally canceled.
		//! [order_conditioning_cancel]
		Order lmt = OrderSamples.LimitOrder("BUY", Decimal.ONE_HUNDRED, 20);
		//The active order will be cancelled if conditioning criteria is met
		lmt.conditionsCancelOrder(true);
		lmt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
		client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), lmt);
		//! [order_conditioning_cancel]
		
	}
	
	private static void contractOperations(IClient client) {
		
		//! [reqcontractdetails]
		client.reqContractDetails(210, ContractSamples.OptionForQuery());
		client.reqContractDetails(211, ContractSamples.EurGbpFx());
		client.reqContractDetails(212, ContractSamples.Bond());
		client.reqContractDetails(213, ContractSamples.FuturesOnOptions());
		client.reqContractDetails(214, ContractSamples.SimpleFuture());
		client.reqContractDetails(215, ContractSamples.USStockAtSmart());
		client.reqContractDetails(216, ContractSamples.CryptoContract());
		client.reqContractDetails(217, ContractSamples.ByIssuerId());
		client.reqContractDetails(218, ContractSamples.Fund());
		client.reqContractDetails(219, ContractSamples.USStock());
		client.reqContractDetails(220, ContractSamples.OptForecastx());
		client.reqContractDetails(221, ContractSamples.OptForecastxZeroStrike());
		client.reqContractDetails(222, ContractSamples.OptForecastxByConId());
		//! [reqcontractdetails]

		//! [reqmatchingsymbols]
		client.reqMatchingSymbols(211, "IB");
		//! [reqmatchingsymbols]

		//! [cancelcontractdata]
		client.cancelContractData(210);
		//! [cancelcontractdata]
	}
	
	private static void contractNewsFeed(IClient client) {
		
		//! [reqcontractdetailsnews]
		client.reqContractDetails(211, ContractSamples.NewsFeedForQuery());
		//! [reqcontractdetailsnews]
		
	}
	
	private static void hedgeSample(IClient client, int nextOrderId) {
		
		//F Hedge order
		//! [hedgesubmit]
		//Parent order on a contract which currency differs from your base currency
		Order parent = OrderSamples.LimitOrder("BUY", Decimal.ONE_HUNDRED, 10);
		parent.orderId(nextOrderId++);
		parent.transmit(false);
		//Hedge on the currency conversion
		Order hedge = OrderSamples.MarketFxHedge(parent.orderId(), "BUY");
		//Place the parent first...
		client.placeOrder(parent.orderId(), ContractSamples.EuropeanStock(), parent);
		//Then the hedge order
		client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), hedge);
		//! [hedgesubmit]
		
	}
	
	private static void testAlgoSamples(IClient client, int nextOrderId) throws InterruptedException {
		
		//! [scale_order]
		Order scaleOrder = OrderSamples.RelativePeggedToPrimary("BUY",  Decimal.get(70000),  189,  0.01);
		AvailableAlgoParams.FillScaleParams(scaleOrder, 2000, 500, true, .02, 189.00, 3600, 2.00, true, 10, 40);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), scaleOrder);
		//! [scale_order]

		sleep(500);

		//! [algo_base_order]
		Order baseOrder = OrderSamples.LimitOrder("BUY", Decimal.get(1000), 1);
		//! [algo_base_order]
		
		//! [accudistr]
		AvailableAlgoParams.FillAccuDistrParams(baseOrder, 5, "MKT", 100, "10:00:00", "11:00:00", "EST");
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [accudistr]
		
		//! [arrivalpx]
		AvailableAlgoParams.FillArrivalPriceParams(baseOrder, 0.1, "Aggressive", "09:00:00 US/Eastern", "16:00:00 US/Eastern", true, true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [arrivalpx]
		
		sleep(500);
		
		//! [darkice]
		AvailableAlgoParams.FillDarkIceParams(baseOrder, 10, "09:00:00 US/Eastern", "16:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [darkice]
		
		sleep(500);
		
		//! [ad]
		// The Time Zone in "startTime" and "endTime" attributes is ignored and always defaulted to GMT
		AvailableAlgoParams.FillAccumulateDistributeParams(baseOrder, 10, 60, true, true, 1, true, true, "12:00:00", "16:00:00");
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [ad]
		
		sleep(500);
		
		//! [twap]
		AvailableAlgoParams.FillTwapParams(baseOrder, "10:00:00 US/Eastern", "11:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [twap]
		
		sleep(500);
		
		//! [vwap]
		AvailableAlgoParams.FillVwapParams(baseOrder, 0.2, "09:00:00 US/Eastern", "16:00:00 US/Eastern", true, true, true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [vwap]
		
		sleep(500);
		
		//! [balanceimpactrisk]
		AvailableAlgoParams.FillBalanceImpactRiskParams(baseOrder, 0.1, "Aggressive", true);
		client.placeOrder(nextOrderId++, ContractSamples.USOptionContract(), baseOrder);
		//! [balanceimpactrisk]
		
		sleep(500);
		
		//! [minimpact]
		AvailableAlgoParams.FillMinImpactParams(baseOrder, 0.3);
		client.placeOrder(nextOrderId++, ContractSamples.USOptionContract(), baseOrder);
		//! [minimpact]
		
		//! [adaptive]
		AvailableAlgoParams.FillAdaptiveParams(baseOrder, "Normal");
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [adaptive]		
		
		//! [closepx]
		AvailableAlgoParams.FillClosePriceParams(baseOrder, 0.5, "Neutral", "12:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [closepx]
                
		//! [pctvol]
		AvailableAlgoParams.FillPctVolParams(baseOrder, 0.5, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [pctvol]               
                
		//! [pctvolpx]
		AvailableAlgoParams.FillPriceVariantPctVolParams(baseOrder, 0.1, 0.05, 0.01, 0.2, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [pctvolpx]
                
		//! [pctvolsz]
		AvailableAlgoParams.FillSizeVariantPctVolParams(baseOrder, 0.2, 0.4, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [pctvolsz]
                
		//! [pctvoltm]
		AvailableAlgoParams.FillTimeVariantPctVolParams(baseOrder, 0.2, 0.4, "12:00:00 US/Eastern", "14:00:00 US/Eastern", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [pctvoltm]
	}
	
	private static void bracketSample(IClient client, int nextOrderId) {
		
		//BRACKET ORDER
        //! [bracketsubmit]
		List<Order> bracket = OrderSamples.BracketOrder(nextOrderId++, "BUY", Decimal.ONE_HUNDRED, 30, 40, 20);
		for(Order o : bracket) {
			client.placeOrder(o.orderId(), ContractSamples.EuropeanStock(), o);
		}
		//! [bracketsubmit]
		
	}
	
	private static void bulletins(IClient client) throws InterruptedException {
		
		//! [reqnewsbulletins]
		client.reqNewsBulletins(true);
		//! [reqnewsbulletins]
		
		sleep(2000);
		
		//! [cancelnewsbulletins]
		client.cancelNewsBulletins();
		//! [cancelnewsbulletins]
		
	}
	
	private static void marketScanners(IClient client) throws InterruptedException {
		
		/*** Requesting all available parameters which can be used to build a scanner request ***/
        //! [reqscannerparameters]
        client.reqScannerParameters();
        //! [reqscannerparameters]
        sleep(2000);

        /*** Triggering a scanner subscription ***/
        //! [reqscannersubscription]
        client.reqScannerSubscription(7001, ScannerSubscriptionSamples.HighOptVolumePCRatioUSIndexes(), null, null);
		
		TagValue t1 = new TagValue("usdMarketCapAbove", "10000");
		TagValue t2 = new TagValue("optVolumeAbove", "1000");
		TagValue t3 = new TagValue("avgVolumeAbove", "100000000");

		List<TagValue> TagValues = Arrays.asList(t1, t2, t3);
		client.reqScannerSubscription(7002, ScannerSubscriptionSamples.HotUSStkByVolume(), null, TagValues); // requires TWS v973+

		//! [reqscannersubscription]

		//! [reqcomplexscanner]
		List<TagValue> AAPLConIDTag = Collections.singletonList(new TagValue("underConID", "265598")); // 265598 is the conID for AAPL stock
		client.reqScannerSubscription(7003, ScannerSubscriptionSamples.ComplexOrdersAndTrades(), null, AAPLConIDTag); // requires TWS v975+

		//! [reqcomplexscanner]


        sleep(4000);
        /*** Canceling the scanner subscription ***/
        //! [cancelscannersubscription]
        client.cancelScannerSubscription(7001);
		client.cancelScannerSubscription(7002);
		client.cancelScannerSubscription(7003);
        //! [cancelscannersubscription]
		
	}
	
    private static void wshCalendarOperations(IClient client) throws InterruptedException  {

        //! [reqmetadata]
        client.reqWshMetaData(1100);
        //! [reqmetadata]

        sleep(1000);
        
        client.cancelWshMetaData(1100);

        //! [reqeventdata]
        client.reqWshEventData(1101, new WshEventData(8314, false, false, false, "20220511", "", 5));
        //! [reqeventdata]

        sleep(3000);

        //! [reqeventdata]
        client.reqWshEventData(1102, new WshEventData("{\"watchlist\":[\"8314\"]}", false, false, false, "", "20220512", Integer.MAX_VALUE));
        //! [reqeventdata]

        sleep(1000);

        client.cancelWshEventData(1101);
        client.cancelWshEventData(1102);
    }
	
	private static void financialAdvisorOperations(IClient client) {
		
		/*** Requesting FA information ***/
		//! [requestfaaliases]
		client.requestFA(FADataType.ALIASES.id());
		//! [requestfaaliases]
		
		//! [requestfagroups]
		client.requestFA(FADataType.GROUPS.id());
		//! [requestfagroups]
		
		/*** Replacing FA information - Fill in with the appropriate XML string. ***/
		//! [replacefaupdatedgroup]
		client.replaceFA(1000, FADataType.GROUPS.id(), FAMethodSamples.FA_UPDATED_GROUP);
		//! [replacefaupdatedgroup]
		
        //! [reqSoftDollarTiers]
        client.reqSoftDollarTiers(4001);
        //! [reqSoftDollarTiers]
	}
	
	private static void testDisplayGroups(IClient client) throws InterruptedException {
		
		//! [querydisplaygroups]
		client.queryDisplayGroups(9001);
		//! [querydisplaygroups]
		
		sleep(500);
		
		//! [subscribetogroupevents]
		client.subscribeToGroupEvents(9002, 1);
		//! [subscribetogroupevents]
		
		sleep(500);
		
		//! [updatedisplaygroup]
		client.updateDisplayGroup(9002, "8314@SMART");
		//! [updatedisplaygroup]
		
		sleep(500);
		
		//! [subscribefromgroupevents]
		client.unsubscribeFromGroupEvents(9002);
		//! [subscribefromgroupevents]
		
	}
	
	private static void marketDataType(IClient client) {
		
		//! [reqmarketdatatype]
        /*** Switch to live (1) frozen (2) delayed (3) or delayed frozen (4)***/
        client.reqMarketDataType(2);
        //! [reqmarketdatatype]
		
	}
	
	private static void optionsOperations(IClient client) {
		
		//! [reqsecdefoptparams]
		client.reqSecDefOptParams(0, "IBM", "", "STK", 8314);
		//! [reqsecdefoptparams]
		
		//! [calculateimpliedvolatility]
		client.calculateImpliedVolatility(5001, ContractSamples.optionSample(), 20, 90, null);
		//! [calculateimpliedvolatility]
		
		//** Canceling implied volatility ***
		client.cancelCalculateImpliedVolatility(5001);
		
		//! [calculateoptionprice]
		client.calculateOptionPrice(5002, ContractSamples.optionSample(), 0.5, 80, null);
		//! [calculateoptionprice]
		
		//** Canceling option's price calculation ***
		client.cancelCalculateOptionPrice(5002);
		
		//! [exercise_options]
		//** Exercising options ***
		client.exerciseOptions(5003, ContractSamples.OptionWithTradingClass(), 1, 1, "", 1, "20231018-12:00:00", "CustAcct", true);
		//! [exercise_options]
	}

	private static void rerouteCFDOperations(IClient client) throws InterruptedException {

		//! [reqmktdatacfd]
		client.reqMktData(16001, ContractSamples.USStockCFD(), "", false, false, null);
		sleep(1000);
		client.reqMktData(16002, ContractSamples.EuropeanStockCFD(), "", false, false, null);
		sleep(1000);
		client.reqMktData(16003, ContractSamples.CashCFD(), "", false, false, null);
		sleep(1000);
		//! [reqmktdatacfd]

		//! [reqmktdepthcfd]
		client.reqMktDepth(16004, ContractSamples.USStockCFD(), 10, false, null);
		sleep(1000);
		client.reqMktDepth(16005, ContractSamples.EuropeanStockCFD(), 10, false, null);
		sleep(1000);
		client.reqMktDepth(16006, ContractSamples.CashCFD(), 10, false, null);
		sleep(1000);
		//! [reqmktdepthcfd]
	}

	private static void marketRuleOperations(IClient client) throws InterruptedException {
		client.reqContractDetails(17001, ContractSamples.USStock());
		client.reqContractDetails(17002, ContractSamples.Bond());

		sleep(2000);
		
		//! [reqmarketrule]
		client.reqMarketRule(26);
		client.reqMarketRule(240);
		//! [reqmarketrule]
	}

	private static void continuousFuturesOperations(IClient client) throws InterruptedException {

		/*** Requesting continuous futures contract details ***/
		client.reqContractDetails(18001, ContractSamples.ContFut());

		/*** Requesting historical data for continuous futures ***/
		//! [reqhistoricaldatacontfut]
		client.reqHistoricalData(18002, ContractSamples.ContFut(), "", "1 Y", "1 month", "TRADES", 0, 1, false, null);
		sleep(10000);
		/*** Canceling historical data request for continuous futures ***/
		client.cancelHistoricalData(18002);
		//! [reqhistoricaldatacontfut]
	}
	
	private static void tickByTickOperations(IClient client) throws InterruptedException {
		
		/*** Requesting tick-by-tick data (only refresh) ***/
		//! [reqtickbytick]
		client.reqTickByTickData(19001, ContractSamples.USStockAtSmart(), "Last", 0, false);
		client.reqTickByTickData(19002, ContractSamples.USStockAtSmart(), "AllLast", 0, false);
		client.reqTickByTickData(19003, ContractSamples.USStockAtSmart(), "BidAsk", 0, true);
		client.reqTickByTickData(19004, ContractSamples.EurGbpFx(), "MidPoint", 0, false);
		//! [reqtickbytick]

		sleep(10000);

		//! [canceltickbytick]
		client.cancelTickByTickData(19001);
		client.cancelTickByTickData(19002);
		client.cancelTickByTickData(19003);
		client.cancelTickByTickData(19004);
		//! [canceltickbytick]
		
		/*** Requesting tick-by-tick data (refresh + historical ticks) ***/
		//! [reqtickbytick]
		client.reqTickByTickData(19005, ContractSamples.EuropeanStock(), "Last", 10, false);
		client.reqTickByTickData(19006, ContractSamples.EuropeanStock(), "AllLast", 10, false);
		client.reqTickByTickData(19007, ContractSamples.EuropeanStock(), "BidAsk", 10, false);
		client.reqTickByTickData(19008, ContractSamples.EurGbpFx(), "MidPoint", 10, true);
		//! [reqtickbytick]

		sleep(10000);

		//! [canceltickbytick]
		client.cancelTickByTickData(19005);
		client.cancelTickByTickData(19006);
		client.cancelTickByTickData(19007);
		client.cancelTickByTickData(19008);
		//! [canceltickbytick]
	}
	
	private static void whatIfSamples(IClient client, int nextOrderId) throws InterruptedException {
		
		/*** Placing what-if order ***/
		//! [whatiforder]
		client.placeOrder(nextOrderId++, ContractSamples.BondWithCusip(), OrderSamples.WhatIfLimitOrder("BUY", Decimal.get(200), 20));
		//! [whatiforder]
	}
	
	private static void ibkratsSample(IClient client, int nextOrderId) {
		
		//! [ibkratssubmit]
		Order ibkratsOrder = OrderSamples.LimitIBKRATS("BUY", Decimal.ONE_HUNDRED, 330);
		client.placeOrder(nextOrderId++, ContractSamples.IBKRATSContract(), ibkratsOrder);
		//! [ibkratssubmit]
		
	}
	
    private static void miscellaneous(IClient client) throws InterruptedException {

        //! [reqcurrenttime]
        client.reqCurrentTime();
        //! [reqcurrenttime]

        sleep(1000);

        //! [reqcurrenttimeinmillis]
        client.reqCurrentTimeInMillis();
        //! [reqcurrenttimeinmillis]
    }
    
    private static void linkingOperations(IClient client) throws InterruptedException {
        //! [querydisplaygroups]
        client.queryDisplayGroups(9001);
        //! [querydisplaygroups]

        sleep(1000);

        //! [subscribetogroupevents]
        client.subscribeToGroupEvents(9002, 1);
        //! [subscribetogroupevents]

        sleep(1000);

        //! [updatedisplaygroup]
        client.updateDisplayGroup(9002, "8314@SMART");
        //! [updatedisplaygroup]

        sleep(1000);

        //! [subscribefromgroupevents]
        client.unsubscribeFromGroupEvents(9002);
        //! [subscribefromgroupevents]
    }

	private static void sleep(int ms) throws InterruptedException {
		// no-op: calls are recorded, not sent to TWS
	}
}
