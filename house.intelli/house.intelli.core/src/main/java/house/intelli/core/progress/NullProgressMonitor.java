package house.intelli.core.progress;

/**
 * @author Alexander Bieber <!-- alex [AT] nightlabs [DOT] de -->
 */
public class NullProgressMonitor implements ProgressMonitor {

	private volatile boolean canceled;

	public NullProgressMonitor() { }

	@Override
	public void beginTask(String name, int totalWork) { }

	@Override
	public void done() { }

	@Override
	public void setTaskName(String name) { }

	@Override
	public void subTask(String name) { }

	@Override
	public void worked(int work) { }

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	@Override
	public void internalWorked(double worked) { }
}
