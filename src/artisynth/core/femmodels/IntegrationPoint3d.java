/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.femmodels.FemNode3d.CoordType;

/**
 * This class stores readonly and transient information for each integration
 * point within a 3D element. To avoid excessive memory requirements, this data
 * will generally be allocated statically within the each element class. The
 * readonly data presents no problem in this regard, while the transient data
 * will be valid only while stiffness update computations are being performed
 * for the element.
 *
 * <p>Data that needs to be stored and retained per-element is declared
 * in the IntegrationData3d class.
 */
public class IntegrationPoint3d {

   protected int myNumNodes;          // static
   protected Vector3d coords;         // static
   protected VectorNd N;              // static
   protected VectorNd H;              // static
   protected Vector3d GNs[];          // static

   protected Vector3d[] GNx = null;   // transient

   protected double myWeight = 1;     // static
   protected int myNum = -1;          // static
   protected ElementClass myElemClass; 
   protected FemElement3dBase myElem;

   protected void init(int nnodes, int npvals) {
      myNumNodes = nnodes;
      N = new VectorNd(nnodes);
      H = new VectorNd(npvals);
      GNs = new Vector3d[nnodes];
      GNx = new Vector3d[nnodes];
      for (int i=0; i<nnodes; i++) {
         GNs[i] = new Vector3d();
         GNx[i] = new Vector3d();
      }
      coords = new Vector3d();
   }

   public IntegrationPoint3d (
      int nnodes, int npvals, 
      double s0, double s1, double s2, double w) {
      init (nnodes, npvals);
      setCoords (s0, s1, s2);
      setWeight (w);
      setElementClass (ElementClass.VOLUMETRIC); // XXX default volumetric
   }
   
   public IntegrationPoint3d (int nnodes) {
      this(nnodes, 1);
   }
   
   public IntegrationPoint3d (int nnodes, int npvals) {
      init(nnodes, npvals);
      setElementClass (ElementClass.VOLUMETRIC); // XXX default volumetric
   }

   protected void setElementClass (ElementClass elemClass) {
      myElemClass = elemClass;
   }
   
   public ElementClass getElementClass() {
      return myElemClass;
   }
   
   /**
    * Returns the number of this integration point. This will be
    * in the range 0 to numi-1, where numi is the number of
    * integration points for the associated elements.
    */
   public int getNumber() {
      return myNum;
   }

   /**
    * Used internally by the system to set the number for this integration
    * point.
    */
   public void setNumber (int num) {
      myNum = num;
   }    

   /** 
    * Create an integration point for a given element at a specific set of
    * natural coordinates.
    *
    * @param elem element to create the integration point for
    * @param s0 first coordinate value
    * @param s1 second coordinate value
    * @param s2 third coordinate value
    * @param w weight 
    * @return new integration point
    */
   public static IntegrationPoint3d create (
      FemElement3dBase elem, double s0, double s1, double s2, double w) {

      int nnodes = elem.numNodes();
      int npvals = 0;
      if (elem instanceof FemElement3d) {
         npvals = ((FemElement3d)elem).numPressureVals();
      }
      
      Vector3d coords = new Vector3d();
      Vector3d dNds = new Vector3d();
      VectorNd shapeWeights = new VectorNd(nnodes);
      VectorNd pressureWeights = new VectorNd(npvals);

      IntegrationPoint3d pnt =
	    new IntegrationPoint3d (nnodes, npvals, s0, s1, s2, w);
      coords.set (s0, s1, s2);
      for (int i=0; i<nnodes; i++) {
	 shapeWeights.set (i, elem.getN (i, coords));
	 elem.getdNds (dNds, i, coords);
	 pnt.setShapeGrad (i, dNds);
      }
      pnt.setShapeWeights (shapeWeights);
      pnt.setElementClass (elem.getElementClass());
      pnt.myElem = elem;
      if (npvals > 0) {
         for (int i=0; i<npvals; i++) {
            pressureWeights.set (i, ((FemElement3d)elem).getH (i, coords));
         }
         pnt.setPressureWeights (pressureWeights);
      }
      return pnt;
   }

   public void setCoords (double s0, double s1, double s2) {
      coords.set (s0, s1, s2);
   }

   public Vector3d getCoords () {
      return coords;
   }

   public double getWeight() {
      return myWeight;
   }

