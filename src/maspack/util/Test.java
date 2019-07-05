/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public class Test {

   public static void main (String[] args) {
      FunctionTimer timer = new FunctionTimer();

      long l = 0;
      long d = 0;
      double v = 0;
      int cnt = 10000000;

      timer.start();
      for (int i = 0; i < cnt; i++) {
         l = 0;
         for (int k = 0; k < 10; k++) {
            l = l * 10 + k;
         }
      }
      timer.stop();
      System.out.println ("long: " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         v = 0;
         for (int k = 0; k < 10; k++) {
            v = v * 10 + k;
         }
      }
      timer.stop();
      System.out.println ("double: " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         d = 0;
         for (int k = 0; k < 10; k++) {
            d = d * 10 + k;
         }
      }
      timer.stop();
      System.out.println ("int: " + timer.result (cnt));
   }
}
