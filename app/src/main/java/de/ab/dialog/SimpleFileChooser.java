package de.ab.dialog;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleFileChooser extends FileChooser {

	public SimpleFileChooser(Context context) {
		super(context);
	}

	public SimpleFileChooser(Context context, int themeResId) {
		super(context, themeResId);
	}

	public SimpleFileChooser(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	@Override
	protected Element getHomeDir() throws FileNotFoundException {
		return new FileElement(Environment.getExternalStorageDirectory().getPath());
	}

	@Override
	protected Element getElementForPath(String path) {
		return new FileElement(path);
	}

	private class FileElement implements Element {
		private File file;

		public FileElement(String path) {
			file = new File(path);
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public String getPath() {
			return file.getAbsolutePath();
		}

		@Override
		public Element getParent() {
			String parent = file.getParent();
			return parent == null ? this : new FileElement(parent);
		}

		@Override
		public Element[] listChildren() {
			List<String> children = new ArrayList<>();
			Collections.addAll(children, file.list());
			Collections.sort(children, String.CASE_INSENSITIVE_ORDER);
			Element[] elements = new Element[children.size()];
			for (int i = 0; i < elements.length; i++)
				elements[i] = new FileElement(new File(getPath(), children.get(i)).getPath());
			return elements;
		}

		@Override
		public Element getChild(String fileName) {
			return new FileElement(new File(getPath(), fileName).getPath());
		}

		@Override
		public boolean exists() {
			return file.exists();
		}

		@Override
		public boolean isDir() {
			return file.isDirectory();
		}

		@Override
		public boolean createDirectory() {
			return file.mkdirs();
		}
	}

}
