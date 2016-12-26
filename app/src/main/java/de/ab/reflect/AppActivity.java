package de.ab.reflect;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;

import de.ab.dialog.FileChooser;
import de.ab.dialog.VisualTask;
import de.ab.reflect.console.ConsoleView;
import de.ab.reflect.console.StatefulConsoleCtrlView;
import de.ab.reflect.impl.RemoteFileChooser;
import de.ab.reflect.sftp.SftpCommand;
import de.ab.reflect.sftp.SftpErrors;
import de.ab.reflect.sftp.SftpTask;
import de.ab.reflect.ssh.SSHConfiguration;
import de.ab.reflect.ssh.SSHSession;
import de.ab.widget.LoaderView;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class AppActivity extends Activity implements LoaderView.Process<Object, SSHSession>, StatefulConsoleCtrlView.OnRetryListener, ConsoleView.OnCloseListener, View.OnClickListener, PopupMenu.OnMenuItemClickListener {

	public static final String SSH_CONFIG = "sshConfig";
	private static final int TIMEOUT = 30000;
	private static final int REQUEST_PERMISSION = 1;
	private static final int OVERFLOW_LIMIT = 1000;
	private SSHSession session;
	private LoaderView loaderView;
	private PopupMenu openFileMenu;
	private OpenFileAdapter openFileAdapter;
	private LocalFiles files;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			initPermissions();
		else
			init();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void initPermissions() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
			init();
		else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			AlertDialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.storage_permission)
					.setMessage(R.string.storage_permission_message)
					.setCancelable(true)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.cancel();
						}
					}).create();
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					ActivityCompat.requestPermissions(AppActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
				}
			});
			dialog.show();
		} else
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_PERMISSION) {
			boolean grantedAll = true;
			for (int i : grantResults)
				if (i != PERMISSION_GRANTED)
					grantedAll = false;
			if (!grantedAll)
				initPermissions();
			else
				init();
		}
	}

	private void init() {
		loaderView = new LoaderView(this);
		setContentView(loaderView);
		loaderView.setContentView(R.layout.app);
		loaderView.load(this, getIntent());
	}

	@Override
	protected void onDestroy() {
		try {
			if (session != null && session.isConnected())
				session.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public SSHSession load(View contentView, Object[] params) throws JSchException, IOException {
		session = new SSHSession(getSSHConfiguration());
		ConsoleView consoleView = (ConsoleView) contentView.findViewById(R.id.stdout);
		consoleView.setOverflowLimit(OVERFLOW_LIMIT);
		consoleView.setOnCloseListener(this);
		session.connect(consoleView, TIMEOUT);
		return session;
	}

	@Override
	public void finished(View contentView, SSHSession session) {
		SSHConfiguration config = getSSHConfiguration();
		StatefulConsoleCtrlView v = (StatefulConsoleCtrlView) contentView.findViewById(R.id.stdin);
		v.setConsole(session);
		v.setOnRetryListener(this);
		v.setIsConnected();
		((TextView) findViewById(R.id.connection)).setText((String) config.get(SSHConfiguration.HOST) + ":" + config.get(SSHConfiguration.PORT));
		View openFileOptions = findViewById(R.id.open_file_options);
		openFileMenu = new PopupMenu(this, openFileOptions);
		openFileMenu.inflate(R.menu.app);
		openFileMenu.setOnMenuItemClickListener(this);
		openFileOptions.setOnClickListener(this);
		update(files);
		/*
		 OpenFileListener openFileListener = new OpenFileListener(files);
		 findViewById(R.id.open_file).setOnClickListener(openFileListener);
		 findViewById(R.id.select_file).setOnClickListener(openFileListener);
		 ListView lv = (ListView) findViewById(R.id.open_files);
		 lv.setAdapter(openFileAdapter = new OpenFileAdapter(this, session, files));
		 lv.setOnItemClickListener(openFileAdapter);
		 */
	}

	@Override
	public void failed(View contentView, Throwable cause) {
		AlertDialog d = new AlertDialog.Builder(this)
				.setTitle(getString(R.string.connection_failed, cause == null ? "null" : cause.getMessage()))
				.setMessage(R.string.do_you_want_to_retry)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface p1, int p2) {
						loaderView.load(AppActivity.this, getIntent());
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface i, int p2) {
						i.cancel();
					}
				})
				.setCancelable(false)
				.create();
		d.setCanceledOnTouchOutside(false);
		d.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface p1) {
				finish();
			}
		});
		d.show();
	}

	@Override
	public void onRetry(StatefulConsoleCtrlView view) {
		loaderView.load(this, getIntent());
	}

	@Override
	public void onClose(ConsoleView view) {
		((StatefulConsoleCtrlView) loaderView.getContentView().findViewById(R.id.stdin)).setIsLost();
	}

	private SSHConfiguration getSSHConfiguration() {
		return SSHConfiguration.deserialize(getIntent().getSerializableExtra(SSH_CONFIG));
	}

	private LocalFiles getLocalFiles() {
		SSHConfiguration sshConfig = getSSHConfiguration();
		return LocalFiles.get(this, (String) sshConfig.get(SSHConfiguration.HOST), (int) sshConfig.get(SSHConfiguration.PORT), LocalFiles.NO_PROJECT);
	}

	private LocalFiles newProject(String title, File remoteDir) {
		SSHConfiguration sshConfig = getSSHConfiguration();
		return LocalFiles.newProject(this, (String) sshConfig.get(SSHConfiguration.HOST), (int) sshConfig.get(SSHConfiguration.PORT), title, remoteDir);
	}

	private LocalFiles openProject(String identifier) {
		SSHConfiguration sshConfig = getSSHConfiguration();
		return LocalFiles.get(this, (String) sshConfig.get(SSHConfiguration.HOST), (int) sshConfig.get(SSHConfiguration.PORT), identifier);
	}

	private void update(LocalFiles newFiles) {
		files = newFiles;

		final ProjectView projectView = (ProjectView) findViewById(R.id.project);

		if (files == null) {
			update(getLocalFiles());
			return;
		}

		projectView.setVisibility(files.isProject() ? View.VISIBLE : View.GONE);
		if (files.isProject()) {
			try {
				final File projectRoot = files.getProjectRoot();

				projectView.setTitle(LocalFiles.getProjectTitle(projectRoot));
				projectView.setRemoteDir(LocalFiles.getProjectRemoteDir(projectRoot).toString());
				projectView.clearShortcuts();
				projectView.setProjectViewListener(new ProjectView.ProjectViewListener() {
					@Override
					public void onRequestClose() {
						update(getLocalFiles());
					}

					@Override
					public void onRequestEdit() {
						projectConfig(LocalFiles.getProject(projectRoot));
					}

					@Override
					public void onRequestAddShortcut(String faTag, CharSequence description, String command) {
						try {
							int itemId = projectView.getShortcutCount();
							LocalFiles.addShortcut(projectRoot, faTag, description.toString(), command);
							projectView.addShortcut(itemId, faTag, description, command);
						} catch (IOException e) {
							Toast.makeText(AppActivity.this, R.string.error_add_shortcut, Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void onRequestEditShortcut(int itemId, String faTag, CharSequence description, String command) {
						try {
							LocalFiles.updateShortcut(projectRoot, projectView.getShortcutIndex(itemId), faTag, description.toString(), command);
							projectView.updateShortcut(itemId, faTag, description, command);
						} catch (IOException e) {
							Toast.makeText(AppActivity.this, R.string.error_edit_shortcut, Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void onRequestMoveShortcut(int itemId, int steps) {
						try {
							int index = projectView.getShortcutIndex(itemId);
							if (index + steps >= 0 && index + steps < projectView.getShortcutCount()) {
								LocalFiles.moveShortcut(projectRoot, index, steps);
								projectView.moveShortcut(itemId, steps);
							}
						} catch (IOException e) {
							Toast.makeText(AppActivity.this, R.string.error_move_shortcut, Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void onRequestRemoveShortcut(final int itemId) {
						new AlertDialog.Builder(AppActivity.this)
								.setTitle(R.string.remove)
								.setMessage(R.string.do_you_really_want_to_remove_shortcut)
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										try {
											LocalFiles.removeShortcut(projectRoot, projectView.getShortcutIndex(itemId));
											projectView.removeShortcut(itemId);
										} catch (IOException e) {
											e.printStackTrace();
											Toast.makeText(AppActivity.this, R.string.error_remove_shortcut, Toast.LENGTH_SHORT).show();
										}
									}
								})
								.setNegativeButton(R.string.cancel, null)
								.show();
					}

					@Override
					public void onRequestRemove() {
						new AlertDialog.Builder(AppActivity.this)
								.setTitle(R.string.remove)
								.setMessage(R.string.do_you_really_want_to_remove_project)
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										if (LocalFiles.closeProject(projectRoot))
											update(getLocalFiles());
										else
											Toast.makeText(AppActivity.this, R.string.error_close_project, Toast.LENGTH_SHORT).show();
									}
								})
								.setNegativeButton(R.string.cancel, null)
								.show();
					}

					@Override
					public void onShortcutClick(View v, int itemId, String command) {
						try {
							View contentView;
							if (loaderView != null && (contentView = loaderView.getContentView()) != null)
								((DrawerLayout) contentView).closeDrawer(GravityCompat.START);
							session.exec(command);
						} catch (IOException e) {
							Toast.makeText(AppActivity.this, R.string.error_exec_command, Toast.LENGTH_SHORT).show();
						}
					}
				});

				for (String[] shortcut : LocalFiles.listShortcuts(projectRoot)) {
					int itemId = projectView.getShortcutCount();
					projectView.addShortcut(itemId, shortcut[0], shortcut[1], shortcut[2]);
				}
			} catch (Throwable t) {
				Toast.makeText(this, R.string.error_open_project, Toast.LENGTH_SHORT).show();
				update(getLocalFiles());
				return;
			}
		}

		TextView openFilePath = (TextView) findViewById(R.id.open_file_path);
		try {
			openFilePath.setText(files.isProject() ? LocalFiles.getProjectRemoteDir(files.getProjectRoot()).toString() : "");
		} catch (IOException e) {
			openFilePath.setText("");
		}
		OpenFileListener openFileListener = new OpenFileListener(files);
		findViewById(R.id.open_file).setOnClickListener(openFileListener);
		findViewById(R.id.select_file).setOnClickListener(openFileListener);

		ListView lv = (ListView) findViewById(R.id.open_files);
		lv.setAdapter(openFileAdapter = new OpenFileAdapter(this, session, files));
		lv.setOnItemClickListener(openFileAdapter);
	}

	@Override
	public void onClick(View view) {
		openFileMenu.show();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.new_project:
				projectConfig(null);
				return true;
			case R.id.open_project:
				SSHConfiguration sshConfig = getSSHConfiguration();
				final ProjectAdapter adapter = new ProjectAdapter(this, (String) sshConfig.get(SSHConfiguration.HOST), (int) sshConfig.get(SSHConfiguration.PORT));

				new AlertDialog.Builder(this)
						.setTitle(R.string.projects)
						.setAdapter(adapter, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								update(openProject(adapter.getItem(i)[0]));
							}
						})
						.show();
				return true;
			default:
				return false;
		}
	}

	private void projectConfig(final LocalFiles project) {
		final View v = getLayoutInflater().inflate(R.layout.app_new_project, null, false);

		v.findViewById(R.id.select_directory).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				RemoteFileChooser chooser = new RemoteFileChooser(AppActivity.this, session);
				chooser.setMode(FileChooser.MODE_DIRECTORIES);
				chooser.setOnFileSelectedListener(new FileChooser.OnFileSelectedListener() {
					@Override
					public void onFileSelected(FileChooser chooser, String resultPath) {
						((EditText) v.findViewById(R.id.directory)).setText(resultPath);
						EditText title = (EditText) v.findViewById(R.id.title);
						if (title.getText().toString().trim().length() == 0)
							title.setText(new File(resultPath).getName());
					}
				});
				chooser.show();
			}
		});

		if (project != null) {
			try {
				((EditText) v.findViewById(R.id.directory)).setText(LocalFiles.getProjectRemoteDir(project.getProjectRoot()).getPath());
				((EditText) v.findViewById(R.id.title)).setText(LocalFiles.getProjectTitle(project.getProjectRoot()));
			} catch (IOException e) {
				Toast.makeText(this, R.string.error_edit_project, Toast.LENGTH_SHORT).show();
				return;
			}
		}

		new AlertDialog.Builder(this)
				.setTitle(project == null ? R.string.new_project : R.string.edit_project)
				.setView(v)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface p1, int p2) {
						String path = ((EditText) v.findViewById(R.id.directory)).getText().toString().trim();
						String title = ((EditText) v.findViewById(R.id.title)).getText().toString().trim();

						if (path.length() == 0 || title.length() == 0) {
							Toast.makeText(AppActivity.this, R.string.nothing_entered, Toast.LENGTH_SHORT).show();
							return;
						}

						LocalFiles _project;
						try {
							if (project == null)
								_project = newProject(title, new File(path));
							else
								LocalFiles.putProjectMapping((_project = project).getProjectRoot(), title, path);
							if (_project != null)
								update(_project);
							else
								Toast.makeText(AppActivity.this, project == null ? R.string.error_open_project : R.string.error_edit_project, Toast.LENGTH_SHORT).show();
						} catch (IOException e) {
							Toast.makeText(AppActivity.this, project == null ? R.string.error_open_project : R.string.error_edit_project, Toast.LENGTH_SHORT).show();
						}
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private class OpenFileListener implements View.OnClickListener, FileChooser.OnFileSelectedListener {

		private LocalFiles files;

		public OpenFileListener(LocalFiles files) {
			this.files = files;
		}

		@Override
		public void onFileSelected(FileChooser chooser, final String resultPath) {
			openFile(resultPath, null);
		}

		@Override
		public void onClick(View view) {
			switch (view.getId()) {
				case R.id.open_file:
					final EditText input = (EditText) findViewById(R.id.open_file_path);
					String path = input.getText().toString().trim();
					if (path.length() == 0)
						Toast.makeText(AppActivity.this, R.string.nothing_entered, Toast.LENGTH_SHORT).show();
					else
						openFile(path, new Runnable() {
							@Override
							public void run() {
								try {
									input.setText(files.isProject() ? LocalFiles.getProjectRemoteDir(files.getProjectRoot()).toString() : "");
								} catch (IOException e) {
									input.setText("");
								}
							}
						});
					break;
				case R.id.select_file:
					RemoteFileChooser chooser = new RemoteFileChooser(AppActivity.this, session);
					chooser.setMode(FileChooser.MODE_FILES | FileChooser.MODE_FILES_FLAG_CUSTOM_FILE_NAMES_ALLOWED);
					try {
						if (files.isProject())
							chooser.setPath(LocalFiles.getProjectRemoteDir(files.getProjectRoot()).toString());
					} catch (IOException e) {
					}
					chooser.setOnFileSelectedListener(this);
					chooser.show();
					break;
			}
		}

		private void openFile(final String path, final Runnable resetInterfaceTask) {
			final String identifier = files.open(new File(path));
			if (identifier == null) {
				Toast.makeText(AppActivity.this, R.string.sftp_error_pull_file, Toast.LENGTH_SHORT).show();
			} else
				try {
					new SftpTask(AppActivity.this, session).execute(SftpTask.transferFiles(SftpTask.DIRECTION_PULL, path, files.getContentFile(identifier).getPath(), new VisualTask.Notifier<SftpCommand>() {
						@Override
						public void onSuccess(VisualTask<SftpCommand> task, SftpCommand command) {
							if (resetInterfaceTask != null)
								resetInterfaceTask.run();
							if (openFileAdapter != null)
								openFileAdapter.update();
						}

						@Override
						public void onFailure(VisualTask<SftpCommand> task, SftpCommand command, Throwable cause) {
							if (cause instanceof SftpException && ((SftpException) cause).id == 2) {
								AlertDialog d = new AlertDialog.Builder(AppActivity.this)
										.setTitle(R.string.no_such_file)
										.setMessage(R.string.do_you_want_to_create_it)
										.setCancelable(false)
										.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface p1, int p2) {
												new SftpTask(AppActivity.this, session).execute(SftpTask.mkdir(new File(path).getParent(), new VisualTask.Notifier<SftpCommand>() {
													@Override
													public void onSuccess(VisualTask<SftpCommand> task, SftpCommand command) {
														if (resetInterfaceTask != null)
															resetInterfaceTask.run();
														if (openFileAdapter != null)
															openFileAdapter.update();
													}

													@Override
													public void onFailure(VisualTask<SftpCommand> task, SftpCommand command, Throwable cause) {
														files.closeFile(identifier);
														String message = SftpErrors.getSftpErrorMessage(AppActivity.this, cause);
														Toast.makeText(AppActivity.this, message == null ? getString(R.string.sftp_error_create_parent_dir) : message, Toast.LENGTH_SHORT).show();
													}
												}));
											}
										})
										.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface p1, int p2) {
												files.closeFile(identifier);
											}
										})
										.create();
								d.setCanceledOnTouchOutside(false);
								d.show();
								return;
							}
							files.closeFile(identifier);
							String message = SftpErrors.getSftpErrorMessage(AppActivity.this, cause);
							Toast.makeText(AppActivity.this, message == null ? getString(R.string.sftp_error_pull_file) : message, Toast.LENGTH_SHORT).show();
						}
					}));
				} catch (IOException e) {
					files.closeFile(identifier);
					Toast.makeText(AppActivity.this, R.string.sftp_error_pull_file, Toast.LENGTH_SHORT).show();
				}
		}

	}
}
