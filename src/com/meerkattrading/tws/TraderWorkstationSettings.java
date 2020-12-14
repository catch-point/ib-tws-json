/* 
 * Copyright (c) 2020 James Leigh
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

import java.lang.reflect.InvocationTargetException;

import ibcalpha.ibc.Settings;

/**
 * Settings used when running IB TWS software
 * 
 * @author James Leigh
 *
 */
public class TraderWorkstationSettings extends Settings {

	private String AcceptIncomingConnectionAction = "manual";
	private boolean AcceptNonBrokerageAccountWarning = true;
	private boolean AllowBlindTrading = false;
	private boolean DismissNSEComplianceNotice = true;
	private boolean DismissPasswordExpiryWarning = false;
	private String ExistingSessionDetectedAction = "manual";
	private boolean FIX = false;
	private String LogComponents = "never";
	private boolean MinimizeMainWindow = false;
	private boolean ReadOnlyLogin = false;
	private boolean StoreSettingsOnServer = false;
	private boolean SuppressInfoMessages = true;

	public TraderWorkstationSettings() {
	}

	public String AcceptIncomingConnectionAction() {
		return this.AcceptIncomingConnectionAction;
	}

	public void AcceptIncomingConnectionAction(String AcceptIncomingConnectionAction) {
		this.AcceptIncomingConnectionAction = AcceptIncomingConnectionAction;
	}

	public boolean AcceptNonBrokerageAccountWarning() {
		return this.AcceptNonBrokerageAccountWarning;
	}

	public void AcceptNonBrokerageAccountWarning(boolean AcceptNonBrokerageAccountWarning) {
		this.AcceptNonBrokerageAccountWarning = AcceptNonBrokerageAccountWarning;
	}

	public boolean AllowBlindTrading() {
		return this.AllowBlindTrading;
	}

	public void AllowBlindTrading(boolean AllowBlindTrading) {
		this.AllowBlindTrading = AllowBlindTrading;
	}

	public boolean DismissNSEComplianceNotice() {
		return this.DismissNSEComplianceNotice;
	}

	public void DismissNSEComplianceNotice(boolean DismissNSEComplianceNotice) {
		this.DismissNSEComplianceNotice = DismissNSEComplianceNotice;
	}

	public boolean DismissPasswordExpiryWarning() {
		return this.DismissPasswordExpiryWarning;
	}

	public void DismissPasswordExpiryWarning(boolean DismissPasswordExpiryWarning) {
		this.DismissPasswordExpiryWarning = DismissPasswordExpiryWarning;
	}

	public String ExistingSessionDetectedAction() {
		return this.ExistingSessionDetectedAction;
	}

	public void ExistingSessionDetectedAction(String ExistingSessionDetectedAction) {
		this.ExistingSessionDetectedAction = ExistingSessionDetectedAction;
	}

	public boolean FIX() {
		return this.FIX;
	}

	public void FIX(boolean FIX) {
		this.FIX = FIX;
	}

	public String LogComponents() {
		return this.LogComponents;
	}

	public void LogComponents(String LogComponents) {
		this.LogComponents = LogComponents;
	}

	public boolean MinimizeMainWindow() {
		return this.MinimizeMainWindow;
	}

	public void MinimizeMainWindow(boolean MinimizeMainWindow) {
		this.MinimizeMainWindow = MinimizeMainWindow;
	}

	public boolean ReadOnlyLogin() {
		return this.ReadOnlyLogin;
	}

	public void ReadOnlyLogin(boolean ReadOnlyLogin) {
		this.ReadOnlyLogin = ReadOnlyLogin;
	}

	public boolean StoreSettingsOnServer() {
		return this.StoreSettingsOnServer;
	}

	public void StoreSettingsOnServer(boolean StoreSettingsOnServer) {
		this.StoreSettingsOnServer = StoreSettingsOnServer;
	}

	public boolean SuppressInfoMessages() {
		return this.SuppressInfoMessages;
	}

	public void SuppressInfoMessages(boolean SuppressInfoMessages) {
		this.SuppressInfoMessages = SuppressInfoMessages;
	}

	@Override
	public boolean getBoolean(String name, boolean defaultValue) {
		Object value = getObject(name);
		if (value != null && value.toString().length() > 0)
			return Boolean.valueOf(value.toString());
		else
			return defaultValue;
	}

	@Override
	public char getChar(String name, String defaultValue) {
		return defaultValue.charAt(0);
	}

	@Override
	public double getDouble(String name, double defaultValue) {
		return defaultValue;
	}

	@Override
	public int getInt(String name, int defaultValue) {
		Object value = getObject(name);
		if (value != null && value.toString().length() > 0)
			return Integer.parseInt(value.toString());
		else
			return defaultValue;
	}

	@Override
	public String getString(String name, String defaultValue) {
		Object value = getObject(name);
		if (value != null)
			return value.toString();
		else
			return defaultValue;
	}

	public Object getObject(String name) {
		try {
			return this.getClass().getMethod(name).invoke(this);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e) {
			return null;
		}
	}

	@Override
	public void logDiagnosticMessage() {
		// nothing to say
	}

}
