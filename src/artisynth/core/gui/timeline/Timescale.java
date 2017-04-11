package artisynth.core.gui.timeline;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.math.BigDecimal;

import artisynth.core.util.TimeBase;

public class Timescale extends JSlider {
   private TimelineController controller;
   private TimescaleUI timescaleUI;
   private BigDecimal[] scaleLabelList;

   static int HEIGHT = 27; // height of the timescale (hard wired)
   
   static final double USEC_TO_SEC = 1e-6;
   static final double SEC_TO_USEC = 1e6;

   // ===================================================
   // Table for implementing "zooming"
   private static final int[] MAJOR_INCREMENT_IN_UNIT = { 
      10000000, // 100 msec/pixel
      5000000,  // 50 msec/pixel
      2000000,  // 20 msec/pixel
      1000000,  // 10 msec/pixel
      500000,   // 5 msec/pixel
      200000,   // 2 msec/pixel
      100000,   // 1 msec/pixel
      50000,    // 500 usec/pixel
      20000,    // 200 usec/pixel
      10000,    // 100 usec/pixel
      5000,     // 50 usec/pixel
      2000,     // 20 usec/pixel
      1000      // 10 usec/pixel
   };

   private static final int[] MINOR_INCREMENT_IN_UNIT = { 
      2000000,  // 100 msec/pixel
      1000000,  // 50 msec/pixel
      400000,   // 20 msec/pixel
      200000,   // 10 msec/pixel
      100000,   // 5 msec/pixel
      40000,    // 2 msec/pixel
      20000,    // 1 msec/pixel
      10000,    // 500 usec/pixel
      4000,     // 200 usec/pixel
      2000,     // 100 usec/pixel
      1000,     // 50 usec/pixel
      400,      // 20 usec/pixel
      200       // 10 usec/pixel
   };

   private static final double[] MAJOR_INCREMENT_IN_TIME = { 
      10, 5, 2, 1, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01, 0.005, 0.002, 0.001};

   private static final int[] LENGTH_IN_PIXEL_WITHOUT_OFFSET = { 
      10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000};

   private static final long serialVersionUID = 1L;

   /**
    * Constructor.
    * 
    * @param frame Timeline window
    * @param durationInSec desired duration in seconds
    * @param zoomLevel Initial zoom level, must be in interval from 0 to
    * {@link #getMaximumZoomLevel()}
    */
   public Timescale (TimelineController frame, int durationInSec, int zoomLevel) {
      setOrientation (HORIZONTAL);
      setMinimum (0);
      setMaximum (1000000);
      setMajorTickSpacing (100000);
      setPaintTicks (true);
      setPaintLabels (false);
      setValue (0);

      Dimension size = new Dimension (getMaximumSize());
      size.height = HEIGHT;
      setSize (size);
      setMaximumSize (size);
      setMinimumSize (size);
      setPreferredSize (size);

      setAlignmentX (JComponent.LEFT_ALIGNMENT);
      setBackground (GuiStorage.COLOR_TIMESCALE_BACKGROUND);

      controller = frame;
      timescaleUI = new TimescaleUI (this);
      setUI (timescaleUI);

      updateTimescaleSizeAndScale (durationInSec, zoomLevel);
   } 

   /**
    * Updates the size and scale of this Timescale
    * 
    * @param durationInSec duration in seconds
    * @param zoomLevel Initial zoom level, must be in interval from 0 to
    * {@link #getMaximumZoomLevel()}
    */
   public void updateTimescaleSizeAndScale (int durationInSec, int zoomLevel) {
      int totalPixelLength =
         computeExactLengthInPixel (durationInSec, zoomLevel);

      Dimension size = new Dimension (totalPixelLength, getHeight());

      setSize (size);
      setMinimumSize (size);
      setMaximumSize (size);
      setPreferredSize (size);

      // Reset the maximum relative to the duration in seconds
      setMinimum (0);
      setMaximum (durationInSec * 1000000);

      // Reset the major and minor increments of the timescale
      setMajorTickSpacing (MAJOR_INCREMENT_IN_UNIT[zoomLevel]);
      setMinorTickSpacing (MINOR_INCREMENT_IN_UNIT[zoomLevel]);

      updateScaleLabel (zoomLevel);
   }

   /**
    * @param zoomLevel Zoom level, must be in interval from 0 to 
    * {@link #getMaximumZoomLevel()}
    */
   public void updateScaleLabel (int zoomLevel) {
      int listSize = (getMaximum() / getMajorTickSpacing()) + 1;
      if (listSize < 1) {
         listSize = 1;
      }

      BigDecimal[] newList = new BigDecimal[listSize];
      double timeLabel = 0;

      for (int i = 0; i < listSize; i++) {
         newList[i] = new BigDecimal (timeLabel);
         newList[i] =
            newList[i].setScale (3, BigDecimal.ROUND_HALF_UP);
         timeLabel += MAJOR_INCREMENT_IN_TIME[zoomLevel];
      }
      scaleLabelList = newList;
   }

