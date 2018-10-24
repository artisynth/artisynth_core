package artisynth.core.femmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;

public abstract class FemElement3dBase extends FemElement {
   
   protected FemNode3d[] myNodes;
   protected FemNodeNeighbor[][] myNbrs = null;

   /* --- Nodes and neighbors --- */

   @Override
   public FemNode3d[] getNodes() {
      return myNodes;
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

   protected boolean containsNode (FemNode3d n) {
      return getNodeIndex(n) != -1;
   }

   /* --- Edges and Faces --- */

   public abstract FemNode3d[][] triangulateFace (FaceNodes3d face);

   public abstract int[] getFaceIndices();

   public abstract int[] getEdgeIndices();

   public abstract FaceNodes3d[] getFaces();

   /* --- Shape functions and coordinates --- */

   public abstract double getN (int i, Vector3d coords);

   public abstract void getdNds (Vector3d dNds, int i, Vector3d coords);

   public abstract double getElementWidgetSize();

   /* --- Integration points and data --- */

   public abstract IntegrationPoint3d getWarpingPoint();

   public abstract IntegrationData3d getWarpingData();

   public abstract IntegrationPoint3d[] getIntegrationPoints();

   public abstract IntegrationData3d[] getIntegrationData();

   protected abstract IntegrationData3d[] doGetIntegrationData();

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
   
}
