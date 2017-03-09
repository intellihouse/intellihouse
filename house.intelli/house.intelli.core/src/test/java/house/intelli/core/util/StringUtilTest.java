package house.intelli.core.util;

import static house.intelli.core.util.StringUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class StringUtilTest {

	@Test
	public void split0() {
		List<String> split = split(null, ':');
		assertThat(split).isEmpty();

		split = split("", ':');
		assertThat(split).isEmpty();
	}

	@Test
	public void split1() {
		List<String> split = split("aaa", ':');
		assertThat(split).isEqualTo(Arrays.asList("aaa"));

		split = split("a- bc", ':');
		assertThat(split).isEqualTo(Arrays.asList("a- bc"));
	}

	@Test
	public void split2() {
		List<String> split = split("abc:d ef", ':');
		assertThat(split).isEqualTo(Arrays.asList("abc", "d ef"));

		split = split(" abc : d ef ", ':');
		assertThat(split).isEqualTo(Arrays.asList(" abc ", " d ef "));
	}

	@Test
	public void split3() {
		List<String> split = split("abc!d ef !xxx", '!');
		assertThat(split).isEqualTo(Arrays.asList("abc", "d ef ", "xxx"));

		split = split(" abc , d ef , x: y:z", ',');
		assertThat(split).isEqualTo(Arrays.asList(" abc ", " d ef ", " x: y:z"));
	}

}
