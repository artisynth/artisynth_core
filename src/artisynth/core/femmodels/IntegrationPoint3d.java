/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import artisynth.core.materials.SolidDeformation;

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

   protected Matrix3d F;
   protected double detF;
   protected double avgJ;
   protected double avgp;

   protected SymmetricMatrix3d sigma;

   protected int myNumNodes;          // static
   protected Vector3d coords;         // static
   protected VectorNd N;              // static
   protected VectorNd H;
   protected Vector3d GNs[];          // static

   protected Matrix3d myJ;            // transient
   protected Matrix3d myInvJ;         // transient

   protected Vector3d[] GNx = null;   // transient

   protected double myWeight = 1;
   protected int myNum = -1;

   protected void init(int nnodes, int npvals) {
      myJ = new Matrix3d();
      myInvJ = new Matrix3d();

      F = new Matrix3d();
      sigma = new SymmetricMatrix3d();

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
   }

   public IntegrationPoint3d (int nnodes) {
      init(nnodes, 1);
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
   public static IntegrationPoint3d create (FemElement3d elem,
	 double s0, double s1, double s2, double w) {

      int nnodes = elem.numNodes();
      int npvals = elem.numPressureVals();
      
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
      for (int i=0; i<npvals; i++) {
         pressureWeights.set (i, elem.getH (i, coords));
      }
      pnt.setShapeWeights (shapeWeights);
      pnt.setPressureWeights (pressureWeights);
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

   public void computeJacobian (FemNode3d[] nodes) {
      myJ.setZero();
      for (int i=0; i<nodes.length; i++) {
         Vector3d pos = nodes[i].getLocalPosition();
         Vector3d dNds = GNs[i];
         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
   }

   public void computeJacobianAndGradient (FemNode3d[] nodes, Matrix3d invJ0) {
      myJ.setZero();
      for (int i=0; i<nodes.length; i++) {
         Vector3d pos = nodes[i].getLocalPosition();
         Vector3d dNds = GNs[i];
         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      F.mul (myJ, invJ0);
      detF = F.determinant();
   }

   public void computeJacobianAndGradient (Point3d[] nodePos, Matrix3d invJ0) {
      myJ.setZero();
      for (int i=0; i<nodePos.length; i++) {
         Vector3d pos = nodePos[i];
         Vector3d dNds = GNs[i];
         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      F.mul (myJ, invJ0);
      detF = F.determinant();
   }

   public void computeJacobianAndGradient (
      SolidDeformation def, FemNode3d[] nodes, Matrix3d invJ0) {
      myJ.setZero();
      for (int i=0; i<nodes.length; i++) {
         Vector3d pos = nodes[i].getLocalPosition();
         Vector3d dNds = GNs[i];
         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      def.setF (myJ, invJ0);
   }

   public void computeGradientForRender (
      Matrix3d Fmat, FemNode3d[] nodes, Matrix3d invJ0) {

      // compute J in Fmat
      Fmat.setZero();
      for (int i=0; i<nodes.length; i++) {
         float[] npos = nodes[i].myRenderCoords;
         Vector3d dNds = GNs[i];
         Fmat.addOuterProduct (npos[0], npos[1], npos[2], dNds.x, dNds.y, dNds.z);
      }
      // now F = J * inv(J0)
      Fmat.mul (invJ0);
   }      

   /**
    * Compute size of an element's render coordinates along a specified
    * direction.
    */
   public double computeDirectedSizeForRender (Vector3d dir, FemNode3d[] nodes) {

      double max = -Double.MAX_VALUE;
      double min = Double.MAX_VALUE;
      for (int i=0; i<nodes.length; i++) {
         float[] npos = nodes[i].myRenderCoords;
         double l = npos[0]*dir.x + npos[1]*dir.y + npos[2]*dir.z;
         if (l > max) {
            max = l;
         }
         if (l < min) {
            min = l;
         }
      }
      return max-min;
   }

   public void computePosition (Point3d pos, FemNode3d[] nodes) {
      double[] Nbuf = N.getBuffer();
      pos.setZero();
      for (int i=0; i<nodes.length; i++) {
         pos.scaledAdd (Nbuf[i], nodes[i].getPosition());
      }
   }
   
   public void computePosition (Point3d pos, FemElement3d elem) {
      computePosition(pos, elem.getNodes());
   }

   public void computeRestPosition (Point3d pos, FemElement3d elem) {
      computeRestPosition(pos, elem.getNodes());
   }
   
   public void computeRestPosition (Point3d pos, FemNode3d[] nodes) {
      double[] Nbuf = N.getBuffer();
      pos.setZero();
      for (int i=0; i<nodes.length; i++) {
         pos.scaledAdd (Nbuf[i], nodes[i].getRestPosition());
      }
   }

   public void computeCoordsForRender (float[] coords, FemNode3d[] nodes) {
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

   public double computeInverseJacobian () {

      double detJ = myInvJ.fastInvert (myJ);
      return detJ;
   }

   public Matrix3d getInvJ() {
      return myInvJ;
   }
   
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

   public Matrix3d getF() {
      return F;
   }

   public void setF(Matrix3d F) {
      this.F.set (F);
      detF = F.determinant();
   }

   public double getAverageJ() {
      return avgJ;
   }

   public double getAveragePressure() {
      return avgp;
   }

   public double getDetF() {
      return detF;
   }

   public Matrix3d getJ() {
      return myJ;
   }

   public SymmetricMatrix3d getStress() {
      return sigma;
   }

   public void setStress (SymmetricMatrix3d sig) {
      sigma.set (sig);
   }

   public void computeRightCauchyGreen (SymmetricMatrix3d C) {
      C.mulTransposeLeft (F);
   }

   public void computeLeftCauchyGreen (SymmetricMatrix3d B) {
      B.mulTransposeRight (F);
   }

   public void computeDevRightCauchyGreen (SymmetricMatrix3d CD) {
      CD.mulTransposeLeft (F);
      CD.scale (Math.pow(detF, -2.0/3.0));
   }

   public void computeDevLeftCauchyGreen (SymmetricMatrix3d BD) {
      BD.mulTransposeRight (F);
      BD.scale (Math.pow(detF, -2.0/3.0));
   }

   public Vector3d[] getGNs() {
      return GNs;
   }
   
   public void setAveragePressure(double p) {
      avgp = p;
   }
   
}