   public void setWeight (double w) {
      myWeight = w;
   }

   public void setShapeWeights (VectorNd vals) {
      N.set (vals);
   }

   public VectorNd getShapeWeights () {
      return N;
   }

   public void setShapeGrad (int i, Vector3d dNds) {
      GNs[i].set (dNds);
   }

   public void setPressureWeights (VectorNd vals) {
      H.set (vals);
   }

   public VectorNd getPressureWeights () {
      return H;
   }

   /**
    * Computes the current Jacobian at this integration point.
    * 
    * @param J returns the Jacobian
    * @param nodes FEM nodes, used to obtain the element node positions
    */
   public void computeJacobian (Matrix3d J, FemNode3d[] nodes) {
      computeJacobian (J, nodes, myElemClass);
   }

   public void computeJacobian (
      Matrix3d J, FemNode3d[] nodes, ElementClass type) {
      
      J.setZero();
      switch (type) {
         case VOLUMETRIC: {
            for (int i=0; i<nodes.length; i++) {
               Vector3d pos = nodes[i].getLocalPosition();
               Vector3d dNds = GNs[i];
               J.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
            }      
            break;
         }
         case SHELL: {
            Vector3d d = new Vector3d();
            Vector3d v = new Vector3d();

            double st = -0.5*(1-getCoords().z);
            for (int i=0; i<nodes.length; i++) {
               FemNode3d node = nodes[i];
               d.sub(node.getLocalPosition(), node.getBackPosition());
               v.scaledAdd (st, d, node.getLocalPosition());
               
               double s0 = GNs[i].x;
               double s1 = GNs[i].y;
               double s2 = N.get(i)*0.5;
               
               J.m00 += s0*v.x; J.m01 += s1*v.x; J.m02 += s2*d.x;
               J.m10 += s0*v.y; J.m11 += s1*v.y; J.m12 += s2*d.y;
               J.m20 += s0*v.z; J.m21 += s1*v.z; J.m22 += s2*d.z;
            }            
            break;
         }
         case MEMBRANE: {
            Vector3d jc0 = new Vector3d();
            Vector3d jc1 = new Vector3d();
            Vector3d jc2 = new Vector3d();
            for (int i=0; i<nodes.length; i++) {
               Vector3d pos = nodes[i].getLocalPosition();
               jc0.scaledAdd (GNs[i].x, pos);
               jc1.scaledAdd (GNs[i].y, pos);
            }            
            jc2.cross (jc0, jc1);
            jc2.normalize();

            J.m00 = jc0.x; J.m01 = jc1.x; J.m02 = jc2.x; 
            J.m10 = jc0.y; J.m11 = jc1.y; J.m12 = jc2.y; 
            J.m20 = jc0.z; J.m21 = jc1.z; J.m22 = jc2.z; 
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Element type " + type + " not supported");
         }
      }
   }

   public void computeJacobian (
      Matrix3d J, FemNode3d[] nodes, ElementClass type, CoordType ctype) {
      
      J.setZero();
      switch (type) {
         case VOLUMETRIC: {
            for (int i=0; i<nodes.length; i++) {
               J.addOuterProduct (nodes[i].getLocalCoordinates (ctype), GNs[i]);
            }      
            break;
         }
         case SHELL: {
            Vector3d d = new Vector3d();
            Vector3d v = new Vector3d();

            double st = -0.5*(1-getCoords().z);
            for (int i=0; i<nodes.length; i++) {
               Point3d pos = nodes[i].getLocalCoordinates (ctype);
               Point3d backPos = nodes[i].getBackCoordinates (ctype);
               d.sub (pos, backPos);
               v.scaledAdd (st, d, pos);
               
               double s0 = GNs[i].x;
               double s1 = GNs[i].y;
               double s2 = N.get(i)*0.5;
               
               J.m00 += s0*v.x; J.m01 += s1*v.x; J.m02 += s2*d.x;
               J.m10 += s0*v.y; J.m11 += s1*v.y; J.m12 += s2*d.y;
               J.m20 += s0*v.z; J.m21 += s1*v.z; J.m22 += s2*d.z;
            }            
            break;
         }
         case MEMBRANE: {
            Vector3d jc0 = new Vector3d();
            Vector3d jc1 = new Vector3d();
            Vector3d jc2 = new Vector3d();
            for (int i=0; i<nodes.length; i++) {
               Point3d pos = nodes[i].getLocalCoordinates (ctype);
               jc0.scaledAdd (GNs[i].x, pos);
               jc1.scaledAdd (GNs[i].y, pos);
            }            
            jc2.cross (jc0, jc1);
            jc2.normalize();

            J.m00 = jc0.x; J.m01 = jc1.x; J.m02 = jc2.x; 
            J.m10 = jc0.y; J.m11 = jc1.y; J.m12 = jc2.y; 
            J.m20 = jc0.z; J.m21 = jc1.z; J.m22 = jc2.z; 
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Element type " + type + " not supported");
         }
      }
   }

