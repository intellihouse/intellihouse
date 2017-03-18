package house.intelli.core.io;

import static house.intelli.core.util.Util.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NoCloseInputStream extends FilterInputStream {

	public NoCloseInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void close() throws IOException {
		doNothing();
	}
}
