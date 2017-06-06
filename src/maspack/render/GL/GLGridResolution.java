/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import maspack.util.NumberFormat;

public class GLGridResolution {
   private double myMajorCellSize = 1;
   private int myNumDivisions = 1;

   public GLGridResolution (double majorCellSize, int numDivisions) {
      set (majorCellSize, numDivisions);
   }

   public GLGridResolution (GLGridResolution res) {
      set (res.myMajorCellSize, res.myNumDivisions);
   }

   public void set (GLGridResolution res) {
      set (res.getMajorCellSize(), res.getNumDivisions());
   }

   /**
    * Sets the resolution.
    */
   public void set (double majorCellSize, int numDivisions) {
      if (majorCellSize < 0) {
         throw new IllegalArgumentException (
            "cell size is "+majorCellSize+"; must not be negative");
      }
      if (numDivisions < 1) {
         throw new IllegalArgumentException (
            "num divisions is "+numDivisions+"; must be >= 1");
      }
      if (majorCellSize != myMajorCellSize) {
         myMajorCellSize = majorCellSize;
      }
      if (numDivisions != myNumDivisions) {
         myNumDivisions = numDivisions;
      }
   }

   public double getMajorCellSize() {
      return myMajorCellSize;
   }

   public int getNumDivisions() {
      return myNumDivisions;
   }

   public boolean equals (Object obj) {
      if (obj instanceof GLGridResolution) {
         GLGridResolution res = (GLGridResolution)obj;
         return (myMajorCellSize == res.myMajorCellSize &&
                 myNumDivisions == res.myNumDivisions);
      }
      else {
         return false;
      }
   }

   public int hashCode() {
      int sum = (int)Double.doubleToLongBits (myMajorCellSize) + myNumDivisions;
      return sum * (sum + 1) + myNumDivisions;
   }

   public String toString() {
      return toString (new NumberFormat ("%.6f"));
   }

   // private String trim(String str)
   // {
   // if (str.indexOf ('.') != -1)
   // { int cut = str.length()-1;
   // while (str.charAt(cut) == '0')
   // { cut--;
   // }
   // if (str.charAt(cut) == '.')
   // { return str.substring (0, cut);
   // }
   // else if (cut < str.length()-1);
   // { return str.substring (0, cut+1);
   // }
   // }
   // else
   // { return str;
   // }
   // }

   public String toString (NumberFormat fmt) {
      String str = fmt.format (myMajorCellSize);
      if (myNumDivisions != 1) {
         str += "/" + myNumDivisions;
      }
      return (str);
   }
}
