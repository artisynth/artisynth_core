/**
 * Copyright (c) 2020, by the Authors: Fabien Péan
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import maspack.matrix.ImproperSizeException;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.render.PointLineRenderProps;
import maspack.render.Renderer;

/**
 * Constrain two dynamic components to remain at a fixed distance from one another
 */
public class DistanceConstraint extends ConstrainerBase {

   public static double DEFAULT_COMPLIANCE = 0;
   public static double DEFAULT_DAMPING = 0;
   
   DynamicComponent myPointA;
   DynamicComponent myPointB;
   MatrixBlock[] myBlks = new MatrixBlock[2];
   double myDesiredDistance = 0;
   double myLambda = 0;
   double myCompliance = 0;
   double myDamping = 0;
   Vector3d myForce = new Vector3d();
   
   static PropertyList myProps = new PropertyList (DistanceConstraint.class, ConstrainerBase.class);
   static {
      myProps.add("renderProps", "render props", new PointLineRenderProps ());
      myProps.add("desiredDistance", "desired distance between components", 0, "[0,inf]");
      myProps.add("compliance", "compliance (inverse of stiffness)", 0, "[0,inf]");
      myProps.add("damping", "damping", 0, "[0,inf]");
      myProps.addReadOnly("components", "names of the 2 connected dynamic components");
      myProps.addReadOnly("distance", "current distance between components");
      myProps.addReadOnly("bilateralForceInA", "bilateral constraint force as seen in body A");
   }
   
   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public double getDesiredDistance() {
      return myDesiredDistance;
   }
   
   public void setDesiredDistance(double d) {
      myDesiredDistance = d;
   }
   
   public double getCompliance() {
      return myCompliance;
   }
   
   public void setCompliance(double c) {
      myCompliance = c;
   }
   
   public double getDamping() {
      return myDamping;
   }
   
   public void setDamping(double d) {
      myDamping = d;
   }
   
   public String getComponents() {
      return myPointA.getName()+" "+myPointB.getName();
   }
   
   public double getDistance() {
      return getDistanceFromPoints();
   }
   
   public Vector3d getBilateralForceInA() {
      return myForce;
   }

   public DistanceConstraint() {
   }

   public DistanceConstraint(DynamicComponent... comps) {
      setPoints(comps);
      setDesiredDistanceFromPoints();
   }
   
   public void setPoints(DynamicComponent... comps) {
      if(comps.length!=2)
         throw new ImproperSizeException("DistanceConstraint requires exactly 2 components");
      myPointA = comps[0];
      myPointB = comps[1];
      myBlks[0] = new MatrixNdBlock(myPointA.getVelStateSize (),1);
      myBlks[1] = new MatrixNdBlock(myPointB.getVelStateSize (),1);
   }
   
   public DynamicComponent[] getPoints() {
      return new DynamicComponent[] {myPointA, myPointB};
   }
   
   public double setDesiredDistanceFromPoints() {
      myDesiredDistance = getDistanceFromPoints();
      return myDesiredDistance;
   }

   public double getDistanceFromPoints() {
      double[] xa = new double[myPointA.getPosStateSize()];
      double[] xb = new double[myPointB.getPosStateSize()];
      myPointA.getPosState (xa, 0);
      myPointB.getPosState (xb, 0);
      return Math.sqrt((xa[0]-xb[0])*(xa[0]-xb[0])+(xa[1]-xb[1])*(xa[1]-xb[1])+(xa[2]-xb[2])*(xa[2]-xb[2]));
   }
   
   public Vector3d getDirectionFromPoints() {
      double[] xa = new double[myPointA.getPosStateSize()];
      double[] xb = new double[myPointB.getPosStateSize()];
      myPointA.getPosState (xa, 0);
      myPointB.getPosState (xb, 0);
      return new Vector3d ((xa[0]-xb[0]), (xa[1]-xb[1]), (xa[2]-xb[2])).normalize();
   }

   @Override
   public void getBilateralSizes(VectorNi sizes) {
      if (myPointA.getSolveIndex() != -1 || myPointB.getSolveIndex() != -1) {
         sizes.append(1);
         return;
      }
   }

   @Override
   public int addBilateralConstraints(
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      Vector3d direction = getDirectionFromPoints();
      VectorNd v = new VectorNd (direction);
      v.setSize (myPointA.getVelStateSize ());
      myBlks[0].set(v);
      v.negate ();
      v.setSize (myPointB.getVelStateSize ());
      myBlks[1].set(v);
      
      int idx = 0;
      int bj = GT.numBlockCols();
      {
         int bi = myPointA.getSolveIndex(); 
         if (bi != -1) {
            myBlks[idx].setBlockRow(bi);
            GT.addBlock(bi, bj, myBlks[idx]);
         }
         idx++;
      }
      {
         int bi = myPointB.getSolveIndex(); 
         if (bi != -1) {
            myBlks[idx].setBlockRow(bi);
            GT.addBlock(bi, bj, myBlks[idx]);
         }
         idx++;
      }
      if (myPointA.getSolveIndex() != -1 || myPointB.getSolveIndex() != -1)
      {
         if (dg != null) {
            dg.set(numb, 0);
         }
         numb++;
      }
      return numb;
   }

   @Override
   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {
      if (myPointA.getSolveIndex() == -1 && myPointB.getSolveIndex() == -1)
         return idx;
      
      double distance = getDistanceFromPoints ();
      ConstraintInfo gi = ginfo[idx++];
      gi.dist = (distance-myDesiredDistance);
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      gi.force = 0;
      return idx;
   }

   @Override
   public int setBilateralForces(VectorNd lam, double h, int idx) {
      if (myPointA.getSolveIndex() != -1 || myPointB.getSolveIndex() != -1) {
         myLambda = lam.get(idx++);
         myForce.set(getDirectionFromPoints());
         myForce.scale(myLambda/h);
      }
      return idx;
   }

   @Override
   public int getBilateralForces(VectorNd lam, int idx) {
      if (myPointA.getSolveIndex() != -1 || myPointB.getSolveIndex() != -1) {
         lam.set(idx++, myLambda);
      }
      return idx;
   }

   @Override
   public void zeroForces() {
      myLambda = 0;
   }

   @Override
   public double updateConstraints(double t, int flags) {
      return 0;
   }

   public void getConstrainedComponents (List<DynamicComponent> list) {
      list.add (myPointA);
      list.add (myPointB);
   }
   
   Point3d getPoint(DynamicComponent c) {
      double[] p = new double[c.getPosStateSize()];
      c.getPosState (p, 0);
      return new Point3d (p[0],p[1],p[2]);
   }
   
   @Override
   public void render (Renderer renderer, int flags) {
      if (myRenderProps.isVisible ()) {
         myRenderProps.setLineRadius(myDesiredDistance/20);
         
         Point3d p0 = getPoint (myPointA);
         Point3d p1 = getPoint (myPointB);
         
         float[] coords0 = new float[] { (float)p0.x, (float)p0.y, (float)p0.z };
         float[] coords1 = new float[] { (float)p1.x, (float)p1.y, (float)p1.z };
   
         renderer.drawLine (myRenderProps, coords0, coords1,
                            /*color=*/null, /*capped=*/true, isSelected());
         renderer.drawPoint (myRenderProps, coords0, isSelected ());
         renderer.drawPoint (myRenderProps, coords1, isSelected ());
      }
   }
}
