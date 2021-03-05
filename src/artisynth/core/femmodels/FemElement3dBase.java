package artisynth.core.femmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.numerics.GoldenSectionSearch;
import maspack.util.*;
import maspack.function.Function1x1;
import maspack.properties.*;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.RenderProps;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.DynamicAttachmentWorker;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.FrameAttachable;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.materials.*;

public abstract class FemElement3dBase extends FemElement
   implements PointAttachable, FrameAttachable {

   protected ElementClass myElementClass;

   // the warping point is an integration point at the center of the element,
   // used for corotated linear behavior and other things
   protected IntegrationData3d myWarpingData;
   protected StiffnessWarper3d myWarper = null;
   protected ElementRotationData myRotationData = null;

    // per-element integration point data
   protected IntegrationData3d[] myIntegrationData;
   protected boolean myIntegrationDataValid = false;

   protected static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.0;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   protected PropertyMode myElementWidgetSizeMode = PropertyMode.Inherited;

   protected FemNode3d[] myNodes;
   protected FemNodeNeighbor[][] myNbrs = null;

   public static PropertyList myProps =
      new PropertyList (FemElement3dBase.class, FemElement.class);

   static {
      myProps.addInheritable (
         "elementWidgetSize:Inherited",
         "size of rendered widget in each element's center",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ElementClass getElementClass() {
      return myElementClass;
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
   
   public FemModel3d getFemModel() {
      ModelComponent gparent = getGrandParent();
      if (gparent instanceof FemModel) {
         return (FemModel3d)gparent;
      }
      else {
         return null;
      }
   }
   
   /* --- Nodes and neighbors --- */

   public abstract double[] getNodeCoords ();

   public abstract double[] getNodeMassWeights ();

   public void getNodeCoords (Vector3d coords, int nodeIdx) {
      if (nodeIdx < 0 || nodeIdx >= numNodes()) {
         throw new IllegalArgumentException (
            "Node index must be in the range [0,"+(numNodes()-1)+"]");
      }
      double[] c = getNodeCoords();
      coords.set (c[nodeIdx*3], c[nodeIdx*3+1], c[nodeIdx*3+2]);
   }

   @Override
   public FemNode3d[] getNodes() {
      return myNodes;
   }

   public void setNodes(FemNode3d[] nodes) {
      myNodes = nodes;
   }
   
   public FemNodeNeighbor[][] getNodeNeighbors() {
      return myNbrs;
   }

   protected int getNodeIndex (FemNode3d n) {
      for (int i=0; i<myNodes.length; i++) {
         if (myNodes[i] == n) {
            return i;
         }
      }
      return -1;
   }

   public boolean containsNode (FemNode3d n) {
      return getNodeIndex(n) != -1;
   }

   /* --- Integration points and data --- */

   /**
    * Returns the natural coordinates and weights for the default integration
    * points associated with this element. The information is returned as an
    * array of length {@code 4*num}, where {@code num} is the number of
    * coordinates, and the information for each coordinate consists of a
    * 4-tuple giving the three natural coordinates followed by the weight.
    *
    * @return coordinates and weights for the default integration points.
    */
   public abstract double[] getIntegrationCoords();

   /**
    * Returns the number of integration points actually used by this element
    * (not including the <i>warping</i> point, if any). Typically this number
    * will equal the length of the integration point array returned by {@link
    * #getIntegrationPoints}, but in some cases (such as shell elements being
    * used as membrane elements), it will be less.
    * 
    * @return number of integration points used by this element
    */
   public abstract int numIntegrationPoints();

   /**
    * Returns the integration points associated with this element, not
    * including the <i>warping</i> point (see {@link #getWarpingPoint}).  In
    * some cases, such as with membrane elements, only the first <i>n</i>
    * points of these points are actaully used, where <i>n</i> is the number
    * returned by {@link #numIntegrationPoints}.
    */
   public abstract IntegrationPoint3d[] getIntegrationPoints();

   /**
    * Returns the number of integration points actually used by this element,
    * including the warping point. Usually, this is 1+<i>n</i>, where <i>n</i>
    * is the number returned by {@link #numIntegrationPoints}. In cases where
    * <i>n</i> = 1, the (one) integration point is also the warping
    * point, and so this method returns 1.
    *
    * @return number of all integration points used by this element
    */
   public int numAllIntegrationPoints() {
      int n = numIntegrationPoints();
      if (n > 1) {
         n++;
      }
      return n;
   }

   /**
    * Returns all integration points actually used by this element, including
    * the <i>warping</i> point (see {@link #getWarpingPoint}).  The number of
    * points equals {@link #numAllIntegrationPoints}. The first <i>n</i> points
    * correspond to the first <i>n</i> points returned by {@link
    * #getIntegrationPoints}, where <i>n</i> is the number returned by {@link
    * #numIntegrationPoints}, and the last point is the warping point.  In
    * cases where <i>n</i> = 1, there is only one integration point which is
    * also the warping point.
    */
   public IntegrationPoint3d[] getAllIntegrationPoints() {
      int numAll = numAllIntegrationPoints();
      IntegrationPoint3d[] pnts = Arrays.copyOf (getIntegrationPoints(), numAll);
      if (numAll > 1) {
         // set the last point to be the warping point.
         pnts[numAll-1] = getWarpingPoint();
      }
      return pnts;
   }

   /**
    * Create a set of integration points for a given element, using
    * a specified set of natural coordinates.
    *
    * @param sampleElem representative element
    * @param ncoords location of the integration points in natural coordinates
    * @return created integration points 
    */
   public static IntegrationPoint3d[] createIntegrationPoints (
      FemElement3dBase sampleElem, double[] ncoords) {
      int numi = ncoords.length/4;
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      if (ncoords.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+ncoords.length+", expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            sampleElem,
            ncoords[k*4], ncoords[k*4+1], ncoords[k*4+2], ncoords[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }

   /**
    * Create a set of integration points for a given element, using
    * the natural coordinates returned by {@link #getIntegrationCoords}.
    * 
    * @param sampleElem representative element
    * @return created integration points 
    */   
   protected static IntegrationPoint3d[] createIntegrationPoints (
      FemElement3dBase sampleElem) {
      return createIntegrationPoints (
         sampleElem, sampleElem.getIntegrationCoords());
   }

   
   protected IntegrationPoint3d[] createIntegrationPoints() {
      double[] ncoords = getIntegrationCoords();
      int numi = ncoords.length/4;
      IntegrationPoint3d[] pnts = new IntegrationPoint3d[numi];
      for (int k=0; k<numi; k++) {
         pnts[k] = IntegrationPoint3d.create (
            this,
            ncoords[k*4], ncoords[k*4+1], ncoords[k*4+2], ncoords[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }
   public IntegrationData3d[] getIntegrationData() {
      IntegrationData3d[] idata = doGetIntegrationData();
      if (!myIntegrationDataValid) {
         // compute rest Jacobians and such
         IntegrationPoint3d[] ipnts = getIntegrationPoints();
         double minDetJ = Double.MAX_VALUE;
         for (int i=0; i<idata.length; i++) {
            double detJ = 
               idata[i].computeInverseRestJacobian (ipnts[i], getNodes());
            if (detJ < minDetJ) {
               minDetJ = detJ;
            }
         }
         if (minDetJ <= 0) {
            System.out.println (
               "Warning: element "+ComponentUtils.getPathName(this)+
               " is inverted at rest, minDetJ=" + minDetJ);
         }
         myIntegrationDataValid = true;
      }
      return idata;
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

   public void invalidateRestData () {
      super.invalidateRestData();
      // will cause rest Jacobians to be recalculated
      myIntegrationDataValid = false;
      myWarpingData = null;
   }
//
//   public void clearState() {
//      IntegrationData3d[] idata = doGetIntegrationData();
//      for (int i=0; i<idata.length; i++) {
//         idata[i].clearState();
//      }
//   }

   /* --- Element rotation data --- */

   /**
    * Returns the rotation of this element, computing it if necessary.
    * The rotation R is computed from the deformation gradient F at
    * the element's warping point, using the modified polar decomposition
    * <pre>
    * F = R H
    * </pre>
    * Here H is the deformation matrix, which is symmetric but not necessarily
    * positive definite, in order to ensure that R is right-handed.
    *
    * @return element rotation matrix
    */
   public RotationMatrix3d getRotation () {
      return getRotation (null);
   }

   /**
    * Returns the rotation of this element, computing it if necessary.  See
    * {@link #getRotation()} for more details.
    *
    * @param polard if non-null, supplies a PolarDecomposition3d to be
    * used if needed for computing the polar decomposition 
    * @return element rotation matrix
    */
   public RotationMatrix3d getRotation (PolarDecomposition3d polard) {
      if (myRotationData == null) {
         myRotationData = new ElementRotationData();
      }
      if ((myRotationData.myValidFlags & ElementRotationData.R_VALID) == 0) {
         myRotationData.computeR (this, polard);
      }
      return myRotationData.myR;   
   }

   /**
    * Returns the deformation matrix H of this element, computing it if
    * necessary. See {@link #getRotation()} for more details.
    *
    * @return element deformation matrix
    */
   public Matrix3d getDeformation () {
      return getDeformation (null);
   }

   /**
    * Returns the deformation matrix H of this element, computing it if
    * necessary. See {@link #getRotation()} for more details.
    *
    * @param polard if non-null, supplies a PolarDecomposition3d to be
    * used if needed for computing the polar decomposition 
    * @return element deformation matrix
    */
   public Matrix3d getDeformation (PolarDecomposition3d polard) {
      if (myRotationData == null) {
         myRotationData = new ElementRotationData();
      }
      if ((myRotationData.myValidFlags & ElementRotationData.R_VALID) == 0) {
         myRotationData.computeR (this, polard);
      }
      return myRotationData.myH;
   }

   /**
    * Returns the inverse B matrix of this element, computing it if necessary.
    * The inverse B matrix is computed from the deformation matrix H using the
    * formula
    * <pre>
    * inv(B) = inv (trace(H) - H)
    * </pre>
    * and is used in computing derivatives of quantities that depend on
    * the rotation matrix R.
    * See {@link #getRotation()} for details about R and H.
    *
    * @return element inverse B matrix
    */
   public Matrix3d getInvB () {
      return getInvB (null);
   }

   /**
    * Returns the inverse B matrix of this element, computing it if necessary.
    * See {@link #getInvB()} for details.
    *
    * @param polard if non-null, supplies a PolarDecomposition3d to be
    * used if needed for computing the polar decomposition 
    * @return element inverse B matrix
    */
   public Matrix3d getInvB (PolarDecomposition3d polard) {
      if (myRotationData == null) {
         myRotationData = new ElementRotationData();
      }
      if ((myRotationData.myValidFlags & ElementRotationData.INVB_VALID) == 0) {
         myRotationData.computeInvB (this, polard);
      }
      return myRotationData.myInvB;
   }

   /**
    * Should be after node positions change and before subsequent calls to
    * {@link #getRotation()}, {@link #getDeformation()}, or {@link #getInvB()}.
    */
   public void invalidateRotationData() {
      if (myRotationData != null) {
         myRotationData.invalidate();
      }
   }

   /**
    * Should be called only if we want to liberate rotation data memory for
    * some reason.
    */
   public void clearRotationData() {
      myRotationData = null;
   }

   /* --- Stiffness warping --- */

   /**
    * Returns the special <i>warping</i> integration point which is located at
    * the center of the element and is used for computing stiffness warping and
    * other specialized applications.
    */
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
      if (myAugMaterials != null) {
         for (FemMaterial amat : myAugMaterials) {
            if (amat.isLinear()) {
               myWarper.addInitialStiffness(this, amat, weight);
            }
         }
      }  
      if (myAuxMaterials != null) {
         for (AuxiliaryMaterial amat : myAuxMaterials) {
            if (amat.isLinear()) {
               myWarper.addInitialStiffness(this, amat, weight);
            }
         }
      }
      myWarpingStiffnessValidP = true;
   }

   /* --- Solving for natural coordinates --- */

   /**
    * Queries whether or not a set of natural coordinates are inside this
    * element.
    *
    * @return {@code true} if {@code ncoords} are inside this element.
    */
   public abstract boolean coordsAreInside (Vector3d ncoords);

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

   public boolean getMarkerCoordinates (
      VectorNd coords, Vector3d ncoords, Point3d pnt, boolean checkInside) {
      if (coords.size() < numNodes()) {
         throw new IllegalArgumentException (
            "coords size "+coords.size()+" != number of nodes "+numNodes());
      }
      if (ncoords == null) {
         ncoords = new Vector3d();
      }
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

   /**
    * Calls {@link #getNaturalCoordinates(Vector3d,Point3d,int)}
    * with a default maximum number of iterations.
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

      computeLocalPosition (res, coords);
      res.sub (pnt);
   }
   
   /**
    * Given point p, get its natural coordinates with respect to this element.
    * Returns a positive number if the algorithm converges, or -1 if a maximum
    * number of iterations has been reached. Uses a modified Newton's method to
    * solve the equations. The <code>coords</code> argument that returned the
    * coordinates is used, on input, to supply an initial guess of the
    * coordinates.  Zero is generally a safe guess.
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
         computeJacobian (dxds, coords);
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.out.println (
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

         // If the residual norm is within tolerance, we have converged.
         if (rn < tol) {
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
   
   protected static class GSSResidual implements Function1x1 {
      private Point3d c0;
      private Point3d target;
      private Vector3d dir;
      private Point3d c;
      private Vector3d res;
      FemElement3dBase elem;
      
      public GSSResidual(FemElement3dBase elem, Point3d target) {
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
         computeJacobian (dxds, coords);
         LUD.factor (dxds);
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.out.println (
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
         computeJacobian (dxds, coords);
         computeLocalPosition (res, coords);
         res.sub (lpnt);
         LUD.factor (dxds);
         
         double cond = LUD.conditionEstimate (dxds);
         if (cond > 1e10)
            System.out.println (
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

   /* --- Extrapolation matrices --- */

   public abstract MatrixNd getNodalExtrapolationMatrix();
   
   /**
    * Creates and returns a shape matrix for this element. This is an N x K
    * matrix, where N is the number of nodes and K is the number of integration
    * points. Each column gives the current nodal shape function values for a
    * particular integration point. 
    *
    * <p>The shape matrix maps data from the nodes to the integration points,
    * and so has inverse functionality to the matrix returned by
    * getNodalExtrapolationMatrix().
    * 
    * @return shape matrix for this element.
    */
   public MatrixNd getShapeMatrix() {
      int numNodes = myNodes.length;
      int numIntegPts = numIntegrationPoints();
      
      MatrixNd shapeMtx = new MatrixNd(numNodes, numIntegPts);
      for (int n = 0; n < numNodes; n++) {
         for (int k = 0; k < numIntegPts; k++) {
            IntegrationPoint3d iPt = getIntegrationPoints()[k];
            double shapeFunc = getN(n, iPt.getCoords ()) ;
            shapeMtx.set (n,k, shapeFunc);
         }
      }
      return shapeMtx;
   } 

   /*
    * Support methods for computing the extrapolants that map values at an
    * element's integration points back to its nodes. These are used to
    * determine the extrapolation matrix that is returned by the element's
    * getNodalExtroplationMatrix() method. Extrapolation from integration
    * points to nodes is used to compute things such as nodal stresses.
    *
    * In general, the extrapolants are determined by mapping the integration
    * points (or a suitable subset thereof) onto a special finite element of
    * their own, and computing the shape function values for the nodes in the
    * new coordinate system.
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
   protected MatrixNd createNodalExtrapolationMatrix (
      Vector3d[] ncoords, int m, FemElement3d subelem) {
      
      int n = ncoords.length;
      MatrixNd mat = new MatrixNd (n, m);
      for (int i=0; i<n; i++) {
         for (int j=0; j<m; j++) {
            if (j < subelem.numNodes()) {
               mat.set (i, j, subelem.getN (j, ncoords[i]));
            }
         }
      }
      return mat;
   }

   /* --- Edges and Faces --- */

   public abstract int[] getEdgeIndices();

   public abstract int[] getFaceIndices();

   public abstract int[] getTriangulatedFaceIndices();

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

   public boolean containsEdge (FemNode3d n0, FemNode3d n1) {
      int i0, i1;
      if ((i0 = getNodeIndex(n0)) == -1) {
         return false;
      }
      if ((i1 = getNodeIndex(n1)) == -1) {
         return false;
      }
      int[] edgeIdxs = getEdgeIndices();
      int k = 0;
      while (k < edgeIdxs.length) {
         int nv = edgeIdxs[k++];
         if (nv == 2) {
            if ((edgeIdxs[k] == i0 && edgeIdxs[k+1] == i1) ||
                (edgeIdxs[k] == i1 && edgeIdxs[k+1] == i0)) {
               return true;
            }
         }
         k += nv;
      }
      return false;
   }

   private int findIndex (int[] indices, int k0, int kend, int idx) {
      for (int k=k0; k<kend; k++) {
         if (indices[k] == idx) {
            return k - k0;
         }
      }
      return -1;
   }

   public boolean containsFace (
      FemNode3d n0, FemNode3d n1, FemNode3d n2) {

      int i0, i1, i2;
      if ((i0 = getNodeIndex(n0)) == -1) {
         return false;
      }
      if ((i1 = getNodeIndex(n1)) == -1) {
         return false;
      }
      if ((i2 = getNodeIndex(n2)) == -1) {
         return false;
      }
      int[] faceIdxs = getFaceIndices();
      int k = 0;
      while (k < faceIdxs.length) {
         // for each face ...
         int nv = faceIdxs[k++];
         if (nv == 3) {
            // check faces with 3 vertices. First see if they contain node n0.
            int j0 = findIndex (faceIdxs, k, k+nv, i0);
            if (j0 != -1) {
               // If so, compute indices j1 and j2 of the following nodes, and
               // see if they match n1 and n2 in either clockwise or
               // counter-clockwise order
               int j1 = k+(j0+1)%nv;
               int j2 = k+(j0+2)%nv;
               if ((faceIdxs[j1]==i1 && faceIdxs[j2]==i2) ||
                   (faceIdxs[j1]==i2 && faceIdxs[j2]==i1)) {
                  return true;
               }              
            }
         }
         k += nv;
      }
      return false;
   }

   public boolean containsFace (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {

      int i0, i1, i2, i3;
      if ((i0 = getNodeIndex(n0)) == -1) {
         return false;
      }
      if ((i1 = getNodeIndex(n1)) == -1) {
         return false;
      }
      if ((i2 = getNodeIndex(n2)) == -1) {
         return false;
      }
      if ((i3 = getNodeIndex(n3)) == -1) {
         return false;
      }
      int[] faceIdxs = getFaceIndices();
      int k = 0;
      while (k < faceIdxs.length) {
         // for each face ...
         int nv = faceIdxs[k++]; // number of vertices
         if (nv == 4) {
            // check faces with 4 vertices. First see if they contain node n0.
            int j0 = findIndex (faceIdxs, k, k+nv, i0);
            if (j0 != -1) {
               // If so, find indices j1, j2, j3 of the following nodes, and
               // see if they match n1, n2, and n3 in clockwise or
               // counter-clockwise order
               int j1 = k+(j0+1)%nv;
               int j2 = k+(j0+2)%nv;
               int j3 = k+(j0+3)%nv;
               if ((faceIdxs[j1]==i1 && faceIdxs[j2]==i2 && faceIdxs[j3]==i3) ||
                   (faceIdxs[j1]==i3 && faceIdxs[j2]==i2 && faceIdxs[j3]==i1)) {
                  return true;
               }
            }
         }
         k += nv;
      }
      return false;
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

   public int getNumFaces() {
      int num = 0;
      int[] idxs = getFaceIndices();
      for (int i=0; i<idxs.length; i+=(idxs[i]+1)) {
         num++;
      }
      return num;
   }

   /* --- Shape functions and coordinates --- */

   public abstract double getN (int i, Vector3d coords);

   public abstract void getdNds (Vector3d dNds, int i, Vector3d coords);

  /**
    * Compute position within element based on natural coordinates
    * @param pnt position within element
    * @param ncoords natural coordinates
    */
   public abstract void computeLocalPosition (Vector3d pnt, Vector3d ncoords);

   public abstract void computeJacobian (Matrix3d J, Vector3d ncoords);

   /* --- Hierarchy --- */
   
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      if (hcomp == getParent()) {
         FemNode3d[] nodes = getNodes();
         // add element dependency first, so that in the case of shells directors
         // will be enabled for the each node and hence also for the node
         // neighbors
         for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes.length; j++) {
               nodes[i].registerNodeNeighbor (
                  nodes[j], /*shell=*/myElementClass==ElementClass.SHELL);
            }
            nodes[i].addElementDependency(this);
         }
         invalidateMassIfNecessary();

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
   }
   
   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      if (hcomp == getParent()) {
         myNbrs = null;

         FemNode3d[] nodes = getNodes();
         for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes.length; j++) {
               nodes[i].deregisterNodeNeighbor (
                  nodes[j], /*shell=*/myElementClass==ElementClass.SHELL);
            }
            nodes[i].invalidateMassIfNecessary ();  // signal dirty
            nodes[i].removeElementDependency(this);
         }
      }
      super.disconnectFromHierarchy (hcomp);
   }

   /* --- Boundable --- */ 

   protected double computeCovarianceFromIntegrationPoints (Matrix3d C) {
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

   public double computeCovariance (Matrix3d C) {
      return computeCovarianceFromIntegrationPoints (C);
   }  

   /* --- Scanning, writing and copying --- */

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "frame")) {
         Matrix3d M = new Matrix3d();
         M.scan (rtok);
         setFrame (M);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }      

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      Matrix3dBase M = getFrame();
      if (M != null) {
         pw.println ("frame=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         M.write (pw, fmt);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

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

   public FemElement3dBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      FemElement3dBase e = (FemElement3dBase)super.copy (flags, copyMap);
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

      // Note that frame information is not presently duplicated
      e.myIntegrationData = null;
      e.myIntegrationDataValid = false;     
      e.myWarper = null;
      e.setElementWidgetSizeMode (myElementWidgetSizeMode);
      if (myElementWidgetSizeMode == PropertyMode.Explicit) {
         e.setElementWidgetSize (myElementWidgetSize);
      }
      return e;
   }

   /** 
    * Set reference frame information for this element. This can be used for
    * computing anisotropic material parameters. In principle, each integration
    * point can have its own frame information, but this method simply sets the
    * same frame information for all the integration points, storing it in each
    * IntegrationData structure. Setting <code>M</code> to <code>null</code>
    * removes the frame information.
    * 
    * @param M frame information (is copied by the method)
    */
   public void setFrame (Matrix3dBase M) {
      Matrix3d F = null;
      if (M != null) {
         F = new Matrix3d (M);
      }
      IntegrationData3d[] idata = doGetIntegrationData();
      for (int i=0; i<idata.length; i++) {
         idata[i].myFrame = F;
      }
   }

   public Matrix3d getFrame() {
      IntegrationData3d[] idata = doGetIntegrationData();
      return idata[0].getFrame();
   }

   /* --- Rendering --- */

   public abstract void renderWidget (
      Renderer renderer, double size, RenderProps props);

   /**
    * Compute size of an element's render coordinates along a specified
    * direction.
    */
   public double computeDirectedRenderSize (Vector3d dir) {
      double max = -Double.MAX_VALUE;
      double min = Double.MAX_VALUE;
      for (int i=0; i<numNodes(); i++) {
         float[] npos = myNodes[i].myRenderCoords;
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

   /* --- Methods for PointAttachable and FrameAttachable --- */

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

   /* --- Misc Methods --- */
   
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

   public MatrixNd computeConsistentMass () {

      MatrixNd M = new MatrixNd (numNodes(), numNodes());
      IntegrationPoint3d[] ipnts = getIntegrationPoints();       
      for (int k=0; k<ipnts.length; k++) {
         IntegrationPoint3d pnt = ipnts[k];
         VectorNd N = pnt.getShapeWeights();
         double w = pnt.getWeight();
         for (int i=0; i<numNodes(); i++) {
            for (int j=0; j<numNodes(); j++) {
               double dm = w*getDensity()*N.get(i)*N.get(j);
               M.set (i, j, dm + M.get (i, j));
            }
         }
      }
      return M;
   }

   public void addConsistentMass (SparseNumberedBlockMatrix M) {
      IntegrationPoint3d[] ipnts = getIntegrationPoints();       
      IntegrationData3d[] idata = getIntegrationData();       
      for (int k=0; k<ipnts.length; k++) {
         IntegrationPoint3d pnt = ipnts[k];
         VectorNd N = pnt.getShapeWeights();
         double density = pnt.getWeight()*getDensity()*idata[k].getDetJ0();
         for (int i=0; i<numNodes(); i++) {
            int bi = myNodes[i].getSolveIndex();
            for (int j=0; j<numNodes(); j++) {
               int bj = myNodes[j].getSolveIndex();
               double dm = density*N.get(i)*N.get(j);
               myNbrs[i][j].myMass00 += dm;
               MatrixBlock blk = M.getBlock (bi, bj);
               if (blk instanceof Matrix3d) {
                  Matrix3d Mblk = (Matrix3d)blk;
                  Mblk.m00 += dm;
                  Mblk.m11 += dm;
                  Mblk.m22 += dm;
               }
            }
         }
      }
   }         

   public void addConsistentGravity (Vector3d gacc) {
      IntegrationPoint3d[] ipnts = getIntegrationPoints();       
      IntegrationData3d[] idata = getIntegrationData();       
      for (int k=0; k<ipnts.length; k++) {
         IntegrationPoint3d pnt = ipnts[k];
         VectorNd N = pnt.getShapeWeights();
         double density = pnt.getWeight()*getDensity()*idata[k].getDetJ0();
         for (int i=0; i<numNodes(); i++) {
            FemNode3d node = myNodes[i];
            node.addScaledForce (density*N.get(i), gacc);
         }
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

   /** 
    * Returns true if the rest position for this element results in a negative
    * Jacobian determinant for at least one integration point.
    *
    * @return true if the rest position is inverted.
    */
   public boolean isInvertedAtRest() {
      // getIntegrationData() should update the rest data if necessary
      IntegrationData3d[] idata = getIntegrationData();
      for (int i = 0; i < idata.length; i++) {
         if (idata[i].getDetJ0() < 0) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Returns flags indicating if stress or strain values should be
    * computed for any of the nodes in this element
    * 
    * @return stress/strain compute flags
    */
   protected int needsStressStrain() {
      int flags = 0;
      for (FemNode3d n : myNodes) {
         flags |= n.needsStressStrain();
      }
      return flags;      
   }

   /* --- remaining implementation of HasNumericState --- */

   public void advanceState (double t0, double t1) {
      if (myNumMaterialsWithState == -1) {
         updateStateObjects();
      }
      if (myNumMaterialsWithState > 0) {
         ArrayList<HasMaterialState> mats = new ArrayList<>();
         collectMaterialsWithState (mats);
         IntegrationData3d[] idata = doGetIntegrationData();
         for (int k = 0; k < idata.length; k++) {
            MaterialStateObject[] sobjs = idata[k].getStateObjects();
            if (mats.size() != sobjs.length) {
               throw new InternalErrorException (
                  "Number of materials with state "+mats.size()+
                  " != number of state objects "+ sobjs.length);
            }
            for (int j=0; j<sobjs.length; j++) {
               mats.get(j).advanceState (sobjs[j], t0, t1);
            }
         }
      }
   }

   public void getState (DataBuffer data) {
      if (myNumMaterialsWithState == -1) {
         updateStateObjects();
      }
      if (myNumMaterialsWithState > 0) {
         IntegrationData3d[] idata = doGetIntegrationData();
         for (int k = 0; k < idata.length; k++) {
            MaterialStateObject[] sobjs = idata[k].getStateObjects();
            for (int j=0; j<sobjs.length; j++) {
               sobjs[j].getState (data);
            }
         }
      }
   }

   public void setState (DataBuffer data) {
      if (myNumMaterialsWithState == -1) {
         updateStateObjects();
      }
      if (myNumMaterialsWithState > 0) {
         IntegrationData3d[] idata = doGetIntegrationData();
         for (int k = 0; k < idata.length; k++) {
            MaterialStateObject[] sobjs = idata[k].getStateObjects();
            for (int j=0; j<sobjs.length; j++) {
               sobjs[j].setState (data);
            }
         }
      }
   }

   protected void updateStateObjects() {
      ArrayList<HasMaterialState> mats = new ArrayList<>();
      collectMaterialsWithState (mats);
      // use doGetIntegrationData() because data does not need to "valid"
      // at this point
      IntegrationData3d[] idata = doGetIntegrationData();
      if (mats.size() > 0) {
         for (int k = 0; k < idata.length; k++) {
            MaterialStateObject[] sobjs = new MaterialStateObject[mats.size()];
            for (int j=0; j<sobjs.length; j++) {
               sobjs[j] = mats.get(j).createStateObject();
            }
            idata[k].setStateObjects(sobjs);
         }         
      }
      else {
         for (int k = 0; k < idata.length; k++) {
            idata[k].setStateObjects(null);
         }         
      }
      myNumMaterialsWithState = mats.size();
   }

   public void notifyStateVersionChanged() {
      myStateVersion++;
      myNumMaterialsWithState = -1;
      IntegrationData3d[] idata = doGetIntegrationData();
      for (int k = 0; k < idata.length; k++) {
         idata[k].clearStateObjects();
      } 
   }
}
