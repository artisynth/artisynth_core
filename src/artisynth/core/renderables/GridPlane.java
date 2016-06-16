package artisynth.core.renderables;

import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import artisynth.core.modelbase.RenderableComponentBase;

public class GridPlane extends RenderableComponentBase {

   protected static AxisAngle myDefaultOrientation = new AxisAngle(
      1, 0, 0, Math.PI / 2);
   protected static Vector2d myDefaultSize = new Vector2d(1, 1);
   protected static Vector2d myDefaultResolution = new Vector2d(20, 20);

   RigidTransform3d XGridToWorld = new RigidTransform3d();
   protected Vector2d mySize = new Vector2d(myDefaultSize);
   protected Vector2d myResolution = new Vector2d(myDefaultResolution);

   public static PropertyList myProps =
      new PropertyList(GridPlane.class, RenderableComponentBase.class);
   static {
      myProps.add("renderProps * *", "render properties", null);
      myProps.add("resolution", "plane resolution", myDefaultResolution);
      myProps.add("size", "plane size", myDefaultSize);
      myProps.add("position", "position of the grid coordinate frame", null);
      myProps.get("position").setAutoWrite(false);
      myProps.add(
         "orientation", "orientation of the grid coordinate frame",
         myDefaultOrientation);
      myProps.get("orientation").setAutoWrite(false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public GridPlane(RigidTransform3d trans, Vector2d size, Vector2d resolution) {
      myRenderProps = createRenderProps();
      setGridToWorld(trans);
      setSize(size);
      setResolution(resolution);
   }

   @Override
   public void render(Renderer renderer, int flags) {
      
      boolean highlight = ((flags & Renderer.HIGHLIGHT) != 0);
      RenderProps props = getRenderProps();
      
      renderer.pushModelMatrix();
      renderer.mulModelMatrix(XGridToWorld);
      
      float [] coords0 = new float[3];
      float[] coords1 = new float[3];
      // draw grid lines
      // draw lines parallel to y axis
      coords0[1] = -(float)(mySize.y/2);
      coords1[1] = (float)(mySize.y/2);
      double dx = mySize.x/myResolution.x;
      for (int i=0; i <= (int)(myResolution.x); i++) {
         coords0[0] = (float)(-mySize.x/2 + i*dx);
         coords1[0] = coords0[0];
         renderer.drawLine(props, coords0, coords1, highlight);
      }

      // draw lines parallel to x axis
      coords0[0] = -(float)(mySize.x/2);
      coords1[0] = (float)(mySize.x/2);
      double dy = mySize.y/myResolution.y;
      for (int i=0; i <= (int)(myResolution.y); i++) {
         coords0[1] = (float)(-mySize.y/2 + i*dy);
         coords1[1] = coords0[1];
         renderer.drawLine(props, coords0, coords1, highlight);
      }
      
      renderer.popModelMatrix();
   }

   /**
    * Gets the size of the display grid
    */
   public Vector2d getSize() {
      return new Vector2d(mySize);
   }

   /**
    * Sets the size of the display grid
    */
   public void setSize(Vector2d size) {
      if (!mySize.equals(size)) {
         mySize.set(size);
      }
   }

   /**
    * Gets the resolution of the display, integers (nx,ny) corresponding
    * to the number of divisions along the x and y axes 
    */
   public Vector2d getResolution() {
      return new Vector2d(myResolution);
   }

   /**
    * Sets the resolution fo teh display, integers (nx, ny) corresponding
    * to the number of divisions along the x and y axes.  This triggers
    * a rebuild of the mesh.
    */
   public void setResolution(Vector2d res) {
      if (!myResolution.equals(res)) {
         myResolution = new Vector2d((int)res.x, (int)res.y);
      }
   }

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createLineProps(this);
      return props;
   }

   /**
    * Gets the 3D position of the centre of the display
    */
   public Point3d getPosition() {
      return new Point3d(XGridToWorld.p);
   }

   /**
    * Sets the 3D position of the centre of the display
    */
   public void setPosition(Point3d pos) {
      RigidTransform3d X = new RigidTransform3d(XGridToWorld);
      X.p.set(pos);
      setGridToWorld(X);
   }

   /**
    * Gets the orientation of the display
    */
   public AxisAngle getOrientation() {
      AxisAngle axisAng = new AxisAngle();
      XGridToWorld.R.getAxisAngle(axisAng);
      return axisAng;
   }

   /**
    * Sets the orientation of the display
    */
   public void setOrientation(AxisAngle axisAng) {
      RigidTransform3d X = new RigidTransform3d(XGridToWorld);
      X.R.setAxisAngle(axisAng);
      setGridToWorld(X);
   }

   /**
    * Sets the transform between the display grid and World
    */
   public void setGridToWorld(RigidTransform3d X) {
      XGridToWorld.set(X);
   }
   
   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      double x = mySize.x/2;
      double y = mySize.y/2;
      
      // four corners
      Point3d c0 = new Point3d(-x, -y, 0);
      c0.transform(XGridToWorld);
      c0.updateBounds(pmin, pmax);
      
      Point3d c1 = new Point3d(x, -y, 0);
      c1.transform(XGridToWorld);
      c1.updateBounds(pmin, pmax);
      
      Point3d c2 = new Point3d(x, y, 0);
      c2.transform(XGridToWorld);
      c2.updateBounds(pmin, pmax);
      
      Point3d c3 = new Point3d(-x, y, 0);
      c3.transform(XGridToWorld);
      c3.updateBounds(pmin, pmax);
   }
   
}
