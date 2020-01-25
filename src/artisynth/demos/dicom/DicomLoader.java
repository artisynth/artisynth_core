package artisynth.demos.dicom;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.renderables.DicomPlaneViewer;
import artisynth.core.renderables.DicomViewer;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.image.dicom.DicomImage;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.render.GL.GLViewer.BlendFactor;
import maspack.widgets.BooleanSelector;
import maspack.widgets.FileNameField;
import maspack.widgets.StringField;

public class DicomLoader extends RootModel {
   
   DicomViewer viewer;
   ArrayList<DicomPlaneViewer> viewerPlanes;
   
   FileNameField fnf;
   StringField fnp;
   BooleanSelector fns;
   ControlPanel panel;
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      viewerPlanes = new ArrayList<>();

      createControlPanel();
      // everything else done in attach and loader
   }
   
   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      driver.getViewer().setBackgroundColor(Color.WHITE);
      driver.getViewer().setBlendDestFactor(BlendFactor.GL_ONE_MINUS_SRC_ALPHA);
   }
   
   void createControlPanel() {
      
      panel = new ControlPanel("DICOM controls");
      
      fnf = new FileNameField("File or folder:", 30);
      fnf.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fnf.getFileChooser().setCurrentDirectory(ArtisynthPath.getSrcRelativeFile(this, "data/"));
      fnf.setName("dicom_path");
      panel.addWidget(fnf);
      
      fnp = new StringField("File regex:", 30);
      fnp.setValue(".*");
      panel.addWidget(fnp);
      
      fns = new BooleanSelector("Subdirectories", true);
      panel.addWidget(fns);
      
      // panel for full-width button
      JPanel jpanel = new JPanel();
      jpanel.setLayout(new GridLayout());
      panel.addWidget(jpanel);
      
      JButton button = new JButton("Load");
      button.setActionCommand("load");
      button.addActionListener(this);
      jpanel.add(button);
      
      button = new JButton("Add Plane");
      button.setActionCommand("plane");
      button.addActionListener(this);
      jpanel.add(button);
      
      addControlPanel(panel);
   }
   
   @Override
   public void actionPerformed(ActionEvent event) {
      
      String cmd = event.getActionCommand();
      
      if ("load".equals(cmd)) {
         load();
      } else if ("plane".equals(cmd)) {
         addPlane();
      } else {
         super.actionPerformed(event);
      }
   }   
   
   void load() {
      String folder = fnf.getText();
      String pattern = fnp.getText();
      boolean subdirs = fns.getBooleanValue();
      
      if (viewer != null) {
         removeRenderable(viewer);
      }
      if (viewerPlanes.size() > 0) {
         for (DicomPlaneViewer dpv : viewerPlanes) {
            removeRenderable(dpv);
         }
         viewerPlanes.clear();
      }
      
      File file = new File(folder);
      String name = file.getName();
      
      viewer = new DicomViewer(name, file.getAbsolutePath(), Pattern.compile(pattern), subdirs);
      DicomImage image = viewer.getImage();
      System.out.println(image.toString());
      
      addRenderable(viewer);
      Main.getMain().getViewer().autoFit();
      
   }
   
   void addPlane() {
      if (viewer == null) {
         return;
      }
      
      DicomImage image = viewer.getImage();
      
      Point3d pmin = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      Point3d pmax = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      viewer.updateBounds(pmin, pmax);
      
      // center transform
      RigidTransform3d trans = new RigidTransform3d();
      trans.p.interpolate(pmin,  0.5, pmax);
      
      Vector2d size = new Vector2d(pmax.x-pmin.x, pmax.y-pmin.y);
      DicomPlaneViewer dpv = new DicomPlaneViewer("plane_" + viewerPlanes.size(), image, trans, size);
      addRenderable(dpv);
      
      viewerPlanes.add(dpv);
      
   }

}
