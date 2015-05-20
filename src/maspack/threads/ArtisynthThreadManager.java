package maspack.threads;

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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ArtisynthThreadManager implements ExecutorService {

   ThreadPoolExecutor _mainExecutorService;
   LinkedList<Future<?>> _mainFutures = new LinkedList<Future<?>>();
   
   static ExecutorService staticManager = null;
   static long defaultTimeoutMS = 1000;  // close thread after 1s of no use
   
   static ExecutorService getMainManager() {
      if (staticManager == null) {
         int nthreads = getDefaultNumThreads();
         staticManager = new ArtisynthThreadManager("Artisynth", nthreads, defaultTimeoutMS);
      }
      return staticManager;
   }
   
   static void setMainManager(ExecutorService manager) {
      staticManager = manager;
   }
   
   public static int getDefaultNumThreads() {
      int nthreads = -11;
      String nstr = System.getenv("ARTISYNTH_NUM_THREADS");
      if (nstr != null && !"".equals(nstr)) {
         try {
            nthreads = Integer.parseInt(nstr);
         } catch (Exception e) {
            nthreads = -1;
         }
      } 
      if (nthreads < 1) {
         nstr = System.getenv("OMP_NUM_THREADS");
         try {
            nthreads = Integer.parseInt(nstr);
         } catch (Exception e) {
            nthreads = -1;
         }
      }
      if (nthreads < 1) {
         nthreads = 1;
      }
      return nthreads;
   }
   
   /**
    * Creates primary thread pool
    * 
    * @param name
    * name of the thread manager
    */
   public ArtisynthThreadManager(String name) {
      this(name, getDefaultNumThreads(), defaultTimeoutMS);
   }
   
   /**
    * Creates primary thread pool
    * 
    * @param name
    * name of the thread manager
    * @param nThreads
    * fixed number of threads in the primary pool
    */
   public ArtisynthThreadManager(String name, int nThreads) {
      this(name, nThreads, defaultTimeoutMS);
   }

   /**
    * Creates primary thread pool
    * 
    * @param name
    * name of the thread manager
    * @param nThreads
    * fixed number of threads in the primary pool
    * @param timeoutMS
    * thread time-out
    */
   public ArtisynthThreadManager(String name, int nThreads,
     long timeoutMS) {

      _mainExecutorService =
         new ThreadPoolExecutor(
            nThreads, nThreads, timeoutMS, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(name));
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

   @Override
   public <T> Future<T> submit(Callable<T> task) {
      return _mainExecutorService.submit(task);
   }

   @Override
   public <T> Future<T> submit(Runnable task, T result) {
      return _mainExecutorService.submit(task, result);
   }

   @Override
   public Future<?> submit(Runnable task) {
      return _mainExecutorService.submit(task);
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
   }

   @Override
   public List<Runnable> shutdownNow() {
      ArrayList<Runnable> out = new ArrayList<Runnable>();
      out.addAll(_mainExecutorService.shutdownNow());
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
   public <T> T invokeAny(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
      return _mainExecutorService.invokeAny(tasks, timeout, unit);
   }

}
