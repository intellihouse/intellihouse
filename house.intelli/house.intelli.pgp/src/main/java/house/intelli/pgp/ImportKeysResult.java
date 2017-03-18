package house.intelli.pgp;

import static house.intelli.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class ImportKeysResult implements Serializable {

	private final Map<PgpKeyId, ImportedMasterKey> pgpKeyId2ImportedMasterKey = new HashMap<>();

	public ImportKeysResult() {
	}

	public Map<PgpKeyId, ImportedMasterKey> getPgpKeyId2ImportedMasterKey() {
		return pgpKeyId2ImportedMasterKey;
	}

	public static class ImportedKey implements Serializable {
		private static final long serialVersionUID = 1L;

		private final PgpKeyId pgpKeyId;

		public ImportedKey(final PgpKeyId pgpKeyId) {
			this.pgpKeyId = assertNotNull(pgpKeyId, "pgpKeyId");
		}

		public PgpKeyId getPgpKeyId() {
			return pgpKeyId;
		}
	}

	public static class ImportedMasterKey extends ImportedKey {
		private static final long serialVersionUID = 1L;

		private final Map<PgpKeyId, ImportedSubKey> pgpKeyId2ImportedSubKey = new HashMap<>();

		public ImportedMasterKey(PgpKeyId pgpKeyId) {
			super(pgpKeyId);
		}

		public Map<PgpKeyId, ImportedSubKey> getPgpKeyId2ImportedSubKey() {
			return pgpKeyId2ImportedSubKey;
		}
	}

	public static class ImportedSubKey extends ImportedKey {
		private static final long serialVersionUID = 1L;

		private final ImportedMasterKey importedMasterKey;

		public ImportedSubKey(PgpKeyId pgpKeyId, ImportedMasterKey importedMasterKey) {
			super(pgpKeyId);
			this.importedMasterKey = assertNotNull(importedMasterKey, "importedMasterKey");
		}

		public ImportedMasterKey getImportedMasterKey() {
			return importedMasterKey;
		}
	}
}
