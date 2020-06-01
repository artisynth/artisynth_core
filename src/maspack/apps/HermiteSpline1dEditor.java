/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import maspack.interpolation.CubicHermiteSpline1d;
import maspack.interpolation.CubicHermiteSpline1d.Knot;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3dBase;
import maspack.render.Dragger3dEvent;
import maspack.render.Dragger3dListener;
import maspack.render.DrawToolEvent;
import maspack.render.DrawToolListener;
import maspack.render.PointTool;
import maspack.render.RenderList;
import maspack.render.RenderListener;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.render.RendererEvent;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionListener;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.GLVersion;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.widgets.DraggerToolBar.ButtonType;
import maspack.widgets.*;
import maspack.widgets.DoubleField;
import maspack.widgets.ViewerFrame;

public class HermiteSpline1dEditor extends ViewerFrame 
   implements ActionListener, RenderListener,
              Dragger3dListener, DrawToolListener, ValueChangeListener {
   
   private static final long serialVersionUID = 1L;

   CubicHermiteSpline1d mySpline;
   Curve myCurve;
   double myYScale = 1.0;
   DoubleField myYScaleField;
   SplineRenderer myRenderer;
   ArrayList<Knot> mySelectedKnots = new ArrayList<>();
   // stores knot data at the start of a drag:
   ArrayList<Vector3d> myKnotStartVals = new ArrayList<>();
   File mySplineFile;
   File myCurveFile;

   class SplineRenderer implements Renderable {
      CubicHermiteSpline1d mySpline;
      
      RenderProps myRenderProps;

      public RenderProps createRenderProps() {
         return new RenderProps();
      }

      public RenderProps getRenderProps() {
         return myRenderProps;
      }

      public void setRenderProps (RenderProps props) {
         myRenderProps = new RenderProps (props);
      }

      public SplineRenderer (CubicHermiteSpline1d spline) {
         myRenderProps = createRenderProps();
         mySpline = spline;
      }

      public boolean isSelectable() {
         return true;
      }

      public int numSelectionQueriesNeeded() {
         return mySpline.numKnots()+1;
      }

      public void getSelection (LinkedList<Object> list, int qid) {
         if (qid < mySpline.numKnots()) {
            list.add (mySpline.getKnot(qid));
         }
      }

      public void prerender (RenderList list) {
      }

      public void render (Renderer renderer, int flags) {
         Knot knot0 = mySpline.getFirstKnot();
         Knot knotl = mySpline.getLastKnot();

         renderer.setShading (Shading.NONE);

         // draw the knots

         renderer.setPointSize (8);

         for (int i=0; i<mySpline.numKnots(); i++) {
            Knot knot = mySpline.getKnot(i);
            renderer.beginSelectionQuery (i);
            if (isSelected (knot)) {
               renderer.setColor (Color.YELLOW); 
            }
            else {
               renderer.setColor (Color.BLUE); 
            }
            renderer.drawPoint (
               new Vector3d (knot.getX(), knot.getY()/myYScale, 0));
            renderer.endSelectionQuery ();
         }

         // draw the curve

         renderer.setLineWidth (2); 
         renderer.setColor (Color.WHITE); 

         renderer.beginSelectionQuery (mySpline.numKnots());
         renderer.endSelectionQuery ();

         renderer.beginDraw (DrawMode.LINE_STRIP);
         if (knot0 != knotl) {
            int nstotal = 200;            
            double dxtotal = knotl.getX() - knot0.getX();
            for (int k=0; k<mySpline.numKnots()-1; k++) {
               Knot knot = mySpline.getKnot (k);
               Knot next = mySpline.getKnot (k+1);
               double dx = next.getX() - knot.getX();
               double dy = Math.abs(next.getY() - knot.getY())/myYScale;
               int ns = (int)Math.ceil(nstotal*(dx+dy)/dxtotal);
               for (int i=0; i<=ns; i++) {
                  double x = knot.getX() + i*dx/ns;
                  double y = mySpline.evalY(x)/myYScale;
                  renderer.addVertex (new Vector3d (x, y, 0));
               }
            }
         }
         renderer.endDraw();
      }

      public void updateBounds (Vector3d pmin, Vector3d pmax) {
         for (int i=0; i<mySpline.numKnots(); i++) {
            Knot knot = mySpline.getKnot(i);
            Point3d pnt = new Point3d (knot.getX(), knot.getY()/myYScale, 0);
            pnt.updateBounds (pmin, pmax);
         }
      }

      public int getRenderHints() {
         return 0;
      }
   }

   class Curve implements Renderable {
      double[] myX;
      double[] myY;
      
      RenderProps myRenderProps;

      public RenderProps createRenderProps() {
         return new RenderProps();
      }

      public RenderProps getRenderProps() {
         return myRenderProps;
      }

      public void setRenderProps (RenderProps props) {
         myRenderProps = new RenderProps (props);
      }

      public Curve (double[] x, double[] y) {
         myRenderProps = createRenderProps();
         set (x, y);
      }

      public void set (double[] x, double[] y) {
         if (x.length != y.length) {
            throw new IllegalArgumentException (
               "x and y lengths differ ("+x.length+" vs. "+y.length+")");
         }
         myX = Arrays.copyOf (x, x.length);
         myY = Arrays.copyOf (y, y.length);
      }

      public boolean isSelectable() {
         return true;
      }

      public int numSelectionQueriesNeeded() {
         return 0;
      }

      public void getSelection (LinkedList<Object> list, int qid) {
         list.add (this);
      }

      public void prerender (RenderList list) {
      }

      public void render (Renderer renderer, int flags) {
         renderer.setShading (Shading.NONE);

         // draw the curve

         renderer.setLineWidth (2); 
         renderer.setColor (Color.GREEN); 

         renderer.beginDraw (DrawMode.LINE_STRIP);
         for (int k=0; k<myX.length; k++) {
            renderer.addVertex (new Vector3d (myX[k], myY[k]/myYScale, 0));
         }
         renderer.endDraw();
      }

      public void updateBounds (Vector3d pmin, Vector3d pmax) {
         for (int k=0; k<myX.length; k++) {
            Point3d pnt = new Point3d (myX[k], myY[k]/myYScale, 0);
            pnt.updateBounds (pmin, pmax);
         }
      }

      public int getRenderHints() {
         return 0;
      }
   }

   private class SelectionHandler implements ViewerSelectionListener {

      public void itemsSelected (ViewerSelectionEvent e) {
         boolean holdSelection;

         long modEx = e.getModifiersEx();
         holdSelection = ((modEx & MouseEvent.SHIFT_DOWN_MASK) != 0);

         if (!holdSelection) {
            clearSelectedKnots();
         }
         if (e.numSelectedQueries() > 0) {
            List<LinkedList<?>> itemPaths = e.getSelectedObjects();
            for (LinkedList<?> path : itemPaths) {
               if (path.getFirst() instanceof Knot) {
                  Knot knot = (Knot)path.getFirst();
                  //myRenderer.clearSelectedKnots();
                  selectKnot (knot);
               }
            }
         }
      }
   }

   public void clearSelectedKnots() {
      mySelectedKnots.clear();
      if (myDraggerToolBar.getDragger() != null) {
         myDraggerToolBar.getDragger().setVisible (false);
      }
   }

   private void centerDraggerOnSelection (Dragger3dBase dragger) {
      double cx = 0;
      double cy = 0;
      for (Knot k : mySelectedKnots) {
         cx += k.getX();
         cy += k.getY()/myYScale;
      }
      cx /= mySelectedKnots.size();
      cy /= mySelectedKnots.size();
      dragger.setDraggerToWorld (new RigidTransform3d (cx, cy, 0));
   }

   public void selectKnot (Knot knot) {
      mySelectedKnots.add (knot);
      Dragger3dBase dragger = myDraggerToolBar.getDragger();
      if (dragger != null) {
         centerDraggerOnSelection (dragger);
         dragger.setVisible(true);
      }
   }

   public boolean isSelected (Knot knot) {
      return mySelectedKnots.contains (knot);
   }

   public int numSelectedKnots() {
      return mySelectedKnots.size();
   }

   public Knot getSelectedKnot (int idx) {
      return mySelectedKnots.get(idx);
   }

   public double getYScale () {
      return myYScale;
   }

   public void setYScale (double s) {
      if (mySpline != null) {
         mySpline.scaleY (s/myYScale);
      }
      if (myYScaleField.getDoubleValue() != s) {
         myYScaleField.setValue(s);
      }
      myYScale = s;
   }

   public File getSplineFile () {
      return mySplineFile;
   }

   public void setSplineFile (File file) {
      mySplineFile = file;
   }

   public File getCurveFile () {
      return myCurveFile;
   }

   public void setCurveFile (File file) {
      myCurveFile = file;
   }

   public boolean loadSpline (File file) {
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (file)));
         mySpline = new CubicHermiteSpline1d();
         mySpline.scan (rtok, null);

         if (myRenderer != null) {
            viewer.removeRenderable (myRenderer);
         }
         myRenderer = new SplineRenderer (mySpline);
         viewer.addRenderable (myRenderer);
         viewer.autoFitOrtho();
         return true;
      }
      catch (Exception e) {
         GuiUtils.showError (this, "Can't load file: " + e);
         return false;
      }
   }

   public boolean loadCurve (File file) {
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (file)));
         ArrayList<Double> xyvals = new ArrayList<>();
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            if (!rtok.tokenIsNumber()) {
               throw new IOException ("numeric x value expected, " + rtok);
            }
            xyvals.add (rtok.nval);
            rtok.nextToken();
            if (!rtok.tokenIsNumber()) {
               throw new IOException ("numeric y value expected, " + rtok);
            }
            xyvals.add (rtok.nval);
         }
         int npairs = xyvals.size()/2;
         double[] x = new double[npairs];
         double[] y = new double[npairs];
         int k = 0;
         for (int i=0; i<npairs; i++) {
            x[i] = xyvals.get(k++);
            y[i] = xyvals.get(k++);
         }
         myCurve = new Curve (x, y);
         viewer.addRenderable (myCurve);
         viewer.autoFitOrtho();
         return true;
      }
      catch (Exception e) {
         GuiUtils.showError (this, "Can't load file: " + e);
         return false;
      }
   }

   private boolean saveSpline (File file) {
      try {
         PrintWriter pw = new IndentingPrintWriter (
            new FileWriter (file));
         mySpline.write (pw, new NumberFormat("%g"), null);
         pw.close();
         return true;
      }
      catch (Exception ex) {
         ex.printStackTrace(); 
         GuiUtils.showError (this, "Can't write file: " + ex);
         return false;
      }         
   }


   public HermiteSpline1dEditor (int w, int h) {
      super ("HermiteSpline1dEditor", w, h);
      init();
   }

   public HermiteSpline1dEditor (int w, int h, GLVersion version) {
      super ("HermiteSpline1dEditor", w, h, version);
      init();
   }

   private void init() {
      //viewer.getCanvas().addMouseListener (mouseHandler);
      //viewer.getCanvas().addMouseMotionListener (mouseHandler);

      JPopupMenu.setDefaultLightWeightPopupEnabled (false);

      viewer.addSelectionListener (new SelectionHandler());

      addMenuBar();
      addTopToolPanel();
      myYScaleField = new DoubleField ("yscale", getYScale(), "%g");
      myYScaleField.addValueChangeListener (this);
      myTopPanel.add (myYScaleField);
      addGridDisplay();

      viewer.setOrthogonal (10, .2, 200);
      viewer.setGridSizeAndPosition (new Point3d(), 10);
      viewer.setDefaultAxialView (AxisAlignedRotation.X_Y);
      viewer.setViewRotationEnabled (false);
      //viewer.setSelectOnPress (true);

      addPopupManager();
      addKeyListener();
      addDraggerToolBar (
         ButtonType.Select,
         ButtonType.TransRotate,
         ButtonType.AddPoint);
      myDraggerToolBar.setDraggerListener (this);
      myDraggerToolBar.setDrawToolListener (this);

   }

   protected void createFileMenu (JMenu menu) {
      addMenuItem (menu, "Load ...");
      if (mySpline != null) {
         if (mySplineFile != null) {
            addMenuItem (menu, "Save");
         }
         addMenuItem (menu, "Save as ...");
      }
      addMenuItem (menu, "Load curve ...");
      super.createFileMenu (menu);
   }

   protected void createPopupMenu (JPopupMenu popup) {
      if (numSelectedKnots() > 0) {
         JMenuItem item = new JMenuItem("Delete knot(s)");
         item.addActionListener(this);
         item.setActionCommand("Delete knots");
         popup.add(item);   
      }
      super.createPopupMenu (popup);
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();

      if (cmd.equals ("Load ...")) {
         JFileChooser chooser = new JFileChooser();
         if (getSplineFile() != null) {
            chooser.setSelectedFile (getSplineFile().getAbsoluteFile());
         }
         else {
            chooser.setCurrentDirectory (new File("."));
         }
         int retVal = chooser.showDialog (this, "Load");
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (loadSpline (file)) {
               setSplineFile (file);
            }
         }
      }
      else if (cmd.equals ("Save")) {
         if (!saveSpline (mySplineFile)) {
            setSplineFile (null);
         }
      }
      else if (cmd.equals ("Save as ...")) {
         JFileChooser chooser = new JFileChooser();
         if (getSplineFile() != null) {
            chooser.setSelectedFile (getSplineFile().getAbsoluteFile());
         }
         else {
            chooser.setCurrentDirectory (new File("."));
         }
         int retVal = chooser.showDialog (this, "Save as");
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (saveSpline (file)) {
               setSplineFile (file);
            }
         }
      }
      else if (cmd.equals ("Load curve ...")) {
         JFileChooser chooser = new JFileChooser();
         if (getCurveFile() != null) {
            chooser.setSelectedFile (getCurveFile().getAbsoluteFile());
         }
         else {
            chooser.setCurrentDirectory (new File("."));
         }
         int retVal = chooser.showDialog (this, "Load");
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (loadCurve (file)) {
               setCurveFile (file);
            }
         }
      }
      else if (cmd.equals ("Delete knots")) {
         for (Knot knot : mySelectedKnots) {
            mySpline.removeKnot (knot);
         }
         viewer.repaint();
      }
      else {
         super.actionPerformed (e);
      }
   }
   /**
    * interface face method for GLViewerListener.
    */
   public void renderOccurred (RendererEvent e) {
      updateWidgets();
   }

   static BooleanHolder drawAxes = new BooleanHolder (false);
   static DoubleHolder axisLength = new DoubleHolder (-1);
   static IntHolder glVersion = new IntHolder (3);
   static DoubleHolder yscale = new DoubleHolder (1);

   public static void main (String[] args) {
      StringHolder splineFileName = new StringHolder();
      StringHolder curveFileName = new StringHolder();
      IntHolder width = new IntHolder (640);
      IntHolder height = new IntHolder (480);

      ArgParser parser =
         new ArgParser ("java maspack.apps.HermiteSpline1dEditor");

      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-spline %s #file defining the spline", splineFileName);
      parser.addOption (
         "-curve %s #file defining the reference curve", curveFileName);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption (
         "-yscale %f #spline y value is yscale * viewer y value", yscale);
      parser.addOption (
         "-GLVersion %d{2,3} " + "#version of openGL for graphics", glVersion);

      parser.matchAllArgs (args);

      HermiteSpline1dEditor editor = null;
      try {
         GLVersion glv = (glVersion.value == 3 ? GLVersion.GL3 : GLVersion.GL2);
         editor = new HermiteSpline1dEditor (width.value, height.value, glv);
         GLViewer viewer = editor.getViewer();
         editor.setYScale (yscale.value);
         if (splineFileName.value != null) {
            File file = new File (splineFileName.value);
            if (editor.loadSpline (file)) {
               editor.setSplineFile (file);
            }
         }
         if (curveFileName.value != null) {
            File file = new File (curveFileName.value);
            if (editor.loadCurve (file)) {
               editor.setCurveFile (file);
            }
         }
         if (drawAxes.value) {
            if (axisLength.value > 0) {
               viewer.setAxisLength (axisLength.value);
            }
            else {
               viewer.setAxisLength (GLViewer.AUTO_FIT);
            }
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }

      editor.setVisible (true);
   }

   // DraggerListener interface

   public void draggerAdded (Dragger3dEvent e) {
      if (e.getSource() instanceof Dragger3dBase) {
         Dragger3dBase dragger = (Dragger3dBase)e.getSource();
         dragger.setVisible (numSelectedKnots()>0);
         centerDraggerOnSelection(dragger);
      }
   }

   public void draggerBegin (Dragger3dEvent e) {
      // store initial knot values:
      myKnotStartVals.clear();
      for (Knot knot : mySelectedKnots) {
         myKnotStartVals.add (
            new Vector3d (
               knot.getX(), knot.getY()/myYScale, knot.getDy()/myYScale));
      }
   }

   double clipX (Knot knot, double x) {
      if (mySpline.numKnots() > 1) {
         double tol = 
            1e-4*(mySpline.getLastKnot().getX() -
                  mySpline.getFirstKnot().getX());
         int idx = knot.getIndex();
         if (idx > 0) {
            double minx = mySpline.getKnot(idx-1).getX();
            if (x <= minx+tol) {
               x = minx+tol;
            }
         }
         if (idx < mySpline.numKnots()-1) {
            double maxx = mySpline.getKnot(idx+1).getX();
            if (x >= maxx-tol) {
               x = maxx-tol;
            }
         }
      }
      return x;
   }

   double clipAng (double ang) {
      double max = Math.toRadians (89);
      if (ang > max) {
         ang = max;
      }
      else if (ang < -max) {
         ang = -max;
      }
      return ang;
   }

   public void draggerMove (Dragger3dEvent e) {
      if (numSelectedKnots() > 0 &&
          e.getTransform() instanceof RigidTransform3d) {
         RigidTransform3d TDW =
            (RigidTransform3d)e.getTransform();
         for (int i=0; i<mySelectedKnots.size(); i++) {
            Vector3d val = myKnotStartVals.get(i);
            Knot knot = mySelectedKnots.get(i);
            knot.setX (clipX (knot, val.x + TDW.p.x));
            knot.setY (myYScale*(val.y + TDW.p.y));
         }
         if (numSelectedKnots() == 1) {
            Knot knot = mySelectedKnots.get(0);
            double ang0 = Math.atan (myKnotStartVals.get(0).z);
            double[] rpy = new double[3];
            TDW.R.getRpy (rpy);
            knot.setDy (myYScale*(Math.tan (clipAng (rpy[0] + ang0))));
         }
         mySpline.updateCoefficients();
      }
   }

   public void draggerEnd (Dragger3dEvent e) {
   }

   public void draggerRemoved (Dragger3dEvent e) {
   }

   private void addKnot (Point2d pnt) {
      if (mySpline == null) {
         mySpline = new CubicHermiteSpline1d();
         if (myRenderer != null) {
            viewer.removeRenderable (myRenderer);
         }
         myRenderer = new SplineRenderer (mySpline);
         viewer.addRenderable (myRenderer);
         viewer.rerender();
      }
      if (mySpline.numKnots() == 0) {
         mySpline.addKnot (pnt.x, pnt.y, /*dy=*/0);
      }
      else if (mySpline.numKnots() == 1) {
         Knot knot = mySpline.getKnot(0);
         double dx = pnt.x-knot.getX();
         double dy = (dx != 0 ? (pnt.y-knot.getY())/dx : 0); // ZZZ
         // if dx == 0, then the "added" knot will simply replace the old one
         mySpline.addKnot (pnt.x, pnt.y, dy);
      }
      else {
         double dy = mySpline.evalDy (pnt.x);
         mySpline.addKnot (pnt.x, pnt.y, dy);
      }
   }

   // DrawToolListener interface

   public void drawToolAdded (DrawToolEvent e) {
      if (e.getSource() instanceof PointTool) {
         PointTool tool = (PointTool)e.getSource();
         tool.setFrameBinding (PointTool.FrameBinding.INTERNAL_FRAME);
         tool.setFrame (new RigidTransform3d());
         //tool.setVisible (false);
         viewer.getCanvas().setCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
      }
   }

   public void drawToolBegin (DrawToolEvent e) {
   }

   public void drawToolEnd (DrawToolEvent e) {
      if (e.getSource() instanceof PointTool) {
         PointTool tool = (PointTool)e.getSource();
         addKnot (tool.getPoint(tool.numPoints()-1));
         tool.clear();
         viewer.repaint();
      }
   }

   public void drawToolRemoved (DrawToolEvent e) {
      if (e.getSource() instanceof PointTool) {
         viewer.getCanvas().setCursor (Cursor.getDefaultCursor());         
      }
   }

   // Value change listener

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() == myYScaleField) {
         setYScale (myYScaleField.getDoubleValue());
      }
   }
}

