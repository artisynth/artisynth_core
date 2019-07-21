/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.Map;

import artisynth.core.materials.HasMaterialState;
import artisynth.core.materials.IncompressibleMaterialBase;
import artisynth.core.modelbase.ModelComponent;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix1x1;
import maspack.matrix.Matrix2d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix4d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.InternalErrorException;

public abstract class FemElement3d extends FemElement3dBase {
   
   protected double[] myRestVolumes;
   protected double[] myVolumes;
   protected double[] myLagrangePressures;

   protected MatrixBlock[] myIncompressConstraints = null;
   private int myIncompressIdx = -1;

   private static Matrix1x1 myPressureWeightMatrix = null;

   public FemElement3d() {
      int npvals = numPressureVals();
      myLagrangePressures = new double[npvals];
      myRestVolumes = new double[npvals];
      myVolumes = new double[npvals];
      myElementClass = ElementClass.VOLUMETRIC;
   }
   
   /* --- Stiffness warping --- */

   // protected void updateWarpingStiffness(double weight) {
   //    super.updateWarpingStiffness (weight);
   // }

   /* --- Volume --- */

   /**
    * {@inheritDoc}
    *
    * <p>Partial volumes are also computed, and stored in the {@code myVolumes}
    * field. If the number of pressure values is 1, then there is only one
    * partial volume which is equal to the overall volume.
    *
    * <p>The base implementation of this method used quadrature.Individual
    * elements can override this with a more efficient method if needed.
    */
   public double computeVolumes() {
      return computeVolumes (myVolumes, /*rest=*/false);      
   }

   /**
    * Returns the partial volumes. The number of partial volumes is
    * equals to the number of pressure values.
    *
    * @return current partial volumes
    */
   public double[] getVolumes() {
      return myVolumes;
   }

   /**
    * {@inheritDoc}
    *
    * <p>Partial rest volumes are also computed, and stored in the {@code
    * myRestVolumes} field. If the number of pressure values is 1, then there
    * is only one partial rest volume which is equal to the overall rest
    * volume.
    *
    * <p>The base implementation of this method used quadrature. Individual
    * elements can override this with a more efficient method if needed.
    */
   public double computeRestVolumes() {
      return computeVolumes (myRestVolumes, /*rest=*/true);
   }

