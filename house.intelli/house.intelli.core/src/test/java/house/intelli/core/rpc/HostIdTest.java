package house.intelli.core.rpc;

import org.junit.Test;

public class HostIdTest {

	@Test
	public void getLocalHostId() throws Exception {
		HostId localHostId = HostId.getLocalHostId();
		System.out.println("localHostId=" + localHostId);
	}

}
