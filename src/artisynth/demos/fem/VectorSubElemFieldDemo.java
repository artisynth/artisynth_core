package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.ShellElement3d;
import artisynth.core.fields.VectorSubElemField;
import artisynth.core.gui.ControlPanel;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

/**
 * Demonstrates creation and visualization of a simple VectorSubElemField.  For
 * the FEM itself, uses the combined volumetric/shell element FEM from
 * CombinedShellFem.
 */
public class VectorSubElemFieldDemo extends CombinedShellFem {

      public void build (String[] args) {

      // First create the FEM and property settings from CombinedShellFem.
      // The FEM is stored in the variable myFem.
      buildStructure();
      setProperties();

      // create a VectorSubElemField and add it to the FEM
      VectorSubElemField<Vector3d> field = 
         new VectorSubElemField<> (Vector3d.class, myFem);
      myFem.addField (field);

      // set field values (for both volumetric and shell elements) to the
      // vector between the integration point and a reference position at the
      // world origin
      Point3d refpos = new Point3d (0,0,0);
      Point3d vec = new Point3d();
      for (FemElement3d e : myFem.getElements()) {
         IntegrationPoint3d [] ipnts = e. getAllIntegrationPoints () ;
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computePosition (vec, e);
            vec.sub (refpos);
            field.setValue (e, k, vec);
         }
      }
      for (ShellElement3d e : myFem.getShellElements()) {
         IntegrationPoint3d [] ipnts = e. getAllIntegrationPoints () ;
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computePosition (vec, e);
            vec.sub (refpos);
            field.setValue (e, k, vec);
         }
      }

      // create a control panel to adjust various field properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "renderScale");
      panel.addWidget (field, "volumeElemsVisible");
      panel.addWidget (field, "shellElemsVisible");
      addControlPanel (panel);

      // -- render properties -- 
      // render field values as blue arrows, with radius 0.1, scaled by 0.5
      // from their true value
      RenderProps.setSolidArrowLines (field, 0.1, new Color(0.2f, 0.6f, 1f));
      field.setRenderScale (0.5);
      // make the shell mesh (created by CombinedShellFem) invisible
      RenderProps.setVisible (myShellMeshComp, false);
      // make FEM lines blue gray (for rendering element edges)
      RenderProps.setLineColor (myFem, new Color (0.7f, 0.7f, 1f));
   }
}
