package artisynth.core.femmodels;

import artisynth.core.femmodels.FemElement.ElementClass;
import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.matrix.MatrixNd;
import maspack.geometry.TriangleIntersector;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/** 
 * Implementation of a square shell element with 4 shell nodes and 8 gauss 
 * points.
 */
public class ShellQuadElement extends ShellElement3d {

   /*** Variables and static blocks declarations ****/
   
   protected static double[] myNodeCoords = null;
   protected static int[] myEdgeIdxs = null;
   protected static int[] myFaceIdxs = null;
   protected static int[] myTriangulatedFaceIdxs = null;
   protected static MatrixNd myNodalExtrapolationMatrix = null;
   protected static IntegrationPoint3d[] myDefaultIntegrationPoints;
   protected static IntegrationPoint3d[] myMembraneIntegrationPoints;
   protected IntegrationPoint3d myWarpingPoint;

   // renderer is allocated static to the class, with positions being updated
   // appropriately for each element that uses it.
   protected static FemElementRenderer myRenderer;

   static {
      /* Expected arrangement of the initial node positions */
      myNodeCoords = new double[] {
         0, 0, 0,
         1, 0, 0,
         1, 1, 0,
         0, 1, 0 
      };
      
      /*
       * 4 edges in total. Each row is for a particular edge. Column #0 =
       * Number nodes comprising the edge. Column #1 = First node index of edge.
       * Column #2 = Second node index of edge.
       */
      myEdgeIdxs = new int[] { 
         2, 0, 1,
         2, 1, 2, 
         2, 2, 3, 
         2, 3, 0,
      };

      /*
       * 1 face in total. Each row is for a particular face. Column #0 =
       * Number nodes comprising the face. Columns #1-3: Node indices comprising 
       * the face.
       */
      myFaceIdxs = new int[] { 
         4, 0, 1, 2, 3
      };

      myTriangulatedFaceIdxs = new int[] { 
         0, 1, 2,
         0, 2, 3
      };
      
   }
      
   protected static double[] myDefaultIntegrationCoords = null;
   protected static double[] myMembraneIntegrationCoords = null;
   
   public static final double[] INTEGRATION_COORDS_GAUSS_8;
   public static final double[] INTEGRATION_COORDS_MEMBRANE;
   
   static {
      double a = 1 / Math.sqrt (3);
      double w = 1.0;
      INTEGRATION_COORDS_GAUSS_8 = new double[] { 
         -a, -a, -a, w,
         +a, -a, -a, w,
                                                 
         +a, +a, -a, w,
         -a, +a, -a, w,
                                                 
         -a, -a, +a, w,
         +a, -a, +a, w,
                                                 
         +a, +a, +a, w,
         -a, +a, +a, w };
      myDefaultIntegrationCoords = INTEGRATION_COORDS_GAUSS_8;
      
      INTEGRATION_COORDS_MEMBRANE = new double[] { 
         -a, -a, 0, w,
         +a, -a, 0, w,
                                                 
         +a, +a, 0, w,
         -a, +a, 0, w };                                              
      myMembraneIntegrationCoords = INTEGRATION_COORDS_MEMBRANE;
   }
   
   /*** End of variables and static blocks declarations ****/

   public ShellQuadElement () {
      myNodes = new FemNode3d[myNodeCoords.length/3];
      myElementClass = ElementClass.SHELL; // assume this by default
   }

   /**
    * Creates a new square shell element with four nodes, arranged
    * counter-clockwise around the elements outer facing normal.
    */
   public ShellQuadElement(
      FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3, double thickness) {
      this (p0, p1, p2, p3, thickness, /*membrane=*/false);
   }
         
   public ShellQuadElement(
      FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3, 
      double thickness, boolean membrane) {
      this ();
      setNodes (p0, p1, p2, p3);
      myDefaultThickness = thickness;
      myElementClass = (membrane ? ElementClass.MEMBRANE : ElementClass.SHELL);
   }

   public void setNodes (
      FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3) {
      myNodes[0] = p0;
      myNodes[1] = p1;
      myNodes[2] = p2;
      myNodes[3] = p3;
      invalidateRestData ();
   }
   
   
   /**
    * Compute shape function of particular node.
    * 
    * @param n
    * Node index
    * 
    * @param rst
    * Integration point (i.e. Gauss point)
    */
   @Override
   public double getN (int n, Vector3d rst) {
      double r = rst.x;
      double s = rst.y;
      double rSign = 0;
      double sSign = 0;

      switch (n) {
         case 0:
            rSign = -1;
            sSign = -1;
            break;
         case 1:
            rSign = +1;
            sSign = -1;
            break;
         case 2:
            rSign = +1;
            sSign = +1;
            break;
         case 3:
            rSign = -1;
            sSign = +1;
            break;
         default: {
            throw new IllegalArgumentException (
               "M function index must be in range [0," + (numNodes () - 1)
               + "]");
         }
      }

      return 0.25 * (1 + rSign * r) * (1 + sSign * s);
   }

