package maspack.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager of multiple threads, with primary and secondary thread pools. 
 * Tasks submitted to primary threads SHOULD NOT DEPEND ON EACH OTHER.
 * This is the purpose of the secondary thread pool.
 * Secondary threads should be reserved for tasks upon which primary
 * threads depend on (e.g. monitoring).  
 * 
 * @author Antonio
 *
 */
public class MultiThreadManager implements ExecutorService {

	ThreadPoolExecutor _mainExecutorService;
	ThreadPoolExecutor _secondaryExecutorService;

	LinkedList<Future<?>> _mainFutures = new LinkedList<Future<?>>();

	/**
	 * Creates primary and secondary thread pools
	 * @param name name of the thread manager
	 * @param nPrimaryThreads fixed number of threads in the primary pool
	 * @param nSecondaryThreads maximum number of threads in the secondary pool
	 * @param timeoutMS thread time-out
	 */
	public MultiThreadManager(String name, int nPrimaryThreads,
			int nSecondaryThreads, long timeoutMS) {
		
		_mainExecutorService = new ThreadPoolExecutor(nPrimaryThreads,
				nPrimaryThreads, timeoutMS, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(
						name));

		_secondaryExecutorService = new ThreadPoolExecutor(0,
				nSecondaryThreads, timeoutMS, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), new NamedThreadFactory(name
						+ "(secondary)"));
	}

	/**
	 * Executes the supplied command at some time in the future.
	 */
	public void execute(Runnable command) {
		_mainFutures.add(_mainExecutorService.submit(command));
	}
	
	public void execute(Callable<?> command) {
		_mainFutures.add(_mainExecutorService.submit(command));
	}

	public Future<?> executeSecondary(Runnable command) {
		return _secondaryExecutorService.submit(command);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return _mainExecutorService.submit(task);
	}

	public <T> Future<T> submitSecondary(Callable<T> task) {
		return _secondaryExecutorService.submit(task);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return _mainExecutorService.submit(task, result);
	}

	public <T> Future<T> submitSecondary(Runnable task, T result) {
		return _secondaryExecutorService.submit(task, result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return _mainExecutorService.submit(task);
	}

	public Future<?> submitSecondary(Runnable task) {
		return _secondaryExecutorService.submit(task);
	}

	public Future<?> getNextCompleted() {
		Iterator<Future<?>> it = _mainFutures.iterator();
		while (it.hasNext()) {
			Future<?> fut = it.next();
			if (fut.isDone()) {
				it.remove();
				return fut;
			}
		}
		return null;
	}

	public Future<?> awaitNextCompleted() {
		while (true) {
			Iterator<Future<?>> it = _mainFutures.iterator();
			for (;;) {
				Future<?> fut = it.next();
				if (fut.isDone()) {
					it.remove();
					return fut;
				}
			}
		}
	}

	@Override
	public void shutdown() {
		_mainExecutorService.shutdown();
		_secondaryExecutorService.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		ArrayList<Runnable> out = new ArrayList<Runnable>();
		out.addAll(_mainExecutorService.shutdownNow());
		out.addAll(_secondaryExecutorService.shutdownNow());
		return out;
	}

	@Override
	public boolean isShutdown() {
		return (_mainExecutorService.isShutdown());
	}

	@Override
	public boolean isTerminated() {
		return (_mainExecutorService.isTerminated());
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return _mainExecutorService.awaitTermination(timeout, unit);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return _mainExecutorService.invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return _mainExecutorService.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return _mainExecutorService.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return _mainExecutorService.invokeAny(tasks, timeout, unit);
	}
}
