/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Collection;

import javax.swing.*;

import artisynth.core.driver.Main;
import artisynth.core.driver.ModelFileChooser;
import artisynth.core.femmodels.AnsysWriter;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.modelbase.*;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.io.GenericMeshWriter;
import maspack.matrix.*;
import maspack.util.NumberFormat;
import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.OptionPanel;
import maspack.widgets.StringField;
import maspack.render.MouseRayEvent;

/**
 * This class contains methods that all Edit Widgets and Editors can access and
 * use.
 * 
 */
public class EditorUtils {

   /**
    * Intersect a mouse ray with a plane that goes through the specified
    * component. The plane that is intersected with is perpendicular to the
    * mouse ray and passes through the specified component.
    * 
    * @param coordinateFrame
    * Coordinate frame defining the plane. The plane is assumed to pass throught
    * the frame's origin and be perpendicular to the z axis.
    * @param ray
    * The mouse ray.
    * @return The point of intersection between the plane and mouse ray.
    */
   public static Point3d intersectWithPlane (
      RigidTransform3d coordinateFrame, MouseRayEvent ray) {
      // create a plane perpendicular to the mouse ray and
      // passing through the specified component
      Point3d isectPoint = new Point3d();
      Point3d point =
         new Point3d (
            coordinateFrame.p.x / 2, coordinateFrame.p.y / 2,
            coordinateFrame.p.z / 2);
      isectPoint = intersectWithPlane (point, ray);

      // need to set the frame, convert from local to world coordinates
      isectPoint.inverseTransform (coordinateFrame);

      return isectPoint;
   }

   public static Point3d intersectWithPlane (Point3d point, MouseRayEvent ray) {
      // create a plane perpendicular to the mouse ray and passing
      // through the specified component
      Point3d isectPoint = new Point3d();
      Plane plane = new Plane (ray.getRay().getDirection(), point);

      // find the intersection of the mouse ray and the plane
      plane.intersectRay (
         isectPoint, ray.getRay().getDirection(), ray.getRay().getOrigin());

      return isectPoint;
   }

   public static void saveComponent (ModelComponent comp) {
      Main main = Main.getMain();
      JFrame frame = main.getMainFrame();
      ModelFileChooser chooser = 
         new ModelFileChooser(null, /*coreCompsOnly=*/false);
      chooser.setCurrentDirectory (main.getModelDirectory());
      int retVal = chooser.showDialog (frame, "Save As");
      if (retVal == JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         int status = 0;
         try {
            ModelComponent ancestor =
               ComponentUtils.nearestEncapsulatingAncestor (comp);
            if (ancestor == null) {
               ancestor = comp;
            }
            status = main.saveComponent (
               file, /*fmtStr=*/null, comp, 
               chooser.getCoreCompsOnly(), ancestor);
         }
         catch (Exception ex) {
            ex.printStackTrace(); 
            GuiUtils.showError (frame, "Error saving file: "+ex.getMessage());
         }
         if (status == -1) {
            GuiUtils.showError (
               frame, "Not a core component");
         }
         else if (status > 0) {
            GuiUtils.showNotice (
               frame, "Removed "+status+" non-core components");
         }
         main.setModelDirectory (chooser.getCurrentDirectory());
      }
   }

   public static void saveComponentNames (
      Collection<? extends ModelComponent> comps) {

      Main main = Main.getMain();
      JFrame frame = main.getMainFrame();
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory (main.getModelDirectory());
      int retVal = chooser.showSaveDialog (frame);
      if (retVal == JFileChooser.APPROVE_OPTION) {
         try {
            PrintWriter pw = new PrintWriter (
               new BufferedWriter (new FileWriter (chooser.getSelectedFile())));
            for (ModelComponent c : comps) {
               pw.println (ComponentUtils.getPathName(c));
            }
            pw.close();
         }
         catch (Exception ex) {
            ex.printStackTrace(); 
            GuiUtils.showError (frame, "Error saving file: "+ex.getMessage());
         }
         main.setModelDirectory (chooser.getCurrentDirectory());
      }
   }

   public static void saveMesh (MeshBase mesh, AffineTransform3dBase X) {
      Main main = Main.getMain();
      JFrame frame = main.getMainFrame();
      if (mesh == null) {
         GuiUtils.showError (frame, "Component does not have a mesh");
         return;
      }
      if (X != null) {
         mesh = mesh.copy();
         mesh.transform (X);
      }
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory (main.getModelDirectory());
      int retVal = chooser.showSaveDialog (frame);
      if (retVal == JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         try {
            
            if (mesh instanceof PolygonalMesh) {
               GenericMeshWriter writer = new GenericMeshWriter(file);
               writer.writeMesh(mesh);
               writer.close();
            } else {
               PrintWriter pw =
                  new PrintWriter (new BufferedOutputStream (new FileOutputStream (
                     file)));
               mesh.write (pw, "%.8g");
               pw.close();
            }
         }
         catch (Exception ex) {
            ex.printStackTrace(); 
            GuiUtils.showError (frame, "Error saving file: "+ex.getMessage());
         }
         main.setModelDirectory (chooser.getCurrentDirectory());
      }
   }

