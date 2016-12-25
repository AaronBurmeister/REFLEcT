package de.ab.reflect.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import de.ab.reflect.console.Console;
import de.ab.util.Configurable;
import de.ab.util.StackedErrors;

import static de.ab.reflect.ssh.SSHConfiguration.HOST;
import static de.ab.reflect.ssh.SSHConfiguration.KNOWN_HOSTS_FILE;
import static de.ab.reflect.ssh.SSHConfiguration.PASSPHRASE;
import static de.ab.reflect.ssh.SSHConfiguration.PASSWORD;
import static de.ab.reflect.ssh.SSHConfiguration.PORT;
import static de.ab.reflect.ssh.SSHConfiguration.RSA_PRIVATE_KEY_FILE;
import static de.ab.reflect.ssh.SSHConfiguration.USER;

public class SSHSession implements Configurable<SSHConfiguration>, Console {

	public static final int
			ERROR_CONSOLE = -1,
			ERROR_CLOSE = -2;
	private StackedErrors.Impl consoleErrorStack = new StackedErrors.Impl();
	private SSHConfiguration config;
	private Session session;
	private ChannelExec channelExec;
	private ChannelSftp channelSftp;
	private Thread thRead;
	private Console console;
	private InputStream is;
	private OutputStream os;

	public SSHSession(SSHConfiguration config) throws JSchException {
		this.config = config;
		JSch.setConfig("StrictHostKeyChecking", "yes");
		JSch jsch = new JSch();
		if (config.containsKey(RSA_PRIVATE_KEY_FILE)) {
			jsch.setKnownHosts((String) config.get(KNOWN_HOSTS_FILE));
			if (config.containsKey(PASSPHRASE))
				jsch.addIdentity((String) config.get(RSA_PRIVATE_KEY_FILE), (String) config.get(PASSPHRASE));
			else
				jsch.addIdentity((String) config.get(RSA_PRIVATE_KEY_FILE));
		}
		session = jsch.getSession((String) config.get(USER), (String) config.get(HOST), (int) config.get(PORT, 22));
		if (config.containsKey(PASSWORD))
			session.setPassword((String) config.get(PASSWORD));
	}

	public void setConsole(Console console) {
		this.console = console;
	}

	public void connect(Console console) throws JSchException, IOException {
		this.console = console;
		conn(null);
	}

	public void connect(Console console, int timeout) throws JSchException, IOException {
		this.console = console;
		conn(timeout);
	}

	private void conn(Integer timeout) throws JSchException, IOException {
		if (timeout == null)
			session.connect();
		else
			session.connect(timeout);

		channelExec = (ChannelExec) session.openChannel("exec");
		channelExec.setCommand("/bin/bash 2>&1");
		is = channelExec.getInputStream();
		os = channelExec.getOutputStream();
		channelExec.connect();

		(thRead = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (channelExec.isConnected() && !channelExec.isEOF()) {
						try {
							int readByte;
							if ((readByte = is.read()) != 0xffffffff)
								console.write(readByte);
						} catch (Throwable t) {
							consoleErrorStack.onError(ERROR_CONSOLE, t);
						}
					}
					console.close();
				} catch (Throwable t) {
					consoleErrorStack.onError(ERROR_CLOSE, t);
				}
			}
		})).start();

		channelSftp = (ChannelSftp) session.openChannel("sftp");
		channelSftp.connect();
	}

	@Override
	public void write(int _byte) throws IOException {
		os.write(_byte);
		os.flush();
	}

	@Override
	public void write(byte... bytes) throws IOException {
		os.write(bytes);
		os.flush();
	}

	public void exec(String cmd) throws IOException {
		write((cmd + '\n').getBytes());
	}

	public String getHomeDir() throws SftpException {
		return channelSftp.getHome();
	}

	public boolean exists(String remotePath) throws SftpException {
		try {
			channelSftp.stat(remotePath);
			return true;
		} catch (SftpException e) {
			if (e.id == 2)
				return false;
			throw e;
		}
	}

	public boolean isDir(String remotePath) {
		SftpATTRS attrs;
		try {
			attrs = channelSftp.stat(remotePath);
			return attrs.isDir();
		} catch (Exception e) {
			return false;
		}
	}

	public void mkdir(String remotePath) throws SftpException {
		channelSftp.cd(remotePath);
	}
	
	public String[] listFiles(String remotePath) throws JSchException, SftpException {
		Vector data = channelSftp.ls(remotePath);
		String[] elements = new String[data.size()];
		for (int i = 0; i < elements.length; i++)
			elements[i] = ((ChannelSftp.LsEntry) data.get(i)).getFilename();
		return elements;
	}

	public String getFinalPath(String path) throws SftpException {
		return channelSftp.realpath(path);
	}

	public void pullFile(String remoteFilePath, String localFilePath) throws SftpException {
		channelSftp.get(remoteFilePath, localFilePath);
	}

	public void pushFile(String localFilePath, String remoteFilePath) throws SftpException {
		channelSftp.put(localFilePath, remoteFilePath);
	}

	public StackedErrors getConsoleErrorStack() {
		return consoleErrorStack;
	}

	@Override
	public SSHConfiguration getConfiguration() {
		return config;
	}

	public boolean isConnected() {
		return session.isConnected();
	}

	@Override
	public void close() {
		session.disconnect();

		if (thRead != null && thRead.isAlive())
			try {
				thRead.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public static class Builder {

		private SSHConfiguration config = new SSHConfiguration();

		public Builder(String host, int port, String user) {
			config.put(HOST, host);
			config.put(PORT, port);
			config.put(USER, user);
		}

		public Builder setPassword(String password) {
			config.put(PASSWORD, password);
			return this;
		}

		public Builder setAuth(String rsaPrivKeyFile, String knownHostsFile) {
			config.put(RSA_PRIVATE_KEY_FILE, rsaPrivKeyFile);
			config.put(KNOWN_HOSTS_FILE, knownHostsFile);
			return this;
		}

		public Builder setAuth(String rsaPrivKeyFile, String passphrase, String knownHostsFile) {
			config.put(RSA_PRIVATE_KEY_FILE, rsaPrivKeyFile);
			config.put(PASSPHRASE, passphrase);
			config.put(KNOWN_HOSTS_FILE, knownHostsFile);
			return this;
		}

		public SSHSession create() throws JSchException {
			return new SSHSession(config);
		}

	}

}
