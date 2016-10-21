/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.util.DataBuffer;
import artisynth.core.materials.ViscoelasticState;

/**
 * This class stores element-specific information related to each integration
 * point within a 3D element.
 */
public class IntegrationData3d {

   Matrix3d myInvJ0;
   double myDetJ0;
   double myDv; // current partial volume at the quadrature point
   // optional coordinate frame information (for anisotropic materials) 
   protected Matrix3d myFrame = null;
   // previous stress and H values, used for computing viscoelasticity:
//   public SymmetricMatrix3d mySigmaPrev; 
//   public double[] myHPrev;
   protected ViscoelasticState myViscoState;

   private void init() {
      myInvJ0 = new Matrix3d(Matrix3d.IDENTITY);
      myDetJ0  = 1;
   }

   public ViscoelasticState getViscoState() {
      return myViscoState;
   }
   
   public void setViscoState (ViscoelasticState state) {
      myViscoState = state;
   }
   
   public IntegrationData3d () {
      init();
   }

   /** 
    * Returns the inverse rest Jacobian.
    * 
    * @return inverse rest Jacobian (must not be modified)
    */
   public Matrix3d getInvJ0() {
      return myInvJ0;
   }

   public double getDetJ0 () {
      return myDetJ0;
   }
   
   public double getDv() {
	   return myDv;
   }
   
   public void setDv(double dv) {
	   myDv = dv;
   }

   public Matrix3d getFrame() {
      return myFrame;
   }
   
   public void setFrame(Matrix3dBase frame) {
      myFrame = new Matrix3d(frame);
   }
   
   public double setRestJacobian(Matrix3d J0) {
      myDetJ0 = myInvJ0.fastInvert(J0);
      return myDetJ0;
   }
   
   public double setRestInverseJacobian(Matrix3d invJ0) {
      myInvJ0.set(invJ0);
      myDetJ0 = 1.0/invJ0.determinant();
      return myDetJ0;
   }
   
   public void setRestInverseJacobian(Matrix3d invJ0, double detJ0) {
      myInvJ0.set(invJ0);
      myDetJ0 = detJ0;
   }
   
   public static double computeRestJacobian (
      Matrix3d invJ0, Vector3d[] GNs, FemNode3d[] nodes) {
      
      Matrix3d J0 = new Matrix3d();
      J0.setZero();
      for (int i=0; i<nodes.length; i++) {
         Vector3d pos = nodes[i].getLocalRestPosition();
         Vector3d dNds = GNs[i];
         J0.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      return invJ0.fastInvert (J0);
   }
   
   public void computeRestJacobian (Vector3d[] GNs, FemNode3d[] nodes) {
      myDetJ0 = computeRestJacobian (myInvJ0, GNs, nodes);
      if (myDetJ0 <= 0) {
         System.out.println ("Warning: inverted rest element, det="+myDetJ0);
      }
   }

   /** 
    * Gets the number of integers required to store structure data.
    */
   public int getStateStructureSize () {
      //return 2;
      return 1;
   }
    
   /** 
    * Gets the number of doubles required to store state data.
    */
   public int getStateSize() {
      if (myViscoState != null) {
         return myViscoState.getStateSize();
      }
      else {
         return 0;
      }
   }

   /** 
    * Stores the information corresponding to zero state
    */
   public void getZeroState (DataBuffer data) {
      data.zput (0);
   }
   
   /** 
    * Stores the state structure in a DataBuffer.
    */  
   public void getState (DataBuffer data) {
      int size;
      if (myViscoState != null) {
         size = myViscoState.getStateSize();
      }
      else {
         size = 0;
      }
      data.zput (size);
      if (size > 0) {
         myViscoState.getState (data);
      }
   }
   
   /** 
    * Sets the state data from a DataBuffer.
    */
   public void setState (DataBuffer data) {
      int size = data.zget();
      if (size == 0) {
         if (myViscoState != null) {
            myViscoState = null;
         }
      }
      else {
         if (myViscoState == null ||
             (myViscoState != null && myViscoState.getStateSize() != size)) {
            // Hmmm - incompatible for some reason. Just clear the
            // state and ignore the remaining state input 
            myViscoState = null;
            data.dskip (size);
         }
         else {
            myViscoState.setState (data);
         }
      }
   }

   public void clearState() {
      myViscoState = null;
   }

   public void setFrame(Matrix3d myFrame) {
      this.myFrame = myFrame;
   }

}
