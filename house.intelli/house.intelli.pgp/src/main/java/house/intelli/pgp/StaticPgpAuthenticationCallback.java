package house.intelli.pgp;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticPgpAuthenticationCallback implements PgpAuthenticationCallback {

	private String defaultPassphrase;
	private Map<String, String> keyId2Passphrase = new HashMap<>();
	private Map<String, String> userId2Passphrase = new HashMap<>();

	private final Map<PgpKeyId, char[]> pgpKeyId2Passphrase = Collections.synchronizedMap(new HashMap<>());

	@Override
	public char[] getPassphrase(final PgpKey pgpKey) {
		final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
		char[] passphrase = pgpKeyId2Passphrase.get(pgpKeyId);
		if (passphrase == null && ! pgpKeyId2Passphrase.containsKey(pgpKeyId)) {
			passphrase = getPassphraseForKeyId(pgpKeyId.toHumanString());

			if (passphrase == null)
				passphrase = getPassphraseForKeyId(pgpKeyId.toString());

			if (passphrase == null)
				passphrase = getPassphraseForUserId(pgpKey.getUserIds());

			if (passphrase == null) {
				String p = getDefaultPassphrase();
				passphrase = p == null ? null : p.toCharArray();
			}
			pgpKeyId2Passphrase.put(pgpKeyId, passphrase);
		}
		return passphrase;
	}

	private char[] getPassphraseForUserId(List<String> userIds) {
		for (String userId : userIds) {
			String passphrase = userId2Passphrase.get(userId);
			if (passphrase != null)
				return passphrase.toCharArray();
		}
		return null;
	}

	private char[] getPassphraseForKeyId(String keyId) {
		String passphrase = keyId2Passphrase.get(keyId);
		if (passphrase != null)
			return passphrase.toCharArray();

		return null;
	}

	public String getDefaultPassphrase() {
		return defaultPassphrase;
	}
	public void setDefaultPassphrase(String passphrase) {
		this.defaultPassphrase = passphrase;
	}

	public Map<String, String> getUserId2Passphrase() {
		return userId2Passphrase;
	}
	public void setUserId2Passphrase(Map<String, String> userId2Passphrase) {
		this.userId2Passphrase = new HashMap<>(userId2Passphrase == null ? new HashMap<>() : userId2Passphrase);
	}

	public Map<String, String> getKeyId2Passphrase() {
		return keyId2Passphrase;
	}
	public void setKeyId2Passphrase(Map<String, String> keyId2Passphrase) {
		this.keyId2Passphrase = new HashMap<>(keyId2Passphrase == null ? new HashMap<>() : keyId2Passphrase);
	}
}
