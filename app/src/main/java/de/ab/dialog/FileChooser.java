package de.ab.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ab.fontawesome.FontAwesomeTextView;
import de.ab.reflect.R;
import de.ab.widget.LoaderView;

public abstract class FileChooser extends Dialog implements AdapterView.OnItemClickListener, LoaderView.Process<Object, FileChooser.Element[]>, View.OnClickListener {

	@FileChooserMode
	public static final int MODE_DIRECTORIES = 0x0;
	@FileChooserMode
	public static final int MODE_FILES = 0x1;
	@FileChooserMode
	public static final int MODE_FILES_FLAG_CUSTOM_FILE_NAMES_ALLOWED = 0x2;
	@FileChooserMode
	public static final int MODE_FILES_FLAG_WARN_IF_FILE_NAME_EXISTS = 0x4;
	private static final int COMP_MODE = 0x1;
	private static final int COMP_FILES_FLAG_CUSTOM_FILE_NAMES_ALLOWED = 0x3;
	private static final int COMP_FILES_FLAG_WARN_IF_FILE_NAME_EXISTS = 0x5;
	private LoaderView loaderView;
	private Element current;
	private TextView path;
	private EditText fileName;
	private DirectoryAdapter adapter;
	private OnFileSelectedListener onFileSelectedListener;
	private int mode = MODE_FILES;

	public FileChooser(Context context) {
		super(context);
	}

	public FileChooser(Context context, int themeResId) {
		super(context, themeResId);
	}

