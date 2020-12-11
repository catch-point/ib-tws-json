package com.meerkattrading.tws;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;

public class LineReader {
	private final BufferedReader reader;

	public LineReader(BufferedReader reader) {
		this.reader = reader;
	}

	public ParsedInput readLine(CharSequence prefix) throws IOException, SyntaxError {
		try {
			String line = reader.readLine();
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

}