   public static void saveMeshAsAnsysFile (final FemModel3d model) {
      
      final Main main = Main.getMain();
      JFrame frame = main.getMainFrame();
      
      final JDialog saveOptions = new JDialog (frame, "Save As Ansys File");
      LabeledComponentPanel savePanel = new LabeledComponentPanel();
      savePanel.setBorder (BorderFactory.createEmptyBorder (8, 8, 0, 8));
      
      final StringField dirField = new StringField (
         "Output folder", main.getModelDirectory ().getAbsolutePath (), 20);
      dirField.setStretchable (true);
      dirField.getTextField ().setEditable (false);
      
      final JButton browseButton = new JButton ("Change");
      browseButton.addActionListener (new ActionListener () {
         public void actionPerformed (ActionEvent a_evt) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory (main.getModelDirectory ());
            chooser.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY);
            
            if ( chooser.showSaveDialog (browseButton) == 
                  JFileChooser.APPROVE_OPTION) {
               dirField.setValue (chooser.getSelectedFile ().getAbsolutePath ());
            }
         }
      });
      browseButton.setMargin (new Insets (3, 3, 3, 3));
      GuiUtils.setFixedSize (browseButton, new Dimension (80, 25));
      dirField.add (browseButton);
      savePanel.addWidget (dirField);
      
      final StringField nodeField = 
         new StringField ("Node file name", "mesh.node", 10);
      nodeField.setStretchable (true);
      savePanel.addWidget (nodeField);
      
      final StringField elemField = 
         new StringField ("Element file name", "mesh.elem", 10);
      elemField.setStretchable (true);
      savePanel.addWidget (elemField);
      
      final JCheckBox elemType = 
         new JCheckBox ("Use custom type values for different elements");
      savePanel.addWidget (elemType);
      
      LabeledComponentPanel elemTypePanel = new LabeledComponentPanel();
      elemTypePanel.setBorder (BorderFactory.createEmptyBorder (0, 15, 0, 0));
      final IntegerField tetField = new IntegerField ("Tets (4 node)", 1);
      tetField.getTextField ().setEditable (false);
      elemTypePanel.addWidget (tetField);
      final IntegerField quadtetField = new IntegerField ("Quadtets (10 node)", 1);
      quadtetField.getTextField ().setEditable (false);
      elemTypePanel.addWidget (quadtetField);
      final IntegerField wedgeField = new IntegerField ("Wedges", 1);
      wedgeField.getTextField ().setEditable (false);
      elemTypePanel.addWidget (wedgeField);
      final IntegerField hexField = new IntegerField ("Hexs (8 node)", 1);
      hexField.getTextField ().setEditable (false);
      elemTypePanel.addWidget (hexField);
      final IntegerField quadhexField = new IntegerField ("Quadhexs (20 node)", 1);
      quadhexField.getTextField ().setEditable (false);
      elemTypePanel.addWidget (quadhexField);
      savePanel.addWidget (elemTypePanel);
      
      elemType.addActionListener (new ActionListener () {
         public void actionPerformed (ActionEvent a_evt) {
            if (elemType.isSelected ()) {
               tetField.getTextField ().setEditable (true);
               quadtetField.getTextField ().setEditable (true);
               wedgeField.getTextField ().setEditable (true);
               hexField.getTextField ().setEditable (true);
               quadhexField.getTextField ().setEditable (true);
            }
            else {
               tetField.getTextField ().setEditable (false);
               quadtetField.getTextField ().setEditable (false);
               wedgeField.getTextField ().setEditable (false);
               hexField.getTextField ().setEditable (false);
               quadhexField.getTextField ().setEditable (false);
            }
         }
      });
      
      OptionPanel dialogOptions = 
         new OptionPanel ("Save Cancel", new ActionListener () {
            public void actionPerformed (ActionEvent a_evt) {
               if (a_evt.getActionCommand ().equals ("Save")) {
                  File directory = new File (dirField.getStringValue ());
                  
                  File nodeFile = new File (directory, nodeField.getStringValue ());
                  File elemFile = new File (directory, elemField.getStringValue ());
                  
                  AnsysWriter.writeNodeFile (model, nodeFile.getAbsolutePath ());
                  AnsysWriter.writeElemFile (model, elemFile.getAbsolutePath ());
               } 
               
               saveOptions.dispose ();
            }
         });
      
      GuiUtils.setFixedSize (
         dialogOptions.getButton ("Save"), new Dimension (80, 25));
      GuiUtils.setFixedSize (
         dialogOptions.getButton ("Cancel"), new Dimension (80, 25));
      dialogOptions.setBorder (BorderFactory.createEmptyBorder (0, 0, 8, 0));
      savePanel.addWidget (dialogOptions);
      
      saveOptions.getContentPane ().add (savePanel);      
      saveOptions.pack ();
      saveOptions.setMinimumSize (saveOptions.getPreferredSize ());
      saveOptions.setVisible (true);
      
   }
}