	public FileChooser(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	protected abstract Element getHomeDir() throws FileNotFoundException;

	protected abstract Element getElementForPath(String path);

	protected void createNewDirectory(final DirectoryCreationInterface i) {
		final EditText et = new EditText(getContext());
		et.setSingleLine();
		et.setMaxLines(1);
		et.setHorizontallyScrolling(true);
		new AlertDialog.Builder(getContext())
				.setTitle(R.string.new_directory)
				.setView(et)
				.setPositiveButton(R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int index) {
						String fileName = et.getText().toString().trim();

						if (fileName.length() > 0)
							if (i.exists(fileName))
								i.warnForExistingFileName(fileName);
							else
								i.onSuccess(fileName);
						else
							Toast.makeText(getContext(), R.string.nothing_entered, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	protected void warnForExistingFileName(String fileName, final ExistingFileNameWarningInterface i) {
		new AlertDialog.Builder(getContext())
				.setTitle(R.string.file_exists)
				.setMessage(getContext().getString(R.string.do_you_want_to_overwrite, fileName))
				.setPositiveButton(R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int index) {
						if (i != null)
							i.onSuccess();
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	protected View onCreateContentView(LayoutInflater inflater) {
		return inflater.inflate(R.layout.file_chooser, null, false);
	}

	protected View onCreateBaseView(LayoutInflater inflater) {
		LoaderView view = new LoaderView(getContext());
		view.setId(R.id.file_chooser_loader);
		return view;
	}

	protected View getItemView(ListAdapter adapter, int i, View convertView, ViewGroup parent, Element data) {
		View view = convertView;
		if (view == null)
			view = LayoutInflater.from(getContext()).inflate(R.layout.file_chooser_item, parent, false);
		FontAwesomeTextView icon = (FontAwesomeTextView) view.findViewById(R.id.icon);
		icon.setHtmlText(data == null ? null : getContext().getString(data.isDir() ? R.string.fa_folder : R.string.fa_file));
		TypedValue typedValue = new TypedValue();
		if (getContext().getTheme().resolveAttribute(data != null && data.isDir() ? (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? android.R.attr.colorAccent : android.R.attr.textColorHint) : android.R.attr.textColor, typedValue, true))
			icon.setTextColor(typedValue.data);
		else
			icon.setTextColor(Color.WHITE);
		((TextView) view.findViewById(R.id.name)).setText(data == null ? null : data.getName());
		return view;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			if (current == null)
				current = new ElementCache(getHomeDir());

			View baseView = onCreateBaseView(getLayoutInflater());
			loaderView = (LoaderView) baseView.findViewById(R.id.file_chooser_loader);
			loaderView.setContentView(onCreateContentView(getLayoutInflater()));

			path = (TextView) loaderView.getContentView().findViewById(R.id.file_chooser_path);

			fileName = (EditText) loaderView.getContentView().findViewById(R.id.file_chooser_filename);
			fileName.setEnabled((mode & COMP_FILES_FLAG_CUSTOM_FILE_NAMES_ALLOWED) == (MODE_FILES | MODE_FILES_FLAG_CUSTOM_FILE_NAMES_ALLOWED));
			fileName.setText((mode & COMP_MODE) == MODE_DIRECTORIES ? current.getName() : null);

			loaderView.getContentView().findViewById(R.id.file_chooser_up).setOnClickListener(this);
			loaderView.getContentView().findViewById(R.id.file_chooser_new_directory).setOnClickListener(this);
			loaderView.getContentView().findViewById(R.id.file_chooser_ok).setOnClickListener(this);

			ListView list = (ListView) loaderView.getContentView().findViewById(R.id.file_chooser_list);
			list.setAdapter(adapter = new DirectoryAdapter());
			list.setOnItemClickListener(this);

			setContentView(baseView);

			loaderView.load(this);
		} catch (FileNotFoundException e) {
			cancel();
		}
	}

	public void setMode(@FileChooserMode int mode) {
		this.mode = mode;
	}

	public void setOnFileSelectedListener(OnFileSelectedListener onFileSelectedListener) {
		this.onFileSelectedListener = onFileSelectedListener;
	}

	public void setPath(String path) {
		setPath(getElementForPath(path));
	}

	protected void setPath(Element path) {
		current = path;
		if (fileName != null)
			fileName.setText((mode & COMP_MODE) == MODE_DIRECTORIES ? current.getName() : null);
		if (loaderView != null)
			loaderView.load(this);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		Element data = adapter.getItem(i);
		if (data.isDir())
			setPath(data);
		else if (fileName != null && (mode & COMP_MODE) == MODE_FILES)
			fileName.setText(adapter.getItem(i).getName());
	}

	@Override
	public Element[] load(View contentView, Object[] params) throws Throwable {
		Element[] children = current.listChildren();
		Element[] cached = new Element[children.length];
		for (int i = 0; i < children.length; i++)
			cached[i] = new ElementCache(children[i]);
		return cached;
	}

	@Override
	public void finished(View contentView, Element[] data) {
		try {
			if (path != null)
				path.setText(current.getPath());
			if (fileName != null)
				fileName.setText((mode & COMP_MODE) == MODE_DIRECTORIES ? current.getName() : null);
			if (adapter != null)
				adapter.update(data);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void failed(View contentView, Throwable cause) {
		try {
			if (path != null)
				path.setText(current.getPath());
			if (fileName != null)
				fileName.setText(null);
			if (adapter != null)
				adapter.update();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.file_chooser_up:
				setPath(current.getParent());
				break;
			case R.id.file_chooser_new_directory:
				createNewDirectory(new DirectoryCreationInterface() {
					@Override
					public void onSuccess(String fileName) {
						current.getChild(fileName).createDirectory();
						setPath(current);
					}

					@Override
					public boolean exists(String fileName) {
						return current.getChild(fileName).exists();
					}

					@Override
					public void warnForExistingFileName(final String fileName) {
						FileChooser.this.warnForExistingFileName(fileName, new ExistingFileNameWarningInterface() {
							@Override
							public void onSuccess() {
								current.getChild(fileName).createDirectory();
								setPath(current);
							}

							@Override
							public void onCancel() {
							}
						});
					}
				});
				break;
			case R.id.file_chooser_ok:
				String fileName = this.fileName == null ? "" : this.fileName.getText().toString().trim();

				if (fileName.length() > 0) {
					final Element element = (mode & COMP_MODE) == MODE_DIRECTORIES ? current : current.getChild(fileName);
					if ((mode & COMP_FILES_FLAG_WARN_IF_FILE_NAME_EXISTS) == (mode | MODE_FILES_FLAG_WARN_IF_FILE_NAME_EXISTS) && element.exists()) {
						warnForExistingFileName(element.getName(), new ExistingFileNameWarningInterface() {
							@Override
							public void onSuccess() {
								if (onFileSelectedListener != null)
									onFileSelectedListener.onFileSelected(FileChooser.this, element.getPath());
								hide();
							}

							@Override
							public void onCancel() {
								hide();
							}
						});
					} else {
						if (onFileSelectedListener != null)
							onFileSelectedListener.onFileSelected(this, element.getPath());
						hide();
					}
				}
				break;
		}
	}

	@Target(value = {ElementType.FIELD, ElementType.PARAMETER})
	public @interface FileChooserMode {
	}

	public interface OnFileSelectedListener {
		void onFileSelected(FileChooser chooser, String resultPath);
	}

	public interface DirectoryCreationInterface {
		void onSuccess(String fileName);

		boolean exists(String fileName);

		void warnForExistingFileName(String fileName);
	}

	public interface ExistingFileNameWarningInterface {
		void onSuccess();

		void onCancel();
	}

	public interface Element {
		String getName();

		String getPath();

		Element getParent();

		Element[] listChildren();

		Element getChild(String fileName);

		boolean exists();

		boolean isDir();

		boolean createDirectory();
	}

	private class DirectoryAdapter extends BaseAdapter {

		private List<Element> data = new ArrayList<>();

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Element getItem(int i) {
			return getCount() > i ? data.get(i) : null;
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View convertView, ViewGroup viewGroup) {
			return getItemView(adapter, i, convertView, viewGroup, getItem(i));
		}

		public void update(Element... data) {
			try {
				this.data.clear();
				Collections.addAll(this.data, data);
				adapter.notifyDataSetChanged();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

	}

	private class ElementCache implements Element {

		private String name, path;
		private boolean exists, isDir;
		private Element parent;
		private Element src;

		public ElementCache(Element src) {
			name = src.getName();
			path = src.getPath();
			exists = src.exists();
			isDir = src.isDir();
			parent = src.getParent();
			this.src = src;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public FileChooser.Element getParent() {
			return parent;
		}

		@Override
		public FileChooser.Element[] listChildren() {
			return src.listChildren();
		}

		@Override
		public Element getChild(String fileName) {
			return src.getChild(fileName);
		}

		@Override
		public boolean exists() {
			return exists;
		}

		@Override
		public boolean isDir() {
			return isDir;
		}

		@Override
		public boolean createDirectory() {
			return src.createDirectory();
		}
	}

}