   /**
    * Computes the exact length of this TimeScale in pixels
    * 
    * @param duration time duration
    * @param zoomLevel Zoom level, must be in interval from 0 to 
    * {@link #getMaximumZoomLevel()}
    * 
    * @return exact length
    */
   public int computeExactLengthInPixel (int duration, int zoomLevel) {
      int lengthWithoutOffset =
         duration * LENGTH_IN_PIXEL_WITHOUT_OFFSET[zoomLevel];
      int frontOffset = getCorrespondingPixel (getMinimumTime());
      int backOffset =
         getWidth() - getCorrespondingPixel (getMaximumTime());

      return lengthWithoutOffset + frontOffset + backOffset - 1;
   }

   public boolean isTimeManuallyDragged() {
      return timescaleUI.isDragging();
   }

   public double getMaximumTime() {
      return USEC_TO_SEC*getMaximum();
   }

   public double getMinimumTime() {
      return USEC_TO_SEC*getMinimum();
   }

   /**
    * Returns maximum zoom level, which can be applied to this Timescale. Value
    * is from range 0 to {@link #LENGTH_IN_PIXEL_WITHOUT_OFFSET}.length - 1
    * 
    * @return maximum zoom level of this Timescale
    */
   public static int getMaximumZoomLevel() {
      return LENGTH_IN_PIXEL_WITHOUT_OFFSET.length - 1;
   }

   public void updateTimeCursor (double t) {
      int newValue = (int)Math.rint (SEC_TO_USEC*t);
      if (newValue != getValue()) {
         setValue (newValue);
      }
   }

   public double getTimescaleCursorTime() {
      return USEC_TO_SEC*getValue();
   }

   public int getCurrentPixel() {
      return timescaleUI.xPositionForValue (getValue());
   }

   public int getCorrespondingPixel (double time) {
      return timescaleUI.xPositionForValue ((int)(SEC_TO_USEC*time));
   }

   public double getCorrespondingTime (int pixel) {
      return USEC_TO_SEC*timescaleUI.valueForXPosition (pixel);
   }

   Graphics[] gbusy = new Graphics[2];

   public void paint (Graphics g) {

      int tidx = 0;
      Thread thr = Thread.currentThread();
      if (thr.toString().equals ("Thread[AWT-EventQueue-0,6,main]")) {
         tidx = 0;
      }
      else {
         tidx = 1;
      }
      if (gbusy[tidx] != null) {
         System.out.println ("Wow!!");
      }
      gbusy[tidx] = g;
      super.paint (g);

      int range[] = controller.getVisibleBound();
      int minVisibleValue = timescaleUI.valueForXPosition (range[0]);
      int maxVisibleValue = timescaleUI.valueForXPosition (range[1]);
      int minVisibleScaleIndex = minVisibleValue / getMajorTickSpacing();
      int maxVisibleScaleIndex = maxVisibleValue / getMajorTickSpacing();

      // Safeguard against access to out of bound index
      if (maxVisibleScaleIndex > scaleLabelList.length - 1) {
         maxVisibleScaleIndex = scaleLabelList.length - 1;
      }

      // Paint the track
      g.setColor (Color.LIGHT_GRAY);
      g.drawLine (range[0], 2, range[1], 2);
      g.setColor (Color.BLACK);
      g.drawLine (range[0], 3, range[1], 3);
      g.setColor (Color.WHITE);
      g.drawLine (range[0], 5, range[1], 5);

      // Paint the labels and the scale markers
      g.setColor (Color.BLACK);
      g.setFont (GuiStorage.TIMESCALE_FONT);
      for (int i = minVisibleScaleIndex; i <= maxVisibleScaleIndex; i++) {
         if (scaleLabelList[i] != null) {
            g.drawString (scaleLabelList[i].toString(), i * 100 - 7, 21);
         }
         g.drawLine (i * 100 + 5, 7, i * 100 + 5, 11);
         g.drawLine (i * 100 + 5, 23, i * 100 + 5, 26);

         // Draw minor increment markers
         for (int j = 1; j <= 4; j++) {
            g.drawLine (i * 100 + 5 + j * 20, 8, i * 100 + 5 + j * 20, 11);
         }
      }
      gbusy[tidx] = null;
   }

   private class TimescaleUI extends BasicSliderUI {
      public TimescaleUI (JSlider slider) {
         super (slider);
      }

      public int xPositionForValue (int value) {
         return super.xPositionForValue (value);
      }

      public boolean isDragging() {
         return super.isDragging();
      }

      public void paintTicks (Graphics g) {}

      public void paintTrack (Graphics g) {}
   }
}