   protected double computeVolumes (double[] partialVolumes, boolean rest) {
      int npvals = numPressureVals();
      
      double vol = 0;
      for (int k=0; k<npvals; k++) {
         partialVolumes[k] = 0;
      }
      
      double minDetJ = Double.MAX_VALUE;

      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int i=0; i<ipnts.length; i++) {
         IntegrationPoint3d pt = ipnts[i];
         double detJ;
         double dv;
         if (rest) {
            detJ = idata[i].getDetJ0();
            dv = detJ*pt.getWeight();
         }
         else {
            detJ = pt.computeJacobianDeterminant (myNodes);
            dv = detJ*pt.getWeight();
            // normalize detJ to get true value relative to rest position
            detJ /= idata[i].getDetJ0();
            // store dv in idata where it can be used for computing soft nodal
            // incompressibity
            idata[i].setDv(dv);
         }
         if (npvals > 1) {
            double[] H = pt.getPressureWeights().getBuffer();
            for (int k=0; k<npvals; k++) {
               partialVolumes[k] += H[k]*dv;
            } 
         }
         if (detJ < minDetJ) {
            minDetJ = detJ;
         }
         vol += dv;
      }
      if (npvals == 1) {
         partialVolumes[0] = vol;         
      }      
      if (rest) {
         myRestVolume = vol;
      }
      else {
         myVolume = vol;
      }
      return minDetJ;
   }
   
   /**
    * Returns the partial rest volumes. The number of partial rest volumes is
    * equals to the number of pressure values.
    *
    * @return current partial rest volumes
    */
   public double[] getRestVolumes() {
      return myRestVolumes;
   }

   /* --- Incompressibility --- */

   // index of the incompressibility constraint associated with
   // this element, if any
   public int getIncompressIndex() {
      return myIncompressIdx;
   }

   public void setIncompressIndex (int idx) {
      myIncompressIdx = idx;
   }

   /**
    * Returns the number of pressure variables associated with this element.  
    * All of the linear elements have one pressure variable, whereas some of 
    * the quadratic elements have more.
    * 
    * @return number of pressure variables.make
    * 
    */
   public int numPressureVals() {
      // higher order elements should override this if necessary
      return 1;
   }

   /**
    * Returns the value of the pressure shape function. By default, this method
    * returns 1, corresponding to a single pressure variable with constant
    * value over the entire element. Elements with a larger number of pressure
    * DOFs should override this method to supply the appropriate shape
    * functions.
    *
    * @param i index of the pressure variable; should be less
    * than the value returned by {@link #numPressureVals}
    * @param coords coordinates at which the shape function should
    * be evaluated.
    * @return value of the pressure shape function
    */
   public double getH (int i, Vector3d coords) {
      return 1;
   }

   /**
    * Returns the pressure weight matrix for this element. The pressure
    * weight matrix is given by the inverse of the integral of
    * H^T H, where H is the row vector formed from the pressure
    * shape functions.
    *
    * <p>By default, this method returns a pressure weight matrix for the case
    * where there is only one pressure value. Such matrices always have a
    * single value of 1. Elements with a larger number of pressure values
    * should override this method to return a pressure weight matrix
    * appropriate for that element.
    *
    * @return pressure weight matrix for this element
    */
   public Matrix getPressureWeightMatrix () {
      if (myPressureWeightMatrix == null) {
         myPressureWeightMatrix = new Matrix1x1();
         myPressureWeightMatrix.m00 = 1;
      }
      return myPressureWeightMatrix;
   }

   /**
    * Creates a pressure weight matrix for an element. Intended for
    * use when overriding of {@link #getPressureWeightMatrix}.
    */
   protected Matrix createPressureWeightMatrix () {
      int npvals = numPressureVals();
      if (npvals == 1) {
         Matrix1x1 M = new Matrix1x1();
         M.m00 = 1;
         return M;
      }
      else {
         IntegrationPoint3d[] ipnts = getIntegrationPoints(); 
         MatrixNd M = new MatrixNd (npvals, npvals);
         for (int k=0; k<ipnts.length; k++) {
            double[] H = ipnts[k].getPressureWeights().getBuffer();
            for (int i=0; i<npvals; i++) {
               for (int j=0; j<npvals; j++) {
                  M.add (i, j, H[i]*H[j]);
               }
            }
         }
         M.invert();
         if (npvals == 2) {
            return new Matrix2d (M);
         }
         else if (npvals == 4) {
            return new Matrix4d (M);
         }
         else {
            return M;
         }
      }
   }
   
   /**  
    * Returns an array of MatrixBlocks to be used as constraints to make the
    * element incompressible. Note this method does not compute values for
    * these constraints; it only returns storage for them.
    *
    * <p>There is one block for each node, with each of size 3 x m, where m is
    * the number of pressure degrees-of-freedom (returned by
    * {@link #numPressureVals()}.
    * 
    * @return incompressibility constraints
    */
   public MatrixBlock[] getIncompressConstraints() {
      if (myIncompressConstraints == null) {
         int n = numNodes();
         MatrixBlock[] constraints = new MatrixBlock[n];
         for (int i=0; i<n; i++) {
            constraints[i] = MatrixBlockBase.alloc (3, numPressureVals());
         }
         myIncompressConstraints = constraints;
      }
      return myIncompressConstraints;
   }
   
   /* --- Shape functions and coordinates --- */

   /**
    * {@inheritDoc}
    */
   @Override
   public void computeLocalPosition (Vector3d pnt, Vector3d ncoords) {
      pnt.setZero();
      for (int i=0; i<numNodes(); ++i) {
         pnt.scaledAdd(getN(i,ncoords), myNodes[i].getLocalPosition());
      }
   }  

   public void computeJacobian (Matrix3d J, Vector3d ncoords) {
      Vector3d dNds = new Vector3d();
      J.setZero();
      for (int k=0; k<numNodes(); k++) {
         getdNds (dNds, k, ncoords);
         J.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
      }      
   }

