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
	private static final byte NL = "\n".getBytes(UTF8)[0];
	private final InputStream input;
	private final Prompter prompter;
	private ByteBuffer buffer = ByteBuffer.allocate(256);
	private boolean eof = false;

	public LineReader(InputStream reader) {
		this(reader, new Prompter());
	}

	public LineReader(InputStream in, Prompter prompter) {
		this.input = in;
		this.prompter = prompter;
	}

	public ParsedInput readLine(CharSequence prefix) throws IOException, SyntaxError {
		try {
			prompter.prompt(prefix.length() > 0);
			CharSequence line = readLine();
			if (line == null) {
				prompter.newLine();
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

	public synchronized String currentLine() throws IOException {
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
