package house.intelli.pgp;

import org.bouncycastle.bcpg.HashAlgorithmTags;

public enum HashAlgorithm {

	MD5(HashAlgorithmTags.MD5),
    SHA1(HashAlgorithmTags.SHA1),
    RIPEMD160(HashAlgorithmTags.RIPEMD160),
    DOUBLE_SHA(HashAlgorithmTags.DOUBLE_SHA),
    MD2(HashAlgorithmTags.MD2),
    TIGER_192(HashAlgorithmTags.TIGER_192),
    HAVAL_5_160(HashAlgorithmTags.HAVAL_5_160),

    SHA256(HashAlgorithmTags.SHA256),
    SHA384(HashAlgorithmTags.SHA384),
    SHA512(HashAlgorithmTags.SHA512),
    SHA224(HashAlgorithmTags.SHA224);

	private final int hashAlgorithmTag;

	private HashAlgorithm(final int hashAlgorithmTag) {
		this.hashAlgorithmTag = hashAlgorithmTag;
	}

	public int getHashAlgorithmTag() {
		return hashAlgorithmTag;
	}
}
