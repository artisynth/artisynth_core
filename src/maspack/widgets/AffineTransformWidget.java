/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.Property;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.StringHolder;

// TODO: Need to implement this using a LabeledPanel, so that
// label alignment works properly
public class AffineTransformWidget extends LabeledControl implements
   ValueChangeListener {
   
   private static final long serialVersionUID = 7095521568107235389L;
   VectorField myTranslationField;
   AxisAngleField myRotationField;
   ScaleField myScaleField;
   VectorField myShearField;

   Vector3d myTranslation;
   AxisAngle myRotation;
   Vector3d myScale;
   Vector3d myShear;

   ArrayList<LabeledTextField> myFields = new ArrayList<LabeledTextField>();

   LabeledComponentPanel myPanel;
   // Note: we don't keep an explicit value, except an indicator as to whether
   // the value is void. Otherwise, the value is constructed from the values in
   // the sub-widgets.
   boolean myValueIsVoid = true;
   Matrix3d myV = new Matrix3d();

   private static double DOUBLE_PREC = 2.220446049250313e-16;

   private Vector3d getTranslation() {
      if (myTranslationField == null) {
         return myTranslation;
      }
      else {
         return (Vector3d)myTranslationField.getValue();
      }
   }

   private AxisAngle getRotation() {
      if (myRotationField == null) {
         return myRotation;
      }
      else {
         return (AxisAngle)myRotationField.getValue();
      }
   }

   private Vector3d getShear() {
      if (myShearField == null) {
         return myShear;
      }
      else {
         return (Vector3d)myShearField.getValue();
      }
   }

   private Vector3d getScale() {
      if (myScaleField == null) {
         return myScale;
      }
      else {
         Object value = myScaleField.getValue();
         if (value instanceof Vector3d) {
            return (Vector3d)value;
         }
         else if (value instanceof Double) {
            double s = ((Double)value).doubleValue();
            return new Vector3d (s, s, s);
         }
         else {
            throw new InternalErrorException (
               "Scaled field returns unknown value type " + value.getClass());
         }
      }
   }

   private void setTranslation (Vector3d p) {
      if (myTranslationField == null) {
         myTranslation.set (p);
      }
      else {
         myTranslationField.setValue (p);
      }
   }

   private void setRotation (AxisAngle axisAng) {
      if (myRotationField == null) {
         myRotation.set (axisAng);
      }
      else {
         myRotationField.setValue (axisAng);
      }
   }

   private void setShear (Vector3d shear) {
      if (myShearField == null) {
         myShear.set (shear);
      }
      else {
         myShearField.setValue (shear);
      }
   }

   private void setScale (Vector3d scale) {
      if (myScaleField == null) {
         myScale.set (scale);
      }
      else {
         if (scale.x == scale.y && scale.x == scale.z) {
            myScaleField.setValue (scale.x);
         }
         else {
            myScaleField.setValue (scale);
         }
      }
   }

   public VectorField getTranslationField() {
      return myTranslationField;
   }

   public AxisAngleField getRotationField() {
      return myRotationField;
   }

   public ScaleField getScaleField() {
      return myScaleField;
   }

   public VectorField getShearField() {
      return myShearField;
   }

   /**
    * Creates a default AffineTransformWidget with an empty label.
    */
   public AffineTransformWidget() {
      this ("", "TRSX");
   }

   /**
    * Creates an AffineTransformWidget with the components specified by the
    * letters contained in <code>compSpec</code>:
    * 
    * <ul>
    * <li>'T': translation field
    * <li>'S': scale field
    * <li>'R': rotation field
    * <li>'X': shear field
    * </ul>
    */
   public AffineTransformWidget (String labelText, String compSpec) {
      super (labelText);
      initialize (compSpec, "%.5g");
      setValue (new RigidTransform3d());
   }

   public AffineTransformWidget (String labelText, String compSpec,
      AffineTransform3dBase initialValue) {
      super (labelText);
      initialize (compSpec, "%.5g");
      setValue (initialValue);
   }

   private void initialize (String compSpec, String fmtStr) {
      myPanel = new LabeledComponentPanel();

      for (int i = 0; i < compSpec.length(); i++) {
         switch (compSpec.charAt (i)) {
            case 'T': {
               if (myTranslationField == null) {
                  myTranslationField = new VectorField ("Pos:", 3);
                  myTranslationField.setResultHolder (new Vector3d());
                  myTranslationField.setFormat (fmtStr);
                  myFields.add (myTranslationField);
               }
               break;
            }
            case 'R': {
               if (myRotationField == null) {
                  myRotationField = new AxisAngleField ("Rot:");
                  myFields.add (myRotationField);
               }
               break;
            }
            case 'S': {
               if (myScaleField == null) {
                  myScaleField = new ScaleField ("Scale:");
                  myScaleField.setFormat (fmtStr);
                  myFields.add (myScaleField);
               }
               break;
            }
            case 'X': {
               if (myShearField == null) {
                  myShearField = new VectorField ("Shear:", 3);
                  myShearField.setResultHolder (new Vector3d());
                  myShearField.setFormat (fmtStr);
                  myFields.add (myShearField);
               }
               break;
            }
            default: { // unknown letters are ignored
            }
         }
      }
      
      int maxColumns = 0;
      for (LabeledTextField c : myFields) {
         c.addValueChangeListener (this);
         c.setVoidValueEnabled (true);
         c.setStretchable (false);
         if (c.getColumns() > maxColumns) {
            maxColumns = c.getColumns();
         }
         myPanel.addWidget(c);
      }
      
      // adjust columns to match
      for (LabeledTextField c : myFields) {
         c.setColumns(maxColumns);
      }
      // packFields();
      
      addMajorComponent (myPanel);

      // allocate places to store values for widgets that are null
      if (myTranslationField == null) {
         myTranslation = new Vector3d();
      }
      if (myRotationField == null) {
         myRotation = new AxisAngle();
      }
      if (myScaleField == null) {
         myScale = new Vector3d();
      }
      if (myShearField == null) {
         myShear = new Vector3d();
      }
   }

   /**
    * Remove all the fields from this widget's panel so that they can be added
    * separately to some other widget.
    */
   public void unpackFields() {
      for (LabeledTextField c : myFields) {
         myPanel.remove (c);
      }
   }

   /**
    * puts all the fields into a panel and adjusts their label widths
    * Need to reimplement this using a labeled panel, so that
    * the fields align properly
    */
   public void packFields() {
      LabelSpacing max = new LabelSpacing();
      LabelSpacing spc = new LabelSpacing();
      for (LabeledTextField c : myFields) {
         c.getLabelSpacing(spc);
         max.expand (spc);
      }
      for (LabeledTextField c : myFields) {
         myPanel.add (c);
         c.setLabelSpacing (max);
      }
      //       int maxLabelWidth = 0;
      //       for (LabeledTextField c : myFields) {
      //          int w = c.getLabelWidth();
      //          if (w > maxLabelWidth) {
      //             maxLabelWidth = w;
      //          }
      //       }
      //       for (LabeledTextField c : myFields) {
      //          myPanel.add (c);
      //          c.setLabelWidth (maxLabelWidth);
      //       }
   }

   private boolean allComponentsAreNonVoid() {
      for (LabeledTextField c : myFields) {
         if (c.valueIsVoid()) {
            return false;
         }
      }
      return true;
   }

   public void valueChange (ValueChangeEvent e) {
      // one or more values in the widgets has changed, so component
      // should have a new value. Fire the value change listeners,
      // but there is no need to call updateValue.
      fireValueChangeListeners (getInternalValue());
   }

   public AffineTransform3d getTransformValue() {
      RotationMatrix3d R = new RotationMatrix3d (getRotation());
      Vector3d scale = getScale();
      Vector3d shear = getShear();

      // XXX Note: if you return a RigidTransform3d, results in
      // an Illegal Argument Exception on "method.invoke(...)"
      //      if (scale.x == 1 && scale.y == 1 && scale.z == 1 &&
      //          shear.x == 0 && shear.y == 0 && shear.z == 0) {
      //         RigidTransform3d RT = new RigidTransform3d();
      //         RT.p.set (getTranslation());
      //         RT.R.set (R);
      //         return RT;
      //      }
      //      else {
      AffineTransform3d XT = new AffineTransform3d();
      XT.p.set (getTranslation());
      XT.setA (R, scale, shear);
      return XT;
      //  }
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
      for (LabeledTextField c : myFields) {
         c.updateDisplay();
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, AffineTransform3dBase.class, errMsg);
   }

   private void maskValueChanges (boolean enable) {
      for (LabeledTextField c : myFields) {
         c.maskValueChangeListeners (enable);
      }
   }

   /**
    * Updates the internal representation of the value, updates any result
    * holders, and returns true if the new value differs from the old value.
    */
   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, getInternalValue())) {
         maskValueChanges (true);

         if (value == Property.VoidValue) {
            for (LabeledTextField c : myFields) {
               c.setValue (Property.VoidValue);
            }
            myValueIsVoid = true;
         }
         else {
            RotationMatrix3d R = new RotationMatrix3d();
            Vector3d p = new Vector3d();
            Vector3d scale = new Vector3d (1, 1, 1);
            Vector3d shear = new Vector3d (0, 0, 0);

            if (value instanceof RigidTransform3d) {
               RigidTransform3d newX = (RigidTransform3d)value;
               p.set (newX.p);
               R.set (newX.R);
               //System.out.println ("newT\n" + newX.toString("%10.6f"));
            }
            else if (value instanceof AffineTransform3d) {
               AffineTransform3d newX = (AffineTransform3d)value;
               p.set (newX.p);
               newX.factorA (R, scale, shear);
               //System.out.println ("newX\n" + newX.toString("%10.6f"));
            }
            else {
               throw new InternalErrorException (
                  "Unknown value type " + value.getClass());
            }
            setTranslation (p);
            setRotation (R.getAxisAngle());
            setScale (scale);
            setShear (shear);
            myValueIsVoid = false;
         }
         maskValueChanges (false);
         return true;
      }
      else {
         return false;
      }
   }

   protected Object getInternalValue() {
      return myValueIsVoid ? Property.VoidValue : getTransformValue();
   }

   @Override
   public void setEnabledAll (boolean enable) {
      super.setEnabledAll (enable);
      for (LabeledTextField c : myFields) {
         c.setEnabledAll (enable);
      }
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("AffineTransformWidget Test");
      LabeledComponentPanel panel = new LabeledComponentPanel();
      AffineTransformWidget widget =
               new AffineTransformWidget ("affineX", "TR");

      frame.getContentPane().add (panel);
      widget.unpackFields();

      AffineTransform3d X = new AffineTransform3d();
      RotationMatrix3d R = new RotationMatrix3d (1, 0, 0, Math.toRadians(45));
      Vector3d scale = new Vector3d (1, 2, 3);
      Vector3d shear = new Vector3d (0.2, 0.4, 0.6);
      //Vector3d scale = new Vector3d (1, 1, 1);
      //Vector3d shear = new Vector3d (0, 0, 0);
      X.setA (R, scale, shear);

      widget.setValue (X);
      widget.addValueChangeListener (
         new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               AffineTransformWidget w = (AffineTransformWidget)e.getSource();
               System.out.println (
                  "new value:\n" + w.getTransformValue().toString("%10.6f"));
            }
         });

      panel.addWidget ("translation", widget.getTranslationField());
      panel.addWidget ("orientation", widget.getRotationField());
      panel.addWidget ("scale", widget.getScaleField());
      panel.addWidget ("shear", widget.getShearField());

      // panel.addWidget (widget);

      frame.pack();
      frame.setVisible (true);
   }

}
