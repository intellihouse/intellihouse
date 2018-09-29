package house.intelli.raspi.pv;

import static java.util.Objects.*;

import java.util.EventObject;

import house.intelli.raspi.PvDataCollector;
import house.intelli.raspi.pv.steca.InverterMode;
import house.intelli.raspi.pv.steca.InverterStatus;

public class DataCollectorEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	private final InverterMode inverterMode;
	private final InverterStatus inverterStatus;
	private final Throwable error;
	private final int consecutiveErrorCount;

	public DataCollectorEvent(PvDataCollector source, final InverterMode inverterMode, final InverterStatus inverterStatus) {
		super(source);
		this.inverterMode = requireNonNull(inverterMode, "inverterMode");
		this.inverterStatus = requireNonNull(inverterStatus, "inverterStatus");
		this.error = null;
		this.consecutiveErrorCount = 0;
	}

	public DataCollectorEvent(PvDataCollector source, final Throwable error, int consecutiveErrorCount) {
		super(source);
		this.inverterMode = null;
		this.inverterStatus = null;
		this.error = requireNonNull(error, "error");
		this.consecutiveErrorCount = consecutiveErrorCount;
	}

	@Override
	public PvDataCollector getSource() {
		return (PvDataCollector) super.getSource();
	}

	public InverterMode getInverterMode() {
		return inverterMode;
	}

	public InverterStatus getInverterStatus() {
		return inverterStatus;
	}

	public Throwable getError() {
		return error;
	}

	public int getConsecutiveErrorCount() {
		return consecutiveErrorCount;
	}
}
