package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.ShellElement3d;
import artisynth.core.fields.ScalarFemField;
import artisynth.core.fields.ScalarSubElemField;
import artisynth.core.gui.ControlPanel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;

/**
 * Demonstrates creation and visualization of a simple ScalarSubElemField.  For
 * the FEM itself, uses the combined volumetric/shell element FEM from
 * CombinedShellFem.
 */
public class ScalarSubElemFieldDemo extends CombinedShellFem {

   public void build (String[] args) {

      // First create the FEM and property settings from CombinedShellFem.  The
      // FEM is stored in the variable myFem.
      buildStructure();
      setProperties();

      // create a ScalarSubElemField and add it to the FEM
      ScalarSubElemField field = new ScalarSubElemField (myFem);
      myFem.addField (field);

      // set field values (for both volumetric and shell elements) to the
      // distance between the integration points and a reference position at
      // the world origin
      Point3d refpos = new Point3d (0,0,0);
      Point3d pos = new Point3d ();
      for (FemElement3d e : myFem.getElements()) {
         IntegrationPoint3d [] ipnts = e. getAllIntegrationPoints () ;
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computePosition (pos, e);
            field.setValue (e, k, pos.distance(refpos));
         }
      }
      for (ShellElement3d e : myFem.getShellElements()) {
         IntegrationPoint3d [] ipnts = e. getAllIntegrationPoints () ;
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computePosition (pos, e);
            field.setValue (e, k, pos.distance(refpos));
         }
      }

      // for SURFACE visualization, create a 10.0 x 10.0 plane, with triangle
      // resolution 40 x 40
      int res = 40;
      PolygonalMesh mesh = MeshFactory.createPlane (10.0, 10.0, res, res);
      // rotate the plane from the x-y plane into the x-z plane
      mesh.transform (new RigidTransform3d (0, 0, 0,  0, 0, Math.PI/2));

      // embed the plane in the FEM, make it invisible so it does not interfere
      // with SURFACE visualization, and add it to the field as a render mesh
      FemMeshComp mcomp = myFem.addMesh (mesh);
      RenderProps.setVisible (mcomp, false);
      field.addRenderMeshComp (mcomp);

      // create a control panel to adjust various field properties         
      ControlPanel panel = new ControlPanel();
      panel.addWidget (field, "visualization");
      panel.addWidget (field, "volumeElemsVisible");
      panel.addWidget (field, "shellElemsVisible");
      panel.addWidget (field, "renderRange");
      panel.addWidget (field, "colorMap");
      addControlPanel (panel);

      // -- render properties --
      // initialize field visualization to POINT
      field.setVisualization (ScalarFemField.Visualization.POINT);
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
