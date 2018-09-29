package house.intelli.raspi.pv;

import java.util.EventListener;

public interface DataCollectorListener extends EventListener {

	void onSuccess(DataCollectorEvent event);

	void onError(DataCollectorEvent event);

}
