package net.lynxlee.toolsstudio.sftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

public class SFTPClient {

	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(SFTPClient.class);

	private String blockFlag = "blockFlag";

	/**
	 * 连接sftp服务器
	 * 
	 * @param host
	 *            主机
	 * @param port
	 *            端口
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return
	 * @throws JSchException
	 */
	// public ChannelSftp connect(String host, int port, String username, String
	// password) throws JSchException {
	// ChannelSftp sftp = null;
	//
	// JSch jsch = new JSch();
	// // jsch.getSession(username, host, port);
	// Session sshSession = jsch.getSession(username, host, port);
	// System.out.println("Session created.");
	// sshSession.setPassword(password);
	// Properties sshConfig = new Properties();
	// sshConfig.put("StrictHostKeyChecking", "no");
	// sshSession.setConfig(sshConfig);
	//
	// //
	// sshSession.setTimeout(Integer.parseInt(StaticInitData.getFtpConnectTimeOut())
	// // * NumberConstant.INT_1000);
	//
	// sshSession.connect();
	// System.out.println("Session connected.");
	// System.out.println("Opening Channel.");
	//
	// //sshSession.isConnected()
	//
	// Channel channel = sshSession.openChannel("sftp");
	// channel.connect();
	// sftp = (ChannelSftp) channel;
	// System.out.println("Connected to " + host + ".");
	//
	// return sftp;
	// }

	/**
	 * 连接sftp服务器
	 * 
	 * @param host
	 *            主机
	 * @param port
	 *            端口
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return Session
	 * @throws JSchException
	 */
	public Session connect(String host, int port, String username, String password) throws JSchException {
		JSch jsch = new JSch();
		Session sshSession = jsch.getSession(username, host, port);
		// System.out.println("Session created.");
		sshSession.setPassword(password);
		Properties sshConfig = new Properties();
		sshConfig.put("StrictHostKeyChecking", "no");
		sshSession.setConfig(sshConfig);
		sshSession.connect();
		// System.out.println("Session connected.");
		// System.out.println("Opening Channel.");
		System.out.println("Connected to " + host + ".");
		return sshSession;
	}

	/**
	 * 获取sftp的传输通道
	 * 
	 * @param sshSession
	 *            连接会话对象
	 * @return sftp的传输通道对象
	 * @throws JSchException
	 */
	public ChannelSftp getChannelSftp(Session sshSession) throws JSchException {
		if (sshSession.isConnected()) {
			ChannelSftp sftp = null;
			Channel channel = sshSession.openChannel("sftp");
			channel.connect();
			sftp = (ChannelSftp) channel;
			return sftp;
		} else {
			throw new JSchException("sftp连接未打开!");
		}
	}

