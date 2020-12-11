package com.meerkattrading.tws;

public class SyntaxError extends Exception {
	private static final long serialVersionUID = -6545811434028080509L;
	private final int line;
	private final int column;

	public SyntaxError(int line, int column, String message) {
		super(message);
		this.line = line;
		this.column = column;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

}
