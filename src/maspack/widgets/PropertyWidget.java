/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.swing.JComponent;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import maspack.geometry.Rectangle2d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.VectorBase;
import maspack.matrix.VectoriBase;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.InheritableProperty;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.GL.GLGridResolution;
import maspack.util.DoubleInterval;
import maspack.util.EnumRange;
import maspack.util.IntegerInterval;
import maspack.util.InternalErrorException;
import maspack.util.NumericInterval;
import maspack.util.Range;
import maspack.util.StringRange;

public class PropertyWidget {

   static Color explicitColor = new Color (0.8f, 0.8f, 0.8f);
   static Color inheritedColor = new Color (0.8f, 0.8f, 1.0f);

   public static JComponent create (HasProperties host, String name) {
      Property prop = host.getProperty (name);
      if (prop != null) {
         return create (prop);
      }
      else {
         return null;
      }
   }

   public static LabeledComponentBase create (
      HasProperties host, String name, double min, double max) {
      Property prop = host.getProperty (name);
      if (prop != null) {
         return create (prop, min, max);
      }
      else {
         return null;
      }
   }

   public static LabeledComponentBase create (
      String labelText, HasProperties host, String name) {
      Property prop = host.getProperty (name);
      LabeledComponentBase comp = null;

      if (prop != null) {
         comp = create (prop);
         if (comp instanceof LabeledWidget) {
            LabeledWidget lwidget = (LabeledWidget)comp;
            lwidget.setLabelText (labelText);
            lwidget.setToolTipText (prop.getInfo().getDescription());
         }
      }
      return comp;
   }

   public static LabeledComponentBase create (
      String labelText, HasProperties host, String name, double min, double max) {
      Property prop = host.getProperty (name);
      LabeledComponentBase comp = null;

      if (prop != null) {
         comp = create (prop, min, max);
         if (comp instanceof LabeledWidget) {
            LabeledWidget lwidget = (LabeledWidget)comp;
            lwidget.setLabelText (labelText);
            lwidget.setToolTipText (prop.getInfo().getDescription());
         }
      }
      return comp;
   }

   public static LabeledComponentBase create (
      Property prop, double min, double max) {
      String name = prop.getName();
      PropertyInfo info = prop.getInfo();
      Class<?> type = info.getValueClass();

      LabeledComponentBase widget = null;

      if (type == double.class || type == float.class || 
         type == Double.class || type == Float.class) {
         DoubleFieldSlider doubleSlider = new DoubleFieldSlider (name, min, max);
         if (info.getPrintFormat() != null) {
            doubleSlider.setFormat (info.getPrintFormat());
         }
         Range range = prop.getRange();
         if (range instanceof NumericInterval) {
            NumericInterval nrange = (NumericInterval)range;
            doubleSlider.setRange (nrange);
            if (nrange.isBounded()) {
               doubleSlider.setSliderRange (nrange);
            }
         }
         GuiUtils.setFixedWidth (doubleSlider.getTextField(), 100);
         doubleSlider.addValueChangeListener (new PropChangeListener (prop));
         widget = doubleSlider;
      }
      else if (type == int.class || type == Integer.class) {
         IntegerFieldSlider intSlider =
            new IntegerFieldSlider (name, (int)min, (int)max);
         Range range = prop.getRange();
         if (range instanceof IntegerInterval) {
            IntegerInterval irange = (IntegerInterval)range;
            intSlider.setRange (irange);
            if (irange.isBounded()) {
               intSlider.setSliderRange (irange);
            }            
            //intSlider.setSliderRange ((IntRange)range);
         }
         GuiUtils.setFixedWidth (intSlider.getTextField(), 100);
         intSlider.addValueChangeListener (new PropChangeListener (prop));
         widget = intSlider;
      }
      if (widget != null) {
         widget.setToolTipText (info.getDescription());
         finishWidget (widget, prop);
      }
      return widget;
   }

