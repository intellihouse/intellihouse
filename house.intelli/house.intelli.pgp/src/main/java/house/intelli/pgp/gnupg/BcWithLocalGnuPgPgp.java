package house.intelli.pgp.gnupg;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.CollectionUtil.nullToEmpty;
import static house.intelli.core.util.HashUtil.*;
import static house.intelli.core.util.IOUtil.*;
import static house.intelli.core.util.PropertiesUtil.*;
import static house.intelli.core.util.StringUtil.*;
import static house.intelli.core.util.Util.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.wot.IoFile;
import org.bouncycastle.openpgp.wot.OwnerTrust;
import org.bouncycastle.openpgp.wot.TrustDb;
import org.bouncycastle.openpgp.wot.Validity;
import org.bouncycastle.openpgp.wot.key.PgpKeyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.config.ConfigDir;
import house.intelli.core.io.LockFile;
import house.intelli.core.io.LockFileFactory;
import house.intelli.core.util.IOUtil;
import house.intelli.pgp.AbstractPgp;
import house.intelli.pgp.CertifyPgpKeyParam;
import house.intelli.pgp.CreatePgpKeyParam;
import house.intelli.pgp.HashAlgorithm;
import house.intelli.pgp.ImportKeysResult;
import house.intelli.pgp.ImportKeysResult.ImportedMasterKey;
import house.intelli.pgp.ImportKeysResult.ImportedSubKey;
import house.intelli.pgp.PgpAuthenticationCallback;
import house.intelli.pgp.PgpDecoder;
import house.intelli.pgp.PgpEncoder;
import house.intelli.pgp.PgpKey;
import house.intelli.pgp.PgpKeyId;
import house.intelli.pgp.PgpKeyValidity;
import house.intelli.pgp.PgpOwnerTrust;
import house.intelli.pgp.PgpRegistry;
import house.intelli.pgp.PgpSignature;
import house.intelli.pgp.PgpSignatureType;
import house.intelli.pgp.PgpUserId;
import house.intelli.pgp.PgpUserIdNameHash;
import house.intelli.pgp.TempImportKeysResult;

public class BcWithLocalGnuPgPgp extends AbstractPgp {
	private static final Logger logger = LoggerFactory.getLogger(BcWithLocalGnuPgPgp.class);

	private File configDir;
	private File gnuPgDir;
	private File pubringFile;
	private File secringFile;
	private File trustDbFile;

	private volatile long pubringFileLastModified = Long.MIN_VALUE;
	private volatile long secringFileLastModified = Long.MIN_VALUE;

	private Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey; // all keys
	private Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey; // only master-keys

	private Properties gpgProperties;
	private final Map<String, Object> pgpKeyIdRange2Mutex = new HashMap<>();
	private final Map<String, Properties> pgpKeyIdRange2LocalRevisionProperties = Collections.synchronizedMap(new HashMap<String, Properties>());

	private SecureRandom secureRandom;

	private PgpKeyRegistry pgpKeyRegistry;
	private TrustDbFactory trustDbFactory;

	private final List<Runnable> finalizerRunnables = new CopyOnWriteArrayList<>();

	private static final List<File> deleteDirsOnExit = new CopyOnWriteArrayList<>();

	@SuppressWarnings("unused")
	private final Object finalizer = new Object() {
		@Override
		protected void finalize() throws Throwable {
			for (Runnable runnable : finalizerRunnables) {
				try {
					runnable.run();
				} catch (Exception x) {
					logger.error("finalizer.finalize: " + x + ' ', x);
				}
			}
		}
	};

	private static void addToDeleteDirsOnExit(final File dir) {
		deleteDirsOnExit.add(assertNotNull(dir, "dir"));
		initShutdownHook();
	}
	private static void removeFromDeleteDirsOnExit(final File dir) {
		deleteDirsOnExit.remove(assertNotNull(dir, "dir"));
	}

	private static Thread shutdownHook;

