/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.ArrayList;

import javax.swing.*;

import maspack.matrix.VectorNd;
import maspack.matrix.RigidTransform3d;
import maspack.util.BooleanHolder;
import maspack.util.DoubleHolder;
import maspack.util.IntHolder;
import maspack.util.IntegerInterval;
import maspack.util.StringHolder;
import maspack.properties.Property;

public class LabeledControlDemo extends JFrame implements ActionListener,
ValueChangeListener {
   private static final long serialVersionUID = 1L;
   BooleanHolder fooHolder = new BooleanHolder();
   BooleanHolder barHolder = new BooleanHolder();
   IntHolder modeHolder = new IntHolder (20);
   DoubleHolder weightHolder = new DoubleHolder (160.0);
   DoubleHolder newWeightHolder = new DoubleHolder (160.0);
   DoubleHolder tempHolder = new DoubleHolder (5);
   StringHolder yourNameHolder = new StringHolder ("charlie");
   // StringHolder fileNameHolder = new StringHolder (
   // "/ai/lloyd/artisynth/design");
   IntHolder baudRate = new IntHolder (1400);
   VectorNd xyz = new VectorNd (3);
   VectorNd abc = new VectorNd (3);
   float[] favoriteColor = new float[4];
   IntHolder hamburgersSold = new IntHolder();
   LabeledComponentPanel panel;
   OptionPanel optionPanel;
   JButton voidButton;
   ArrayList<Object> initialValues;
   ArrayList<LabeledControl> widgets;

   enum Planets {
      MERCURY, VENUS, EARTH, MARS, JUPITER, SATURN, URANUS, NEPTUNE, PLUTO
   }

   public LabeledControlDemo() {
      super ("Labeled component demo");
      getContentPane().setLayout (
         new BoxLayout (getContentPane(), BoxLayout.Y_AXIS));

      panel = new LabeledComponentPanel();

      BooleanSelector bsel =
         new BooleanSelector ("one check box:", fooHolder.value);
      bsel.setResultHolder (fooHolder);
      panel.addWidget (bsel);

      bsel = new BooleanSelector ("and another ...", barHolder.value);
      bsel.setResultHolder (barHolder);
      panel.addWidget (bsel);

      StringField sfield =
         new StringField ("your name?", yourNameHolder.value, 16);
      sfield.setResultHolder (yourNameHolder);
      panel.addWidget (sfield);

      DoubleField dfield =
         new DoubleField ("your weight (lbs)?", weightHolder.value, "%8.3f");
      dfield.setResultHolder (weightHolder);
      dfield.setRange (0, 300);
      dfield.setGUIVoidEnabled (true);
      panel.addWidget (dfield);

      panel.addWidget (new DoubleFieldSlider (
         "easily adjusted weight:", 100, 0, 300));

      IntegerSelector isel =
         new IntegerSelector (
            "your favorite fruit?", modeHolder.value, new int[] { 10, 20, 30 },
            new String[] { "apples", "pears", "oranges" });
      isel.setResultHolder (modeHolder);
      panel.addWidget (isel);

      DoubleFieldSlider dfs =
         new DoubleFieldSlider ("ideal room temp:", -10, -40, 40);
      dfs.setLabels ("%4.0f", 20);
      dfs.setMajorTickSpacing (5);
      dfs.setPaintTicks (true);
      dfs.setRoundingTolerance (0.1);
      // dfs.getSlider().setExtent(300);
      panel.addWidget (dfs);

      isel =
         new IntegerSelector (
            "pick a baud rate:", baudRate.value, new int[] { 300, 700, 1400,
                                                            2800, 4800, 9600 });
      isel.setResultHolder (baudRate);
      panel.addWidget (isel);

      VectorField vecField =
         new VectorField ("initial position:", xyz, "%6.2f");
      vecField.setRange (-100, 100);
      vecField.setGUIVoidEnabled (true);
      panel.addWidget (vecField);

      VectorMultiField vecFieldX =
         new VectorMultiField ("another position:", abc, "%6.2f");
      vecFieldX.setGUIVoidEnabled (true);
      vecFieldX.setRange (-100, 100);
      panel.addWidget (vecFieldX);

      DoubleIntervalField drange = new DoubleIntervalField ("range");
      drange.setGUIVoidEnabled (true);
      panel.addWidget (drange);

      IntegerIntervalField irange = new IntegerIntervalField ("int range", new IntegerInterval());
      panel.addWidget (irange);

      panel.addWidget (new StringSelector (
         "engine type:", "jet", new String[] { "jet", "ram jet", "rocket" }));

      ColorSelector csel =
         new ColorSelector ("your favorite color?", Color.ORANGE);
      csel.setResultHolder (favoriteColor);
      panel.addWidget (csel);

      panel.addWidget (new ColorSelector ("color + null color:", null));

      panel.addWidget (new JSeparator());

      IntegerField burgers =
         new IntegerField ("hamburgers sold:", hamburgersSold.value, "%d");
      burgers.setResultHolder (hamburgersSold);
      burgers.setRange (0, 1000000000);
      panel.addWidget (burgers);

      IntegerField unlimited = new IntegerField ("unlimited:");
      panel.addWidget (unlimited);

      panel.addWidget (new IntegerField ("magic flags:", 0, "%#x"));

      IntegerFieldSlider volume =
         new IntegerFieldSlider ("this volume goes to 10 ...", 0, 0, 10, "%d");
      volume.setLabels (1);
      volume.setMajorTickSpacing (1);
      volume.setPaintTicks (true);
      panel.addWidget (volume);

      panel.addWidget (new IntegerFieldSlider (
         "and this one goes to 11!", 0, 0, 11, "%d"));

      panel.addWidget (new GridResolutionField ("resolution"));

      panel.addWidget (new JSeparator());

      IntegerSlider islider = new IntegerSlider ("solo int slider", 0, 10);
      islider.setLabels (1);
      islider.setMajorTickSpacing (1);
      islider.setPaintTicks (true);
      panel.addWidget (islider);

      DoubleSlider sds = new DoubleSlider ("solo double slider", -40, 40);
      sds.setLabels ("%4.0f", 20);
      sds.setMajorTickSpacing (20);
      sds.setMajorTickSpacing (5);
      sds.setPaintTicks (true);
      sds.setRoundingTolerance (0.1);
      panel.addWidget (sds);

      DoubleSelector dsel =
         new DoubleSelector (
            "symbolic constant", new double[] { Math.PI, Math.E, 1.6180339 },
            new String[] { "e", "pi", "phi" });
      // dsel.setGUIVoidEnabled (true);
      panel.addWidget (dsel);

      panel.addWidget (new DoubleSelector (
         "numeric constant", new double[] { Math.PI, Math.E, 1.6180339 },
         "%10.8f"));

      EnumSelector esel =
         new EnumSelector (
            "pick a planet:", Planets.EARTH, new Enum[] { Planets.MERCURY,
                                                         Planets.EARTH,
                                                         Planets.MARS });
      esel.setGUIVoidEnabled (true);
      panel.addWidget (esel);

      panel.addWidget (new EnumSelector (
         "pick another planet:", Planets.SATURN, null));

      panel.addWidget (new RigidTransformWidget (
         "rigid transform:", RigidTransform3d.IDENTITY));

      panel.setAlignmentX (Component.LEFT_ALIGNMENT);

      initialValues = new ArrayList<Object>();
      widgets = new ArrayList<LabeledControl>();
      for (Component comp : panel.getWidgets()) {
         if (comp instanceof LabeledControl) {
            LabeledControl c = (LabeledControl)comp;
            widgets.add (c);
            c.setVoidValueEnabled (true);
            c.addValueChangeListener (this);
            initialValues.add (c.getValue());
         }
      }

      getContentPane().add (panel);

      JSeparator sep = new JSeparator();
      sep.setAlignmentX (Component.LEFT_ALIGNMENT);
      getContentPane().add (GuiUtils.createBoxFiller());
      getContentPane().add (sep);

      optionPanel = new OptionPanel ("Void Done", this);
      voidButton = optionPanel.getButton ("Void");
      optionPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      getContentPane().add (optionPanel);
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("Done")) {
         System.exit (0);
      }
      else if (actionCmd.equals ("Void")) {
         voidButton.setText ("Reset");
         voidButton.setActionCommand ("Reset");
         for (int i = 0; i < widgets.size(); i++) {
            LabeledControl widget = widgets.get (i);
            setValueTest (widget, Property.VoidValue);
         }
      }
      else if (actionCmd.equals ("Reset")) {
         voidButton.setText ("Void");
         voidButton.setActionCommand ("Void");
         for (int i = 0; i < widgets.size(); i++) {
            setValueTest (widgets.get (i), initialValues.get (i));
         }
      }
   }

   private void setValueTest (LabeledControl widget, Object value) {
      myValueChangeCnt = 0;
      widget.setValue (value);
      if (myValueChangeCnt != 1) {
         System.out.println ("setValue: change cnt=" + myValueChangeCnt
         + ", widget " + widget.getLabelText());
      }
      myValueChangeCnt = 0;
      widget.setValue (value);
      if (myValueChangeCnt != 0) {
         System.out.println ("setValue twice: change cnt=" + myValueChangeCnt
         + ", widget " + widget.getLabelText());
      }
   }

   int myValueChangeCnt = 0;

   public void valueChange (ValueChangeEvent e) {
      myValueChangeCnt++;
   }

   public static void main (String[] args) {
      LabeledControlDemo demoPanel = new LabeledControlDemo();
      demoPanel.pack();
      demoPanel.setVisible (true);

      // demoPanel.setFocusCycleRoot(true);
      // System.out.println ("isRoot=" + demoPanel.isFocusCycleRoot());
      // demoPanel.setFocusTraversalPolicy(
      // new ContainerOrderFocusTraversalPolicy());
      // FocusTraversalPolicy ftp =
      // demoPanel.getFocusTraversalPolicy();
      // Component comp = ftp.getFirstComponent(demoPanel);
      // Component comp0 = comp;
      // do
      // { System.out.println (comp.getClass().getName() + " " +
      // comp.isFocusable());
      // if (comp instanceof Container)
      // { Container cont = (Container)comp;
      // System.out.println (" " + cont.getFocusTraversalPolicy());
      // }
      // comp = ftp.getComponentAfter (demoPanel, comp);
      // }
      // while (comp != null && comp != comp0);

   }
}
