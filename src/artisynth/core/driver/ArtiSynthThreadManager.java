package artisynth.core.driver;

import java.util.concurrent.ExecutorService;

import maspack.concurrency.SimpleThreadManager;

public class ArtiSynthThreadManager extends SimpleThreadManager {

   static ExecutorService staticManager = null;
   
   static ExecutorService getMainManager() {
      if (staticManager == null) {
         int nthreads = getDefaultNumThreads();
         staticManager = new ArtiSynthThreadManager("Artisynth", nthreads, defaultTimeoutMS);
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
   public ArtiSynthThreadManager(String name) {
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
   public ArtiSynthThreadManager(String name, int nThreads) {
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
   public ArtiSynthThreadManager(String name, int nThreads,
     long timeoutMS) {
      super(name,nThreads,timeoutMS);
   }

}