	private static synchronized void initShutdownHook() {
		if (shutdownHook != null)
			return;

		shutdownHook = new Thread("BcWithLocalGnuPgPgpShutdownHook") {
			{
				setDaemon(false);
			}
			@Override
			public void run() {
				for (final File dir : deleteDirsOnExit) {
					if (! dir.exists()) {
						logger.info("shutdownHook.run: Skipping deletion of non-existent '{}'!", dir);
						return;
					}
					logger.info("shutdownHook.run: Deleting '{}'...", dir);
					deleteDirectoryRecursively(dir);
					logger.info("shutdownHook.run: Deleted '{}'.", dir);
				}
			}
		};

		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	/**
	 * @deprecated Don't manually invoke this constructor! Unfortunately the {@link ServiceLoader}
	 * requires it to be public.
	 */
	@Deprecated
	public BcWithLocalGnuPgPgp() {
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public boolean isSupported() {
		// always supported, because using BC which is in the dependencies.
		return true;
	}

	public File getGnuPgDir() {
		if (gnuPgDir == null)
			gnuPgDir = GnuPgDir.getInstance().getFile();

		return gnuPgDir;
	}

	@Override
	public Collection<PgpKey> getMasterKeys() {
		loadIfNeeded();
		final List<PgpKey> pgpKeys = new ArrayList<PgpKey>(pgpKeyId2masterKey.size());
		for (final BcPgpKey bcPgpKey : pgpKeyId2masterKey.values())
			pgpKeys.add(bcPgpKey.getPgpKey());

		return Collections.unmodifiableList(pgpKeys);
	}

	@Override
	public PgpKey getPgpKey(final PgpKeyId pgpKeyId) {
		assertNotNull(pgpKeyId, "pgpKeyId");
		loadIfNeeded();

		if (PgpKey.TEST_DUMMY_PGP_KEY_ID.equals(pgpKeyId))
			return PgpKey.TEST_DUMMY_PGP_KEY;

		final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		final PgpKey result = bcPgpKey == null ? null : bcPgpKey.getPgpKey();
		return result;
	}

	@Override
	public synchronized void exportPublicKeysWithSecretKeys(final Set<PgpKey> pgpKeys, OutputStream out) {
		assertNotNull(pgpKeys, "pgpKeys");
		assertNotNull(out, "out");

		if (! (out instanceof BCPGOutputStream))
			out = new BCPGOutputStream(out); // seems not necessary, but maybe better (faster for sure, since it doesn't need to be created again and again).

		try {
			for (final PgpKey pgpKey : pgpKeys) {
				final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
				bcPgpKey.getPublicKeyRing().encode(out);

				final PGPSecretKeyRing secretKeyRing = bcPgpKey.getSecretKeyRing();
				if (secretKeyRing != null)
					secretKeyRing.encode(out);
			}
			out.flush();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

//	@Override
//	public byte[] exportPublicKeysWithSecretKeys(Set<PgpKey> pgpKeys) {
//		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
//		exportPublicKeysWithSecretKeys(pgpKeys, bout);
//		return bout.toByteArray();
//	}

	@Override
	public synchronized void exportPublicKeys(final Set<PgpKey> pgpKeys, OutputStream out) {
		assertNotNull(pgpKeys, "pgpKeys");
		assertNotNull(out, "out");

		if (! (out instanceof BCPGOutputStream))
			out = new BCPGOutputStream(out); // seems not necessary, but maybe better (faster for sure, since it doesn't need to be created again and again).

		try {
			for (final PgpKey pgpKey : pgpKeys) {
				final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
				bcPgpKey.getPublicKeyRing().encode(out);
			}
			out.flush();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public TempImportKeysResult importKeysTemporarily(InputStream in) {
		assertNotNull(in, "in");

		try {
			final BcWithLocalGnuPgPgp tempPgp = new BcWithLocalGnuPgPgp();
			final File tempDir = IOUtil.createUniqueRandomFolder(IOUtil.getTempDir(), "tempPgp-");
			tempPgp.gnuPgDir = tempPgp.configDir = tempDir;

			addToDeleteDirsOnExit(tempDir);

			tempPgp.finalizerRunnables.add(new Runnable() {
				@Override
				public void run() {
					if (! tempDir.exists()) {
						logger.info("finalizerRunnable.run: Skipping deletion of non-existent '{}'!", tempDir);
						return;
					}
					logger.info("finalizerRunnable.run: Deleting '{}'...", tempDir);
					deleteDirectoryRecursively(tempDir);
					logger.info("finalizerRunnable.run: Deleted '{}'.", tempDir);

					removeFromDeleteDirsOnExit(tempDir);
				}
			});

			copyFile(this.getPubringFile(), tempPgp.getPubringFile());
			copyFile(this.getSecringFile(), tempPgp.getSecringFile());
			ImportKeysResult importKeysResult = tempPgp.importKeys(in);
			return new TempImportKeysResult(tempPgp, importKeysResult);
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public synchronized ImportKeysResult importKeys(InputStream in) {
		assertNotNull(in, "in");

		final ImportKeysResult importKeysResult = new ImportKeysResult();
		boolean modified = false;
		try {
			in = PGPUtil.getDecoderStream(in);
			final PGPObjectFactory pgpF = new PGPObjectFactory(in, new BcKeyFingerprintCalculator());

			Object o;
			while ((o = pgpF.nextObject()) != null) {
				if (o instanceof PGPPublicKeyRing)
					modified |= importPublicKeyRing(importKeysResult, (PGPPublicKeyRing) o);
				else if (o instanceof PGPSecretKeyRing)
					modified |= importSecretKeyRing(importKeysResult, (PGPSecretKeyRing) o);
				else
					throw new IllegalStateException("Unexpected object in InputStream (only PGPPublicKeyRing and PGPSecretKeyRing are supported): " + o);
			}
		} catch (IOException | PGPException x) {
			throw new RuntimeException(x);
		}

		if (modified) // make sure the localRevision is incremented, even if the timestamp does not change (e.g. because the time resolution of the file system is too low).
			incLocalRevision();

		return importKeysResult;
	}

	private synchronized boolean importPublicKeyRing(ImportKeysResult importKeysResult, final PGPPublicKeyRing publicKeyRing) throws IOException, PGPException {
		assertNotNull(publicKeyRing, "publicKeyRing");

		PGPPublicKeyRingCollection oldPublicKeyRingCollection;

		final File pubringFile = getPubringFile();
		if (!pubringFile.isFile())
			oldPublicKeyRingCollection = new PGPPublicKeyRingCollection(new ByteArrayInputStream(new byte[0]), new BcKeyFingerprintCalculator());
		else {
			try (InputStream in = new BufferedInputStream(new FileInputStream(pubringFile))) {
				oldPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
			}
		}

		PGPPublicKeyRingCollection newPublicKeyRingCollection = oldPublicKeyRingCollection;
		newPublicKeyRingCollection = mergePublicKeyRing(importKeysResult, newPublicKeyRingCollection, publicKeyRing);

		if (oldPublicKeyRingCollection != newPublicKeyRingCollection) {
			final File tmpFile = new File(pubringFile.getParentFile(), pubringFile.getName() + ".tmp");
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
				newPublicKeyRingCollection.encode(out);
			}
			pubringFile.delete();
			tmpFile.renameTo(pubringFile);

			// ensure that it's re-loaded.
			markStale();
			return true;
		}
		return false;
	}

	public void markStale() {
		pubringFileLastModified = 0;
	}

	private PGPPublicKeyRingCollection mergePublicKeyRing(ImportKeysResult importKeysResult, PGPPublicKeyRingCollection publicKeyRingCollection, final PGPPublicKeyRing publicKeyRing) throws PGPException {
		assertNotNull(publicKeyRingCollection, "publicKeyRingCollection");
		assertNotNull(publicKeyRing, "publicKeyRing");

		final PgpKeyId masterKeyId = new PgpKeyId(publicKeyRing.getPublicKey().getKeyID());
		ImportedMasterKey importedMasterKey = importKeysResult.getPgpKeyId2ImportedMasterKey().get(masterKeyId);
		if (importedMasterKey == null) {
			importedMasterKey = new ImportedMasterKey(masterKeyId);
			importKeysResult.getPgpKeyId2ImportedMasterKey().put(masterKeyId, importedMasterKey);
		}

		for (final Iterator<?> it = publicKeyRing.getPublicKeys(); it.hasNext(); ) {
			final PGPPublicKey publicKey = (PGPPublicKey) it.next();
			final PgpKeyId subKeyId = new PgpKeyId(publicKey.getKeyID());
			if (! masterKeyId.equals(subKeyId)) {
				ImportedSubKey importedSubKey = importedMasterKey.getPgpKeyId2ImportedSubKey().get(subKeyId);
				if (importedSubKey == null) {
					importedSubKey = new ImportedSubKey(subKeyId, importedMasterKey);
					importedMasterKey.getPgpKeyId2ImportedSubKey().put(subKeyId, importedSubKey);
				}
			}
		}

		PGPPublicKeyRing oldPublicKeyRing = publicKeyRingCollection.getPublicKeyRing(publicKeyRing.getPublicKey().getKeyID());
		if (oldPublicKeyRing == null)
			publicKeyRingCollection = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRingCollection, publicKeyRing);
		else {
			PGPPublicKeyRing newPublicKeyRing = oldPublicKeyRing;
			for (final Iterator<?> it = publicKeyRing.getPublicKeys(); it.hasNext(); ) {
				PGPPublicKey publicKey = (PGPPublicKey) it.next();
				newPublicKeyRing = mergePublicKey(newPublicKeyRing, publicKey);
			}

			if (newPublicKeyRing != oldPublicKeyRing) {
				publicKeyRingCollection = PGPPublicKeyRingCollection.removePublicKeyRing(publicKeyRingCollection, oldPublicKeyRing);

				final PGPPublicKeyRing pkr = publicKeyRingCollection.getPublicKeyRing(publicKeyRing.getPublicKey().getKeyID());
				if (pkr != null)
				    throw new IllegalStateException("PGPPublicKeyRingCollection.removePublicKeyRing(...) had no effect!");

				publicKeyRingCollection = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRingCollection, newPublicKeyRing);
			}
		}
		return publicKeyRingCollection;
	}

	private PGPPublicKeyRing mergePublicKey(PGPPublicKeyRing publicKeyRing, final PGPPublicKey publicKey) {
		assertNotNull(publicKeyRing, "publicKeyRing");
		assertNotNull(publicKey, "publicKey");

		PGPPublicKey oldPublicKey = publicKeyRing.getPublicKey(publicKey.getKeyID());
		if (oldPublicKey == null)
			publicKeyRing = PGPPublicKeyRing.insertPublicKey(publicKeyRing, publicKey);
		else {
			PGPPublicKey newPublicKey = oldPublicKey;
			for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getKeySignatures()); it.hasNext(); ) {
				PGPSignature signature = (PGPSignature) it.next();
				newPublicKey = mergeKeySignature(newPublicKey, signature);
			}
			for (@SuppressWarnings("unchecked") Iterator<?> uit = nullToEmpty(publicKey.getUserIDs()); uit.hasNext(); ) {
				final String userId = (String) uit.next();
				for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getSignaturesForID(userId)); it.hasNext(); ) {
					PGPSignature signature = (PGPSignature) it.next();
					newPublicKey = mergeUserIdSignature(newPublicKey, userId, signature);
				}
			}
			for (@SuppressWarnings("unchecked") Iterator<?> uit = nullToEmpty(publicKey.getUserAttributes()); uit.hasNext(); ) {
				final PGPUserAttributeSubpacketVector userAttribute = (PGPUserAttributeSubpacketVector) uit.next();
				for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getSignaturesForUserAttribute(userAttribute)); it.hasNext(); ) {
					PGPSignature signature = (PGPSignature) it.next();
					newPublicKey = mergeUserAttributeSignature(newPublicKey, userAttribute, signature);
				}
			}

			if (newPublicKey != oldPublicKey) {
				publicKeyRing = PGPPublicKeyRing.removePublicKey(publicKeyRing, oldPublicKey);

				final PGPPublicKey pk = publicKeyRing.getPublicKey(publicKey.getKeyID());
				if (pk != null)
				    throw new IllegalStateException("PGPPublicKeyRing.removePublicKey(...) had no effect!");

				publicKeyRing = PGPPublicKeyRing.insertPublicKey(publicKeyRing, newPublicKey);
			}
		}
		return publicKeyRing;
	}

	private PGPPublicKey mergeKeySignature(PGPPublicKey publicKey, final PGPSignature signature) {
		assertNotNull(publicKey, "publicKey");
		assertNotNull(signature, "signature");

		PGPSignature oldSignature = getKeySignature(publicKey, signature);
		if (oldSignature == null)
			publicKey = PGPPublicKey.addCertification(publicKey, signature);

		return publicKey;
	}

	private PGPPublicKey mergeUserIdSignature(PGPPublicKey publicKey, final String userId, final PGPSignature signature) {
		assertNotNull(publicKey, "publicKey");
		assertNotNull(userId, "userId");
		assertNotNull(signature, "signature");

		PGPSignature oldSignature = getUserIdSignature(publicKey, userId, signature);
		if (oldSignature == null)
			publicKey = PGPPublicKey.addCertification(publicKey, userId, signature);

		return publicKey;
	}

	private PGPPublicKey mergeUserAttributeSignature(PGPPublicKey publicKey, final PGPUserAttributeSubpacketVector userAttribute, final PGPSignature signature) {
		assertNotNull(publicKey, "publicKey");
		assertNotNull(userAttribute, "userAttribute");
		assertNotNull(signature, "signature");

		PGPSignature oldSignature = getUserAttributeSignature(publicKey, userAttribute, signature);
		if (oldSignature == null)
			publicKey = PGPPublicKey.addCertification(publicKey, userAttribute, signature);

		return publicKey;
	}

	private static PGPSignature getKeySignature(final PGPPublicKey publicKey, final PGPSignature signature) {
		assertNotNull(publicKey, "publicKey");
		assertNotNull(signature, "signature");

		for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getKeySignatures()); it.hasNext(); ) {
			final PGPSignature s = (PGPSignature) it.next();
			if (isSignatureEqual(s, signature))
				return s;
		}
		return null;
	}

	private static PGPSignature getUserIdSignature(final PGPPublicKey publicKey, final String userId, final PGPSignature signature) {
		assertNotNull(publicKey, "publicKey");
		assertNotNull(userId, "userId");
		assertNotNull(signature, "signature");

		for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getSignaturesForID(userId)); it.hasNext(); ) {
			final PGPSignature s = (PGPSignature) it.next();
			if (isSignatureEqual(s, signature))
				return s;
		}
		return null;
	}

	private static PGPSignature getUserAttributeSignature(final PGPPublicKey publicKey, final PGPUserAttributeSubpacketVector userAttribute, final PGPSignature signature) {
		assertNotNull(publicKey, "publicKey");
		assertNotNull(userAttribute, "userAttribute");
		assertNotNull(signature, "signature");

		for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getSignaturesForUserAttribute(userAttribute)); it.hasNext(); ) {
			final PGPSignature s = (PGPSignature) it.next();
			if (isSignatureEqual(s, signature))
				return s;
		}
		return null;
	}

	private static boolean isSignatureEqual(final PGPSignature one, final PGPSignature two) {
		return equal(one.getKeyID(), two.getKeyID())
				&& equal(one.getCreationTime(), two.getCreationTime())
				&& equal(one.getHashAlgorithm(), two.getHashAlgorithm())
				&& equal(one.getKeyAlgorithm(), two.getKeyAlgorithm())
				&& equal(one.getSignatureType(), two.getSignatureType());
	}

	private synchronized boolean importSecretKeyRing(ImportKeysResult importKeysResult, final PGPSecretKeyRing secretKeyRing) throws IOException, PGPException {
		assertNotNull(secretKeyRing, "secretKeyRing");

		PGPSecretKeyRingCollection oldSecretKeyRingCollection;

		final File secringFile = getSecringFile();
		if (!secringFile.isFile())
			oldSecretKeyRingCollection = new PGPSecretKeyRingCollection(new ByteArrayInputStream(new byte[0]), new BcKeyFingerprintCalculator());
		else {
			try (InputStream in = new BufferedInputStream(new FileInputStream(secringFile))) {
				oldSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
			}
		}

		PGPSecretKeyRingCollection newSecretKeyRingCollection = oldSecretKeyRingCollection;
		newSecretKeyRingCollection = mergeSecretKeyRing(importKeysResult, newSecretKeyRingCollection, secretKeyRing);

		if (oldSecretKeyRingCollection != newSecretKeyRingCollection) {
			final File tmpFile = new File(secringFile.getParentFile(), secringFile.getName() + ".tmp");
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
				newSecretKeyRingCollection.encode(out);
			}
			secringFile.delete();
			tmpFile.renameTo(secringFile);

			// ensure that it's re-loaded.
			secringFileLastModified = 0;
			return true;
		}
		return false;
	}

	private PGPSecretKeyRingCollection mergeSecretKeyRing(ImportKeysResult importKeysResult, PGPSecretKeyRingCollection secretKeyRingCollection, final PGPSecretKeyRing secretKeyRing) throws PGPException {
		assertNotNull(secretKeyRingCollection, "secretKeyRingCollection");
		assertNotNull(secretKeyRing, "secretKeyRing");

		final PgpKeyId masterKeyId = new PgpKeyId(secretKeyRing.getSecretKey().getKeyID());
		ImportedMasterKey importedMasterKey = importKeysResult.getPgpKeyId2ImportedMasterKey().get(masterKeyId);
		if (importedMasterKey == null) {
			importedMasterKey = new ImportedMasterKey(masterKeyId);
			importKeysResult.getPgpKeyId2ImportedMasterKey().put(masterKeyId, importedMasterKey);
		}

		for (final Iterator<?> it = secretKeyRing.getSecretKeys(); it.hasNext(); ) {
			final PGPSecretKey secretKey = (PGPSecretKey) it.next();
			final PgpKeyId subKeyId = new PgpKeyId(secretKey.getKeyID());
			if (! masterKeyId.equals(subKeyId)) {
				ImportedSubKey importedSubKey = importedMasterKey.getPgpKeyId2ImportedSubKey().get(subKeyId);
				if (importedSubKey == null) {
					importedSubKey = new ImportedSubKey(subKeyId, importedMasterKey);
					importedMasterKey.getPgpKeyId2ImportedSubKey().put(subKeyId, importedSubKey);
				}
			}
		}

		PGPSecretKeyRing oldSecretKeyRing = secretKeyRingCollection.getSecretKeyRing(secretKeyRing.getSecretKey().getKeyID());
		if (oldSecretKeyRing == null)
			secretKeyRingCollection = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRingCollection, secretKeyRing);
		else {
			PGPSecretKeyRing newSecretKeyRing = oldSecretKeyRing;
			for (final Iterator<?> it = secretKeyRing.getSecretKeys(); it.hasNext(); ) {
				PGPSecretKey secretKey = (PGPSecretKey) it.next();
				newSecretKeyRing = mergeSecretKey(newSecretKeyRing, secretKey);
			}

			if (newSecretKeyRing != oldSecretKeyRing) {
				secretKeyRingCollection = PGPSecretKeyRingCollection.removeSecretKeyRing(secretKeyRingCollection, oldSecretKeyRing);
				secretKeyRingCollection = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRingCollection, newSecretKeyRing);
			}
		}
		return secretKeyRingCollection;
	}

	private PGPSecretKeyRing mergeSecretKey(PGPSecretKeyRing secretKeyRing, final PGPSecretKey secretKey) {
		assertNotNull(secretKeyRing, "secretKeyRing");
		assertNotNull(secretKey, "secretKey");

		PGPSecretKey oldSecretKey = secretKeyRing.getSecretKey(secretKey.getKeyID());
		if (oldSecretKey == null)
			secretKeyRing = PGPSecretKeyRing.insertSecretKey(secretKeyRing, secretKey);
		// else: there is nothing to merge - a secret key is immutable. btw. it contains a public key - but without signatures.

		return secretKeyRing;
	}

	public BcPgpKey getBcPgpKeyOrFail(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKey(pgpKey);
		if (bcPgpKey == null)
			throw new IllegalArgumentException("Unknown pgpKey with pgpKeyId=" + pgpKey.getPgpKeyId());

		return bcPgpKey;
	}

	public BcPgpKey getBcPgpKey(final PgpKey pgpKey) {
		final PgpKeyId pgpKeyId = assertNotNull(pgpKey, "pgpKey").getPgpKeyId();
		return getBcPgpKey(pgpKeyId);
	}

	public BcPgpKey getBcPgpKey(final PgpKeyId pgpKeyId) {
		assertNotNull(pgpKeyId, "pgpKeyId");
		loadIfNeeded();
		final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		return bcPgpKey;
	}

	@Override
	protected PgpDecoder _createDecoder() {
		return new BcPgpDecoder(this);
	}

	@Override
	protected PgpEncoder _createEncoder() {
		return new BcPgpEncoder(this);
	}

	protected File getPubringFile() {
		if (pubringFile == null) {
			final File gnuPgDir = getGnuPgDir();
			gnuPgDir.mkdirs();
			pubringFile = new File(gnuPgDir, "pubring.gpg");
		}
		return pubringFile;
	}

	protected File getSecringFile() {
		if (secringFile == null) {
			final File gnuPgDir = getGnuPgDir();
			gnuPgDir.mkdirs();
			secringFile = new File(gnuPgDir, "secring.gpg");
		}
		return secringFile;
	}

	protected File getTrustDbFile()
    {
	    if (trustDbFile == null) {
	        final File gnuPgDir = getGnuPgDir();
            gnuPgDir.mkdirs();
            trustDbFile = new File(gnuPgDir, "trustdb.gpg");
	    }
        return trustDbFile;
    }

	protected synchronized void loadIfNeeded() {
		if (pgpKeyId2bcPgpKey == null
				|| getPubringFile().lastModified() != pubringFileLastModified
				|| getSecringFile().lastModified() != secringFileLastModified) {
			logger.debug("loadIfNeeded: invoking load().");
			load();
		}
		else
			logger.trace("loadIfNeeded: *not* invoking load().");
	}

	protected synchronized void load() {
		final Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey = new HashMap<PgpKeyId, BcPgpKey>();
		final Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey = new HashMap<PgpKeyId, BcPgpKey>();

		final long pubringFileLastModified;
		final long secringFileLastModified;
		try {
			final File secringFile = getSecringFile();
			logger.debug("load: secringFile='{}'", secringFile);
			secringFileLastModified = secringFile.lastModified();
			if (secringFile.isFile()) {
				final PGPSecretKeyRingCollection pgpSecretKeyRingCollection;
				try (InputStream in = new BufferedInputStream(new FileInputStream(secringFile))) {
					pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
				}
				for (final Iterator<?> it1 = pgpSecretKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
					final PGPSecretKeyRing keyRing = (PGPSecretKeyRing) it1.next();
					BcPgpKey masterKey = null;
					for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
						final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
						masterKey = enlistPublicKey(pgpKeyId2bcPgpKey,
								pgpKeyId2masterKey, masterKey, keyRing, publicKey);
					}

					for (final Iterator<?> it3 = keyRing.getSecretKeys(); it3.hasNext(); ) {
						final PGPSecretKey secretKey = (PGPSecretKey) it3.next();
						final PgpKeyId pgpKeyId = new PgpKeyId(secretKey.getKeyID());
						final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
						if (bcPgpKey == null)
							throw new IllegalStateException("Secret key does not have corresponding public key in secret key ring! pgpKeyId=" + pgpKeyId);

						bcPgpKey.setSecretKey(secretKey);
						logger.debug("load: read secretKey with pgpKeyId={}", pgpKeyId);
					}
				}
			}

			final File pubringFile = getPubringFile();
			logger.debug("load: pubringFile='{}'", pubringFile);
			pubringFileLastModified = pubringFile.lastModified();
			if (pubringFile.isFile()) {
				final PGPPublicKeyRingCollection pgpPublicKeyRingCollection;
				try (InputStream in = new BufferedInputStream(new FileInputStream(pubringFile))) {
					pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
				}

				for (final Iterator<?> it1 = pgpPublicKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
					final PGPPublicKeyRing keyRing = (PGPPublicKeyRing) it1.next();
					BcPgpKey masterKey = null;
					for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
						final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
						masterKey = enlistPublicKey(pgpKeyId2bcPgpKey,
								pgpKeyId2masterKey, masterKey, keyRing, publicKey);
					}
				}
			}
		} catch (IOException | PGPException x) {
			throw new RuntimeException(x);
		}

		for (final BcPgpKey bcPgpKey : pgpKeyId2bcPgpKey.values()) {
			if (bcPgpKey.getPublicKey() == null)
				throw new IllegalStateException("bcPgpKey.publicKey == null :: keyId = " + bcPgpKey.getPgpKeyId());

			if (bcPgpKey.getPublicKeyRing() == null)
				throw new IllegalStateException("bcPgpKey.publicKeyRing == null :: keyId = " + bcPgpKey.getPgpKeyId());
		}

		this.secringFileLastModified = secringFileLastModified;
		this.pubringFileLastModified = pubringFileLastModified;
		this.pgpKeyId2bcPgpKey = pgpKeyId2bcPgpKey;
		this.pgpKeyId2masterKey = pgpKeyId2masterKey;
	}

	@Override
	public Collection<PgpSignature> getCertifications(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final PGPPublicKey publicKey = bcPgpKey.getPublicKey();
		final List<PgpSignature> result = new ArrayList<PgpSignature>();

		final IdentityHashMap<PGPSignature, PGPSignature> bcPgpSignatures = new IdentityHashMap<>();

		final LinkedHashSet<String> userIds = new LinkedHashSet<>();
		for (@SuppressWarnings("unchecked") final Iterator<?> userIDsIterator = nullToEmpty(publicKey.getUserIDs()); userIDsIterator.hasNext(); ) {
			final String userId = (String) userIDsIterator.next();
			userIds.add(userId);

			for (@SuppressWarnings("unchecked") final Iterator<?> itSig = nullToEmpty(publicKey.getSignaturesForID(userId)); itSig.hasNext(); ) {
				final PGPSignature bcPgpSignature = (PGPSignature) itSig.next();
				bcPgpSignatures.put(bcPgpSignature, bcPgpSignature);
				final PgpSignature pgpSignature = createPgpSignature(bcPgpSignature);

				// all of them should be certifications, but we still check to make 100% sure
				if (! pgpSignature.getSignatureType().isCertification())
					continue;

				pgpSignature.setUserId(userId);
				result.add(pgpSignature);
			}
		}

		for (@SuppressWarnings("unchecked") final Iterator<?> userAttributesIterator = nullToEmpty(publicKey.getUserAttributes()); userAttributesIterator.hasNext(); ) {
			final PGPUserAttributeSubpacketVector userAttribute = (PGPUserAttributeSubpacketVector) userAttributesIterator.next();

			for (@SuppressWarnings("unchecked") final Iterator<?> itSig = nullToEmpty(publicKey.getSignaturesForUserAttribute(userAttribute)); itSig.hasNext(); ) {
				final PGPSignature bcPgpSignature = (PGPSignature) itSig.next();
				bcPgpSignatures.put(bcPgpSignature, bcPgpSignature);
				final PgpSignature pgpSignature = createPgpSignature(bcPgpSignature);

				// all of them should be certifications, but we still check to make 100% sure
				if (! pgpSignature.getSignatureType().isCertification())
					continue;

				PgpUserIdNameHash nameHash = PgpUserIdNameHash.createFromUserAttribute(userAttribute);
				pgpSignature.setNameHash(nameHash);
				result.add(pgpSignature);
			}
		}

		// It seems, there are both: certifications for individual user-ids and certifications for the
		// entire key. I therefore first take the individual ones (above) into account then and then
		// the ones for the entire key (below).
		// Normally, the signatures bound to the key are never 'certifications', but it rarely happens.
		// Don't know, if these are malformed or deprecated (very old) keys, but I should take them into account.
		for (@SuppressWarnings("unchecked") final Iterator<?> it = nullToEmpty(publicKey.getKeySignatures()); it.hasNext(); ) {
			final PGPSignature bcPgpSignature = (PGPSignature) it.next();
			if (bcPgpSignatures.containsKey(bcPgpSignature))
				continue;

			final PgpSignatureType signatureType = signatureTypeToEnum(bcPgpSignature.getSignatureType());
			if (! signatureType.isCertification())
				continue;

			result.add(createPgpSignature(bcPgpSignature));
		}
		return Collections.unmodifiableList(result);
	}

	public PgpSignature createPgpSignature(final PGPSignature bcPgpSignature) {
		final PgpSignature pgpSignature = new PgpSignature();
		pgpSignature.setPgpKeyId(new PgpKeyId(bcPgpSignature.getKeyID()));
		pgpSignature.setCreated(bcPgpSignature.getCreationTime());
		pgpSignature.setSignatureType(signatureTypeToEnum(bcPgpSignature.getSignatureType()));
		return pgpSignature;
	}

	@Override
	public Collection<PgpKey> getMasterKeysWithSecretKey() {
		final List<PgpKey> result = new ArrayList<PgpKey>();
		final Collection<PgpKey> masterKeys = getMasterKeys();
		for (final PgpKey pgpKey : masterKeys) {
			if (pgpKey.isSecretKeyAvailable())
				result.add(pgpKey);
		}
		return Collections.unmodifiableList(result);
	}

