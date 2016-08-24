package artisynth.core.workspace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.io.*;

import javax.swing.JMenuItem;

import java.util.*;

import artisynth.demos.fem.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.geometry.*;
import maspack.render.*;
import artisynth.core.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.gui.*;
import artisynth.core.driver.*;

public class FemModelDeformer extends FemModel3d implements ActionListener {

   MechModel myMechMod;
   FemGeometryTransformer myTransformer;
   boolean myDeformedP = false;

   public FemModelDeformer() {
      super();
   }

   public FemModelDeformer (String name, RootModel root, int maxn) {
      this();
      MechModel mech = null;
      for (Model m : root.models()) {
         if (m instanceof MechModel) {
            mech = (MechModel)m;
            break;
         }
      }
      if (mech == null) {
         throw new IllegalArgumentException (
            "Root model does not contain a mech model");
      }
      setForMechModel (name, mech, maxn);
   }

   public FemModelDeformer (String name, MechModel mech, int maxn) {
      setForMechModel (name, mech, maxn);
   }

   public void setForMechModel (String name, MechModel mech, int maxn) {

      Point3d min = new Point3d();
      Point3d max = new Point3d();
      RenderableUtils.getBounds (mech, min, max);

      Vector3d widths = new Vector3d();
      Vector3d center = new Vector3d();
      center.add (max, min);
      center.scale (0.5);
      widths.sub (max, min);

      double margin = 0.2*widths.maxElement();
      widths.x += margin;
      widths.y += margin;
      widths.z += margin;

      double maxw = widths.maxElement();

      int nx = (int)Math.round((widths.x/maxw)*maxn);
      int ny = (int)Math.round((widths.y/maxw)*maxn);
      int nz = (int)Math.round((widths.z/maxw)*maxn);

      doSet (name, mech, widths, center, nx, ny, nz);
   }

   public void setForMechModel (
      String name, MechModel mech,
      double wx, double wy, double wz, int nx, int ny, int nz) {

      Point3d min = new Point3d();
      Point3d max = new Point3d();
      RenderableUtils.getBounds (mech, min, max);

      Vector3d center = new Vector3d();
      center.add (max, min);
      center.scale (0.5);

      doSet (name, mech, new Vector3d(wx, wy, wz), center, nz, ny, nz);
   }

   private void doSet (
      String name, MechModel mech, Vector3d widths, Vector3d center,
      int nx, int ny, int nz) {

      setName (name);
      FemFactory.createHexGrid (this, widths.x, widths.y, widths.z, nx, ny, nz);
      transformGeometry (new RigidTransform3d (center.x, center.y, center.z));

      double maxw = widths.maxElement();
      setGravity (Vector3d.ZERO);
      setDynamicsEnabled (false);
      setDensity (1000/(maxw*maxw*maxw));
      setMaterial (new LinearMaterial (500000/maxw, 0.33));
      RenderProps.setSphericalPoints (this, maxw*0.01, Color.GRAY);
      //RenderProps.setVisible (this, false);
      RenderProps.setPointColor (this, new Color (0.2f, 0.6f, 1.0f));
      RenderProps.setLineColor (this, new Color (0.2f, 0.6f, 1.0f));

      myTransformer = new FemGeometryTransformer (this);
      myMechMod = mech;
   }

   public JMenuItem makeMenuItem (String cmd, String toolTip) {
      JMenuItem item = new JMenuItem(cmd);
      item.addActionListener(this);
      item.setActionCommand(cmd);
      if (toolTip != null && !toolTip.equals ("")) {
         item.setToolTipText (toolTip);
      }
      return item;
   }

   public ControlPanel createControlPanel() {
      ControlPanel panel = new ControlPanel();
      panel.addWidget (
         "model dynamic", myMechMod, "dynamicsEnabled");
      panel.addWidget (
         "grid dynamic", this, "dynamicsEnabled");
      panel.addWidget (
         "grid visible", this, "renderProps.visible");
      panel.addWidget (
         "grid nodes visible", this, "nodes:renderProps.visible");
      return panel;
   }

   private void resetGrid() {
      for (FemNode3d n : getNodes()) {
         n.setPosition (n.getRestPosition());
      }
      RootModel root = RootModel.getRoot (this);
      if (root != null) {
         root.rerender();
      }
   }

   private void resetTime() {

      RootModel root = RootModel.getRoot (this);
      if (root != null) {
         root.getWayPoint(0).setValid (false);
         Main main = Main.getMain();
         if (main != null) {
            main.reset();
         }
      }
   }

   public void undoDeformation () {
      if (myDeformedP) {
         myTransformer.setUndoState (
            GeometryTransformer.UndoState.RESTORING);
         TransformGeometryContext.transform (myMechMod, myTransformer, 0);
         myDeformedP = false;
         resetTime();
      }
   }

   public void applyDeformation () {
      if (!myDeformedP) {
         myTransformer.setUndoState (GeometryTransformer.UndoState.SAVING);
         TransformGeometryContext.transform (myMechMod, myTransformer, 0);
         myDeformedP = true;
         resetTime();
      }
   }

   public boolean getMenuItems(List<Object> items) {
      JMenuItem menuItem;
      menuItem = makeMenuItem ("deform", "deform the model");
      menuItem.setEnabled (!myDeformedP); 
      items.add (menuItem);
      menuItem = makeMenuItem ("undo", "unfo the deformation");
      menuItem.setEnabled (myDeformedP);
      items.add (menuItem);
      items.add (makeMenuItem ("reset grid", "reset the deformation grid"));
      return true;
   }   

   public void actionPerformed(ActionEvent event) {
      if (event.getActionCommand().equals ("deform")) {
         applyDeformation();
      }
      else if (event.getActionCommand().equals ("undo")) {
         undoDeformation();
      }
      else if (event.getActionCommand().equals ("reset grid")) {
         resetGrid();
      }
   } 

   public boolean hierarchyContainsReferences() {
      return false;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "model", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "model")) {
         myMechMod = postscanReference (tokens, MechModel.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public FemGeometryTransformer getTransformer() {
      return myTransformer;
   }

}
