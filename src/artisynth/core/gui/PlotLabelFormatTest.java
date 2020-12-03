package artisynth.core.gui;

import maspack.util.*;

public class PlotLabelFormatTest extends UnitTest {
   
   private void test (double num, boolean useExp, String chk) {
      PlotLabelFormat fmt = new PlotLabelFormat();
      String str = fmt.createLabel (num, useExp);
      if (!str.equals (chk)) {
         throw new TestException (
            "Error: formatted to '"+str+"', expected '"+chk+"'");
      }
   }

   private void test (long m, int exp, String chk, String expChk) {
      PlotLabelFormat fmt = new PlotLabelFormat();
      String str = fmt.createLabel (m, exp, false);
      if (!str.equals (chk)) {
         throw new TestException (
            "Error: formatted to '"+str+"', expected '"+chk+"'");
      }
      str = fmt.createLabel (m, exp, true);
      if (!str.equals (expChk)) {
         throw new TestException (
            "Error: formatted to '"+str+"', expected '"+expChk+"'");
      }
   }

   private void test (long m, int exp, String chk) {
      test (m, exp, chk, chk);
   }

   public void test() {
      test (0, false, "0");
      test (1.0, false, "1");
      test (10.0, false, "10");
      test (12300.0, false, "12300");
      test (1230000.0, false, "1230000"); 
      test (12300000.0, false, "1.23E7");
      test (1200000.0, false, "1.2E6");
      test (100000.0, false, "1.0E5");
      test (1200000000.0, false, "1.2E9");
      test (123.0, false, "123");
      test (123.4, false, "123.4");
      test (123.45, false, "123.45");
      test (0.1, false, "0.1");
      test (0.01, false, "0.01");
      test (0.001, false, "0.001");
      test (0.001234, false, "0.001234");      
      test (0.0001234, false, "1.234E-4");
      test (0.000000000012345, false, "1.2345E-11");

      test (0, true, "0");
      test (1.0, true, "1.0E0");
      test (10.0, true, "1.0E1");
      test (12300.0, true, "1.23E4");
      test (1230000.0, true, "1.23E6"); 
      test (12300000.0, true, "1.23E7");
      test (1200000.0, true, "1.2E6");
      test (1200000000.0, true, "1.2E9");
      test (123.0, true, "1.23E2");
      test (123.4, true, "1.234E2");
      test (123.45, true, "1.2345E2");
      test (0.1, true, "1.0E-1");
      test (0.01, true, "1.0E-2");
      test (0.001, true, "1.0E-3");
      test (0.001234, true, "1.234E-3");      
      test (0.0001234, true, "1.234E-4");
      test (0.000000000012345, true, "1.2345E-11");   

      test (0, 0, "0", "0");
      test (0, 2, "0", "0");
      test (34, 2, "3400", "3.4E3");
      test (-34, 2, "-3400","-3.4E3");
      test (123, 2, "12300", "1.23E4");
      test (12300, 2, "1230000", "1.23E6");
      test (123000, 2, "1.23E7");
      test (12300, 3, "1.23E7");
      test (123000, 4, "1.23E9");
      test (-123000, 4, "-1.23E9");
      test (1230000, 1, "1.23E7");
      test (12300000, 0, "1.23E7");
      test (123000000, -1, "1.23E7");
      test (456, 2, "45600", "4.56E4");
      test (456, 3, "456000", "4.56E5");
      test (456, 4, "4560000", "4.56E6");
      test (456, 5, "4.56E7");
      test (45, 5, "4.5E6");
      test (-45, 5, "-4.5E6");
      test (1, -3, "0.001", "1.0E-3");
      test (-1, -3, "-0.001", "-1.0E-3");
      test (1, -2, "0.01", "1.0E-2");
      test (-1, -2, "-0.01", "-1.0E-2");
      test (1, -1, "0.1", "1.0E-1");
      test (1, -4, "1.0E-4");
      test (-1, -4, "-1.0E-4");
      test (1, -5, "1.0E-5");
      test (82, -2, "0.82", "8.2E-1");
      test (82, -3, "0.082", "8.2E-2");
      test (82, -4, "0.0082", "8.2E-3");
      test (82, -5, "8.2E-4", "8.2E-4");
      test (83, -6, "8.3E-5");
      test (83, -7, "8.3E-6");
      test (523, 0, "523", "5.23E2");
      test (523, -1, "52.3", "5.23E1");
      test (523, -2, "5.23", "5.23E0");
      test (523, -3, "0.523", "5.23E-1");
      test (523, -4, "0.0523", "5.23E-2");
      test (523, -5, "0.00523", "5.23E-3");
      test (523, -6, "5.23E-4");
      test (523, -7, "5.23E-5");
      test (-523, -7, "-5.23E-5");
   
   }

   public static void main (String[] args) {
      PlotLabelFormatTest tester = new PlotLabelFormatTest();
      tester.runtest();
   }

}
   
