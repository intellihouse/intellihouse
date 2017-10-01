package house.intelli.raspi;

import java.util.EventObject;

@SuppressWarnings("serial")
public class AutoOffEvent extends EventObject {
	public AutoOffEvent(AutoOff source) {
		super(source);
	}
	@Override
	public AutoOff getSource() {
		return (AutoOff) super.getSource();
	}
}
