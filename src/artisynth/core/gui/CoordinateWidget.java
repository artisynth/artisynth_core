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

public class CoordinateWidget extends DoubleFieldSlider
   implements ValueChangeListener, PostScannable {

   private static final double INF = Double.POSITIVE_INFINITY;
   private static final double DTOR = Math.PI/180;
   private static final double RTOD = 180/Math.PI;

   JointCoordinateHandle myHandle;

   DoubleInterval findSliderRange (JointCoordinateHandle handle) {
      DoubleInterval range = new DoubleInterval(handle.getValueRangeDeg());
      double lower = range.getLowerBound();
      double upper = range.getUpperBound();
      if (lower == -INF || upper == INF) {
         MotionType mtype = handle.getMotionType();
         double limit = (mtype==MotionType.ROTARY ? 180 : 1);
         if (upper == INF && lower == -INF) {
            range.setUpperBound (limit);
            range.setLowerBound (-limit);
         }
         else if (upper == INF) {
            range.setUpperBound (lower+limit);
         }
         else {
            range.setLowerBound (upper-limit);
         }
      }
      return range;
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
   }

   public CoordinateWidget (
      String label, JointBase joint, int idx, double min, double max) {
      super();
      initialize (label, joint, idx);
      setSliderRange (min, max);
   }

   private void initialize (String label, JointBase joint, int idx) {
      if (label == null) {
         label = joint.getCoordinateName (idx);
      }
      setLabelText (label);
      myHandle = new JointCoordinateHandle (joint, idx);
      setRange (myHandle.getValueRangeDeg());
      addValueChangeListener (this);
   }

   public void valueChange (ValueChangeEvent e) {
      myHandle.setValueDeg ((Double)e.getValue());
   }

   public void updateValue() {
      maskValueChangeListeners (true);
      // disabel auto-ranging because we don't want the slider to adjust its
      // range if there is a slight difference between the set and get values.
      setAutoRangingEnabled (false);
      setValue (myHandle.getStoredValueDeg());
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