   /**
    * Compute 1st derivative of shape function of particular node.
    */
   @Override
   public void getdNds (Vector3d dNds, int n, Vector3d rst) {
      switch (n) {
         case 0:
            dNds.x = -0.25 * (1 - rst.y);
            dNds.y = -0.25 * (1 - rst.x);
            break;
         case 1:
            dNds.x = +0.25 * (1 - rst.y);
            dNds.y = -0.25 * (1 + rst.x);
            break;
         case 2:
            dNds.x = +0.25 * (1 + rst.y);
            dNds.y = +0.25 * (1 + rst.x);
            break;
         case 3:
            dNds.x = -0.25 * (1 + rst.y);
            dNds.y = +0.25 * (1 - rst.x);
            break;
         default: {
            throw new IllegalArgumentException (
               "M function index must be in range [0," + (numNodes () - 1)
               + "]");
         }
      }
      dNds.z = 0;     // unused
   }
   
   public double[] getNodeCoords () {
      return myNodeCoords;
   }
   
   private static double[] myNodeMassWeights = new double[] {
      0.25,
      0.25,
      0.25,
      0.25
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   public int[] getEdgeIndices () {
      return myEdgeIdxs;
   }

   public int[] getFaceIndices () {
      return myFaceIdxs;
   }
   
   public int[] getTriangulatedFaceIndices() {
      return myTriangulatedFaceIdxs;
   }

   public double[] getIntegrationCoords () {
      return myDefaultIntegrationCoords;
   }

   public int numIntegrationPoints () {
      if (myElementClass == ElementClass.MEMBRANE) {
         return numPlanarIntegrationPoints();
      }
      else {
         return getIntegrationPoints().length;
      }
   }

   public IntegrationPoint3d[] getIntegrationPoints () {
      if (myDefaultIntegrationPoints == null) {
         ShellElement3d sampleElem = new ShellQuadElement();
         sampleElem.myElementClass = ElementClass.SHELL; 
         myDefaultIntegrationPoints = 
            createIntegrationPoints (sampleElem, myDefaultIntegrationCoords);
      }
      if (myElementClass == ElementClass.MEMBRANE) {
         if (myMembraneIntegrationPoints == null) {
            ShellElement3d sampleElem = new ShellQuadElement();
            sampleElem.myElementClass = ElementClass.MEMBRANE;            
            myMembraneIntegrationPoints = 
            createIntegrationPoints (sampleElem, myMembraneIntegrationCoords);
         }
         return myMembraneIntegrationPoints;
      }
      else {
         return myDefaultIntegrationPoints;
      }
   }

   public int numPlanarIntegrationPoints() {

      return 4;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         myWarpingPoint = IntegrationPoint3d.create (
            this, 0, 0, 0, 1);
         myWarpingPoint.setNumber(numIntegrationPoints());
      }
      return myWarpingPoint;
   }

   public boolean coordsAreInside (Vector3d coords) {
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      return (s0 >= -1 && s0 <= 1 && s1 >= -1 && s1 <= 1 && s2 >= -1 && s2 <= 1);
   }
   
//   protected void updateWarpingPoint() {
//      if (myWarpingPoint != null) {
//         myWarpingPoint.updateCoContraVectors();
//      }
//   }

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // For now, just use integration point values at corresponding nodes
         myNodalExtrapolationMatrix = new MatrixNd (4, 8);
         myNodalExtrapolationMatrix.set (new double[] {
            0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0,
            0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0,
            0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0,
            0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5,
         });
      }
      return myNodalExtrapolationMatrix;
   }

   public double nearestPoint (Point3d near, Point3d pnt) {
      TriangleIntersector trisect = new TriangleIntersector();

      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      Point3d p3 = myNodes[3].getPosition();

      double dmin = trisect.nearestpoint (p0, p1, p2, pnt, near, null);      
      Point3d ptmp = new Point3d();      
      double d    = trisect.nearestpoint (p0, p2, p3, pnt, ptmp, null);      
      if (d < dmin) {
         near.set (ptmp);
         return d;
      }
      else {
         return dmin;
      }
   }

   public double computeRestNodeNormal (Vector3d nrm, FemNode3d node) {
      int idx = getNodeIndex (node);
      Point3d p0 = myNodes[0].getRestPosition();
      Point3d p1 = myNodes[1].getRestPosition();
      Point3d p2 = myNodes[2].getRestPosition();
      Point3d p3 = myNodes[3].getRestPosition();
      switch (idx) {
         case 0: return computeNormal (nrm, p3, p0, p1);
         case 1: return computeNormal (nrm, p0, p1, p2);
         case 2: return computeNormal (nrm, p1, p2, p3);
         case 3: return computeNormal (nrm, p2, p3, p0);
         default: {
            nrm.setZero();
            return 0;
         }
      }
   }

   public double computeNodeNormal (Vector3d nrm, FemNode3d node) {
      int idx = getNodeIndex (node);
      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      Point3d p3 = myNodes[3].getPosition();
      switch (idx) {
         case 0: return computeNormal (nrm, p3, p0, p1);
         case 1: return computeNormal (nrm, p0, p1, p2);
         case 2: return computeNormal (nrm, p1, p2, p3);
         case 3: return computeNormal (nrm, p2, p3, p0);
         default: {
            nrm.setZero();
            return 0;
         }
      }
   }

   /* --- Renderer --- */
   
   @Override
   public void render (Renderer renderer, RenderProps props, int flags) {
      if (myRenderer == null) {
         myRenderer = new FemElementRenderer (this);
      }
      myRenderer.render (renderer, this, props);
   }

   public void renderWidget (
      Renderer renderer, double size, RenderProps props) {
      if (myRenderer == null) {
         myRenderer = new FemElementRenderer (this);
      }
      myRenderer.renderWidget (renderer, this, size, props);
   }


}
