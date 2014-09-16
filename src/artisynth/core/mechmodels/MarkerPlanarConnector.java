/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Faces;
import maspack.spatialmotion.RigidBodyConstraint;
import maspack.util.*;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.*;

public class MarkerPlanarConnector extends PlanarConnector {

   protected FrameMarker myFrameMarker = null;
  
   public MarkerPlanarConnector (FrameMarker mkr, RigidBody bodyB,
   RigidTransform3d XPB) {
      this ();
      set ((RigidBody)mkr.getFrame (), mkr.getLocation (), bodyB, XPB);
   }
   
   public MarkerPlanarConnector (FrameMarker mkr,
   RigidTransform3d XPW) {
      this();
      set ((RigidBody)mkr.getFrame (), mkr.getLocation (), XPW);
      myFrameMarker = mkr;
   }

   public MarkerPlanarConnector () {
   }
   
   public void setFrameMarker(FrameMarker mkr) {
      myFrameMarker = mkr;
      set ((RigidBody)mkr.getFrame(), mkr.getLocation(), getTDB());
   }
   
   public FrameMarker getFrameMarker() {
      return myFrameMarker;
   }

   /*
    * before returning constraints, update XFA from frame marker location if needed
    */
   public int getBilateralConstraints (ArrayList<RigidBodyConstraint> bilaterals) {
      if (myFrameMarker != null && 
          !myFrameMarker.getLocation().equals (getTFA().p)) {
         RigidTransform3d XFA = new RigidTransform3d ();
         XFA.p.set (myFrameMarker.getLocation ());
         setTFA (XFA);
      }
      return myCoupling.getBilateralConstraints (bilaterals);
   }

   public double getUnilateralConstraints(
	 ArrayList<RigidBodyConstraint> unilaterals, boolean setEngaged) {
      if (myFrameMarker != null && 
          !myFrameMarker.getLocation().equals (getTFA().p)) {
	 RigidTransform3d XFA = new RigidTransform3d();
	 XFA.p.set(myFrameMarker.getLocation());
	 setTFA(XFA);
      }
      return myCoupling.getUnilateralConstraints(unilaterals, setEngaged);
   }

   
   /*
    * change rendering to see unilateral planar connector as one-sided
    */
   public void setUnilateral (boolean unilateral) {
      super.setUnilateral (unilateral);
      RenderProps.setFaceStyle (this, 
         unilateral?Faces.FRONT:Faces.FRONT_AND_BACK);
   }
   
   /*
    * scan and write code changed to use frame marker reference for bodyA
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReference (rtok, "frameMarker", tokens)) {
         return true;
      }
      else if (scanAndStoreReference (rtok, "bodyA", tokens)) {
         return true;
      }
      else if (scanAndStoreReference (rtok, "bodyB", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "XFA")) {
         RigidTransform3d XFA = new RigidTransform3d();
         XFA.scan (rtok);
         setTFA (XFA);
         return true;
      }
      else if (scanAttributeName (rtok, "XDB")) {
         RigidTransform3d XDB = new RigidTransform3d();
         XDB.scan (rtok);
         setTDB (XDB);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "bodyA")) {
         myBodyA = postscanReference (
            tokens, RigidBody.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "bodyB")) {
         myBodyB = postscanReference (
            tokens, RigidBody.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "frameMarker")) {
         myFrameMarker = postscanReference (
            tokens, FrameMarker.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }


   protected void printMarkerReference(
      PrintWriter pw, CompositeComponent ancestor) {
      if (myFrameMarker != null) {
         pw.println ("frameMarker = "+ComponentUtils.getWritePathName (
            ancestor, myFrameMarker));
      }
   }
   
   protected void printBodyBReference(
      PrintWriter pw, CompositeComponent ancestor) {
      if (myBodyB != null) {
         pw.println ("bodyB = "+ComponentUtils.getWritePathName (
            ancestor, myBodyB));
      }
   }

   protected void printBodyAReference(
      PrintWriter pw, CompositeComponent ancestor) {
      if (myBodyA != null) {
         pw.println ("bodyA = "+ComponentUtils.getWritePathName (
            ancestor, myBodyA));
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      RigidTransform3d XFA = getTFA ();
      RigidTransform3d XDB = getTDB ();
      printMarkerReference (pw, ancestor);
      printBodyAReference (pw, ancestor);
      printBodyBReference (pw, ancestor);
      pw.println ("XFA="
      + XFA.toString (fmt, RigidTransform3d.AXIS_ANGLE_STRING));
      pw.println ("XDB="
      + XDB.toString (fmt, RigidTransform3d.AXIS_ANGLE_STRING));
      getAllPropertyInfo ().writeNonDefaultProps (this, pw, fmt);
   }

}
