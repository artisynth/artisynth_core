/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;

/**
 * Base class for a NURBS curve or surface.
 */
public abstract class NURBSObject implements Renderable {
   protected ArrayList<Vector4d> myCtrlPnts;
   protected ArrayList<Boolean> myCtrlPntSelected;

   protected int myPointSize = 3;
   protected int myLineWidth = 2;

   protected float[] pointColor = new float[] { 0, 1, 0 };
   protected float[] edgeColor = new float[] { 0, 0, 1 };
   protected float[] contourColor = new float[] { 0.8f, 0.8f, 0.8f };
   protected float[] selectedColor = new float[] { 1, 1f, 0.2f };

   protected RigidTransform3d myXObjToWorld = RigidTransform3d.IDENTITY;
   protected RenderProps myRenderProps;
   protected static final boolean DEFAULT_DRAW_CONTROL_SHAPE = true;
   protected boolean myDrawControlShapeP = DEFAULT_DRAW_CONTROL_SHAPE;
   
   protected NURBSObject() {
      myRenderProps = createRenderProps();
      myRenderProps.setPointSize (3);
      myCtrlPnts = new ArrayList<Vector4d>();
      myCtrlPntSelected = new ArrayList<Boolean>();
   }

   /**
    * Returns the 3D transform from the coordinate frame of this
    * NURBS object to the world.
    */
   public RigidTransform3d getObjToWorld() {
      return myXObjToWorld;
   }

   /**
    * Gets the 3D transform from the coordinate frame of this
    * NURBS object to the world.
    */
   public void getObjToWorld (RigidTransform3d TOW) {
      TOW.set (myXObjToWorld);
   }

   /**
    * Sets the 3D transform from the coordinate frame of this
    * NURBS object to the world.
    */
   public void setObjToWorld (RigidTransform3d TOW) {
      if (TOW.equals (RigidTransform3d.IDENTITY)) {
         myXObjToWorld = RigidTransform3d.IDENTITY;
      }
      else {
         myXObjToWorld = new RigidTransform3d(TOW);
      }
   }

   public void setLineWidth (int w) {
      myLineWidth = w;
   }

   public int getLineWidth() {
      return myLineWidth;
   }

   public void setPointSize (int w) {
      myPointSize = w;
   }

   public int getPointSize() {
      return myPointSize;
   }

   public boolean getDrawControlShape() {
      return myDrawControlShapeP;
   }

   public void setDrawControlShape (boolean enable) {
      myDrawControlShapeP = enable;
   }

   /**
    * Returns the number of control points used by this curve.
    * 
    * @return number of control points
    */
   public int numControlPoints() {
      return myCtrlPnts.size();
   }

   public Vector4d getControlPoint (int i) {
      return myCtrlPnts.get(i);
   }
   
   /**
    * Returns the control points used by this curve.
    * 
    * @return control points
    */
   public Vector4d[] getControlPoints() {
      return myCtrlPnts.toArray (new Vector4d[0]);
   }

   protected void setControlPoints (Vector4d[] pnts, int numc) {
      myCtrlPnts = new ArrayList<Vector4d>(numc);
      myCtrlPntSelected = new ArrayList<Boolean>(numc);
      for (int i = 0; i < numc; i++) {
         addControlPoint (pnts[i]);
      }
   }

   protected void addControlPoint (Vector4d pnt) {
      myCtrlPnts.add (new Vector4d (pnt));
      myCtrlPntSelected.add (false);
   }

   protected void addControlPoint (int idx, Vector4d pnt) {
      myCtrlPnts.add (idx, new Vector4d (pnt));
      myCtrlPntSelected.add (idx, false);
   }

   protected void removeControlPoint (int i) {
      myCtrlPnts.remove (i);
      myCtrlPntSelected.remove (i);
   }

   public void selectControlPoint (int i, boolean selected) {
      myCtrlPntSelected.set (i, selected);
   }