	/**
	 * 上传文件
	 * 
	 * @param directory
	 *            上传的目录
	 * @param uploadFile
	 *            要上传的文件
	 * @param sftp
	 * @throws SftpException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void upload(String directory, String uploadFile, ChannelSftp sftp)
			throws SftpException, InterruptedException, IOException {
		synchronized (blockFlag) {
			try {
				// 缺少创建远端目录的代码实现
				sftp.cd(directory);
				File file = new File(uploadFile);
				sftp.put(new FileInputStream(file), file.getName());
				blockFlag.wait();
			} catch (SftpException e) {
				throw new SftpException((int) Thread.currentThread().getId(), directory + uploadFile, e);
			} catch (IOException e) {
				throw new IOException("创建本地文件[" + directory + uploadFile + "]失败!", e);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new InterruptedException("[" + Thread.currentThread().getName() + "]线程异常被中断!");
			}
		}
	}

	/**
	 * 下载文件
	 * 
	 * @param directory
	 *            下载目录
	 * @param downloadFile
	 *            下载的文件
	 * @param saveFile
	 *            存在本地的路径
	 * @param sftp
	 * @throws IOException
	 * @throws SftpException
	 * @throws InterruptedException
	 */
	public void download(String directory, String downloadFile, String saveFile, ChannelSftp sftp)
			throws IOException, SftpException, InterruptedException {
		synchronized (blockFlag) {
			try {
				sftp.cd(directory);
				File file = new File(saveFile);
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					file.createNewFile();
				}
				sftp.get(downloadFile, new FileOutputStream(file), new SFTPProgressMonitor(file.getName()));
				blockFlag.wait();
			} catch (SftpException e) {
				throw new SftpException((int) Thread.currentThread().getId(), downloadFile, e);
			} catch (IOException e) {
				throw new IOException("创建本地文件[" + downloadFile + "]失败!", e);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new InterruptedException("[" + Thread.currentThread().getName() + "]线程异常被中断!");
			}
		}
	}

	/**
	 * 下载文件
	 * 
	 * @param downloadFile
	 *            下载的文件
	 * @param saveFile
	 *            存在本地的路径
	 * @param sftp
	 * @throws IOException
	 * @throws SftpException
	 * @throws InterruptedException
	 */
	public void download(String downloadFile, String saveFile, ChannelSftp sftp)
			throws IOException, SftpException, InterruptedException {
		synchronized (blockFlag) {
			try {
				File file = new File(saveFile);
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					file.createNewFile();
				}
				sftp.get(downloadFile, new FileOutputStream(file), new SFTPProgressMonitor(file.getName()));
				blockFlag.wait();
			} catch (SftpException e) {
				blockFlag.notify();
				throw new SftpException((int) Thread.currentThread().getId(), downloadFile, e);
			} catch (IOException e) {
				throw new IOException("创建本地文件[" + downloadFile + "]失败!", e);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new InterruptedException("[" + Thread.currentThread().getName() + "]线程异常被中断!");
			}
		}
	}

	/**
	 * 删除文件
	 * 
	 * @param directory
	 *            要删除文件所在目录
	 * @param deleteFile
	 *            要删除的文件
	 * @param sftp
	 * @throws SftpException
	 */
	public void delete(String directory, String deleteFile, ChannelSftp sftp) throws SftpException {
		// synchronized (blockFlag) {
		try {
			sftp.cd(directory);
			sftp.rm(deleteFile);
			// blockFlag.wait();
		} catch (SftpException e) {
			throw new SftpException((int) Thread.currentThread().getId(), directory + deleteFile, e);
		}
		// }
	}

	/**
	 * 
	 * @param deleteFile
	 * @param sftp
	 * @throws SftpException
	 */
	public void delete(String deleteFile, ChannelSftp sftp) throws SftpException {
		// synchronized (blockFlag) {
		try {
			sftp.rm(deleteFile);
			// blockFlag.wait();
		} catch (SftpException e) {
			throw new SftpException((int) Thread.currentThread().getId(), deleteFile, e);
		}
	}

	/**
	 * 列出目录下的文件
	 * 
	 * @param directory
	 *            要列出的目录
	 * @param sftp
	 * @return
	 * @throws SftpException
	 */
	@SuppressWarnings("unchecked")
	public Vector<LsEntry> listFiles(String directory, ChannelSftp sftp) throws SftpException {
		return sftp.ls(directory);
	}

	/**
	 * sftp传输监控类
	 * 
	 * @author air
	 *
	 */
	public class SFTPProgressMonitor implements SftpProgressMonitor {

		// private long transfered;

		@SuppressWarnings("unused")
		private String fileName;

		public SFTPProgressMonitor(String fileName) {
			this.fileName = fileName;
		}

		public boolean count(long arg0) {
			// transfered = transfered + arg0;
			// System.out.println("Currently transferred total size: " +
			// transfered + " bytes");
			// System.out.print(".");
			return true;
		}

		public void end() {
			blockFlag.notify();
			// System.out.println("Transferring done.");
			// System.out.print("done\n");
		}

		public void init(int arg0, String arg1, String arg2, long arg3) {
			// synchronized (blockFlag) {
			// try {
			// System.out.print("[" + this.fileName + "]");
			// System.out.println("Transferring begin.");
			// blockFlag.wait();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
		}

	}
}
