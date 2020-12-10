package com.meerkattrading.tws;

import java.util.List;

import com.ib.client.Contract;
import com.ib.client.EWrapper;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.ScannerSubscription;
import com.ib.client.TagValue;

public interface IClient {

	boolean isUseV100Plus();

	int serverVersion();

	String getTwsConnectionTime();

	EWrapper wrapper();

	boolean isConnected();

	void optionalCapabilities(String val);

	String optionalCapabilities();

	void disableUseV100Plus();

	void setConnectOptions(String options);

	void eDisconnect();

	void startAPI();

	void cancelScannerSubscription(int tickerId);

	void reqScannerParameters();

	void reqScannerSubscription(int tickerId, ScannerSubscription subscription,
			List<TagValue> scannerSubscriptionOptions, List<TagValue> scannerSubscriptionFilterOptions);

	void reqMktData(int tickerId, Contract contract, String genericTickList, boolean snapshot,
			boolean regulatorySnapshot, List<TagValue> mktDataOptions);

	void cancelHistoricalData(int tickerId);

	void cancelRealTimeBars(int tickerId);

	void reqHistoricalData(int tickerId, Contract contract, String endDateTime, String durationStr,
			String barSizeSetting, String whatToShow, int useRTH, int formatDate, boolean keepUpToDate,
			List<TagValue> chartOptions);

	void reqHeadTimestamp(int tickerId, Contract contract, String whatToShow, int useRTH, int formatDate);

	void cancelHeadTimestamp(int tickerId);

	void reqRealTimeBars(int tickerId, Contract contract, int barSize, String whatToShow, boolean useRTH,
			List<TagValue> realTimeBarsOptions);

	void reqContractDetails(int reqId, Contract contract);

	void reqMktDepth(int tickerId, Contract contract, int numRows, boolean isSmartDepth,
			List<TagValue> mktDepthOptions);

	void cancelMktData(int tickerId);

	void cancelMktDepth(int tickerId, boolean isSmartDepth);

	void exerciseOptions(int tickerId, Contract contract, int exerciseAction, int exerciseQuantity, String account,
			int override);

	void placeOrder(int id, Contract contract, Order order);

	void reqAccountUpdates(boolean subscribe, String acctCode);

	void reqExecutions(int reqId, ExecutionFilter filter);

	void cancelOrder(int id);

	void reqOpenOrders();

	void reqIds(int numIds);

	void reqNewsBulletins(boolean allMsgs);

	void cancelNewsBulletins();

	void setServerLogLevel(int logLevel);

	void reqAutoOpenOrders(boolean bAutoBind);

	void reqAllOpenOrders();

	void reqManagedAccts();

	void requestFA(int faDataType);

	void replaceFA(int faDataType, String xml);

	void reqCurrentTime();

	void reqFundamentalData(int reqId, Contract contract, String reportType, List<TagValue> fundamentalDataOptions);

	void cancelFundamentalData(int reqId);

	void calculateImpliedVolatility(int reqId, Contract contract, double optionPrice, double underPrice,
			List<TagValue> impliedVolatilityOptions);

	void cancelCalculateImpliedVolatility(int reqId);

	void calculateOptionPrice(int reqId, Contract contract, double volatility, double underPrice,
			List<TagValue> optionPriceOptions);

	void cancelCalculateOptionPrice(int reqId);

	void reqGlobalCancel();

	void reqMarketDataType(int marketDataType);

	void reqPositions();

	void reqSecDefOptParams(int reqId, String underlyingSymbol, String futFopExchange, String underlyingSecType,
			int underlyingConId);

	void reqSoftDollarTiers(int reqId);

	void cancelPositions();

	void reqPositionsMulti(int reqId, String account, String modelCode);

	void cancelPositionsMulti(int reqId);

	void cancelAccountUpdatesMulti(int reqId);

	void reqAccountUpdatesMulti(int reqId, String account, String modelCode, boolean ledgerAndNLV);

	void reqAccountSummary(int reqId, String group, String tags);

	void cancelAccountSummary(int reqId);

	void verifyRequest(String apiName, String apiVersion);

	void verifyMessage(String apiData);

	void verifyAndAuthRequest(String apiName, String apiVersion, String opaqueIsvKey);

	void verifyAndAuthMessage(String apiData, String xyzResponse);

	void queryDisplayGroups(int reqId);

	void subscribeToGroupEvents(int reqId, int groupId);

	void updateDisplayGroup(int reqId, String contractInfo);

	void unsubscribeFromGroupEvents(int reqId);

	void reqMatchingSymbols(int reqId, String pattern);

	void reqFamilyCodes();

	void reqMktDepthExchanges();

	void reqSmartComponents(int reqId, String bboExchange);

	void reqNewsProviders();

	void reqNewsArticle(int requestId, String providerCode, String articleId, List<TagValue> newsArticleOptions);

	void reqHistoricalNews(int requestId, int conId, String providerCodes, String startDateTime, String endDateTime,
			int totalResults, List<TagValue> historicalNewsOptions);

	void reqHistogramData(int tickerId, Contract contract, boolean useRTH, String timePeriod);

	void cancelHistogramData(int tickerId);

	void reqMarketRule(int marketRuleId);

	void reqPnL(int reqId, String account, String modelCode);

	void cancelPnL(int reqId);

	void reqPnLSingle(int reqId, String account, String modelCode, int conId);

	void cancelPnLSingle(int reqId);

	void reqHistoricalTicks(int reqId, Contract contract, String startDateTime, String endDateTime, int numberOfTicks,
			String whatToShow, int useRth, boolean ignoreSize, List<TagValue> miscOptions);

	void reqTickByTickData(int reqId, Contract contract, String tickType, int numberOfTicks, boolean ignoreSize);

	void cancelTickByTickData(int reqId);

	void reqCompletedOrders(boolean apiOnly);

	String connectedHost();

}