   public void computeRestJacobian (Matrix3d J0, FemNode3d[] nodes) {
      //computeRestJacobian (J0, nodes, myElemType);
      computeJacobian (J0, nodes, myElemClass, CoordType.REST);
   }

//   public void computeRestJacobian (
//      Matrix3d J0, FemNode3d[] nodes, ElementClass type) {
//      J0.setZero();
//      switch (type) {
//         case VOLUMETRIC: {
//            for (int i=0; i<nodes.length; i++) {
//               Vector3d pos = nodes[i].getLocalRestPosition();
//               Vector3d dNds = GNs[i];
//               J0.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
//            }      
//            break;
//         }
//         case SHELL: {
//            Vector3d d = new Vector3d();
//            Vector3d v = new Vector3d();
//
//            double st = -0.5*(1-getCoords().z);
//            for (int i=0; i<nodes.length; i++) {
//               FemNode3d node = nodes[i];
//               d.sub(node.getRestPosition(), node.getBackRestPosition());
//               v.scaledAdd (st, d, node.getRestPosition());
//               
//               double s0 = GNs[i].x;
//               double s1 = GNs[i].y;
//               double s2 = N.get(i)*0.5;
//               
//               J0.m00 += s0*v.x; J0.m01 += s1*v.x; J0.m02 += s2*d.x;
//               J0.m10 += s0*v.y; J0.m11 += s1*v.y; J0.m12 += s2*d.y;
//               J0.m20 += s0*v.z; J0.m21 += s1*v.z; J0.m22 += s2*d.z;
//            }            
//            break;
//         }
//         case MEMBRANE: {
//            Vector3d jc0 = new Vector3d();
//            Vector3d jc1 = new Vector3d();
//            Vector3d jc2 = new Vector3d();
//            for (int i=0; i<nodes.length; i++) {
//               Vector3d pos = nodes[i].getLocalRestPosition();
//               jc0.scaledAdd (GNs[i].x, pos);
//               jc1.scaledAdd (GNs[i].y, pos);
//            }            
//            jc2.cross (jc0, jc1);
//            jc2.normalize();
//
//            J0.m00 = jc0.x; J0.m01 = jc1.x; J0.m02 = jc2.x; 
//            J0.m10 = jc0.y; J0.m11 = jc1.y; J0.m12 = jc2.y; 
//            J0.m20 = jc0.z; J0.m21 = jc1.z; J0.m22 = jc2.z; 
//            break;
//         }
//         default: {
//            throw new UnsupportedOperationException (
//               "Element type " + type + " not supported");
//         }
//      }
//   }

   public double computeInverseRestJacobian (Matrix3d invJ0, FemNode3d[] nodes) {
      Matrix3d J0 = new Matrix3d();
      computeRestJacobian (J0, nodes);
      return invJ0.fastInvert (J0);
   }

   /**
    * Computes the determinant of the current Jacobian at this 
    * integration point.
    * 
    * @param nodes FEM nodes, used to obtain the element node positions
    * @return determinant of the Jacobian
    */
   public double computeJacobianDeterminant (FemNode3d[] nodes) {
      Matrix3d J = new Matrix3d();
      computeJacobian (J, nodes);
      return J.determinant();
   }

//   /**
//    * Computes the current Jacobian and deformation gradient
//    * at this integration point.
//    * 
//    * @param J returns the Jacobian
//    * @param F returns the deformation gradient
//    * @param nodes FEM nodes, used to obtain the element node positions
//    * @param invJ0 inverse rest position Jacobian
//    * @return determinant of F
//    */
//   public double computeJacobianAndGradient (
//      Matrix3d J, Matrix3d F, FemNode3d[] nodes, Matrix3d invJ0) {
//      computeJacobian (J, nodes);
//      F.mul (J, invJ0);
//      return F.determinant();
//   }