   public static NumericInterval getNumericRange (Property prop) {
      NumericInterval nrange = null;
      PropertyInfo info = prop.getInfo();
      if (info.isReadOnly() || !info.isSliderAllowed()) {
         return null;
      }
      Class<?> type = info.getValueClass();
      if (type == double.class || type == float.class ||
         type == Double.class || type == Float.class) {
         Object value = prop.get();
         // we can only assign a slider if there is a common numeric value;
         // i.e., if the value is not Void
         if (value instanceof Number) {
            double x = ((Number)value).doubleValue();
            if (info.hasRestrictedRange()) {
               Range range = prop.getRange();
               NumericInterval defaultRange = info.getDefaultNumericRange();
               if (range instanceof NumericInterval) {
                  nrange = new DoubleInterval((NumericInterval)range);
                  if (!nrange.isBounded()) {
                     if (defaultRange != null) {
                        nrange.intersect (defaultRange);
                     }
                  }
                  nrange = SliderRange.estimateBoundsIfNecessary (nrange, x);
               }
               else if (defaultRange != null) {
                  nrange = SliderRange.estimateBoundsIfNecessary (defaultRange, x);
               }
            }
            if (nrange == null) {
               // try to calculate range a from the value
               if (x == -1) {
                  nrange = new DoubleInterval (-1, 1);
               }
               else {
                  nrange = new DoubleInterval (); // interval is unbounded
                  nrange = SliderRange.estimateBoundsIfNecessary (nrange, x);
               }
            }
         }
      } else if (type == int.class || type == Integer.class) {
         Object value = prop.get();
         // we can only assign a slider if there is a common numeric value;
         // i.e., if the value is not Void
         if (value instanceof Number) {
            int x = ((Number)value).intValue();
            if (info.hasRestrictedRange()) {
               Range range = prop.getRange();
               NumericInterval defaultRange = info.getDefaultNumericRange();
               if (range instanceof NumericInterval) {
                  nrange = new IntegerInterval((NumericInterval)range);
                  if (!nrange.isBounded()) {
                     if (defaultRange != null) {
                        nrange.intersect (defaultRange);
                     }
                  }
                  nrange = SliderRange.estimateBoundsIfNecessary (nrange, x);
               }
               else if (defaultRange != null) {
                  nrange = SliderRange.estimateBoundsIfNecessary (defaultRange, x);
               }
            }
         }
      }
      return nrange;
   }

   public static boolean canCreateWithSlider (PropertyInfo info) {
      Class<?> type = info.getValueClass();

      if (info.isReadOnly()) {
         return false;
      }
      if (type == double.class || type == float.class ||
         type == Double.class || type == Float.class ||
         type == int.class || type == Integer.class) {
         return true;
      }
      else {
         return false;
      }
   }

   public static boolean canCreate (PropertyInfo info) {
      Class<?> type = info.getValueClass();

      if (String.class.isAssignableFrom (type) ||
         type == double.class ||
         type == float.class ||
         type == Double.class ||
         type == Float.class ||
         type == int.class ||
         type == Integer.class ||
         type == boolean.class ||
         type == Boolean.class ||
         (VectorBase.class.isAssignableFrom (type) &&
            info.getDimension() != -1) ||
            SymmetricMatrix3d.class.isAssignableFrom (type) ||
            AxisAngle.class.isAssignableFrom (type) ||
            RigidTransform3d.class.isAssignableFrom (type) ||
            Rectangle2d.class.isAssignableFrom(type) ||
            // Material.class.isAssignableFrom (type) ||
            // MuscleMaterial.class.isAssignableFrom (type) ||
            Enum.class.isAssignableFrom (type) ||
            Color.class.isAssignableFrom (type) ||
            NumericInterval.class.isAssignableFrom (type) ||
            GLGridResolution.class.isAssignableFrom (type) ||
            CompositeProperty.class.isAssignableFrom (type)) {
         return true;
      }
      else {
         return false;
      }
   }

