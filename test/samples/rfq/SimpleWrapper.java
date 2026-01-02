/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package samples.rfq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.ib.client.Bar;
import com.ib.client.CommissionAndFeesReport;
import com.ib.client.Contract;
import com.ib.client.ContractDescription;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.DepthMktDataDescription;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.FamilyCode;
import com.ib.client.HistogramEntry;
import com.ib.client.HistoricalSession;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.NewsProvider;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.PriceIncrement;
import com.ib.client.SoftDollarTier;
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.ib.client.protobuf.ErrorMessageProto.ErrorMessage;
import com.ib.client.protobuf.ExecutionDetailsEndProto.ExecutionDetailsEnd;
import com.ib.client.protobuf.ExecutionDetailsProto.ExecutionDetails;
import com.ib.client.protobuf.OpenOrderProto.OpenOrder;
import com.ib.client.protobuf.OpenOrdersEndProto.OpenOrdersEnd;
import com.ib.client.protobuf.OrderStatusProto.OrderStatus;


public class SimpleWrapper implements EWrapper {
	private static final int MAX_MESSAGES = 1000000;
	private final static SimpleDateFormat m_df = new SimpleDateFormat("HH:mm:ss"); 

	// main client
	private EJavaSignal m_signal = new EJavaSignal();
	private EClientSocket m_client = new EClientSocket(this, m_signal);

	// utils
	private long ts;
	private PrintStream m_output;
	private int m_outputCounter = 0;
	private int m_messageCounter;	

	protected EClientSocket client() { return m_client; }

	protected SimpleWrapper() {
		initNextOutput();
		attachDisconnectHook(this);
	}

	public void connect() {
		connect(1);	
	}

	public void connect(int clientId) {
		String host = System.getProperty("jts.host");
		host = host != null ? host : "";
		m_client.eConnect(host, 7497, clientId);
		
        final EReader reader = new EReader(m_client, m_signal);
        
        reader.start();
       
		new Thread(() -> {
            while (m_client.isConnected()) {
                m_signal.waitForSignal();
                try {
                    SwingUtilities.invokeAndWait(() -> {
                                try {
                                    reader.processMsgs();
                                } catch (IOException e) {
                                    error(e);
                                }
                            });
                } catch (Exception e) {
                    error(e);
                }
            }
        }).start();
	}

	public void disconnect() {
		m_client.eDisconnect();
	}

	/* ***************************************************************
	 * AnyWrapper
	 *****************************************************************/

	public void error(Exception e) {
		e.printStackTrace(m_output);
	}

	public void error(String str) {
		m_output.println(str);
	}

	public void error(int id, int errorCode, String errorMsg) {
		logIn("Error id=" + id + " code=" + errorCode + " msg=" + errorMsg);
	}

	public void connectionClosed() {
		m_output.println("--------------------- CLOSED ---------------------");
	}	

	/* ***************************************************************
	 * EWrapper
	 *****************************************************************/

