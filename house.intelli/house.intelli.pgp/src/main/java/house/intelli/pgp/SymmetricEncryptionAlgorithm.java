package house.intelli.pgp;

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;

public enum SymmetricEncryptionAlgorithm {

	NULL(SymmetricKeyAlgorithmTags.NULL),
    IDEA(SymmetricKeyAlgorithmTags.IDEA),
    TRIPLE_DES(SymmetricKeyAlgorithmTags.TRIPLE_DES),
    CAST5(SymmetricKeyAlgorithmTags.CAST5),
    BLOWFISH(SymmetricKeyAlgorithmTags.BLOWFISH),
    SAFER(SymmetricKeyAlgorithmTags.SAFER),
    DES(SymmetricKeyAlgorithmTags.DES),
    AES_128(SymmetricKeyAlgorithmTags.AES_128),
    AES_192(SymmetricKeyAlgorithmTags.AES_192),
    AES_256(SymmetricKeyAlgorithmTags.AES_256),
	TWOFISH(SymmetricKeyAlgorithmTags.TWOFISH);

	private final int symmetricKeyAlgorithmTag;

	private SymmetricEncryptionAlgorithm(final int symmetricKeyAlgorithmTag) {
		this.symmetricKeyAlgorithmTag = symmetricKeyAlgorithmTag;
	}

	public int getSymmetricKeyAlgorithmTag() {
		return symmetricKeyAlgorithmTag;
	}
}
