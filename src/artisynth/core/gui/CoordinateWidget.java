package artisynth.core.gui;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.ArrayDeque;

import maspack.util.*;
import maspack.widgets.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;

/**
 * Subclass of {@link DoubleFieldSlider} used to adjust the values of a joint
 * coordinate.
 */
public class CoordinateWidget extends DoubleFieldSlider
   implements ValueChangeListener, PostScannable {

   private static final double INF = Double.POSITIVE_INFINITY;
   private static final double DTOR = Math.PI/180;
   private static final double RTOD = 180/Math.PI;

   JointCoordinateHandle myHandle;
   boolean myUseDegrees = true;

   DoubleInterval getCoordRange() {
      if (myUseDegrees) {
         return new DoubleInterval(myHandle.getValueRangeDeg());
      }
      else {
         return new DoubleInterval(myHandle.getValueRange());
      }
   }

   boolean getUseDegrees() {
      return myUseDegrees;
   }

   void setUseDegrees (boolean enable) {
      if (myUseDegrees != enable) {
         myUseDegrees = enable;
         if (enable) {
            setRange (getCoordRange());
            setSliderRange (findSliderRange (myHandle));
            updateValue();
         }
         else {
            updateValue();
            setRange (getCoordRange());
            setSliderRange (findSliderRange (myHandle));
         }
      }
   }

   DoubleInterval findSliderRange (JointCoordinateHandle handle) {
      MotionType mtype = handle.getMotionType();
      double limit;
      if (mtype == MotionType.ROTARY) {
         limit = myUseDegrees ? 360 : 2*Math.PI;
      }
      else {
         limit = 1; // fix
      }
      DoubleInterval range = getCoordRange();
      double lower = range.getLowerBound();
      double upper = range.getUpperBound();
      if (lower == -INF) {
         lower = -limit;
      }
      if (upper == INF) {
         upper = limit;
      }
      if (upper-lower > 2*limit) {
         double mid = (upper+lower)/2;
         upper = mid + limit;
         lower = mid - limit;
      }
      return new DoubleInterval (lower, upper);
   }

   /**
    * No-args constructor needed for scan/write.
    */
   public CoordinateWidget () {
   }

   public CoordinateWidget (String label, JointBase joint, int idx) {
      super();
      initialize (label, joint, idx);
      setSliderRange (findSliderRange (myHandle));
      updateValue();
   }

   public CoordinateWidget (
      String label, JointBase joint, int idx, double min, double max) {
      super();
      initialize (label, joint, idx);
      setSliderRange (min, max);
      updateValue();
   }

   public CoordinateWidget (String label, JointCoordinateHandle handle) {
      this (label, handle, /*useDegrees*/true);
   }

   CoordinateWidget (
      String label, JointCoordinateHandle handle, boolean useDegrees) {
      super();
      myUseDegrees = useDegrees;
      initialize (label, handle);
      setSliderRange (findSliderRange (myHandle));
      updateValue();
   }

   public CoordinateWidget (
      String label, JointCoordinateHandle handle, double min, double max) {
      this (label, handle, min, max, /*useDegrees*/true);
   }

   CoordinateWidget (
      String label, JointCoordinateHandle handle,
      double min, double max, boolean useDegrees) {
      super();
      myUseDegrees = useDegrees;
      initialize (label, handle);
      setSliderRange (min, max);
      updateValue();
   }

   private void initialize (String label, JointBase joint, int idx) {
      if (label == null) {
         label = joint.getCoordinateName (idx);
      }
      if (label == null) {
         label = "coordinate "+idx;
      }
      setLabelText (label);
      myHandle = new JointCoordinateHandle (joint, idx);
      setRange (getCoordRange());
      addValueChangeListener (this);
   }

   private void initialize (String label, JointCoordinateHandle handle) {
      if (label == null) {
         label = handle.getJoint().getCoordinateName (handle.getIndex());
      }
      if (label == null) {
         label = "coordinate " + handle.getIndex();
      }
      setLabelText (label);
      myHandle = new JointCoordinateHandle (handle);
      setRange (getCoordRange());
      addValueChangeListener (this);
   }

   public void valueChange (ValueChangeEvent e) {
      if (myUseDegrees) {
         myHandle.setValueDeg ((Double)e.getValue());
      }
      else {
         myHandle.setValue ((Double)e.getValue());
      }
   }

   public void updateValue() {
      maskValueChangeListeners (true);
      // disabel auto-ranging because we don't want the slider to adjust its
      // range if there is a slight difference between the set and get values.
      setAutoRangingEnabled (false);
      if (myUseDegrees) {
         setValue (myHandle.getValueDeg());
      }
      else {
         setValue (myHandle.getValue());
      }
      setAutoRangingEnabled (true);
      maskValueChangeListeners (false);
   }

   public JointCoordinateHandle getHandle() {
      return myHandle;
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      Deque<ScanToken> tokens = (Deque<ScanToken>)ref;
      if (tokens == null) {
         tokens = new ArrayDeque<> ();
      }
      setScanning (true);
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (rtok.nextToken() != ']') {
         if (ScanWriteUtils.scanAttributeName (rtok, "handle")) {
            myHandle = new JointCoordinateHandle();
            tokens.offer (new StringToken ("handle"));
            myHandle.scan (rtok, tokens);
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "useDegrees")) {
            myUseDegrees = rtok.scanBoolean();
         }
         else if (rtok.tokenIsWord()) {
            String fieldName = rtok.sval;               
            if (!ScanWriteUtils.scanProperty (rtok, this, tokens)) {
               System.out.println (
                  "Warning: internal widget property '" + fieldName +
                  "' not found for " + this + "; ignoring");
            }
         }
         else {
            throw new IOException (
               "Expecting attribute or property name, got "+rtok);
         }
      }
      tokens.offer (ScanToken.END); // terminator token
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      CompositeComponent ancestor = ComponentUtils.castRefToAncestor (ref);
      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      if (myHandle != null) {
         pw.print ("handle=");
         myHandle.write (pw, ancestor);
      }
      if (!myUseDegrees) {
         pw.println ("useDegrees=false");
      }
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanWriteUtils.postscanBeginToken (tokens, this);
      while (tokens.peek() != ScanToken.END) {
         if (ScanWriteUtils.postscanAttributeName (tokens, "handle")) {
            myHandle.postscan (tokens, ancestor);
         }
         else {
            throw new IOException (
               "Unexpected token for CoordinateWidget: " + tokens.poll());
         }
      }      
      tokens.poll(); // eat END token
      setScanning (false);
      addValueChangeListener (this);
   }

   public boolean isWritable() {
      return true;
   }
   
}


