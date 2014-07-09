/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matlab;

import java.io.IOException;

import maspack.matrix.MatrixNd;

public class MatlabInterfaceTest
{

   MatlabInterface matlab;

   public static void main(String[] args)
   {
      MatlabInterfaceTest main = new MatlabInterfaceTest();
   }

   public MatlabInterfaceTest()
   {
      try
      {
	 matlab = new MatlabInterface();
	 matlab.open();

	 testGetMatrix();
//	 testSetMatrix();
//	 testSetSparse();
//	 testIntArray();
//	 testWriteArray();
//	 testReadArray();

	 closeEngine();

      }
      catch (Exception e)
      {
	 System.out.println("MatlabInterface Test failed.");
	 System.err.println(e.getMessage());
      }

   }
   
   public void testSetMatrix() throws IOException
   {
      double[][] mat = new double[][]{{1, 2, 3},{4, 5, 6}, {7, 8, 9}};
      MatrixNd M = new MatrixNd(mat);
      System.out.println("Java: M = \n" + M.toString());
      
      matlab.putMatrix("M", mat);
      matlab.evalString("X = M*2");

      // Get output
      System.out.println(matlab.getOutputString(500));
   }
   
   public void testGetMatrix() throws IOException
   {
      String name = "X";
//      String str = name + " = [3, 4; 9, 6; 5, 2];";
//      double[][] mat = new double[3][2];
//      String str = name + " = gallery(5);";
//      double[][] mat = new double[5][5];
      String str = name + " = [gallery(3); gallery(3)*pi];";
      double[][] mat = new double[6][3];

      System.out.println("Send cmd: " + str);
      matlab.evalString(str);
      matlab.getMatrix("X", mat);
      MatrixNd M = new MatrixNd(mat);
      System.out.println("M = \n" + M.toString());
   }
   
   public void testSetSparse() throws IOException
   {
	 double[] I = new double[]{1,2,3,4,5};
//	 int[] I = new int[]{1,2,3,4,5};
	 double[] S = new double[]{3.3, 4.5, 6.6, 1.0, 9.0};
	 matlab.setSparse(I, I, S);
	 matlab.evalString("inv(A)");
	 System.out.println(matlab.getOutputString(500));
   }
   
   public void testIntArray() throws IOException
   {
	 int[] I = new int[]{1,2,3,4,5};
	 matlab.putIntArray("I", I);
	 matlab.evalString("I = I*2");
	 matlab.getIntArray("I", I);
	 System.out.print("I = ");
	 for (int i = 0; i < I.length; i++)
	 {
	    if (i == I.length-1)
	       System.out.println(I[i]);
	    else
	       System.out.print(I[i] + ", ");
	 }
	 System.out.println(matlab.getOutputString(500));
   }

   public void testWriteArray() throws IOException
   {
      double[] array = new double[] { 1.1, 2.2, 3.2, 4.4, 5.5, 6.6 };
      System.out.print("input array = [ ");
      for (int i = 0; i < array.length; i++)
	 System.out.print(array[i] + ", ");
      System.out.println(" ]");
      matlab.putArray("FOO", array);
      matlab.evalString("FOO*2");

      // Get output
      System.out.println(matlab.getOutputString(500));
   }

   public void testReadArray() throws IOException
   {
      // engine.evalString("matlabarray = [1.0,2.0,3.0,4.0,5.0,6.0]'");
      String name = "X";
      String str = name + " = [1.5,0.4,6.0]";
      System.out.println("Send cmd: " + str);

      matlab.evalString(str);
//      System.out.println(engine.getOutputString(500));

      double[] array2 = new double[3];

      matlab.getArray(name, array2);
      if (array2 != null)
      {
	 System.out.print("Read " + name + " = [ ");
	 for (int i = 0; i < array2.length; i++)
	    System.out.print(array2[i] + ", ");
	 System.out.println(" ]");
      }
      // Get output
//      System.out.println(engine.getOutputString(500));
   }
   
   public void closeEngine() throws IOException
   {
	 // Close Matlab session
	 matlab.close();
   }

}
