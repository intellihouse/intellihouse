package house.intelli.pgp;

import static java.util.Objects.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;
import house.intelli.core.observable.ObservableList;
import house.intelli.core.observable.standard.StandardPostModificationEvent;
import house.intelli.core.observable.standard.StandardPostModificationListener;

public class CreatePgpKeyParam extends AbstractBean<CreatePgpKeyParam.Property> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		userIds,
		passphrase,
		validitySeconds,
		algorithm,
		strength
	}

	private final List<PgpUserId> _userIds = new ArrayList<>();
	private transient ObservableList<PgpUserId> userIds = ObservableList.decorate(_userIds);
	{
		userIds.getHandler().addPostModificationListener(new PostModificationListener());
	}

	private class PostModificationListener implements StandardPostModificationListener {
		@Override
		public void modificationOccurred(StandardPostModificationEvent event) {
			firePropertyChange(PropertyEnum.userIds, null, getUserIds());
		}
	};

	private char[] passphrase = {};

	private long validitySeconds = 10L * 365 * 24 * 3600;

	private Algorithm algorithm = Algorithm.RSA;

	private int strength = max(algorithm.getSupportedStrengths());

	public CreatePgpKeyParam() {
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		userIds = ObservableList.decorate(_userIds);
		userIds.getHandler().addPostModificationListener(new PostModificationListener());
	}

	public ObservableList<PgpUserId> getUserIds() {
		return userIds;
	}

	public char[] getPassphrase() {
		return passphrase;
	}
	public void setPassphrase(char[] passphrase) {
		setPropertyValue(PropertyEnum.passphrase, passphrase);
	}

	/**
	 * Gets the validity of the newly created key in seconds.
	 * <p>
	 * This means, how long after its creation will the new key be valid before it expires. A value of
	 * 0 means the key is valid forever, i.e. it never expires.
	 *
	 * @return the validity of the newly created key in seconds.
	 */
	public long getValiditySeconds() {
		return validitySeconds;
	}
	public void setValiditySeconds(long validitySeconds) {
		if (validitySeconds < 0)
			throw new IllegalArgumentException("validitySeconds < 0");

		setPropertyValue(PropertyEnum.validitySeconds, validitySeconds);
	}

	public Algorithm getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(final Algorithm algorithm) {
		setPropertyValue(PropertyEnum.algorithm, requireNonNull(algorithm, "algorithm"));
		if (!algorithm.isSupportedStrength(strength))
			setStrength(max(algorithm.getSupportedStrengths()));
	}

	public int getStrength() {
		return strength;
	}
	public void setStrength(int strength) {
		if (!algorithm.isSupportedStrength(strength))
			throw new IllegalArgumentException(String.format("strength=%s is not supported by algorithm %s!", strength, algorithm));

		setPropertyValue(PropertyEnum.strength, strength);
	}

	public static enum Algorithm {
		RSA(1024, 2048, 4096),
		DSA_AND_EL_GAMAL(1024, 2048, 4096);

		private final List<Integer> supportedStrengths;

		private Algorithm(final int ... supportedStrengths) {
			final List<Integer> l = new ArrayList<Integer>(supportedStrengths.length);
			for (final int supportedStrength : requireNonNull(supportedStrengths, "supportedStrengths"))
				l.add(supportedStrength);

			this.supportedStrengths = Collections.unmodifiableList(l);
		}

		public List<Integer> getSupportedStrengths() {
			return supportedStrengths;
		}

		public boolean isSupportedStrength(int strength) {
			return supportedStrengths.contains(strength);
		}
	}

	private static int max(List<Integer> values) {
		int result = Integer.MIN_VALUE;
		for (int v : values) {
			if (result < v)
				result = v;
		}
		return result;
	}

	/**
	 * Creates a new instance of {@code this} that can be transferred to the
	 * client-process-VM.
	 * <p>
	 * This object may contain instances of JavaFX-specific sub-classes (e.g. {@code FxPgpUserId}).
	 * These classes are not available in the client-process-VM, because they are UI-specific.
	 * Hence, we must create a clone that is free of such UI-specific objects.
	 * @return a new instance wiht the same data as {@code this}. Never <code>null</code>.
	 */
	public CreatePgpKeyParam toPortable() {
		final CreatePgpKeyParam result = new CreatePgpKeyParam();

		for (PgpUserId pgpUserId : this._userIds)
			result._userIds.add(new PgpUserId(pgpUserId));

		result.passphrase = this.passphrase;
		result.validitySeconds = this.validitySeconds;
		result.algorithm = this.algorithm;
		result.strength = this.strength;

		return result;
	}
}
