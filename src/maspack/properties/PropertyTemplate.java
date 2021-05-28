package maspack.properties;

import maspack.properties.*;

/**
 * Template showing how to define properties within a class
 */
public class PropertyTemplate implements HasProperties {

   // property attribute variables
   public static final String DEFAULT_TEXT = null;
   protected String myText = DEFAULT_TEXT;

   public static final boolean DEFAULT_ACTIVE = false;
   protected boolean myActive = DEFAULT_ACTIVE;

   public static final double DEFAULT_SCALE = 1.0;
   protected double myScale = DEFAULT_SCALE;
   protected PropertyMode myScaleMode;

   // static property definitions

   public static PropertyList myProps =
      new PropertyList (PropertyTemplate.class /*, ParentClass.class*/);

   static {
      myProps.addReadOnly (
         "status", "a readonly integer");
      myProps.add (
         "text", "a text string", DEFAULT_TEXT);
      myProps.add (
         "active isActive *", "a boolean predicate", DEFAULT_ACTIVE);
      myProps.addInheritable (
         "scale", "a scale value with a range", DEFAULT_SCALE, "[0,2]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   // property accessor methods

   public int getStatus() {
      return 7;
   }

   public String getText() {
      return myText;
   }

   public void setText (String text) {
      myText = text;
   }

   public boolean isActive() {
      return myActive;
   }

   public void setActive (boolean active) {
      myActive = active;
   }

   public double getScale() {
      return myScale;
   }

   public void setScale (double scale) {
      myScale = scale;
      myScaleMode =
         PropertyUtils.propagateValue (this, "scale", myScale, myScaleMode);
   }

   public PropertyMode getScaleMode() {
      return myScaleMode;
   }

   public void setScaleMode (PropertyMode mode) {
      myScaleMode =
         PropertyUtils.setModeAndUpdate (this, "scale", myScaleMode, mode);
   }

   public static void main (String[] args) {
      PropertyTemplate template = new PropertyTemplate();
   }
}
