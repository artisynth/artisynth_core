package artisynth.core.util;

import java.io.*;

import maspack.util.*;
import maspack.properties.*;

/**
 * Encapsulates scalar range and range updating information. Originally
 * designed to maintain the range for color plots.
 */
public class ScalarRange 
   implements CompositeProperty, Scannable, Clonable {

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public static enum Updating {
      FIXED,
      AUTO_EXPAND,
      AUTO_FIT
   };

   static Class<?>[] mySubClasses = new Class<?>[] {
      ScalarRange.class,
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   static Updating defaultUpdating = Updating.AUTO_FIT;
   Updating myUpdating = defaultUpdating;
   PropertyMode myUpdatingMode = PropertyMode.Inherited;

   static DoubleInterval defaultInterval = new DoubleInterval(0,1);
   DoubleInterval myInterval = new DoubleInterval(defaultInterval);
   PropertyMode myIntervalMode = PropertyMode.Inherited;

   public PropertyInfo getPropertyInfo () { 
      return myPropInfo;
    }

   public void setPropertyInfo (PropertyInfo info) {
      myPropInfo = info;
    }

   public HasProperties getPropertyHost() {
      return myPropHost;
    }

   public void setPropertyHost (HasProperties newParent) {
      myPropHost = newParent;
    }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public boolean hasProperty (String name) {
      return getAllPropertyInfo().get(name) != null;
   }

   public static PropertyList myProps = new PropertyList(ScalarRange.class);

   static {
      myProps.addInheritable (
         "updating:Inherited", "how the range is updated",
         defaultUpdating);         
      myProps.addInheritable (
         "interval:Inherited", 
         "range interval", defaultInterval);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ScalarRange() {
   }
   
   public ScalarRange (double lo, double hi) {
      setInterval (new DoubleInterval(lo, hi));
      setUpdating (Updating.FIXED);
   }
   
   public ScalarRange clone() {
      ScalarRange range = null;
      try {
         range = (ScalarRange)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "cannot clone super in ScalarRange");
      }      
      range.myInterval = new DoubleInterval(myInterval);
      return range;
   }

   public boolean isWritable() {
      return true;
   }

   public Updating getUpdating (){
      return myUpdating;
   }

   public void setUpdating (Updating updating) {
      if (myUpdating != updating) {
         if (updating == Updating.AUTO_FIT) {
           zeroInterval();
         }
         myUpdating = updating;
      }
      myUpdatingMode =
         PropertyUtils.propagateValue (
            this, "updating", updating, myUpdatingMode);
   }
   
   public PropertyMode getUpdatingMode() {
      return myUpdatingMode;
   }
   
   public void setUpdatingMode (PropertyMode mode) {
      if (mode != myUpdatingMode) {
         myUpdatingMode = PropertyUtils.setModeAndUpdate (
            this, "updating", myUpdatingMode, mode);
      }
   }
    
   public PropertyMode getIntervalMode() {
      return myIntervalMode;
   }
   
   public void setIntervalMode(PropertyMode mode) {
      if (mode != myIntervalMode) {
         myIntervalMode = PropertyUtils.setModeAndUpdate (
            this, "interval", myIntervalMode, mode);
      }
   }

   public DoubleInterval getInterval (){
      return new DoubleInterval (myInterval);
   }

   public void setInterval (DoubleInterval range) {
      myInterval = new DoubleInterval (range);
      myIntervalMode =
         PropertyUtils.propagateValue (
            this, "interval", range, myIntervalMode);
   }

   public void expandInterval (DoubleInterval range) {
      myInterval.merge (range);
      myIntervalMode =
         PropertyUtils.propagateValue (
            this, "interval", range, myIntervalMode);
   }

   public void updateInterval (DoubleInterval minmax) {
      switch (myUpdating) {
         case FIXED: {
            // do nothing
            break;
         }
         case AUTO_FIT: {
            setInterval (minmax);
            break;
         }
         case AUTO_EXPAND: {
            expandInterval (minmax);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented updating method: " + myUpdating);
         }
      }
   }

   public double getUpperBound() {
      return myInterval.getUpperBound();
   }

   public double getLowerBound() {
      return myInterval.getLowerBound();
   }

   public double clip (double value) {
      return myInterval.clipToRange (value);
   }
   
   public double getRange() {
      return myInterval.getRange();
   }

   public void updateInterval (double min, double max) {
      updateInterval (new DoubleInterval (min, max));
   }

   public void zeroInterval () {
      myInterval.set (0, 0);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) 
      throws IOException {

      getAllPropertyInfo().setDefaultValues (this);
      getAllPropertyInfo().setDefaultModes (this);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

   public void scale (double s) {
      myInterval.scale (s);
   }
}
