/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Base unit test class
 */
public class UnitTest {

   public void test() {
   }

   public void runtest() {
      try {
         test();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");      
   }      

}
