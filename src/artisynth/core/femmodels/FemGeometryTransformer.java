package artisynth.core.femmodels;

import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Uses the piecewise smooth deformation field imported by a deformed FEM model
 * to effect a nonlinear geometric transformation.
 */
public class FemGeometryTransformer extends DeformationTransformer {

   FemModel3d myFem;
   FemModel3d myRestFem;

   /**
    * Creates a FemGeometry transformer for the specified FEM model
    * 
    * @param fem FEM model associated with the transformer
    */
   public FemGeometryTransformer (FemModel3d fem) {
      myFem = fem;
      myRestFem = fem.copy (CopyableComponent.REST_POSITION, null);
   }

   /**
    * {@inheritDoc}
    */
   public void getDeformation (Vector3d p, Matrix3d F, Vector3d r) {
      
      Point3d rpos = new Point3d(r);
      FemElement3dBase restElem = myRestFem.findNearestElement (null, rpos);
      Vector3d coords = new Vector3d();
      if (!restElem.getNaturalCoordinates (coords, rpos)) {
         //System.out.println ("warning...");
      }
      IntegrationPoint3d ipnt =
         IntegrationPoint3d.create (restElem, coords.x, coords.y, coords.z, 1);
      Matrix3d invJ0 = new Matrix3d();
      double detJ0 = 
         ipnt.computeInverseRestJacobian (invJ0, restElem.getNodes());
      if (detJ0 <= 0) {
         //System.out.println ("warning...");
      }

      int elemIdx = myRestFem.getElements().indexOf(restElem);
      FemElement3d elem = myFem.getElements().get(elemIdx);

      if (p != null) {
         Point3d pos = new Point3d();
         ipnt.computePosition (pos, elem.getNodes());
         p.set (pos);         
      }
      if (F != null) {
         ipnt.computeGradient (F, elem.getNodes(), invJ0);
         //F.set (ipnt.getF());
      }
   }

}