   public static LabeledComponentBase create (Property prop) {
      NumericInterval range = getNumericRange (prop);
      if (range != null && range.isClosed()) {
         // range needs to be closed because otherwise it is hard to
         // make the end points conform to slider bounds
         return create (prop, range.getLowerBound(), range.getUpperBound());
      }
      else {
         LabeledComponentBase widget = createWidget (prop);
         if (widget != null) {
            initializeWidget (widget, prop);
            finishWidget (widget, prop);
         }
         return widget;
      }
   }

   public static void updateValue (LabeledControl widget, Property prop) {
      widget.maskValueChangeListeners (true);
      // mask value checks because (a) we assume updated values are valid,
      // and (b) Void values, if present, may not pass the checks
      widget.maskValueChecks (true);
      if (widget instanceof NumericFieldSlider) {
         // do this because we don't want the slider to adjust its range
         // if there is a slight difference between the set and get values.
         ((NumericFieldSlider)widget).setAutoRangingEnabled (false);
      }
      widget.setValue (prop.get());
      if (widget instanceof NumericFieldSlider) {
         ((NumericFieldSlider)widget).setAutoRangingEnabled (true);
      }
      widget.maskValueChecks (false);
      widget.maskValueChangeListeners (false);
      if (prop.getInfo().isInheritable()) {
         PropertyModeButton button = getModeButton (widget);
         if (button != null) {
            button.setMode (((InheritableProperty)prop).getMode());
         }
      }
      // if the widget is a slider, and the property has a range,
      // update the slider to reflect any changes in the range
      if (prop.getInfo().hasRestrictedRange()) {
         if (widget instanceof NumericFieldSlider) {
            Range range = prop.getRange();
            if (range instanceof NumericInterval) {
               ((NumericFieldSlider)widget).setRange ((NumericInterval)range);
            }
         }
         else if (widget instanceof EnumSelector) {
            Range range = prop.getRange();
            if (range instanceof EnumRange) {
               ((EnumSelector)widget).setSelections (
                  ((EnumRange<?>)range).getValidEnums());
            }            
         }
         else if (widget instanceof StringSelector) {
            Range range = prop.getRange();
            if (range instanceof StringRange) {
               String[] values = ((StringRange)range).getValidStrings();
               if (values != null) {
                  ((StringSelector)widget).setSelections (
                     values);
               }
            }
         }
      }

   }

   public static void finishWidget (LabeledComponentBase widget, Property prop) {

      Object value = prop.get();
      PropertyInfo info = prop.getInfo();

      if (widget instanceof LabeledControl) {
         LabeledControl control = (LabeledControl)widget;
         control.setVoidValueEnabled (true);
         control.maskValueChangeListeners (true);
         control.setValue (value);
         control.maskValueChangeListeners (false);
      }
      if (info.isReadOnly()) {
         if (widget instanceof LabeledControl) {
            ((LabeledControl)widget).setEnabledAll (false);
         }
         else if (widget instanceof Component) {
            ((Component)widget).setEnabled (false);
         }
      }
      if (info.hasRestrictedRange()) {
         if (widget instanceof LabeledControl) {
            ((LabeledControl)widget).addValueCheckListener (
               new PropCheckListener (prop));
         }
      }
   }

   /** 
    * Removes any listeners that may have been added to a
    * widget as a result of its being associated with an earlier property.
    */
   private static void removeOldListeners (LabeledControl ctrl) {
      for (ValueChangeListener l : ctrl.getValueChangeListeners()) {
         if (l instanceof PropChangeListener) {
            ctrl.removeValueChangeListener (l);
         }
      }
      for (ValueCheckListener l : ctrl.getValueCheckListeners()) {
         if (l instanceof PropCheckListener) {
            ctrl.removeValueCheckListener (l);
         }
      }
   }

   private static boolean textIsEmpty (String text) {
      return text == null || text.length() == 0;
   }

   private static boolean formatIsDefault(LabeledTextField field) {
      return (field.getFormat().equals (field.getDefaultFormat()));
   }

