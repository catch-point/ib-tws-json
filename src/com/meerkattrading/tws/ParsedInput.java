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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParsedInput {
	private static int EOF = -1;
	private final List<String> values = new ArrayList<>();
	private final CharSequence buffer;
	private int pos = 0;

	public ParsedInput(CharSequence input) throws SyntaxError, MoreInputExpected {
		this.buffer = input;
		try {
			values.add(readJavaIdentifier().toString());
			readWhiteSpace();
			while (EOF != charAt(buffer, pos)) {
				values.add(readValue().toString());
				readWhiteSpace();
			}
		} catch (SyntaxError e) {
			if (EOF == charAt(buffer, pos))
				throw new MoreInputExpected(e.getMessage());
			else
				throw e;
		}
	}

	public String toString() {
		return buffer.toString();
	}

	public CharSequence getInput() {
		return buffer;
	}

	public List<String> getParsedValues() {
		return values;
	}

	private CharSequence readJavaIdentifier() {
		int start = pos;
		if (Character.isJavaIdentifierStart(charAt(buffer, pos))) {
			while (Character.isJavaIdentifierPart(charAt(buffer, pos))) {
				pos++;
			}
		}
		return buffer.subSequence(start, pos);
	}

	private CharSequence readValue() throws SyntaxError {
		readWhiteSpace();
		if ('"' == charAt(buffer, pos)) {
			return readQuotedString();
		} else if ('-' == charAt(buffer, pos) || Character.isDigit(charAt(buffer, pos))) {
			return readNumber();
		} else if ('{' == charAt(buffer, pos)) {
			return readObject();
		} else if ('[' == charAt(buffer, pos)) {
			return readArray();
		} else if ('t' == charAt(buffer, pos)) {
			return read("true");
		} else if ('f' == charAt(buffer, pos)) {
			return read("false");
		} else if ('n' == charAt(buffer, pos)) {
			return read("null");
		} else {
			throw new SyntaxError(lineNumber(), column(), "Expected JSON value");
		}
	}

	private CharSequence readWhiteSpace() {
		final int start = pos;
		while (Character.isWhitespace(charAt(buffer, pos))) {
			pos++;
		}
		return buffer.subSequence(start, pos);
	}

	private CharSequence readQuotedString() throws SyntaxError {
		int start = pos;
		read('"');
		final int pos1 = pos;
		while (charAt(buffer, pos) != buffer.charAt(start) && !(EOF == charAt(buffer, pos1))) {
			if ('\\' == charAt(buffer, pos)) {
				read('\\');
				if ('u' == charAt(buffer, pos)) {
					read('u');
					readHexDigit();
					readHexDigit();
					readHexDigit();
					readHexDigit();
				} else {
					readChar(new char[] { '"', '\\', '/', 'b', 'f', 'n', 'r', 't' });
				}
			} else {
				pos++;
			}
		}
		read(buffer.charAt(start));
		return buffer.subSequence(start, pos);
	}

	private CharSequence readHexDigit() throws SyntaxError {
		int chr = charAt(buffer, pos);
		if (Character.isDigit(chr) || 'a' <= chr && chr <= 'f' || 'A' <= chr && chr <= 'F') {
			return buffer.subSequence(pos, pos++);
		}
		throw new SyntaxError(lineNumber(), column(), "Expected hex digit but got " + chr);
	}

	private CharSequence readNumber() throws SyntaxError {
		int start = pos;
		if ('-' == charAt(buffer, pos))
			pos++;
		if ('1' <= charAt(buffer, pos) && charAt(buffer, pos) <= '9') {
			while (Character.isDigit(charAt(buffer, pos)))
				pos++;
		}
		if ('0' == charAt(buffer, pos))
			pos++;
		if ('.' == charAt(buffer, pos)) {
			pos++;
			while (Character.isDigit(charAt(buffer, pos)))
				pos++;
		}
		if ('e' == charAt(buffer, pos) || 'E' == charAt(buffer, pos)) {
			pos++;
			if ('-' == charAt(buffer, pos) || '+' == charAt(buffer, pos))
				pos++;
			while (Character.isDigit(charAt(buffer, pos)))
				pos++;
		}
		return buffer.subSequence(start, pos);
	}

	private CharSequence readObject() throws SyntaxError {
		int start = pos;
		read('{');
		readWhiteSpace();
		if ('}' != charAt(buffer, pos) && !(EOF == charAt(buffer, pos))) {
			do {
				if (',' == charAt(buffer, pos))
					read(',');
				readWhiteSpace();
				readQuotedString();
				readWhiteSpace();
				read(':');
				readValue();
				readWhiteSpace();
			} while (',' == charAt(buffer, pos));
		}
		read('}');
		return buffer.subSequence(start, pos);
	}

	private CharSequence readArray() throws SyntaxError {
		int start = pos;
		read('[');
		readWhiteSpace();
		while (']' != charAt(buffer, pos)) {
			readValue();
			readWhiteSpace();
			if (',' != charAt(buffer, pos))
				break;
			read(',');
		}
		read(']');
		return buffer.subSequence(start, pos);
	}

	private CharSequence read(String word) throws SyntaxError {
		for (char chr : word.toCharArray()) {
			read(chr);
		}
		return word;
	}

	private CharSequence readChar(char[] chars) throws SyntaxError {
		int ch = charAt(buffer, pos);
		for (int c = 0; c < chars.length; c++) {
			if (ch == chars[c])
				return buffer.subSequence(pos, pos++);
		}
		throw new SyntaxError(lineNumber(), column(),
				"Expected one of " + Arrays.asList(chars).toString() + " but got " + ch);
	}

	private CharSequence read(char c) throws SyntaxError {
		if (c == charAt(buffer, pos)) {
			return buffer.subSequence(pos, pos++);
		} else {
			throw new SyntaxError(lineNumber(), column(), "Expected " + c + " but got " + ((char) charAt(buffer, pos)));
		}
	}

	private int charAt(CharSequence buffer, int pos) {
		if (buffer.length() <= pos)
			return EOF;
		return buffer.charAt(pos);
	}

	private int lineNumber() {
		int line_num = 0;
		for (int i = 0; i < pos; i++) {
			if ('\n' == charAt(buffer, i) || '\r' == charAt(buffer, i)) {
				line_num++;
				if (charAt(buffer, i) != charAt(buffer, i + 1)
						&& ('\n' == charAt(buffer, i + 1) || '\r' == charAt(buffer, i + 1)))
					i++;
			}
		}
		return line_num;
	}

	private int column() {
		int line_start = 0;
		for (int i = 0; i < pos; i++) {
			if ('\n' == charAt(buffer, i) || '\r' == charAt(buffer, i)) {
				if (charAt(buffer, i) != charAt(buffer, i + 1)
						&& ('\n' == charAt(buffer, i + 1) || '\r' == charAt(buffer, i + 1)))
					i++;
				line_start = i;
			}
		}
		return pos - line_start;
	}

}
