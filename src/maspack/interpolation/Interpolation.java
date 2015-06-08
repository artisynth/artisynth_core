/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import java.io.*;
import maspack.util.*;

/**
 * Describes how a numeric list should be interpolated.
 */
public class Interpolation implements Scannable {
   protected Order myOrder = Order.Linear;
   protected boolean myDataExtendedP = true;

   /**
    * Order of the interpolation.
    */
   public enum Order {

      /**
       * Values are set to the values of the closest previous knots points.
       */
      Step,

      /**
       * Values are set by linear interpolation of the closest surrounding knot
       * points.
       */
      Linear,

      /**
       * Values are set by quadartic interpolation of the surrounding knot
       * points.
       */
      Parabolic,

      /**
       * Values are set by cubic Catmull interpolation between the surrounding
       * knot points.
       */
      Cubic,
      
      /**
       * Values are set by cubic hermite interpolation between the surrounding
       * knot points. Slopes are set to zero.
       */
      CubicStep,

      /**
       * Where appropriate, 3D rotation values are set by piecewise spherical
       * linear interpolation (i.e., "slerp", as described by Shoemake's 1985
       * SIGGRAPH paper).  Otherwise, interpolation is linear.
       */
      SphericalLinear,

      /**
       * Where appropriate, 3D rotation values are set by spherical
       * cubic interpolation (i.e., "slerp", as described by Shoemake's 1985
       * SIGGRAPH paper).  Otherwise, interpolation is cubic.
       */
      SphericalCubic;
      
      public static Order fromString (String str) {
         try {
            return valueOf (str); 
         }
         catch (Exception e) {
            return null;
         }
      }
         
   };

   /**
    * Creates a new Interpolation with LINEAR interpolation and no extension of
    * end data.
    */
   public Interpolation() {
      myOrder = Order.Linear;
      setDataExtended (false);
   }

   /**
    * Creates a new Interpolation which is a copy of an existing one.
    * 
    * @param interp
    * interpolation to copy
    */
   public Interpolation (Interpolation interp) {
      this();
      set (interp);
   }

   /**
    * Creates a new Interpolation with a specified order and data extension
    * policy.
    * 
    * @param order
    * order to the interpolation
    * @param extendEnd
    * if true, causes data to be extended past the last knot point
    */
   public Interpolation (Order order, boolean extendEnd) {
      this();
      setOrder (order);
      setDataExtended (extendEnd);
   }

   /**
    * Sets this interpolation to the value of another interpolation.
    * 
    * @param interp
    * interpolation to copy
    */
   public void set (Interpolation interp) {
      myOrder = interp.myOrder;
      myDataExtendedP = interp.myDataExtendedP;
   }

   /**
    * Sets the interpolation order. The default is <code>Linear</code>.
    * 
    * @param order
    * interpolation order
    */
   public void setOrder (Order order) {
      myOrder = order;
   }

   /**
    * Returns the interpolation order for this list.
    * 
    * @return interpolation order
    * @see #setOrder
    */
   public Order getOrder() {
      return myOrder;
   }

   /**
    * Enables the extension of data values beyond the last knot point. If
    * enabled, values beyond the last knot point are set to those of the last
    * knot point. Otherwise, these values are set to zero.
    * 
    * @param enable
    * if true, enables extension of data values.
    */
   public void setDataExtended (boolean enable) {
      myDataExtendedP = enable;
   }

   /**
    * Returns true if the extension of data values beyond the last knot point is
    * enabled. See {@link #setDataExtended setDataExtended} for details.
    * 
    * @return true if the extension of data values is enabled.
    */
   public boolean isDataExtended() {
      return myDataExtendedP;
   }

   public String toString () {
      return "[ " + myOrder + " " + myDataExtendedP + " ]";
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
   throws IOException {
      pw.println (toString());
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      String order = rtok.scanWord();
      boolean found = false;
      for (Order o : Order.values()) {
         if (order.equals (o.name())) {
            setOrder (o);
            found = true;
            break;
         }
      }
      if (!found) {
         throw new IOException (
            "Invalid order specification " + order);
      }
      myDataExtendedP = rtok.scanBoolean();
      rtok.scanToken (']');
   }

}
