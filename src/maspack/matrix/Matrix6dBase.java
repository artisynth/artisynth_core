/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.InternalErrorException;
import maspack.util.Clonable;

/**
 * Base class for 6 x 6 matrices in which the elements are stored as explicit
 * fields. A primary motivation for such objects is computational speed.
 */
public abstract class Matrix6dBase extends DenseMatrixBase implements Clonable {

   /**
    * Matrix element (0,0)
    */
   public double m00;

   /**
    * Matrix element (0,1)
    */
   public double m01;

   /**
    * Matrix element (0,2)
    */
   public double m02;

   /**
    * Matrix element (0,3)
    */
   public double m03;

   /**
    * Matrix element (0,4)
    */
   public double m04;

   /**
    * Matrix element (0,5)
    */
   public double m05;

   /**
    * Matrix element (1,0)
    */
   public double m10;

   /**
    * Matrix element (1,1)
    */
   public double m11;

   /**
    * Matrix element (1,2)
    */
   public double m12;

   /**
    * Matrix element (1,3)
    */
   public double m13;

   /**
    * Matrix element (1,4)
    */
   public double m14;

   /**
    * Matrix element (1,5)
    */
   public double m15;

   /**
    * Matrix element (2,0)
    */
   public double m20;

   /**
    * Matrix element (2,1)
    */
   public double m21;

   /**
    * Matrix element (2,2)
    */
   public double m22;

   /**
    * Matrix element (2,3)
    */
   public double m23;

   /**
    * Matrix element (2,4)
    */
   public double m24;

   /**
    * Matrix element (2,5)
    */
   public double m25;

   /**
    * Matrix element (3,0)
    */
   public double m30;

   /**
    * Matrix element (3,1)
    */
   public double m31;

   /**
    * Matrix element (3,2)
    */
   public double m32;

   /**
    * Matrix element (3,3)
    */
   public double m33;

   /**
    * Matrix element (3,4)
    */
   public double m34;

   /**
    * Matrix element (3,5)
    */
   public double m35;

   /**
    * Matrix element (4,0)
    */
   public double m40;

   /**
    * Matrix element (4,1)
    */
   public double m41;

   /**
    * Matrix element (4,2)
    */
   public double m42;

   /**
    * Matrix element (4,3)
    */
   public double m43;

   /**
    * Matrix element (4,4)
    */
   public double m44;

   /**
    * Matrix element (4,5)
    */
   public double m45;

   /**
    * Matrix element (5,0)
    */
   public double m50;

   /**
    * Matrix element (5,1)
    */
   public double m51;

   /**
    * Matrix element (5,2)
    */
   public double m52;

   /**
    * Matrix element (5,3)
    */
   public double m53;

   /**
    * Matrix element (5,4)
    */
   public double m54;

   /**
    * Matrix element (5,5)
    */
   public double m55;

   /**
    * Returns the number of rows in this matrix (which is always 6).
    * 
    * @return 6
    */
   public final int rowSize() {
      return 6;
   }

   /**
    * Returns the number of columns in this matrix (which is always 6).
    * 
    * @return 6
    */
   public final int colSize() {
      return 6;
   }

