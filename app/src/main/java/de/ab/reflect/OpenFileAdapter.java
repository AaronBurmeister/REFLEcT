package de.ab.reflect;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import de.ab.dialog.FileChooser;
import de.ab.dialog.SimpleFileChooser;
import de.ab.dialog.VisualTask;
import de.ab.reflect.sftp.SftpCommand;
import de.ab.reflect.sftp.SftpErrors;
import de.ab.reflect.sftp.SftpTask;
import de.ab.reflect.ssh.SSHSession;
import de.ab.util.Mappable;
import java.util.*;

public class OpenFileAdapter extends BaseAdapter implements OnItemClickListener, Mappable<String, MenuItem> {

	private Context context;
	private SSHSession session;
	private LocalFiles files;

	private String[][] openFiles;

	public OpenFileAdapter(Context context, SSHSession session, LocalFiles files) {
		this.context = context;
		this.session = session;
		this.files = files;

		update();
	}

	public void update() {
		String[] openFileIdentifiers = files.listIdentifiers();
		openFiles = new String[openFileIdentifiers.length][3];
		for (int i = 0; i < openFileIdentifiers.length; i++) {
			openFiles[i][0] = openFileIdentifiers[i];
			try {
				openFiles[i][1] = files.getContentName(openFileIdentifiers[i]);
				openFiles[i][2] = files.getRemoteDir(openFileIdentifiers[i]).getPath();
			} catch (IOException e) {
				openFiles[i][1] = context.getResources().getString(R.string.unknown_file_name);
			}
		}
		Arrays.sort(openFiles, new Comparator<String[]>() {
				@Override
				public int compare(String[] a, String[] b)
				{
					return a[1].compareToIgnoreCase(b[1]);
				}
			});
		notifyDataSetChanged();
	}

	@Override
	public String[] getItem(int i) {
		return openFiles[i];
	}

	@Override
	public int getCount() {
		return openFiles.length;
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View convertView, ViewGroup viewGroup) {
		View view = convertView;
		if (view == null)
			view = LayoutInflater.from(context).inflate(R.layout.open_file_adapter_item, viewGroup, false);
		String[] data = getItem(i);
		view.setTag(data[0]);
		((TextView) view.findViewById(R.id.file_name)).setText(data[1]);
		((TextView) view.findViewById(R.id.dir_path)).setText(data[2]);
		EntryHandler.apply(context, this, view.findViewById(R.id.options), data[0]);
		return view;
	}

	@Override
	public void onItemClick(AdapterView<?> p1, View view, int position, long p4) {
		if (view.getTag() != null)
			open((String) view.getTag(), false);
	}

	@Override
	public boolean map(final String identifier, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.open_with:
				open(identifier, true);
				return true;
			case R.id.pull:
				try {
					new SftpTask(context, session).execute(
						SftpTask.transferFiles(SftpTask.DIRECTION_PULL, files.getRemoteFile(identifier).getPath(), files.getContentFile(identifier).getPath(), new VisualTask.Notifier<SftpCommand>() {
								@Override
								public void onSuccess(VisualTask<SftpCommand> task, SftpCommand command) {
									Toast.makeText(context, R.string.file_pulled, Toast.LENGTH_SHORT).show();
								}

								@Override
								public void onFailure(VisualTask<SftpCommand> task, SftpCommand command, Throwable cause) {
									String message = SftpErrors.getSftpErrorMessage(context, cause);
									Toast.makeText(context, message == null ? context.getString(R.string.sftp_error_pull_file) : message, Toast.LENGTH_SHORT).show();
								}
							}));
				} catch (IOException e) {
					Toast.makeText(context, R.string.sftp_error_pull_file, Toast.LENGTH_SHORT).show();
				}
				return true;
			case R.id.push:
				try {
					new SftpTask(context, session).execute(
							SftpTask.transferFiles(SftpTask.DIRECTION_PUSH, files.getContentFile(identifier).getPath(), files.getRemoteFile(identifier).getPath(), new VisualTask.Notifier<SftpCommand>() {
								@Override
								public void onSuccess(VisualTask<SftpCommand> task, SftpCommand command) {
									Toast.makeText(context, R.string.file_pushed, Toast.LENGTH_SHORT).show();
								}

								@Override
								public void onFailure(VisualTask<SftpCommand> task, SftpCommand command, Throwable cause) {
									String message = SftpErrors.getSftpErrorMessage(context, cause);
									Toast.makeText(context, message == null ? context.getString(R.string.sftp_error_push_file) : message, Toast.LENGTH_SHORT).show();
								}
							}));
				} catch (IOException e) {
					Toast.makeText(context, R.string.sftp_error_push_file, Toast.LENGTH_SHORT).show();
				}
				return true;
			case R.id.export:
				SimpleFileChooser chooser = new SimpleFileChooser(context);
				chooser.setMode(FileChooser.MODE_FILES | FileChooser.MODE_FILES_FLAG_CUSTOM_FILE_NAMES_ALLOWED | FileChooser.MODE_FILES_FLAG_WARN_IF_FILE_NAME_EXISTS);
				chooser.setOnFileSelectedListener(new FileChooser.OnFileSelectedListener() {
					@Override
					public void onFileSelected(FileChooser chooser, String resultPath) {
						FileChannel sourceChannel = null;
						FileChannel destChannel = null;
						try {
							sourceChannel = files.openInputStream(identifier).getChannel();
							destChannel = new FileOutputStream(new File(resultPath)).getChannel();
							destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
						} catch (IOException e) {
							Toast.makeText(context, R.string.error_export_file, Toast.LENGTH_SHORT).show();
						} finally {
							try {
								if (sourceChannel != null)
									sourceChannel.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							try {
								if (destChannel != null)
									destChannel.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				});
				chooser.show();
				return true;
			case R.id.close:
				new AlertDialog.Builder(context)
						.setTitle(R.string.close)
						.setMessage(R.string.do_you_really_want_to_close_file)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								if (files.closeFile(identifier))
									update();
								else
									Toast.makeText(context, R.string.error_close_file, Toast.LENGTH_SHORT).show();
							}
						})
						.setNegativeButton(R.string.cancel, null)
						.show();
				return true;
			default:
				return false;
		}
	}

	private void open(String identifier, boolean chooser) {
		try {
			Intent i = new Intent(Intent.ACTION_EDIT);
			i.setDataAndType(Uri.parse(files.getContentFile(identifier).toURI().toString()), "text/plain");
			if (chooser)
				i = Intent.createChooser(i, context.getString(R.string.open_with));
			context.startActivity(i);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, R.string.no_editor_installed, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(context, R.string.error_open_file, Toast.LENGTH_SHORT).show();
		}
	}

	public static class EntryHandler implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

		private Mappable<String, MenuItem> mappable;
		private String identifier;
		private PopupMenu menu;

		public EntryHandler(Context context, Mappable<String, MenuItem> mappable, View target, String identifier) {
			this.mappable = mappable;
			this.identifier = identifier;
			menu = new PopupMenu(context, target);
			menu.inflate(R.menu.open_file_adapter);
			menu.setOnMenuItemClickListener(this);
		}

		public static EntryHandler apply(Context context, Mappable<String, MenuItem> mappable, View target, String identifier) {
			EntryHandler handler = new EntryHandler(context, mappable, target, identifier);
			target.setOnClickListener(handler);
			return handler;
		}

		@Override
		public void onClick(View view) {
			menu.show();
		}

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			return mappable.map(identifier, item);
		}
	}

}
