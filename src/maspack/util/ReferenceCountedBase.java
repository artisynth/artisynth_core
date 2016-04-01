package maspack.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Resource used by GL
 */
public abstract class ReferenceCountedBase implements ReferenceCounted {
   
   AtomicLong acquireCount;
   
   public ReferenceCountedBase() {
      acquireCount = new AtomicLong (0);
   }
         
   @Override
   public ReferenceCountedBase acquire() {
      acquireCount.incrementAndGet ();
      return this;
   }
   
   @Override
   public long acquireAndCount() {
      return acquireCount.incrementAndGet ();
   }
   
   @Override
   public long releaseAndCount() {
      long ac = acquireCount.decrementAndGet ();
      if (ac == -1) {
         System.err.println ("Uh oh, something didn't properly keep track of releases");
      }
      return ac;
   }
   
   @Override
   public void release() {
      long ac = acquireCount.decrementAndGet ();
      if (ac == -1) {
         System.err.println ("Uh oh, something didn't properly keep track of releases");
      }
   }
   
   @Override
   public long getReferenceCount () {
      return acquireCount.get ();
   }

}
