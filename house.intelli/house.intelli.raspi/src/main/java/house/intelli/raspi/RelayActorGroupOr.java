package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

public class RelayActorGroupOr extends RelayActorGroup {

	@Override
	protected void onInputEnergizedChanged(RelayActor unused, boolean oldEnergized, boolean newEnergized) {
		assertEventThread();

		RelayActor outputRelayActor = getOutput();
		if (outputRelayActor == null)
			return;

		if (newEnergized) // as soon as 1 input switches to true, we must switch the output to true -- no need to check the others.
			outputRelayActor.setEnergized(true);
		else { // if an input switches to false, we must check the others -- maybe another one is still true.
			boolean outputEnergized = false;
			for (RelayActor inputRelayActor : getInput()) {
				if (inputRelayActor.isEnergized()) {
					outputEnergized = true;
					break;
				}
			}
			outputRelayActor.setEnergized(outputEnergized);
		}
	}

}
