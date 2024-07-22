package artisynth.core.mechmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

import artisynth.core.util.*;
import artisynth.core.util.ObjectToken;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.util.*;

import maspack.matrix.*;
import maspack.function.*;
import artisynth.core.mechmodels.*;

/**
 * Special frame marker that updates its location (with respect to the frame)
 * as a function of joint coordinates. This component is specifically intended
 * to implement the MovingPathPoint component in OpenSim.
 */
public class JointBasedMovingMarker extends FrameMarker 
   implements MovingMarker {

   JointCoordinateHandle[] myCoords;
   Diff1Function1x1[] myFxns;

   public void updateMarkerLocation() {
      Point3d loc = new Point3d();
      loc.x = myFxns[0].eval (myCoords[0].getValue());
      loc.y = myFxns[1].eval (myCoords[1].getValue());
      loc.z = myFxns[2].eval (myCoords[2].getValue());
      setLocation (loc);
   }

   public JointBasedMovingMarker () {
      myCoords = new JointCoordinateHandle[3];
      myFxns = new Diff1Function1x1[3];
   }

   public JointBasedMovingMarker (
      Diff1Function1x1 xfxn, JointCoordinateHandle xcoord,
      Diff1Function1x1 yfxn, JointCoordinateHandle ycoord,
      Diff1Function1x1 zfxn, JointCoordinateHandle zcoord) {

      this();
      myFxns[0] = xfxn; // XXX clone these?
      myFxns[1] = yfxn;
      myFxns[2] = zfxn;
      myCoords[0] = new JointCoordinateHandle (xcoord);
      myCoords[1] = new JointCoordinateHandle (ycoord);
      myCoords[2] = new JointCoordinateHandle (zcoord);
   }

   public JointBasedMovingMarker (
      String name,
      Diff1Function1x1 xfxn, JointCoordinateHandle xcoord,
      Diff1Function1x1 yfxn, JointCoordinateHandle ycoord,
      Diff1Function1x1 zfxn, JointCoordinateHandle zcoord) {
      this (xfxn, xcoord, yfxn, ycoord, zfxn, zcoord);
      setName (name);
   }
   
   public JointCoordinateHandle[] getCoordinateHandles() {
      return myCoords;
   }

   public Diff1Function1x1[] getCoordinateFunctions() {
      return myFxns;
   }

   /* --- begin I/O methods --- */
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("coordinates=");
      JointCoordinateHandle.writeHandles (pw, myCoords, ancestor);
      pw.println ("functions=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (Diff1Function1x1 fxn : myFxns) {
         FunctionUtils.write (pw, fxn, fmt);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "coordinates")) {
         tokens.offer (new StringToken ("coordinates", rtok.lineno()));
         JointCoordinateHandle.scanHandles (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "functions")) {
         rtok.scanToken ('[');
         for (int i=0; i<3; i++) {
            myFxns[i] = FunctionUtils.scan (rtok, Diff1Function1x1.class);
         }
         rtok.scanToken (']');
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "coordinates")) {
         ArrayList<JointCoordinateHandle> handles =
            JointCoordinateHandle.postscanHandles (tokens, ancestor);
         myCoords = handles.toArray(new JointCoordinateHandle[0]);      
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /* --- end I/O methods --- */

   // public static JointCoordinateMarker create (
   //    String name, MovingPathPoint pp, OpenSimObject ref,
   //    ModelComponentMap componentMap) {
      
   //    JointCoordinateMarker mkr = new JointCoordinateMarker (name);
   //    mkr.x_coord = CoordinateHandle.create (pp.x_coordinate, ref, componentMap);
   //    mkr.y_coord = CoordinateHandle.create (pp.y_coordinate, ref, componentMap);
   //    mkr.z_coord = CoordinateHandle.create (pp.z_coordinate, ref, componentMap);
   //    if (mkr.x_coord == null || mkr.y_coord == null || mkr.z_coord == null) {
   //       return null;
   //    }
   //    mkr.x_location = pp.x_location;
   //    mkr.y_location = pp.y_location;
   //    mkr.z_location = pp.z_location;
   //    return mkr;
   // }
}

