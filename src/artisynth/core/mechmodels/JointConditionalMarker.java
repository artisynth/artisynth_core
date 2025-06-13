package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.matrix.Point3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Special frame marker that is conditional upon being within a certain joint
 * coordinate range.
 */
public class JointConditionalMarker extends FrameMarker
   implements ConditionalPoint {

   JointCoordinateHandle myCoord;
   DoubleInterval myRange;

   static boolean DEFAULT_INVISIBLE_IF_INACTIVE = true;
   boolean myInvisibleIfInactive = DEFAULT_INVISIBLE_IF_INACTIVE;

   public static PropertyList myProps =
      new PropertyList (JointConditionalMarker.class, FrameMarker.class);

   static {
      myProps.add (
         "invisibleIfInactive",
         "controls if the marker should be made invisible if inactive", true);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean getInvisibleIfInactive() {
      return myInvisibleIfInactive;
   }
   
   public void setInvisibleIfInactive (boolean enable) {
      if (enable != myInvisibleIfInactive) {
         myInvisibleIfInactive = enable;
         if (enable) {
            isPointActive(); // call to update visibility
         }
         else {
            myRenderProps.setVisibleMode (PropertyMode.Inherited);
         }
      }
   }
   
   public boolean isPointActive() {
      boolean active = myRange.withinRange (myCoord.getStoredValue());
      if (myInvisibleIfInactive) {
         myRenderProps.setVisible (active);
      }
      return active;
   }

   public JointConditionalMarker () {
      setRenderProps (createRenderProps());
   }

   public JointConditionalMarker (
      JointCoordinateHandle coord, DoubleInterval range) {
      this();
      myCoord = new JointCoordinateHandle (coord);
      myRange = new DoubleInterval (range);
      isPointActive(); // call this to update the visibility 
   }

   public JointConditionalMarker (
      Frame frame, Point3d loc,
      JointCoordinateHandle coord, DoubleInterval range) {
      this (coord, range);
      myFrameAttachment.setLocation (loc);
      setFrame (frame);
   }

   /* --- begin I/O methods --- */
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("coordinate=");
      myCoord.write (pw, ancestor);
      pw.println ("range=");
      myRange.write (pw, fmt, ancestor);
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "coordinate")) {
         tokens.offer (new StringToken ("coordinate", rtok.lineno()));
         myCoord = new JointCoordinateHandle();
         myCoord.scan (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "range")) {
         myRange = new DoubleInterval();
         myRange.scan (rtok, null);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "coordinate")) {
         myCoord.postscan (tokens, ancestor);
         isPointActive(); // call to update visibility
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

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

