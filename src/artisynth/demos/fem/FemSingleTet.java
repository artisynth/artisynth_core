package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.widgets.DoubleFieldSlider;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.TetElement;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class FemSingleTet extends RootModel {
   public static boolean debug = false;

   FemModel3d myFemMod;

   static double myDensity = 1000;

   public static PropertyList myProps =
      new PropertyList (FemSingleTet.class, RootModel.class);

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemSingleTet() {
      super (null);
   }

   public FemSingleTet (String name) {
      this();
      setName (name);

      int nn = 1;
      // myFemMod =
      // FemModel3d.createGrid (
      // "fem", 0.6, 0.2, 0.2, nn*3, nn*1, nn*1, myDensity);

      FemNode3d node0 = new FemNode3d (-0.1, -0.1, -0.1);
      FemNode3d node1 = new FemNode3d (-0.1, -0.1, 0.1);
      FemNode3d node2 = new FemNode3d (0.1, -0.1, 0.1);
      FemNode3d node3 = new FemNode3d (0.1, 0.1, 0.1);
      TetElement tet = new TetElement (node0, node1, node2, node3);
      myFemMod = new FemModel3d ("fem");
      myFemMod.addNode (node0);
      myFemMod.addNode (node1);
      myFemMod.addNode (node2);
      myFemMod.addNode (node3);
      myFemMod.addElement (tet);

      // System.out.println (
      // "K=\n" + myFemMod.getActiveStiffnessMatrix().toString ("%8.3f"));

      node0.setDynamic (false);
      node1.setDynamic (false);
      node3.setDynamic (false);

      myFemMod.setBounds (new Point3d (-0.3, 0, 0), new Point3d (0.3, 0, 0));
      myFemMod.setLinearMaterial (20000, 0.4, true);

      myFemMod.setStiffnessDamping (0.002);
      Renderable elems = myFemMod.getElements();
      RenderProps.setLineWidth (elems, 2);
      RenderProps.setLineColor (elems, Color.BLUE);
      Renderable nodes = myFemMod.getNodes();
      RenderProps.setPointStyle (nodes, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (nodes, 0.005);
      RenderProps.setPointColor (nodes, Color.GREEN);

      addModel (myFemMod);
   }

   ControlPanel myControlPanel;

   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      JFrame frame = driver.getFrame();

      myFemMod = (FemModel3d)findComponent ("models/fem");

      if (getControlPanels().size() == 0) {
         myControlPanel = new ControlPanel ("options", "");
//         DoubleFieldSlider ymSlider =
//            (DoubleFieldSlider)myControlPanel.addWidget (
//               myFemMod, "YoungsModulus", 0, 10000);
//         ymSlider.setRoundingTolerance (10);
         FemControlPanel.addFemControls (myControlPanel, myFemMod, myFemMod);

         myControlPanel.pack();
         myControlPanel.setVisible (true);
         Point loc = frame.getLocation();
         myControlPanel.setLocation (loc.x + frame.getWidth(), loc.y);
         addControlPanel (myControlPanel);
      }
   }

   @Override
   public void detach (DriverInterface driver) {
      super.detach (driver);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
}
