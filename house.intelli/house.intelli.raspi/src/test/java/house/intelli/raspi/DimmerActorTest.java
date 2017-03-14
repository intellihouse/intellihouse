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

	private DimmerActor dimmerActor;

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
		dimmerActor = new DimmerActor() {
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
		if (dimmerActor != null)
			dimmerActor.close();
	}

	@Test
	public void dimmerValue0() {
		dimmerActor.setDimmerValue(0);
		assertThat(dimmerActor.getDimmerValue()).isEqualTo(0);
	}

	@Test
	public void dimmerValue100() {
		dimmerActor.setDimmerValue(100);
		assertThat(dimmerActor.getDimmerValue()).isEqualTo(100);
	}

	@Test(expected=IllegalArgumentException.class)
	public void dimmerValue101() {
		dimmerActor.setDimmerValue(101);
	}

	@Test(expected=IllegalArgumentException.class)
	public void dimmerValue_1() {
		dimmerActor.setDimmerValue(-1);
	}

	@Test
	public void dimmerValue() {
		int dimmerValue = 87;
		dimmerActor.setDimmerValue(dimmerValue);
		assertThat(dimmerActor.getDimmerValue()).isEqualTo(dimmerValue);
	}

	@Test
	public void dimmerValues() {
		for (int dimmerValue : LightController.LIGHT_DIMMER_VALUES) {
			dimmerActor.setDimmerValue(dimmerValue);
			assertThat(dimmerActor.getDimmerValue()).isEqualTo(dimmerValue);
		}
	}

}
