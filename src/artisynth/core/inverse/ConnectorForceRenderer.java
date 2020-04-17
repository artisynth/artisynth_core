package artisynth.core.inverse;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.SphericalJoint;
import artisynth.core.mechmodels.SphericalJointBase;
import artisynth.core.modelbase.MonitorBase;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;

/**
 * Monitor that renders the force for either a spherical joint or a planar
 * connector.
 */
public class ConnectorForceRenderer extends MonitorBase {

   public ConnectorForceRenderer () {
   }

   BodyConnector myConnector;
   float[] start = new float[3];
   float[] end = new float[3];
   Vector3d startvec = new Vector3d ();
   Vector3d endvec = new Vector3d ();
   Vector3d myNormal = new Vector3d ();

   public static final double DEFAULT_ARROW_SIZE = 1d;
   double arrowSize = DEFAULT_ARROW_SIZE;

   public ConnectorForceRenderer (BodyConnector con) {
      super ();
      myConnector = con;
      setRenderProps (createRenderProps ());
      RigidTransform3d tr = myConnector.getCurrentTDW ();

      myNormal.transform (tr, Vector3d.Z_UNIT);
      myNormal.normalize ();
   }

   protected RenderProps myRenderProps = null;

   public static PropertyList myProps =
      new PropertyList (ConnectorForceRenderer.class, MonitorBase.class);

   static private RenderProps defaultRenderProps = new RenderProps ();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
      myProps.add ("arrowSize * *", "arrow size", DEFAULT_ARROW_SIZE);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   @Override
   public void apply (double t0, double t1) {
      // do nothing, just renderer
   }

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
//      System.out.println ("CFR-pr");

      if (myConnector instanceof PlanarConnector) {
         prerender ((PlanarConnector)myConnector);
      }
      else if (myConnector instanceof SphericalJointBase) {
         prerender ((SphericalJointBase)myConnector);
      }
      else {
         throw new RuntimeException (
            "ConstraintForceRenderer -- unsupported type of constraint: "
            + myConnector.getClass ());
      }
   }

   @Override
   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
//      System.out.println ("CFR-ren");
      renderer.drawArrow (getRenderProps (), start, end, true, isSelected ());
   }

   public void prerender (SphericalJointBase myConnector) {
      // TODO: fix activation scale here (divide by timestep?)
      startvec = myConnector.getCurrentTCW ().p;
      endvec.x = myConnector.getActivation (0) * arrowSize;
      endvec.y = myConnector.getActivation (1) * arrowSize;
      endvec.z = myConnector.getActivation (2) * arrowSize;
      endvec.add (startvec);
      set (start, startvec);
      set (end, endvec);
   }

   public void prerender (PlanarConnector myConnector) {
      startvec = myConnector.getCurrentTDW ().p;
      endvec.set (myNormal);
      endvec.scale (myConnector.getPlanarActivation () * arrowSize);
      endvec.add (startvec);
      set (start, startvec);
      set (end, endvec);
   }

   @Override
   public RenderProps createRenderProps () {
      return super.createRenderProps ();
   }

   @Override
   public boolean isSelectable () {
      return true;
   }

   private void set (float[] dest, Vector3d src) {
      dest[0] = (float)src.get (0);
      dest[1] = (float)src.get (1);
      dest[2] = (float)src.get (2);
   }

   public void setArrowSize (double size) {
      arrowSize = size;
   }

   public double getArrowSize () {
      return arrowSize;
   }

}
