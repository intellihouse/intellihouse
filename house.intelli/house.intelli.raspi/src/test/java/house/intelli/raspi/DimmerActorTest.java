package house.intelli.raspi;

import static org.assertj.core.api.Assertions.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import house.intelli.core.util.AssertUtil;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class DimmerActorTest {

	private DimmerActorImpl dimmerActorImpl;

	@BeforeClass
	public static void beforeDimmerActorTest() {
		new MockUp<AssertUtil>(AssertUtil.class) {
			@Mock
			void assertEventThread() {
				// nothing!
			}
		};
	}

	@Before
	public void before() {
		dimmerActorImpl = new DimmerActorImpl() {
			@Override
			protected void applyDimmerValue() {
				// We have no hardware!
				int pwm = getPwm();
				System.out.println(pwm);
			}
		};
	}

	@After
	public void after() {
		if (dimmerActorImpl != null)
			dimmerActorImpl.close();
	}

	@Test
	public void dimmerValue0() {
		dimmerActorImpl.setDimmerValue(0);
		assertThat(dimmerActorImpl.getDimmerValue()).isEqualTo(0);
	}

	@Test
	public void dimmerValue100() {
		dimmerActorImpl.setDimmerValue(100);
		assertThat(dimmerActorImpl.getDimmerValue()).isEqualTo(100);
	}

	@Test(expected=IllegalArgumentException.class)
	public void dimmerValue101() {
		dimmerActorImpl.setDimmerValue(101);
	}

	@Test(expected=IllegalArgumentException.class)
	public void dimmerValue_1() {
		dimmerActorImpl.setDimmerValue(-1);
	}

	@Test
	public void dimmerValue() {
		int dimmerValue = 87;
		dimmerActorImpl.setDimmerValue(dimmerValue);
		assertThat(dimmerActorImpl.getDimmerValue()).isEqualTo(dimmerValue);
	}

	@Test
	public void dimmerValues() {
		for (int dimmerValue : LightControllerImpl.LIGHT_DIMMER_VALUES) {
			dimmerActorImpl.setDimmerValue(dimmerValue);
			assertThat(dimmerActorImpl.getDimmerValue()).isEqualTo(dimmerValue);
		}
	}

}
