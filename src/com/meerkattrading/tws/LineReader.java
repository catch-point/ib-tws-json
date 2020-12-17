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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Reads and verifies the input from the shell
 * 
 * @author James Leigh
 *
 */
public class LineReader {
	private final BufferedReader reader;

	public LineReader(BufferedReader reader) {
		this.reader = reader;
	}

	public ParsedInput readLine(CharSequence prefix) throws IOException, SyntaxError {
		try {
			String line = readLine();
			if (line == null)
				throw new EOFException();
			StringBuilder sb = new StringBuilder();
			sb.append(prefix).append(line);
			try {
				return new ParsedInput(sb);
			} catch (MoreInputExpected e) {
				return readLine(sb.append("\n"));
			}
		} catch (InterruptedIOException e) {
			return null;
		}
	}

	private String readLine() throws IOException {
		return reader.readLine();
	}

}
