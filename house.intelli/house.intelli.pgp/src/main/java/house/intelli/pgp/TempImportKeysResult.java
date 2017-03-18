package house.intelli.pgp;

import static house.intelli.core.util.AssertUtil.*;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TempImportKeysResult implements Serializable {

	private final Pgp tempPgp;
	private final ImportKeysResult importKeysResult;

	public TempImportKeysResult(Pgp tempPgp, ImportKeysResult importKeysResult) {
		this.tempPgp = assertNotNull(tempPgp, "tempPgp");
		this.importKeysResult = assertNotNull(importKeysResult, "importKeysResult");
	}

	public Pgp getTempPgp() {
		return tempPgp;
	}

	public ImportKeysResult getImportKeysResult() {
		return importKeysResult;
	}
}
