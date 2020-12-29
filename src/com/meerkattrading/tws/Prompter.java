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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Prompter {
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final String PROMPT = "\r> ";
	private static final String MORE = "\r... ";
	private final OutputStream output;
	private volatile boolean enabled = false;
	private volatile boolean more = false;

	public Prompter() {
		this.output = null;
	}

	public Prompter(OutputStream output) {
		this.output = output;
	}

	public synchronized void newLine() throws IOException {
		if (enabled && output != null) {
			output.write('\n');
			output.flush();
		}
	}

	public synchronized void returnLine() throws IOException {
		if (enabled && output != null) {
			output.write('\r');
			output.flush();
		}
	}

	public synchronized void prompt(boolean more) throws IOException {
		this.enabled = true;
		this.more = more;
		prompt();
	}

	public synchronized void prompt() throws IOException {
		if (enabled && output != null) {
			String str = more ? MORE : PROMPT;
			output.write(str.getBytes(UTF8));
			output.flush();
		}
	}

}
