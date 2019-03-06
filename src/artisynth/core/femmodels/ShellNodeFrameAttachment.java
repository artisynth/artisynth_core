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
public class ShellNodeFrameAttachment
   extends PointFrameAttachment implements HasAttachments {

   protected DirectorFrameAttachment myDirectorAttachment;

   public ShellNodeFrameAttachment () {
   }

   public ShellNodeFrameAttachment (FemNode3d node, Frame frame) {
      super (frame, node);
      updateDirectorAttachment();
   }

   public FemNode3d getNode() {
      return (FemNode3d)myPoint;
   }
   
   public boolean hasDirectorAttachment() {
      return myDirectorAttachment != null;
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
         BackNode3d backNode = node.getBackNode();
         if (node.hasDirector()) {
            myDirectorAttachment = 
               new DirectorFrameAttachment (node, getFrame());
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
   }

   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      FemNode3d node = getNode();
      if (ComponentUtils.areConnectedVia (this, node, hcomp)) {
         if (node.hasDirector()) {
            node.getBackNode().setAttached (myDirectorAttachment);
         }
      }
      // if (connector == getParent()) {
      //    FemNode3d node = getNode();
      //    if (node.hasDirector()) {
      //       node.getBackNode().setAttached (myDirectorAttachment);
      //    }
      // }
   }

   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      super.disconnectFromHierarchy (hcomp);
      FemNode3d node = getNode();
      if (ComponentUtils.areConnectedVia (this, node, hcomp)) {
         if (node.hasDirector()) {
            node.getBackNode().setAttached (null);
         }
      }
      // if (connector == getParent()) {
      //    FemNode3d node = getNode();
      //    if (node.hasDirector()) {
      //       node.getBackNode().setAttached (null);
      //    }
      // }
   }
   
   public DirectorFrameAttachment getDirectorAttachment() {
      return myDirectorAttachment;
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
         myDirectorAttachment = new DirectorFrameAttachment();
         Vector3d locDir = new Vector3d();
         locDir.scan (rtok);
         myDirectorAttachment.setLocDir (locDir);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   @Override
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "point")) {
         // intercept the postscanning of point so that we can also set the
         // femNode on the directorAttachment, if any 
         myPoint = postscanReference (tokens, FemNode3d.class, ancestor);
         if (myDirectorAttachment != null) {
            myDirectorAttachment.setNode ((FemNode3d)myPoint);
         }
         return true;
      }
      else if (postscanAttributeName (tokens, "frame")) {
         // intercept the postscanning of frame so that we can also set the
         // frame on the directorAttachment, if any
         Frame frame = postscanReference (tokens, Frame.class, ancestor);
         setFrame (frame, myLoc);
         if (myDirectorAttachment != null) {
            myDirectorAttachment.setFrame (frame);
            myDirectorAttachment.setLoc (myLoc);
         }
         return true;
      }
      return super.postscanItem (tokens, ancestor);
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