   /**
    * Computes the current deformation gradient at this integration point.
    * 
    * @param F returns the deformation gradient
    * @param nodes FEM nodes, used to obtain the element node positions
    * @param invJ0 inverse rest position Jacobian
    * @return determinant of F
    */
   public double computeGradient (
      Matrix3d F, FemNode3d[] nodes, Matrix3d invJ0) {
      computeJacobian (F, nodes); // compute J in F
      F.mul (F, invJ0);
      return F.determinant();
   }

   /**
    * Computes the current inverse Jacobian at this integration point.
    * 
    * @param invJ returns the inverse Jacobian
    * @param nodes FEM nodes, used to obtain the element node positions
    * @return determinant of the Jacobian
    */
   public double computeInverseJacobian (Matrix3d invJ, FemNode3d[] nodes) {
      computeJacobian (invJ, nodes);
      return invJ.fastInvert(invJ);
   }
   
//   public void computeJacobianAndGradient (FemNode3d[] nodes, Matrix3d invJ0) {
//      myJ.setZero();
//      for (int i=0; i<nodes.length; i++) {
//         Vector3d pos = nodes[i].getLocalPosition();
//         Vector3d dNds = GNs[i];
//         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
//      }
//      F.mul (myJ, invJ0);
//      detF = F.determinant();
//   }

//   public void computeJacobianAndGradient (Point3d[] nodePos, Matrix3d invJ0) {
//      myJ.setZero();
//      for (int i=0; i<nodePos.length; i++) {
//         Vector3d pos = nodePos[i];
//         Vector3d dNds = GNs[i];
//         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
//      }
//      F.mul (myJ, invJ0);
//      detF = F.determinant();
//   }

//   public void computeJacobianAndGradient (
//      SolidDeformation def, FemNode3d[] nodes, Matrix3d invJ0) {
//      myJ.setZero();
//      for (int i=0; i<nodes.length; i++) {
//         Vector3d pos = nodes[i].getLocalPosition();
//         Vector3d dNds = GNs[i];
//         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
//      }
//      def.setF (myJ, invJ0);
//   }
//
   public void computeGradientForRender (
      Matrix3d Fmat, FemNode3d[] nodes, Matrix3d invJ0) {

      // first compute J in Fmat
      computeJacobian (Fmat, nodes, myElemClass, CoordType.RENDER);  
      // then F = J * inv(J0)
      Fmat.mul (invJ0);
   }      

   public void computePosition (Point3d pos, FemNode3d[] nodes) {
      double[] Nbuf = N.getBuffer();
      pos.setZero();
      switch (myElemClass) {
         case VOLUMETRIC: 
         case MEMBRANE: {
            for (int i=0; i<nodes.length; ++i) {
               pos.scaledAdd (Nbuf[i], nodes[i].getPosition());
            }  
            break;
         } 
         case SHELL: {     
            Vector3d tmp = new Vector3d();
            for (int i=0; i<nodes.length; ++i) {
               nodes[i].getDirector (tmp);
               tmp.scaledAdd (
                  0.5*(coords.z-1), tmp, nodes[i].getPosition());
               pos.scaledAdd (Nbuf[i], tmp);
            }
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Element class " + myElemClass + " not supported");
         }  
      }     
   }
   
   public void computePosition (Point3d pos, FemElement3dBase elem) {
      computePosition(pos, elem.getNodes());
   }

   public void computeRestPosition (Point3d pos, FemElement3dBase elem) {
      computeRestPosition(pos, elem.getNodes());
   }
   
   public void computeRestPosition (Point3d pos, FemNode3d[] nodes) {
      double[] Nbuf = N.getBuffer();
      pos.setZero();
      switch (myElemClass) {
         case VOLUMETRIC: 
         case MEMBRANE: {
            for (int i=0; i<nodes.length; ++i) {
               pos.scaledAdd (Nbuf[i], nodes[i].getRestPosition());
            }  
            break;
         } 
         case SHELL: {     
            Vector3d tmp = new Vector3d();
            for (int i=0; i<nodes.length; ++i) {
               nodes[i].getRestDirector (tmp);
               tmp.scaledAdd (
                  0.5*(coords.z-1), tmp, nodes[i].getRestPosition());
               pos.scaledAdd (Nbuf[i], tmp);
            }
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Element class " + myElemClass + " not supported");
         }  
      }     
   }

