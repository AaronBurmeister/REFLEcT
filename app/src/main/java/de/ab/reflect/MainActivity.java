package de.ab.reflect;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import java.io.File;

import de.ab.dialog.FileChooser;
import de.ab.dialog.SimpleFileChooser;
import de.ab.reflect.ssh.SSHConfiguration;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static de.ab.reflect.ssh.SSHConfiguration.HOST;
import static de.ab.reflect.ssh.SSHConfiguration.KNOWN_HOSTS_FILE;
import static de.ab.reflect.ssh.SSHConfiguration.PASSPHRASE;
import static de.ab.reflect.ssh.SSHConfiguration.PASSWORD;
import static de.ab.reflect.ssh.SSHConfiguration.PORT;
import static de.ab.reflect.ssh.SSHConfiguration.RSA_PRIVATE_KEY_FILE;
import static de.ab.reflect.ssh.SSHConfiguration.USER;

public class MainActivity extends Activity {

	private static final String
			CONFIG_HOST = "host",
			CONFIG_PORT = "port",
			CONFIG_USER = "user",
			CONFIG_PASSWORD_ENABLED = "passwordEnabled",
			CONFIG_AUTH_ENABLED = "authEnabled",
			CONFIG_AUTH = "auth",
			CONFIG_PASSPHRASE_ENABLED = "passphraseEnabled",
			CONFIG_KNOWN_HOSTS = "knownHosts",
			CONFIG_PASSWORD = "password",
			CONFIG_PASSPHRASE = "passphrase",
			CONFIG_SAVE_CONFIG = "saveConfig",
			CONFIG_SAVE_PASSWORDS = "savePasswords";
	private static final int REQUEST_PERMISSION = 1;

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
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
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
					ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
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
		setContentView(R.layout.main);

