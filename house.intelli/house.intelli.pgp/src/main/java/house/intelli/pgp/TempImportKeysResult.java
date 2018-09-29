package house.intelli.pgp;

import static java.util.Objects.*;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TempImportKeysResult implements Serializable {

	private final Pgp tempPgp;
	private final ImportKeysResult importKeysResult;

	public TempImportKeysResult(Pgp tempPgp, ImportKeysResult importKeysResult) {
		this.tempPgp = requireNonNull(tempPgp, "tempPgp");
		this.importKeysResult = requireNonNull(importKeysResult, "importKeysResult");
	}

	public Pgp getTempPgp() {
		return tempPgp;
	}

	public ImportKeysResult getImportKeysResult() {
		return importKeysResult;
	}
}
