/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import maspack.geometry.Face;
import maspack.geometry.LaplacianSmoother;
import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.GenericMeshReader;
import maspack.geometry.io.GenericMeshWriter;
import maspack.geometry.io.MeshReader;
import maspack.geometry.io.WavefrontReader;
import maspack.geometry.io.XyzbReader;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.render.RenderListener;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.RendererEvent;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GLSupport.GLVersionInfo;
import maspack.render.GL.GLViewer.GLVersion;
import maspack.render.GL.GLViewerFrame;
import maspack.widgets.GridDisplay;
import maspack.widgets.GuiUtils;
import maspack.widgets.MenuAdapter;
import maspack.widgets.PropertyDialog;
import maspack.widgets.RenderPropsPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.ViewerKeyListener;
import maspack.widgets.ViewerPopupManager;
import maspack.widgets.ViewerToolBar;

public class MeshViewer extends GLViewerFrame
   implements ActionListener, HasProperties, RenderListener {

   private static final long serialVersionUID = 1L;
   ArrayList<MeshBase> myMeshes = new ArrayList<MeshBase> (0);
   ArrayList<String> myMeshQueue = null;
   int myMeshCursor = 0;

   JLabel myMeshLabel;
   String myLastMeshName;
   double myPointNormalLen = 0;

   private static double DEFAULT_VERTEX_MERGE_DIST = 0;
   private static double DEFAULT_SMOOTHING_LAMBDA = 0.7;
   private static double DEFAULT_SMOOTHING_MU = 0;
   private static int DEFAULT_SMOOTHING_COUNT = 3;

   double myVertexMergeDist = DEFAULT_VERTEX_MERGE_DIST;
   double mySmoothingLambda = DEFAULT_SMOOTHING_LAMBDA;
   double mySmoothingMu = DEFAULT_SMOOTHING_MU;
   int mySmoothingCount = DEFAULT_SMOOTHING_COUNT;

   PropertyDialog myPropertyDialog;
   ViewerPopupManager myPopupManager; // manages viewer property pop-ups
   GridDisplay myGridDisplay;  // display grid resolution when grid turned on
   int myGridDisplayIndex;     // location of grid display in tooldbar
   JPanel myToolBar;           // tool bar across the top of the frame
   protected JColorChooser myColorChooser = new JColorChooser();

   public static PropertyList myProps =
      new PropertyList (MeshViewer.class);

   static Color myDefaultBackground = new Color (0f, 0f, 0.2f);
   Color myBackgroundColor = myDefaultBackground;

   static {
      myProps.add ("pointNormalLen", "length of point cloud normals", 0);
      myProps.add ("backgroundColor", "viewer background color",
         myDefaultBackground);
      myProps.add (
         "vertexMergeDist", "distance for vertex merging",
         DEFAULT_VERTEX_MERGE_DIST);
      myProps.add (
         "smoothingLambda", "lambda for two-stage Laplacian smoothing",
         DEFAULT_SMOOTHING_LAMBDA);
      myProps.add (
         "smoothingMu", "mu for two-stage Laplacian smoothing",
         DEFAULT_SMOOTHING_MU);
      myProps.add (
         "smoothingCount", "count for two-stage Laplacian smoothing",
         DEFAULT_SMOOTHING_COUNT);
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

   public double getPointNormalLen() {
      return myPointNormalLen;
   }

   public void setPointNormalLen (double len) {
      if (myPointNormalLen != len) {
         myPointNormalLen = len;
         for (MeshBase m : myMeshes) {
            if (m instanceof PointMesh) {
               ((PointMesh)m).setNormalRenderLen (len);
            }
         }
      }
   }

   public Color getBackgroundColor () {
      return myBackgroundColor;
   }

   public void setBackgroundColor (Color color) {
      myBackgroundColor = color;
      viewer.setBackgroundColor (color);
   }

   public double getVertexMergeDist () {
      return myVertexMergeDist;
   }

   public void setVertexMergeDist (double dist) {
      myVertexMergeDist = dist;
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

   RenderProps myRenderProps;

   JFileChooser myMeshChooser = new JFileChooser();

   private static class MeshInfo {
      String myName;
      MeshBase myMesh;

      MeshInfo (String name, MeshBase mesh) {
         myName = name;
         myMesh = mesh;
      }
   }

   static ArrayList<MeshInfo> createInfoList (ArrayList<? extends MeshBase> list) {
      ArrayList<MeshInfo> infoList = new ArrayList<MeshInfo>();
      for (MeshBase mesh : list) {
         infoList.add (new MeshInfo (" ", mesh));
      }
      return infoList;
   }

   private void exit(int retval) {
      remove(viewer.getCanvas().getComponent());
      System.exit (retval);
   }
   
   private class KeyHandler extends KeyAdapter {
      public void keyTyped (KeyEvent e) {
         switch (e.getKeyChar()) {
            case 'q': {
               exit(0);
               break;
            }
            case 's': {
               smoothMeshes();
               break;
            }
            case 'c': {
               cleanMeshes();
               break;
            }
            case ' ': {
               if (myMeshQueue != null) {
                  if (myMeshCursor == myMeshQueue.size()-1) {
                     loadMeshAt (0);
                  }
                  else {
                     loadMeshAt (myMeshCursor+1);
                  }
               }
               break;
            }
            case '\u0008': {
               if (myMeshQueue != null) {
                  if (myMeshCursor == 0) {
                     loadMeshAt (myMeshQueue.size()-1);
                  }
                  else {
                     loadMeshAt (myMeshCursor-1);
                  }
               }
               break;
            }
         }
      }
   }

   private class ObjFileFilter extends FileFilter {

      public String getDescription () {
         return "Object files (*.obj)";
      }

      public boolean accept(File file) {
         if (file.isDirectory()) {
            return true;
         } else {
            String path = file.getAbsolutePath().toLowerCase();
            return path.endsWith(".obj");
         }
      }
   }

   class MeshSelector extends JFrame {

      private JList meshList;
      private boolean listSelectionMasked = false;

      public MeshSelector() {
         initUI();
      }

      public void setNames() {
         String[] names = new String[myMeshQueue.size()];
         int i = 0;
         for (String fileName : myMeshQueue) {
            names[i++] = new File(fileName).getName();
         }
         meshList.setListData (names);
      }

      private void initUI () {
         JPanel panel = new JPanel();
         panel.setLayout(new BorderLayout());
         panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


         meshList = new JList ();
         setNames();
         meshList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
               if (!e.getValueIsAdjusting()) {
                  int idx = meshList.getSelectedIndex();
                  if (!listSelectionMasked) {
                     loadMeshAt (idx);
                  }
               }
            }
         });

         JScrollPane pane = new JScrollPane();
         pane.getViewport().add(meshList);
         pane.setPreferredSize(new Dimension(640, 200));
         panel.add(pane);

         add (panel);
         pack();
         //setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      }

      public void setSelected (int idx) {
         listSelectionMasked = true;
         meshList.setSelectedIndex (idx);
         listSelectionMasked = false;
         meshList.ensureIndexIsVisible (idx);
      }
   }

   private MeshSelector myMeshSelector = null;

   private void createMeshSelector () {

      myMeshSelector = new MeshSelector ();
   }

   private RenderProps createRenderProps (MeshBase mesh) {
      RenderProps props = mesh.createRenderProps();

      props.setShading (smooth.value ? Shading.SMOOTH : Shading.FLAT);
      if (noDrawFaces.value) {
         props.setFaceStyle (FaceStyle.NONE);
      }
      else if (oneSided.value) {
         props.setFaceStyle (FaceStyle.FRONT);
      }
      else {
         props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      }
      props.setDrawEdges (drawEdges.value);
      if (edgeColor[0] != -1) {
         props.setLineColor (new Color (
            edgeColor[0], edgeColor[1], edgeColor[2]));
      }

      Color gold = new Color (0.93f, 0.8f, 0.063f);
      Color gray = new Color (0.5f, 0.5f, 0.5f);

      props.setFaceColor (gray);
      props.setBackColor (gray);
      return props;
   }

   private void addMenuItem (JMenu menu, String name) {
      JMenuItem item = new JMenuItem (name);
      item.addActionListener(this);
      item.setActionCommand(name);
      menu.add (item);
   }

   private void createFileMenu (JMenu menu) {
      addMenuItem (menu, "Load mesh ...");
      if (myMeshes.size() > 0) {
         addMenuItem (menu, "Save mesh ...");
      }
      if (myMeshQueue != null && myMeshQueue.size() > 0) {
         if (myMeshSelector == null || !myMeshSelector.isVisible()) {
            addMenuItem (menu, "Show mesh selector");
         }
         else {
            addMenuItem (menu, "Hide mesh selector");
         }
      }
      addMenuItem (menu, "Quit");
   }

   private void createViewMenu (JMenu menu) {
      addMenuItem (menu, "Background color");
      if (viewer.isOrthogonal()) {
         addMenuItem(menu, "Perspective view");
      }
      else {
         addMenuItem(menu, "Orthographic view");
      }
      if (isPropertyDialogVisible()) {
         addMenuItem(menu, "Hide property dialog");
      }
      else {
         addMenuItem(menu, "Show property dialog ...");
      }
   }

   private void createMenuBar () {
      JMenuBar menuBar = new JMenuBar();
      setJMenuBar (menuBar);

      JMenu menu = new JMenu ("File");
      menu.addMenuListener(new MenuAdapter() {
         public void menuSelected(MenuEvent m_evt) {
            createFileMenu((JMenu)m_evt.getSource());
         }
      });      

      menuBar.add (menu);      

      menu = new JMenu ("View");
      menu.addMenuListener(new MenuAdapter() {
         public void menuSelected(MenuEvent m_evt) {
            createViewMenu((JMenu)m_evt.getSource());
         }
      });      

      menuBar.add (menu);      
   }

   private void createToolBars() {
      myToolBar = new JPanel();
      myToolBar.setLayout(new BoxLayout(myToolBar, BoxLayout.LINE_AXIS));
      myToolBar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      myMeshLabel = new JLabel(" ");
      myToolBar.add (myMeshLabel);

      // add glue to separate label from grid display
      myToolBar.add(Box.createHorizontalGlue());
      myGridDisplayIndex = myToolBar.getComponentCount();
      // add a place-holder component for the grid display
      myToolBar.add(GridDisplay.createPlaceHolder());

      getContentPane().add(myToolBar, BorderLayout.NORTH);
      JPanel leftPanel = new JPanel(new BorderLayout());
      ViewerToolBar viewerToolBar = 
         new ViewerToolBar (viewer, /*addGridPanel=*/false);
      viewerToolBar.setOrientation(JToolBar.VERTICAL);
      leftPanel.add (viewerToolBar, BorderLayout.SOUTH);
      getContentPane().add(leftPanel, BorderLayout.WEST);
   }

   private void setLabelText (String name, MeshBase mesh) {

      System.out.println ("numv=" + mesh.numVertices());
      String text = name == null ? " " : name;
      text += "   numv=" + mesh.numVertices();
      if (mesh instanceof PolygonalMesh) {
         PolygonalMesh pmesh = (PolygonalMesh)mesh;
         text += " manifold=" + pmesh.isManifold();
         text += " closed=" + pmesh.isClosed();
      }
      myMeshLabel.setText (text);
   }

   private void addMesh (String name, MeshBase mesh) {
      myMeshes.add (mesh);
      if (mesh instanceof PointMesh &&
          ((PointMesh)mesh).getNormals() == null) {
         RenderProps.setShading (mesh, Shading.NONE);
      }
      viewer.addRenderable (mesh);     
      myLastMeshName = name;
      setLabelText (name, mesh);
   }

   public void setMeshQueue (ArrayList<String> queue) {
      myMeshQueue = queue;
      myMeshCursor = 0;
      if (myMeshQueue != null) {
         loadMeshAt (myMeshCursor);
      }
   }

   public MeshViewer (ArrayList<? extends MeshBase> meshList, int w, int h) {
      this ("MeshViewer", createInfoList(meshList), w, h, GLVersion.GL3);
   }

   public MeshViewer (
      String title, ArrayList<MeshInfo> infoList, int w, int h, 
      GLVersion glVersion) {

      super (title, w, h, glVersion);

      myMeshChooser.setFileFilter (new ObjFileFilter());
      myMeshChooser.setCurrentDirectory (new File("."));

      createMenuBar();
      createToolBars();

      for (MeshInfo info : infoList) {
         MeshBase mesh = info.myMesh;
         if (mesh instanceof PolygonalMesh) {
            ((PolygonalMesh)mesh).triangulate();
         }
         if (myRenderProps == null) {
            myRenderProps = createRenderProps (mesh);
         }
         if (mesh.getRenderProps() == null) {
            mesh.setRenderProps (myRenderProps);
         }
         else {
            myRenderProps.set (mesh.getRenderProps());
         }
         addMesh (info.myName, mesh);
      }
      if (myRenderProps == null) {
         myRenderProps = RenderProps.createMeshProps(null);
      }

      viewer.autoFitPerspective ();
      if (backgroundColor[0] != -1) {
         viewer.setBackgroundColor (
            backgroundColor[0], backgroundColor[1], backgroundColor[2]);
      }
      else {
         viewer.setBackgroundColor (myDefaultBackground);
      }
      if (drawAxes.value) {
         if (axisLength.value > 0) {
            viewer.setAxisLength (axisLength.value);
         }
         else {
            viewer.setAxisLength (GLViewer.AUTO_FIT);
         }
      }

      viewer.addMouseInputListener (new MouseInputAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
               displayPopup (e);
            }
         }
      });

      viewer.addKeyListener (new KeyHandler());
      viewer.addKeyListener (new ViewerKeyListener(viewer));
      viewer.addRenderListener (this);

      myPopupManager = new ViewerPopupManager (viewer);

      pack();

   }

   private void checkFaces (PolygonalMesh mesh) {

      for (Face face : mesh.getFaces()) {
         Vector3d nrm = face.getNormal();
         if (nrm.containsNaN()) {
            System.out.println ("face "+face.getIndex()+" badly formed");
            for (int i=0; i<3; i++) {
               Vertex3d v = face.getVertex(i);
               System.out.println (" " + v + " " + v.pnt + " " +
                  v.numIncidentHalfEdges());
            }
         }
      }
   }       

   private void smoothMeshes() {
      for (MeshBase m : myMeshes) {
         if (m instanceof PolygonalMesh) {
            PolygonalMesh pmesh = (PolygonalMesh)m;
            LaplacianSmoother.smooth (
               pmesh, mySmoothingCount, mySmoothingLambda, mySmoothingMu);
            pmesh.notifyVertexPositionsModified();
         }
      }
      viewer.rerender();
   }

   private void cleanMeshes() {
      System.out.println ("Cleaning ...");
      for (MeshBase m : myMeshes) {
         if (m instanceof PolygonalMesh) {
            boolean modified = false;
            PolygonalMesh pmesh = (PolygonalMesh)m;
            int nv = pmesh.mergeCloseVertices (myVertexMergeDist);
            if (nv > 0) {
               System.out.println ("removed "+nv+" vertices");
               modified = true;
            }
            int nf = pmesh.removeDisconnectedFaces();
            if (nf > 0) {
               System.out.println ("removed "+nf+" disconnected faces");
               modified = true;
            }
            if (modified) {
               pmesh.notifyVertexPositionsModified();
               if (m == myMeshes.get(myMeshes.size()-1)) {
                  // if last mesh in list, need to update label text as well
                  setLabelText (myLastMeshName, m);
               }
            }
         }
      }
      System.out.println ("done");
      viewer.rerender();
   }

   private void loadMeshAt (int cursor) {
      if (myMeshQueue != null && cursor >= 0 && cursor < myMeshQueue.size()) {
         String fileName = myMeshQueue.get(cursor);
         if (!loadMesh (new File(fileName))) {
            // remove from the queue
            myMeshQueue.remove (cursor);
            if (myMeshCursor > cursor) {
               myMeshCursor--;
            }
            if (myMeshSelector != null) {
               myMeshSelector.setNames();
               myMeshSelector.setSelected (myMeshCursor);
            }
         }
         else {
            myMeshCursor = cursor;
            if (myMeshSelector != null) {
               myMeshSelector.setSelected (myMeshCursor);
            }
         }
      }
   }


   private boolean loadMesh (File file) {
      MeshBase mesh = null;
      try {
         mesh = GenericMeshReader.readMesh (file);
         // if (file.getName().endsWith (".xyzb")) {
         //    XyzbReader reader = new XyzbReader (file);
         //    reader.setSkip (skipCount.value);
         //    mesh = reader.readMesh();
         //    ((PointMesh)mesh).setNormalRenderLen (myPointNormalLen);
         // }
         // else if (file.getName().endsWith (".obj")) {
         //    mesh = new PolygonalMesh (file);
         // }
         // else if (file.getName().endsWith (".ply")) {
         //    PlyReader reader = new PlyReader (file);
         //    mesh = reader.readMesh();
         // }
         // else {
         //    throw new IOException ("Unrecognized file type");
         // }
      }
      catch (Exception ex) {
         ex.printStackTrace(); 
         GuiUtils.showError (this, "Can't open or read file: " + ex);
         mesh = null;
      }         
      if (mesh != null) {
         viewer.clearRenderables();
         myMeshes.clear();
         addMesh (file.getName(), mesh);
         viewer.autoFitPerspective ();
         viewer.rerender();
      }
      return mesh != null;
   }

   private boolean saveMesh (File file) {
      MeshBase mesh = myMeshes.get(myMeshes.size()-1);
      try {
         GenericMeshWriter.writeMesh (file, mesh);
         // if (file.getName().endsWith (".xyzb")) {
         //    XyzbWriter writer = new XyzbWriter (file);
         //    writer.writeMesh ((PointMesh)mesh);
         // }
         // else if (file.getName().endsWith (".obj")) {
         //    PrintWriter pw = new PrintWriter (
         //       new BufferedWriter (new FileWriter (file)));
         //    mesh.write (pw, "%.8g");
         //    pw.close();
         // }
         // else if (file.getName().endsWith (".ply")) {
         // }
         // else {
         //    throw new IOException ("Unrecognized file type");
         // }
         // return true;
      }
      catch (Exception ex) {
         ex.printStackTrace(); 
         GuiUtils.showError (this, "Can't write file: " + ex);
         mesh = null;
      }         
      return false;
   }

   private void locateRight (Window win, Component ref) {
      Window refWin;
      Point compLoc;
      if (ref instanceof Window) {
         refWin = (Window)ref;
         compLoc = new Point();
      }
      else {
         refWin = SwingUtilities.windowForComponent (ref);
         compLoc = SwingUtilities.convertPoint (ref, 0, 0, refWin);
      }
      if (refWin == null) {
         return;
      }
      Point refLoc = refWin.getLocation();
      Dimension refSize = refWin.getSize();

      Point newLoc = new Point (refLoc.x + refSize.width, refLoc.y + compLoc.y);
      win.setLocation (newLoc);
   }

   private boolean isPropertyDialogVisible () {
      return myPropertyDialog != null && myPropertyDialog.isVisible();
   }

   private void setPropertyDialogVisible (boolean visible) {
      if (isPropertyDialogVisible () != visible) {
         if (visible) {
            if (myPropertyDialog == null) {
               myPropertyDialog =
                  new PropertyDialog (
                     "Edit properties", this, "Done");
            }
            myPropertyDialog.locateRight (this);
            myPropertyDialog.setVisible (true);
         }
         else {
            myPropertyDialog.setVisible (false);
         }
      }
   }

   public void setBackgroundColor() {

      myColorChooser.setColor(viewer.getBackgroundColor());

      ActionListener setBColor = new ActionListener() {   
         @Override
         public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("OK")) {
               viewer.setBackgroundColor(myColorChooser.getColor());
            } else if (cmd.equals("Cancel")) {
               // do nothing
            }
         }
      };
      JDialog dialog =
      JColorChooser.createDialog(
         this, "color chooser", /* modal= */true, myColorChooser,
         setBColor, setBColor);
      GuiUtils.locateRight(dialog, this);
      dialog.setVisible(true);
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Edit render props")) {
         PropertyDialog dialog =
            new PropertyDialog ("Edit render props", new RenderPropsPanel (
               PropertyUtils.createProperties (myRenderProps)), "OK Cancel");
         dialog.locateRight (this);
         dialog.addGlobalValueChangeListener (new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               for (MeshBase mesh : myMeshes) {
                  mesh.setRenderProps (myRenderProps);
               }
               viewer.rerender();
            }
         });
         myPopupManager.registerDialog (dialog);
         dialog.setVisible (true);
      } else if (cmd.equals("Edit viewer props")) {
         PropertyDialog dialog =
            myPopupManager.createPropertyDialog ("OK Cancel");
         dialog.setVisible(true);
      } else if (cmd.equals ("Quit")) {
         exit(0);
      }
      else if (cmd.equals ("Load mesh ...")) {
         int retVal = myMeshChooser.showOpenDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = myMeshChooser.getSelectedFile();
            loadMesh (file);
         }
      }
      else if (cmd.equals ("Save mesh ...")) {
         int retVal = myMeshChooser.showOpenDialog(this);
         if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = myMeshChooser.getSelectedFile();
            saveMesh (file);
         }
      }
      else if (cmd.equals ("Show mesh selector")) {
         if (myMeshSelector == null) {
            createMeshSelector ();
            myMeshSelector.setSelected (myMeshCursor);
         }
         myMeshSelector.setLocationRelativeTo(this);
         myMeshSelector.setVisible (true);
      }
      else if (cmd.equals ("Hide mesh selector")) {
         myMeshSelector.setVisible (false);
      }
      else if (cmd.equals ("Show property dialog ...")) {
         setPropertyDialogVisible (true);
      }
      else if (cmd.equals ("Hide property dialog")) {
         setPropertyDialogVisible (false);
      }
      else if (cmd.equals("Background color")) {
         setBackgroundColor();
      }
      else if (cmd.equals("Perspective view")) {
         viewer.setOrthographicView(false);
      }
      else if (cmd.equals("Orthographic view")) {
         viewer.setOrthographicView(true);
      }
   }

   private void displayPopup (MouseEvent evt) {
      JPopupMenu popup = new JPopupMenu();
      JMenuItem item = new JMenuItem ("Edit render props");
      item.addActionListener (this);
      item.setActionCommand ("Edit render props");
      popup.add (item);

      item = new JMenuItem("Edit viewer props");
      item.addActionListener(this);
      item.setActionCommand("Edit viewer props");
      popup.add(item);

      item = new JMenuItem("Sort faces");
      item.addActionListener(this);
      item.setActionCommand("Sort faces");
      popup.add(item);

      popup.setLightWeightPopupEnabled (false);
      popup.show (evt.getComponent(), evt.getX(), evt.getY());
   }

   static BooleanHolder drawAxes = new BooleanHolder (false);
   static BooleanHolder printBounds = new BooleanHolder (false);
   static DoubleHolder axisLength = new DoubleHolder (-1);
   static BooleanHolder drawEdges = new BooleanHolder (false);
   static BooleanHolder noDrawFaces = new BooleanHolder (false);
   static float[] edgeColor = new float[] { -1, -1, -1 };
   static float[] backgroundColor = new float[] { -1, -1, -1 };
   static float[] rotation = new float[] { 1, 0, 0, 0 };
   static BooleanHolder smooth = new BooleanHolder (false);
   static BooleanHolder oneSided = new BooleanHolder (false);
   static BooleanHolder zeroIndexed = new BooleanHolder (false);
   static StringHolder className =
      new StringHolder ("maspack.geometry.PolygonalMesh");
   static BooleanHolder queueMeshes = new BooleanHolder (false);
   static BooleanHolder pointMesh = new BooleanHolder (false);
   static IntHolder skipCount = new IntHolder (1);
   protected static IntHolder glVersion = new IntHolder (3);

   private static void doPrintBounds (MeshBase mesh) {
      double inf = Double.POSITIVE_INFINITY;
      Point3d max = new Point3d (-inf, -inf, -inf);
      Point3d min = new Point3d ( inf,  inf,  inf);
      Point3d tmp = new Point3d();
      mesh.updateBounds (min, max);
      System.out.println ("Bounds:");
      System.out.println ("min    = " + min);
      System.out.println ("max    = " + max);
      tmp.sub (max, min);
      System.out.println ("widths = " + tmp);
      tmp.add (max, min);
      tmp.scale (0.5);
      System.out.println ("center = " + tmp);
   }

   public static void loadMeshes (
      ArrayList<MeshInfo> infoList, String fileName, AffineTransform3dBase X) {
      try {
         File meshFile = new File (fileName);

         if (!meshFile.exists()) {
            System.out.println (
               "Error: mesh file " + meshFile.getName() + " not found");
            return;
         }

         if (meshFile.getName().endsWith (".obj") && !pointMesh.value) {

            Class meshClass = Class.forName (className.value);
            Class meshBaseClass = new PolygonalMesh().getClass();

            if (meshClass == null) {
               System.out.println ("can't find class " + className.value);
               return;
            }
            if (!meshBaseClass.isAssignableFrom (meshClass)) {
               System.out.println (
                  className.value+" is not an instance of "+
                  meshBaseClass.getName());
               return;
            }

            WavefrontReader reader = new WavefrontReader (meshFile);
            reader.parse ();
            reader.close ();
            
            String[] names = reader.getPolyhedralGroupNames();
            for (int i=0; i<names.length; i++) {

               if (meshClass == PolygonalMesh.class) {
                  PolygonalMesh mesh = (PolygonalMesh)meshClass.newInstance();
                  reader.setMesh (mesh, names[i]);
                  if (printBounds.value) {
                     doPrintBounds (mesh);
                  }

                  if (mesh.numVertices() > 0) {
                     System.out.print (
                        "mesh "+names[i]+": "+mesh.numVertices()+" vertices, "+
                           mesh.numFaces()+" faces, ");
                     if (mesh.isTriangular()) {
                        System.out.println ("triangular");
                     }
                     else if (mesh.isQuad()) {
                        System.out.println ("quad");
                     }
                     else {
                        System.out.println ("polygonal");
                     }
                     mesh.transform (X);
                     if (names[i] == null || names[i].equals ("default")) {
                        infoList.add (
                           new MeshInfo(meshFile.getName(), mesh));
                     }
                     else {
                        infoList.add (
                           new MeshInfo (
                              meshFile.getName()+"("+names[i]+")", mesh));
                     }
                  }
               }
               else if (meshClass == PolylineMesh.class) {
                  PolylineMesh mesh = (PolylineMesh)meshClass.newInstance();
                  reader.setMesh (mesh, names[i]);
                  mesh.transform (X);
                  if (printBounds.value) {
                     doPrintBounds (mesh);
                  }
                  if (names[i].equals ("default")) {
                     infoList.add (
                        new MeshInfo(meshFile.getName(), mesh));
                  }
                  else {
                     infoList.add (
                        new MeshInfo (meshFile.getName()+"("+names[i]+")", mesh));
                  }

               }
               else if (meshClass == PointMesh.class) {
                  PointMesh mesh = (PointMesh)meshClass.newInstance();
                  reader.setMesh (mesh, names[i]);
                  mesh.transform (X);
                  if (printBounds.value) {
                     doPrintBounds (mesh);
                  }

                  if (names[i].equals ("default")) {
                     infoList.add (
                        new MeshInfo(meshFile.getName(), mesh));
                  }
                  else {
                     infoList.add (
                        new MeshInfo (meshFile.getName()+"("+names[i]+")", mesh));
                  }
               }
            }
         }
         else {
            MeshReader reader;
            if (meshFile.getName().endsWith (".xyzb")) {
               XyzbReader xyzbReader = new XyzbReader (meshFile);
               xyzbReader.setSkip (skipCount.value);
               reader = xyzbReader;
            }
            else {
               reader = new GenericMeshReader (meshFile);
            }
            MeshBase mesh = pointMesh.value ? new PointMesh() : null;
            mesh = reader.readMesh (mesh);
            if (printBounds.value) {
               doPrintBounds (mesh);
            }
            infoList.add (new MeshInfo(meshFile.getName(), mesh));
         }
      }
      catch (Exception e) {
         System.out.println ("Error creating mesh");
         e.printStackTrace();
      }
   }

   public static void main (String[] args) {
       StringHolder fileName = new StringHolder();
      IntHolder width = new IntHolder (640);
      IntHolder height = new IntHolder (480);
       Vector meshFileList = new Vector();
      ArrayList<String> meshQueue = new ArrayList<String>();

      ArgParser parser = new ArgParser (
         "java maspack.geometry.MeshViewer [options] <objFile> ...");
      //parser.addOption ("-file %s #mesh file names", meshFileList);
      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-drawEdges %v #draw mesh edges", drawEdges);
      parser.addOption ("-noDrawFaces %v #do not draw faces", noDrawFaces);
      parser.addOption ("-edgeColor %fX3 #edge color", edgeColor);
      parser.addOption (
         "-backgroundColor %fX3 #background color", backgroundColor);
      parser.addOption ("-rotate %fX4 #rotation (axis-angle)", rotation);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption ("-smooth %v #use smooth shading", smooth);
      parser.addOption ("-oneSided %v #draw only front faces", oneSided);
      parser.addOption ("-class %s #use PolygonalMesh sub class", className);
      parser.addOption (
         "-zeroIndexed %v #zero indexed (for writing)", zeroIndexed);
      parser.addOption ("-queue %v #queue meshes for viewing", queueMeshes);
      parser.addOption ("-printBounds %v #print bounds for meshes", printBounds);
      parser.addOption (
         "-skip %d #for .xyzb point meshes, use every n-th point", skipCount);
      parser.addOption (
         "-pointMesh %v #specifies that mesh must be read as a point mesh",
         pointMesh);
      parser.addOption (
         "-GLVersion %d{2,3} " + "#version of openGL for graphics", glVersion);

      RigidTransform3d X = new RigidTransform3d();
      ArrayList<MeshInfo> infoList = new ArrayList<MeshInfo> (0);

      int idx = 0;
      while (idx < args.length) {
         try {
            idx = parser.matchArg (args, idx);
            if (parser.getUnmatchedArgument() != null) {
            	String meshFileName = parser.getUnmatchedArgument();
                if (queueMeshes.value) {
                   meshQueue.add (meshFileName);
                }
                else {
                   X.R.setAxisAngle (rotation[0], rotation[1], rotation[2],
                                     Math.toRadians(rotation[3]));
                   loadMeshes (infoList, meshFileName, X);
                }
            }
         }
         catch (Exception e) {
            // malformed or erroneous argument
            parser.printErrorAndExit (e.getMessage());
         }
      }

      //       parser.matchAllArgs (args);
      //       for (Iterator it = meshFileList.iterator(); it.hasNext();) {
      //          loadMeshes (meshList, ((StringHolder)it.next()).value, X);
      //       }
      // call this to prevent awful looking fonts:
      System.setProperty("awt.useSystemAAFontSettings","on");

      GLVersion glv = (glVersion.value == 3 ? GLVersion.GL3 : GLVersion.GL2);

      // check if GL3 version is supported
      if (glv == GLVersion.GL3) {
         GLVersionInfo vinfo = GLSupport.getMaxGLVersionSupported();
         if ( (vinfo.getMajorVersion() < glv.getMajorVersion()) ||
            ((vinfo.getMajorVersion() == glv.getMajorVersion()) && 
               (vinfo.getMinorVersion() < glv.getMinorVersion()))) {
            System.err.println("WARNING: " + glVersion.toString() + " is not supported on this system.");
            System.err.println("     Required: OpenGL " + glv.getMajorVersion() + "." + glv.getMinorVersion());
            System.err.println("     Available: OpenGL " + vinfo.getMajorVersion() + "." + vinfo.getMinorVersion());
            glv = GLVersion.GL2;
         }
      }

      final MeshViewer frame =
         new MeshViewer (
            "MeshViewer", infoList, width.value, height.value, glv);
      frame.setMeshQueue (meshQueue);
      frame.setVisible (true);

      // add close
      frame.addWindowListener(new WindowListener() {

         @Override
         public void windowOpened(WindowEvent arg0) {}

         @Override
         public void windowIconified(WindowEvent arg0) {}

         @Override
         public void windowDeiconified(WindowEvent arg0) {}

         @Override
         public void windowDeactivated(WindowEvent arg0) {}

         @Override
         public void windowClosing(WindowEvent arg0) {frame.exit(0);}

         @Override
         public void windowClosed(WindowEvent arg0) {}

         @Override
         public void windowActivated(WindowEvent arg0) {}
      });
   }

   public void updateWidgets() {
      // return if  not visible, since updating widgets while
      // frame is being set visible can cause some problems
      if (!isVisible()) {
         return;
      }
      boolean gridOn = viewer.getGridVisible();
      GLGridPlane grid = viewer.getGrid();
      if ((myGridDisplay != null) != gridOn) {
         if (gridOn) {
            myGridDisplay =
               GridDisplay.createAndAdd (grid, myToolBar, myGridDisplayIndex);
         }
         else {
            GridDisplay.removeAndDispose (
               myGridDisplay, myToolBar, myGridDisplayIndex);
            myGridDisplay = null;
         }
      }
      if (myGridDisplay != null) {
         myGridDisplay.updateWidgets();
      }
   }

   /**
    * interface face method for GLViewerListener.
    */
   public void renderOccurred (RendererEvent e) {
      updateWidgets();
   }

}
