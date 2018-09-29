package house.intelli.pgp;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractPgp implements Pgp {

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	@Override
	public PgpEncoder createEncoder(final InputStream in, final OutputStream out) {
		final PgpEncoder encoder = _createEncoder();
		encoder.setInputStream(in);
		encoder.setOutputStream(out);
		return encoder;
	}

	protected abstract PgpEncoder _createEncoder();

	@Override
	public PgpDecoder createDecoder(final InputStream in, final OutputStream out) {
		final PgpDecoder decoder = _createDecoder();
		decoder.setInputStream(in);
		decoder.setOutputStream(out);
		return decoder;
	}

	protected abstract PgpDecoder _createDecoder();

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}

//	@Override
//	public void exportPublicKeysWithSecretKeys(final Set<PgpKey> pgpKeys, final File file) {
//		requireNonNull("pgpKeys", pgpKeys);
//		try {
//			try (OutputStream out = requireNonNull("file", file).createOutputStream();) {
//				exportPublicKeysWithSecretKeys(pgpKeys, out);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	public void exportPublicKeys(final Set<PgpKey> pgpKeys, final File file) {
//		requireNonNull("pgpKeys", pgpKeys);
//		try {
//			try (OutputStream out = requireNonNull("file", file).createOutputStream();) {
//				exportPublicKeys(pgpKeys, out);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	public byte[] exportPublicKeys(Set<PgpKey> pgpKeys) {
//		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
//		exportPublicKeys(pgpKeys, bout);
//		return bout.toByteArray();
//	}
//
//	@Override
//	public ImportKeysResult importKeys(final File file) {
//		try {
//			try (InputStream in = requireNonNull("file", file).createInputStream();) {
//				return importKeys(in);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	public ImportKeysResult importKeys(byte[] data) {
//		requireNonNull("data", data);
//		return importKeys(new ByteArrayInputStream(data));
//	}
//
//	@Override
//	public TempImportKeysResult importKeysTemporarily(File file) {
//		try {
//			try (InputStream in = requireNonNull("file", file).createInputStream();) {
//				return importKeysTemporarily(in);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	public TempImportKeysResult importKeysTemporarily(byte[] data) {
//		requireNonNull("data", data);
//		return importKeysTemporarily(new ByteArrayInputStream(data));
//	}
}
