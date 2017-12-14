package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

public class RelayActorGroupAnd extends RelayActorGroup {

	@Override
	protected void onInputEnergizedChanged(RelayActor unused, boolean oldEnergized, boolean newEnergized) {
		assertEventThread();

		RelayActor outputRelayActor = getOutput();
		if (outputRelayActor == null)
			return;

		if (! newEnergized) // as soon as 1 input switches to false, we must switch the output to false -- no need to check the others.
			outputRelayActor.setEnergized(false);
		else { // if an input switches to true, we must check the others -- maybe another one is still false.
			boolean outputEnergized = true;
			for (RelayActor inputRelayActor : getInput()) {
				if (! inputRelayActor.isEnergized()) {
					outputEnergized = false;
					break;
				}
			}
			outputRelayActor.setEnergized(outputEnergized);
		}
	}

}
