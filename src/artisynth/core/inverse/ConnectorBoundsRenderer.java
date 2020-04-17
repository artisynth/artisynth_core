package artisynth.core.inverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import artisynth.core.mechmodels.Frame;
import artisynth.core.modelbase.MonitorBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.FaceRenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.DrawMode;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;

/**
 * Monitor that renders the bounds for a spherical joint, as specified by a
 * SphericalJointForceBound.
 */
public class ConnectorBoundsRenderer extends MonitorBase {
   
   ArrayList<LineInfo> lines = new ArrayList<LineInfo> ();
   ArrayList<TriInfo> planes = new ArrayList<TriInfo> ();
   double mySize = 1d;
   
   SphericalJointForceBound shoulderConstraint;
   Frame baseFrame;
   
   public ConnectorBoundsRenderer (SphericalJointForceBound bounds, Frame glenoidFrame) {
      this (bounds, glenoidFrame, 1d);
   }
   
   public ConnectorBoundsRenderer (SphericalJointForceBound bounds, Frame baseFrame, double size) {
      mySize = size;
      setRenderProps (createRenderProps ());
      shoulderConstraint = bounds;
      this.baseFrame = baseFrame;
      addRenderables();
   }
   
   public ConnectorBoundsRenderer (SphericalJointForceBound bounds, Point3d basePoint) {
      this(bounds, basePoint, 1d);
   }
   
   public ConnectorBoundsRenderer (SphericalJointForceBound bounds, Point3d basePoint, double size) {
      this(bounds, new Frame(new RigidTransform3d (basePoint, RotationMatrix3d.IDENTITY)), size);
   }

   protected RenderProps myRenderProps = null;

   public static PropertyList myProps =
      new PropertyList (ConnectorBoundsRenderer.class, MonitorBase.class);

   static private RenderProps defaultRenderProps = new RenderProps ();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }
   
   public SphericalJointForceBound getBounds() {
      return shoulderConstraint;
   }
   
   public void addRenderables () {
      
      Iterator<Vector3d> viter;
      Iterator<Point3d> piter;
      Vector3d prev, first;
      Point3d prevPt, firstPt;
      Vector3d tmp = new Vector3d ();
      Point3d p0 = baseFrame.getPosition ();
    
      ArrayList<Point3d> polyPts = new ArrayList<Point3d> ();
      viter = shoulderConstraint.getBoundNormals ().iterator ();
      prev = shoulderConstraint.getBoundNormals ().get (shoulderConstraint.getBoundNormals ().size ()-1);
      while (viter.hasNext ()) {
         Vector3d cur = viter.next ();
         tmp.cross (prev, cur);
         tmp.normalize ();
         tmp.scale (mySize);
         tmp.add (p0);
         polyPts.add (new Point3d (tmp));
         lines.add (new LineInfo (p0, tmp, Color.WHITE));
         prev = cur;
      }

      piter = polyPts.iterator ();
      firstPt = piter.next ();
      prevPt = firstPt;
      while (piter.hasNext ()) {
         Point3d curPt = piter.next ();
         planes.add (new TriInfo (p0, prevPt, curPt, Color.LIGHT_GRAY));
          //lines.add (new LineInfo (p0, tmp, Color.RED));
         prevPt = curPt;
      }
      planes.add (new TriInfo (p0, prevPt, firstPt, Color.LIGHT_GRAY));

   }

   @Override
   public void apply (double t0, double t1) {
      //do nothing, just renderer
   }

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
    //clear the lines and planes and re-create the information
      lines = new ArrayList<LineInfo> ();
      planes = new ArrayList<TriInfo> ();
      addRenderables();  
//      System.out.println ("CBR-pr");
//      list.addIfVisible (this);
   }

   @Override
   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
//      System.out.println ("CBR-ren");
      for (LineInfo line : lines) {
         renderer.drawLine (line.props, line.p0, line.p1, isSelected ());
      }

      for (TriInfo tri : planes) {
         drawTriangle (renderer, tri);
      }
   }

   @Override
   public RenderProps createRenderProps () {
      return super.createRenderProps ();
   }

   @Override
   public boolean isSelectable () {
      return true;
   }

   private double myPlaneSize = 1d;
   private Point3d[] myRenderVtxs;

   public void drawTriangle (Renderer renderer, TriInfo tri) {
      
      RenderProps props = tri.props;

      Shading savedShading = renderer.setPropsShading (props);
      renderer.setFaceColoring (props, isSelected());
      renderer.setFaceStyle (props.getFaceStyle ());
      renderer.beginDraw (DrawMode.TRIANGLES);
      renderer.setNormal (tri.nrm);
      for (int i = 0; i < tri.pts.length; i++) {
         renderer.addVertex (tri.pts[i]);
      }
      renderer.endDraw();
      renderer.setShading (savedShading);
      renderer.setFaceStyle (FaceStyle.FRONT);
   }

   public class TriInfo {
      FaceRenderProps props = new FaceRenderProps ();
      Point3d[] pts = new Point3d[3];
      Vector3d nrm = new Vector3d ();

      public TriInfo (Point3d p0, Point3d p1, Point3d p2, Color color) {
         pts[0] = p0;
         pts[1] = p1;
         pts[2] = p2;
         Vector3d v1 = new Vector3d ();
         v1.sub (p1, p0);
         Vector3d v2 = new Vector3d ();
         v2.sub (p2, p0);
         nrm.cross (v1, v2);
         nrm.normalize ();
         props.setFaceColor (color);
         props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
         props.setAlpha (0.4);
      }

      public void setRenderProps (RenderProps props) {
         this.props.set (props);
      }
   }

   public class LineInfo {
      LineRenderProps props = new LineRenderProps ();
      float[] p0 = new float[3];
      float[] p1 = new float[3];

      public LineInfo (Vector3d p0, Vector3d p1, Color color) {
         set (this.p0, p0);
         set (this.p1, p1);
         props.setLineColor (color);
         props.setLineWidth (2);
      }

      public LineInfo (Vector3d p0, Vector3d p1, Color color,
      Vector3d translation) {
         Vector3d tmp = new Vector3d ();
         tmp.add (p0, translation);
         set (this.p0, tmp);
         tmp.add (p1, translation);
         set (this.p1, tmp);
         props.setLineColor (color);
         props.setLineWidth (2);
      }

      public void set (float[] p0, Vector3d p) {
         p0[0] = (float)p.x;
         p0[1] = (float)p.y;
         p0[2] = (float)p.z;
      }

      public void setRenderProps (RenderProps props) {
         this.props.set (props);
      }
   }

}