//	@Override
//	public boolean isTrusted(final PgpKey pgpKey) {
//		return getKeyTrustLevel(pgpKey).compareTo(PgpKeyValidity.TRUSTED_EXPIRED) >= 0;
//	}

	@Override
	public PgpKeyValidity getKeyValidity(PgpKey pgpKey) {
		assertNotNull(pgpKey, "pgpKey");
		if (pgpKey.getMasterKey() != null)
			pgpKey = pgpKey.getMasterKey();

		try (TrustDb trustDb = createTrustDb()) {
			final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
			final PGPPublicKey publicKey = bcPgpKey.getPublicKey();

			if (trustDb.isDisabled(publicKey))
				return PgpKeyValidity.DISABLED;

			if (publicKey.hasRevocation())
				return PgpKeyValidity.REVOKED;

			if (trustDb.isExpired(publicKey))
				return PgpKeyValidity.EXPIRED;

			final Validity validity = trustDb.getValidity(publicKey);
			return toPgpKeyValidity(validity);
		}
	}

	@Override
	public PgpKeyValidity getKeyValidity(PgpKey pgpKey, final String userId) {
		assertNotNull(pgpKey, "pgpKey");
		assertNotNull(userId, "userId");
		if (pgpKey.getMasterKey() != null)
			pgpKey = pgpKey.getMasterKey();

		try (TrustDb trustDb = createTrustDb()) {
			final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
			final PGPPublicKey publicKey = bcPgpKey.getPublicKey();

			if (trustDb.isDisabled(publicKey))
				return PgpKeyValidity.DISABLED;

			if (publicKey.hasRevocation())
				return PgpKeyValidity.REVOKED;

			if (trustDb.isExpired(publicKey))
				return PgpKeyValidity.EXPIRED;

			final Validity validity = trustDb.getValidity(publicKey);
			return toPgpKeyValidity(validity);
		}
	}

	private static PgpKeyValidity toPgpKeyValidity(final Validity validity) {
		switch (assertNotNull(validity, "validity")) {
			case NONE:
			case UNDEFINED:
				return PgpKeyValidity.NOT_TRUSTED;
			case MARGINAL:
				return PgpKeyValidity.MARGINAL;
			case FULL:
				return PgpKeyValidity.FULL;
			case ULTIMATE:
				return PgpKeyValidity.ULTIMATE;
			default :
				throw new IllegalStateException("Unknown validity: " + validity);
		}
	}

	@Override
	public void setDisabled(PgpKey pgpKey, boolean disabled) {
		assertNotNull(pgpKey, "pgpKey");
		if (pgpKey.getMasterKey() != null)
			pgpKey = pgpKey.getMasterKey();

		try (TrustDb trustDb = createTrustDb()) {
			final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
			final PGPPublicKey publicKey = bcPgpKey.getPublicKey();

			trustDb.setDisabled(publicKey, disabled);
			markStale();
		}
	}

	@Override
	public PgpOwnerTrust getOwnerTrust(PgpKey pgpKey) {
		assertNotNull(pgpKey, "pgpKey");
		if (pgpKey.getMasterKey() != null)
			pgpKey = pgpKey.getMasterKey();

		try (TrustDb trustDb = createTrustDb()) {
			final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
			final PGPPublicKey publicKey = bcPgpKey.getPublicKey();

			final OwnerTrust ownerTrust = trustDb.getOwnerTrust(publicKey);
			if (ownerTrust == null)
				return PgpOwnerTrust.UNSPECIFIED;

			switch (ownerTrust) {
				case UNKNOWN:
					return PgpOwnerTrust.UNKNOWN;
				case NEVER:
					return PgpOwnerTrust.NEVER;
				case MARGINAL:
					return PgpOwnerTrust.MARGINAL;
				case FULL:
					return PgpOwnerTrust.FULL;
				case ULTIMATE:
					return PgpOwnerTrust.ULTIMATE;
				default :
					throw new IllegalStateException("Unknown ownerTrust: " + ownerTrust);
			}
		}
	}

	@Override
	public void setOwnerTrust(PgpKey pgpKey, final PgpOwnerTrust ownerTrust) {
		assertNotNull(pgpKey, "pgpKey");
		assertNotNull(ownerTrust, "ownerTrust");

		if (ownerTrust == PgpOwnerTrust.UNSPECIFIED)
			throw new IllegalArgumentException("ownerTrust cannot be set to UNSPECIFIED!");

		if (pgpKey.getMasterKey() != null)
			pgpKey = pgpKey.getMasterKey();

		try (TrustDb trustDb = createTrustDb()) {
			final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
			final PGPPublicKey publicKey = bcPgpKey.getPublicKey();

			switch (ownerTrust) {
				case UNKNOWN:
					trustDb.setOwnerTrust(publicKey, OwnerTrust.UNKNOWN);
					break;
				case NEVER:
					trustDb.setOwnerTrust(publicKey, OwnerTrust.NEVER);
					break;
				case MARGINAL:
					trustDb.setOwnerTrust(publicKey, OwnerTrust.MARGINAL);
					break;
				case FULL:
					trustDb.setOwnerTrust(publicKey, OwnerTrust.FULL);
					break;
				case ULTIMATE:
					trustDb.setOwnerTrust(publicKey, OwnerTrust.ULTIMATE);
					break;
				default :
					throw new IllegalStateException("Unknown ownerTrustLevel: " + ownerTrust);
			}
		}
	}

	@Override
	public void updateTrustDb() {
		try (TrustDb trustDb = createTrustDb()) {
			trustDb.updateTrustDb();
			firePropertyChange(PropertyEnum.trustdb, null, null);
		}
	}

	private BcPgpKey enlistPublicKey(final Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey,
			final Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey,
			BcPgpKey masterKey, final PGPKeyRing keyRing, final PGPPublicKey publicKey)
	{
		final PgpKeyId pgpKeyId = new PgpKeyId(publicKey.getKeyID());
		BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		if (bcPgpKey == null) {
			bcPgpKey = new BcPgpKey(this, pgpKeyId);
			pgpKeyId2bcPgpKey.put(pgpKeyId, bcPgpKey);
		}

		if (keyRing instanceof PGPSecretKeyRing)
			bcPgpKey.setSecretKeyRing((PGPSecretKeyRing)keyRing);
		else if (keyRing instanceof PGPPublicKeyRing)
			bcPgpKey.setPublicKeyRing((PGPPublicKeyRing)keyRing);
		else
			throw new IllegalArgumentException("keyRing is neither an instance of PGPSecretKeyRing nor PGPPublicKeyRing!");

		bcPgpKey.setPublicKey(publicKey);

		if (publicKey.isMasterKey()) {
			masterKey = bcPgpKey;
			pgpKeyId2masterKey.put(bcPgpKey.getPgpKeyId(), bcPgpKey);
		}
		else {
			if (masterKey == null)
				throw new IllegalStateException("First key is a non-master key!");

			bcPgpKey.setMasterKey(masterKey);
			masterKey.getSubKeyIds().add(bcPgpKey.getPgpKeyId());
		}
		return masterKey;
	}

	private PgpSignatureType signatureTypeToEnum(final int signatureType) {
		switch (signatureType) {
			case PGPSignature.BINARY_DOCUMENT:
				return PgpSignatureType.BINARY_DOCUMENT;
			case PGPSignature.CANONICAL_TEXT_DOCUMENT:
				return PgpSignatureType.CANONICAL_TEXT_DOCUMENT;
			case PGPSignature.STAND_ALONE:
				return PgpSignatureType.STAND_ALONE;

			case PGPSignature.DEFAULT_CERTIFICATION:
				return PgpSignatureType.DEFAULT_CERTIFICATION;
			case PGPSignature.NO_CERTIFICATION:
				return PgpSignatureType.NO_CERTIFICATION;
			case PGPSignature.CASUAL_CERTIFICATION:
				return PgpSignatureType.CASUAL_CERTIFICATION;
			case PGPSignature.POSITIVE_CERTIFICATION:
				return PgpSignatureType.POSITIVE_CERTIFICATION;

			case PGPSignature.SUBKEY_BINDING:
				return PgpSignatureType.SUBKEY_BINDING;
			case PGPSignature.PRIMARYKEY_BINDING:
				return PgpSignatureType.PRIMARYKEY_BINDING;
			case PGPSignature.DIRECT_KEY:
				return PgpSignatureType.DIRECT_KEY;
			case PGPSignature.KEY_REVOCATION:
				return PgpSignatureType.KEY_REVOCATION;
			case PGPSignature.SUBKEY_REVOCATION:
				return PgpSignatureType.SUBKEY_REVOCATION;
			case PGPSignature.CERTIFICATION_REVOCATION:
				return PgpSignatureType.CERTIFICATION_REVOCATION;
			case PGPSignature.TIMESTAMP:
				return PgpSignatureType.TIMESTAMP;

			default:
				throw new IllegalArgumentException("Unknown signatureType: " + signatureType);
		}
	}

	private int signatureTypeFromEnum(final PgpSignatureType signatureType) {
		switch (assertNotNull(signatureType, "signatureType")) {
			case BINARY_DOCUMENT:
				return PGPSignature.BINARY_DOCUMENT;
			case CANONICAL_TEXT_DOCUMENT:
				return PGPSignature.CANONICAL_TEXT_DOCUMENT;
			case STAND_ALONE:
				return PGPSignature.STAND_ALONE;

			case DEFAULT_CERTIFICATION:
				return PGPSignature.DEFAULT_CERTIFICATION;
			case NO_CERTIFICATION:
				return PGPSignature.NO_CERTIFICATION;
			case CASUAL_CERTIFICATION:
				return PGPSignature.CASUAL_CERTIFICATION;
			case POSITIVE_CERTIFICATION:
				return PGPSignature.POSITIVE_CERTIFICATION;

			case SUBKEY_BINDING:
				return PGPSignature.SUBKEY_BINDING;
			case PRIMARYKEY_BINDING:
				return PGPSignature.PRIMARYKEY_BINDING;
			case DIRECT_KEY:
				return PGPSignature.DIRECT_KEY;
			case KEY_REVOCATION:
				return PGPSignature.KEY_REVOCATION;
			case SUBKEY_REVOCATION:
				return PGPSignature.SUBKEY_REVOCATION;
			case CERTIFICATION_REVOCATION:
				return PGPSignature.CERTIFICATION_REVOCATION;
			case TIMESTAMP:
				return PGPSignature.TIMESTAMP;

			default:
				throw new IllegalArgumentException("Unknown signatureType: " + signatureType);
		}
	}

	private File getConfigDir() {
		if (configDir == null)
			configDir = ConfigDir.getInstance().getFile();

		return configDir;
	}

	private File getGpgPropertiesFile() {
		return new File(getConfigDir(), "gpg.properties");
	}

	@Override
	public long getLocalRevision() {
		final Properties gpgProperties = getGpgProperties();

		loadIfNeeded();
		long pubringFileLastModified = this.pubringFileLastModified;
		long secringFileLastModified = this.secringFileLastModified;

		boolean needIncLocalRevision = false;
		long localRevision;
		synchronized (gpgProperties) {
			localRevision = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_LOCAL_REVISION, -1L);
			if (localRevision < 0)
				needIncLocalRevision = true;
			else {
				long oldPubringFileLastModified = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_PUBRING_FILE_LAST_MODIFIED, 0L);
				long oldSecringFileLastModified = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_SECRING_FILE_LAST_MODIFIED, 0L);

				if (oldPubringFileLastModified != pubringFileLastModified || oldSecringFileLastModified != secringFileLastModified)
					needIncLocalRevision = true;
			}
		}
		if (needIncLocalRevision)
			return incLocalRevision();
		else
			return localRevision;
	}

	private long incLocalRevision() {
		final Properties gpgProperties = getGpgProperties();

		loadIfNeeded();
		long pubringFileLastModified = this.pubringFileLastModified;
		long secringFileLastModified = this.secringFileLastModified;

		final long localRevision;
		synchronized (gpgProperties) {
			localRevision = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_LOCAL_REVISION, -1L) + 1;
			gpgProperties.setProperty(PGP_PROPERTY_KEY_LOCAL_REVISION, Long.toString(localRevision));
			gpgProperties.setProperty(PGP_PROPERTY_KEY_PUBRING_FILE_LAST_MODIFIED, Long.toString(pubringFileLastModified));
			gpgProperties.setProperty(PGP_PROPERTY_KEY_SECRING_FILE_LAST_MODIFIED, Long.toString(secringFileLastModified));
			writeGpgProperties();
		}
		firePropertyChange(PropertyEnum.localRevision, localRevision - 1, localRevision);
		return localRevision;
	}

	private static final String PGP_PROPERTY_KEY_PUBRING_FILE_LAST_MODIFIED = "pubringFileLastModified";
	private static final String PGP_PROPERTY_KEY_SECRING_FILE_LAST_MODIFIED = "secringFileLastModified";
	private static final String PGP_PROPERTY_KEY_LOCAL_REVISION = "localRevision";

	private Properties getGpgProperties() {
		if (gpgProperties == null) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getGpgPropertiesFile(), 30000);) {
				final Lock lock = lockFile.getLock();
				lock.lock();
				try {
					if (gpgProperties == null) {
						final Properties p = new Properties();
						try (final InputStream in = lockFile.createInputStream()) {
							p.load(in);
						}
						gpgProperties = p;
					}
				} finally {
					lock.unlock();
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
		return gpgProperties;
	}

	private void writeGpgProperties() {
		final Properties gpgProperties = getGpgProperties();
		synchronized (gpgProperties) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getGpgPropertiesFile(), 30000);) {
				try (final OutputStream out = lockFile.createOutputStream()) { // acquires LockFile.lock implicitly
					gpgProperties.store(out, null);
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
	}

	private Properties getLocalRevisionProperties(final PgpKeyId pgpKeyId) {
		final String pgpKeyIdRange = getPgpKeyIdRange(pgpKeyId);
		synchronized (getPgpKeyIdRangeMutex(pgpKeyIdRange)) {
			Properties properties = pgpKeyIdRange2LocalRevisionProperties.get(pgpKeyIdRange);
			if (properties == null) {
				properties = new Properties();

				try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalRevisionPropertiesFile(pgpKeyIdRange), 30000);) {
					try (final InputStream in = lockFile.createInputStream()) {
						properties.load(in);
					}
				} catch (final IOException x) {
					throw new RuntimeException(x);
				}

				pgpKeyIdRange2LocalRevisionProperties.put(pgpKeyIdRange, properties);
			}
			return properties;
		}
	}

	private void writeLocalRevisionProperties(final PgpKeyId pgpKeyId) {
		final String pgpKeyIdRange = getPgpKeyIdRange(pgpKeyId);
		synchronized (getPgpKeyIdRangeMutex(pgpKeyIdRange)) {
			Properties properties = pgpKeyIdRange2LocalRevisionProperties.get(pgpKeyIdRange);
			if (properties != null) {
				try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalRevisionPropertiesFile(pgpKeyIdRange), 30000);) {
					try (final OutputStream out = lockFile.createOutputStream()) {
						properties.store(out, null);
					}
				} catch (final IOException x) {
					throw new RuntimeException(x);
				}
			}
		}
	}

	private File getLocalRevisionPropertiesFile(final String pgpKeyIdRange) {
		assertNotNull(pgpKeyIdRange, "pgpKeyIdRange");
		final File dir = new File(getConfigDir(), "gpgLocalRevision");
		final File file = new File(dir, pgpKeyIdRange + ".properties");
		file.getParentFile().mkdirs();
		return file;
	}

	private Object getPgpKeyIdRangeMutex(final String pgpKeyIdRange) {
		assertNotNull(pgpKeyIdRange, "pgpKeyIdRange");
		synchronized (pgpKeyIdRange2Mutex) {
			Object mutex = pgpKeyIdRange2Mutex.get(pgpKeyIdRange);
			if (mutex == null) {
				mutex = pgpKeyIdRange;
				pgpKeyIdRange2Mutex.put(pgpKeyIdRange, mutex);
			}
			return mutex;
		}
	}

	private String getPgpKeyIdRange(final PgpKeyId pgpKeyId) {
		assertNotNull(pgpKeyId, "pgpKeyId");
		final int range1 = ((int) pgpKeyId.longValue()) & 0xff;
//		final int range2 = ((int) (pgpKeyId.longValue() >>> 8)) & 0xff;
//		return encodeHexStr(new byte[] { (byte)range2 }) + '/' + encodeHexStr(new byte[] { (byte)range1 });
//		return Integer.toHexString(range2) + '/' + Integer.toHexString(range1);
		return encodeHexStr(new byte[] { (byte)range1 });
	}

	@Override
	public long getLocalRevision(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
		final String pgpKeyIdRange = getPgpKeyIdRange(pgpKeyId);
		final long globalLocalRevision = getLocalRevision();

		synchronized (getPgpKeyIdRangeMutex(pgpKeyIdRange)) {
			final Properties localRevisionProperties = getLocalRevisionProperties(pgpKeyId);

			final String propertyKeyPrefix = pgpKeyId.toString() + '.';
			final String globalLocalRevisionPropertyKey = propertyKeyPrefix + "globalLocalRevision";
			final String localRevisionPropertyKey = propertyKeyPrefix + "localRevision";

			final long oldGlobalLocalRevision = getPropertyValueAsLong(localRevisionProperties, globalLocalRevisionPropertyKey, -1);
			long localRevision = getPropertyValueAsLong(localRevisionProperties, localRevisionPropertyKey, -1);

			if (globalLocalRevision != oldGlobalLocalRevision || localRevision < 0) {
				final String publicKeySha1PropertyKey = propertyKeyPrefix + "publicKeySha1";
				final String secretKeySha1PropertyKey = propertyKeyPrefix + "secretKeySha1";

				final String oldPublicKeySha1 = localRevisionProperties.getProperty(publicKeySha1PropertyKey);
				final String oldSecretKeySha1 = localRevisionProperties.getProperty(secretKeySha1PropertyKey);

				final String publicKeySha1;
				final String secretKeySha1;
				try {
					publicKeySha1 = sha1(bcPgpKey.getPublicKey().getEncoded());
					secretKeySha1 = bcPgpKey.getSecretKey() == null ? null : sha1(bcPgpKey.getSecretKey().getEncoded());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				// if no change, we only need to set the new globalLocalRevision (we always need to update this).
				localRevisionProperties.setProperty(globalLocalRevisionPropertyKey, Long.toString(globalLocalRevision));
				if (!equal(oldPublicKeySha1, publicKeySha1) || !equal(oldSecretKeySha1, secretKeySha1) || localRevision < 0) {
					localRevisionProperties.setProperty(publicKeySha1PropertyKey, publicKeySha1);

					if (isEmpty(secretKeySha1))
						localRevisionProperties.remove(secretKeySha1PropertyKey);
					else
						localRevisionProperties.setProperty(secretKeySha1PropertyKey, secretKeySha1);

					// It was changed, hence we set this key's localRevision to the current global localRevision.
					localRevision = globalLocalRevision;
					localRevisionProperties.setProperty(localRevisionPropertyKey, Long.toString(localRevision));
				}
				writeLocalRevisionProperties(pgpKeyId);
			}
			return localRevision;
		}
	}

	@Override
	public boolean testPassphrase(final PgpKey pgpKey, final char[] passphrase) throws IllegalArgumentException {
		assertNotNull(pgpKey, "pgpKey");
		assertNotNull(passphrase, "passphrase"); // empty for no passphrase! never null!
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final PGPSecretKey secretKey = bcPgpKey.getSecretKey();
		if (secretKey == null)
			throw new IllegalArgumentException("pgpKey has no secret key!");

		try {
//			secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase));
			extractPrivateKey(secretKey, passphrase);
			return true;
		} catch (PGPException e) {
			logger.debug("testPassphrase: " + e, e);
			return false;
		}
	}

	private static PGPPrivateKey extractPrivateKey(final PGPSecretKey secretKey, final char[] passphrase) throws PGPException {
		final PGPPrivateKey privateKey = secretKey.extractPrivateKey(
				new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase));

		return privateKey;
	}

	@Override
	public PgpKey createPgpKey(final CreatePgpKeyParam createPgpKeyParam) {
		assertNotNull(createPgpKeyParam, "createPgpKeyParam");
		try {
			final Pair<PGPPublicKeyRing, PGPSecretKeyRing> pair = createPGPSecretKeyRing(createPgpKeyParam);
			final PGPPublicKeyRing pgpPublicKeyRing = pair.a;
			final PGPSecretKeyRing pgpSecretKeyRing = pair.b;

			final ImportKeysResult importKeysResult = new ImportKeysResult();
			synchronized (this) {
				importPublicKeyRing(importKeysResult, pgpPublicKeyRing);
				importSecretKeyRing(importKeysResult, pgpSecretKeyRing);
			}

			final PGPSecretKey secretKey = pgpSecretKeyRing.getSecretKey();
			final PgpKey pgpKey = getPgpKey(new PgpKeyId(secretKey.getKeyID()));
			assertNotNull(pgpKey, "pgpKey");
			return pgpKey;
		} catch (IOException | NoSuchAlgorithmException | PGPException e) {
			throw new RuntimeException(e);
		}
	}

	private static final class Pair<A, B> {
		public final A a;
		public final B b;

		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}

	private Pair<PGPPublicKeyRing, PGPSecretKeyRing> createPGPSecretKeyRing(final CreatePgpKeyParam createPgpKeyParam) throws PGPException, NoSuchAlgorithmException {
		assertNotNull(createPgpKeyParam, "createPgpKeyParam");

		final List<PgpUserId> secondaryUserIds = new ArrayList<>(createPgpKeyParam.getUserIds());
		if (secondaryUserIds.isEmpty())
			throw new IllegalArgumentException("createPgpKeyParam.userIds is empty!");

		for (final PgpUserId pgpUserId : secondaryUserIds) {
			if (pgpUserId.isEmpty())
				throw new IllegalArgumentException("createPgpKeyParam.userIds contains empty element!");
		}

		final String primaryUserId = secondaryUserIds.remove(0).toString();

		logger.info("createPGPSecretKeyRing: Creating PGP key: primaryUserId='{}' algorithm='{}' strength={}",
				primaryUserId, createPgpKeyParam.getAlgorithm(), createPgpKeyParam.getStrength());

		final Date now = new Date();

		final int masterKeyAlgorithm = getMasterKeyAlgorithm(createPgpKeyParam);
		final int subKey1Algorithm = getSubKey1Algorithm(createPgpKeyParam);
		final int secretKeyEncryptionAlgorithm = SymmetricKeyAlgorithmTags.TWOFISH;
		final PgpSignatureType certificationLevel = PgpSignatureType.POSITIVE_CERTIFICATION;

		final int[] preferredHashAlgorithms = new int[] { // TODO configurable?!
				HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA1
		};

		final int[] preferredSymmetricAlgorithms = new int[] { // TODO configurable?!
				SymmetricKeyAlgorithmTags.TWOFISH, SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.BLOWFISH
		};

		// null causes an exception - empty is possible, though
		final char[] passphrase = createPgpKeyParam.getPassphrase() == null ? new char[0] : createPgpKeyParam.getPassphrase();

		logger.info("createPGPSecretKeyRing: Creating masterKeyPairGenerator...");
		final AsymmetricCipherKeyPairGenerator masterKeyPairGenerator = createAsymmetricCipherKeyPairGenerator(createPgpKeyParam, 0);

		logger.info("createPGPSecretKeyRing: Creating sub1KeyPairGenerator...");
		final AsymmetricCipherKeyPairGenerator sub1KeyPairGenerator = createAsymmetricCipherKeyPairGenerator(createPgpKeyParam, 1);


		/* Create the master (signing-only) key. */
		logger.info("createPGPSecretKeyRing: Creating masterKeyPair...");
		final BcPGPKeyPair masterKeyPair = new BcPGPKeyPair(masterKeyAlgorithm, masterKeyPairGenerator.generateKeyPair(), now);

		final PGPSignatureSubpacketGenerator masterSubpckGen = new PGPSignatureSubpacketGenerator();

		// Using KeyFlags instead of PGPKeyFlags, because the latter seem incomplete.
		masterSubpckGen.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER | KeyFlags.AUTHENTICATION);
		masterSubpckGen.setPreferredSymmetricAlgorithms(false, preferredSymmetricAlgorithms);
		masterSubpckGen.setPreferredHashAlgorithms(false, preferredHashAlgorithms);
		masterSubpckGen.setPreferredCompressionAlgorithms(false, new int[] { CompressionAlgorithmTags.ZIP });
		masterSubpckGen.setKeyExpirationTime(false, createPgpKeyParam.getValiditySeconds());


		/* Create an encryption sub-key. */
		logger.info("createPGPSecretKeyRing: Creating sub1KeyPair...");
		final BcPGPKeyPair sub1KeyPair = new BcPGPKeyPair(subKey1Algorithm, sub1KeyPairGenerator.generateKeyPair(), now);

		final PGPSignatureSubpacketGenerator sub1SubpckGen = new PGPSignatureSubpacketGenerator();

		sub1SubpckGen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
		sub1SubpckGen.setPreferredSymmetricAlgorithms(false, preferredSymmetricAlgorithms);
		sub1SubpckGen.setPreferredHashAlgorithms(false, preferredHashAlgorithms);
		sub1SubpckGen.setPreferredCompressionAlgorithms(false, new int[] { CompressionAlgorithmTags.ZIP });
		sub1SubpckGen.setKeyExpirationTime(false, createPgpKeyParam.getValiditySeconds());


		/* Create the key ring. */
		logger.info("createPGPSecretKeyRing: Creating keyRingGenerator...");
		final BcPGPDigestCalculatorProvider digestCalculatorProvider = new BcPGPDigestCalculatorProvider();
		final BcPGPContentSignerBuilder signerBuilder = new BcPGPContentSignerBuilder(masterKeyAlgorithm, HashAlgorithmTags.SHA512);
		final BcPBESecretKeyEncryptorBuilder pbeSecretKeyEncryptorBuilder = new BcPBESecretKeyEncryptorBuilder(
				secretKeyEncryptionAlgorithm, digestCalculatorProvider.get(HashAlgorithmTags.SHA512));

		// Tried SHA512 for checksumCalculator => org.bouncycastle.openpgp.PGPException: only SHA1 supported for key checksum calculations.
		final PGPDigestCalculator checksumCalculator = digestCalculatorProvider.get(HashAlgorithmTags.SHA1);

		final PGPSignatureSubpacketVector hashedSubpackets = masterSubpckGen.generate();
		final PGPSignatureSubpacketVector unhashedSubpackets = null;
		PGPKeyRingGenerator keyRingGenerator = new PGPKeyRingGenerator(
				signatureTypeFromEnum(certificationLevel),
				masterKeyPair,
				primaryUserId,
				checksumCalculator,
				hashedSubpackets,
				unhashedSubpackets,
				signerBuilder,
				pbeSecretKeyEncryptorBuilder.build(passphrase));


		/* Add encryption subkey. */
		keyRingGenerator.addSubKey(sub1KeyPair, sub1SubpckGen.generate(), null);


		/* Generate the key ring. */
		logger.info("createPGPSecretKeyRing: generateSecretKeyRing...");
		PGPSecretKeyRing secretKeyRing = keyRingGenerator.generateSecretKeyRing();

		logger.info("createPGPSecretKeyRing: generatePublicKeyRing...");
		PGPPublicKeyRing publicKeyRing = keyRingGenerator.generatePublicKeyRing();

		/* Add secondary (additional) user-IDs. */
		if (! secondaryUserIds.isEmpty()) {
			for (final PgpUserId pgpUserId : secondaryUserIds)
				publicKeyRing = addUserId(publicKeyRing, pgpUserId.toString(), secretKeyRing, passphrase, signerBuilder, hashedSubpackets, unhashedSubpackets);

			secretKeyRing = PGPSecretKeyRing.replacePublicKeys(secretKeyRing, publicKeyRing);
		}

		logger.info("createPGPSecretKeyRing: all done!");
		return new Pair<>(publicKeyRing,  secretKeyRing);
	}

	private static PGPPublicKeyRing addUserId(
			final PGPPublicKeyRing publicKeyRing, final String userId,
			final PGPSecretKeyRing secretKeyRing,
			final char[] passphrase,
			PGPContentSignerBuilder signerBuilder,
			final PGPSignatureSubpacketVector hashedSubpackets,
			final PGPSignatureSubpacketVector unhashedSubpackets) throws PGPException {
		assertNotNull(publicKeyRing, "publicKeyRing");
		assertNotNull(userId, "userId");

		final PGPPublicKey masterPublicKey = getMasterKeyOrFail(publicKeyRing);
		final PGPSecretKey masterSecretKey = secretKeyRing.getSecretKey(masterPublicKey.getKeyID());
		assertNotNull(masterSecretKey, "masterSecretKey");
		final PGPPrivateKey privateKey = extractPrivateKey(masterSecretKey, passphrase);

		final PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);

		sGen.init(PGPSignature.POSITIVE_CERTIFICATION, privateKey);

		sGen.setHashedSubpackets(hashedSubpackets);
		sGen.setUnhashedSubpackets(unhashedSubpackets);

		final PGPSignature certification = sGen.generateCertification(userId, masterPublicKey);
		final PGPPublicKey newMasterPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, certification);

		PGPPublicKeyRing result = PGPPublicKeyRing.removePublicKey(publicKeyRing, masterPublicKey);
		result = PGPPublicKeyRing.insertPublicKey(result, newMasterPublicKey);
		return result;
	}

	private static PGPPublicKey getMasterKeyOrFail(final PGPPublicKeyRing publicKeyRing) {
		for (Iterator<?> it = publicKeyRing.getPublicKeys(); it.hasNext(); ) {
			PGPPublicKey pk = (PGPPublicKey) it.next();
			if (pk.isMasterKey()) {
				return pk;
			}
		}
		throw new IllegalStateException("No masterKey found!");
	}

	private int getMasterKeyAlgorithm(final CreatePgpKeyParam createPgpKeyParam) {
		switch (createPgpKeyParam.getAlgorithm()) {
			case DSA_AND_EL_GAMAL:
				return PublicKeyAlgorithmTags.DSA;
			case RSA:
				return PublicKeyAlgorithmTags.RSA_SIGN;
			default:
				throw new IllegalStateException("Unknown algorithm: " + createPgpKeyParam.getAlgorithm());
		}
	}

	private int getSubKey1Algorithm(final CreatePgpKeyParam createPgpKeyParam) {
		switch (createPgpKeyParam.getAlgorithm()) {
			case DSA_AND_EL_GAMAL:
				return PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT; // ELGAMAL_GENERAL does not work - Thunderbird/Enigmail says it wouldn't find a suitable sub-key.
			case RSA:
				return PublicKeyAlgorithmTags.RSA_ENCRYPT; // RSA_GENERAL and RSA_ENCRYPT both work.
			default:
				throw new IllegalStateException("Unknown algorithm: " + createPgpKeyParam.getAlgorithm());
		}
	}

	private AsymmetricCipherKeyPairGenerator createAsymmetricCipherKeyPairGenerator(final CreatePgpKeyParam createPgpKeyParam, final int keyIndex) throws NoSuchAlgorithmException {
//		final CryptoRegistry cryptoRegistry = CryptoRegistry.getInstance();
//		final AsymmetricCipherKeyPairGenerator keyPairGenerator;
//		switch (createPgpKeyParam.getAlgorithm()) {
//			case DSA_AND_EL_GAMAL:
//				if (keyIndex == 0) { // master-key
//					keyPairGenerator = cryptoRegistry.createKeyPairGenerator("DSA", false);
//					keyPairGenerator.init(createDsaKeyGenerationParameters(createPgpKeyParam));
//				}
//				else { // sub-key 1
//					keyPairGenerator = cryptoRegistry.createKeyPairGenerator("ElGamal", false);
//					keyPairGenerator.init(createElGamalKeyGenerationParameters(createPgpKeyParam));
//				}
//				break;
//			case RSA:
//				keyPairGenerator = cryptoRegistry.createKeyPairGenerator("RSA", false);
//				keyPairGenerator.init(createRsaKeyGenerationParameters(createPgpKeyParam));
//				break;
//			default:
//				throw new IllegalStateException("Unknown algorithm: " + createPgpKeyParam.getAlgorithm());
//		}
//		return keyPairGenerator;
		throw new UnsupportedOperationException("I do not want to port the CryptoRegistry and it is probably not needed!");
	}

