/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.MenuEvent;

import argparser.ArgParser;
import argparser.IntHolder;
import argparser.StringHolder;
import maspack.geometry.DistanceGrid;
import maspack.geometry.HalfEdge;
import maspack.geometry.LaplacianSmoother;
import maspack.geometry.MeshBase;
import maspack.geometry.NURBSCurve2d;
import maspack.geometry.NURBSCurve3d;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.QuadBezierDistance2d;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.GenericMeshReader;
import maspack.geometry.io.GenericMeshWriter;
import maspack.geometry.io.XyzbReader;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.render.Dragger3d;
import maspack.render.DrawToolBase.FrameBinding;
import maspack.render.DrawToolEvent;
import maspack.render.DrawToolListener;
import maspack.render.RenderListener;
import maspack.render.RenderProps;
import maspack.render.RenderableBase;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.FaceStyle;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionListener;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.widgets.DraggerToolBar;
import maspack.widgets.DraggerToolBar.ButtonType;
import maspack.widgets.GuiUtils;
import maspack.widgets.MenuAdapter;
import maspack.widgets.PropertyDialog;
import maspack.widgets.RenderPropsPanel;
import maspack.widgets.SplineTool;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ViewerFrame;

public class MeshThicken extends ViewerFrame 
   implements ActionListener, DrawToolListener, RenderListener,
              HasProperties, ViewerSelectionListener {

   private static final long serialVersionUID = 1L;
   ArrayList<Region> myRegions = new ArrayList<Region>();
   MeshBase myMesh;
   JFileChooser myRegionChooser = new JFileChooser();
   JFileChooser myMeshChooser = new JFileChooser();
   File myMeshFile = null;
   File myRegionFile = null;
   static final double DEFAULT_REGION_HEIGHT = 8;
   static final double DEFAULT_REGION_MARGIN = 5;
   static final double DEFAULT_REGION_THICKENING = 0.5;
   static final boolean DEFAULT_NORMAL_Z_SCALING = false;
   static final boolean DEFAULT_REGION_THICKEN_BACK_SIDE = true;
   static double DEFAULT_SMOOTHING_LAMBDA = 0.8;
   static double DEFAULT_SMOOTHING_MU = -0.8160;
   static int DEFAULT_SMOOTHING_COUNT = 10;
   static int DEFAULT_GROW_LENGTH = 1;
   static Vector3i DEFAULT_REMESH_RES = new Vector3i (20, 20, 20);

   double myDefaultRegionHeight = DEFAULT_REGION_HEIGHT;
   double myDefaultRegionMargin = DEFAULT_REGION_MARGIN;
   double myDefaultRegionThickening = DEFAULT_REGION_THICKENING;
   boolean myDefaultThickenBackSide = DEFAULT_REGION_THICKEN_BACK_SIDE;
   double myDefaultRegionUnthickening = DEFAULT_REGION_THICKENING;
   double mySmoothingLambda = DEFAULT_SMOOTHING_LAMBDA;
   double mySmoothingMu = DEFAULT_SMOOTHING_MU;
   int mySmoothingCount = DEFAULT_SMOOTHING_COUNT;
   double myGrowLength = DEFAULT_GROW_LENGTH;
   Vector3i myRemeshRes = new Vector3i (DEFAULT_REMESH_RES);

   protected ValueChangeListener myRerenderListener =
      new ValueChangeListener () {
         public void valueChange (ValueChangeEvent evt) {
            viewer.rerender();
         }
      };
     
   public static class Region extends RenderableBase implements HasProperties {
      NURBSCurve2d myCurve;
      QuadBezierDistance2d myDist;
      double myHeight;
      double myBackHeight;
      double myMargin;
      double myThickening;
      double myUnthickening;
      boolean myThickenBackSideP = DEFAULT_REGION_THICKEN_BACK_SIDE;
      boolean myUseNormalZScalingP = DEFAULT_NORMAL_Z_SCALING;
      RigidTransform3d myFrame;
      protected int myResolution = 5;
      protected int myOrientation = 1;
      protected boolean mySelectedP = false;
      protected boolean myVisibleP = true;
      protected double myMinRadius = 0;

      public RenderProps createRenderProps() {
         RenderProps props = new RenderProps();
         props.setFaceColor (new Color(0.5f, 0.5f, 1f));
         props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
         return props;
      }

      public static PropertyList myProps =
         new PropertyList (MeshThicken.Region.class);

      static {
         myProps.add (
            "height", "height of this region",
            DEFAULT_REGION_HEIGHT);
         myProps.add (
            "backHeight", "height of the back of this region",
            DEFAULT_REGION_HEIGHT);
         myProps.add (
            "margin", "margin of this region",
            DEFAULT_REGION_MARGIN);
         myProps.add (
            "useNormalZScaling",
            "scale the thickening by the z component of the normal",
            DEFAULT_NORMAL_Z_SCALING);
         myProps.add (
            "thickening",
            "thickening to be applied to this region",
            DEFAULT_REGION_THICKENING);
         myProps.add (
            "unthickening",
            "unthickening to be applied to this region",
            DEFAULT_REGION_THICKENING);
         myProps.add (
            "thickenBackSide",
            "thicken back side as well as the front of the region",
            DEFAULT_REGION_THICKEN_BACK_SIDE);
         myProps.addReadOnly (
            "minRadius", "recommended upper bound for margin");
      }

      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      /**
       * {@inheritDoc}
       */
      public Property getProperty (String name) {
         return myProps.getProperty (name, this);
      }

      public double getMinRadius() {
         return myMinRadius;
      }

      public double getMargin () {
         return myMargin;
      }

      public void setMargin (double margin) {
         myMargin = margin;
      }

      public double getHeight () {
         return myHeight;
      }

      public void setHeight (double height) {
         myHeight = height;
         updateCurveFrame ();
      }

      public double getBackHeight () {
         return myBackHeight;
      }

      public void setBackHeight (double backHeight) {
         myBackHeight = backHeight;
         updateCurveFrame ();
      }

      public boolean getVisible () {
         return myVisibleP;
      }

      public void setVisible (boolean visible) {
         myVisibleP = visible;
      }

      public double getThickening () {
         return myThickening;
      }

      public void setThickening (double thickening) {
         myThickening = thickening;
      }

      public void setThickenBackSide (boolean enable) {
         myThickenBackSideP = enable;
      }

      public boolean getThickenBackSide () {
         return myThickenBackSideP;
      }

      public void setUseNormalZScaling (boolean enable) {
         myUseNormalZScalingP = enable;
      }

      public boolean getUseNormalZScaling () {
         return myUseNormalZScalingP;
      }

      public double getUnthickening () {
         return myUnthickening;
      }

      public void setUnthickening (double unthickening) {
         myUnthickening = unthickening;
      }

      Region () {
         myRenderProps = createRenderProps();
         myFrame = new RigidTransform3d();
      }

      Region (NURBSCurve2d curve, RigidTransform3d X, double height) {
         this();
         myFrame.set (X);
         myHeight = height;
         myBackHeight = height;
         setCurve (new NURBSCurve2d (curve));
      }

      boolean isSelected() {
         return mySelectedP;
      }

      void setSelected (boolean selected) {
         mySelectedP = selected;
      }

      void updateCurveFrame () {
         RigidTransform3d X = new RigidTransform3d(myFrame);
         X.mulXyz (0, 0, myHeight);
         myCurve.setObjToWorld (X);
      }

      void setCurve (NURBSCurve2d curve) {
         myCurve = curve;
         myCurve.setDrawControlShape (false);
         if (!myCurve.isClosed()) {
            // make sure that the curve is closed
            myCurve.reset (
               myCurve.getDegree(), NURBSCurve3d.CLOSED, /*knots=*/null);
         }
         myDist = new QuadBezierDistance2d (myCurve);
         myMinRadius = 1/myDist.computeMaxCurvature();
         updateCurveFrame ();
         myOrientation = myCurve.getOrientation();
      }

      void scan (ReaderTokenizer rtok) throws IOException {

         boolean backHeightSpecified = false;
         rtok.scanToken ('[');
         NURBSCurve2d curve = null;
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            String fieldName = rtok.scanWord();
            rtok.scanToken ('=');
            if (fieldName.equals ("curve")) {
               rtok.scanToken ('[');
               curve = new NURBSCurve2d();
               curve.read (rtok);
               rtok.scanToken (']');
            }
            else if (fieldName.equals ("margin")) {
               myMargin = rtok.scanNumber();
            }
            else if (fieldName.equals ("thickening")) {
               myThickening = rtok.scanNumber();
            }
            else if (fieldName.equals ("unthickening")) {
               myUnthickening = rtok.scanNumber();
            }
            else if (fieldName.equals ("thickenBackSide")) {
               myThickenBackSideP = rtok.scanBoolean();
            }
            else if (fieldName.equals ("useNormalZScaling")) {
               myUseNormalZScalingP = rtok.scanBoolean();
            }
            else if (fieldName.equals ("height")) {
               myHeight = rtok.scanNumber();
            }
            else if (fieldName.equals ("backHeight")) {
               myBackHeight = rtok.scanNumber();
               backHeightSpecified = true;
            }
            else if (fieldName.equals ("frame")) {
               myFrame.scan (rtok);
            }
         }
         if (!backHeightSpecified) {
            myBackHeight = myHeight;
         }
         if (curve != null) {
            setCurve (curve);
         }
      }

      void write (PrintWriter pw) throws IOException {
         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         if (myCurve != null) {
            IndentingPrintWriter.addIndentation (pw, 2);
            pw.println ("curve=[");
            myCurve.write (pw);
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("EOF\n]");
         }
         pw.println ("margin=" + myMargin);
         pw.println ("thickening=" + myThickening);
         pw.println ("thickenBackSide=" + myThickenBackSideP);
         pw.println ("useNormalZScaling=" + myUseNormalZScalingP);
         pw.println ("unthickening=" + myUnthickening);
         pw.println ("height=" + myHeight);
         pw.println ("backHeight=" + myBackHeight);
         pw.println ("frame=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         myFrame.write (pw, new NumberFormat ("%.8g"));
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");  
         pw.flush();
      }

      public void render (Renderer renderer, int flags) {
         render (renderer, myRenderProps, /*flags=*/0);
      }

      public void render (Renderer renderer, RenderProps props, int flags) {

         if (!myVisibleP) {
            return;
         }
         if (myCurve != null) {
            myCurve.render (renderer, flags);
         }
         
         renderer.pushModelMatrix();
         RigidTransform3d X = new RigidTransform3d (myFrame);
         X.mulXyz (0, 0, myHeight);
         renderer.mulModelMatrix (X);

         //draw the curve itself
         renderer.setFaceColoring (props, mySelectedP);

         double len = myCurve.computeControlPolygonLength();
         double res = myResolution*renderer.distancePerPixel (X.p);
         int nsegs = (int)Math.max(10, len/res);      

         Point3d pnt0 = new Point3d();
         Point3d pnt1 = new Point3d();
         Point3d pnt2 = new Point3d();
         Point3d pnt3 = new Point3d();
         Vector3d nrm = new Vector3d();
         Vector3d zdir = new Vector3d(0, 0, 1);
         //myFrame.R.getColumn (2, zdir);

         renderer.setFaceStyle (props.getFaceStyle());

         renderer.beginDraw (DrawMode.TRIANGLES);
         double[] urange = new double[2];
         myCurve.getRange (urange);
         myCurve.eval (pnt0, urange[0]);
         double depth = myHeight+myBackHeight;
         for (int i=1; i<=nsegs; i++) {
            myCurve.eval (pnt3, urange[0] + (urange[1]-urange[0])*i/nsegs);
            if (myOrientation < 0) {
               nrm.sub (pnt1, pnt0);
            }
            else {
               nrm.sub (pnt0, pnt1);
            }
            nrm.cross (zdir);
            nrm.normalize();
            //nrm.negate();

            pnt1.scaledAdd (-depth, zdir, pnt0);
            pnt2.scaledAdd (-depth, zdir, pnt3);
            renderer.setNormal (nrm.x, nrm.y, nrm.z);
            renderer.addVertex (pnt0); // first triangle
            renderer.addVertex (pnt1);
            renderer.addVertex (pnt2); 
            renderer.addVertex (pnt0); // second triangle
            renderer.addVertex (pnt2);
            renderer.addVertex (pnt3);
            pnt0.set (pnt3);
         }
         renderer.endDraw();         
         
         renderer.setFaceStyle (FaceStyle.FRONT); // set default

         renderer.popModelMatrix();
      }

      public boolean isSelectable() {
         return true;
      }
   }

   public static PropertyList myProps = new PropertyList (MeshThicken.class);

   static {
      myProps.add (
         "defaultRegionHeight", "default height of thickening regions",
         DEFAULT_REGION_HEIGHT);
      myProps.add (
         "defaultRegionMargin", "default margin of thickening regions",
         DEFAULT_REGION_MARGIN);
      myProps.add (
         "defaultRegionThickening",
         "default thickening to be applied to regions",
         DEFAULT_REGION_THICKENING);
      myProps.add (
         "defaultRegionUnthickening",
         "default unthickening to be applied to regions",
         DEFAULT_REGION_THICKENING);
      myProps.add (
         "smoothingLambda", "lambda for two-stage Laplacian smoothing",
         DEFAULT_SMOOTHING_LAMBDA);
      myProps.add (
         "smoothingMu", "mu for two-stage Laplacian smoothing",
         DEFAULT_SMOOTHING_MU);
      myProps.add (
         "smoothingCount", "count for two-stage Laplacian smoothing",
         DEFAULT_SMOOTHING_COUNT);
      myProps.add (
         "growLength",
         "amount to grow vertices by",
         DEFAULT_GROW_LENGTH);
      myProps.add (
         "remeshRes",
         "resolutions to use when remeshing",
         DEFAULT_REMESH_RES);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String name) {
      return myProps.getProperty (name, this);
   }

   public MeshThicken (String meshFileName, String regionFileName) {
      super ("MeshThicken",  640, 480);

      addMenuBar();
      addGridDisplay ();
      addViewerToolBar (JToolBar.VERTICAL);
      addDraggerToolBar (
         ButtonType.Select,
         ButtonType.Spline);
      DraggerToolBar draggerBar = getDraggerToolBar();
      draggerBar.setDrawToolListener (this);
      draggerBar.setDrawToolFrameBinding (FrameBinding.CLIPPING_PLANE);
      draggerBar.setDrawToolFrameOffset (myDefaultRegionHeight);
      draggerBar.setSplineToolMaxDegree (2);
      addPopupManager();
      addKeyListener();
      pack();

      viewer.setAxialView (AxisAlignedRotation.X_Z);
      if (meshFileName != null) {
         loadMesh (new File(meshFileName), skipCount.value);
      }
      if (regionFileName != null) {
         loadRegions (new File (regionFileName));
      }
      myRegionChooser.setCurrentDirectory (new File("."));
      myMeshChooser.setCurrentDirectory (new File("."));

      viewer.addSelectionListener (this);
   }

   public double getDefaultRegionHeight() {
      return myDefaultRegionHeight;
   }

   public void setDefaultRegionHeight (double height) {
      myDefaultRegionHeight = height;
   }

   public double getDefaultRegionMargin() {
      return myDefaultRegionMargin;
   }

   public void setDefaultRegionMargin (double margin) {
      myDefaultRegionMargin = margin;
   }

   public double getDefaultRegionThickening() {
      return myDefaultRegionThickening;
   }

   public void setDefaultRegionThickening (double thickening) {
      myDefaultRegionThickening = thickening;
   }

   public double getDefaultRegionUnthickening() {
      return myDefaultRegionUnthickening;
   }

   public void setDefaultRegionUnthickening (double unthickening) {
      myDefaultRegionUnthickening = unthickening;
   }

   public double getSmoothingLambda () {
      return mySmoothingLambda;
   }

   public void setSmoothingLambda (double lam) {
      mySmoothingLambda = lam;
   }

   public double getSmoothingMu () {
      return mySmoothingMu;
   }

   public void setSmoothingMu (double mu) {
      mySmoothingMu = mu;
   }

   public int getSmoothingCount () {
      return mySmoothingCount;
   }

   public void setSmoothingCount (int count) {
      mySmoothingCount = count;
   }

   public double getGrowLength () {
      return myGrowLength;
   }

   public void setGrowLength (double len) {
      myGrowLength = len;
   }

   public Vector3i getRemeshRes () {
      return myRemeshRes;
   }

   public void setRemeshRes (Vector3i res) {
      myRemeshRes.set (res);
   }

   public JMenuBar addMenuBar() {
      JMenuBar menuBar = super.addMenuBar ();

      JMenu menu = new JMenu ("Edit");
      menu.addMenuListener(new MenuAdapter() {
         public void menuSelected(MenuEvent m_evt) {
            createEditMenu((JMenu)m_evt.getSource());
         }
      });      
      menuBar.add (menu);  
      return menuBar;
   }

   protected void createFileMenu (JMenu menu) {
      JMenuItem item = addMenuItem (menu, "Load mesh ...");
      item = addMenuItem (menu, "Save mesh");
      item.setEnabled (myMeshFile != null);
      item = addMenuItem (menu, "Save mesh as ...");
      item.setEnabled (myMesh != null);
      item = addMenuItem (menu, "Clear regions");
      item.setEnabled (myRegions.size() > 0);
      addMenuItem (menu, "Load regions ...");
      item = addMenuItem (menu, "Save regions");
      item.setEnabled (myRegionFile != null);
      addMenuItem (menu, "Save regions as ...");
      super.createFileMenu (menu);
   }

   protected void createViewMenu (JMenu menu) {
      JMenuItem item = addMenuItem (menu, "Hide all regions");
      item = addMenuItem (menu, "Show all regions");
      super.createViewMenu (menu);
   }

   protected void createEditMenu (JMenu menu) {
      JMenuItem item;
      item = addMenuItem (menu, "Thicken");
      item.setEnabled (myRegions.size() > 0);
      item = addMenuItem (menu, "Reverse thicken");
      item.setEnabled (myRegions.size() > 0);
      item = addMenuItem (menu, "Unthicken");
      item.setEnabled (myRegions.size() > 0);
      item = addMenuItem (menu, "Reverse unthicken");
      item.setEnabled (myRegions.size() > 0);
      item = addMenuItem (menu, "Smooth");
      item.setEnabled (myMesh instanceof PolygonalMesh);
      item = addMenuItem (menu, "Grow");
      item.setEnabled (myMesh instanceof PolygonalMesh);
      item = addMenuItem (menu, "Remesh");
      item.setEnabled (myMesh instanceof PolygonalMesh);
   }

   public void loadRegions (File file) {
      try {
         ArrayList<Region> regions = new ArrayList<Region>();
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (file)));
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            Region region = new Region();
            region.scan (rtok);
            regions.add (region);
         }
         myRegionFile = file;
         clearRegions();
         for (Region region : regions) {
            addRegion (region);
         }
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   public void saveRegions (File file) {
      try {
         PrintWriter pw = new IndentingPrintWriter (
            new PrintWriter (new BufferedWriter (new FileWriter (file))));
         pw.println ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);         
         for (Region region : myRegions) {
            region.write (pw);
         }
         IndentingPrintWriter.addIndentation (pw, -2);         
         pw.println ("]");
         pw.close();
         myRegionFile = file;
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   protected Region myEditingRegion;

   public void drawToolAdded (DrawToolEvent e) {
      Dragger3d tool = e.getSource();
      System.out.println ("Added " + tool);
      myEditingRegion = null;
      if (tool instanceof SplineTool) {
         SplineTool splineTool = (SplineTool)tool;
         Region selectedRegion = null;
         for (Region region : myRegions) {
            if (region.isSelected()) {
               selectedRegion = region;
            }
         }
         if (selectedRegion != null) {
            splineTool.setFrame (selectedRegion.myFrame);
            splineTool.setFrameOffset (selectedRegion.myHeight);
            splineTool.setCurve (selectedRegion.myCurve);
            splineTool.setFrameBinding (FrameBinding.INTERNAL_FRAME);
            myEditingRegion = selectedRegion;
         }
      }
   }

   public void drawToolBegin (DrawToolEvent e) {
   }

   public void drawToolEnd (DrawToolEvent e) {
      System.out.println ("Entered");
      Dragger3d tool = e.getSource();
      if (tool instanceof SplineTool) {
         SplineTool splineTool = (SplineTool)tool;
         NURBSCurve2d curve = splineTool.getCurve();
         if (curve != null) {
            if (myEditingRegion != null) {
               myEditingRegion.setCurve (new NURBSCurve2d (curve));
               myEditingRegion = null;
            }
            else {
               RigidTransform3d X = new RigidTransform3d();
               splineTool.getToolToWorld (X);
               X.mulXyz (0, 0, -myDefaultRegionHeight);
               Region region = new Region (curve, X, myDefaultRegionHeight);
               region.myMargin = myDefaultRegionMargin;
               region.myThickening = myDefaultRegionThickening;
               region.myUnthickening = myDefaultRegionUnthickening;
               addRegion (region);
            }
         }
         splineTool.clear();
         viewer.rerender();
      }

   }

   public void drawToolRemoved (DrawToolEvent e) {
   }


   public void loadMesh (File file, int vertexSkip) {
      try {
         MeshBase mesh = null;
         if (file.getName().endsWith (".xyzb")) {
            XyzbReader reader = new XyzbReader (file);
            reader.setSkip (vertexSkip);
            mesh = reader.readMesh();
         }
         else {
            mesh = GenericMeshReader.readMesh (file);
         }
         removeMesh();
         myMesh = mesh;
         RenderProps.setFaceStyle (mesh, Renderer.FaceStyle.FRONT_AND_BACK);
         RenderProps.setBackColor (mesh, new Color (1f, 204/255f, 51/355f));
         viewer.addRenderable (myMesh);
         viewer.repaint();
         myMeshFile = file;
         System.out.println ("num vertices: " + mesh.numVertices());
         if (mesh instanceof PolygonalMesh) {
            PolygonalMesh pmesh = (PolygonalMesh)mesh;
            System.out.println ("num faces: " + pmesh.numFaces());
         }
         setTitle ("MeshThicken " + file.getName());
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   public void removeMesh () {
      if (myMesh != null) {
         viewer.removeRenderable (myMesh);
         viewer.repaint();
         myMesh = null;
      }
   }

   public void setMesh (MeshBase mesh) {
      if (myMesh != null) {
         viewer.removeRenderable (myMesh);
         if (mesh != null) {
            mesh.setRenderProps (myMesh.getRenderProps());
         }
      }
      if (mesh != null) {
         viewer.addRenderable (mesh);
      }
      myMesh = mesh;
   }

   public void addRegion (Region region) {
      myRegions.add (region);
      viewer.addRenderable (region);
      System.out.println ("added curve " + region.myCurve.numControlPoints());
      viewer.repaint();
   }

   public void removeRegion (Region region) {
      myRegions.remove (region);
      viewer.removeRenderable (region);
      viewer.repaint();
   }

   public void clearRegions() {
      for (Region region : myRegions) {
         viewer.removeRenderable (region);
      }
      viewer.repaint();
      myRegions.clear();
   }

   private void saveMesh (File file) {
      if (myMesh != null) {
         try {
            GenericMeshWriter.writeMesh (file, myMesh);
            myMeshFile = file;
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
      }
   }

   protected double computeDeltaZ (
      double d, double margin, double thickening) {

      double dz;
      if (d >= margin) {
         dz = thickening;
      }
      else {
         double l = d/margin;
         dz = thickening*(-2*l+3)*l*l;
      }
      return dz;
   }

   private boolean adjacentFacesCrossNormal (Vertex3d vtx, Vector3d nrm) {
      double lastdot = 1;
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         double dot = he.getFace().getNormal().dot(nrm);
         if (dot*lastdot <= 0) {
            return true;
         }
         lastdot = dot;
      }
      return false;
   }

   public void applyGrowth (PolygonalMesh mesh, double dn) {
      mesh.autoGenerateNormals();
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d v = mesh.getVertex(i);
         Vector3d n = mesh.getNormal(i);
         v.pnt.scaledAdd (dn, n);
      }
      myMesh.notifyVertexPositionsModified();
      viewer.rerender();
   }

   public void applyRemesh (PolygonalMesh mesh, Vector3i res) {
      DistanceGrid grid = new DistanceGrid (res);
      grid.computeFromFeatures (
         mesh.getFaces(), 0.1, null, 0, /*signed=*/true);
      grid.smooth (0.33, -0.2, 30);
      mesh = grid.createQuadDistanceSurface(0, 5);
      setMesh (mesh);
   }

   public void applyThickening (Region region, MeshBase mesh, double thickening) {
      
      double margin = region.myMargin;

      Point3d pnt = new Point3d();
      Vector3d nrm = new Vector3d();
      Vector2d p2d = new Vector2d();
      ArrayList<Vertex3d> verts = myMesh.getVertices();
      ArrayList<Vector3d> nrmls = mesh.getNormals();

      if (nrmls == null) {
         System.out.println ("Mesh does not have normals; thickening ignored");
      }
      int cnt = 0;
      Vector3d regionNrm = new Vector3d();
      // region normal in mesh coordinates
      region.myFrame.R.getColumn (2, regionNrm);
      for (int i=0; i<verts.size(); i++) {
         Vertex3d v = verts.get(i);
         pnt.inverseTransform (region.myFrame, v.pnt);
         nrm.inverseTransform (region.myFrame, nrmls.get(i));

         if (pnt.z <= region.myHeight && pnt.z >= -region.myBackHeight) {
            //if (Math.abs(pnt.z) <= region.myHeight) {
            p2d.set (pnt.x, pnt.y);
            double d = region.myDist.computeInteriorDistance (/*near=*/null, p2d);
            if (d <= 0) {
               double dz = computeDeltaZ (-d, margin, thickening);
               if (region.myUseNormalZScalingP) {
                  if (adjacentFacesCrossNormal (v, regionNrm)) {
                     dz = 0;
                  }
                  else {
                     dz *= nrm.z;
                  }
               }
               else {
                  dz = (nrm.z >= 0 ? dz : -dz);
               }
               if (nrm.z >= 0) {
                  pnt.z += dz;
               }
               else {
                  if (region.getThickenBackSide()) {
                     pnt.z += dz;
                  }
               }
               v.pnt.transform (region.myFrame, pnt);
               cnt++;
            }
         }
      }
      System.out.println ("count=" + cnt);
      myMesh.notifyVertexPositionsModified();
      viewer.rerender();
   }
   
   protected boolean confirmOverwrite (File file) {
      int result = JOptionPane.showConfirmDialog (
         this, "File " + file.getName() + " exists, overwrite?",
         "Existing file", JOptionPane.YES_NO_OPTION);
      return (result == JOptionPane.YES_OPTION);
   }

   protected void createPopupMenu (JPopupMenu popup) {
      addMenuItem (popup, "Edit MeshThicken properties ...");
      if (numSelectedRegions() > 0) {
         addMenuItem (popup, "Edit region properties ...");
         addMenuItem (popup, "Hide regions");
      }
      if (myMesh != null) {
         addMenuItem (popup, "Edit mesh render properties ...");
      }
      super.createPopupMenu (popup);
   }

   protected void setupRegionEditing (Region region) {
   }

   public void actionPerformed (ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals ("Load mesh ...")) {
         if (myMeshFile != null) {
            myMeshChooser.setSelectedFile(myMeshFile);
         }
         int retVal = myMeshChooser.showOpenDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = myMeshChooser.getSelectedFile();
            try {
               loadMesh (file, /*vertexSkip=*/1);
            }
            catch (Exception e) {
               GuiUtils.showError (this, "Can't load mesh", e);
            }
         }
      }
      else if (cmd.equals ("Save mesh")) {
         saveMesh (myMeshFile);
      } 
      else if (cmd.equals ("Save mesh as ...")) {
         if (myMeshFile != null) {
            myMeshChooser.setSelectedFile(myMeshFile);
         }
         int retVal = myMeshChooser.showSaveDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = myMeshChooser.getSelectedFile();
            if (!file.exists() || confirmOverwrite (file)) {
               try {
                  saveMesh (file);
               }
               catch (Exception e) {
                  GuiUtils.showError (this, "Can't save mesh", e);
               }
            }
         }
      }
      else if (cmd.equals ("Clear regions")) {
         clearRegions();
      }
      else if (cmd.equals ("Load regions ...")) {
         if (myRegionFile != null) {
            myRegionChooser.setSelectedFile(myRegionFile);
         }
         int retVal = myRegionChooser.showOpenDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = myRegionChooser.getSelectedFile();
            try {
               loadRegions (file);
            }
            catch (Exception e) {
               GuiUtils.showError (this, "Can't load regions", e);
            }
         }
      }
      else if (cmd.equals ("Save regions")) {
         saveRegions (myRegionFile);
      } 
      else if (cmd.equals ("Save regions as ...")) {
         if (myRegionFile != null) {
            myRegionChooser.setSelectedFile(myRegionFile);
         }
         int retVal = myRegionChooser.showSaveDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = myRegionChooser.getSelectedFile();
            if (!file.exists() || confirmOverwrite (file)) {
               try {
                  saveRegions (file);
               }
               catch (Exception e) {
                  GuiUtils.showError (this, "Can't save regions", e);
               }
            }
         }
      }
      else if (cmd.equals ("Grow")) {
         PolygonalMesh pmesh = (PolygonalMesh)myMesh;
         applyGrowth (pmesh, myGrowLength);
      }
      else if (cmd.equals ("Remesh")) {
         PolygonalMesh pmesh = (PolygonalMesh)myMesh;
         applyRemesh (pmesh, myRemeshRes);
      }
      else if (cmd.equals ("Thicken")) {
         for (Region region : myRegions) {
            applyThickening (region, myMesh, region.getThickening());
         }
      }
      else if (cmd.equals ("Unthicken")) {
         for (Region region : myRegions) {
            applyThickening (region, myMesh, -region.getUnthickening());
         }
      }      
      else if (cmd.equals ("Reverse thicken")) {
         for (Region region : myRegions) {
            applyThickening (region, myMesh, -region.getThickening());
         }
      }
      else if (cmd.equals ("Reverse unthicken")) {
         for (Region region : myRegions) {
            applyThickening (region, myMesh, region.getUnthickening());
         }
      }
      else if (cmd.equals ("Smooth")) {
         PolygonalMesh pmesh = (PolygonalMesh)myMesh;
         LaplacianSmoother.smooth (
            pmesh, mySmoothingCount, mySmoothingLambda, mySmoothingMu);
         pmesh.notifyVertexPositionsModified();         
         viewer.rerender();
      }
      else if (cmd.equals ("Hide all regions")) {
         for (Region region : myRegions) {
            region.setVisible (false);
         }
         viewer.rerender();
      }
      else if (cmd.equals ("Hide regions")) {
         for (Region region : getSelectedRegions()) {
            region.setVisible (false);
         }
         viewer.rerender();
      }
      else if (cmd.equals ("Show all regions")) {
         for (Region region : myRegions) {
            region.setVisible (true);
         }
         viewer.rerender();
      }
      else if (cmd.equals ("Edit MeshThicken properties ...")) {
         ArrayList<HasProperties> list = new ArrayList<HasProperties>();
         list.add (this);
         PropertyDialog dialog =
            PropertyDialog.createDialog (
               "MeshThicken properties", list,
               "OK Cancel", viewer.getCanvas().getComponent(), myRerenderListener);
         if (dialog != null) {
            dialog.setVisible (true);
         }
      }
      else if (cmd.equals ("Edit region properties ...")) {
         PropertyDialog dialog =
            PropertyDialog.createDialog (
               "Region properties", getSelectedRegions(),
               "OK Cancel", viewer.getCanvas().getComponent(), myRerenderListener);
         if (dialog != null) {
            dialog.setVisible (true);
         }
      }
      else if (cmd.equals ("Edit mesh render properties ...")) {
         PropertyDialog dialog =
            new PropertyDialog (
               "Edit render props",
               new RenderPropsPanel (
                  PropertyUtils.createProperties (
                     myMesh.getRenderProps())),
               "Done");
         dialog.locateRight (this);
         dialog.addGlobalValueChangeListener (myRerenderListener);
         dialog.setVisible (true);
      }
      else if (cmd.equals ("Quit")) {
         System.exit (0);
      }
      else {
         super.actionPerformed (evt);
      }
   }

   protected int numSelectedRegions() {
      int num = 0;
      for (Region region : myRegions) {
         if (region.isSelected()) {
            num++;
         }
      }
      return num;
   }

   protected LinkedList<Region> getSelectedRegions() {
      LinkedList<Region> selectedRegions = new LinkedList<Region>();
      for (Region region : myRegions) {
         if (region.isSelected()) {
            selectedRegions.add (region);
         }
      }
      return selectedRegions;
   }

   protected void deselectRegions() {
      for (Region region : myRegions) {
         region.setSelected (false);
      }
   }

   public void itemsSelected (ViewerSelectionEvent e) {
      deselectRegions();
      List<LinkedList<?>> itemPaths = e.getSelectedObjects();
      for (List<?> path : itemPaths) {
         Object obj = path.get (0);
         if (obj instanceof Region) {
            ((Region)obj).setSelected (true);
         }
      }
   }

   protected static StringHolder meshFile = new StringHolder();
   protected static StringHolder regionFile = new StringHolder();
   static IntHolder skipCount = new IntHolder (1);

   public static void main (String[] args) {

      ArgParser parser = new ArgParser (
         "java maspack.apps.MeshThicken [options] ...");
      //parser.addOption ("-file %s #mesh file names", meshFileList);
      parser.addOption ("-mesh %s #mesh file", meshFile);
      parser.addOption ("-regions %s #region file", regionFile);
      parser.addOption (
         "-skip %d #for .xyzb point meshes, use every n-th point", skipCount);

      parser.matchAllArgs (args);
      // int idx = 0;
      // while (idx < args.length) {
      //    try {
      //       idx = parser.matchArg (args, idx);
      //    }
      //    catch (Exception e) {
      //       // malformed or erroneous argument
      //       parser.printErrorAndExit (e.getMessage());
      //    }
      // }

      MeshThicken thickener =
         new MeshThicken(meshFile.value, regionFile.value);

      //tester.addTestCurves();
      //tester.addBox();

      thickener.getViewer().autoFit();
      thickener.getViewer().setBackgroundColor (new Color (0f, 0f, 0.2f));
      thickener.setVisible(true);
   }   

}
