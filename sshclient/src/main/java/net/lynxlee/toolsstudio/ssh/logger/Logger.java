package net.lynxlee.toolsstudio.ssh.logger;

public class Logger extends java.util.logging.Logger {

	protected Logger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	public boolean isDebugEnabled() {
		return false;

	}

	public void debug(String msg) {

	}

	public void debug(String format, Object... arguments) {

	}

	public void debug(String msg, Throwable t) {

	}

	public boolean isInfoEnabled() {
		return this.getLevel() == this.getLevel().INFO ? true : false;

	}

	public void info(String msg) {

	}

	public void info(String format, Object... arguments) {

	}

	public void info(String msg, Throwable t) {

	}

	public boolean isErrorEnabled() {
		return false;

	}

	public void error(String msg) {

	}

	public void error(String format, Object... arguments) {

	}

	public void error(String msg, Throwable t) {

	}

}
