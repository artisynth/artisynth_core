package maspack.render;

import maspack.util.DisposeObservable;
import maspack.util.DisposeObserver;
import maspack.util.DynamicIntArray;
import maspack.util.DisposeObserver.DisposeObserverImpl;

public class VertexIndexArray extends DynamicIntArray implements DisposeObservable {

   private static class VertexIndexDisposeObserver extends DisposeObserverImpl {
      @Override
      protected void dispose () {
         super.dispose ();
      }
   }
   
   VertexIndexDisposeObserver observer;
   
   public VertexIndexArray() {
      super();
      init();
   }
   
   public VertexIndexArray(int cap) {
      super(cap);
      init();
   }
   
   public VertexIndexArray(int... vals) {
      super(vals);
      init();
   }
   
   private void init() {
      observer = new VertexIndexDisposeObserver ();
   }
   
   public void dispose() {
      observer.dispose ();
   }
   
   @Override
   public boolean isDisposed () {
      return observer.isDisposed ();
   }

   @Override
   public DisposeObserver getDisposeObserver () {
      return observer;
   }
   
   @Override
   protected void finalize () throws Throwable {
      dispose();
   }
   
   @Override
   public VertexIndexArray clone () {
      VertexIndexArray out = (VertexIndexArray)super.clone ();
      out.observer = new VertexIndexDisposeObserver (); // replace observer
      return out;
   }

}
