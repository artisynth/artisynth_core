/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;

import javax.swing.*;

import maspack.matrix.VectorNd;

public class LabeledControlVoidDemo extends LabeledComponentPanel {
   private static final long serialVersionUID = 1L;
   VectorNd xyz = new VectorNd (3);
   VectorNd abc = new VectorNd (3);
   float[] favoriteColor = new float[4];

   enum Planets {
      MERCURY, VENUS, EARTH, MARS, JUPITER, SATURN, URANUS, NEPTUNE, PLUTO
   }

   public LabeledControlVoidDemo() {
      addWidget (new BooleanSelector ("one check box:"));
      addWidget (new StringField ("your name?", 16));
      addWidget (new DoubleField ("your weight (lbs)?"));
      addWidget (new VectorField ("initial position:", 3));
      addWidget (new VectorMultiField ("another position:", 3));
      addWidget (new IntegerField ("matrix size"));
      addWidget (new DoubleFieldSlider ("easily adjusted weight:", 0, 300));
      addWidget (new IntegerSelector (
         "your favorite fruit?", new int[] { 10, 20, 30 },
         new String[] { "apples", "pears", "oranges" }));

      // addWidget (new FileNameField ("pick a file:", 30));

      addWidget (new IntegerSelector ("pick a baud rate:", new int[] { 300, 700,
                                                                  1400, 2800,
                                                                  4800, 9600 }));

      addWidget (new StringSelector ("engine type:", new String[] { "jet",
                                                                   "ram jet",
                                                                   "rocket" }));

      addWidget (new DoubleSlider ("range", 1, 10));

      IntegerSlider intSlider = new IntegerSlider ("level", 0, 50);
      intSlider.setLabels (10);
      addWidget (intSlider);
      addWidget (new ColorSelector ("your favorite color?"));
      addWidget (new ColorSelector ("next favorite color?", null));
      ColorSelector csel = new ColorSelector ("and a third?");
      csel.enableNullColors();
      addWidget (csel);
      addWidget (new DoubleIntervalField ("range"));
      addWidget (new JSeparator());

      IntegerFieldSlider volume =
         new IntegerFieldSlider ("this volume goes to 10 ...", 0, 10);
      volume.setLabels (1);
      volume.setMajorTickSpacing (1);
      volume.setPaintTicks (true);
      addWidget (volume);
      addWidget (new DoubleSelector (
         "symbolic constant", new double[] { Math.PI, Math.E, 1.6180339 },
         new String[] { "e", "pi", "phi" }));

      addWidget (new EnumSelector (
         "pick a planet:", new Enum[] { Planets.MERCURY, Planets.EARTH,
                                       Planets.MARS }));
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("labled component demo");
      LabeledControlVoidDemo demoPanel = new LabeledControlVoidDemo();
      frame.getContentPane().add (demoPanel);
      frame.pack();
      frame.setVisible (true);
   }
}