   public boolean controlPointIsSelected (int i) {
      return myCtrlPntSelected.get(i);
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < myCtrlPnts.size()) {
         list.add (this);
         list.add (new Integer(qid));
         // selectControlPoint (id, true);
      }
   }
   
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Point3d tmp = new Point3d();
      for (int i=0; i<myCtrlPnts.size(); i++) {
         Vector4d cpnt = myCtrlPnts.get(i);
         tmp.set (cpnt.x, cpnt.y, cpnt.z);
         if (myXObjToWorld != RigidTransform3d.IDENTITY) {
            tmp.transform (myXObjToWorld);
         }
         tmp.updateBounds (pmin, pmax);
      }
   }

   private void drawControlPoint (
      Renderer renderer, RenderProps props, int i, Point3d tmp) {

      int psize = props.getPointSize();
      boolean selected = myCtrlPntSelected.get(i); 
      renderer.setPointSize (selected ? psize+1 : psize);
      renderer.setPointColoring (props, selected);
      Vector4d cpnt = myCtrlPnts.get(i);
      renderer.drawPoint (cpnt.x, cpnt.y, cpnt.z);
   } 

   protected void drawControlPoints (
      Renderer renderer, RenderProps props, int flags) {

      Point3d tmp = new Point3d();
      boolean selecting = renderer.isSelecting();
      for (int i=0; i<myCtrlPnts.size(); i++) {
         if (selecting) {
            renderer.beginSelectionQuery (i);
            drawControlPoint (renderer, props, i, tmp);
            renderer.endSelectionQuery ();
         }
         else {
            drawControlPoint (renderer, props, i, tmp);
         }
      }
      renderer.setPointSize (1);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return myCtrlPnts.size()+2;
   }

   public void prerender (RenderList list) {
   }

   public int getRenderHints() {
      return 0;
   }

   /**
    * Writes a text description of this NURBS object to a PrintWriter, using a
    * subset of the Wavefront OBJ format described in the documentation for the
    * <code>read</code> method for this object.
    * 
    * @param pw
    * PrintWriter for outputing the text description
    * @throws IOException
    * if an I/O error occurs
    */
   public void write (PrintWriter pw) throws IOException {
      write (pw, "%.8g", false);
   }

   /**
    * Writes a text description of this NURBS object to a PrintWriter, using a
    * subset of the Wavefront OBJ format described in the documentation
    * for the <code>read</code> method for this object.
    * 
    * @param pw
    * PrintWriter for outputing the text description
    * @param fmtStr
    * printf-style format string for formatting the floating point values (e.g.,
    * "%g", "%10.3f").
    * @throws IOException
    * if an I/O error occurs
    */
   public void write (PrintWriter pw, String fmtStr) throws IOException {
      write (pw, fmtStr, false);
   }

    /**
    * Writes a text description of this NURBS object to a PrintWriter, using a
    * subset of the Wavefront OBJ format as described in the documentation
    * for the <code>read</code> method for this object.
    * 
    * @param pw
    * PrintWriter for outputing the text description
    * @param fmtStr
    * printf-style format string for formatting the floating point values (e.g.,
    * "%g", "%10.3f").
    * @param relative
    * if true, then control point indices are written as negative values giving
    * their location relative to the <code>curv</code> statement.
    * @throws IOException
    * if an I/O error occurs
    */
   public abstract void write (PrintWriter pw, String fmtStr, boolean relative)
      throws IOException;

   /**
    * {@inheritDoc}
    */
   public void setRenderProps (RenderProps props) {
      if (props == null) {
         throw new IllegalArgumentException ("Render props cannot be null");
      }
      myRenderProps = createRenderProps();
      myRenderProps.set (props);
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void render (Renderer renderer, int flags) {
      render (renderer, myRenderProps, /*flags=*/0);
   }

   public abstract void render (
      Renderer renderer, RenderProps props, int flags);

   /**
    * Applies an affine transformation to the control points this NURBS
    * object.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      for (Vector4d cpnt : myCtrlPnts) {
         cpnt.transform (X);
      }
   }

   protected void set (NURBSObject nobj) {
      myCtrlPnts.clear();
      for (Vector4d cpnt : nobj.myCtrlPnts) {
         myCtrlPnts.add (new Vector4d (cpnt));
      }
      myCtrlPntSelected = (ArrayList<Boolean>)nobj.myCtrlPntSelected.clone();
      if (nobj.myXObjToWorld.isIdentity()) {
         myXObjToWorld = RigidTransform3d.IDENTITY;
      }
      else {
         myXObjToWorld = new RigidTransform3d (nobj.myXObjToWorld);
      }
      myRenderProps = nobj.myRenderProps.clone();
   }

}
