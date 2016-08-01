package artisynth.core.inverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import artisynth.core.modelbase.MonitorBase;
import maspack.matrix.Point3d;
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

public class ConnectorBoundsRenderer extends MonitorBase {

   
   ArrayList<LineInfo> lines = new ArrayList<LineInfo> ();
   ArrayList<TriInfo> planes = new ArrayList<TriInfo> ();

   public ConnectorBoundsRenderer (SphericalJointForceBound bounds, Point3d p0) {
      setRenderProps (createRenderProps ());
      addRenderables(bounds, p0);
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
   
   public void addRenderables (SphericalJointForceBound bounds, Point3d p0) {
      
      Iterator<Vector3d> viter;
      Iterator<Point3d> piter;
      Vector3d prev, first;
      Point3d prevPt, firstPt;
      Vector3d tmp = new Vector3d ();

      ArrayList<Point3d> polyPts = new ArrayList<Point3d> ();
      viter = bounds.getBoundNormals ().iterator ();
      prev = bounds.getBoundNormals ().get (bounds.getBoundNormals ().size ()-1);
      while (viter.hasNext ()) {
         Vector3d cur = viter.next ();
         tmp.cross (prev, cur);
         tmp.normalize ();
         tmp.add (p0);
         polyPts.add (new Point3d (tmp));
         lines.add (new LineInfo (p0, tmp, Color.ORANGE));
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

//   public void setShoulderStabilityConstraints (
//      double[] shoulderStabilityRatios, Point3d p0, Vector3d N, double r,
//      TrackingController con) {
//      double theta = 0;
//      double dtheta = Math.PI * 2d / shoulderStabilityRatios.length;
//      Vector3d tmp = new Vector3d ();
//
//      SphericalConstraintForceBound bounds =
//         new SphericalConstraintForceBound (1d, con);
//      con.addInequalityTerm (bounds);
//
//      ArrayList<Vector3d> fs = new ArrayList<Vector3d> ();
//      ArrayList<Vector3d> ns = new ArrayList<Vector3d> ();
//
//      for (double ratio : shoulderStabilityRatios) {
//         double phi = Math.atan (ratio / 100d);
//         Vector3d f =
//            new Vector3d (
//               Math.cos (theta) * Math.sin (phi),
//               Math.sin (theta) * Math.sin (phi), Math.cos (phi));
//         RotationMatrix3d R = new RotationMatrix3d ();
//         R.rotateZDirection (N);
//         f.transform (R);
//         tmp.cross (f, N);
//         Vector3d n = new Vector3d ();
//         n.cross (tmp, f);
//         n.normalize ();
//         // if (theta == 0) {
//         ns.add (n);
//         fs.add (f);
//         // }
//         bounds.addHalfspaceBound (n);
//         n.scale (0.1);
//         lines.add (new LineInfo (p0, f, Color.GREEN));
//         lines.add (new LineInfo (p0, n, Color.WHITE, f));
//
//         // System.out.println("theta="+theta/Math.PI*180);
//         theta += dtheta;
//      }
//
//      // for (int i = 0; i < fs.size (); i++) {
//      // Vector3d f = fs.get (i);
//      // Vector3d n = ns.get (i);
//      // tmp.cross (f, n);
//      // tmp.normalize ();
//      // tmp.scale (0.4);
//      // Point3d p1 = new Point3d();
//      // p1.add (f, tmp);
//      // Point3d p2 = new Point3d();
//      // p2.sub (f, tmp);
//      // planes.add (new TriInfo (p0, p1, p2, Color.CYAN));
//      // }
//
//      Iterator<Vector3d> viter;
//      Iterator<Point3d> piter;
//      Vector3d prev, first;
//      Point3d prevPt, firstPt;
//
//      // viter = fs.iterator ();
//      // firstPt = new Point3d (viter.next ());
//      // prevPt = firstPt;
//      // while (viter.hasNext ()) {
//      // Point3d curPt = new Point3d (viter.next ());
//      // planes.add (new TriInfo (p0, prevPt, curPt, Color.CYAN));
//      // // lines.add (new LineInfo (p0, tmp, Color.ORANGE));
//      // prevPt = curPt;
//      // }
//      // planes.add (new TriInfo (p0, prevPt, firstPt, Color.CYAN));
//
//      ArrayList<Point3d> polyPts = new ArrayList<Point3d> ();
//      viter = bounds.getBoundNormals ().iterator ();
//      prev = viter.next ();
//      while (viter.hasNext ()) {
//         Vector3d cur = viter.next ();
//         tmp.cross (prev, cur);
//         tmp.normalize ();
//         polyPts.add (new Point3d (tmp));
//         lines.add (new LineInfo (p0, tmp, Color.ORANGE));
//         prev = cur;
//      }
//
//      piter = polyPts.iterator ();
//      firstPt = piter.next ();
//      prevPt = firstPt;
//      while (piter.hasNext ()) {
//         Point3d curPt = piter.next ();
//         planes.add (new TriInfo (p0, prevPt, curPt, Color.MAGENTA));
//         // lines.add (new LineInfo (p0, tmp, Color.ORANGE));
//         prevPt = curPt;
//      }
//      planes.add (new TriInfo (p0, prevPt, firstPt, Color.MAGENTA));
//
//   }

   @Override
   public void apply (double t0, double t1) {
      // do nothing, just renderer
   }

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
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
