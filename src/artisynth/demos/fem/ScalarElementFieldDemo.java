package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.ShellElement3d;
import artisynth.core.fields.ScalarElementField;
import artisynth.core.fields.ScalarFemField;
import artisynth.core.gui.ControlPanel;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;

/**
 * Demonstrates creation and visualization of a simple ScalarElementField.  For
 * the FEM itself, uses the combined volumetric/shell element FEM from
 * CombinedShellFem.
 */
public class ScalarElementFieldDemo extends CombinedShellFem {

   public void build (String[] args) {

      // First create the FEM and property settings from CombinedShellFem.
      // The FEM is stored in the variable myFem.
      buildStructure();
      setProperties();

      // create a ScalarElementField and add it to the FEM
      ScalarElementField field = new ScalarElementField (myFem);
      myFem.addField (field);

      // set field values (for both volumetric and shell elements) to the
      // distance between the element centroid and a reference position at the
      // world origin
      Point3d refpos = new Point3d (0,0,0);
      for (FemElement3d e : myFem.getElements()) {
         Point3d cent = new Point3d();
         e.computeCentroid (cent);
         field.setValue (e, cent.distance(refpos));
      }
      for (ShellElement3d e : myFem.getShellElements()) {
         Point3d cent = new Point3d();
         e.computeCentroid (cent);
         field.setValue (e, cent.distance(refpos));
      }

      // create a control panel to adjust various field properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "elementWidgetSize");
      panel.addWidget (field, "volumeElemsVisible");
      panel.addWidget (field, "shellElemsVisible");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      addControlPanel (panel);

      // -- render properties --
      // initialize field visualization to ELEMENT
      field.setVisualization (ScalarFemField.Visualization.ELEMENT);
      // set field points to render as spheres with radii 0.25 (for POINT
      // visualization)
      RenderProps.setPointRadius (field, 0.25);
      RenderProps.setPointStyle (field, PointStyle.SPHERE);
      // make the shell mesh (created by CombinedShellFem) invisible
      RenderProps.setVisible (myShellMeshComp, false);
      // make FEM lines blue gray (for rendering element edges)
      RenderProps.setLineColor (myFem, new Color (0.7f, 0.7f, 1f));
   }
}
