/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.util.NumberFormat;
import maspack.util.StringHolder;

public class RigidTransformWidget extends LabeledControl {
   private static final long serialVersionUID = -1679756323747891445L;
   
   VectorField myTranslationField;
   AxisAngleField myRotationField;
   LabeledComponentPanel myPanel;
   Object myValue = Property.VoidValue;

   private Vector3d getTranslation() {
      VectorNd vec = (VectorNd)myTranslationField.getValue();
      return new Vector3d (vec.get (0), vec.get (1), vec.get (2));
   }

   private AxisAngle getRotation() {
      return (AxisAngle)myRotationField.getValue();
   }

   /**
    * Creates a default RigidTransformWidget with an empty label.
    */
   public RigidTransformWidget() {
      this ("");
   }

   public RigidTransformWidget (String labelText) {
      super (labelText);
      initialize ("%.6g");
      setValue (new RigidTransform3d());
   }

   public RigidTransformWidget (String labelText, RigidTransform3d initialValue) {
      super (labelText);
      initialize ("%.6g");
      setValue (initialValue);
   }

   public RigidTransformWidget (String labelText,
   RigidTransform3d initialValue, String fmtStr) {
      super (labelText);
      initialize (fmtStr);
      setValue (initialValue);
   }

   private void initialize (String fmtStr) {
      
      myTranslationField = new VectorField ("Pos:", 3);
      myTranslationField.setVoidValueEnabled (true);
      myRotationField = new AxisAngleField ("Rot:");
      myRotationField.setVoidValueEnabled (true);

      myPanel = new LabeledComponentPanel();

      myTranslationField.addValueChangeListener (new ValueChangeListener() {
         public void valueChange (ValueChangeEvent e) {
            if (!myRotationField.valueIsVoid()) {
               RigidTransform3d X = new RigidTransform3d();
               X.p.set (getTranslation());
               X.R.setAxisAngle (getRotation());
               updateValue (X);
            }
         }
      });

      myRotationField.addValueChangeListener (new ValueChangeListener() {
         public void valueChange (ValueChangeEvent e) {
            if (!myTranslationField.valueIsVoid()) {
               RigidTransform3d X = new RigidTransform3d();
               X.p.set (getTranslation());
               X.R.setAxisAngle (getRotation());
               updateValue (X);
            }
         }
      });

      myPanel.addWidget(myTranslationField);
      myPanel.addWidget(myRotationField);

      if (myTranslationField.getColumns() > myRotationField.getColumns()) {
         myRotationField.setColumns(myTranslationField.getColumns());
      } else {
         myTranslationField.setColumns(myRotationField.getColumns());
      }
      
      myTranslationField.setFormat (fmtStr);

      addMajorComponent (myPanel);
   }

   /**
    * Sets the formatter used to convert the position field into text. The
    * formatter is specified using a C <code>printf</code> style format
    * string. For a description of the format string syntax, see
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param fmtStr
    * format specification string
    * @see #getFormat
    * @throws IllegalArgumentException
    * if the format string syntax is invalid
    */
   protected void setFormat (String fmtStr) {
      myTranslationField.setFormat (fmtStr);
   }

   /**
    * Directly sets the formatter used used to convert the position field into
    * text.
    * 
    * @param fmt
    * numeric formatter
    * @see #getFormat
    * @see #setFormat(String)
    */
   public void setFormat (NumberFormat fmt) {
      myTranslationField.setFormat (fmt);
   }

   /**
    * Returns the formatter used to convert the position field into text. See
    * {@link #setFormat setFormat} for more details.
    * 
    * @return numeric formatter
    * @see #setFormat
    */
   protected String getFormat() {
      return myTranslationField.getFormat();
   }

   public void dispose() {
      super.dispose();
   }

   /**
    * Updates the control display to reflect the current internl value.
    */
   protected void updateDisplay() {
      myTranslationField.updateDisplay();
      myRotationField.updateDisplay();
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, RigidTransform3d.class, errMsg);
   }

   /**
    * Updates the internal representation of the value, updates any result
    * holders, and returns true if the new value differs from the old value.
    */
   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         myTranslationField.maskValueChangeListeners (true);
         myRotationField.maskValueChangeListeners (true);

         if (value instanceof RigidTransform3d) {
            RigidTransform3d newX = (RigidTransform3d)value;

            if (myTranslationField.valueIsVoid()
            || !newX.p.equals (getTranslation())) {
               myTranslationField.setValue (newX.p);
            }
            RotationMatrix3d R = new RotationMatrix3d();
            if (!myRotationField.valueIsVoid()) {
               R.setAxisAngle (getRotation());
            }
            if (myRotationField.valueIsVoid() || !newX.R.equals (R)) {
               AxisAngle newAxisAngle = new AxisAngle();
               newX.R.getAxisAngle (newAxisAngle);
               myRotationField.setValue (newAxisAngle);
            }
            value = new RigidTransform3d (newX);
         }
         else if (value == Property.VoidValue) {
            myTranslationField.setValue (Property.VoidValue);
            myRotationField.setValue (Property.VoidValue);
         }
         myTranslationField.maskValueChangeListeners (false);
         myRotationField.maskValueChangeListeners (false);

         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object getInternalValue() {
      return myValue;
   }

   @Override
   public void setEnabledAll (boolean enable) {
      super.setEnabledAll (enable);
      myTranslationField.setEnabledAll (enable);
      myRotationField.setEnabledAll (enable);
   }

}
