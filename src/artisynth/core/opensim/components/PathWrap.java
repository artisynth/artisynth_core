package artisynth.core.opensim.components;

import maspack.util.IntegerInterval;

public class PathWrap extends OpenSimObject {
   String wrap_object;
   String method;
   IntegerInterval range;
   
   public PathWrap() {
      wrap_object = null;
      method = null;
      range = new IntegerInterval();
   }
   
   public String getWrapObject() {
      return wrap_object;
   }
   
   public void setWrapObject(String name) {
      wrap_object = name;
   }
   
   public String getMethod() {
      return method;
   }
   
   public void setMethod(String method) {
      this.method = method;
   }
   
   public IntegerInterval getRange() {
      return range;
   }
   
   public void setRange(int min, int max) {
      range.set (min, max);
   }
   
   public void setRange(IntegerInterval range) {
      this.range = range;
   }
   
   @Override
   public PathWrap clone () {
      PathWrap pw = (PathWrap) super.clone ();
      
      pw.setWrapObject (wrap_object);
      pw.setMethod (method);
      if (range != null) {
         pw.setRange (range.clone ());
      }
      
      return pw;
   }
}