	public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
		logIn("tickPrice");
	}

	public void tickSize(int tickerId, int field, int size) {
		logIn("tickSize");
	}

	public void tickGeneric(int tickerId, int tickType, double value) {
		logIn("tickGeneric");
	}

	public void tickString(int tickerId, int tickType, String value) {
		logIn("tickString");
	}	

	public void tickSnapshotEnd(int tickerId) {
		logIn("tickSnapshotEnd");
	}

	@Override
	public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol,
			double delta, double optPrice, double pvDividend,
			double gamma, double vega, double theta, double undPrice) {
		logIn("tickOptionComputation");
	}

	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate) {
		logIn("tickEFP");
	}

	public void orderStatus(int orderId, String status, double filled, double remaining,
			double avgFillPrice, int permId, int parentId, double lastFillPrice,
			int clientId, String whyHeld, double mktCapPrice) {
		logIn("orderStatus");    	
	}

	public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		logIn("openOrder");
	}

	public void openOrderEnd() {
		logIn("openOrderEnd");
	}

	public void updateAccountValue(String key, String value, String currency, String accountName) {
		logIn("updateAccountValue");
	}

	public void updatePortfolio(Contract contract, double position, double marketPrice, double marketValue,
			double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
		logIn("updatePortfolio");
	}

	public void updateAccountTime(String timeStamp) {
		logIn("updateAccountTime");
	}

	public void accountDownloadEnd(String accountName) {
		logIn("accountDownloadEnd");
	}

	public void nextValidId(int orderId) {
		logIn("nextValidId");
	}

	public void contractDetails(int reqId, ContractDetails contractDetails) {
		logIn("contractDetails");
	}

	public void contractDetailsEnd(int reqId) {
		logIn("contractDetailsEnd");
	}

	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		logIn("bondContractDetails");
	}

	public void execDetails(int reqId, Contract contract, Execution execution) {
		logIn("execDetails");
	}

	public void execDetailsEnd(int reqId) {
		logIn("execDetailsEnd");
	}

	public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {
		logIn("updateMktDepth");
	}

	public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation,
			int side, double price, int size, boolean isSmartDepth) {
		logIn("updateMktDepthL2");
	}

	public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
		logIn("updateNewsBulletin");
	}

	public void managedAccounts(String accountsList) {
		logIn("managedAccounts");
	}

	public void receiveFA(int faDataType, String xml) {
		logIn("receiveFA");
	}

	@Override
	public void replaceFAEnd(int faDataType, String xml) {
		logIn("replaceFAEnd");
	}

	public void historicalData(int reqId, Bar bar) {
		logIn("historicalData");
	}

	public void scannerParameters(String xml) {
		logIn("scannerParameters");
	}

	public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
			String benchmark, String projection, String legsStr) {
		logIn("scannerData");
	}

	public void scannerDataEnd(int reqId) {
		logIn("scannerDataEnd");
	}

	public void realtimeBar(int reqId, long time, double open, double high, double low, double close, 
			long volume, double wap, int count) {
		logIn("realtimeBar");
	}

	public void currentTime(long millis) {
		logIn("currentTime");
	}

	public void fundamentalData(int reqId, String data) {
		logIn("fundamentalData");    	
	}

	public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
		logIn("deltaNeutralValidation");    	
	}

	public void marketDataType(int reqId, int marketDataType) {
		logIn("marketDataType");
	}
	
	@Override
	public void commissionAndFeesReport(CommissionAndFeesReport arg0) {
		logIn("commissionAndFeesReport");
	}

	public void position(String account, Contract contract, double pos, double avgCost) {
		logIn("position");
	}
	
	public void positionEnd() {
		logIn("positionEnd");
	}
	
	public void accountSummary( int reqId, String account, String tag, String value, String currency) {
		logIn("accountSummary");
	}

	public void accountSummaryEnd( int reqId) {
		logIn("accountSummaryEnd");
	}

	public void verifyMessageAPI( String apiData) {
		logIn("verifyMessageAPI");
	}

	public void verifyCompleted( boolean isSuccessful, String errorText){
		logIn("verifyCompleted");
	}

	public void verifyAndAuthMessageAPI( String apiData, String xyzChallenge) {
		logIn("verifyAndAuthMessageAPI");
	}

	public void verifyAndAuthCompleted( boolean isSuccessful, String errorText){
		logIn("verifyAndAuthCompleted");
	}

	public void displayGroupList( int reqId, String groups){
		logIn("displayGroupList");
	}

	public void displayGroupUpdated( int reqId, String contractInfo){
		logIn("displayGroupUpdated");
	}

	public void positionMulti( int reqId, String account, String modelCode, Contract contract, double pos, double avgCost) {
		logIn("positionMulti");
	}
	
	public void positionMultiEnd( int reqId) {
		logIn("positionMultiEnd");
	}
	
	public void accountUpdateMulti( int reqId, String account, String modelCode, String key, String value, String currency) {
		logIn("accountUpdateMulti");
	}

	public void accountUpdateMultiEnd( int reqId) {
		logIn("accountUpdateMultiEnd");
	}

	/* ***************************************************************
	 * Helpers
	 *****************************************************************/
	protected void logIn(String method) {
		m_messageCounter++;
		if (m_messageCounter == MAX_MESSAGES) {
			m_output.close();
			initNextOutput();
			m_messageCounter = 0;
		}    	
		m_output.println("[W] > " + method);
	}

	protected static void consoleMsg(String str) {
		System.out.println(Thread.currentThread().getName() + " (" + tsStr() + "): " + str);
	}

	protected static String tsStr() {
		synchronized (m_df) {
			return m_df.format(new Date());			
		}
	}

	protected static void sleepSec(int sec) {
		sleep(sec * 1000);
	}

	private static void sleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected void swStart() {
		ts = System.currentTimeMillis();
	}

	protected void swStop() {
		long dt = System.currentTimeMillis() - ts;
		m_output.println("[API]" + " Time=" + dt);
	}

	private void initNextOutput() {
		try {
			m_output = new PrintStream(new File("sysout_" + (++m_outputCounter) + ".log"), "UTF-8");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}		
	}

	private static void attachDisconnectHook(final SimpleWrapper ut) {
		Runtime.getRuntime().addShutdownHook(new Thread(ut::disconnect));
	}
	
	public void connectAck() {
		m_client.startAPI();
	}

	@Override
	public void completedOrder(Contract arg0, Order arg1, OrderState arg2) {
		logIn("completedOrder");
	}

	@Override
	public void completedOrdersEnd() {
		logIn("completedOrdersEnd");
	}

	@Override
	public void currentTimeInMillis(long arg0) {
		logIn("currentTimeInMillis");
	}

	@Override
	public void error(int arg0, long arg1, int arg2, String arg3, String arg4) {
		logIn("error");
	}

	@Override
	public void errorProtoBuf(ErrorMessage arg0) {
		logIn("errorProtoBuf");
	}

	@Override
	public void execDetailsEndProtoBuf(ExecutionDetailsEnd arg0) {
		logIn("execDetailsEndProtoBuf");
	}

	@Override
	public void execDetailsProtoBuf(ExecutionDetails arg0) {
		logIn("execDetailsProtoBuf");
	}

	@Override
	public void familyCodes(FamilyCode[] arg0) {
		logIn("familyCodes");
	}

	@Override
	public void headTimestamp(int arg0, String arg1) {
		logIn("headTimestamp");
	}

	@Override
	public void histogramData(int arg0, List<HistogramEntry> arg1) {
		logIn("histogramData");
	}

	@Override
	public void historicalDataEnd(int arg0, String arg1, String arg2) {
		logIn("historicalDataEnd");
	}

	@Override
	public void historicalDataUpdate(int arg0, Bar arg1) {
		logIn("historicalDataUpdate");
	}

	@Override
	public void historicalNews(int arg0, String arg1, String arg2, String arg3, String arg4) {
		logIn("historicalNews");
	}

	@Override
	public void historicalNewsEnd(int arg0, boolean arg1) {
		logIn("historicalNewsEnd");
	}

	@Override
	public void historicalSchedule(int arg0, String arg1, String arg2, String arg3, List<HistoricalSession> arg4) {
		logIn("historicalSchedule");
	}

	@Override
	public void historicalTicks(int arg0, List<HistoricalTick> arg1, boolean arg2) {
		logIn("historicalTicks");
	}

	@Override
	public void historicalTicksBidAsk(int arg0, List<HistoricalTickBidAsk> arg1, boolean arg2) {
		logIn("historicalTicksBidAsk");
	}

	@Override
	public void historicalTicksLast(int arg0, List<HistoricalTickLast> arg1, boolean arg2) {
		logIn("historicalTicksLast");
	}

	@Override
	public void marketRule(int arg0, PriceIncrement[] arg1) {
		logIn("marketRule");
	}

	@Override
	public void mktDepthExchanges(DepthMktDataDescription[] arg0) {
		logIn("mktDepthExchanges");
	}

	@Override
	public void newsArticle(int arg0, int arg1, String arg2) {
		logIn("newsArticle");
	}

	@Override
	public void newsProviders(NewsProvider[] arg0) {
		logIn("newsProviders");
	}

	@Override
	public void openOrderProtoBuf(OpenOrder arg0) {
		logIn("openOrderProtoBuf");
	}

	@Override
	public void openOrdersEndProtoBuf(OpenOrdersEnd arg0) {
		logIn("openOrdersEndProtoBuf");
	}

	@Override
	public void orderBound(long arg0, int arg1, int arg2) {
		logIn("orderBound");
	}

	@Override
	public void orderStatus(int arg0, String arg1, Decimal arg2, Decimal arg3, double arg4, long arg5, int arg6,
			double arg7, int arg8, String arg9, double arg10) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderStatusProtoBuf(OrderStatus arg0) {
		logIn("orderStatusProtoBuf");
	}

	@Override
	public void pnl(int arg0, double arg1, double arg2, double arg3) {
		logIn("pnl");
	}

	@Override
	public void pnlSingle(int arg0, Decimal arg1, double arg2, double arg3, double arg4, double arg5) {
		logIn("pnlSingle");
	}

	@Override
	public void position(String arg0, Contract arg1, Decimal arg2, double arg3) {
		logIn("position");
	}

	@Override
	public void positionMulti(int arg0, String arg1, String arg2, Contract arg3, Decimal arg4, double arg5) {
		logIn("positionMulti");
	}

	@Override
	public void realtimeBar(int arg0, long arg1, double arg2, double arg3, double arg4, double arg5, Decimal arg6,
			Decimal arg7, int arg8) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rerouteMktDataReq(int arg0, int arg1, String arg2) {
		logIn("rerouteMktDataReq");
	}

	@Override
	public void rerouteMktDepthReq(int arg0, int arg1, String arg2) {
		logIn("rerouteMktDepthReq");
	}

	@Override
	public void securityDefinitionOptionalParameter(int arg0, String arg1, int arg2, String arg3, String arg4,
			Set<String> arg5, Set<Double> arg6) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void securityDefinitionOptionalParameterEnd(int arg0) {
		logIn("securityDefinitionOptionalParameterEnd");
	}

	@Override
	public void smartComponents(int arg0, Map<Integer, Entry<String, Character>> arg1) {
		logIn("smartComponents");
	}

	@Override
	public void softDollarTiers(int arg0, SoftDollarTier[] arg1) {
		logIn("softDollarTiers");
	}

	@Override
	public void symbolSamples(int arg0, ContractDescription[] arg1) {
		logIn("symbolSamples");
	}

	@Override
	public void tickByTickAllLast(int arg0, int arg1, long arg2, double arg3, Decimal arg4, TickAttribLast arg5,
			String arg6, String arg7) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickByTickBidAsk(int arg0, long arg1, double arg2, double arg3, Decimal arg4, Decimal arg5,
			TickAttribBidAsk arg6) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tickByTickMidPoint(int arg0, long arg1, double arg2) {
		logIn("tickByTickMidPoint");
	}

	@Override
	public void tickNews(int arg0, long arg1, String arg2, String arg3, String arg4, String arg5) {
		logIn("tickNews");
	}

	@Override
	public void tickReqParams(int arg0, double arg1, String arg2, int arg3) {
		logIn("tickReqParams");
	}

	@Override
	public void tickSize(int arg0, int arg1, Decimal arg2) {
		logIn("tickSize");
	}

	@Override
	public void updateMktDepth(int arg0, int arg1, int arg2, int arg3, double arg4, Decimal arg5) {
		logIn("updateMktDepth");
	}

	@Override
	public void updateMktDepthL2(int arg0, int arg1, String arg2, int arg3, int arg4, double arg5, Decimal arg6,
			boolean arg7) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updatePortfolio(Contract arg0, Decimal arg1, double arg2, double arg3, double arg4, double arg5,
			double arg6, String arg7) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void userInfo(int arg0, String arg1) {
		logIn("userInfo");
	}

	@Override
	public void wshEventData(int arg0, String arg1) {
		logIn("wshEventData");
	}

	@Override
	public void wshMetaData(int arg0, String arg1) {
		logIn("wshMetaData");
	}
}