		SharedPreferences pref = getConfig();
		((EditText) findViewById(R.id.host)).setText(pref.getString(CONFIG_HOST, ""));
		((EditText) findViewById(R.id.port)).setText(String.valueOf(pref.getInt(CONFIG_PORT, Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString()))));
		((EditText) findViewById(R.id.user)).setText(pref.getString(CONFIG_USER, ""));
		((CheckBox) findViewById(R.id.password_enabled)).setChecked(pref.getBoolean(CONFIG_PASSWORD_ENABLED, ((CheckBox) findViewById(R.id.password_enabled)).isChecked()));
		((CheckBox) findViewById(R.id.auth_enabled)).setChecked(pref.getBoolean(CONFIG_AUTH_ENABLED, ((CheckBox) findViewById(R.id.auth_enabled)).isChecked()));
		((EditText) findViewById(R.id.auth)).setText(pref.getString(CONFIG_AUTH, ""));
		((CheckBox) findViewById(R.id.passphrase_enabled)).setChecked(pref.getBoolean(CONFIG_PASSPHRASE_ENABLED, ((CheckBox) findViewById(R.id.passphrase_enabled)).isChecked()));
		((EditText) findViewById(R.id.known_hosts)).setText(pref.getString(CONFIG_KNOWN_HOSTS, ""));
		((EditText) findViewById(R.id.password)).setText(pref.getString(CONFIG_PASSWORD, ""));
		((EditText) findViewById(R.id.passphrase)).setText(pref.getString(CONFIG_PASSPHRASE, ""));
		((CheckBox) findViewById(R.id.save_config)).setChecked(pref.getBoolean(CONFIG_SAVE_CONFIG, ((CheckBox) findViewById(R.id.save_config)).isChecked()));
		((CheckBox) findViewById(R.id.save_passwords)).setChecked(pref.getBoolean(CONFIG_SAVE_PASSWORDS, ((CheckBox) findViewById(R.id.save_passwords)).isChecked()));

		setCompoundBinding(R.id.password_enabled, R.id.password);
		setCompoundBinding(R.id.passphrase_enabled, R.id.passphrase);
		setCompoundBinding(R.id.auth_enabled, R.id.auth, R.id.auth_select, R.id.passphrase_enabled, R.id.passphrase, R.id.known_hosts, R.id.known_hosts_select);
		setCompoundBinding(R.id.save_config, R.id.save_passwords);

		setFileChooserBinding(R.id.auth, R.id.auth_select);
		setFileChooserBinding(R.id.known_hosts, R.id.known_hosts_select);
	}

	private void setCompoundBinding(@IdRes int idCompound, @IdRes int... idBindings) {
		CompoundButton compoundButton = (CompoundButton) findViewById(idCompound);
		final View[] bindings = new View[idBindings.length];
		final boolean[] originalStates = new boolean[idBindings.length];
		for (int i = 0; i < bindings.length; i++)
			originalStates[i] = (bindings[i] = findViewById(idBindings[i])).isEnabled();
		class InnerExecClass {
			void run(boolean state) {
				if (state)
					for (int i = 0; i < bindings.length; i++)
						bindings[i].setEnabled(originalStates[i]);
				else {
					for (int i = 0; i < bindings.length; i++) {
						View v = bindings[i];
						originalStates[i] = v.isEnabled();
						v.setEnabled(false);
					}
				}
			}
		}
		final InnerExecClass execInstance = new InnerExecClass();
		compoundButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
				execInstance.run(checked);
			}
		});
		execInstance.run(compoundButton.isChecked());
	}

	private void setFileChooserBinding(@IdRes final int editText, @IdRes int button) {
		findViewById(button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SimpleFileChooser chooser = new SimpleFileChooser(MainActivity.this);
				chooser.setMode(FileChooser.MODE_FILES);
				chooser.setOnFileSelectedListener(new FileChooser.OnFileSelectedListener() {
					@Override
					public void onFileSelected(FileChooser chooser, String resultPath) {
						((EditText) findViewById(editText)).setText(resultPath);
					}
				});
				chooser.show();
			}
		});
	}

	public void connect(View v) {
		String host = ((EditText) findViewById(R.id.host)).getText().toString().trim();
		String port = ((EditText) findViewById(R.id.port)).getText().toString().trim();
		int portNum;
		String user = ((EditText) findViewById(R.id.user)).getText().toString().trim();
		boolean passwordEnabled = ((CheckBox) findViewById(R.id.password_enabled)).isChecked();
		String password = ((EditText) findViewById(R.id.password)).getText().toString();
		boolean authEnabled = ((CheckBox) findViewById(R.id.auth_enabled)).isChecked();
		String auth = ((EditText) findViewById(R.id.auth)).getText().toString().trim();
		boolean passphraseEnabled = ((CheckBox) findViewById(R.id.passphrase_enabled)).isChecked();
		String passphrase = ((EditText) findViewById(R.id.passphrase)).getText().toString();
		String knownHosts = ((EditText) findViewById(R.id.known_hosts)).getText().toString().trim();

		boolean saveConfig = ((CheckBox) findViewById(R.id.save_config)).isChecked();
		boolean savePasswords = ((CheckBox) findViewById(R.id.save_passwords)).isChecked();

		if (host.trim().length() == 0) {
			format(R.id.host);
			return;
		}
		try {
			if (port.trim().length() == 0 || (portNum = Integer.parseInt(port)) < 1) {
				format(R.id.port);
				return;
			}
		} catch (NumberFormatException e) {
			format(R.id.port);
			return;
		}
		if (user.trim().length() == 0) {
			format(R.id.user);
			return;
		}
		if (authEnabled && (auth.trim().length() == 0 || !new File(auth).isFile())) {
			format(R.id.auth);
			return;
		}
		if (authEnabled && (knownHosts.trim().length() == 0 || !new File(knownHosts).isFile())) {
			format(R.id.known_hosts);
			return;
		}

		SharedPreferences.Editor prefEdit = getConfig().edit();
		if (saveConfig) {
			prefEdit.putString("host", host);
			prefEdit.putInt("port", portNum);
			prefEdit.putString("user", user);
			prefEdit.putBoolean("passwordEnabled", passwordEnabled);
			prefEdit.putBoolean("authEnabled", authEnabled);
			prefEdit.putString("auth", auth);
			prefEdit.putBoolean("passphraseEnabled", passphraseEnabled);
			prefEdit.putString("knownHosts", knownHosts);
			if (savePasswords) {
				prefEdit.putString("password", password);
				prefEdit.putString("passphrase", passphrase);
			} else {
				prefEdit.remove("password");
				prefEdit.remove("passphrase");
			}
		} else
			prefEdit.clear();
		prefEdit.putBoolean("saveConfig", saveConfig);
		prefEdit.putBoolean("savePasswords", savePasswords);
		prefEdit.apply();

		SSHConfiguration config = new SSHConfiguration();
		config.put(HOST, host);
		config.put(PORT, portNum);
		config.put(USER, user);
		if (passwordEnabled)
			config.put(PASSWORD, password);
		if (authEnabled) {
			config.put(RSA_PRIVATE_KEY_FILE, auth);
			config.put(KNOWN_HOSTS_FILE, knownHosts);
			if (passphraseEnabled)
				config.put(PASSPHRASE, passphrase);
		}

		Intent intent = new Intent(this, AppActivity.class);
		intent.putExtra(AppActivity.SSH_CONFIG, config);
		startActivity(intent);
	}

	private void format(int id) {
		EditText et = (EditText) findViewById(id);
		et.setText(null);
		et.requestFocus();
		et.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tremble));
	}

	public void cancel(View v) {
		finish();
	}

	private SharedPreferences getConfig() {
		return getSharedPreferences("config", MODE_PRIVATE);
	}

}
