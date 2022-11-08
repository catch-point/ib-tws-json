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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;

import javax.swing.JFrame;

import ibcalpha.ibc.AbstractLoginHandler;
import ibcalpha.ibc.LoginManager;

/**
 * Decodes the Base64 username and password to login to IB TWS
 *
 * @author James Leigh
 *
 */
public class Base64LoginManager extends LoginManager {

	private String FIXBase64UserName;
	private String FIXBase64Password;
	private String IBAPIBase64UserName;
	private String IBAPIBase64Password;

	private volatile JFrame loginFrame = null;

    private volatile AbstractLoginHandler loginHandler = null;

	private String decode(String encoded) {
		if (encoded != null && encoded.length() > 0) {
			Decoder decoder = Base64.getDecoder();
			byte[] decoded = decoder.decode(encoded);
			return new String(decoded, StandardCharsets.UTF_8);
		} else {
			return "";
		}
	}

	@Override
	public synchronized void setLoginState(LoginState arg0) {
		super.setLoginState(arg0);
		this.notifyAll();
	}

	public String FIXBase64UserName() {
		return this.FIXBase64UserName;
	}

	public void FIXBase64UserName(String FIXBase64UserName) {
		this.FIXBase64UserName = FIXBase64UserName;
	}

	public String FIXBase64Password() {
		return this.FIXBase64Password;
	}

	public void FIXBase64Password(String FIXBase64Password) {
		this.FIXBase64Password = FIXBase64Password;
	}

	@Override
	public String FIXUserName() {
		return decode(FIXBase64UserName());
	}

	@Override
	public String FIXPassword() {
		return decode(FIXBase64Password());
	}

	public String IBAPIBase64UserName() {
		return this.IBAPIBase64UserName;
	}

	public void IBAPIBase64UserName(String IBAPIBase64UserName) {
		this.IBAPIBase64UserName = IBAPIBase64UserName;
	}

	public String IBAPIBase64Password() {
		return this.IBAPIBase64Password;
	}

	public void IBAPIBase64Password(String IBAPIBase64Password) {
		this.IBAPIBase64Password = IBAPIBase64Password;
	}

	@Override
	public String IBAPIUserName() {
		return decode(IBAPIBase64UserName());
	}

	@Override
	public String IBAPIPassword() {
		return decode(IBAPIBase64Password());
	}

	@Override
	public JFrame getLoginFrame() {
		return loginFrame;
	}

	@Override
	public void setLoginFrame(JFrame window) {
		loginFrame = window;
	}

	@Override
	public void logDiagnosticMessage() {
		// nothing to say
	}

	@Override
	public AbstractLoginHandler getLoginHandler() {
		return loginHandler;
	}

	@Override
	public void setLoginHandler(AbstractLoginHandler loginHandler) {
		this.loginHandler = loginHandler;
	}

}
