package artisynth.core.femmodels;

import java.io.*;
import java.util.List;
import java.util.Deque;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Connects a FemNode3d to a Frame, and provides an additional internal
 * attachment to attach the node's director to the frame as well.
 */
public class ShellNodeFem3dAttachment
   extends PointFem3dAttachment implements HasAttachments {

   protected DirectorFem3dAttachment myDirectorAttachment;

   public ShellNodeFem3dAttachment () {
   }

   public ShellNodeFem3dAttachment (FemNode3d node) {
      super (node);
      updateDirectorAttachment();
   }

   public ShellNodeFem3dAttachment (FemNode3d node, FemModel3d fem) {
      super (node, fem);
      updateDirectorAttachment();
   }
   
   public ShellNodeFem3dAttachment (
      FemNode3d node, FemNode3d[] nodes, double[] coords) {
      super (node, nodes, coords);
      updateDirectorAttachment();
   }

   public FemNode3d getNode() {
      return (FemNode3d)myPoint;
   }
   
   public boolean hasDirectorAttachment() {
      return myDirectorAttachment != null;
   }
   
   public DirectorFem3dAttachment getDirectorAttachment() {
      return myDirectorAttachment;
   }

   public void updateAttachment() {
      super.updateAttachment();
      if (myElement != null) {
         // only need to update director if myElement != null, since
         // super.updateAttachment() doesn't do anything otherwise
         updateDirectorAttachment();
      }
   }

   protected void dosetNodes (FemNode[] nodes, double[] weights) {
      super.dosetNodes (nodes, weights);
      if (!isScanning()) {
         // if scanning, update will be handled after postscan
         updateDirectorAttachment();
      }
   }

   public boolean setFromElement (
      Point3d pos, FemElement elem, double reduceTol) {
      boolean converged = super.setFromElement (pos, elem, /*reduceTol=*/-1);
      updateDirectorAttachment();
      return converged;
   }

   /**
    * Allocates or deallocates a DirectorFrameAttachment depending on whether
    * the node currently contains a director. Care should be taken to ensure
    * that this method is called in a context that will ensure a
    * StructureChangeEvent before any subsequent simulation involving the node.
    */
   void updateDirectorAttachment() {
      FemNode3d node = getNode();
      if (node.hasDirector() != hasDirectorAttachment()) {
         // allocate or deallocate director attachment as needed
         BackNode3d backNode = node.getBackNode();
         if (node.hasDirector()) {
            myDirectorAttachment = 
               new DirectorFem3dAttachment (node);
            if (ComponentUtils.areConnected (this, node)) {
               backNode.setAttached (myDirectorAttachment);
            }
         }
         else {
            myDirectorAttachment = null;
            if (ComponentUtils.areConnected (this, node)) {
               backNode.setAttached (null);
            }
         }
      }
      if (myDirectorAttachment != null) {
         if (myElement != null) {
            if (!(myElement instanceof FemElement3dBase)) {
               throw new UnsupportedOperationException (
                  "ShellNodeFem3dAttachment only supported for elements of type FemElement3dBase");
            }
            myDirectorAttachment.setFromElement (
               myNodes, myCoords, myNatCoords, (FemElement3dBase)myElement);
         }
         else if (myNodes != null) {
            myDirectorAttachment.setFromNodes (myNodes, myCoords);
         }
         myDirectorAttachment.setLocDir (node.getDirector());
      }
   }

   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      FemNode3d node = getNode();
      if (ComponentUtils.areConnectedVia (this, node, hcomp)) {
         if (node.hasDirector()) {
            node.getBackNode().setAttached (myDirectorAttachment);
         }
      }
   }

   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      super.disconnectFromHierarchy (hcomp);
      FemNode3d node = getNode();
      if (ComponentUtils.areConnectedVia (this, node, hcomp)) {
         if (node.hasDirector()) {
            node.getBackNode().setAttached (null);
         }
      }
   }
   
   public void getAttachments (List<DynamicAttachment> list) {
      list.add (this);
      if (myDirectorAttachment != null) {
         list.add (myDirectorAttachment);
      }
   }

   @Override
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "locDir")) {
         // create a director attachment and scan locDir into it
         myDirectorAttachment = new DirectorFem3dAttachment();
         Vector3d locDir = new Vector3d();
         locDir.scan (rtok);
         myDirectorAttachment.setLocDir (locDir);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      if (myDirectorAttachment != null) {
         myDirectorAttachment.setNode (getNode());
         if (myElement != null) {
            if (!(myElement instanceof FemElement3dBase)) {
               throw new UnsupportedOperationException (
                  "ShellNodeFem3dAttachment only supported for elements of type FemElement3dBase");
            }
            myDirectorAttachment.setFromElement (
               myNodes, myCoords, myNatCoords, (FemElement3dBase)myElement);
         }
         else {
            myDirectorAttachment.setFromNodes (myNodes, myCoords);
         }
      }
   }   

   @Override
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      if (myDirectorAttachment != null) {
         pw.print ("locDir=");
         myDirectorAttachment.myLocDir.write (pw, fmt, /*withBrackets=*/true);
         pw.println ("");
      }
   }

}
