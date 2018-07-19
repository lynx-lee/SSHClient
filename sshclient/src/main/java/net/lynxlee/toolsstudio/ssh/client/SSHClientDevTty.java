package net.lynxlee.toolsstudio.ssh.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;
import net.lynxlee.toolsstudio.ssh.util.Util;

public class SSHClientDevTty {

	private static Logger logger = LoggerFactory.getLogger(SSHClientDevTty.class);

	// private static Logger logger = Logger.getLogger("SSHClientDevTty");

	private static String knownHostPath = "config/known_hosts";
	private static String idDSAPath = "config/id_dsa";
	private static String idRSAPath = "config/id_rsa";

	public SSHClientDevTty() {
		knownHostPath = ConfigHelper.getInstance().getPropertie("ssh.knownHostPath");
		idDSAPath = ConfigHelper.getInstance().getPropertie("ssh.idDSAPath");
		idRSAPath = ConfigHelper.getInstance().getPropertie("ssh.idRSAPath");
	}

	/**
	 * This ServerHostKeyVerifier asks the user on how to proceed if a key cannot be
	 * found in the in-memory database.
	 * 
	 * @author air
	 *
	 */
	class AdvancedVerifier implements ServerHostKeyVerifier {
		private KnownHosts database = null;

		public AdvancedVerifier(KnownHosts database) {
			this.database = database;
		}

