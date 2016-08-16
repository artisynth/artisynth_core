package maspack.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Base implementation of {@link ReferenceCounted}
 */
public abstract class ReferenceCountedBase implements ReferenceCounted {
   
   AtomicLong acquireCount;
   
   public ReferenceCountedBase() {
      acquireCount = new AtomicLong (0);
   }
         
   @Override
   public ReferenceCountedBase acquire() {
      // long ac = 
      acquireCount.incrementAndGet ();
      // System.out.println (this + " acquired - " + ac);
      return this;
   }
   
   @Override
   public long acquireAndCount() {
      return acquireCount.incrementAndGet ();
   }
   
   @Override
   public long releaseAndCount() {
      long ac = acquireCount.decrementAndGet ();
      // System.out.println (this + " released - " + ac);
      if (ac == -1) {
         System.err.println ("Uh oh, " + this.getClass() + " didn't properly keep track of releases");
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
