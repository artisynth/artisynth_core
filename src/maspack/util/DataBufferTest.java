/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public class DataBufferTest {

   public void storeUsingRaw (DataBuffer buf, double[] data) {
      buf.resetOffsets();
      double[] dbuf = buf.dbuffer();
      int di = buf.doff;
      for (int i=0; i<data.length; i++) {
         dbuf[di++] = data[i];
      }
      buf.doff = di;
   }      

   public void storeUsingAppend (DataBuffer buf, double[] data) {
      buf.resetOffsets();
      for (int i=0; i<data.length; i++) {
         buf.dput (data[i]);
      }
   }      

   public void timing() {
      
      // do some timing tests
      int size = 10000000;
      double[] stuff = new double[size];
      double val = 0.98352679;
      for (int i=0; i<size; i++) {
         val *= 1.00567384637;
         stuff[i] = val;
      }
      
      int cnt = 100;
      DataBuffer buf = new DataBuffer (0, size, 0);
      for (int i=0; i<cnt; i++) {
         storeUsingRaw (buf, stuff);
         storeUsingAppend (buf, stuff);
      }

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<cnt; i++) {
         storeUsingRaw (buf, stuff);
      }
      timer.stop();
      System.out.println ("raw time: " + timer.result(cnt));

      timer.start();
      for (int i=0; i<cnt; i++) {
         storeUsingAppend (buf, stuff);
      }
      timer.stop();
      System.out.println ("append time: " + timer.result(cnt));
   }

   public static void main (String[] args) {

      DataBufferTest tester = new DataBufferTest();
      tester.timing();
   }



}