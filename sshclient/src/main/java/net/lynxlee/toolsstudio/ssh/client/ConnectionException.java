package net.lynxlee.toolsstudio.ssh.client;

import java.io.IOException;

public class ConnectionException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConnectionException(String str) {
		super(str);
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

}
