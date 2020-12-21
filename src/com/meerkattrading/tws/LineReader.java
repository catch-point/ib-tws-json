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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads and verifies the input from the shell
 *
 * @author James Leigh
 *
 */
public class LineReader {
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final byte CR = "\r".getBytes(UTF8)[0];
	private static final byte NL = "\n".getBytes(UTF8)[0];;
	private static final String PROMPT = "\r> ";
	private static final String MORE = "\r... ";
	private final InputStream input;
	private final OutputStream output;
	private ByteBuffer buffer = ByteBuffer.allocate(256);
	private boolean eof = false;
	private volatile boolean more = false;

	public LineReader(InputStream reader) {
		this(reader, null);
	}

	public LineReader(InputStream in, OutputStream out) {
		this.input = in;
		this.output = out;
	}

	public ParsedInput readLine(CharSequence prefix) throws IOException, SyntaxError {
		try {
			more = prefix.length() > 0;
			prompt();
			CharSequence line = readLine();
			if (line == null) {
				newLine();
				throw new EOFException();
			}
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

	public synchronized void prompt() throws IOException {
		if (output != null) {
			String str = more ? MORE : PROMPT;
			output.write(str.getBytes(UTF8));
			String lastLine = lastLine();
			if (lastLine != null) {
				output.write(lastLine.getBytes(UTF8));
			}
			output.flush();
		}
	}

	public synchronized void returnLine() throws IOException {
		if (output != null) {
			output.write('\r');
			output.flush();
		}
	}

	public synchronized void newLine() throws IOException {
		if (output != null) {
			output.write('\n');
			output.flush();
		}
	}

	private CharSequence readLine() throws IOException, SyntaxError {
		try {
			CharSequence buffered = readLineFromBuffer();
			if (buffered != null)
				return buffered;
			int off = buffer.position() + buffer.arrayOffset();
			int len = Math.min(Math.max(input.available(), 1), buffer.remaining());
			int read = eof ? -1 : input.read(buffer.array(), off, len);
			if (read > 0)
				buffer.position(buffer.position() + read);
			CharSequence readLine = readLineFromBuffer();
			if (readLine != null) {
				return readLine;
			} else if (read > 0) {
				expand(read);
				return readLine();
			} else {
				eof = true;
				return readFromBuffer();
			}
		} catch (InterruptedIOException e) {
			return null;
		}
	}

	private synchronized CharSequence readLineFromBuffer() throws IOException, SyntaxError {
		for (int i = 0; i < buffer.position(); i++) {
			byte chr = buffer.get(i);
			if (chr == '\n' || chr == '\r') {
				while (i < buffer.position()
						&& (buffer.get(i) == '\n' || buffer.get(i) == '\r')) {
					i++;
				}
				byte[] line = new byte[i];
				buffer.flip();
				buffer.get(line);
				buffer.compact();
				return new String(line, UTF8);
			}
		}
		return null;
	}

	private synchronized CharSequence readFromBuffer() throws IOException {
		if (buffer.position() == 0)
			return null;
		buffer.flip();
		byte[] line = new byte[buffer.remaining()];
		buffer.get(line);
		buffer.clear();
		return new String(line, UTF8);
	}

	private String lastLine() throws IOException {
		ByteBuffer copy = buffer.duplicate();
		copy.flip();
		for (int i = copy.limit(); i > 0; i--) {
			byte chr = copy.get(i - 1);
			if (chr == NL || chr == CR) {
				byte[] line = new byte[copy.limit() - i];
				copy.position(i);
				copy.get(line);
				return new String(line, UTF8);
			}
		}
		byte[] line = new byte[copy.remaining()];
		copy.get(line);
		return new String(line, UTF8);
	}

	private synchronized void expand(int expected) {
		if (buffer.remaining() < expected) {
			ByteBuffer new_buf = ByteBuffer.allocate(buffer.capacity() * 2);
			ByteBuffer old_buf = buffer.duplicate();
			old_buf.flip();
			new_buf.put(old_buf);
			buffer = new_buf;
		}
	}

}