   public static boolean initializeWidget (
      LabeledComponentBase widget, Property prop) {
      String name = prop.getName();
      PropertyInfo info = prop.getInfo();
      Class<?> type = info.getValueClass();

      // Object value = prop.get();

      if (widget instanceof LabeledWidget) {
         LabeledWidget lwidget = (LabeledWidget)widget;
         if (textIsEmpty (lwidget.getLabelText())) {
            lwidget.setLabelText (name);
         }
         if (textIsEmpty (lwidget.getToolTipText())) {
            lwidget.setToolTipText (info.getDescription());
         }
      }
      if (widget instanceof LabeledControl) {
         removeOldListeners ((LabeledControl)widget);
      }

      try {
         if (String.class.isAssignableFrom (type)) {
            // if String has a range, then use StringSelector
            // otherwise, use a simple StringField
            Range stringRange = prop.getRange ();
            if (info.isInheritable () || stringRange == null 
               || !(stringRange instanceof StringRange)
               || ((StringRange)stringRange).isWildcard ()) {
               StringField stringField = (StringField)widget;
               stringField.setColumns (20);
               stringField.addValueChangeListener (new PropChangeListener (prop) {
                  public void valueChange (ValueChangeEvent e) {
                     if (e.getValue() == null || e.getValue().equals ("")) {
                        super.valueChange (new ValueChangeEvent (
                           e.getSource(), ""));
                     }
                     else {
                        super.valueChange (e);
                     }
                  }
               });
               stringField.setStretchable (true);
            } else {
               String [] constants = ((StringRange)stringRange).getValidStrings();

               StringSelector selector = (StringSelector)widget;
               selector.setSelections (constants, null);
               selector.addValueChangeListener (new PropChangeListener (prop));
            }
         }
         else if (type == double.class || type == float.class ||
            type == Double.class || type == Float.class) {
            DoubleField doubleField = (DoubleField)widget;
            Range range = prop.getRange();
            if (range instanceof NumericInterval) {
               doubleField.setRange ((NumericInterval)range);
            }
            // if ((range = info.getNumericRange()) != null) {
            //    doubleField.setRange (range);
            // }
            if (info.getPrintFormat() != null && 
               formatIsDefault(doubleField)) {
               doubleField.setFormat (info.getPrintFormat());
            }
            GuiUtils.setFixedWidth (doubleField.getTextField(), 100);
            doubleField.addValueChangeListener (new PropChangeListener (prop));
         }
         else if (type == int.class || type == Integer.class) {
            IntegerField intField = (IntegerField)widget;
            GuiUtils.setFixedWidth (intField.getTextField(), 100);
            intField.addValueChangeListener (new PropChangeListener (prop));
         }
         else if (type == boolean.class || type == Boolean.class) {
            if (info.isReadOnly()) {
               StringField stringField = (StringField)widget;
               stringField.setColumns (5);
            }
            else {
               BooleanSelector selector = (BooleanSelector)widget;
               selector.addValueChangeListener (new PropChangeListener (prop));
            }
         }
         else if (VectorBase.class.isAssignableFrom (type) &&
            info.getDimension() != -1) {
            VectorBase resultVec;
            try {
               resultVec = (VectorBase)type.newInstance();
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "Error creating no-args instance of " + type);
            }
            if (resultVec instanceof VectorNd) {
               ((VectorNd)resultVec).setSize(info.getDimension());
            }
            VectorField vectorField = (VectorField)widget;
            // take care not to zorch existing widget values in case
            // the widget is being reinitialized (instead of being created
            // from scratch)            
            if (vectorField.getVectorSize() != info.getDimension()) {
               vectorField.setVectorSize (info.getDimension());
            }
            else {
               VectorNd existingValue = vectorField.getVectorValue();
               if (existingValue != null) {
                  resultVec.set (existingValue);
               }
            }
            vectorField.setResultHolder (resultVec);
            vectorField.addValueChangeListener (new PropChangeListener (prop));
            if (info.getPrintFormat() != null &&
               formatIsDefault (vectorField)) {
               vectorField.setFormat (info.getPrintFormat());
            }
            vectorField.setStretchable (true);
         }
         else if (VectoriBase.class.isAssignableFrom (type) &&
            info.getDimension() != -1) {
            VectoriBase resultVec;
            try {
               resultVec = (VectoriBase)type.newInstance();
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "Error creating no-args instance of " + type);
            }
            if (resultVec instanceof VectorNi) {
               ((VectorNi)resultVec).setSize(info.getDimension());
            }
            VectoriField vectorField = (VectoriField)widget;
            // take care not to zorch existing widget values in case
            // the widget is being reinitialized (instead of being created
            // from scratch)            
            if (vectorField.getVectorSize() != info.getDimension()) {
               vectorField.setVectorSize (info.getDimension());
            }
            else {
               VectorNi existingValue = vectorField.getVectorValue();
               if (existingValue != null) {
                  resultVec.set (existingValue);
               }
            }
            vectorField.setResultHolder (resultVec);
            vectorField.addValueChangeListener (new PropChangeListener (prop));
            if (info.getPrintFormat() != null &&
               formatIsDefault (vectorField)) {
               vectorField.setFormat (info.getPrintFormat());
            }
            vectorField.setStretchable (true);
         }
         else if (VectorBase.class.isAssignableFrom(type) &&
                  info.getDimension() == -1) {
            VectorBase resultVec;
            try {
               resultVec = (VectorBase)type.newInstance();
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "Error creating no-args instance of " + type);
            }
            VariableVectorField vectorField = (VariableVectorField)widget;
            VectorNd existingValue = vectorField.getVectorValue();
            if (vectorField.getVectorSize() != resultVec.size()) {
               resultVec.setSize(vectorField.getVectorSize());
            }
            if (existingValue != null) {
               resultVec.set (existingValue);
            }
            vectorField.setResultHolder (resultVec);
            vectorField.addValueChangeListener (new PropChangeListener (prop));
            if (info.getPrintFormat() != null &&
                formatIsDefault (vectorField)) {
               vectorField.setFormat (info.getPrintFormat());
            }
            vectorField.setStretchable (true);
         }
         else if (SymmetricMatrix3d.class.isAssignableFrom (type)) {
            SymmetricMatrix3dField matrixField = (SymmetricMatrix3dField)widget;
            matrixField.addValueChangeListener (new PropChangeListener (prop));
            if (info.getPrintFormat() != null &&
               formatIsDefault (matrixField)) {
               matrixField.setFormat (info.getPrintFormat());
            }
            matrixField.setStretchable (true);
         }
         else if (RigidTransform3d.class.isAssignableFrom (type)) {
            RigidTransformWidget transformField = (RigidTransformWidget)widget;
            transformField.addValueChangeListener (
               new PropChangeListener (prop));
            transformField.setStretchable (true);
         }
         else if (AffineTransform3d.class.isAssignableFrom (type)) {
            AffineTransformWidget transformField = (AffineTransformWidget)widget;
            transformField.addValueChangeListener (
               new PropChangeListener (prop));
            transformField.setStretchable (true);
         }
         else if (Rectangle2d.class.isAssignableFrom(type)) {
            RectangleField rectField = (RectangleField)widget;
            rectField.addValueChangeListener (new PropChangeListener (prop));
            if (info.getPrintFormat() != null &&
               formatIsDefault (rectField)) {
               rectField.setFormat (info.getPrintFormat());
            }
            rectField.setStretchable (true);
         }
         // else if (Material.class.isAssignableFrom (type)) {
         //    MaterialPanel materialWidget = (MaterialPanel)widget;
         //    materialWidget.initializeSelection (prop);
         //    //materialWidget.setNullAllowed (prop.getInfo().getNullValueOK());
         // }
         // else if (MuscleMaterial.class.isAssignableFrom (type)) {
         //    MuscleMaterialPanel materialWidget = (MuscleMaterialPanel)widget;
         //    materialWidget.initializeSelection (prop);
         //    //materialWidget.setNullAllowed (prop.getInfo().getNullValueOK());
         // }
         // else if (ViscoelasticBehavior.class.isAssignableFrom (type)) {
         //    ViscoelasticBehaviorPanel viscoWidget =
         //       (ViscoelasticBehaviorPanel)widget;
         //    viscoWidget.initializeSelection (prop);
         // }
         else if (AxisAngle.class.isAssignableFrom (type)) {
            AxisAngleField orientationField = (AxisAngleField)widget;
            orientationField.addValueChangeListener (new PropChangeListener (
               prop));
            orientationField.setStretchable (true);
         }
         else if (Enum.class.isAssignableFrom (type)) {
            Enum<?>[] constants = null;
            Range range = prop.getRange();
            if (range != null && range instanceof EnumRange) {
               constants = ((EnumRange<?>)range).getValidEnums();
            }
            else {
               constants = (Enum[])type.getEnumConstants();
            }
            if (info.isReadOnly()) {
               StringField stringField = (StringField)widget;
               int ncols = 0;
               for (int i = 0; i < constants.length; i++) {
                  int len = constants[i].toString().length();
                  if (len > ncols) {
                     ncols = len;
                  }
               }
               stringField.setColumns (ncols);
            }
            else {
               EnumSelector selector = (EnumSelector)widget;
               selector.setSelections (constants, null);
               selector.addValueChangeListener (new PropChangeListener (prop));
            }
         }
         else if (Color.class.isAssignableFrom (type)) {
            ColorSelector selector = (ColorSelector)widget;
            if (info.getNullValueOK()) {
               selector.enableNullColors();
            }
            selector.addValueChangeListener (new PropChangeListener (prop));
         }
         else if (IntegerInterval.class.isAssignableFrom (type)) {
            IntegerIntervalField rangeField = (IntegerIntervalField)widget;
            rangeField.addValueChangeListener (new PropChangeListener (prop));
            rangeField.setStretchable (true);
         }
         else if (NumericInterval.class.isAssignableFrom (type)) {
            DoubleIntervalField rangeField = (DoubleIntervalField)widget;
            rangeField.addValueChangeListener (new PropChangeListener (prop));
            rangeField.setStretchable (true);
         }
         else if (GLGridResolution.class.isAssignableFrom (type)) {
            GridResolutionField resField = (GridResolutionField)widget;
            resField.addValueChangeListener (new PropChangeListener (prop));
         }
         else if (Font.class.isAssignableFrom (type)) {
            FontField fontField = (FontField)widget;
            fontField.addValueChangeListener (new PropChangeListener (prop));
         }
         else if (CompositeProperty.class.isAssignableFrom (type)) {
            if (widget instanceof CompositePropertyWidget) {
               CompositePropertyWidget compProp = (CompositePropertyWidget)widget;
               compProp.setProperty (prop);
            }
            else {
               CompositePropertyPanel compProp = (CompositePropertyPanel)widget;
               compProp.setExpandState (info.getWidgetExpandState());
               compProp.initializeSelection (prop);
            }
         }
         else {
            return false;
         }
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException (
            "widget type "+widget.getClass()+
            " inappropriate for property type "+type);
      }
      // finishWidget (widget, prop);
      return true;
   }

   protected static LabeledComponentBase createWidget (Property prop) {
      PropertyInfo info = prop.getInfo ();
      Class<?> type = info.getValueClass();

      if (String.class.isAssignableFrom (type)) {
         Range range = prop.getRange ();
         if (info.isReadOnly () || range == null || 
            !(range instanceof StringRange)
            || ((StringRange)range).isWildcard ()) {
            return new StringField();   
         } else {
            return new StringSelector();
         }

      }
      else if (type == double.class || type == float.class ||
         type == Double.class || type == Float.class) {
         return new DoubleField();
      }
      else if (type == int.class || type == Integer.class) {
         return new IntegerField();
      }
      else if (type == boolean.class || type == Boolean.class) {
         if (info.isReadOnly()) {
            return new StringField();
         }
         else {
            return new BooleanSelector();
         }
      }
      else if (VectorBase.class.isAssignableFrom (type)) {
         if (VectorNd.class.isAssignableFrom(type) ||
            info.getDimension() == -1) {
            return new VariableVectorField();
         }
         else {
            return new VectorField();
         }
      }
      else if (VectoriBase.class.isAssignableFrom (type) &&
               info.getDimension() != -1) {
         return new VectoriField();
      }
      else if (SymmetricMatrix3d.class.isAssignableFrom (type)) {
         return new SymmetricMatrix3dField();
      } 
      else if (RigidTransform3d.class.isAssignableFrom (type)) {
         return new RigidTransformWidget();
      }
      else if (AffineTransform3d.class.isAssignableFrom (type)) {
         return new AffineTransformWidget();
      }
      // else if (Material.class.isAssignableFrom (type)) {
      //    return new MaterialPanel("material", info.getNullValueOK());
      // }
      // else if (MuscleMaterial.class.isAssignableFrom (type)) {
      //    return new MuscleMaterialPanel("muscleMaterial", info.getNullValueOK());
      // }
      // else if (ViscoelasticBehavior.class.isAssignableFrom (type)) {
      //     return new ViscoelasticBehaviorPanel (
      //        "viscoBehavior", info.getNullValueOK());
      // }
      else if (Rectangle2d.class.isAssignableFrom(type)) {
         return new RectangleField();
      }
      else if (AxisAngle.class.isAssignableFrom (type)) {
         return new AxisAngleField();
      }
      else if (Enum.class.isAssignableFrom (type)) {
         if (info.isReadOnly()) {
            return new StringField();
         }
         else {
            return new EnumSelector();
         }
      }
      else if (Color.class.isAssignableFrom (type)) {
         return new ColorSelector();
      }
      else if (IntegerInterval.class.isAssignableFrom (type)) {
         return new IntegerIntervalField();
      }
      else if (NumericInterval.class.isAssignableFrom (type)) {
         return new DoubleIntervalField();
      }
      else if (GLGridResolution.class.isAssignableFrom (type)) {
         return new GridResolutionField();
      }
      else if (Font.class.isAssignableFrom (type)) {
         return new FontField ();
      }
      else if (CompositeProperty.class.isAssignableFrom (type)) {
         Class<?>[] subclasses = 
            PropertyUtils.findCompositePropertySubclasses(info);
         if (subclasses != null) {
            return new CompositePropertyPanel();   
         }
         else {
            return new CompositePropertyWidget();   
         }
         // if (info.getWidgetType() == PropertyInfo.WidgetType.Panel) {
         //    return new CompositePropertyWidget();            
         // }
         // else {
         //    return new CompositePropertyWidget();
         // }
      }
      else {
         return null;
      }
   }

   public static boolean hasModeButton (Component comp) {
      if (comp instanceof LabeledControl) {
         return getModeButton ((LabeledControl)comp) != null;
      }
      else {
         return false;
      }
   }

   public static PropertyModeButton getModeButton (Component comp) {

      if (comp instanceof LabeledControl) {
         LabeledControl widget = (LabeledControl)comp;
         if (widget.numMajorComponents() >= 1 &&
            (widget.getMajorComponent (0) instanceof PropertyModeButton)) {
            return (PropertyModeButton)widget.getMajorComponent (0);
         }
      }
      return null;
   }

   public static void setSynchronizeObject (
      JComponent widget, Object syncObj) {
      if (widget instanceof LabeledControl) {
         LabeledControl control = (LabeledControl)widget;
         for (ValueChangeListener l : control.getValueChangeListeners()) {
            if (l instanceof PropChangeListener) {
               ((PropChangeListener)l).setSynchronizeObject (syncObj);
            }
         }
      }
   }

   public static Property getProperty (LabeledComponentBase widget) {
      if (widget instanceof LabeledControl) {
         LabeledControl control = (LabeledControl)widget;
         for (ValueChangeListener l : control.getValueChangeListeners()) {
            if (l instanceof PropChangeListener) {
               return ((PropChangeListener)l).getProperty();
            }
         }
      }
      else if (widget instanceof CompositePropertyWidget) {
         return ((CompositePropertyWidget)widget).getProperty();
      }
      else if (widget instanceof CompositePropertyPanel) {
         return ((CompositePropertyPanel)widget).getProperty();
      }
      // else if (widget instanceof MaterialPanel) {
      //    return ((MaterialPanel)widget).getProperty();
      // }
      // else if (widget instanceof MuscleMaterialPanel) {
      //    return ((MuscleMaterialPanel)widget).getProperty();
      // }
      // else if (widget instanceof ViscoelasticBehaviorPanel) {
      //     return ((ViscoelasticBehaviorPanel)widget).getProperty();
      // }
      return null;
   }

   public static PropertyModeButton addModeButtonIfNecessary (
      LabeledControl widget, InheritableProperty prop) {
      // add a property mode button to a widget if it does not already have one
      Component comp = widget.getMajorComponent(0);
      PropertyModeButton button = null;
      if (!(comp instanceof PropertyModeButton)) {
         button = new PropertyModeButton (widget, prop);
         widget.addMajorComponent (button, 0);
         widget.addValueChangeListener (new SetHandler (button));
      }
      return button;
   }

   public static void addModeButtonSpace (LabeledComponentBase widget) {
      if (widget instanceof LabeledWidget) {
         LabeledWidget lwidget = (LabeledWidget)widget;
         LabelSpacing spacing = new LabelSpacing();
         lwidget.getLabelSpacing (spacing);
         spacing.preSpacing = PropertyModeButton.getButtonSize().width;
         lwidget.setLabelSpacing (spacing);
         //          ((LabeledWidget)widget).setPrelabelSpacing (
         //             PropertyModeButton.getButtonSize().width);
         LabeledComponentBase.alignAllLabels(widget);
      }
   }

   public static void addModeButtonOrSpace (
      LabeledComponentBase widget, Property prop) {

      // ignore if is a type of panel
      if (widget instanceof CompositePropertyPanel) {
         return;
      }
      if (prop.getInfo().isInheritable()) {
         if (!(widget instanceof LabeledControl)) {
            throw new InternalErrorException (
               "Inheritable property widget is not a LabeledControl");
         }
         addModeButtonIfNecessary ((LabeledControl)widget, (InheritableProperty)prop);
      }
      else {
         addModeButtonSpace (widget);
      }
   }

   public static void setModeAppearance (
      LabeledControl control, Container parent, PropertyMode mode) {
      switch (mode) {
         case Explicit: {
            // control.setBackground (null);
            control.setBorder (new EmptyBorder (1, 1, 1, 1));
            break;
         }
         case Inherited: {
            // control.setBackground (null);
            control.setBorder (new ThinBevelBorder (BevelBorder.RAISED));
            break;
         }
         case Void: {
            // control.setBackground (null);
            control.setBorder (new EmptyBorder (1, 1, 1, 1));
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled property mode: "
               + mode);
         }
      }
   }

}

class SetHandler implements ValueChangeListener {
   private PropertyModeButton myButton;

   SetHandler (PropertyModeButton button) {
      myButton = button;
   }

   public void valueChange (ValueChangeEvent e) {
      LabeledControl control = (LabeledControl)e.getSource();
      if (!myButton.isSelected()) {
         myButton.setSelected (true);
         PropertyWidget.setModeAppearance (
            control, control.getParent(), PropertyMode.Explicit);
      }
   }
}
