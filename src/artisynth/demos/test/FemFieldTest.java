package artisynth.demos.test; 

import java.awt.Color;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;

public class FemFieldTest extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d fem = new FemModel3d ("fem");
      fem.setDensity (1000);
      FemFactory.createHexGrid (fem, 1, 0.25, 0.25, 4, 2, 2);
      mech.addModel (fem);

      RenderProps.setSphericalPoints (fem, 0.01, Color.GREEN);
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setLineWidth (fem, 2);

      Matrix3d prod = new Matrix3d();

      VectorNodalField<Vector3d> v3nfield = new VectorNodalField<> (
         Vector3d.class, fem, Vector3d.ZERO);
      VectorNdNodalField vnnfield = new VectorNdNodalField (
         3, fem, new VectorNd(3));
      MatrixNdNodalField vmnfield = new MatrixNdNodalField (
         3, 3, fem, new MatrixNd(3,3));
      ScalarNodalField snfield = new ScalarNodalField (fem, 0);

      for (FemNode3d n : fem.getNodes()) {
         Point3d pos = n.getPosition();
         v3nfield.setValue (n, pos);
         vnnfield.setValue (n, new VectorNd(pos));
         prod.outerProduct (pos, pos);
         vmnfield.setValue (n, new MatrixNd(prod));         
         snfield.setValue (n, pos.norm());
      }
      fem.addField (v3nfield);
      fem.addField (vnnfield);
      fem.addField (vmnfield);
      fem.addField (snfield);

      VectorElementField<Vector3d> v3efield = new VectorElementField<> (
         Vector3d.class, fem, Vector3d.ZERO);
      VectorSubElemField<Vector3d> v3sfield = new VectorSubElemField<> (
         Vector3d.class, fem, Vector3d.ZERO);
      VectorNdElementField vnefield = new VectorNdElementField (
         3, fem);
      VectorNdSubElemField vnsfield = new VectorNdSubElemField (
         3, fem, new VectorNd(3));
      MatrixNdElementField vmefield = new MatrixNdElementField (
         3, 3, fem);
      MatrixNdSubElemField vmsfield = new MatrixNdSubElemField (
         3, 3, fem, new MatrixNd(3,3));
      ScalarElementField sefield = new ScalarElementField (fem, 0);
      ScalarSubElemField ssfield = new ScalarSubElemField (fem, 0);

      for (FemElement3d e : fem.getElements()) {
         IntegrationPoint3d[] ipnts = e.getAllIntegrationPoints();
         Point3d pos = new Point3d();
         Point3d center = new Point3d();
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computeRestPosition (pos, e.getNodes());
            v3sfield.setValue (e, k, pos);
            vnsfield.setValue (e, k, new VectorNd(pos));
            prod.outerProduct (pos, pos);
            vmsfield.setValue (e, k, new MatrixNd(prod));
            ssfield.setValue (e, k, pos.norm());
            center.add (pos);
         }
         center.scale (1.0/ipnts.length);
         v3efield.setValue (e, center);
         vnefield.setValue (e, new VectorNd(center));
         prod.outerProduct (center, center);
         vmefield.setValue (e, new MatrixNd(prod));
         sefield.setValue (e, center.norm());
      }
      fem.addField (v3efield);
      fem.addField (vnefield);
      fem.addField (vmefield);
      fem.addField (sefield);
      fem.addField (v3sfield);
      fem.addField (vnsfield);
      fem.addField (vmsfield);
      fem.addField (ssfield);
   }
}
