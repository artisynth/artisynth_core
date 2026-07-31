package maspack.util;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base implementation of {@link ReferenceCounted}
 */
public abstract class ReferenceCountedBase implements ReferenceCounted {

   /**
    * When set true, every {@link #acquire}/{@link #release} records the calling
    * stack, and the full acquire/release history is dumped if the reference
    * count ever underflows. This makes it possible to pinpoint the two
    * colliding call sites (the extra release, and the acquire it should have
    * balanced against). Off by default since it adds per-call overhead and
    * synchronization; enable it while hunting a "didn't properly keep track of
    * releases" message. May be set via the system property
    * {@code maspack.util.traceReferences=true}.
    */
   public static boolean traceReferences =
      Boolean.getBoolean ("maspack.util.traceReferences");

   /**
    * Number of most-recent acquire/release events retained per object when
    * {@link #traceReferences} is enabled.
    */
   public static int traceHistorySize = 32;

   AtomicLong acquireCount;

   // history of acquire/release stacks, only allocated when tracing is on
   private ArrayDeque<Throwable> myTrace;

   public ReferenceCountedBase() {
      acquireCount = new AtomicLong (0);
   }

   private void recordTrace (String label, long count) {
      // guard on the deque itself so concurrent acquire/release from the
      // render thread and the garbage-collector thread stay consistent
      synchronized (this) {
         if (myTrace == null) {
            myTrace = new ArrayDeque<> ();
         }
         while (myTrace.size () >= traceHistorySize) {
            myTrace.pollFirst ();
         }
         myTrace.addLast (
            new Throwable (
               label + " (count=" + count + ", thread="
               + Thread.currentThread ().getName () + ")"));
      }
   }

   private void dumpTrace () {
      synchronized (this) {
         if (myTrace != null) {
            System.err.println (
               "--- acquire/release history for " + this.getClass ()
               + " (oldest first) ---");
            for (Throwable t : myTrace) {
               t.printStackTrace ();
            }
            System.err.println ("--- end history ---");
         }
      }
   }

   @Override
   public ReferenceCountedBase acquire() {
      long ac = acquireCount.incrementAndGet ();
      // System.out.println (this + " acquired - " + ac);
      if (traceReferences) {
         recordTrace ("acquire", ac);
      }
      return this;
   }

   @Override
   public long acquireAndCount() {
      long ac = acquireCount.incrementAndGet ();
      if (traceReferences) {
         recordTrace ("acquireAndCount", ac);
      }
      return ac;
   }

   @Override
   public long releaseAndCount() {
      long ac = acquireCount.decrementAndGet ();
      // System.out.println (this + " released - " + ac);
      if (traceReferences) {
         recordTrace ("release", ac);
      }
      if (ac < 0) {
         // Reference count underflowed: this object was released more times
         // than it was acquired. Dump the offending call stack (and, if
         // tracing is on, the full acquire/release history) so the extra
         // release site can be identified.
         System.err.println (
            "Uh oh, " + this.getClass()
            + " didn't properly keep track of releases (count=" + ac + ")");
         new Throwable ("release-underflow stack trace").printStackTrace ();
         if (traceReferences) {
            dumpTrace ();
         }
      }
      return ac;
   }

   @Override
   public void release() {
      releaseAndCount ();  // release but don't return count
   }

   @Override
   public long getReferenceCount () {
      return acquireCount.get ();
   }

}