		public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm,
				byte[] serverHostKey) throws Exception {

			/* Check database */

			int result = this.database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

			if (result == KnownHosts.HOSTKEY_IS_OK) {
				return true;
			}

			/* Be really paranoid. We use a hashed hostname entry */

			String hashedHostname = KnownHosts.createHashedHostname(hostname);

			/* Add the hostkey to the in-memory database */

			this.database.addHostkey(new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);

			/* Also try to add the key to a known_host file */

			try {
				KnownHosts.addHostkeyToFile(new File(knownHostPath), new String[] { hashedHostname },
						serverHostKeyAlgorithm, serverHostKey);
			} catch (IOException ignore) {
				if (logger.isErrorEnabled()) {
					logger.error("添加[known_hosts]文件失败！", ignore);
				}
			}
			return true;

		}
	}

	/**
	 * 关闭会话
	 */
	public void closeSession(Session session) {
		// synchronized (this) {
		if (null != session) {
			session.close();
			session = null;
		}
		// }
	}

	/**
	 * 关闭连接
	 */
	public void closeConnection(Connection connection) {
		// synchronized (this) {
		if (null != connection) {
			connection.close();
			connection = null;
		}
		// }
	}

	/**
	 * 从known_hosts文件中读取Hostkeys信息
	 * 
	 * @param database
	 */
	private void addHostkeys(KnownHosts database) {
		File knownHostFile = new File(knownHostPath);
		if (knownHostFile.exists()) {
			try {
				database.addHostkeys(knownHostFile);
			} catch (IOException e) {
				if (logger.isErrorEnabled()) {
					logger.error("读取[known_hosts]文件失败!", e);
				}
			}
		}
	}

	/**
	 * 连接服务器
	 * 
	 * @param host
	 *            地址
	 * @param port
	 *            端口
	 * @return 连接对象
	 * @throws IOException
	 */
	public Connection connectServer(String host, int port) throws IOException {
		return this.connectServer(host, port, 3000, 0);
	}

	/**
	 * 连接服务器
	 * 
	 * @param host
	 *            地址
	 * @param port
	 *            端口
	 * @param connectTimeout
	 *            连接超时时间 单位毫秒
	 * @param kexTimeout
	 *            连接持续时间 单位毫秒
	 * @return 连接对象
	 * @throws IOException
	 */
	public Connection connectServer(String host, int port, int connectTimeout, int kexTimeout) throws IOException {
		try {
			Connection conn = new Connection(host, port);
			// conn.connect(null, connectTimeout, kexTimeout);
			KnownHosts database = new KnownHosts();

			this.addHostkeys(database);

			/*
			 * 
			 * CONNECT AND VERIFY SERVER HOST KEY (with callback)
			 * 
			 */

			String[] hostkeyAlgos = database.getPreferredServerHostkeyAlgorithmOrder(host);

			if (hostkeyAlgos != null) {
				conn.setServerHostKeyAlgorithms(hostkeyAlgos);
			}
			conn.connect(new AdvancedVerifier(database), connectTimeout, kexTimeout);

			return conn;
		} catch (IOException e) {
			throw new IOException("创建服务器连接失败!", e);
		}
	}

	/**
	 * 登录服务器
	 * 
	 * @param connection
	 *            服务器连接对象
	 * @param user
	 *            用户名
	 * @param password
	 *            密码
	 * @param charset
	 *            字符集
	 * @param x_width
	 *            屏幕字符换行宽度
	 * @param y_height
	 *            屏幕字符高度
	 * @return 会话对象
	 * @throws IOException
	 */
	public Session login(Connection connection, String user, String password, String charset, int x_width, int y_height)
			throws IOException {
		/*
		 * 
		 * AUTHENTICATION PHASE
		 * 
		 */
		boolean enableDSA = false;
		boolean enableRSA = false;
		boolean enablePWD = false;
		try {
			if (null != connection) {
				if (connection.isAuthMethodAvailable(user, "publickey")) {
					File key = new File(idDSAPath);
					if (key.exists()) {
						if (connection.authenticateWithPublicKey(user, key, password)) {
							enableDSA = true;
						} else {
							enableDSA = false;
						}
					} else {
						enableDSA = false;
					}
					if (!enableDSA) {
						key = new File(idRSAPath);
						if (key.exists()) {
							if (connection.authenticateWithPublicKey(user, key, password)) {
								enableRSA = true;
							} else {
								enableRSA = false;
							}
						}
					}
				}
				if (!enableDSA && !enableRSA) {
					if (connection.isAuthMethodAvailable(user, "password")) {
						if (connection.authenticateWithPassword(user, password)) {
							enablePWD = true;
						} else {
							enablePWD = false;
						}
					}
				}

				if (!enableDSA && !enableRSA && !enablePWD) {
					return null;
				} else {
					x_width = x_width == 0 ? 900 : x_width;
					y_height = y_height == 0 ? 300 : y_height;
					Session session = connection.openSession();
					session.requestPTY("dumb", x_width, y_height, 0, 0, null);
					session.startShell();
					return session;
				}
			} else {
				throw new NullPointerException("连接对象[Connection]为空!");
			}
		} catch (IOException e) {
			throw new IOException("No supported authentication methods available.", e);
		}
	}

	/**
	 * 异步输出监控类
	 * 
	 * @author air
	 *
	 */
	public class SSHClientOutputObservable extends Observable implements Runnable {

		Session session = null;
		String charset = null;

		public SSHClientOutputObservable(Session session, String charset) {
			this.session = session;
			this.charset = charset;
		}

		public void run() {
			if (null != session) {
				try {
					// 获取错误信息
					InputStream stderr = session.getStderr();
					// 获取标准输出
					InputStream stdout = session.getStdout();

					BufferedReader br = new BufferedReader(new InputStreamReader(stdout, charset));
					BufferedReader ebr = new BufferedReader(new InputStreamReader(stderr, charset));
					int _x = 0;
					StringBuffer _sb = new StringBuffer();
					do {
						// System.out.print((char)_x);
						if ((char) _x == '\n' || (char) _x == 32) {
							_sb.append((char) _x);
							this.setChanged();
							this.notifyObservers(Util.toCharset(_sb.toString(), charset));
							_sb = new StringBuffer();
						} else if ((char) _x == '\r') {
							continue;
						} else {
							_sb.append((char) _x);
						}
					} while ((_x = br.read()) > 0 || (_x = ebr.read()) > 0);

				} catch (IOException e) {
					if (logger.isErrorEnabled()) {
						logger.error("获取命令执行结果失败!", e);
					}
				}
			} else {
				// throw new NullPointerException("会话对象[Session]为空!");
				if (logger.isErrorEnabled()) {
					logger.error("会话对象[Session]为空!");
				}
			}
		}

	}

	/**
	 * 
	 * @param session
	 * @return
	 */
	public OutputStream getOutputStream(Session session) {
		if (null != session) {
			try {
				OutputStream outs = session.getStdin();
				return outs;
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error("获取输出流失败!", e);
				}
			}
		}
		return null;

	}

	/**
	 * 
	 * @param outs
	 */
	public void closeOutputStream(OutputStream outs) {
		if (null != outs) {
			try {
				outs.flush();
				outs.close();
			} catch (IOException e) {
				if (logger.isErrorEnabled()) {
					logger.error("关闭输出流失败!", e);
				}
			}
		}
	}

	/**
	 * 执行shell
	 * 
	 * @param execCommand
	 *            需要执行的shell
	 * @return 执行结果
	 * @throws IOException
	 */

	public void execProc(Session session, String execCommand, String charset) {
		if (null != session) {

			OutputStream outs = this.getOutputStream(session);
			execCommand = execCommand.trim();
			try {
				outs.write(execCommand.trim().getBytes());
				outs.write("\n".getBytes());
			} catch (IOException e) {
				if (logger.isErrorEnabled()) {
					logger.error("写输出流失败！", e);
				}
			} finally {
				try {
					outs.flush();
				} catch (IOException e) {
					if (logger.isErrorEnabled()) {
						logger.error("写输出流失败！", e);
					}
				}
			}
		} else {
			// throw new NullPointerException("会话对象[Session]为空!");
			if (logger.isErrorEnabled()) {
				logger.error("会话对象[Session]为空!");
			}
		}
	}
}
