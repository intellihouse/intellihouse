package house.intelli.pgp.rpc;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.security.SecureRandom;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgpTransportSupportTest {
	private static final Logger logger = LoggerFactory.getLogger(PgpTransportSupportTest.class);

	private static final SecureRandom random = new SecureRandom();

	@Test
	public void hashTest() throws Exception {
		final byte[] plainData = new byte[10 + random.nextInt(100000)];
		random.nextBytes(plainData);

		byte[] plainDataWithHash = PgpTransportSupport.combinePlainDataWithHash(plainData);
		byte[] plainData2 = PgpTransportSupport.splitPlainDataFromHashWithVerification(plainDataWithHash);
		assertThat(plainData2).isEqualTo(plainData);

		int byteIndex = random.nextInt(plainDataWithHash.length);

		if (plainDataWithHash[byteIndex] == 0)
			plainDataWithHash[byteIndex] = 1;
		else
			plainDataWithHash[byteIndex] = 0;

		try {
			PgpTransportSupport.splitPlainDataFromHashWithVerification(plainDataWithHash);
			fail("Data corruption was not detected!");
		} catch (IOException x) {
			logger.info("Data corruption was properly detected: " + x);
		}
	}

}
