/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import maspack.geometry.MeshFactory;
import maspack.geometry.NURBSCurve3d;
import maspack.geometry.NURBSMesh;
import maspack.geometry.NURBSObject;
import maspack.geometry.NURBSSurface;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.WavefrontReader;
import maspack.matrix.*;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionListener;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.GLVersion;
import maspack.render.GL.GLViewerFrame;
import maspack.widgets.*;
import maspack.widgets.DraggerToolBar.ButtonType;
import maspack.interpolation.*;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

public class HermiteSpline1dEditor extends ViewerFrame 
   implements ActionListener, RenderListener, Dragger3dListener {
   
   private static final long serialVersionUID = 1L;

   CubicHermiteSpline1d mySpline;
   SplineRenderer myRenderer;
   ArrayList<Knot> mySelectedKnots = new ArrayList<>();
   // stores knot data at the start of a drag:
   ArrayList<Vector3d> myKnotStartVals = new ArrayList<>();
   

   JFileChooser mySplineChooser = new JFileChooser();
   //JPanel myToolBar;           // tool bar across the top of the frame

   //JLabel mySplineLabel;
   //String myLastSplineName;
   //DraggerToolBar myDraggerToolBar;

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
            System.out.println ("qid=" + qid);
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
            renderer.drawPoint (new Vector3d (knot.getX(), knot.getY(), 0));
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
               double dy = Math.abs(next.getY() - knot.getY());
               int ns = (int)Math.ceil(nstotal*(dx+dy)/dxtotal);
               for (int i=0; i<=ns; i++) {
                  double x = knot.getX() + i*dx/ns;
                  double y = mySpline.eval (x);               
                  renderer.addVertex (new Vector3d (x, y, 0));
               }
            }
         }
         renderer.endDraw();
      }

      public void updateBounds (Vector3d pmin, Vector3d pmax) {
         for (int i=0; i<mySpline.numKnots(); i++) {
            Knot knot = mySpline.getKnot(i);
            Point3d pnt = new Point3d (knot.getX(), knot.getY(), 0);
            pnt.updateBounds (pmin, pmax);
         }
      }

      public int getRenderHints() {
         return 0;
      }
   }

   class SelectionHandler implements ViewerSelectionListener {

      public void itemsSelected (ViewerSelectionEvent e) {
         boolean holdSelection, selectAll;

         long modEx = e.getModifiersEx();
         holdSelection = ((modEx & MouseEvent.SHIFT_DOWN_MASK) != 0);
         selectAll = ((modEx & MouseEvent.ALT_DOWN_MASK) != 0);

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
                  // NURBSObject nurbsObj = (NURBSObject)path.getFirst();
                  // if (path.size() > 1 && path.get (1) instanceof Integer) {
                  //    int idx = ((Integer)path.get (1)).intValue();
                  //    if (!nurbsObj.controlPointIsSelected (idx)) {
                  //       nurbsObj.selectControlPoint (idx, true);
                  //       selectedPnts.add (nurbsObj.getControlPoints()[idx]);
                  //    }
                  //    else {
                  //       nurbsObj.selectControlPoint (idx, false);
                  //       selectedPnts.remove (nurbsObj.getControlPoints()[idx]);
                  //    }
                  //    if (!selectAll) {
                  //       break;
                  //    }
                  // }
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
         cy += k.getY();
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


   private void translateKnot (double dx, double dy) {
      Knot knot = getSelectedKnot (0);
      knot.setX (knot.getX()+dx);
      knot.setY (knot.getY()+dy);
      mySpline.updateCoefficients (knot);
   }
         

   class MouseHandler extends MouseInputAdapter {
      boolean dragging = false;
      int lastX;
      int lastY;

      public void mousePressed (MouseEvent e) {
         int modEx = e.getModifiersEx();
         if ((modEx & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            if (myRenderer != null && numSelectedKnots() > 0) {
               dragging = true;
               lastX = e.getX();
               lastY = e.getY();
            }
         }
      }

      // public void mouseClicked (MouseEvent e) {
      //    if (e.getClickCount() == 2) {
      //       myRenderer.clearSelectedKnots();
      //       viewer.getCanvas().repaint();
      //    }
      // }

      public void mouseReleased (MouseEvent e) {
         dragging = false;
      }

      public void mouseDragged (MouseEvent e) {
         if (dragging) {
            RigidTransform3d XV = new RigidTransform3d();
            viewer.getViewMatrix (XV);
            Vector3d del =
               new Vector3d (e.getX() - lastX, lastY - e.getY(), 0);
            del.inverseTransform (XV);
            del.scale (viewer.centerDistancePerPixel());
            Vector4d del4d = new Vector4d (del.x, del.y, del.z, 0);
            System.out.println ("del=" + del.x + " " + del.y);
            translateKnot (del.x, del.y);
            
            lastX = e.getX();
            lastY = e.getY();
            
            viewer.getCanvas().repaint();
         }
      }
   }

   public void loadSpline (File file) {

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
      }
      catch (Exception e) {
         GuiUtils.showError (this, "Can't load file: " + e);
      }
      
   }

   private boolean saveSpline (File file) {
      try {
         PrintWriter pw = new IndentingPrintWriter (
            new FileWriter (file));
         mySpline.write (pw, new NumberFormat("%g"), null);
         pw.close();
      }
      catch (Exception ex) {
         ex.printStackTrace(); 
         GuiUtils.showError (this, "Can't write file: " + ex);
      }         
      return false;
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
      MouseHandler mouseHandler = new MouseHandler();
      //viewer.getCanvas().addMouseListener (mouseHandler);
      //viewer.getCanvas().addMouseMotionListener (mouseHandler);
      viewer.addSelectionListener (new SelectionHandler());

      addMenuBar();
      addTopToolPanel();

      viewer.autoFitOrtho ();
      viewer.setAxialView (AxisAlignedRotation.X_Y);
      //viewer.setSelectOnPress (true);

      addPopupManager();
      addKeyListener();
      addDraggerToolBar (
         ButtonType.Select,
         ButtonType.TransRotate,
         ButtonType.AddPoint);
      myDraggerToolBar.setDraggerListener (this);

      mySplineChooser.setCurrentDirectory (new File("."));
   }

   protected void createFileMenu (JMenu menu) {
      addMenuItem (menu, "Load spline ...");
      if (mySpline != null) {
         addMenuItem (menu, "Save spline ...");
      }
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

//   private void createMenuBar () {
//      JMenuBar menuBar = new JMenuBar();
//      setJMenuBar (menuBar);
//
//      JMenu menu = new JMenu ("File");
//      menu.addMenuListener(new MenuAdapter() {
//         public void menuSelected(MenuEvent m_evt) {
//            createFileMenu((JMenu)m_evt.getSource());
//         }
//      });      
//
//      menuBar.add (menu);      
//
//      // menu = new JMenu ("View");
//      // menu.addMenuListener(new MenuAdapter() {
//      //    public void menuSelected(MenuEvent m_evt) {
//      //       createViewMenu((JMenu)m_evt.getSource());
//      //    }
//      // });      
//
//      // menuBar.add (menu);   
//      
//   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();

      if (cmd.equals ("Load spline ...")) {
         int retVal = mySplineChooser.showOpenDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = mySplineChooser.getSelectedFile();
            loadSpline (file);
         }
      }
      else if (cmd.equals ("Save spline ...")) {
         int retVal = mySplineChooser.showOpenDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = mySplineChooser.getSelectedFile();
            saveSpline (file);
         }
      }
      else if (cmd.equals ("Delete knots")) {
         for (Knot knot : mySelectedKnots) {
            mySpline.removeKnot (knot);
         }
      }
      else {
         super.actionPerformed (e);
      }
   }

   // public void setBackgroundColor() {

   //    myColorChooser.setColor(viewer.getBackgroundColor());

   //    ActionListener setBColor = new ActionListener() {   
   //       @Override
   //       public void actionPerformed(ActionEvent e) {
   //          String cmd = e.getActionCommand();
   //          if (cmd.equals("OK")) {
   //             viewer.setBackgroundColor(myColorChooser.getColor());
   //          } else if (cmd.equals("Cancel")) {
   //             // do nothing
   //          }
   //       }
   //    };
   //    JDialog dialog =
   //    JColorChooser.createDialog(
   //       this, "color chooser", /* modal= */true, myColorChooser,
   //       setBColor, setBColor);
   //    GuiUtils.locateRight(dialog, this);
   //    dialog.setVisible(true);
   // }

   // private void createToolBars() {
   //    myToolBar = new JPanel();
   //    myToolBar.setLayout(new BoxLayout(myToolBar, BoxLayout.LINE_AXIS));
   //    myToolBar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
   //    mySplineLabel = new JLabel(" ");
   //    myToolBar.add (mySplineLabel);

   //    // add glue to separate label from grid display
   //    myToolBar.add(Box.createHorizontalGlue());
   //    myGridDisplayIndex = myToolBar.getComponentCount();
   //    // add a place-holder component for the grid display
   //    myToolBar.add(GridDisplay.createPlaceHolder());

   //    getContentPane().add(myToolBar, BorderLayout.NORTH);
   //    JPanel leftPanel = new JPanel(new BorderLayout());
   // }

   // public void updateWidgets() {
   //    // return if  not visible, since updating widgets while
   //    // frame is being set visible can cause some problems
   //    if (!isVisible()) {
   //       return;
   //    }
   //    boolean gridOn = viewer.getGridVisible();
   //    GLGridPlane grid = viewer.getGrid();
   //    if ((myGridDisplay != null) != gridOn) {
   //       if (gridOn) {
   //          myGridDisplay =
   //             GridDisplay.createAndAdd (grid, myToolBar, myGridDisplayIndex);
   //       }
   //       else {
   //          GridDisplay.removeAndDispose (
   //             myGridDisplay, myToolBar, myGridDisplayIndex);
   //          myGridDisplay = null;
   //       }
   //    }
   //    if (myGridDisplay != null) {
   //       myGridDisplay.updateWidgets();
   //    }
   // }

   /**
    * interface face method for GLViewerListener.
    */
   public void renderOccurred (RendererEvent e) {
      updateWidgets();
   }

   static BooleanHolder drawAxes = new BooleanHolder (false);
   static DoubleHolder axisLength = new DoubleHolder (-1);
   static IntHolder glVersion = new IntHolder (3);

   public static void main (String[] args) {
      StringHolder fileName = new StringHolder();
      IntHolder width = new IntHolder (640);
      IntHolder height = new IntHolder (480);

      ArgParser parser = new ArgParser ("java maspack.apps.HermiteSpline1dEditor");
      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-spline %s #file defining the spline", fileName);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption (
         "-GLVersion %d{2,3} " + "#version of openGL for graphics", glVersion);

      parser.matchAllArgs (args);

      HermiteSpline1dEditor viewFrame = null;
      try {
         GLVersion glv = (glVersion.value == 3 ? GLVersion.GL3 : GLVersion.GL2);
         viewFrame = new HermiteSpline1dEditor (width.value, height.value, glv);
         GLViewer viewer = viewFrame.getViewer();
         if (fileName.value != null) {
            viewFrame.loadSpline (new File (fileName.value));
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

      viewFrame.setVisible (true);
   }

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
            new Vector3d (knot.getX(), knot.getY(), knot.getDy()));
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
            knot.setY (val.y + TDW.p.y);
         }
         if (numSelectedKnots() == 1) {
            Knot knot = mySelectedKnots.get(0);
            double ang0 = Math.atan (myKnotStartVals.get(0).z);
            double[] rpy = new double[3];
            TDW.R.getRpy (rpy);
            knot.setDy (Math.tan (clipAng (rpy[0] + ang0)));
         }
         mySpline.updateCoefficients();
      }
   }

   public void draggerEnd (Dragger3dEvent e) {
   }

   public void draggerRemoved (Dragger3dEvent e) {
   }
}
