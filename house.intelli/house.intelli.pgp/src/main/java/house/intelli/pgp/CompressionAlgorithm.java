package house.intelli.pgp;

import org.bouncycastle.bcpg.CompressionAlgorithmTags;

public enum CompressionAlgorithm {

	UNCOMPRESSED(CompressionAlgorithmTags.UNCOMPRESSED),
	ZIP(CompressionAlgorithmTags.ZIP),
	ZLIB(CompressionAlgorithmTags.ZLIB),
	BZIP2(CompressionAlgorithmTags.BZIP2);

	private final int compressionAlgorithmTag;

	private CompressionAlgorithm(final int compressionAlgorithmTag) {
		this.compressionAlgorithmTag = compressionAlgorithmTag;
	}

	public int getCompressionAlgorithmTag() {
		return compressionAlgorithmTag;
	}
}