//   public void computePressures (
//      double[] pressures, IncompressibleMaterialBase imat) {
//
//      int npvals = numPressureVals();
//      IntegrationPoint3d[] ipnts = getIntegrationPoints();
//      IntegrationData3d[] idata = getIntegrationData();
//      for (int k=0; k<npvals; k++) {
//         pressures[k] = 0;
//      }
//      for (int i=0; i<ipnts.length; i++) {
//         double dV = idata[i].getDetJ0()*ipnts[i].getWeight();
//         IntegrationPoint3d pt = ipnts[i];
//         double detJ = pt.computeJacobianDeterminant (myNodes);
//         double[] H = pt.getPressureWeights().getBuffer();
//         for (int k=0; k<npvals; k++) {
//            pressures[k] += H[k]*dV*imat.getEffectivePressure (detJ);
//         }            
//      }
//   }

   /**
    * Lagrange pressures array for use with incompressibility 
    * @return pressures
    */
   public double[] getLagrangePressures() {
      return myLagrangePressures;
   }
   
   // /**
   //  * {@inheritDoc}
   //  */
   // public boolean materialsAreInvertible() {
   //    if (!super.materialsAreInvertible()) {
   //       return false;
   //    }
   //    if (myAuxMaterials != null) {
   //       for (AuxiliaryMaterial mat : myAuxMaterials) {
   //          if (!mat.isInvertible()) {
   //             return false;
   //          }
   //       }
   //    }
   //    return true;
   // }

//   public double computeDirectedRenderSize (Vector3d dir) {
//      IntegrationPoint3d ipnt = getWarpingPoint();
//      return ipnt.computeDirectedSizeForRender (dir, getNodes());
//   }

   static int numEdgeSegs = 10;

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      double scubed = s*s*s;
      for (int i=0; i<numPressureVals(); i++) {
         myLagrangePressures[i] /= s;
         myVolumes[i] *= scubed; 
         myRestVolumes[i] *= scubed; 
      }
   }

   // protected void collectMaterialsWithState (
   //    ArrayList<HasMaterialState> mats) {
   //    super.collectMaterialsWithState (mats);
   //    if (myAuxMaterials != null) {
   //       for (AuxiliaryMaterial aux: myAuxMaterials) {
   //          if (aux.hasState()) {
   //             mats.add (aux);
   //          }
   //       }
   //    }
   // }

   /* --- Scanning, writing and copying --- */

   @Override
   public FemElement3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      FemElement3d e = (FemElement3d)super.copy (flags, copyMap);
      e.myIncompressConstraints = null;
      e.myLagrangePressures = new double[numPressureVals()];
      e.myIncompressIdx = -1;
      // e.myAuxMaterials = null;
      // if (myAuxMaterials != null) {
      //    for (AuxiliaryMaterial a : myAuxMaterials) {
      //       try {
      //          e.addAuxiliaryMaterial ((AuxiliaryMaterial)a.clone());
      //       }
      //       catch (Exception ex) {
      //          throw new InternalErrorException (
      //             "Can't clone " + a.getClass());
      //       }
            
      //    }
      // }
      return e;
   }

   /* --- Element creation --- */

   public static FemElement3d createElement (FemNode3d[] nodes) {
      
      switch(nodes.length) {
         case 4:
            return new TetElement(nodes);
         case 5:
            return new PyramidElement(nodes);
         case 6:
            return new WedgeElement(nodes);
         case 8:
            return new HexElement(nodes);
         default:
            throw new IllegalArgumentException(
               "Unknown element type with " + nodes.length + " nodes");
      }
   }

   public static FemElement3d createElement (FemNode3d[] nodes, boolean flipped) {
      
      if (flipped) {
         switch(nodes.length) {
            case 4:
               return new TetElement (
                  nodes[0], nodes[2], nodes[1], nodes[3]);
            case 5:
               return new PyramidElement (
                  nodes[0], nodes[3], nodes[2], nodes[1], nodes[4]);
            case 6:
               return new WedgeElement (
                  nodes[0], nodes[2], nodes[1], nodes[3], nodes[5], nodes[4]);
            case 8:
               return new HexElement(
                  nodes[0], nodes[3], nodes[2], nodes[1], 
                  nodes[4], nodes[7], nodes[6], nodes[5]);
            default:
               throw new IllegalArgumentException(
                  "Unknown element type with " + nodes.length + " nodes");
         }
      }
      else {
         return createElement(nodes);
      }
   }

}

