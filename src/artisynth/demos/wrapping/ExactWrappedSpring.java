package artisynth.demos.wrapping;

import java.util.ArrayList;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.RenderableComponentBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/**
 * Class that represents a wrapped spring with an exact wrapping solution.
 * 
 * @author Omar
 */
public class ExactWrappedSpring extends RenderableComponentBase {
   protected Point fm1; // Start point of the spring.
   protected Point fm2; // End point of the spring.
   protected ExactWrappableGeometry wrap; // The object around which to wrap.
   protected ExactWrapPath wp; // Latest wrapped path.

   protected double myRestLength;
   protected AxialMaterial myMaterial;

   public double getRestLength () {
      return myRestLength;
   }

   public void setRestLength (double myRestLength) {
      this.myRestLength = myRestLength;
   }

   public AxialMaterial getMaterial () {
      return myMaterial;
   }

   public void setMaterial (AxialMaterial myMaterial) {
      this.myMaterial = myMaterial;
   }

   public static PropertyList myProps =
      new PropertyList (ExactWrappedSpring.class, RenderableComponentBase.class);

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   static {
      myProps.add ("renderProps * *", "renderer properties", null);
      myProps.addReadOnly ("length", "length of the spring");
      myProps.addReadOnly ("lengthDot", "derivative of the length of the spring");
   }

   /**
    * Create a new wrapped spring with the given name.
    * 
    * @param name
    * name of the new spring
    */
   public ExactWrappedSpring (String name) {
      setName (name);
      myRenderProps = new RenderProps ();
   }

   public ArrayList<Point3d> getAllABPoints () {
      if (wp == null) {
         wp = wrap.wrap (fm1, fm2);
      }
      ArrayList<Point3d> pnts = new ArrayList<> ();
      // no need to transform to world, since new methods now compute
      // A/P points in world
      //RigidTransform3d toW = wrap.getTransformToWorld ();
      for (Point3d pnt3d : wp.getAllABPoints ()) {
         Point3d pnt = new Point3d (pnt3d);
         //pnt.transform (toW);
         pnts.add (pnt);
      }
      return pnts;
   }

   /**
    * Returns the current length of the spring.
    * 
    * @return current length of this spring
    */
   public double getLength () {
      if (wp == null) {
         wp = wrap.wrap (fm1, fm2);
      }
      return wp.getLength ();
   }

   Vector3d e1 = new Vector3d();
   Vector3d e2 = new Vector3d();
   Vector3d Bvel = new Vector3d();
   Vector3d Avel = new Vector3d();
   
   /**
    * Returns the current length time derivative of the spring.
    *  
    * @return current length time derivative of this spring
    */
   public double getLengthDot () {
      if (wp == null) {
         wp = wrap.wrap (fm1, fm2);
      }
      
      /* XXX assume only one wrap object */
      if (wp.getNumSegments () > 3 || getAllABPoints ().size ()<2) {
         return 0;
      }
      
      /* path velocity computed based on velocity of path points, 
       * see equation (81) in Scholz, A., et al. (2016). A fast multi-obstacle 
       * muscle wrapping method using natural geodesic variations. 
       * Multibody System Dynamics, 36(2), 195-219.
       */
      
      Point3d Opos = fm1.getPosition ();
      Point3d Ipos = fm2.getPosition ();
      Point3d Apos = getAllABPoints ().get (0); // start of geodesic
      Point3d Bpos = getAllABPoints ().get (1); // end of geodesic
      
      e1.sub (Apos,Opos);
      e1.normalize ();
      e2.sub (Ipos,Bpos);
      e2.normalize ();

      // compute A/B point velocity based on wrappable velocity
      Apos.inverseTransform (wrap.parent.getPose ()); // transform to body coordinates
      Bpos.inverseTransform (wrap.parent.getPose ()); // transform to body coordinates
      wrap.parent.computePointVelocity (Avel, Apos);
      wrap.parent.computePointVelocity (Bvel, Bpos);

//      Twist frameVel = wrap.parent.getVelocity ();
//      Avel.crossAdd (frameVel.w, Apos, frameVel.v); 
//      Bvel.crossAdd (frameVel.w, Bpos, frameVel.v);
      
      Vector3d Ovel = fm1.getVelocity ();
      Vector3d Ivel = fm2.getVelocity ();
      
      double ldot =
              - e1.dot (Ovel)
              + e1.dot (Avel)
              - e2.dot (Bvel) 
              + e2.dot (Ivel);

      return ldot;
   }

   /**
    * Update wrapping.
    */
   public void updateWrapSegments () {
      wp = wrap.wrap (fm1, fm2, wp);
   }

   /**
    * Set the points of this spring.
    * 
    * @param f1
    * first point
    * @param f2
    * second point
    */
   public void setPoints (Point f1, Point f2) {
      fm1 = f1;
      fm2 = f2;
   }

   /**
    * Set the wrap geometry.
    * 
    * @param wg
    * wrap geometry
    */
   public void setWrapGeometry (ExactWrappableGeometry wg) {
      wrap = wg;
   }

   public ExactWrappableGeometry getWrapGeometry() {
      return wrap;
   }
   
   /**
    * Render the points on the path as wOverrideell as the tangents at these points.
    * Points are drawn as small red spheres. Tangent vector at each point is
    * drawn with a blue arrow.
    * 
    * @param renderer
    * renderer to use for drawing
    * @param flags
    * unused
    */
   public void render (Renderer renderer, int flags) {
      if (wp != null) {
         wp.renderPath (renderer, myRenderProps);
      }
   }

   /**
    * Returns the force magnitude of this spring.
    * 
    * @return the force magnitude of this spring
    */
   public double getForce () {
      return myMaterial.computeF (
         getLength (), getLengthDot(), myRestLength, 0);
   }
}
