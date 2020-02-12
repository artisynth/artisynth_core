package artisynth.core.opensim.components;

public class GeometryPath extends HasVisibleObjectOrAppearance {

   PathPointSet pathPointSet;
   PathWrapSet pathWrapSet;
   
   public PathPointSet getPathPointSet() {
      return pathPointSet;
   }
   
   public void setPathPointSet(PathPointSet set) {
      pathPointSet = set;
      pathPointSet.setParent(this);
   }
   
   public PathWrapSet getPathWrapSet() {
      return pathWrapSet;
   }
   
   public void setPathWrapSet(PathWrapSet set) {
      pathWrapSet = set;
      pathWrapSet.setParent(this);
   }
   
   @Override
   public GeometryPath clone () {
      GeometryPath gp = (GeometryPath)super.clone ();
      if (pathPointSet != null) {
         gp.setPathPointSet (pathPointSet.clone ());
      }
      if (pathWrapSet != null) {
         gp.setPathWrapSet (pathWrapSet.clone());
      }
      return gp;
   }
   
}
