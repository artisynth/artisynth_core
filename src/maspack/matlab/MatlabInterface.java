/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matlab;

import java.io.IOException;

import maspack.fileutil.NativeLibraryManager;
import maspack.util.FunctionTimer;

public class MatlabInterface
{

   boolean nativeSupportLoaded = false;

   public MatlabInterface() throws UnsupportedOperationException
   {
      if (!nativeSupportLoaded)
      { // try loading in the native code
	 try
	 {
	    NativeLibraryManager.load("MatlabInterface");
	    nativeSupportLoaded = true;
	 }
	 catch (UnsatisfiedLinkError e)
	 {
	    // e.printStackTrace();
	    System.out.println(e.getMessage());
	    throw new UnsupportedOperationException(
		     "Can't load native library \"MatlabInterface\"");
	 }
      }

   }

   public void open()
   {
      try
      {
//	 open("matlab -nosplash -nojvm -c 1711@localhost");
	 open("matlab -nosplash -nojvm");
	 System.out.println(getOutputString(500));
      }
      catch (IOException e)
      {
	 System.err
		  .println("MatlabInterface.open(): unable to open Matlab session");
	 System.err.println(e.getMessage());
      }
   }

   public void clearWorkspace()
   {
      try
      {
	 evalString("clear all;");
      }
      catch (IOException e)
      {
	 System.err.println("clearing workspace failed.");
      }
   }
   
   public void setSparseIndices(double[] rowIdxs, double[] colIdxs) throws IOException
   {
      putArray("I", rowIdxs);
      putArray("J", colIdxs);
   }
   
   public void setSparseVals(double[] vals) throws IOException
   {
      // indices, "I" and "J" should already be set in workspace
      putArray("S", vals);
      evalString("A = sparse(I,J,S);");
   }
   
   FunctionTimer settimer = new FunctionTimer();
   public void setSparseValsTimed(double[] vals) throws IOException
   {
      // indices, "I" and "J" should already be set in workspace
      settimer.start();
      putArray("S", vals);
      settimer.stop();
      System.out.println("put vals time = " + settimer.result(1));
      
      settimer.start();
      evalString("A = sparse(I,J,S);");
      settimer.stop();
      System.out.println("eval sparse() time = " + settimer.result(1));
   }
   
   
   public void setSparse(double[] rowIdxs, double[] colIdxs, double[] vals) throws IOException
   {
      putArray("I", rowIdxs);
      putArray("J", colIdxs);
      putArray("S", vals);
      evalString("A = sparse(I,J,S);");
   }

   public void solveSparse(double[] rowIdxs, double[] colIdxs, double[] vals, 
	    double[] x, double[] b)
   {
      try {
	 setSparse(rowIdxs, colIdxs, vals);
      } catch (IOException e)
      {
	 System.err.println("Cannot set A matrix in Matlab workspace");
      }
      solveSparse(x,b);
   }
   
   public void solveSparse(double[] x, double[] b)
   {
      try {
	 putArray("b", b);
      } catch (IOException e)
      {
	 System.err.println("Cannot set b vector in Matlab workspace");
      }
      
      try {
	 evalString("x = A \\ b';");
      } catch (IOException e)
      {
	 System.err.println("Error evaluating \"x = A \\ b'\"");
      }
      
      try {
	 getArray("x",x);
      } catch (IOException e)
      {
	 System.err.println("Cannot get x vector from Matlab workspace");
      }
   }
   
   FunctionTimer solvetimer = new FunctionTimer();
   public void solveSparseTimed(double[] x, double[] b)
   {
      try {
	 solvetimer.start();
	 putArray("b", b);
	 solvetimer.stop();
	 System.out.println("put b time = " + solvetimer.result(1));
      } catch (IOException e)
      {
	 System.err.println("Cannot set b vector in Matlab workspace");
      }
      
      try {
	 solvetimer.start();
	 evalString("x = A \\ b';");
	 solvetimer.stop();
	 System.out.println("eval solve time = " + solvetimer.result(1));
      } catch (IOException e)
      {
	 System.err.println("Error evaluating \"x = A \\ b'\"");
      }
      
      try {
	 solvetimer.start();
	 getArray("x",x);
	 solvetimer.stop();
	 System.out.println("get x time = " + solvetimer.result(1));
      } catch (IOException e)
      {
	 System.err.println("Cannot get x vector from Matlab workspace");
      }
   }

   
   private native void open(String str) throws IOException;

   public native void close() throws IOException;

   public native void evalString(String str) throws IOException;

   public native String getOutputString(int size) throws IOException;

   public native void putArray(String name, double[] array) throws IOException;

   public native void getArray(String name, double[] array) throws IOException;
   
   public native void putIntArray(String name, int[] array) throws IOException;

   public native void getIntArray(String name, int[] array) throws IOException;
   
   public native void putMatrix(String name, double[][] matrix) throws IOException;
   
   public native void getMatrix(String name, double[][] matrix) throws IOException;
//   public native double[][] getMatrix(String name) throws IOException;
   
}
