package artisynth.core.modelbase;

import java.io.IOException;
import java.util.Deque;
import artisynth.core.util.*;
import maspack.util.Scannable;

/**
 * A Scannable object that also needs a postscan method for second pass
 * scanning.
 */
public interface PostScannable extends Scannable {

   /**
    * Performs any required post-scanning for this component. 
    * This involves handling any information whose processing was deferred 
    * during the <code>scan()</code> method and stored in the token queue.
    * The most common use of this method is to resolve the paths
    * of component references, which may not have been created
    * at the time of the initial <code>scan()</code> call.
    * 
    * @param tokens token information that was stored during 
    * <code>scan()</code>.
    * @param ancestor ancestor component with respect to which
    * reference component paths are defined.
    * @throws IOException if an error is encountered (such as a reference to a
    * non-existent component)
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException;
}
