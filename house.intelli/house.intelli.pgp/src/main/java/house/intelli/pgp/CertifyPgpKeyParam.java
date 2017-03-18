package house.intelli.pgp;

import java.io.Serializable;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;

public class CertifyPgpKeyParam extends AbstractBean<CertifyPgpKeyParam.Property> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		pgpKey,
		signPgpKey,
		certificationLevel,
		hashAlgorithm
	}

	private PgpKey pgpKey;
	private PgpKey signPgpKey;
	private PgpSignatureType certificationLevel;
	private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA256;

	public CertifyPgpKeyParam() {
	}

	public PgpKey getPgpKey() {
		return pgpKey;
	}
	public void setPgpKey(PgpKey pgpKey) {
		setPropertyValue(PropertyEnum.pgpKey, pgpKey);
	}

	public PgpKey getSignPgpKey() {
		return signPgpKey;
	}
	public void setSignPgpKey(PgpKey signPgpKey) {
		setPropertyValue(PropertyEnum.signPgpKey, signPgpKey);
	}

	public PgpSignatureType getCertificationLevel() {
		return certificationLevel;
	}
	public void setCertificationLevel(PgpSignatureType certificationLevel) {
		setPropertyValue(PropertyEnum.certificationLevel, certificationLevel);
	}

	public HashAlgorithm getHashAlgorithm() {
		return hashAlgorithm;
	}
	public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		setPropertyValue(PropertyEnum.hashAlgorithm, hashAlgorithm);
	}
}