//	private DSAKeyGenerationParameters createDsaKeyGenerationParameters(final CreatePgpKeyParam createPgpKeyParam) {
//		/*
//		 * How certain do we want to be that the chosen primes are really primes.
//		 * <p>
//		 * The higher this number, the more tests are done to make sure they are primes (and not composites).
//		 * <p>
//		 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
//		 * and
//		 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
//		 */
//		final int certainty = 12;
//
//		final SecureRandom random = getSecureRandom();
//
//		final DSAParametersGenerator pGen = new DSAParametersGenerator();
//		pGen.init(createPgpKeyParam.getStrength(), certainty, random);
//		final DSAParameters dsaParameters = pGen.generateParameters();
//		return new DSAKeyGenerationParameters(random, dsaParameters);
//	}
//
//	private ElGamalKeyGenerationParameters createElGamalKeyGenerationParameters(final CreatePgpKeyParam createPgpKeyParam) {
//		/*
//		 * How certain do we want to be that the chosen primes are really primes.
//		 * <p>
//		 * The higher this number, the more tests are done to make sure they are primes (and not composites).
//		 * <p>
//		 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
//		 * and
//		 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
//		 */
//		final int certainty = 8; // 12 takes ages - and DSA+El-Gamal is anyway a bad idea and discouraged. Reducing this to make it bearable.
//
//		final SecureRandom random = getSecureRandom();
//
//		ElGamalParametersGenerator pGen = new ElGamalParametersGenerator();
//		pGen.init(createPgpKeyParam.getStrength(), certainty, random);
//		ElGamalParameters elGamalParameters = pGen.generateParameters();
//
//		// Maybe we should generate our "DH safe primes" only once and store them somewhere? Or maybe we should provide a long list
//		// of them in the resources? DHParametersHelper.generateSafePrimes(size, certainty, random); takes really really very long.
//		// BUT BEWARE: Attacks on El Gamal can re-use expensively calculated stuff, if p (one of the "safe primes) is the same.
//		// However, it is still not *so* easy. Hmmm... don't know. Security is really important here.
//
//		return new ElGamalKeyGenerationParameters(random, elGamalParameters);
//	}
//
//	private RSAKeyGenerationParameters createRsaKeyGenerationParameters(final CreatePgpKeyParam createPgpKeyParam) {
//		/*
//		 * This value should be a Fermat number. 0x10001 (F4) is current recommended value. 3 (F1) is known to be safe also.
//		 * 3, 5, 17, 257, 65537, 4294967297, 18446744073709551617,
//		 * <p>
//		 * Practically speaking, Windows does not tolerate public exponents which do not fit in a 32-bit unsigned integer.
//		 * Using e=3 or e=65537 works "everywhere".
//		 * <p>
//		 * See: <a href="http://stackoverflow.com/questions/11279595/rsa-public-exponent-defaults-to-65537-what-should-this-value-be-what-are-the">stackoverflow: RSA Public exponent defaults to 65537. ... What are the impacts of my choices?</a>
//		 */
//		final BigInteger publicExponent = BigInteger.valueOf(0x10001);
//
//		/*
//		 * How certain do we want to be that the chosen primes are really primes.
//		 * <p>
//		 * The higher this number, the more tests are done to make sure they are primes (and not composites).
//		 * <p>
//		 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
//		 * and
//		 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
//		 */
//		final int certainty = 12;
//
//		return new RSAKeyGenerationParameters(
//				publicExponent, getSecureRandom(), createPgpKeyParam.getStrength(), certainty);
//	}

	private synchronized SecureRandom getSecureRandom() {
		if (secureRandom == null)
			secureRandom = new SecureRandom();

		return secureRandom;
	}

	protected PgpKeyRegistry getPgpKeyRegistry()
	{
		if (pgpKeyRegistry == null) {
			try {
				pgpKeyRegistry = PgpKeyRegistry.Helper.createInstance(
						new IoFile(getPubringFile()), new IoFile(getSecringFile()));
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}

		return pgpKeyRegistry;
	}

	protected synchronized TrustDbFactory getTrustDbFactory() {
		if (trustDbFactory == null)
			trustDbFactory = new TrustDbFactory(getTrustDbFile(), getPgpKeyRegistry());

		return trustDbFactory;
	}

	protected PgpRegistry getPgpRegistry() {
		return PgpRegistry.getInstance();
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallback() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = getPgpRegistry().getPgpAuthenticationCallback();
		return pgpAuthenticationCallback;
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallbackOrFail() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = getPgpAuthenticationCallback();
		if (pgpAuthenticationCallback == null)
			throw new IllegalStateException("There is no PgpAuthenticationCallback assigned!");

		return pgpAuthenticationCallback;
	}

	protected TrustDb createTrustDb() {
		final TrustDbFactory trustDbFactory = getTrustDbFactory();
		return trustDbFactory.createTrustDb();
	}

	@Override
	public void certify(final CertifyPgpKeyParam certifyPgpKeyParam) {
		try {
			assertNotNull(certifyPgpKeyParam, "certifyPgpKeyParam");
			PgpKey pgpKey = assertNotNull(certifyPgpKeyParam.getPgpKey(), "certifyPgpKeyParam.pgpKey");
			PgpKey signPgpKey = assertNotNull(certifyPgpKeyParam.getSignPgpKey(), "certifyPgpKeyParam.signPgpKey");
			final PgpSignatureType certificationLevel = assertNotNull(certifyPgpKeyParam.getCertificationLevel(), "certifyPgpKeyParam.certificationLevel");
			final HashAlgorithm hashAlgorithm = assertNotNull(certifyPgpKeyParam.getHashAlgorithm(), "certifyPgpKeyParam.hashAlgorithm");

			if (pgpKey.getMasterKey() != null)
				pgpKey = pgpKey.getMasterKey();

			if (signPgpKey.getMasterKey() != null)
				signPgpKey = signPgpKey.getMasterKey();

			final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
			final BcPgpKey bcSignPgpKey = getBcPgpKeyOrFail(signPgpKey);

			if (! PgpSignatureType.CERTIFICATIONS.contains(certificationLevel))
				throw new IllegalArgumentException("certifyPgpKeyParam.certificationLevel is not contained in PgpSignatureType.CERTIFICATIONS.");

			final PgpAuthenticationCallback callback = getPgpAuthenticationCallbackOrFail();
			final char[] signPassphrase = callback.getPassphrase(signPgpKey);

			PGPPublicKeyRing publicKeyRing = bcPgpKey.getPublicKeyRing();

			final PGPSecretKey signSecretKey = bcSignPgpKey.getSecretKey();
			if (signSecretKey == null)
				throw new IllegalArgumentException("signPgpKey does not have a secret key assigned!");

			final PGPPrivateKey signPrivKey = signSecretKey.extractPrivateKey(
					new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(signPassphrase));

			final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
					new BcPGPContentSignerBuilder(signSecretKey.getPublicKey().getAlgorithm(), hashAlgorithm.getHashAlgorithmTag()));

			signatureGenerator.init(signatureTypeFromEnum(certificationLevel), signPrivKey);

			if (pgpKey.getUserIds().isEmpty())
				throw new IllegalArgumentException("certifyPgpKeyParam.pgpKey.userIds is empty!");

			for (final String userId : pgpKey.getUserIds()) {
				final PGPPublicKey publicKey = publicKeyRing.getPublicKey();
				PGPSignature signature = signatureGenerator.generateCertification(userId, publicKey);
				PGPPublicKey newKey = PGPPublicKey.addCertification(publicKey, userId, signature);
				PGPPublicKeyRing newPublicKeyRing = PGPPublicKeyRing.removePublicKey(publicKeyRing, publicKey);
				newPublicKeyRing = PGPPublicKeyRing.insertPublicKey(newPublicKeyRing, newKey);
				publicKeyRing = newPublicKeyRing;
			}

			final ImportKeysResult importKeysResult = new ImportKeysResult();
			final boolean modified = importPublicKeyRing(importKeysResult, publicKeyRing);

			if (modified) // make sure the localRevision is incremented, even if the timestamp does not change (e.g. because the time resolution of the file system is too low).
				incLocalRevision();

		} catch (IOException | PGPException e) {
			throw new RuntimeException(e);
		}
	}
}
