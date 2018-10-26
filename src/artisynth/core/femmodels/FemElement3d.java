/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;
import java.util.*;

import maspack.matrix.*;
import maspack.numerics.GoldenSectionSearch;
import maspack.util.*;
import maspack.function.Function1x1;
import maspack.properties.*;
import maspack.util.InternalErrorException;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.render.RenderProps;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.IncompressibleMaterial;
import artisynth.core.mechmodels.DynamicAttachmentWorker;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.FrameAttachable;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.*;

public abstract class FemElement3d extends FemElement3dBase
   implements PointAttachable, FrameAttachable {
   
   // the warping point is an integration point at the center of the element,
   // used for corotated linear behavior and other things
   protected IntegrationData3d myWarpingData;
   protected StiffnessWarper3d myWarper = null;
  
    // per-element integration point data
   protected IntegrationData3d[] myIntegrationData;
   protected boolean myIntegrationDataValid = false;

   protected double[] myRestVolumes;
   protected double[] myVolumes;
   protected double[] myLagrangePressures;

   protected MatrixBlock[] myIncompressConstraints = null;
   private int myIncompressIdx = -1;

   private static Matrix1x1 myPressureWeightMatrix = null;

   private static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   protected PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   // Auxiliary Materials are mainly used for implementing muscle fibres
   protected ArrayList<AuxiliaryMaterial> myAuxMaterials = null;

   public static PropertyList myProps =
      new PropertyList (FemElement3d.class, FemElement.class);

   static {
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemElement3d() {
      int npvals = numPressureVals();
      myLagrangePressures = new double[npvals];
      myRestVolumes = new double[npvals];
      myVolumes = new double[npvals];
   }
   
   public ElementClass getElementClass() {
      return ElementClass.VOLUMETRIC;
   }

   /* --- Element Widget Size --- */

   public void setElementWidgetSize (double size) {
      myElementWidgetSize = size;
      myElementWidgetSizeMode = 
         PropertyUtils.propagateValue (
            this, "elementWidgetSize",
            myElementWidgetSize, myElementWidgetSizeMode);
   }

   public double getElementWidgetSize () {
      return myElementWidgetSize;
   }

   public void setElementWidgetSizeMode (PropertyMode mode) {
      myElementWidgetSizeMode =
         PropertyUtils.setModeAndUpdate (
            this, "elementWidgetSize", myElementWidgetSizeMode, mode);
   }

   public PropertyMode getElementWidgetSizeMode() {
      return myElementWidgetSizeMode;
   }
   

   public abstract double[] getNodeCoords ();

   public void getNodeCoords (Vector3d coords, int nodeIdx) {
      if (nodeIdx < 0 || nodeIdx >= numNodes()) {
         throw new IllegalArgumentException (
            "Node index must be in the range [0,"+(numNodes()-1)+"]");
      }
      double[] c = getNodeCoords();
      coords.set (c[nodeIdx*3], c[nodeIdx*3+1], c[nodeIdx*3+2]);
   }

   /* --- Integration points and data --- */

   public abstract IntegrationPoint3d getWarpingPoint();

   public IntegrationData3d getWarpingData() {
      IntegrationData3d wdata = myWarpingData;
      if (wdata == null) {
         int numPnts = getIntegrationPoints().length;
         if (numPnts == 1) {
            // then integration and warping points/data are the same
            wdata = getIntegrationData()[0];
         }
         else {
            wdata = new IntegrationData3d();
            wdata.computeInverseRestJacobian (getWarpingPoint(), myNodes);
         }
         myWarpingData = wdata;
      }
      return wdata;
   }
   
   public abstract double[] getIntegrationCoords ();

   public abstract IntegrationPoint3d[] getIntegrationPoints();

   public static IntegrationPoint3d[] createIntegrationPoints (
      FemElement3d exampleElem, double[] cdata) {
      int numi = cdata.length/4;
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      if (cdata.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+cdata.length+", expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            exampleElem, cdata[k*4], cdata[k*4+1], cdata[k*4+2], cdata[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }
   
   protected IntegrationPoint3d[] createIntegrationPoints(double[] cdata) {
      return createIntegrationPoints(this, cdata);
   }
   
   protected IntegrationPoint3d[] createIntegrationPoints() {
      int numi = numIntegrationPoints();
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      double[] cdata = getIntegrationCoords();
      if (cdata.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+cdata.length+", expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            this, cdata[k*4], cdata[k*4+1], cdata[k*4+2], cdata[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }

   protected IntegrationData3d[] doGetIntegrationData() {
      IntegrationData3d[] idata = myIntegrationData;
      if (idata == null) {
         int numPnts = numIntegrationPoints();
         idata = new IntegrationData3d[numPnts];
         for (int i=0; i<numPnts; i++) {
            idata[i] = new IntegrationData3d();
         }
         myIntegrationData = idata;
      }
      return idata;
   }

   public IntegrationData3d[] getIntegrationData() {
      IntegrationData3d[] idata = doGetIntegrationData();
      if (!myIntegrationDataValid) {
         // compute rest Jacobians and such
         IntegrationPoint3d[] ipnts = getIntegrationPoints();
         for (int i=0; i<idata.length; i++) {
            idata[i].computeInverseRestJacobian (ipnts[i], myNodes);
         }
         myIntegrationDataValid = true;
      }
      return idata;
   }

   // /** 
   //  * Set reference frame information for this element. This can be used for
   //  * computing anisotropic material parameters. In principle, each integration
   //  * point can have its own frame information, but this method simply sets the
   //  * same frame information for all the integration points, storing it in each
   //  * IntegrationData structure. Setting <code>M</code> to <code>null</code>
   //  * removes the frame information.
   //  * 
   //  * @param M frame information (is copied by the method)
   //  */
   // public void setFrame (Matrix3dBase M) {
   //    Matrix3d F = null;
   //    if (M != null) {
   //       F = new Matrix3d (M);
   //    }
   //    IntegrationData3d[] idata = doGetIntegrationData();
   //    for (int i=0; i<idata.length; i++) {
   //       idata[i].myFrame = F;
   //    }
   // }

   // public Matrix3d getFrame() {
   //    IntegrationData3d[] idata = doGetIntegrationData();
   //    return idata[0].getFrame();
   // }

   public void invalidateRestData () {
      super.invalidateRestData();
      // will cause rest Jacobians to be recalculated
      myIntegrationDataValid = false;
      myWarpingData = null;
   }

   /* --- coordinates --- */
   
   /**
    * Calls {@link #getNaturalCoordinates(Vector3d,Point3d,int)}
    * with a default maximum number of interations.
    * 
    * @param coords
    * Outputs the natural coordinates, and supplies (on input) an initial
    * guess as to their position.
    * @param pnt
    * A given point (in world coords)
    * @return true if the calculation converged
    */
   public boolean getNaturalCoordinates (Vector3d coords, Point3d pnt) {
      return getNaturalCoordinates (coords, pnt, /*maxIters=*/1000) >= 0;
   }

   private void computeNaturalCoordsResidual (
      Vector3d res, Vector3d coords, Point3d pnt) {

      res.setZero();
      for (int k=0; k<numNodes(); k++) {
         res.scaledAdd (
            getN (k, coords), myNodes[k].getLocalPosition(), res);
      }
      res.sub (pnt);
   }
   
   /**
    * Given point p, get its natural coordinates with respect to this element.
    * Returns a positive number if the algorithm converges, or -1 if a maximum
    * number of iterations has been reached. Uses a modified Newton's method to solve the 
    * equations. The <code>coords</code> argument that returned the coordinates is
    * used, on input, to supply an initial guess of the coordinates.
    * Zero is generally a safe guess.
    * 
    * @param coords
    * Outputs the natural coordinates, and supplies (on input) an initial
    * guess as to their position.
    * @param pnt
    * A given point (in world coords)
    * @param maxIters
    * Maximum number of Newton iterations
    * @return the number of iterations required for convergence, or
    * -1 if the calculation did not converge.
    */
   public int getNaturalCoordinates (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         // if not inside, reset coords to 0
         coords.setZero();
      }

      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }

      Vector3d res = new Point3d();
      int i;

      double tol = RenderableUtils.getRadius (this) * 1e-12;
      computeNaturalCoordsResidual (res, coords, lpnt);
      double prn = res.norm();
      //System.out.println ("res=" + prn);
      if (prn < tol) {
         // already have the right answer
         return 0;
      }

      LUDecomposition LUD = new LUDecomposition();
      Vector3d prevCoords = new Vector3d();
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d del = new Point3d();

      /*
       * solve using Newton's method.
       */
      for (i = 0; i < maxIters; i++) {

         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         for (int k=0; k<numNodes(); k++) {
            getdNds (dNds, k, coords);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
         }
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         // solve Jacobian to obtain an update for the coords
         LUD.solve (del, res);

         // if del is very small assume we are close to the root. Assume
         // natural coordinates are generally around 1 in magnitude, so we can
         // use an absolute value for a tolerance threshold
         if (del.norm() < 1e-10) {
            //System.out.println ("1 res=" + res.norm());
            return i+1;
         }

         prevCoords.set (coords);         
         coords.sub (del);                              
         computeNaturalCoordsResidual (res, coords, lpnt);
         double rn = res.norm();
         //System.out.println ("res=" + rn);

         // If the residual norm is within tolerance, we have converged.
         if (rn < tol) {
            //System.out.println ("2 res=" + rn);
            return i+1;
         }
         if (rn > prn) {
            // it may be that "coords + del" is a worse solution.  Let's make
            // sure we go the correct way binary search suitable alpha in [0 1]

            double eps = 1e-12;
            // start by limiting del to a magnitude of 1
            if (del.norm() > 1) {
               del.normalize();  
            }
            // and keep cutting the step size in half until the residual starts
            // dropping again
            double alpha = 0.5;
            while (alpha > eps && rn > prn) {
               coords.scaledAdd (-alpha, del, prevCoords);
               computeNaturalCoordsResidual (res, coords, lpnt);
               rn = res.norm();
               alpha *= 0.5;
               //System.out.println ("  alpha=" + alpha + " rn=" + rn);
            }
            //System.out.println (" alpha=" + alpha + " rn=" + rn + " prn=" + prn);
            if (alpha < eps) {
               return -1;  // failed
            }
         }
         prn = rn;
      }
      return -1; // failed
   }
   
   private static class GSSResidual implements Function1x1 {
      private Point3d c0;
      private Point3d target;
      private Vector3d dir;
      private Point3d c;
      private Vector3d res;
      FemElement3d elem;
      
      public GSSResidual(FemElement3d elem, Point3d target) {
         this.elem = elem;
         this.c0 = new Point3d();
         this.dir = new Vector3d();
         this.target = new Point3d(target);
         this.c = new Point3d();
         this.res = new Vector3d();
      }
      
      public void set(Point3d coord, Vector3d dir) {
         this.c0.set(coord);
         this.dir.set(dir);
      }

      @Override
      public double eval(double x) {
         c.scaledAdd(x, dir, c0);
         elem.computeNaturalCoordsResidual(res, c, target);
         return res.normSquared();
      }
   }
   
   /**
    * Given point p, get its natural coordinates with respect to this element.
    * Returns a positive number if the algorithm converges, or -1 if a maximum
    * number of iterations has been reached. Uses a modified Newton's method to solve the 
    * equations. The <code>coords</code> argument that returned the coordinates is
    * used, on input, to supply an initial guess of the coordinates.
    * Zero is generally a safe guess.
    * 
    * @param coords
    * Outputs the natural coordinates, and supplies (on input) an initial
    * guess as to their position.
    * @param pnt
    * A given point (in world coords)
    * @param maxIters
    * Maximum number of Newton iterations
    * @return the number of iterations required for convergence, or
    * -1 if the calculation did not converge.
    */
   public int getNaturalCoordinatesGSS (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         // if not inside, reset coords to 0
         coords.setZero();
      }

      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }

      Vector3d res = new Point3d();
      int i;

      double tol = RenderableUtils.getRadius (this) * 1e-12;
      computeNaturalCoordsResidual (res, coords, lpnt);
      double prn = res.norm();
      //System.out.println ("res=" + prn);
      if (prn < tol) {
         // already have the right answer
         return 0;
      }

      LUDecomposition LUD = new LUDecomposition();
      Point3d prevCoords = new Point3d();
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d del = new Point3d();
      
      GSSResidual func = new GSSResidual(this, lpnt);

      /*
       * solve using Newton's method.
       */
      for (i = 0; i < maxIters; i++) {

         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         for (int k=0; k<numNodes(); k++) {
            getdNds (dNds, k, coords);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
         }
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         // solve Jacobian to obtain an update for the coords
         LUD.solve (del, res);
         del.negate();

         // if del is very small assume we are close to the root. Assume
         // natural coordinates are generally around 1 in magnitude, so we can
         // use an absolute value for a tolerance threshold
         if (del.norm() < 1e-10) {
            //System.out.println ("1 res=" + res.norm());
            return i+1;
         }

         prevCoords.set (coords);         
         coords.add (del);                              
         computeNaturalCoordsResidual (res, coords, lpnt);
         double rn = res.norm();
         //System.out.println ("res=" + rn);

         // If the residual norm is within tolerance, we have converged.
         if (rn < tol) {
            //System.out.println ("2 res=" + rn);
            return i+1;
         }
         
         // do a golden section search in range
         double eps = 1e-12;
         func.set(prevCoords, del);
         double alpha = GoldenSectionSearch.minimize(func, 0, 1, eps, 0.8*prn*prn);
         coords.scaledAdd(alpha, del, prevCoords);
         computeNaturalCoordsResidual(res, coords, lpnt);
         rn = res.norm();
         
         //System.out.println (" alpha=" + alpha + " rn=" + rn + " prn=" + prn);
         if (alpha < eps) {
            return -1;  // failed
         }
         
         prn = rn;
      }
      return -1; // failed
   }
   

   public int getNaturalCoordinatesStd (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         coords.setZero();
      }
      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }

      LUDecomposition LUD = new LUDecomposition();

      /*
       * solve using Newton's method: Need a good guess! Just guessed zero here.
       */
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d res = new Point3d();
      Vector3d del = new Point3d();
      Vector3d prevCoords = new Vector3d();
      Vector3d prevRes = new Point3d();

      int i;
      for (i = 0; i < maxIters; i++) {

         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         res.setZero();
         for (int k=0; k<numNodes(); k++) {
            getdNds (dNds, k, coords);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
            res.scaledAdd (
               getN (k, coords), myNodes[k].getLocalPosition(), res);
         }
         res.sub (lpnt);
         LUD.factor (dxds);
         
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         LUD.solve (del, res);
         // assume that natural coordinates are general around 1 in magnitude
         if (del.norm() < 1e-10) {
            // System.out.println ("* res=" + res.norm());
            return i+1;
         }
         
         prevCoords.set(coords);
         prevRes.set(res);
         
         // it may be that "coords + del" is a worse solution.  Let's make sure we
         // go the correct way
         // binary search suitable alpha in [0 1]
         double eps = 1e-12;
         double prn2 = prevRes.normSquared();
         double rn2 = prn2*2; // initialization
         double alpha = 1;
         del.normalize();  // if coords expected ~1, we don't want to stray too far
         
         while (alpha > eps && rn2 > prn2) {
            coords.scaledAdd (-alpha, del, prevCoords);
            res.setZero();
            for (int k=0; k<numNodes(); k++) {
               res.scaledAdd (
                  getN (k, coords), myNodes[k].getLocalPosition(), res);
            }
            res.sub (lpnt);
            
            rn2 = res.normSquared();
            alpha = alpha / 2;
         }         
         if (alpha < eps) {
            return -1;  // failed
         }
         //System.out.println ("res=" + res.norm() + " alpha=" + alpha);
      }
      //      System.out.println ("coords: " + coords.toString ("%4.3f"));
      return -1;
   }
   
   public boolean getNaturalCoordinatesOld (
      Vector3d coords, Point3d pnt, int maxIters) {

      if (!coordsAreInside(coords)) {
         coords.setZero();
      }

      // if FEM is frame-relative, transform to local coords
      Point3d lpnt = new Point3d(pnt);
      if (getGrandParent() instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)getGrandParent();
         if (fem.isFrameRelative()) {
            lpnt.inverseTransform (fem.getFrame().getPose());
         }
      }
      LUDecomposition LUD = new LUDecomposition();

      /*
       * solve using Newton's method: Need a good guess! Just guessed zero here.
       */
      Vector3d dNds = new Vector3d();
      Matrix3d dxds = new Matrix3d();
      Vector3d res = new Point3d();
      Vector3d del = new Point3d();

      int i;
      for (i = 0; i < maxIters; i++) {
//         double s1 = coords.x;
//         double s2 = coords.y;
//         double s3 = coords.z;

         // compute the Jacobian dx/ds for the current guess
         dxds.setZero();
         res.setZero();
         for (int k=0; k<numNodes(); k++) {
            getdNds (dNds, k, coords);
            dxds.addOuterProduct (myNodes[k].getLocalPosition(), dNds);
            res.scaledAdd (getN (k, coords), myNodes[k].getLocalPosition(), res);
         }
         res.sub (pnt);
         System.out.println ("res=" + res.norm());
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.err.println (
               "Warning: condition number for solving natural coordinates is "
               + cond);
         LUD.solve (del, res);
         // assume that natural coordinates are general around 1 in magnitude
         if (del.norm() < 1e-10) {
            break;
         }
         System.out.println ("iter=" + i + " del=" + del.norm());
         coords.sub (del);
      }
      //      System.out.println ("coords: " + coords.toString ("%4.3f"));

      // natural coordinates should all be between -1 and +1 iff the point is
      // inside it
      return true;
   }

   /* --- Stiffness warping --- */

   /**
    * Explicitly sets a stiffness warper
    * @param warper new stiffness warper to use
    */
   public void setStiffnessWarper(StiffnessWarper3d warper) {
      myWarper = warper;
      myWarpingStiffnessValidP = false;
   }
   
   /**
    * Retrieves the current stiffness warper.  The warper's
    * cached rest stiffness is updated if necessary
    * @param weight meta weighting to be applied to integration points
    * when updating the rest stiffness. Default value should be 1.0. 
    * @return stiffness warper
    */
   public StiffnessWarper3d getStiffnessWarper(double weight) {
      // don't allow invalid stiffness to leak
      if (!myWarpingStiffnessValidP) {
         updateWarpingStiffness(weight);
      }
      return myWarper;
   }
   
   protected StiffnessWarper3d createStiffnessWarper () {
      return new StiffnessWarper3d (this);
   }
   
   protected void updateWarpingStiffness(double weight) {
      FemMaterial mat = getEffectiveMaterial();
      if (myWarper == null){
         myWarper = createStiffnessWarper();
      } else {
         myWarper.initialize(this);
      }
      
      if (mat.isLinear()) {
         myWarper.addInitialStiffness (this, mat, weight);
      }
      for (AuxiliaryMaterial amat : getAuxiliaryMaterials()) {
         if (amat.isLinear()) {
            myWarper.addInitialStiffness(this, amat, weight);
         }
      }
      myWarpingStiffnessValidP = true;
   }

   /* --- Volume --- */

   /**
    * Default method to compute an element's volume and partial volumes.  Uses
    * quadrature. If the number of pressure values is 1, then there is only one
    * partial rest volume which is equal to the overall rest volume. The volume
    * at each quadrature point is also stored in the <code>dv</code> field of
    * the elements integration data, for possible future use.
    *
    * <p>The method should return the minimum Jacobian value found when
    * computing the volume for this element. A negative value indicates
    * element inversion.
    *
    * <p>Individual elements can override this with a more efficient 
    * method if needed.
    */
   public double computeVolumes() {
      int npvals = numPressureVals();
      
      double vol = 0;
      for (int k=0; k<npvals; k++) {
         myVolumes[k] = 0;
      }
      
      double minDetJ = Double.MAX_VALUE;

      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int i=0; i<ipnts.length; i++) {
         IntegrationPoint3d pt = ipnts[i];
         double detJ = pt.computeJacobianDeterminant (myNodes);
         //double detJ = pt.getJ().determinant();
         double dv = detJ*pt.getWeight();
         // normalize detJ to get true value relative to rest position
         detJ /= idata[i].getDetJ0();
         idata[i].setDv(dv);
         if (npvals > 1) {
            double[] H = pt.getPressureWeights().getBuffer();
            for (int k=0; k<npvals; k++) {
               myVolumes[k] += H[k]*dv;
            } 
         }
         if (detJ < minDetJ) {
            minDetJ = detJ;
         }
         vol += dv;
      }
      if (npvals == 1) {
         myVolumes[0] = vol;         
      }      
      myVolume = vol;
      return minDetJ;
   }

   /**
    * Volumes array for use with incompressibility 
    * @return current volumes
    */
   public double[] getVolumes() {
      return myVolumes;
   }

   /**
    * Default method to compute the element rest volume and partial volumes.
    * Uses quadrature. If the number of pressure values is 1, then there is
    * only one partial rest volume which is equal to the overall rest
    * volume. Individual elements can override this with a more efficient
    * method if needed.
    */
   public double computeRestVolumes() {
      int npvals = numPressureVals();

      double vol = 0;
      for (int k=0; k<npvals; k++) {
         myRestVolumes[k] = 0;
      }
      
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int i=0; i<ipnts.length; i++) {
         double dV = idata[i].getDetJ0()*ipnts[i].getWeight();
         if (npvals > 1) {
            double[] H = ipnts[i].getPressureWeights().getBuffer();
            for (int k=0; k<npvals; k++) {
               myRestVolumes[k] += H[k]*dV;
            }
         }
         vol += dV;
      }
      if (npvals == 1) {
         myRestVolumes[0] = vol;         
      }
      return vol;
   }
   
   /**
    * Volumes array for use with incompressibility 
    * @return rest volumes
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
   
   /* --- Edges and Faces --- */

   public abstract int[] getEdgeIndices();

   public abstract int[] getFaceIndices();

   public FemNode3d[][] triangulateFace (FaceNodes3d face) {
      FemNode3d[] nodes = face.getNodes();
      FemNode3d[][] triangles = new FemNode3d[nodes.length-2][3];
      for (int i=0; i<triangles.length; i++) {
         setTriangle (triangles[i], nodes[0], nodes[i+1], nodes[i+2]);
      }
      return triangles;
   }

   protected void setTriangle (
      FemNode3d[] tri, FemNode3d n0, FemNode3d n1, FemNode3d n2) {
      tri[0] = n0;
      tri[1] = n1;
      tri[2] = n2;
   }

   /**
    * Returns an array of FaceNodes3d describing a set of faces
    * associated with this element. If adjoining elements have matching
    * faces, then the elimination of repeated faces can be used to
    * generate an external mesh for the FEM. If the returned list has zero
    * length, then this method is not supported for the element in question.
    * 
    * @return list of faces for this element
    */
   public FaceNodes3d[] getFaces() {
      FaceNodes3d[] faces = new FaceNodes3d[getNumFaces()];
      int[] idxs = getFaceIndices();
      int k = 0;
      for (int i=0; i<faces.length; i++ ) {
         faces[i] = new FaceNodes3d(this, idxs[k++]);
         FemNode3d[] faceNodes = faces[i].getNodes();
         for (int j=0; j<faceNodes.length; j++) {
            faceNodes[j] = myNodes[idxs[k++]];
         }
      }
      return faces;
   }

   public int getNumEdges() {
      int num = 0;
      int[] idxs = getEdgeIndices();
      for (int i=0; i<idxs.length; i+=(idxs[i]+1)) {
         num++;
      }
      return num;
   }
   
   public int getNumFaces() {
      int num = 0;
      int[] idxs = getFaceIndices();
      for (int i=0; i<idxs.length; i+=(idxs[i]+1)) {
         num++;
      }
      return num;
   }

   public boolean hasEdge (FemNode3d n0, FemNode3d n1) {
      return false;
   }

   public boolean hasFace (FemNode3d n0, FemNode3d n1, FemNode3d n2) {
      return false;
   }

   public boolean hasFace (FemNode3d n0, FemNode3d n1,
                           FemNode3d n2, FemNode3d n3) {
      return false;
   }

   /* --- Extrpolation matrices --- */

   public abstract double[] getNodalExtrapolationMatrix ();

   /**
    * Tests whether or not a point is inside an element.  
    * @param pnt point to check if is inside
    * @return true if point is inside the element
    */
   public boolean isInside (Point3d pnt) {
      Vector3d coords = new Vector3d();
      if (!getNaturalCoordinates (coords, pnt)) {
         // if the calculation did not converge, assume we are outside
         return false;
      }
      return coordsAreInside (coords);
   }
   
   /** 
    * Returns true if the rest position for this element results in a negative
    * Jacobian determinant for at least one integration point. This method
    * should be used sparingly since it is computes this result from scratch.
    *
    * @return true if the rest position is inverted.
    */
   public boolean isInvertedAtRest() {
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      for (int i = 0; i < ipnts.length; i++) {
         IntegrationData3d idata = new IntegrationData3d();
         
         /*
          *  code snippet from IntegrationData3d.computeRestJacobian()
          *  -- there may be better way to check "invertedness"
          */
         Matrix3d J0 = new Matrix3d();
         J0.setZero();
         for (int j = 0; j < myNodes.length; j++) {
            Vector3d pos = myNodes[j].myRest;
            Vector3d dNds = ipnts[i].GNs[j];
            J0.addOuterProduct(pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
         }
         if (idata.getInvJ0().fastInvert(J0) <= 0) {
            return true;
         }
      }
      return false;
   }      



   /**
    * Compute position within element based on natural coordinates
    * @param pnt  populated position within element
    * @param coords natural coordinates
    */
   public void computePosition(Point3d pnt, Vector3d coords) {
      pnt.setZero();
      for (int i=0; i<numNodes(); ++i) {
         pnt.scaledAdd(getN(i, coords), myNodes[i].getPosition());
      }
   }

   public double computeCovariance (Matrix3d C) {
      double vol = 0;

      Point3d pnt = new Point3d();
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      for (int i=0; i<ipnts.length; i++) {
         IntegrationPoint3d pt = ipnts[i];
         // compute the current position of the integration point in pnt:
         pnt.setZero();
         for (int k=0; k<pt.N.size(); k++) {
            pnt.scaledAdd (pt.N.get(k), myNodes[k].getPosition());
         }
         // now get dv, the current volume at the integration point
         double detJ = pt.computeJacobianDeterminant (myNodes);
         double dv = detJ*pt.getWeight();
         C.addScaledOuterProduct (dv, pnt, pnt);
         vol += dv;
      }
      return vol;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (int i = 0; i < numNodes(); i++) {
         myNodes[i].getPosition().updateBounds (pmin, pmax);
      }
   }

   public void clearState() {
      IntegrationData3d[] idata = doGetIntegrationData();
      for (int i=0; i<idata.length; i++) {
         idata[i].clearState();
      }
   }

   /** 
    * Used to create edge nodes for quadratic elements.
    */
   protected static FemNode3d createEdgeNode (FemNode3d n0, FemNode3d n1) {
      Point3d px = new Point3d();
      px.add (n0.getPosition(), n1.getPosition());
      px.scale (0.5);
      return new FemNode3d (px);
   }   

   public void connectToHierarchy () {
      super.connectToHierarchy ();
      
      FemNode3d[] nodes = getNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].registerNodeNeighbor(nodes[j], /*shell=*/false);
         }
         nodes[i].addElementDependency(this);
      }
      setMass(0);

      myNbrs = new FemNodeNeighbor[numNodes()][numNodes()];
      for (int i=0; i<myNodes.length; i++) {
         FemNode3d node = myNodes[i];
         int cnt = 0;
         for (FemNodeNeighbor nbr : node.getNodeNeighbors()){
            int j = getLocalNodeIndex (nbr.myNode);
            if (j != -1) {
               myNbrs[i][j] = nbr;
               cnt++;
            }
         }
         if (cnt != myNodes.length) {
            System.out.println ("element class " + getClass());
            throw new InternalErrorException (
               "Node "+node.getNumber()+" has "+cnt+
               " local neighbors, expecting "+myNodes.length);
         }
      }
   }

   public void disconnectFromHierarchy () {
      myNbrs = null;

      FemNode3d[] nodes = getNodes();
      // double massPerNode = getMass()/numNodes();
      for (int i = 0; i < nodes.length; i++) {
         for (int j = 0; j < nodes.length; j++) {
            nodes[i].deregisterNodeNeighbor(nodes[j], /*shell=*/false);
         }
         // nodes[i].addMass(-massPerNode);
         nodes[i].invalidateMassIfNecessary ();  // signal dirty
         nodes[i].removeElementDependency(this);
      }

      super.disconnectFromHierarchy ();
   }

   public abstract boolean coordsAreInside (Vector3d coords);

   public boolean getMarkerCoordinates (
      VectorNd coords, Point3d pnt, boolean checkInside) {
      if (coords.size() < numNodes()) {
         throw new IllegalArgumentException (
            "coords size "+coords.size()+" != number of nodes "+numNodes());
      }
      Vector3d ncoords = new Vector3d();
      boolean converged = getNaturalCoordinates (ncoords, pnt);
      for (int i=0; i<numNodes(); i++) {
         coords.set (i, getN (i, ncoords));
      }
      if (!converged) {
         return false;
      }
      else if (checkInside) {
         return coordsAreInside (ncoords);
      }
      else {
         return true;
      }
   }
   
   public void computeJacobian(Vector3d s, Matrix3d J) {
      FemNode3d[] nodes = getNodes();
      J.setZero();
      Vector3d dNds = new Vector3d();
      for (int i=0; i<nodes.length; ++i) {
         getdNds(dNds, i, s);
         J.addOuterProduct(nodes[i].getLocalPosition(), dNds);
      }
   }
   
   public void computePressures (
      double[] pressures, IncompressibleMaterial imat) {

      int npvals = numPressureVals();
      IntegrationPoint3d[] ipnts = getIntegrationPoints();
      IntegrationData3d[] idata = getIntegrationData();
      for (int k=0; k<npvals; k++) {
         pressures[k] = 0;
      }
      for (int i=0; i<ipnts.length; i++) {
         double dV = idata[i].getDetJ0()*ipnts[i].getWeight();
         IntegrationPoint3d pt = ipnts[i];
         double detJ = pt.computeJacobianDeterminant (myNodes);
         double[] H = pt.getPressureWeights().getBuffer();
         for (int k=0; k<npvals; k++) {
            pressures[k] += H[k]*dV*imat.getEffectivePressure (detJ);
         }            
      }
      
   }

   /**
    * Lagrange pressures array for use with incompressibility 
    * @return pressures
    */
   public double[] getLagrangePressures() {
      return myLagrangePressures;
   }
   
   /**
    * Queries if the effective material for this element, and all auxiliary
    * materials, are defined for non-positive deformation gradients.
    *
    * @return <code>true</code> if the materials associated with this
    * element are invertible
    */
   public boolean materialsAreInvertible() {
      
      FemMaterial mat = getEffectiveMaterial();
      if (!mat.isInvertible()) {
         return false;
      }
      if (myAuxMaterials != null) {
         for (int i=0; i<myAuxMaterials.size(); i++) {
            if (!myAuxMaterials.get(i).isInvertible()) {
               return false;
            }
         }
      }
      return true;
   }

   public abstract void renderWidget (
      Renderer renderer, double size, RenderProps props);

   public double computeDirectedRenderSize (Vector3d dir) {
      IntegrationPoint3d ipnt = getWarpingPoint();
      return ipnt.computeDirectedSizeForRender (dir, getNodes());
   }

   /**
    * Computes the current coordinates and deformation gradient at the
    * warping point, using render coordinates. This is used in element
    * rendering.
    *
    * @param F returns the deformation gradient
    * @param coords returns the current coordinates
    */
   public void computeRenderCoordsAndGradient (Matrix3d F, float[] coords) {
      IntegrationPoint3d ipnt = getWarpingPoint();
      IntegrationData3d idata = getWarpingData();
      ipnt.computeGradientForRender (F, getNodes(), idata.myInvJ0);
      ipnt.computeCoordsForRender (coords, getNodes());
   }

   // Auxiliary materials, used for implementing muscle fibres
   public void addAuxiliaryMaterial (AuxiliaryMaterial mat) {
      if (myAuxMaterials == null) {
         myAuxMaterials = new ArrayList<AuxiliaryMaterial>(4);
      }
      myAuxMaterials.add (mat);
   }

   public boolean removeAuxiliaryMaterial (AuxiliaryMaterial mat) {
      if (myAuxMaterials != null) {
         return myAuxMaterials.remove (mat);
      }
      else {
         return false;
      }
   }

   public int numAuxiliaryMaterials() {
      return myAuxMaterials == null ? 0 : myAuxMaterials.size();
   }

   public AuxiliaryMaterial[] getAuxiliaryMaterials() {
      if (myAuxMaterials == null) {
         return new AuxiliaryMaterial[0];
      }
      return myAuxMaterials.toArray (new AuxiliaryMaterial[0]);
   }

   static int numEdgeSegs = 10;

   /**
    * Support methods for computing the extrapolants that map values at an
    * element's integration points back to its nodes. These are used to
    * determine the extrapolation matrix that is returned by the element's
    * getNodalExtroplationMatrix() method. Extrapolation from integration
    * points to nodes is used to compute things such as nodal stresses.
    *
    * <p> In general, the extrapolants are determined by mapping the integration
    * points (or a suitable subset thereof) onto a special finite element of
    * their own, and computing the shape function values for the nodes
    * in the new coordinate system.
    */
   /** 
    * Returns scaled values for the nodal coordinates of this element as an
    * array of Vector3d. An optional offset value can be added to the values as
    * well.
    */
   protected Vector3d[] getScaledNodeCoords (double s, Vector3d offset) {
      int nn = numNodes();
      Vector3d[] array = new Vector3d[nn];
      double[] coords = getNodeCoords();
      for (int i=0; i<array.length; i++) {
         array[i] = new Vector3d (coords[i*3], coords[i*3+1], coords[i*3+2]);
         array[i].scale (s);
         if (offset != null) {
            array[i].add (offset);
         }
      }
      return array;
   }

   /** 
    * Creates a node extrapolation matrix from a set of (transformed) nodal
    * coordinate values. The size of this matrix is n x m, where n is the
    * number of coordinate values and m is the number of integration points for
    * the element. The extrapolation is based on the shape functions of a
    * specified sub-element. If the number of sub-element nodes is less than m,
    * the remaining matrix elements are set to 0.
    */
   protected double[] createNodalExtrapolationMatrix (
      Vector3d[] ncoords, int m, FemElement3d subelem) {
      
      int n = ncoords.length;
      double[] mat = new double[n*m];
      for (int i=0; i<n; i++) {
         for (int j=0; j<m; j++) {
            if (j < subelem.numNodes()) {
               mat[i*m+j] = subelem.getN (j, ncoords[i]);
            }
            else {
               mat[i*m+j] = 0;
            }
         }
      }
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myVolume *= (s * s * s);
      myRestVolume *= (s * s * s);
      for (int i=0; i<numPressureVals(); i++) {
         myLagrangePressures[i] /= s;
      }
      //myLagrangePressure /= s;
   }

   /* --- Scanning, writing and copying --- */

   // protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   //    throws IOException {

   //    rtok.nextToken();
   //    if (scanAttributeName (rtok, "frame")) {
   //       Matrix3d M = new Matrix3d();
   //       M.scan (rtok);
   //       setFrame (M);
   //       return true;
   //    }
   //    rtok.pushBack();
   //    return super.scanItem (rtok, tokens);
   // }      

   // protected void writeItems (
   //    PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
   //    throws IOException {
   //    super.writeItems (pw, fmt, ancestor);
   //    Matrix3dBase M = getFrame();
   //    if (M != null) {
   //       pw.println ("frame=[");
   //       IndentingPrintWriter.addIndentation (pw, 2);
   //       M.write (pw, fmt);
   //       IndentingPrintWriter.addIndentation (pw, -2);
   //       pw.println ("]");
   //    }
   // }   

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      for (int i=0; i<numNodes(); i++) {
         if (!ComponentUtils.addCopyReferences (refs, myNodes[i], ancestor)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public FemElement3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      FemElement3d e = (FemElement3d)super.copy (flags, copyMap);
      e.myNodes = new FemNode3d[numNodes()];
      for (int i=0; i<numNodes(); i++) {
         FemNode3d n = myNodes[i];
         FemNode3d newn = (FemNode3d)ComponentUtils.maybeCopy (flags, copyMap, n);
         if (newn == null) {
            throw new InternalErrorException (
               "No duplicated node found for node number "+n.getNumber());
         }
         e.myNodes[i] = newn;
      }
      e.myNbrs = null;
      e.myIncompressConstraints = null;
   
      // Note that frame information is not presently duplicated
      e.myIntegrationData = null;
      e.myIntegrationDataValid = false;

      e.myLagrangePressures = new double[numPressureVals()];
      e.myWarper = null;
      e.myIncompressIdx = -1;

      e.setElementWidgetSizeMode (myElementWidgetSizeMode);
      if (myElementWidgetSizeMode == PropertyMode.Explicit) {
         e.setElementWidgetSize (myElementWidgetSize);
      }

      e.myAuxMaterials = null;
      if (myAuxMaterials != null) {
         for (AuxiliaryMaterial a : myAuxMaterials) {
            try {
               e.addAuxiliaryMaterial ((AuxiliaryMaterial)a.clone());
            }
            catch (Exception ex) {
               throw new InternalErrorException (
                  "Can't clone " + a.getClass());
            }
            
         }
      }
      return e;
   }

   /* --- Methods for PointAttachable --- */

   /**
    * {@inheritDoc}
    */
   public PointAttachment createPointAttachment (
      Point pnt) {
      if (pnt.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (!(pnt instanceof Particle)) {
         throw new IllegalArgumentException ("point is not a particle");
      }
      if (ComponentUtils.isAncestorOf (getGrandParent(), pnt)) {
         throw new IllegalArgumentException (
            "Element's FemModel is an ancestor of the point");
      }
      PointFem3dAttachment ax = new PointFem3dAttachment (pnt);
      ax.setFromElement (pnt.getPosition(), this);
      ax.updateAttachment();
      return ax;
   }

   /**
    * {@inheritDoc}
    */
   public FrameFem3dAttachment createFrameAttachment (
      Frame frame, RigidTransform3d TFW) {

      if (frame == null && TFW == null) {
         throw new IllegalArgumentException (
            "frame and TFW cannot both be null");
      }
      if (frame != null && frame.isAttached()) {
         throw new IllegalArgumentException ("frame is already attached");
      }
      FrameFem3dAttachment ffa = new FrameFem3dAttachment (frame);
      if (!ffa.setFromElement (TFW, this)) {
         return null;
      }
      if (frame != null) {
         if (DynamicAttachmentWorker.containsLoop (ffa, frame, null)) {
            throw new IllegalArgumentException (
               "attachment contains loop");
         }
      }
      return ffa;
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

   /* --- Misc methods --- */

   public VectorNd computeGravityWeights () {

      VectorNd weights = new VectorNd (numNodes());
      IntegrationPoint3d[] ipnts = getIntegrationPoints();       
      double wtotal = 0;
      for (int k=0; k<ipnts.length; k++) {
         IntegrationPoint3d pnt = ipnts[k];
         VectorNd N = pnt.getShapeWeights();
         double w = pnt.getWeight();
         for (int i=0; i<numNodes(); i++) {
            weights.add (i, w*N.get(i));
         }
         wtotal += w;
      }
      weights.scale (1/wtotal);
      return weights;
   }

}

