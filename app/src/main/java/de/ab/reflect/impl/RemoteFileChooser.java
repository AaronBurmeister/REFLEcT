package de.ab.reflect.impl;

import android.content.Context;

import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ab.dialog.FileChooser;
import de.ab.reflect.ssh.SSHSession;

public class RemoteFileChooser extends FileChooser {

	private SSHSession session;

	public RemoteFileChooser(Context context, SSHSession session) {
		super(context);
		this.session = session;
	}

	public RemoteFileChooser(Context context, int themeResId, SSHSession session) {
		super(context, themeResId);
		this.session = session;
	}

	public RemoteFileChooser(Context context, boolean cancelable, OnCancelListener cancelListener, SSHSession session) {
		super(context, cancelable, cancelListener);
		this.session = session;
	}

	@Override
	protected Element getHomeDir() throws FileNotFoundException {
		try {
			return new SftpElement(session.getHomeDir());
		} catch (SftpException e) {
			throw new FileNotFoundException(e.getMessage());
		}
	}

	@Override
	protected Element getElementForPath(String path) {
		return new SftpElement(path);
	}

	private class SftpElement implements Element {
		private File file;

		public SftpElement(String path) {
			file = new File(path);
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public String getPath() {
			try {
				return session.getFinalPath(file.getPath());
			} catch (SftpException e) {
				return file.getPath();
			}
		}

		@Override
		public Element getParent() {
			String parent = file.getParent();
			return parent == null ? this : new SftpElement(parent);
		}

		@Override
		public Element[] listChildren() {
			List<String> children = new ArrayList<>();
			try {
				Collections.addAll(children, session.listFiles(getPath()));
			} catch (Throwable t) {
			}
			Collections.sort(children, String.CASE_INSENSITIVE_ORDER);
			Element[] elements = new Element[children.size()];
			for (int i = 0; i < elements.length; i++)
				elements[i] = new SftpElement(new File(getPath(), children.get(i)).getPath());
			return elements;
		}

		@Override
		public Element getChild(String fileName) {
			return new SftpElement(new File(getPath(), fileName).getPath());
		}

		@Override
		public boolean exists() {
			try {
				return session.exists(getPath());
			} catch (SftpException e) {
				return false;
			}
		}

		@Override
		public boolean isDir() {
			return session.isDir(file.getPath());
		}

		@Override
		public boolean createDirectory() {
			try {
				session.mkdir(file.getPath());
				return true;
			} catch (SftpException e) {
				return false;
			}
		}
	}
}
