package net.lynxlee.toolsstudio.ssh.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.air.svnSyncTools.conf.config.ConfigHelper;
import com.air.svnSyncTools.util.Util;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPInputStream;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;

public class SSHClientDevNull {

	private static Log log = LogFactory.getLog(SSHClientDevNull.class);

	private static String knownHostPath = "config/known_hosts";
	private static String idDSAPath = "config/id_dsa";
	private static String idRSAPath = "config/id_rsa";

	public SSHClientDevNull() {
		knownHostPath = ConfigHelper.getInstance().getPropertie("ssh.knownHostPath");
		idDSAPath = ConfigHelper.getInstance().getPropertie("ssh.idDSAPath");
		idRSAPath = ConfigHelper.getInstance().getPropertie("ssh.idRSAPath");
	}

	/**
	 * This ServerHostKeyVerifier asks the user on how to proceed if a key
	 * cannot be found in the in-memory database.
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
				log.error(ignore);
			}
			return true;

		}
	}

	/**
	 * 关闭会话
	 */
	public void closeSession(Session session) {
		if (null != session) {
			session.close();
			session = null;
		}
	}

	/**
	 * 关闭连接
	 */
	public void closeConnection(Connection connection) {
		if (null != connection) {
			connection.close();
			connection = null;
		}
	}

	/**
	 * 
	 * @param database
	 */
	private void addHostkeys(KnownHosts database) {
		File knownHostFile = new File(knownHostPath);
		if (knownHostFile.exists()) {
			try {
				database.addHostkeys(knownHostFile);
			} catch (IOException e) {
				log.error(e);
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
	public Connection connectServer(String host, int port, String user, String password) throws IOException {
		return this.connectServer(host, port, user, password, 0, 0);
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
	public Connection connectServer(String host, int port, String user, String password, int connectTimeout,
			int kexTimeout) throws IOException {
		/*
		 * 
		 * AUTHENTICATION PHASE
		 * 
		 */
		boolean enableDSA = false;
		boolean enableRSA = false;
		boolean enablePWD = false;
		boolean authMethod = false;
		try {
			Connection connection = new Connection(host, port);

			connection.addConnectionMonitor(new ConnectionMonitorX());

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
				connection.setServerHostKeyAlgorithms(hostkeyAlgos);
			}
			connection.connect(new AdvancedVerifier(database), connectTimeout, kexTimeout);
			if (!connection.isAuthenticationComplete()) {
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
			} else {
				authMethod = true;
			}
			if (!enableDSA && !enableRSA && !enablePWD && !authMethod) {
				throw new IOException("服务器用户名或密码或证书校验失败,创建服务器连接失败!");
			} else {
				log.info("[" + connection.getConnectionInfo() + "]连接创建成功!");
				return connection;
			}
		} catch (IOException e) {
			throw new IOException("创建服务器连接失败!", e);
		}
	}

	/**
	 * 获得scp的执行对象
	 * 
	 * @param connection
	 *            连接对象
	 * @param user
	 *            用户名
	 * @param password
	 *            密码
	 * @return scp的执行对象
	 * @throws IOException
	 */
	public SCPClient getSCPClient(Connection connection, String user, String password) throws IOException {
		try {
			if (null != connection) {
				SCPClient scpClient = connection.createSCPClient();
				return scpClient;
				// scpClient.get("");
			} else {
				throw new NullPointerException("连接对象[Connection]为空!");
			}
		} catch (IOException e) {
			if (e.getMessage().equals("Sorry, this connection is closed.")) {
				// log.error("Sorry, this connection is closed.", e);
				throw new ConnectionException("Sorry, this connection is closed.", e);
				// String host = connection.getHostname();
				// int port = connection.getPort();
				// connection = null;
				// connection = this.connectServer(host, port, user, password);
				// return this.getSCPClient(connection, user, password);
			}
			throw new IOException("No supported authentication methods available.", e);
		}
	}

	/**
	 * 获取连接会话对象
	 * 
	 * @param connection
	 *            连接对象
	 * @param user
	 *            用户名
	 * @param password
	 *            密码
	 * @return 连接会话对象
	 * @throws IOException
	 */
	public Session getSession(Connection connection, String user, String password) throws IOException {
		try {
			if (null != connection) {
				Session session = connection.openSession();
				return session;

			} else {
				throw new NullPointerException("连接对象[Connection]为空!");
			}
		} catch (IOException e) {
			if (e.getMessage().equals("Sorry, this connection is closed.")) {
				throw new ConnectionException("Sorry, this connection is closed.", e);
			}
			throw new IOException("No supported authentication methods available.", e);
		}
	}

	/**
	 * 通过scp命令下载文件
	 * 
	 * @param scpClient
	 *            scp的执行对象
	 * @param remoteFile
	 *            远端全路径文件名
	 * @param localFile
	 *            本地全路径文件名
	 * @return
	 * @throws IOException
	 */
	public boolean downloadFileByScp(SCPClient scpClient, String remoteFile, String localFile) throws IOException {
		if (null != scpClient) {
			// ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
			FileOutputStream fileOps = null;
			SCPInputStream scpIps = null;
			if (null == remoteFile && "".equals(remoteFile)) {
				throw new NullPointerException("远程文件为空!");
			}

			if (null == localFile && "".equals(localFile)) {
				throw new NullPointerException("本地文件为空!");
			}
			try {
				byte[] buffer = new byte[1024];
				int len = -1;
				File file = new File(localFile);
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					file.createNewFile();
				}
				fileOps = new FileOutputStream(file);
				// scpClient.setCharset(localFile);
				scpIps = scpClient.get(remoteFile);
				while ((len = scpIps.read(buffer)) != -1) {
					fileOps.write(buffer, 0, len);
					// outSteam.write(buffer, 0, len);
				}
				return true;
			} finally {
				if (null != scpIps) {
					scpIps.close();
				}
				if (null != fileOps) {
					fileOps.flush();
					fileOps.close();
				}
			}
		} else {
			throw new NullPointerException("安全拷贝对象[SCPClient]为空!");
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

	public String execProc(Session session, String execCommand, String charset) throws IOException {
		if (null != session) {
			try {
				session.execCommand(execCommand.trim());
				// 获取错误信息
				InputStream stderr = session.getStderr();
				// 获取标准输出
				InputStream stdout = session.getStdout();

				BufferedReader br = new BufferedReader(new InputStreamReader(stdout, charset));
				BufferedReader ebr = new BufferedReader(new InputStreamReader(stderr, charset));
				String str = null;
				StringBuffer sb = new StringBuffer();
				// 获取标准输出或错误输出
				while (null != (str = br.readLine()) || null != (str = ebr.readLine())) {
					if ("".equals(str)) {
						continue;
					}
					if (!str.equals("null")) {
						sb.append(str + "\n");
					}
				}
				return Util.toCharset(sb.toString(), charset);
			} catch (IOException e) {
				throw new IOException("执行脚本失败!", e);
			} finally {
				// session.startShell();
			}
		} else {
			throw new NullPointerException("会话对象[Session]为空!");
		}
	}

	/**
	 * 判断连接是否连接
	 * 
	 * @param connection
	 * @return
	 */
	public boolean isClosed(Connection connection) {
		try {
			if (null != connection) {

				connection.openSession().close();
				return false;
			} else {
				return true;
			}
		} catch (IOException e) {
			if (e.getMessage().equals("Sorry, this connection is closed.")) {
				log.error("Sorry, this connection is closed.", e);
			}
			return true;
		}
	}

	private class ConnectionMonitorX implements ConnectionMonitor {

		public void connectionLost(Throwable arg0) {
			
			

		}

	}
}