   /**
    * {@inheritDoc}
    */
   public double get (int i, int j) {
      switch (i) {
         case 0: {
            switch (j) {
               case 0:
                  return m00;
               case 1:
                  return m01;
               case 2:
                  return m02;
               case 3:
                  return m03;
               case 4:
                  return m04;
               case 5:
                  return m05;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 1: {
            switch (j) {
               case 0:
                  return m10;
               case 1:
                  return m11;
               case 2:
                  return m12;
               case 3:
                  return m13;
               case 4:
                  return m14;
               case 5:
                  return m15;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
                  return m20;
               case 1:
                  return m21;
               case 2:
                  return m22;
               case 3:
                  return m23;
               case 4:
                  return m24;
               case 5:
                  return m25;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            switch (j) {
               case 0:
                  return m30;
               case 1:
                  return m31;
               case 2:
                  return m32;
               case 3:
                  return m33;
               case 4:
                  return m34;
               case 5:
                  return m35;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 4: {
            switch (j) {
               case 0:
                  return m40;
               case 1:
                  return m41;
               case 2:
                  return m42;
               case 3:
                  return m43;
               case 4:
                  return m44;
               case 5:
                  return m45;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 5: {
            switch (j) {
               case 0:
                  return m50;
               case 1:
                  return m51;
               case 2:
                  return m52;
               case 3:
                  return m53;
               case 4:
                  return m54;
               case 5:
                  return m55;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void get (double[] values) {
      values[0] = m00;
      values[1] = m01;
      values[2] = m02;
      values[3] = m03;
      values[4] = m04;
      values[5] = m05;

      values[6] = m10;
      values[7] = m11;
      values[8] = m12;
      values[9] = m13;
      values[10] = m14;
      values[11] = m15;

      values[12] = m20;
      values[13] = m21;
      values[14] = m22;
      values[15] = m23;
      values[16] = m24;
      values[17] = m25;

      values[18] = m30;
      values[19] = m31;
      values[20] = m32;
      values[21] = m33;
      values[22] = m34;
      values[23] = m35;

      values[24] = m40;
      values[25] = m41;
      values[26] = m42;
      values[27] = m43;
      values[28] = m44;
      values[29] = m45;

      values[30] = m50;
      values[31] = m51;
      values[32] = m52;
      values[33] = m53;
      values[34] = m54;
      values[35] = m55;
   }

   /**
    * Gets the contents of this matrix into a MatrixNd.
    *
    * M matrix to receive the values of this matrix.
    */
   public void get (MatrixNd M) {

      double[] buf = M.getBuffer();
      int w = M.getBufferWidth();
      int off = M.getBufferBase();

      int idx = off;
      buf[idx + 0] = m00;
      buf[idx + 1] = m01;
      buf[idx + 2] = m02;
      buf[idx + 3] = m03;
      buf[idx + 4] = m04;
      buf[idx + 5] = m05;

      idx += w;
      buf[idx + 0] = m10;
      buf[idx + 1] = m11;
      buf[idx + 2] = m12;
      buf[idx + 3] = m13;
      buf[idx + 4] = m14;
      buf[idx + 5] = m15;

      idx += w;
      buf[idx + 0] = m20;
      buf[idx + 1] = m21;
      buf[idx + 2] = m22;
      buf[idx + 3] = m23;
      buf[idx + 4] = m24;
      buf[idx + 5] = m25;

      idx += w;
      buf[idx + 0] = m30;
      buf[idx + 1] = m31;
      buf[idx + 2] = m32;
      buf[idx + 3] = m33;
      buf[idx + 4] = m34;
      buf[idx + 5] = m35;

      idx += w;
      buf[idx + 0] = m40;
      buf[idx + 1] = m41;
      buf[idx + 2] = m42;
      buf[idx + 3] = m43;
      buf[idx + 4] = m44;
      buf[idx + 5] = m45;

      idx += w;
      buf[idx + 0] = m50;
      buf[idx + 1] = m51;
      buf[idx + 2] = m52;
      buf[idx + 3] = m53;
      buf[idx + 4] = m54;
      buf[idx + 5] = m55;
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values) {
      getColumn (j, values, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void getColumn (int j, double[] values, int off) {
      switch (j) {
         case 0: {
            values[0 + off] = m00;
            values[1 + off] = m10;
            values[2 + off] = m20;
            values[3 + off] = m30;
            values[4 + off] = m40;
            values[5 + off] = m50;
            break;
         }
         case 1: {
            values[0 + off] = m01;
            values[1 + off] = m11;
            values[2 + off] = m21;
            values[3 + off] = m31;
            values[4 + off] = m41;
            values[5 + off] = m51;
            break;
         }
         case 2: {
            values[0 + off] = m02;
            values[1 + off] = m12;
            values[2 + off] = m22;
            values[3 + off] = m32;
            values[4 + off] = m42;
            values[5 + off] = m52;
            break;
         }
         case 3: {
            values[0 + off] = m03;
            values[1 + off] = m13;
            values[2 + off] = m23;
            values[3 + off] = m33;
            values[4 + off] = m43;
            values[5 + off] = m53;
            break;
         }
         case 4: {
            values[0 + off] = m04;
            values[1 + off] = m14;
            values[2 + off] = m24;
            values[3 + off] = m34;
            values[4 + off] = m44;
            values[5 + off] = m54;
            break;
         }
         case 5: {
            values[0 + off] = m05;
            values[1 + off] = m15;
            values[2 + off] = m25;
            values[3 + off] = m35;
            values[4 + off] = m45;
            values[5 + off] = m55;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getRow (int i, double[] values) {
      getRow (i, values, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void getRow (int i, double[] values, int off) {
      switch (i) {
         case 0: {
            values[0 + off] = m00;
            values[1 + off] = m01;
            values[2 + off] = m02;
            values[3 + off] = m03;
            values[4 + off] = m04;
            values[5 + off] = m05;
            break;
         }
         case 1: {
            values[0 + off] = m10;
            values[1 + off] = m11;
            values[2 + off] = m12;
            values[3 + off] = m13;
            values[4 + off] = m14;
            values[5 + off] = m15;
            break;
         }
         case 2: {
            values[0 + off] = m20;
            values[1 + off] = m21;
            values[2 + off] = m22;
            values[3 + off] = m23;
            values[4 + off] = m24;
            values[5 + off] = m25;
            break;
         }
         case 3: {
            values[0 + off] = m30;
            values[1 + off] = m31;
            values[2 + off] = m32;
            values[3 + off] = m33;
            values[4 + off] = m34;
            values[5 + off] = m35;
            break;
         }
         case 4: {
            values[0 + off] = m40;
            values[1 + off] = m41;
            values[2 + off] = m42;
            values[3 + off] = m43;
            values[4 + off] = m44;
            values[5 + off] = m45;
            break;
         }
         case 5: {
            values[0 + off] = m50;
            values[1 + off] = m51;
            values[2 + off] = m52;
            values[3 + off] = m53;
            values[4 + off] = m54;
            values[5 + off] = m55;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) {
      switch (i) {
         case 0: {
            switch (j) {
               case 0:
                  m00 = value;
                  return;
               case 1:
                  m01 = value;
                  return;
               case 2:
                  m02 = value;
                  return;
               case 3:
                  m03 = value;
                  return;
               case 4:
                  m04 = value;
                  return;
               case 5:
                  m05 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 1: {
            switch (j) {
               case 0:
                  m10 = value;
                  return;
               case 1:
                  m11 = value;
                  return;
               case 2:
                  m12 = value;
                  return;
               case 3:
                  m13 = value;
                  return;
               case 4:
                  m14 = value;
                  return;
               case 5:
                  m15 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 2: {
            switch (j) {
               case 0:
                  m20 = value;
                  return;
               case 1:
                  m21 = value;
                  return;
               case 2:
                  m22 = value;
                  return;
               case 3:
                  m23 = value;
                  return;
               case 4:
                  m24 = value;
                  return;
               case 5:
                  m25 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 3: {
            switch (j) {
               case 0:
                  m30 = value;
                  return;
               case 1:
                  m31 = value;
                  return;
               case 2:
                  m32 = value;
                  return;
               case 3:
                  m33 = value;
                  return;
               case 4:
                  m34 = value;
                  return;
               case 5:
                  m35 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 4: {
            switch (j) {
               case 0:
                  m40 = value;
                  return;
               case 1:
                  m41 = value;
                  return;
               case 2:
                  m42 = value;
                  return;
               case 3:
                  m43 = value;
                  return;
               case 4:
                  m44 = value;
                  return;
               case 5:
                  m45 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         case 5: {
            switch (j) {
               case 0:
                  m50 = value;
                  return;
               case 1:
                  m51 = value;
                  return;
               case 2:
                  m52 = value;
                  return;
               case 3:
                  m53 = value;
                  return;
               case 4:
                  m54 = value;
                  return;
               case 5:
                  m55 = value;
                  return;
               default:
                  throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
            }
         }
         default:
            throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] vals) {
      m00 = vals[0];
      m01 = vals[1];
      m02 = vals[2];
      m03 = vals[3];
      m04 = vals[4];
      m05 = vals[5];

      m10 = vals[6];
      m11 = vals[7];
      m12 = vals[8];
      m13 = vals[9];
      m14 = vals[10];
      m15 = vals[11];

      m20 = vals[12];
      m21 = vals[13];
      m22 = vals[14];
      m23 = vals[15];
      m24 = vals[16];
      m25 = vals[17];

      m30 = vals[18];
      m31 = vals[19];
      m32 = vals[20];
      m33 = vals[21];
      m34 = vals[22];
      m35 = vals[23];

      m40 = vals[24];
      m41 = vals[25];
      m42 = vals[26];
      m43 = vals[27];
      m44 = vals[28];
      m45 = vals[29];

      m50 = vals[30];
      m51 = vals[31];
      m52 = vals[32];
      m53 = vals[33];
      m54 = vals[34];
      m55 = vals[35];
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      switch (j) {
         case 0: {
            m00 = values[0];
            m10 = values[1];
            m20 = values[2];
            m30 = values[3];
            m40 = values[4];
            m50 = values[5];
            break;
         }
         case 1: {
            m01 = values[0];
            m11 = values[1];
            m21 = values[2];
            m31 = values[3];
            m41 = values[4];
            m51 = values[5];
            break;
         }
         case 2: {
            m02 = values[0];
            m12 = values[1];
            m22 = values[2];
            m32 = values[3];
            m42 = values[4];
            m52 = values[5];
            break;
         }
         case 3: {
            m03 = values[0];
            m13 = values[1];
            m23 = values[2];
            m33 = values[3];
            m43 = values[4];
            m53 = values[5];
            break;
         }
         case 4: {
            m04 = values[0];
            m14 = values[1];
            m24 = values[2];
            m34 = values[3];
            m44 = values[4];
            m54 = values[5];
            break;
         }
         case 5: {
            m05 = values[0];
            m15 = values[1];
            m25 = values[2];
            m35 = values[3];
            m45 = values[4];
            m55 = values[5];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("j=" + j);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setRow (int i, double[] values) {
      switch (i) {
         case 0: {
            m00 = values[0];
            m01 = values[1];
            m02 = values[2];
            m03 = values[3];
            m04 = values[4];
            m05 = values[5];
            break;
         }
         case 1: {
            m10 = values[0];
            m11 = values[1];
            m12 = values[2];
            m13 = values[3];
            m14 = values[4];
            m15 = values[5];
            break;
         }
         case 2: {
            m20 = values[0];
            m21 = values[1];
            m22 = values[2];
            m23 = values[3];
            m24 = values[4];
            m25 = values[5];
            break;
         }
         case 3: {
            m30 = values[0];
            m31 = values[1];
            m32 = values[2];
            m33 = values[3];
            m34 = values[4];
            m35 = values[5];
            break;
         }
         case 4: {
            m40 = values[0];
            m41 = values[1];
            m42 = values[2];
            m43 = values[3];
            m44 = values[4];
            m45 = values[5];
            break;
         }
         case 5: {
            m50 = values[0];
            m51 = values[1];
            m52 = values[2];
            m53 = values[3];
            m54 = values[4];
            m55 = values[5];
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException ("i=" + i);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void set (Matrix M) {
      if (M instanceof Matrix6dBase) {
         set ((Matrix6dBase)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 = M.get (0, 0);
         m01 = M.get (0, 1);
         m02 = M.get (0, 2);
         m03 = M.get (0, 3);
         m04 = M.get (0, 4);
         m05 = M.get (0, 5);

         m10 = M.get (1, 0);
         m11 = M.get (1, 1);
         m12 = M.get (1, 2);
         m13 = M.get (1, 3);
         m14 = M.get (1, 4);
         m15 = M.get (1, 5);

         m20 = M.get (2, 0);
         m21 = M.get (2, 1);
         m22 = M.get (2, 2);
         m23 = M.get (2, 3);
         m24 = M.get (2, 4);
         m25 = M.get (2, 5);

         m30 = M.get (3, 0);
         m31 = M.get (3, 1);
         m32 = M.get (3, 2);
         m33 = M.get (3, 3);
         m34 = M.get (3, 4);
         m35 = M.get (3, 5);

         m40 = M.get (4, 0);
         m41 = M.get (4, 1);
         m42 = M.get (4, 2);
         m43 = M.get (4, 3);
         m44 = M.get (4, 4);
         m45 = M.get (4, 5);

         m50 = M.get (5, 0);
         m51 = M.get (5, 1);
         m52 = M.get (5, 2);
         m53 = M.get (5, 3);
         m54 = M.get (5, 4);
         m55 = M.get (5, 5);
      }
   }

   /**
    * Sets the values of this matrix to those of matrix M.
    * 
    * @param M
    * matrix whose values are to be copied
    */
   public void set (Matrix6dBase M) {
      m00 = M.m00;
      m01 = M.m01;
      m02 = M.m02;
      m03 = M.m03;
      m04 = M.m04;
      m05 = M.m05;

      m10 = M.m10;
      m11 = M.m11;
      m12 = M.m12;
      m13 = M.m13;
      m14 = M.m14;
      m15 = M.m15;

      m20 = M.m20;
      m21 = M.m21;
      m22 = M.m22;
      m23 = M.m23;
      m24 = M.m24;
      m25 = M.m25;

      m30 = M.m30;
      m31 = M.m31;
      m32 = M.m32;
      m33 = M.m33;
      m34 = M.m34;
      m35 = M.m35;

      m40 = M.m40;
      m41 = M.m41;
      m42 = M.m42;
      m43 = M.m43;
      m44 = M.m44;
      m45 = M.m45;

      m50 = M.m50;
      m51 = M.m51;
      m52 = M.m52;
      m53 = M.m53;
      m54 = M.m54;
      m55 = M.m55;
   }

   
   /**
    * Multiplies this matrix by M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mul (Matrix6dBase M1) {
      mul (this, M1);
   }

   /**
    * Multiplies matrix M1 by M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mul (Matrix6dBase M1, Matrix6dBase M2) {
      double x00, x01, x02, x03, x04, x05;
      double x10, x11, x12, x13, x14, x15;
      double x20, x21, x22, x23, x24, x25;
      double x30, x31, x32, x33, x34, x35;
      double x40, x41, x42, x43, x44, x45;
      double x50, x51, x52, x53, x54, x55;

      x00 = (M1.m00*M2.m00 + M1.m01*M2.m10 + M1.m02*M2.m20 +
             M1.m03*M2.m30 + M1.m04*M2.m40 + M1.m05*M2.m50);
      x01 = (M1.m00*M2.m01 + M1.m01*M2.m11 + M1.m02*M2.m21 +
             M1.m03*M2.m31 + M1.m04*M2.m41 + M1.m05*M2.m51);
      x02 = (M1.m00*M2.m02 + M1.m01*M2.m12 + M1.m02*M2.m22 +
             M1.m03*M2.m32 + M1.m04*M2.m42 + M1.m05*M2.m52);
      x03 = (M1.m00*M2.m03 + M1.m01*M2.m13 + M1.m02*M2.m23 +
             M1.m03*M2.m33 + M1.m04*M2.m43 + M1.m05*M2.m53);
      x04 = (M1.m00*M2.m04 + M1.m01*M2.m14 + M1.m02*M2.m24 +
             M1.m03*M2.m34 + M1.m04*M2.m44 + M1.m05*M2.m54);
      x05 = (M1.m00*M2.m05 + M1.m01*M2.m15 + M1.m02*M2.m25 +
             M1.m03*M2.m35 + M1.m04*M2.m45 + M1.m05*M2.m55);

      x10 = (M1.m10*M2.m00 + M1.m11*M2.m10 + M1.m12*M2.m20 +
             M1.m13*M2.m30 + M1.m14*M2.m40 + M1.m15*M2.m50);
      x11 = (M1.m10*M2.m01 + M1.m11*M2.m11 + M1.m12*M2.m21 +
             M1.m13*M2.m31 + M1.m14*M2.m41 + M1.m15*M2.m51);
      x12 = (M1.m10*M2.m02 + M1.m11*M2.m12 + M1.m12*M2.m22 +
             M1.m13*M2.m32 + M1.m14*M2.m42 + M1.m15*M2.m52);
      x13 = (M1.m10*M2.m03 + M1.m11*M2.m13 + M1.m12*M2.m23 +
             M1.m13*M2.m33 + M1.m14*M2.m43 + M1.m15*M2.m53);
      x14 = (M1.m10*M2.m04 + M1.m11*M2.m14 + M1.m12*M2.m24 +
             M1.m13*M2.m34 + M1.m14*M2.m44 + M1.m15*M2.m54);
      x15 = (M1.m10*M2.m05 + M1.m11*M2.m15 + M1.m12*M2.m25 +
             M1.m13*M2.m35 + M1.m14*M2.m45 + M1.m15*M2.m55);

      x20 = (M1.m20*M2.m00 + M1.m21*M2.m10 + M1.m22*M2.m20 +
             M1.m23*M2.m30 + M1.m24*M2.m40 + M1.m25*M2.m50);
      x21 = (M1.m20*M2.m01 + M1.m21*M2.m11 + M1.m22*M2.m21 +
             M1.m23*M2.m31 + M1.m24*M2.m41 + M1.m25*M2.m51);
      x22 = (M1.m20*M2.m02 + M1.m21*M2.m12 + M1.m22*M2.m22 +
             M1.m23*M2.m32 + M1.m24*M2.m42 + M1.m25*M2.m52);
      x23 = (M1.m20*M2.m03 + M1.m21*M2.m13 + M1.m22*M2.m23 +
             M1.m23*M2.m33 + M1.m24*M2.m43 + M1.m25*M2.m53);
      x24 = (M1.m20*M2.m04 + M1.m21*M2.m14 + M1.m22*M2.m24 +
             M1.m23*M2.m34 + M1.m24*M2.m44 + M1.m25*M2.m54);
      x25 = (M1.m20*M2.m05 + M1.m21*M2.m15 + M1.m22*M2.m25 +
             M1.m23*M2.m35 + M1.m24*M2.m45 + M1.m25*M2.m55);

      x30 = (M1.m30*M2.m00 + M1.m31*M2.m10 + M1.m32*M2.m20 +
             M1.m33*M2.m30 + M1.m34*M2.m40 + M1.m35*M2.m50);
      x31 = (M1.m30*M2.m01 + M1.m31*M2.m11 + M1.m32*M2.m21 +
             M1.m33*M2.m31 + M1.m34*M2.m41 + M1.m35*M2.m51);
      x32 = (M1.m30*M2.m02 + M1.m31*M2.m12 + M1.m32*M2.m22 +
             M1.m33*M2.m32 + M1.m34*M2.m42 + M1.m35*M2.m52);
      x33 = (M1.m30*M2.m03 + M1.m31*M2.m13 + M1.m32*M2.m23 +
             M1.m33*M2.m33 + M1.m34*M2.m43 + M1.m35*M2.m53);
      x34 = (M1.m30*M2.m04 + M1.m31*M2.m14 + M1.m32*M2.m24 +
             M1.m33*M2.m34 + M1.m34*M2.m44 + M1.m35*M2.m54);
      x35 = (M1.m30*M2.m05 + M1.m31*M2.m15 + M1.m32*M2.m25 +
             M1.m33*M2.m35 + M1.m34*M2.m45 + M1.m35*M2.m55);

      x40 = (M1.m40*M2.m00 + M1.m41*M2.m10 + M1.m42*M2.m20 +
             M1.m43*M2.m30 + M1.m44*M2.m40 + M1.m45*M2.m50);
      x41 = (M1.m40*M2.m01 + M1.m41*M2.m11 + M1.m42*M2.m21 +
             M1.m43*M2.m31 + M1.m44*M2.m41 + M1.m45*M2.m51);
      x42 = (M1.m40*M2.m02 + M1.m41*M2.m12 + M1.m42*M2.m22 +
             M1.m43*M2.m32 + M1.m44*M2.m42 + M1.m45*M2.m52);
      x43 = (M1.m40*M2.m03 + M1.m41*M2.m13 + M1.m42*M2.m23 +
             M1.m43*M2.m33 + M1.m44*M2.m43 + M1.m45*M2.m53);
      x44 = (M1.m40*M2.m04 + M1.m41*M2.m14 + M1.m42*M2.m24 +
             M1.m43*M2.m34 + M1.m44*M2.m44 + M1.m45*M2.m54);
      x45 = (M1.m40*M2.m05 + M1.m41*M2.m15 + M1.m42*M2.m25 +
             M1.m43*M2.m35 + M1.m44*M2.m45 + M1.m45*M2.m55);

      x50 = (M1.m50*M2.m00 + M1.m51*M2.m10 + M1.m52*M2.m20 +
             M1.m53*M2.m30 + M1.m54*M2.m40 + M1.m55*M2.m50);
      x51 = (M1.m50*M2.m01 + M1.m51*M2.m11 + M1.m52*M2.m21 +
             M1.m53*M2.m31 + M1.m54*M2.m41 + M1.m55*M2.m51);
      x52 = (M1.m50*M2.m02 + M1.m51*M2.m12 + M1.m52*M2.m22 +
             M1.m53*M2.m32 + M1.m54*M2.m42 + M1.m55*M2.m52);
      x53 = (M1.m50*M2.m03 + M1.m51*M2.m13 + M1.m52*M2.m23 +
             M1.m53*M2.m33 + M1.m54*M2.m43 + M1.m55*M2.m53);
      x54 = (M1.m50*M2.m04 + M1.m51*M2.m14 + M1.m52*M2.m24 +
             M1.m53*M2.m34 + M1.m54*M2.m44 + M1.m55*M2.m54);
      x55 = (M1.m50*M2.m05 + M1.m51*M2.m15 + M1.m52*M2.m25 +
             M1.m53*M2.m35 + M1.m54*M2.m45 + M1.m55*M2.m55);

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;
      m04 = x04;
      m05 = x05;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;
      m14 = x14;
      m15 = x15;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;
      m24 = x24;
      m25 = x25;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
      m34 = x34;
      m35 = x35;

      m40 = x40;
      m41 = x41;
      m42 = x42;
      m43 = x43;
      m44 = x44;
      m45 = x45;

      m50 = x50;
      m51 = x51;
      m52 = x52;
      m53 = x53;
      m54 = x54;
      m55 = x55;
   }

   /**
    * Multiplies this matrix by the transpose of M1 and places the result in
    * this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void mulTranspose (Matrix6dBase M1) {
      mulTransposeRight (this, M1);
   }

   /**
    * Multiplies the transpose of matrix M1 by M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mulTransposeLeft (Matrix6dBase M1, Matrix6dBase M2) {
      double x00, x01, x02, x03, x04, x05;
      double x10, x11, x12, x13, x14, x15;
      double x20, x21, x22, x23, x24, x25;
      double x30, x31, x32, x33, x34, x35;
      double x40, x41, x42, x43, x44, x45;
      double x50, x51, x52, x53, x54, x55;

      x00 = (M1.m00*M2.m00 + M1.m10*M2.m10 + M1.m20*M2.m20 +
             M1.m30*M2.m30 + M1.m40*M2.m40 + M1.m50*M2.m50);
      x01 = (M1.m00*M2.m01 + M1.m10*M2.m11 + M1.m20*M2.m21 +
             M1.m30*M2.m31 + M1.m40*M2.m41 + M1.m50*M2.m51);
      x02 = (M1.m00*M2.m02 + M1.m10*M2.m12 + M1.m20*M2.m22 +
             M1.m30*M2.m32 + M1.m40*M2.m42 + M1.m50*M2.m52);
      x03 = (M1.m00*M2.m03 + M1.m10*M2.m13 + M1.m20*M2.m23 +
             M1.m30*M2.m33 + M1.m40*M2.m43 + M1.m50*M2.m53);
      x04 = (M1.m00*M2.m04 + M1.m10*M2.m14 + M1.m20*M2.m24 +
             M1.m30*M2.m34 + M1.m40*M2.m44 + M1.m50*M2.m54);
      x05 = (M1.m00*M2.m05 + M1.m10*M2.m15 + M1.m20*M2.m25 +
             M1.m30*M2.m35 + M1.m40*M2.m45 + M1.m50*M2.m55);

      x10 = (M1.m01*M2.m00 + M1.m11*M2.m10 + M1.m21*M2.m20 +
             M1.m31*M2.m30 + M1.m41*M2.m40 + M1.m51*M2.m50);
      x11 = (M1.m01*M2.m01 + M1.m11*M2.m11 + M1.m21*M2.m21 +
             M1.m31*M2.m31 + M1.m41*M2.m41 + M1.m51*M2.m51);
      x12 = (M1.m01*M2.m02 + M1.m11*M2.m12 + M1.m21*M2.m22 +
             M1.m31*M2.m32 + M1.m41*M2.m42 + M1.m51*M2.m52);
      x13 = (M1.m01*M2.m03 + M1.m11*M2.m13 + M1.m21*M2.m23 +
             M1.m31*M2.m33 + M1.m41*M2.m43 + M1.m51*M2.m53);
      x14 = (M1.m01*M2.m04 + M1.m11*M2.m14 + M1.m21*M2.m24 +
             M1.m31*M2.m34 + M1.m41*M2.m44 + M1.m51*M2.m54);
      x15 = (M1.m01*M2.m05 + M1.m11*M2.m15 + M1.m21*M2.m25 +
             M1.m31*M2.m35 + M1.m41*M2.m45 + M1.m51*M2.m55);

      x20 = (M1.m02*M2.m00 + M1.m12*M2.m10 + M1.m22*M2.m20 +
             M1.m32*M2.m30 + M1.m42*M2.m40 + M1.m52*M2.m50);
      x21 = (M1.m02*M2.m01 + M1.m12*M2.m11 + M1.m22*M2.m21 +
             M1.m32*M2.m31 + M1.m42*M2.m41 + M1.m52*M2.m51);
      x22 = (M1.m02*M2.m02 + M1.m12*M2.m12 + M1.m22*M2.m22 +
             M1.m32*M2.m32 + M1.m42*M2.m42 + M1.m52*M2.m52);
      x23 = (M1.m02*M2.m03 + M1.m12*M2.m13 + M1.m22*M2.m23 +
             M1.m32*M2.m33 + M1.m42*M2.m43 + M1.m52*M2.m53);
      x24 = (M1.m02*M2.m04 + M1.m12*M2.m14 + M1.m22*M2.m24 +
             M1.m32*M2.m34 + M1.m42*M2.m44 + M1.m52*M2.m54);
      x25 = (M1.m02*M2.m05 + M1.m12*M2.m15 + M1.m22*M2.m25 +
             M1.m32*M2.m35 + M1.m42*M2.m45 + M1.m52*M2.m55);

      x30 = (M1.m03*M2.m00 + M1.m13*M2.m10 + M1.m23*M2.m20 +
             M1.m33*M2.m30 + M1.m43*M2.m40 + M1.m53*M2.m50);
      x31 = (M1.m03*M2.m01 + M1.m13*M2.m11 + M1.m23*M2.m21 +
             M1.m33*M2.m31 + M1.m43*M2.m41 + M1.m53*M2.m51);
      x32 = (M1.m03*M2.m02 + M1.m13*M2.m12 + M1.m23*M2.m22 +
             M1.m33*M2.m32 + M1.m43*M2.m42 + M1.m53*M2.m52);
      x33 = (M1.m03*M2.m03 + M1.m13*M2.m13 + M1.m23*M2.m23 +
             M1.m33*M2.m33 + M1.m43*M2.m43 + M1.m53*M2.m53);
      x34 = (M1.m03*M2.m04 + M1.m13*M2.m14 + M1.m23*M2.m24 +
             M1.m33*M2.m34 + M1.m43*M2.m44 + M1.m53*M2.m54);
      x35 = (M1.m03*M2.m05 + M1.m13*M2.m15 + M1.m23*M2.m25 +
             M1.m33*M2.m35 + M1.m43*M2.m45 + M1.m53*M2.m55);

      x40 = (M1.m04*M2.m00 + M1.m14*M2.m10 + M1.m24*M2.m20 +
             M1.m34*M2.m30 + M1.m44*M2.m40 + M1.m54*M2.m50);
      x41 = (M1.m04*M2.m01 + M1.m14*M2.m11 + M1.m24*M2.m21 +
             M1.m34*M2.m31 + M1.m44*M2.m41 + M1.m54*M2.m51);
      x42 = (M1.m04*M2.m02 + M1.m14*M2.m12 + M1.m24*M2.m22 +
             M1.m34*M2.m32 + M1.m44*M2.m42 + M1.m54*M2.m52);
      x43 = (M1.m04*M2.m03 + M1.m14*M2.m13 + M1.m24*M2.m23 +
             M1.m34*M2.m33 + M1.m44*M2.m43 + M1.m54*M2.m53);
      x44 = (M1.m04*M2.m04 + M1.m14*M2.m14 + M1.m24*M2.m24 +
             M1.m34*M2.m34 + M1.m44*M2.m44 + M1.m54*M2.m54);
      x45 = (M1.m04*M2.m05 + M1.m14*M2.m15 + M1.m24*M2.m25 +
             M1.m34*M2.m35 + M1.m44*M2.m45 + M1.m54*M2.m55);

      x50 = (M1.m05*M2.m00 + M1.m15*M2.m10 + M1.m25*M2.m20 +
             M1.m35*M2.m30 + M1.m45*M2.m40 + M1.m55*M2.m50);
      x51 = (M1.m05*M2.m01 + M1.m15*M2.m11 + M1.m25*M2.m21 +
             M1.m35*M2.m31 + M1.m45*M2.m41 + M1.m55*M2.m51);
      x52 = (M1.m05*M2.m02 + M1.m15*M2.m12 + M1.m25*M2.m22 +
             M1.m35*M2.m32 + M1.m45*M2.m42 + M1.m55*M2.m52);
      x53 = (M1.m05*M2.m03 + M1.m15*M2.m13 + M1.m25*M2.m23 +
             M1.m35*M2.m33 + M1.m45*M2.m43 + M1.m55*M2.m53);
      x54 = (M1.m05*M2.m04 + M1.m15*M2.m14 + M1.m25*M2.m24 +
             M1.m35*M2.m34 + M1.m45*M2.m44 + M1.m55*M2.m54);
      x55 = (M1.m05*M2.m05 + M1.m15*M2.m15 + M1.m25*M2.m25 +
             M1.m35*M2.m35 + M1.m45*M2.m45 + M1.m55*M2.m55);

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;
      m04 = x04;
      m05 = x05;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;
      m14 = x14;
      m15 = x15;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;
      m24 = x24;
      m25 = x25;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
      m34 = x34;
      m35 = x35;

      m40 = x40;
      m41 = x41;
      m42 = x42;
      m43 = x43;
      m44 = x44;
      m45 = x45;

      m50 = x50;
      m51 = x51;
      m52 = x52;
      m53 = x53;
      m54 = x54;
      m55 = x55;
   }

   /**
    * Multiplies matrix M1 by the transpose of M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mulTransposeRight (Matrix6dBase M1, Matrix6dBase M2) {
      double x00, x01, x02, x03, x04, x05;
      double x10, x11, x12, x13, x14, x15;
      double x20, x21, x22, x23, x24, x25;
      double x30, x31, x32, x33, x34, x35;
      double x40, x41, x42, x43, x44, x45;
      double x50, x51, x52, x53, x54, x55;

      x00 = (M1.m00*M2.m00 + M1.m01*M2.m01 + M1.m02*M2.m02 +
             M1.m03*M2.m03 + M1.m04*M2.m04 + M1.m05*M2.m05);
      x01 = (M1.m00*M2.m10 + M1.m01*M2.m11 + M1.m02*M2.m12 +
             M1.m03*M2.m13 + M1.m04*M2.m14 + M1.m05*M2.m15);
      x02 = (M1.m00*M2.m20 + M1.m01*M2.m21 + M1.m02*M2.m22 +
             M1.m03*M2.m23 + M1.m04*M2.m24 + M1.m05*M2.m25);
      x03 = (M1.m00*M2.m30 + M1.m01*M2.m31 + M1.m02*M2.m32 +
             M1.m03*M2.m33 + M1.m04*M2.m34 + M1.m05*M2.m35);
      x04 = (M1.m00*M2.m40 + M1.m01*M2.m41 + M1.m02*M2.m42 +
             M1.m03*M2.m43 + M1.m04*M2.m44 + M1.m05*M2.m45);
      x05 = (M1.m00*M2.m50 + M1.m01*M2.m51 + M1.m02*M2.m52 +
             M1.m03*M2.m53 + M1.m04*M2.m54 + M1.m05*M2.m55);

      x10 = (M1.m10*M2.m00 + M1.m11*M2.m01 + M1.m12*M2.m02 +
             M1.m13*M2.m03 + M1.m14*M2.m04 + M1.m15*M2.m05);
      x11 = (M1.m10*M2.m10 + M1.m11*M2.m11 + M1.m12*M2.m12 +
             M1.m13*M2.m13 + M1.m14*M2.m14 + M1.m15*M2.m15);
      x12 = (M1.m10*M2.m20 + M1.m11*M2.m21 + M1.m12*M2.m22 +
             M1.m13*M2.m23 + M1.m14*M2.m24 + M1.m15*M2.m25);
      x13 = (M1.m10*M2.m30 + M1.m11*M2.m31 + M1.m12*M2.m32 +
             M1.m13*M2.m33 + M1.m14*M2.m34 + M1.m15*M2.m35);
      x14 = (M1.m10*M2.m40 + M1.m11*M2.m41 + M1.m12*M2.m42 +
             M1.m13*M2.m43 + M1.m14*M2.m44 + M1.m15*M2.m45);
      x15 = (M1.m10*M2.m50 + M1.m11*M2.m51 + M1.m12*M2.m52 +
             M1.m13*M2.m53 + M1.m14*M2.m54 + M1.m15*M2.m55);

      x20 = (M1.m20*M2.m00 + M1.m21*M2.m01 + M1.m22*M2.m02 +
             M1.m23*M2.m03 + M1.m24*M2.m04 + M1.m25*M2.m05);
      x21 = (M1.m20*M2.m10 + M1.m21*M2.m11 + M1.m22*M2.m12 +
             M1.m23*M2.m13 + M1.m24*M2.m14 + M1.m25*M2.m15);
      x22 = (M1.m20*M2.m20 + M1.m21*M2.m21 + M1.m22*M2.m22 +
             M1.m23*M2.m23 + M1.m24*M2.m24 + M1.m25*M2.m25);
      x23 = (M1.m20*M2.m30 + M1.m21*M2.m31 + M1.m22*M2.m32 +
             M1.m23*M2.m33 + M1.m24*M2.m34 + M1.m25*M2.m35);
      x24 = (M1.m20*M2.m40 + M1.m21*M2.m41 + M1.m22*M2.m42 +
             M1.m23*M2.m43 + M1.m24*M2.m44 + M1.m25*M2.m45);
      x25 = (M1.m20*M2.m50 + M1.m21*M2.m51 + M1.m22*M2.m52 +
             M1.m23*M2.m53 + M1.m24*M2.m54 + M1.m25*M2.m55);

      x30 = (M1.m30*M2.m00 + M1.m31*M2.m01 + M1.m32*M2.m02 +
             M1.m33*M2.m03 + M1.m34*M2.m04 + M1.m35*M2.m05);
      x31 = (M1.m30*M2.m10 + M1.m31*M2.m11 + M1.m32*M2.m12 +
             M1.m33*M2.m13 + M1.m34*M2.m14 + M1.m35*M2.m15);
      x32 = (M1.m30*M2.m20 + M1.m31*M2.m21 + M1.m32*M2.m22 +
             M1.m33*M2.m23 + M1.m34*M2.m24 + M1.m35*M2.m25);
      x33 = (M1.m30*M2.m30 + M1.m31*M2.m31 + M1.m32*M2.m32 +
             M1.m33*M2.m33 + M1.m34*M2.m34 + M1.m35*M2.m35);
      x34 = (M1.m30*M2.m40 + M1.m31*M2.m41 + M1.m32*M2.m42 +
             M1.m33*M2.m43 + M1.m34*M2.m44 + M1.m35*M2.m45);
      x35 = (M1.m30*M2.m50 + M1.m31*M2.m51 + M1.m32*M2.m52 +
             M1.m33*M2.m53 + M1.m34*M2.m54 + M1.m35*M2.m55);

      x40 = (M1.m40*M2.m00 + M1.m41*M2.m01 + M1.m42*M2.m02 +
             M1.m43*M2.m03 + M1.m44*M2.m04 + M1.m45*M2.m05);
      x41 = (M1.m40*M2.m10 + M1.m41*M2.m11 + M1.m42*M2.m12 +
             M1.m43*M2.m13 + M1.m44*M2.m14 + M1.m45*M2.m15);
      x42 = (M1.m40*M2.m20 + M1.m41*M2.m21 + M1.m42*M2.m22 +
             M1.m43*M2.m23 + M1.m44*M2.m24 + M1.m45*M2.m25);
      x43 = (M1.m40*M2.m30 + M1.m41*M2.m31 + M1.m42*M2.m32 +
             M1.m43*M2.m33 + M1.m44*M2.m34 + M1.m45*M2.m35);
      x44 = (M1.m40*M2.m40 + M1.m41*M2.m41 + M1.m42*M2.m42 +
             M1.m43*M2.m43 + M1.m44*M2.m44 + M1.m45*M2.m45);
      x45 = (M1.m40*M2.m50 + M1.m41*M2.m51 + M1.m42*M2.m52 +
             M1.m43*M2.m53 + M1.m44*M2.m54 + M1.m45*M2.m55);

      x50 = (M1.m50*M2.m00 + M1.m51*M2.m01 + M1.m52*M2.m02 +
             M1.m53*M2.m03 + M1.m54*M2.m04 + M1.m55*M2.m05);
      x51 = (M1.m50*M2.m10 + M1.m51*M2.m11 + M1.m52*M2.m12 +
             M1.m53*M2.m13 + M1.m54*M2.m14 + M1.m55*M2.m15);
      x52 = (M1.m50*M2.m20 + M1.m51*M2.m21 + M1.m52*M2.m22 +
             M1.m53*M2.m23 + M1.m54*M2.m24 + M1.m55*M2.m25);
      x53 = (M1.m50*M2.m30 + M1.m51*M2.m31 + M1.m52*M2.m32 +
             M1.m53*M2.m33 + M1.m54*M2.m34 + M1.m55*M2.m35);
      x54 = (M1.m50*M2.m40 + M1.m51*M2.m41 + M1.m52*M2.m42 +
             M1.m53*M2.m43 + M1.m54*M2.m44 + M1.m55*M2.m45);
      x55 = (M1.m50*M2.m50 + M1.m51*M2.m51 + M1.m52*M2.m52 +
             M1.m53*M2.m53 + M1.m54*M2.m54 + M1.m55*M2.m55);

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;
      m04 = x04;
      m05 = x05;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;
      m14 = x14;
      m15 = x15;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;
      m24 = x24;
      m25 = x25;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
      m34 = x34;
      m35 = x35;

      m40 = x40;
      m41 = x41;
      m42 = x42;
      m43 = x43;
      m44 = x44;
      m45 = x45;

      m50 = x50;
      m51 = x51;
      m52 = x52;
      m53 = x53;
      m54 = x54;
      m55 = x55;
   }

   /**
    * Multiplies the transpose of matrix M1 by the transpose of M2 and places
    * the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void mulTransposeBoth (Matrix6dBase M1, Matrix6dBase M2) {

      double x00, x01, x02, x03, x04, x05;
      double x10, x11, x12, x13, x14, x15;
      double x20, x21, x22, x23, x24, x25;
      double x30, x31, x32, x33, x34, x35;
      double x40, x41, x42, x43, x44, x45;
      double x50, x51, x52, x53, x54, x55;

      x00 = (M1.m00*M2.m00 + M1.m10*M2.m01 + M1.m20*M2.m02 +
             M1.m30*M2.m03 + M1.m40*M2.m04 + M1.m50*M2.m05);
      x01 = (M1.m00*M2.m10 + M1.m10*M2.m11 + M1.m20*M2.m12 +
             M1.m30*M2.m13 + M1.m40*M2.m14 + M1.m50*M2.m15);
      x02 = (M1.m00*M2.m20 + M1.m10*M2.m21 + M1.m20*M2.m22 +
             M1.m30*M2.m23 + M1.m40*M2.m24 + M1.m50*M2.m25);
      x03 = (M1.m00*M2.m30 + M1.m10*M2.m31 + M1.m20*M2.m32 +
             M1.m30*M2.m33 + M1.m40*M2.m34 + M1.m50*M2.m35);
      x04 = (M1.m00*M2.m40 + M1.m10*M2.m41 + M1.m20*M2.m42 +
             M1.m30*M2.m43 + M1.m40*M2.m44 + M1.m50*M2.m45);
      x05 = (M1.m00*M2.m50 + M1.m10*M2.m51 + M1.m20*M2.m52 +
             M1.m30*M2.m53 + M1.m40*M2.m54 + M1.m50*M2.m55);

      x10 = (M1.m01*M2.m00 + M1.m11*M2.m01 + M1.m21*M2.m02 +
             M1.m31*M2.m03 + M1.m41*M2.m04 + M1.m51*M2.m05);
      x11 = (M1.m01*M2.m10 + M1.m11*M2.m11 + M1.m21*M2.m12 +
             M1.m31*M2.m13 + M1.m41*M2.m14 + M1.m51*M2.m15);
      x12 = (M1.m01*M2.m20 + M1.m11*M2.m21 + M1.m21*M2.m22 +
             M1.m31*M2.m23 + M1.m41*M2.m24 + M1.m51*M2.m25);
      x13 = (M1.m01*M2.m30 + M1.m11*M2.m31 + M1.m21*M2.m32 +
             M1.m31*M2.m33 + M1.m41*M2.m34 + M1.m51*M2.m35);
      x14 = (M1.m01*M2.m40 + M1.m11*M2.m41 + M1.m21*M2.m42 +
             M1.m31*M2.m43 + M1.m41*M2.m44 + M1.m51*M2.m45);
      x15 = (M1.m01*M2.m50 + M1.m11*M2.m51 + M1.m21*M2.m52 +
             M1.m31*M2.m53 + M1.m41*M2.m54 + M1.m51*M2.m55);

      x20 = (M1.m02*M2.m00 + M1.m12*M2.m01 + M1.m22*M2.m02 +
             M1.m32*M2.m03 + M1.m42*M2.m04 + M1.m52*M2.m05);
      x21 = (M1.m02*M2.m10 + M1.m12*M2.m11 + M1.m22*M2.m12 +
             M1.m32*M2.m13 + M1.m42*M2.m14 + M1.m52*M2.m15);
      x22 = (M1.m02*M2.m20 + M1.m12*M2.m21 + M1.m22*M2.m22 +
             M1.m32*M2.m23 + M1.m42*M2.m24 + M1.m52*M2.m25);
      x23 = (M1.m02*M2.m30 + M1.m12*M2.m31 + M1.m22*M2.m32 +
             M1.m32*M2.m33 + M1.m42*M2.m34 + M1.m52*M2.m35);
      x24 = (M1.m02*M2.m40 + M1.m12*M2.m41 + M1.m22*M2.m42 +
             M1.m32*M2.m43 + M1.m42*M2.m44 + M1.m52*M2.m45);
      x25 = (M1.m02*M2.m50 + M1.m12*M2.m51 + M1.m22*M2.m52 +
             M1.m32*M2.m53 + M1.m42*M2.m54 + M1.m52*M2.m55);

      x30 = (M1.m03*M2.m00 + M1.m13*M2.m01 + M1.m23*M2.m02 +
             M1.m33*M2.m03 + M1.m43*M2.m04 + M1.m53*M2.m05);
      x31 = (M1.m03*M2.m10 + M1.m13*M2.m11 + M1.m23*M2.m12 +
             M1.m33*M2.m13 + M1.m43*M2.m14 + M1.m53*M2.m15);
      x32 = (M1.m03*M2.m20 + M1.m13*M2.m21 + M1.m23*M2.m22 +
             M1.m33*M2.m23 + M1.m43*M2.m24 + M1.m53*M2.m25);
      x33 = (M1.m03*M2.m30 + M1.m13*M2.m31 + M1.m23*M2.m32 +
             M1.m33*M2.m33 + M1.m43*M2.m34 + M1.m53*M2.m35);
      x34 = (M1.m03*M2.m40 + M1.m13*M2.m41 + M1.m23*M2.m42 +
             M1.m33*M2.m43 + M1.m43*M2.m44 + M1.m53*M2.m45);
      x35 = (M1.m03*M2.m50 + M1.m13*M2.m51 + M1.m23*M2.m52 +
             M1.m33*M2.m53 + M1.m43*M2.m54 + M1.m53*M2.m55);

      x40 = (M1.m04*M2.m00 + M1.m14*M2.m01 + M1.m24*M2.m02 +
             M1.m34*M2.m03 + M1.m44*M2.m04 + M1.m54*M2.m05);
      x41 = (M1.m04*M2.m10 + M1.m14*M2.m11 + M1.m24*M2.m12 +
             M1.m34*M2.m13 + M1.m44*M2.m14 + M1.m54*M2.m15);
      x42 = (M1.m04*M2.m20 + M1.m14*M2.m21 + M1.m24*M2.m22 +
             M1.m34*M2.m23 + M1.m44*M2.m24 + M1.m54*M2.m25);
      x43 = (M1.m04*M2.m30 + M1.m14*M2.m31 + M1.m24*M2.m32 +
             M1.m34*M2.m33 + M1.m44*M2.m34 + M1.m54*M2.m35);
      x44 = (M1.m04*M2.m40 + M1.m14*M2.m41 + M1.m24*M2.m42 +
             M1.m34*M2.m43 + M1.m44*M2.m44 + M1.m54*M2.m45);
      x45 = (M1.m04*M2.m50 + M1.m14*M2.m51 + M1.m24*M2.m52 +
             M1.m34*M2.m53 + M1.m44*M2.m54 + M1.m54*M2.m55);

      x50 = (M1.m05*M2.m00 + M1.m15*M2.m01 + M1.m25*M2.m02 +
             M1.m35*M2.m03 + M1.m45*M2.m04 + M1.m55*M2.m05);
      x51 = (M1.m05*M2.m10 + M1.m15*M2.m11 + M1.m25*M2.m12 +
             M1.m35*M2.m13 + M1.m45*M2.m14 + M1.m55*M2.m15);
      x52 = (M1.m05*M2.m20 + M1.m15*M2.m21 + M1.m25*M2.m22 +
             M1.m35*M2.m23 + M1.m45*M2.m24 + M1.m55*M2.m25);
      x53 = (M1.m05*M2.m30 + M1.m15*M2.m31 + M1.m25*M2.m32 +
             M1.m35*M2.m33 + M1.m45*M2.m34 + M1.m55*M2.m35);
      x54 = (M1.m05*M2.m40 + M1.m15*M2.m41 + M1.m25*M2.m42 +
             M1.m35*M2.m43 + M1.m45*M2.m44 + M1.m55*M2.m45);
      x55 = (M1.m05*M2.m50 + M1.m15*M2.m51 + M1.m25*M2.m52 +
             M1.m35*M2.m53 + M1.m45*M2.m54 + M1.m55*M2.m55);

      m00 = x00;
      m01 = x01;
      m02 = x02;
      m03 = x03;
      m04 = x04;
      m05 = x05;

      m10 = x10;
      m11 = x11;
      m12 = x12;
      m13 = x13;
      m14 = x14;
      m15 = x15;

      m20 = x20;
      m21 = x21;
      m22 = x22;
      m23 = x23;
      m24 = x24;
      m25 = x25;

      m30 = x30;
      m31 = x31;
      m32 = x32;
      m33 = x33;
      m34 = x34;
      m35 = x35;

      m40 = x40;
      m41 = x41;
      m42 = x42;
      m43 = x43;
      m44 = x44;
      m45 = x45;

      m50 = x50;
      m51 = x51;
      m52 = x52;
      m53 = x53;
      m54 = x54;
      m55 = x55;
   }

   /**
    * Applies a rotational transformation R to M1 and place the result in this
    * matrix. This is equivalent to applying a rotational transform to
    * each of the 4 3x3 submatrices:
    * 
    * <pre>
    *    [ R  0 ]      [ R^T 0  ]
    *    [      ]  M1  [        ]
    *    [ 0  R ]      [ 0  R^T ]
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M1
    * matrix to transform
    */
   protected void transform (RotationMatrix3d R, Matrix6dBase M1) {
      dotransform (R, M1, /*inverse=*/false);
   }

   /**
    * Applies an inverse rotational transformation R to a matrix M1 and place
    * the result in this matrix. This is equivalent to applying an inverse
    * rotational transform to each of the 4 3x3 submatrices:
    * 
    * <pre>
    *    [ R^T  0 ]      [ R  0 ]
    *    [        ]  M1  [      ]
    *    [ 0  R^T ]      [ 0  R ]
    * </pre>
    * 
    * @param R
    * rotational transformation matrix
    * @param M1
    * matrix to transform
    */
   protected void inverseTransform (RotationMatrix3d R, Matrix6dBase M1) {
      dotransform (R, M1, /*inverse=*/true);
   }

   private void dotransform (RotationMatrix3d R, Matrix6dBase M1, boolean inverse) {

      double tmp00, tmp01, tmp02;
      double tmp10, tmp11, tmp12;
      double tmp20, tmp21, tmp22;

      double R00, R01, R02;
      double R10, R11, R12;
      double R20, R21, R22;

      if (inverse) {
         R00 = R.m00;
         R01 = R.m10;
         R02 = R.m20;
         R10 = R.m01;
         R11 = R.m11;
         R12 = R.m21;
         R20 = R.m02;
         R21 = R.m12;
         R22 = R.m22;
      }
      else {
         R00 = R.m00;
         R01 = R.m01;
         R02 = R.m02;
         R10 = R.m10;
         R11 = R.m11;
         R12 = R.m12;
         R20 = R.m20;
         R21 = R.m21;
         R22 = R.m22;
      }

      tmp00 = M1.m00 * R00 + M1.m01 * R01 + M1.m02 * R02;
      tmp01 = M1.m00 * R10 + M1.m01 * R11 + M1.m02 * R12;
      tmp02 = M1.m00 * R20 + M1.m01 * R21 + M1.m02 * R22;
      tmp10 = M1.m10 * R00 + M1.m11 * R01 + M1.m12 * R02;
      tmp11 = M1.m10 * R10 + M1.m11 * R11 + M1.m12 * R12;
      tmp12 = M1.m10 * R20 + M1.m11 * R21 + M1.m12 * R22;
      tmp20 = M1.m20 * R00 + M1.m21 * R01 + M1.m22 * R02;
      tmp21 = M1.m20 * R10 + M1.m21 * R11 + M1.m22 * R12;
      tmp22 = M1.m20 * R20 + M1.m21 * R21 + M1.m22 * R22;

      m00 = R00 * tmp00 + R01 * tmp10 + R02 * tmp20;
      m01 = R00 * tmp01 + R01 * tmp11 + R02 * tmp21;
      m02 = R00 * tmp02 + R01 * tmp12 + R02 * tmp22;
      m10 = R10 * tmp00 + R11 * tmp10 + R12 * tmp20;
      m11 = R10 * tmp01 + R11 * tmp11 + R12 * tmp21;
      m12 = R10 * tmp02 + R11 * tmp12 + R12 * tmp22;
      m20 = R20 * tmp00 + R21 * tmp10 + R22 * tmp20;
      m21 = R20 * tmp01 + R21 * tmp11 + R22 * tmp21;
      m22 = R20 * tmp02 + R21 * tmp12 + R22 * tmp22;

      tmp00 = M1.m03 * R00 + M1.m04 * R01 + M1.m05 * R02;
      tmp01 = M1.m03 * R10 + M1.m04 * R11 + M1.m05 * R12;
      tmp02 = M1.m03 * R20 + M1.m04 * R21 + M1.m05 * R22;
      tmp10 = M1.m13 * R00 + M1.m14 * R01 + M1.m15 * R02;
      tmp11 = M1.m13 * R10 + M1.m14 * R11 + M1.m15 * R12;
      tmp12 = M1.m13 * R20 + M1.m14 * R21 + M1.m15 * R22;
      tmp20 = M1.m23 * R00 + M1.m24 * R01 + M1.m25 * R02;
      tmp21 = M1.m23 * R10 + M1.m24 * R11 + M1.m25 * R12;
      tmp22 = M1.m23 * R20 + M1.m24 * R21 + M1.m25 * R22;

      m03 = R00 * tmp00 + R01 * tmp10 + R02 * tmp20;
      m04 = R00 * tmp01 + R01 * tmp11 + R02 * tmp21;
      m05 = R00 * tmp02 + R01 * tmp12 + R02 * tmp22;
      m13 = R10 * tmp00 + R11 * tmp10 + R12 * tmp20;
      m14 = R10 * tmp01 + R11 * tmp11 + R12 * tmp21;
      m15 = R10 * tmp02 + R11 * tmp12 + R12 * tmp22;
      m23 = R20 * tmp00 + R21 * tmp10 + R22 * tmp20;
      m24 = R20 * tmp01 + R21 * tmp11 + R22 * tmp21;
      m25 = R20 * tmp02 + R21 * tmp12 + R22 * tmp22;

      tmp00 = M1.m30 * R00 + M1.m31 * R01 + M1.m32 * R02;
      tmp01 = M1.m30 * R10 + M1.m31 * R11 + M1.m32 * R12;
      tmp02 = M1.m30 * R20 + M1.m31 * R21 + M1.m32 * R22;
      tmp10 = M1.m40 * R00 + M1.m41 * R01 + M1.m42 * R02;
      tmp11 = M1.m40 * R10 + M1.m41 * R11 + M1.m42 * R12;
      tmp12 = M1.m40 * R20 + M1.m41 * R21 + M1.m42 * R22;
      tmp20 = M1.m50 * R00 + M1.m51 * R01 + M1.m52 * R02;
      tmp21 = M1.m50 * R10 + M1.m51 * R11 + M1.m52 * R12;
      tmp22 = M1.m50 * R20 + M1.m51 * R21 + M1.m52 * R22;

      m30 = R00 * tmp00 + R01 * tmp10 + R02 * tmp20;
      m31 = R00 * tmp01 + R01 * tmp11 + R02 * tmp21;
      m32 = R00 * tmp02 + R01 * tmp12 + R02 * tmp22;
      m40 = R10 * tmp00 + R11 * tmp10 + R12 * tmp20;
      m41 = R10 * tmp01 + R11 * tmp11 + R12 * tmp21;
      m42 = R10 * tmp02 + R11 * tmp12 + R12 * tmp22;
      m50 = R20 * tmp00 + R21 * tmp10 + R22 * tmp20;
      m51 = R20 * tmp01 + R21 * tmp11 + R22 * tmp21;
      m52 = R20 * tmp02 + R21 * tmp12 + R22 * tmp22;

      tmp00 = M1.m33 * R00 + M1.m34 * R01 + M1.m35 * R02;
      tmp01 = M1.m33 * R10 + M1.m34 * R11 + M1.m35 * R12;
      tmp02 = M1.m33 * R20 + M1.m34 * R21 + M1.m35 * R22;
      tmp10 = M1.m43 * R00 + M1.m44 * R01 + M1.m45 * R02;
      tmp11 = M1.m43 * R10 + M1.m44 * R11 + M1.m45 * R12;
      tmp12 = M1.m43 * R20 + M1.m44 * R21 + M1.m45 * R22;
      tmp20 = M1.m53 * R00 + M1.m54 * R01 + M1.m55 * R02;
      tmp21 = M1.m53 * R10 + M1.m54 * R11 + M1.m55 * R12;
      tmp22 = M1.m53 * R20 + M1.m54 * R21 + M1.m55 * R22;

      m33 = R00 * tmp00 + R01 * tmp10 + R02 * tmp20;
      m34 = R00 * tmp01 + R01 * tmp11 + R02 * tmp21;
      m35 = R00 * tmp02 + R01 * tmp12 + R02 * tmp22;
      m43 = R10 * tmp00 + R11 * tmp10 + R12 * tmp20;
      m44 = R10 * tmp01 + R11 * tmp11 + R12 * tmp21;
      m45 = R10 * tmp02 + R11 * tmp12 + R12 * tmp22;
      m53 = R20 * tmp00 + R21 * tmp10 + R22 * tmp20;
      m54 = R20 * tmp01 + R21 * tmp11 + R22 * tmp21;
      m55 = R20 * tmp02 + R21 * tmp12 + R22 * tmp22;
   }

   /**
    * Multiplies matrix M1 by the inverse of M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @return false if M2 is singular
    */
   protected boolean mulInverseRight (Matrix6dBase M1, Matrix6dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix6d Tmp = new Matrix6d();
         nonSingular = Tmp.invert (M2);
         mul (M1, Tmp);
      }
      else {
         nonSingular = invert (M2);
         mul (M1, this);
      }
      return nonSingular;
   }

   /**
    * Multiplies the inverse of matrix M1 by M2 and places the result in this
    * matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @return false if M1 is singular
    */
   protected boolean mulInverseLeft (Matrix6dBase M1, Matrix6dBase M2) {
      boolean nonSingular = true;
      if (M1 == this || M1 == this) {
         Matrix6d Tmp = new Matrix6d();
         nonSingular = Tmp.invert (M1);
         mul (Tmp, M2);
      }
      else {
         nonSingular = invert (M1);
         mul (this, M2);
      }
      return nonSingular;
   }

   /**
    * Multiplies the inverse of matrix M1 by the inverse of M2 and places the
    * result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    * @return false if M1 or M2 is singular
    */
   protected boolean mulInverseBoth (Matrix6dBase M1, Matrix6dBase M2) {
      mul (M2, M1);
      return invert();
   }

   /**
    * Multiplies this matrix by the column vector v1 and places the result in
    * the vector vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = M v1
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    */
   public void mul (VectorNd vr, VectorNd v1) {
      if (v1.size() < 6) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size 6");
      }
      if (vr.size() < 6) {
         vr.setSize(6);
      }
      double[] br = vr.getBuffer();
      double[] b1 = v1.getBuffer();

      double r0 = (m00*b1[0] + m01*b1[1] + m02*b1[2] +
                   m03*b1[3] + m04*b1[4] + m05*b1[5]);
      double r1 = (m10*b1[0] + m11*b1[1] + m12*b1[2] +
                   m13*b1[3] + m14*b1[4] + m15*b1[5]);
      double r2 = (m20*b1[0] + m21*b1[1] + m22*b1[2] +
                   m23*b1[3] + m24*b1[4] + m25*b1[5]);
      double r3 = (m30*b1[0] + m31*b1[1] + m32*b1[2] +
                   m33*b1[3] + m34*b1[4] + m35*b1[5]);
      double r4 = (m40*b1[0] + m41*b1[1] + m42*b1[2] +
                   m43*b1[3] + m44*b1[4] + m45*b1[5]);
      double r5 = (m50*b1[0] + m51*b1[1] + m52*b1[2] +
                   m53*b1[3] + m54*b1[4] + m55*b1[5]);

      br[0] = r0;
      br[1] = r1;
      br[2] = r2;
      br[3] = r3;
      br[4] = r4;
      br[5] = r5;
   }

   /**
    * Multiplies this matrix by the column vector vr and places the result back
    * into vr. If M represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr = M vr
    * </pre>
    * 
    * @param vr
    * vector to multiply (in place)
    */
   public void mul (VectorNd vr) {
      mul (vr, vr);
   }

   /**
    * Multiplies the transpose of this matrix by the vector v1 and places the
    * result in vr. If M represents this matrix, this is equivalent to computing
    * 
    * <pre>
    *  vr = v1 M
    * </pre>
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    */
   public void mulTranspose (VectorNd vr, VectorNd v1) {
      if (v1.size() < 6) {
         throw new ImproperSizeException (
            "v1 size "+v1.size()+" < column size 6");
      }
      if (vr.size() < 6) {
         vr.setSize(6);
      }
      double[] br = vr.getBuffer();
      double[] b1 = v1.getBuffer();

      double r0 = (m00*b1[0] + m10*b1[1] + m20*b1[2] +
                   m30*b1[3] + m40*b1[4] + m50*b1[5]);
      double r1 = (m01*b1[0] + m11*b1[1] + m21*b1[2] +
                   m31*b1[3] + m41*b1[4] + m51*b1[5]);
      double r2 = (m02*b1[0] + m12*b1[1] + m22*b1[2] +
                   m32*b1[3] + m42*b1[4] + m52*b1[5]);
      double r3 = (m03*b1[0] + m13*b1[1] + m23*b1[2] +
                   m33*b1[3] + m43*b1[4] + m53*b1[5]);
      double r4 = (m04*b1[0] + m14*b1[1] + m24*b1[2] +
                   m34*b1[3] + m44*b1[4] + m54*b1[5]);
      double r5 = (m05*b1[0] + m15*b1[1] + m25*b1[2] +
                   m35*b1[3] + m45*b1[4] + m55*b1[5]);

      br[0] = r0;
      br[1] = r1;
      br[2] = r2;
      br[3] = r3;
      br[4] = r4;
      br[5] = r5;
   }

   /**
    * Multiplies the transpose of this matrix by the vector vr and places the
    * result back in vr. If M represents this matrix, this is equivalent to
    * computing
    * 
    * <pre>
    *  vr = vr M
    * </pre>
    * 
    * @param vr
    * vector to multiply by (in place)
    */
   public void mulTranspose (VectorNd vr) {
      mulTranspose (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse of this matrix and places
    * the result in vr.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return false if this matrix is singular
    */
   public boolean mulInverse (VectorNd vr, VectorNd v1) {
      Matrix6d Tmp = new Matrix6d();
      boolean nonSingular = Tmp.invert (this);
      Tmp.mul (vr, v1);
      return nonSingular;
   }

   /**
    * Multiplies the column vector vr by the inverse of this matrix and places
    * the result back in vr.
    * 
    * @param vr
    * vector to multiply by (in place)
    * @return false if this matrix is singular
    */
   public boolean mulInverse (VectorNd vr) {
      return mulInverse (vr, vr);
   }

   /**
    * Multiplies the column vector v1 by the inverse transpose of this matrix
    * and places the result in vr.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return false if this matrix is singular
    */
   public boolean mulInverseTranspose (VectorNd vr, VectorNd v1) {
      Matrix6d Tmp = new Matrix6d();
      boolean nonSingular = Tmp.invert (this);
      Tmp.mulTranspose (vr, v1);
      return nonSingular;
   }

   /**
    * Multiplies the column vector vr by the inverse transpose of this matrix
    * and places the result back in vr.
    * 
    * @param vr
    * vector to multiply by (in place)
    * @return false if this matrix is singular
    */
   public boolean mulInverseTranspose (VectorNd vr) {
      return mulInverseTranspose (vr, vr);
   }

   /**
    * Adds matrix M1 to M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void add (Matrix6dBase M1, Matrix6dBase M2) {
      m00 = M1.m00 + M2.m00;
      m01 = M1.m01 + M2.m01;
      m02 = M1.m02 + M2.m02;
      m03 = M1.m03 + M2.m03;
      m04 = M1.m04 + M2.m04;
      m05 = M1.m05 + M2.m05;

      m10 = M1.m10 + M2.m10;
      m11 = M1.m11 + M2.m11;
      m12 = M1.m12 + M2.m12;
      m13 = M1.m13 + M2.m13;
      m14 = M1.m14 + M2.m14;
      m15 = M1.m15 + M2.m15;

      m20 = M1.m20 + M2.m20;
      m21 = M1.m21 + M2.m21;
      m22 = M1.m22 + M2.m22;
      m23 = M1.m23 + M2.m23;
      m24 = M1.m24 + M2.m24;
      m25 = M1.m25 + M2.m25;

      m30 = M1.m30 + M2.m30;
      m31 = M1.m31 + M2.m31;
      m32 = M1.m32 + M2.m32;
      m33 = M1.m33 + M2.m33;
      m34 = M1.m34 + M2.m34;
      m35 = M1.m35 + M2.m35;

      m40 = M1.m40 + M2.m40;
      m41 = M1.m41 + M2.m41;
      m42 = M1.m42 + M2.m42;
      m43 = M1.m43 + M2.m43;
      m44 = M1.m44 + M2.m44;
      m45 = M1.m45 + M2.m45;

      m50 = M1.m50 + M2.m50;
      m51 = M1.m51 + M2.m51;
      m52 = M1.m52 + M2.m52;
      m53 = M1.m53 + M2.m53;
      m54 = M1.m54 + M2.m54;
      m55 = M1.m55 + M2.m55;
   }

   /**
    * Adds this matrix to M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void add (Matrix6dBase M1) {
      add (this, M1);
   }

   /**
    * Subtracts matrix M1 from M2 and places the result in this matrix.
    * 
    * @param M1
    * left-hand matrix
    * @param M2
    * right-hand matrix
    */
   protected void sub (Matrix6dBase M1, Matrix6dBase M2) {
      m00 = M1.m00 - M2.m00;
      m01 = M1.m01 - M2.m01;
      m02 = M1.m02 - M2.m02;
      m03 = M1.m03 - M2.m03;
      m04 = M1.m04 - M2.m04;
      m05 = M1.m05 - M2.m05;

      m10 = M1.m10 - M2.m10;
      m11 = M1.m11 - M2.m11;
      m12 = M1.m12 - M2.m12;
      m13 = M1.m13 - M2.m13;
      m14 = M1.m14 - M2.m14;
      m15 = M1.m15 - M2.m15;

      m20 = M1.m20 - M2.m20;
      m21 = M1.m21 - M2.m21;
      m22 = M1.m22 - M2.m22;
      m23 = M1.m23 - M2.m23;
      m24 = M1.m24 - M2.m24;
      m25 = M1.m25 - M2.m25;

      m30 = M1.m30 - M2.m30;
      m31 = M1.m31 - M2.m31;
      m32 = M1.m32 - M2.m32;
      m33 = M1.m33 - M2.m33;
      m34 = M1.m34 - M2.m34;
      m35 = M1.m35 - M2.m35;

      m40 = M1.m40 - M2.m40;
      m41 = M1.m41 - M2.m41;
      m42 = M1.m42 - M2.m42;
      m43 = M1.m43 - M2.m43;
      m44 = M1.m44 - M2.m44;
      m45 = M1.m45 - M2.m45;

      m50 = M1.m50 - M2.m50;
      m51 = M1.m51 - M2.m51;
      m52 = M1.m52 - M2.m52;
      m53 = M1.m53 - M2.m53;
      m54 = M1.m54 - M2.m54;
      m55 = M1.m55 - M2.m55;
   }

   /**
    * Subtracts this matrix from M1 and places the result in this matrix.
    * 
    * @param M1
    * right-hand matrix
    */
   protected void sub (Matrix6dBase M1) {
      sub (this, M1);
   }

   /**
    * Scales the elements of matrix M1 by <code>s</code> and places the
    * results in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    */
   protected void scale (double s, Matrix6dBase M1) {
      m00 = s * M1.m00;
      m01 = s * M1.m01;
      m02 = s * M1.m02;
      m03 = s * M1.m03;
      m04 = s * M1.m04;
      m05 = s * M1.m05;

      m10 = s * M1.m10;
      m11 = s * M1.m11;
      m12 = s * M1.m12;
      m13 = s * M1.m13;
      m14 = s * M1.m14;
      m15 = s * M1.m15;

      m20 = s * M1.m20;
      m21 = s * M1.m21;
      m22 = s * M1.m22;
      m23 = s * M1.m23;
      m24 = s * M1.m24;
      m25 = s * M1.m25;

      m30 = s * M1.m30;
      m31 = s * M1.m31;
      m32 = s * M1.m32;
      m33 = s * M1.m33;
      m34 = s * M1.m34;
      m35 = s * M1.m35;

      m40 = s * M1.m40;
      m41 = s * M1.m41;
      m42 = s * M1.m42;
      m43 = s * M1.m43;
      m44 = s * M1.m44;
      m45 = s * M1.m45;

      m50 = s * M1.m50;
      m51 = s * M1.m51;
      m52 = s * M1.m52;
      m53 = s * M1.m53;
      m54 = s * M1.m54;
      m55 = s * M1.m55;
   }


   /**
    * Computes s M1 + M2 and places the result in this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled
    * @param M2
    * matrix to be added
    */
   protected void scaledAdd (double s, Matrix6dBase M1, Matrix6dBase M2) {
      m00 = s * M1.m00 + M2.m00;
      m01 = s * M1.m01 + M2.m01;
      m02 = s * M1.m02 + M2.m02;
      m03 = s * M1.m03 + M2.m03;
      m04 = s * M1.m04 + M2.m04;
      m05 = s * M1.m05 + M2.m05;

      m10 = s * M1.m10 + M2.m10;
      m11 = s * M1.m11 + M2.m11;
      m12 = s * M1.m12 + M2.m12;
      m13 = s * M1.m13 + M2.m13;
      m14 = s * M1.m14 + M2.m14;
      m15 = s * M1.m15 + M2.m15;

      m20 = s * M1.m20 + M2.m20;
      m21 = s * M1.m21 + M2.m21;
      m22 = s * M1.m22 + M2.m22;
      m23 = s * M1.m23 + M2.m23;
      m24 = s * M1.m24 + M2.m24;
      m25 = s * M1.m25 + M2.m25;

      m30 = s * M1.m30 + M2.m30;
      m31 = s * M1.m31 + M2.m31;
      m32 = s * M1.m32 + M2.m32;
      m33 = s * M1.m33 + M2.m33;
      m34 = s * M1.m34 + M2.m34;
      m35 = s * M1.m35 + M2.m35;

      m40 = s * M1.m40 + M2.m40;
      m41 = s * M1.m41 + M2.m41;
      m42 = s * M1.m42 + M2.m42;
      m43 = s * M1.m43 + M2.m43;
      m44 = s * M1.m44 + M2.m44;
      m45 = s * M1.m45 + M2.m45;

      m50 = s * M1.m50 + M2.m50;
      m51 = s * M1.m51 + M2.m51;
      m52 = s * M1.m52 + M2.m52;
      m53 = s * M1.m53 + M2.m53;
      m54 = s * M1.m54 + M2.m54;
      m55 = s * M1.m55 + M2.m55;
   }

   /**
    * Computes s M1 and adds the result to this matrix.
    * 
    * @param s
    * scaling factor
    * @param M1
    * matrix to be scaled and added
    */
   public void scaledAdd (double s, Matrix6dBase M1) {
      m00 += s * M1.m00;
      m01 += s * M1.m01;
      m02 += s * M1.m02;
      m03 += s * M1.m03;
      m04 += s * M1.m04;
      m05 += s * M1.m05;

      m10 += s * M1.m10;
      m11 += s * M1.m11;
      m12 += s * M1.m12;
      m13 += s * M1.m13;
      m14 += s * M1.m14;
      m15 += s * M1.m15;

      m20 += s * M1.m20;
      m21 += s * M1.m21;
      m22 += s * M1.m22;
      m23 += s * M1.m23;
      m24 += s * M1.m24;
      m25 += s * M1.m25;

      m30 += s * M1.m30;
      m31 += s * M1.m31;
      m32 += s * M1.m32;
      m33 += s * M1.m33;
      m34 += s * M1.m34;
      m35 += s * M1.m35;

      m40 += s * M1.m40;
      m41 += s * M1.m41;
      m42 += s * M1.m42;
      m43 += s * M1.m43;
      m44 += s * M1.m44;
      m45 += s * M1.m45;

      m50 += s * M1.m50;
      m51 += s * M1.m51;
      m52 += s * M1.m52;
      m53 += s * M1.m53;
      m54 += s * M1.m54;
      m55 += s * M1.m55;
   }


   /**
    * Sets this matrix to the negative of M1.
    * 
    * @param M1
    * matrix to negate
    */
   protected void negate (Matrix6dBase M1) {
      scale (-1, M1);
   }

   /**
    * Negates this matrix in place.
    */
   public void negate() {
      negate (this);
   }

   /**
    * Transposes this matrix in place.
    */
   public void transpose() {
      double tmp;

      tmp = m01;
      m01 = m10;
      m10 = tmp;
      tmp = m02;
      m02 = m20;
      m20 = tmp;
      tmp = m03;
      m03 = m30;
      m30 = tmp;
      tmp = m04;
      m04 = m40;
      m40 = tmp;
      tmp = m05;
      m05 = m50;
      m50 = tmp;

      tmp = m12;
      m12 = m21;
      m21 = tmp;
      tmp = m13;
      m13 = m31;
      m31 = tmp;
      tmp = m14;
      m14 = m41;
      m41 = tmp;
      tmp = m15;
      m15 = m51;
      m51 = tmp;

      tmp = m23;
      m23 = m32;
      m32 = tmp;
      tmp = m24;
      m24 = m42;
      m42 = tmp;
      tmp = m25;
      m25 = m52;
      m52 = tmp;

      tmp = m34;
      m34 = m43;
      m43 = tmp;
      tmp = m35;
      m35 = m53;
      m53 = tmp;

      tmp = m45;
      m45 = m54;
      m54 = tmp;
   }

   /**
    * Sets this matrix to the transpose of M
    *
    * @param M
    * matrix to take the transpose of
    */
   protected void transpose (Matrix6dBase M) {
      double tmp01 = M.m01;
      double tmp02 = M.m02;
      double tmp03 = M.m03;
      double tmp04 = M.m04;
      double tmp05 = M.m05;
      double tmp12 = M.m12;
      double tmp13 = M.m13;
      double tmp14 = M.m14;
      double tmp15 = M.m15;
      double tmp23 = M.m23;
      double tmp24 = M.m24;
      double tmp25 = M.m25;
      double tmp34 = M.m34;
      double tmp35 = M.m35;
      double tmp45 = M.m45;

      m00 = M.m00;
      m11 = M.m11;
      m22 = M.m22;
      m33 = M.m33;
      m44 = M.m44;
      m55 = M.m55;

      m01 = M.m10;
      m02 = M.m20;
      m03 = M.m30;
      m04 = M.m40;
      m05 = M.m50;
      m12 = M.m21;
      m13 = M.m31;
      m14 = M.m41;
      m15 = M.m51;
      m23 = M.m32;
      m24 = M.m42;
      m25 = M.m52;
      m34 = M.m43;
      m35 = M.m53;
      m45 = M.m54;

      m10 = tmp01;
      m20 = tmp02;
      m30 = tmp03;
      m40 = tmp04;
      m50 = tmp05;
      m21 = tmp12;
      m31 = tmp13;
      m41 = tmp14;
      m51 = tmp15;
      m32 = tmp23;
      m42 = tmp24;
      m52 = tmp25;
      m43 = tmp34;
      m53 = tmp35;
      m54 = tmp45;
   }

   /**
    * Sets this matrix to the identity.
    */
   public void setIdentity() {
      setZero();

      m00 = 1.0;
      m11 = 1.0;
      m22 = 1.0;
      m33 = 1.0;
      m44 = 1.0;
      m55 = 1.0;
   }

   /**
    * Sets the elements of this matrix to zero.
    */
   protected void setZero() {
      m00 = 0.0;
      m01 = 0.0;
      m02 = 0.0;
      m03 = 0.0;
      m04 = 0.0;
      m05 = 0.0;

      m10 = 0.0;
      m11 = 0.0;
      m12 = 0.0;
      m13 = 0.0;
      m14 = 0.0;
      m15 = 0.0;

      m20 = 0.0;
      m21 = 0.0;
      m22 = 0.0;
      m23 = 0.0;
      m24 = 0.0;
      m25 = 0.0;

      m30 = 0.0;
      m31 = 0.0;
      m32 = 0.0;
      m33 = 0.0;
      m34 = 0.0;
      m35 = 0.0;

      m40 = 0.0;
      m41 = 0.0;
      m42 = 0.0;
      m43 = 0.0;
      m44 = 0.0;
      m45 = 0.0;

      m50 = 0.0;
      m51 = 0.0;
      m52 = 0.0;
      m53 = 0.0;
      m54 = 0.0;
      m55 = 0.0;
   }

   /**
    * Returns true if the elements of this matrix equal those of matrix
    * <code>M1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param M1
    * matrix to compare with
    * @param epsilon
    * comparison tolerance
    * @return false if the matrices are not equal within the specified tolerance
    */
   public boolean epsilonEquals (Matrix6dBase M1, double epsilon) {
      if (abs (m00 - M1.m00) <= epsilon && abs (m01 - M1.m01) <= epsilon &&
          abs (m02 - M1.m02) <= epsilon && abs (m03 - M1.m03) <= epsilon &&
          abs (m04 - M1.m04) <= epsilon && abs (m05 - M1.m05) <= epsilon &&

          abs (m10 - M1.m10) <= epsilon && abs (m11 - M1.m11) <= epsilon &&
          abs (m12 - M1.m12) <= epsilon && abs (m13 - M1.m13) <= epsilon &&
          abs (m14 - M1.m14) <= epsilon && abs (m15 - M1.m15) <= epsilon &&

          
          abs (m20 - M1.m20) <= epsilon && abs (m21 - M1.m21) <= epsilon &&
          abs (m22 - M1.m22) <= epsilon && abs (m23 - M1.m23) <= epsilon &&
          abs (m24 - M1.m24) <= epsilon && abs (m25 - M1.m25) <= epsilon &&

          
          abs (m30 - M1.m30) <= epsilon && abs (m31 - M1.m31) <= epsilon &&
          abs (m32 - M1.m32) <= epsilon && abs (m33 - M1.m33) <= epsilon &&
          abs (m34 - M1.m34) <= epsilon && abs (m35 - M1.m35) <= epsilon &&

          
          abs (m40 - M1.m40) <= epsilon && abs (m41 - M1.m41) <= epsilon &&
          abs (m42 - M1.m42) <= epsilon && abs (m43 - M1.m43) <= epsilon &&
          abs (m44 - M1.m44) <= epsilon && abs (m45 - M1.m45) <= epsilon &&

          
          abs (m50 - M1.m50) <= epsilon && abs (m51 - M1.m51) <= epsilon &&
          abs (m52 - M1.m52) <= epsilon && abs (m53 - M1.m53) <= epsilon &&
          abs (m54 - M1.m54) <= epsilon && abs (m55 - M1.m55) <= epsilon) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if the elements of this matrix exactly equal those of matrix
    * <code>M1</code>.
    * 
    * @param M1
    * matrix to compare with
    * @return false if the matrices are not equal
    */
   public boolean equals (Matrix6dBase M1) {
      if ((m00 == M1.m00) && (m01 == M1.m01) && (m02 == M1.m02) &&
          (m03 == M1.m03) && (m04 == M1.m04) && (m05 == M1.m05) &&

          (m10 == M1.m10) && (m11 == M1.m11) && (m12 == M1.m12) &&
          (m13 == M1.m13) && (m14 == M1.m14) && (m15 == M1.m15) &&

          (m20 == M1.m20) && (m21 == M1.m21) && (m22 == M1.m22) &&
          (m23 == M1.m23) && (m24 == M1.m24) && (m25 == M1.m25) &&

          (m30 == M1.m30) && (m31 == M1.m31) && (m32 == M1.m32) &&
          (m33 == M1.m33) && (m34 == M1.m34) && (m35 == M1.m35) &&

          (m40 == M1.m40) && (m41 == M1.m41) && (m42 == M1.m42) &&
          (m43 == M1.m43) && (m44 == M1.m44) && (m45 == M1.m45) &&

          (m50 == M1.m50) && (m51 == M1.m51) && (m52 == M1.m52) &&
          (m53 == M1.m53) && (m54 == M1.m54) && (m55 == M1.m55)) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Returns the infinity norm of this matrix. This is equal to the maximum of
    * the vector 1-norm of each row.
    * 
    * @return infinity norm of this matrix
    */
   public double infinityNorm() {
      // returns the largest row sum of the absolute value\
      double abs0, abs1, abs2, abs3, abs4, abs5;
      double max, sum;

      abs0 = (m00 >= 0 ? m00 : -m00);
      abs1 = (m01 >= 0 ? m01 : -m01);
      abs2 = (m02 >= 0 ? m02 : -m02);
      abs3 = (m03 >= 0 ? m03 : -m03);
      abs4 = (m04 >= 0 ? m04 : -m04);
      abs5 = (m05 >= 0 ? m05 : -m05);
      max = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;

      abs0 = (m10 >= 0 ? m10 : -m10);
      abs1 = (m11 >= 0 ? m11 : -m11);
      abs2 = (m12 >= 0 ? m12 : -m12);
      abs3 = (m13 >= 0 ? m13 : -m13);
      abs4 = (m14 >= 0 ? m14 : -m14);
      abs5 = (m15 >= 0 ? m15 : -m15);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m20 >= 0 ? m20 : -m20);
      abs1 = (m21 >= 0 ? m21 : -m21);
      abs2 = (m22 >= 0 ? m22 : -m22);
      abs3 = (m23 >= 0 ? m23 : -m23);
      abs4 = (m24 >= 0 ? m24 : -m24);
      abs5 = (m25 >= 0 ? m25 : -m25);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m30 >= 0 ? m30 : -m30);
      abs1 = (m31 >= 0 ? m31 : -m31);
      abs2 = (m32 >= 0 ? m32 : -m32);
      abs3 = (m33 >= 0 ? m33 : -m33);
      abs4 = (m34 >= 0 ? m34 : -m34);
      abs5 = (m35 >= 0 ? m35 : -m35);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m40 >= 0 ? m40 : -m40);
      abs1 = (m41 >= 0 ? m41 : -m41);
      abs2 = (m42 >= 0 ? m42 : -m42);
      abs3 = (m43 >= 0 ? m43 : -m43);
      abs4 = (m44 >= 0 ? m44 : -m44);
      abs5 = (m45 >= 0 ? m45 : -m45);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m50 >= 0 ? m50 : -m50);
      abs1 = (m51 >= 0 ? m51 : -m51);
      abs2 = (m52 >= 0 ? m52 : -m52);
      abs3 = (m53 >= 0 ? m53 : -m53);
      abs4 = (m54 >= 0 ? m54 : -m54);
      abs5 = (m55 >= 0 ? m55 : -m55);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      return max;
   }

   /**
    * Returns the 1 norm of this matrix. This is equal to the maximum of the
    * vector 1-norm of each column.
    * 
    * @return 1 norm of this matrix
    */
   public double oneNorm() {
      // returns the largest column sum of the absolute value
      double abs0, abs1, abs2, abs3, abs4, abs5;
      double max, sum;

      abs0 = (m00 >= 0 ? m00 : -m00);
      abs1 = (m10 >= 0 ? m10 : -m10);
      abs2 = (m20 >= 0 ? m20 : -m20);
      abs3 = (m30 >= 0 ? m30 : -m30);
      abs4 = (m40 >= 0 ? m40 : -m40);
      abs5 = (m50 >= 0 ? m50 : -m50);
      max = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;

      abs0 = (m01 >= 0 ? m01 : -m01);
      abs1 = (m11 >= 0 ? m11 : -m11);
      abs2 = (m21 >= 0 ? m21 : -m21);
      abs3 = (m31 >= 0 ? m31 : -m31);
      abs4 = (m41 >= 0 ? m41 : -m41);
      abs5 = (m51 >= 0 ? m51 : -m51);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m02 >= 0 ? m02 : -m02);
      abs1 = (m12 >= 0 ? m12 : -m12);
      abs2 = (m22 >= 0 ? m22 : -m22);
      abs3 = (m32 >= 0 ? m32 : -m32);
      abs4 = (m42 >= 0 ? m42 : -m42);
      abs5 = (m52 >= 0 ? m52 : -m52);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m03 >= 0 ? m03 : -m03);
      abs1 = (m13 >= 0 ? m13 : -m13);
      abs2 = (m23 >= 0 ? m23 : -m23);
      abs3 = (m33 >= 0 ? m33 : -m33);
      abs4 = (m43 >= 0 ? m43 : -m43);
      abs5 = (m53 >= 0 ? m53 : -m53);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m04 >= 0 ? m04 : -m04);
      abs1 = (m14 >= 0 ? m14 : -m14);
      abs2 = (m24 >= 0 ? m24 : -m24);
      abs3 = (m34 >= 0 ? m34 : -m34);
      abs4 = (m44 >= 0 ? m44 : -m44);
      abs5 = (m54 >= 0 ? m54 : -m54);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      abs0 = (m05 >= 0 ? m05 : -m05);
      abs1 = (m15 >= 0 ? m15 : -m15);
      abs2 = (m25 >= 0 ? m25 : -m25);
      abs3 = (m35 >= 0 ? m35 : -m35);
      abs4 = (m45 >= 0 ? m45 : -m45);
      abs5 = (m55 >= 0 ? m55 : -m55);
      sum = abs0 + abs1 + abs2 + abs3 + abs4 + abs5;
      if (sum > max) {
         max = sum;
      }
      return max;
   }

   /**
    * Returns the Frobenius norm of this matrix. This is equal to the square
    * root of the sum of the squares of each element.
    * 
    * @return Frobenius norm of this matrix
    */
   public double frobeniusNorm() {
      // returns sqrt(sum (diag (M'*M))
      double sum =
         (m00*m00 + m01*m01 + m02*m02 + m03*m03 + m04*m04 + m05*m05
         + m10*m10 + m11*m11 + m12*m12 + m13*m13 + m14*m14
         + m15*m15 + m20*m20 + m21*m21 + m22*m22 + m23*m23
         + m24*m24 + m25*m25 + m30*m30 + m31*m31 + m32*m32
         + m33*m33 + m34*m34 + m35*m35 + m40*m40 + m41*m41 + m42
        *m42 + m43*m43 + m44*m44 + m45*m45 + m50*m50 + m51*m51
         + m52*m52 + m53*m53 + m54*m54 + m55*m55);
      return Math.sqrt (sum);
   }

   /**
    * Inverts this matrix in place, returning false if the matrix is detected to
    * be singular. The inverse is computed using an LU decomposition with
    * partial pivoting.
    */
   public boolean invert() {
      return invert (this);
   }

   /**
    * Inverts the matrix M and places the result in this matrix, return false if
    * M is detected to be singular. The inverse is computed using an LU
    * decomposition with partial pivoting.
    * 
    * @param M1
    * matrix to invert
    * @return false if M is singular
    */
   protected boolean invert (Matrix6dBase M1) {
      LUDecomposition lu = new LUDecomposition();
      lu.factor (M1);
      boolean singular = false;
      try {
         singular = lu.inverse (this);
      }
      catch (ImproperStateException e) { // can't happen
      }
      return singular;
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified by the
    * array vals.
    * 
    * @param vals
    * diagonal values
    */
   protected void setDiagonal (double[] vals) {
      if (vals.length < 6) {
         throw new IllegalArgumentException (
            "vals must have a length of at least 6");
      }
      setZero();
      m00 = vals[0];
      m11 = vals[1];
      m22 = vals[2];
      m33 = vals[3];
      m44 = vals[4];
      m55 = vals[5];
   }

   /**
    * Sets this matrix to a diagonal matrix whose values are specified.
    * 
    * @param m00 first diagonal value
    * @param m11 second diagonal value
    * @param m22 third diagonal value
    * @param m33 fourth diagonal value
    * @param m44 fifth diagonal value
    * @param m55 sixth diagonal value
    */
   protected void setDiagonal (
      double m00, double m11, double m22, double m33, double m44, double m55) {
      setZero();
      this.m00 = m00;
      this.m11 = m11;
      this.m22 = m22;
      this.m33 = m33;
      this.m44 = m44;
      this.m55 = m55;
   }

   /**
    * Returns the determinant of this matrix
    * 
    * @return matrix determinant
    */
   public double determinant() throws ImproperSizeException {
      LUDecomposition lu = new LUDecomposition();
      lu.factor (this);
      return (lu.determinant());
   }

   public Matrix6dBase clone() {
      try {
         return (Matrix6dBase)super.clone();
      }
      catch (CloneNotSupportedException e) { // shouldn't happen
         throw new InternalErrorException ("clone failed for Matrix6dBase");
      }
   }

}
