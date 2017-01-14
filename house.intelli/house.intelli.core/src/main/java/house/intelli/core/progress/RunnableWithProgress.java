package house.intelli.core.progress;

public interface RunnableWithProgress {
	void run(ProgressMonitor monitor) throws Exception;
}
