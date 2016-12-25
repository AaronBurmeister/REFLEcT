package de.ab.reflect.ssh;

import java.io.Serializable;
import java.util.Map;

import de.ab.util.Configuration;

public class SSHConfiguration extends Configuration {

	public static final String
			HOST = "host",
			PORT = "port",
			USER = "user",
			PASSWORD = "password",
			RSA_PRIVATE_KEY_FILE = "rsaPrivateKeyFile",
			PASSPHRASE = "passphrase",
			KNOWN_HOSTS_FILE = "knownHostsFile";

	public SSHConfiguration() {
	}

	public SSHConfiguration(Map<? extends String, ? extends Object> other) {
		super(other);
	}

	public static SSHConfiguration deserialize(Serializable src) {
		if (src instanceof Map)
			return new SSHConfiguration((Map) src);
		return null;
	}

	@Override
	protected Configuration.Pattern onCreatePattern() {
		return new WhitelistPattern(
				HOST,
				PORT,
				USER,
				PASSWORD,
				RSA_PRIVATE_KEY_FILE,
				PASSPHRASE,
				KNOWN_HOSTS_FILE
		);
	}

}