   public void computeCoordsForRender (float[] coords, FemNode3d[] nodes) {
      // can use the same code for both volume and shell elements
      // because for shells we ignore the director component
      double[] Nbuf = N.getBuffer();
      coords[0] = coords[1] = coords[2] = 0;
      for (int i=0; i<nodes.length; i++) {
         float[] npos = nodes[i].myRenderCoords;         
         float n = (float)Nbuf[i];
         coords[0] += n*npos[0];
         coords[1] += n*npos[1];
         coords[2] += n*npos[2];
      }
   }

//   public double computeInverseJacobian () {
//
//      double detJ = myInvJ.fastInvert (myJ);
//      return detJ;
//   }
//
//   public Matrix3d getInvJ() {
//      return myInvJ;
//   }
   
   /** 
    * Computes and returns the gradient dN/dx of the shape functions, given an
    * inverse Jacobian. The gradient is supplied as an array of 3-vectors, one
    * for each shape function, because this is more convenient for computation.
    *
    * @param invJ inverse Jacobian
    * @return shape function gradient (can be modifed).
    */
   public Vector3d[] computeShapeGradient (Matrix3d invJ) {
      Vector3d[] GNx = new Vector3d[myNumNodes];
      for (int i=0; i<myNumNodes; ++i) {
         GNx[i] = new Vector3d();
      }
      computeShapeGradient(invJ, GNx);
      return GNx;
   }
   
   /** 
    * Computes and returns the gradient dN/dx of the shape functions, given an
    * inverse Jacobian. The gradient is supplied as an array of 3-vectors, one
    * for each shape function, because this is more convenient for computation.
    *
    * @param invJ inverse Jacobian
    * @param out shape function gradient to populate
    */
   public void computeShapeGradient(Matrix3d invJ, Vector3d[] out) {
      for (int i=0; i<myNumNodes; i++) {
         out[i].set(GNs[i]);
         invJ.mulTranspose (out[i], out[i]);
      }
   }
   
   /** 
    * Updates and returns the gradient dN/dx of the shape functions, given an
    * inverse Jacobian. The gradient is supplied as an array of 3-vectors, one
    * for each shape function, because this is more convenient for computation.
    *
    * @param invJ inverse Jacobian
    * @return shape function gradient (must not be modifed).
    */
   public Vector3d[] updateShapeGradient (Matrix3d invJ) {
      computeShapeGradient(invJ, GNx);
      return GNx;
   }

   /** 
    * Returns the gradient dN/dx of the shape functions. This is supplied as an
    * array of 3-vectors, one for each shape function, because this is more
    * convenient for computation.
    *
    * @return shape function gradient (must not be modifed).
    */
   public Vector3d[] getShapeGradient() {
      return GNx;
   }

//   public Matrix3d getF() {
//      return F;
//   }
//
//   public void setF(Matrix3d F) {
//      this.F.set (F);
//      //detF = F.determinant();
//   }

//   public double getAveragePressure() {
//      return avgp;
//   }

//   public double getDetF() {
//      return detF;
//   }

//   public Matrix3d getJ() {
//      return myJ;
//   }

//   public SymmetricMatrix3d getStress() {
//      return sigma;
//   }
//
//   public void setStress (SymmetricMatrix3d sig) {
//      sigma.set (sig);
//   }

//   public void computeRightCauchyGreen (SymmetricMatrix3d C) {
//      C.mulTransposeLeft (F);
//   }
//
//   public void computeLeftCauchyGreen (SymmetricMatrix3d B) {
//      B.mulTransposeRight (F);
//   }
//
//   public void computeDevRightCauchyGreen (SymmetricMatrix3d CD) {
//      CD.mulTransposeLeft (F);
//      CD.scale (Math.pow(detF, -2.0/3.0));
//   }
//
//   public void computeDevLeftCauchyGreen (SymmetricMatrix3d BD) {
//      BD.mulTransposeRight (F);
//      BD.scale (Math.pow(detF, -2.0/3.0));
//   }

   public Vector3d[] getGNs() {
      return GNs;
   }
   
//   public void setAveragePressure(double p) {
//      avgp = p;
//   }
   
}
