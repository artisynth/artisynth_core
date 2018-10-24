package artisynth.core.femmodels;

import artisynth.core.femmodels.FemElement.ElementType;
import maspack.matrix.MatrixNd;
import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.geometry.TriangleIntersector;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/** 
 * Implementation of a triangle shell element with 3 shell nodes and 9 gauss
 * points.
 */
public class ShellTriElement extends ShellElement3d {

   /*** Variables and static blocks declarations ****/
   
   protected static double[] myNodeCoords = null;
   protected static int[] myEdgeIdxs = null;
   protected static int[] myFaceIdxs = null;
   protected static double[] myNodalExtrapolationMatrix = null;
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
     };
      
      /*
       * 3 edges in total. Each row is for a particular edge. Column #0 =
       * Number nodes comprising the edge. Column #1 = First node index of edge.
       * Column #2 = Second node index of edge.
       */
      myEdgeIdxs = new int[] { 
         2, 0, 1,
         2, 0, 2,
         2, 1, 2
      };

      /*
       * 1 face in total. Each row is for a particular face. Column #0 =
       * Number nodes comprising the face. Column #1 = First node index of face.
       * Column #2 = Second node index of face. Column #3 = Third node index of
       * face.
       */
      myFaceIdxs = new int[] { 
         3, 0, 1, 2
      };
   }

   protected static double[] myDefaultIntegrationCoords = null;
   public static final double[] INTEGRATION_COORDS_GAUSS_9;
   static {
      double a = 1/6.0;
      double b = 2/3.0;
      double w1 = 5/9.0;
      double w2 = 8/9.0;
      INTEGRATION_COORDS_GAUSS_9 = new double[] { 
         a, a, -b, a*w1,
         b, a, -b, a*w1,
         a, b, -b, a*w1, 
         
         a, a, 0, a*w2, 
         b, a, 0, a*w2, 
         a, b, 0, a*w2,
         
         a, a, b, a*w1, 
         b, a, b, a*w1, 
         a, b, b, a*w1
      };
      myDefaultIntegrationCoords = INTEGRATION_COORDS_GAUSS_9;
   }
   
   /*** End of variables and static blocks declarations ****/

   public ShellTriElement () {
      myNodes = new FemNode3d[myNodeCoords.length/3];
   }

   /**
    * Creates a new triangle element from three nodes, arranged
    * counter-clockwise around the elements outer facing normal.
    */
   public ShellTriElement (
      FemNode3d p0, FemNode3d p1, FemNode3d p2, double thickness) {
      this (p0, p1, p2, thickness, false);
   }

   /**
    * Creates a new triangle element from three nodes, arranged
    * counter-clockwise around the elements outer facing normal.
    */
   public ShellTriElement (
      FemNode3d p0, FemNode3d p1, FemNode3d p2,
      double thickness, boolean membrane) {
      this ();
      setNodes (p0, p1, p2);
      myDefaultThickness = thickness;
      myType = (membrane ? ElementType.MEMBRANE : ElementType.SHELL);
   }

   public void setNodes (FemNode3d p0, FemNode3d p1, FemNode3d p2) {
      myNodes[0] = p0;
      myNodes[1] = p1;
      myNodes[2] = p2;
      invalidateRestData ();
   }
   
   /*** End of Methods pertaining to integration coordinates and points **/

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

      switch (n) {
         case 0:
            return 1-r-s;
         case 1:
            return r;
         case 2:
            return s;
         default: {
            throw new IllegalArgumentException (
               "M function index must be in range [0," + (numNodes () - 1)
               + "]");
         }
      }
   }

   /**
    * Compute 1st derivative of shape function of particular node.
    */
   @Override
   public void getdNds (Vector3d dNds, int n, Vector3d rst) {
      switch (n) {
         case 0:
            dNds.x = -1;
            dNds.y = -1;
            break;
         case 1:
            dNds.x = 1;
            dNds.y = 0;
            break;
         case 2:
            dNds.x = 0;
            dNds.y = 1;
            break;
         default: {
            throw new IllegalArgumentException (
               "M function index must be in range [0," + (numNodes () - 1)
               + "]");
         }
      }
      dNds.z = 0;               // unused
   }
   
   public double[] getNodeCoords () {
      return myNodeCoords;
   }
   
   public int[] getEdgeIndices () {
      return myEdgeIdxs;
   }

   public int[] getFaceIndices () {
      return myFaceIdxs;
   }
   
   public double[] getIntegrationCoords () {
      return myDefaultIntegrationCoords;
   }

   public int numIntegrationPoints () {
      if (myType == ElementType.MEMBRANE) {
         return numPlanarIntegrationPoints();
      }
      else {
         return getIntegrationPoints().length;
      }
   }

   public IntegrationPoint3d[] getIntegrationPoints () {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints (myDefaultIntegrationCoords);
      }
      if (myType == ElementType.MEMBRANE) {
         if (myMembraneIntegrationPoints == null) {
            myMembraneIntegrationPoints = 
               createMembraneIntegrationPoints (myDefaultIntegrationPoints);
         }
         return myMembraneIntegrationPoints;
      }
      else {
         return myDefaultIntegrationPoints;
      }
   }

   public int numPlanarIntegrationPoints() {
      return 3;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         myWarpingPoint = IntegrationPoint3d.create (
            this, 1/3.0, 1/3.0, 0, 1);
      }
      return myWarpingPoint;
   }  
   
   public double[] getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // For now, just use integration point values at corresponding nodes
         double a = 0.4444444444;
         double b = (1-a)/2;
         myNodalExtrapolationMatrix = new double[] {
            b, 0, 0, a, 0, 0, b, 0, 0,
            0, b, 0, 0, a, 0, 0, b, 0,
            0, 0, b, 0, 0, a, 0, 0, b,
         };
      }
      return myNodalExtrapolationMatrix;
   }
   
   public double nearestPoint (Point3d near, Point3d pnt) {
      TriangleIntersector trisect = new TriangleIntersector();
      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      return trisect.nearestpoint (p0, p1, p2, pnt, near, null);      
   }

   public double computeRestNodeNormal (Vector3d nrm, FemNode3d node) {
      return computeNormal (
         nrm,
         myNodes[0].getRestPosition(),
         myNodes[1].getRestPosition(),
         myNodes[2].getRestPosition());
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
