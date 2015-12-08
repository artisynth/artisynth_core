/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import maspack.util.*;
import maspack.fileutil.*;
import maspack.fileutil.NativeLibraryManager.LibDesc;
import maspack.fileutil.NativeLibraryManager.SystemType;

/**
 * Test class for NativeLibraryManager.
 */
public class NativeLibraryManagerTest {

   NativeLibraryManager myManager;

   NativeLibraryManagerTest () {
      myManager = new NativeLibraryManager();
   }

   public void parseTest (
      String name, String baseName, int major, int minor, String vstr,
      SystemType type) {

      LibDesc desc = null;
      try {
         desc = myManager.createDesc (name);
      }
      catch (Exception e) {
         if (baseName != null) {
            throw new TestException ("Unexpected exception "+e);
         }
         else {
            return;
         }
      }
      if (baseName == null) {
         throw new TestException ("Expected parse failure");
      }
      if (!baseName.equals (desc.myBasename)) {
         throw new TestException (
            "Expected base name "+baseName+", got "+desc.myBasename);
      }
      if (!vstr.equals (desc.myVersionStr)) {
         throw new TestException (
            "Expected version str "+vstr+", got "+desc.myVersionStr);
      }
      if (major != desc.myMajorNum) {
         throw new TestException (
            "Expected major num "+major+", got "+desc.myMajorNum);
      }
      if (minor != desc.myMinorNum) {
         throw new TestException (
            "Expected minor num "+minor+", got "+desc.myMinorNum);
      }
      if (type != desc.mySys) {
         throw new TestException (
            "Expected system type "+type+", got "+desc.mySys);
      }
   }

   public void matchMinorTest (
      SystemType type, String libName, String fileName, int minor) {
      
      myManager.mySystemType = type;
      LibDesc desc = null;
      try {
         desc = myManager.createDesc (libName);
      }
      catch (Exception e) {
         throw new TestException ("Unexpected exception "+e);
      }
      int mnum = desc.majorNameMatches (fileName, myManager.mySystemType);
      if (mnum != minor) {
         throw new TestException (
            "Major name of "+fileName+" to "+libName+", system "+type+
            ": expected "+minor+", got "+mnum);
      }
   }      

   public void test () {
      // check bad file names
      parseTest (".4", null, 0, 0, null, null);
      parseTest ("foo.so", null, 0, 0, null, null);
      parseTest ("foo.dylib", null, 0, 0, null, null);
      parseTest ("lib.so", null, 0, 0, null, null);
      parseTest ("lib.dylib", null, 0, 0, null, null);

      // check good file names
      parseTest ("libfoo.so", "foo", -1, -1, "", SystemType.Linux);
      parseTest ("libfoo.dylib", "foo", -1, -1, "", SystemType.MacOS);
      parseTest ("foo.dll", "foo", -1, -1, "", SystemType.Windows);
      parseTest ("foo", "foo", -1, -1, "", SystemType.Generic);

      parseTest ("libfoo.so.1", "foo", 1, -1, ".1", SystemType.Linux);
      parseTest ("libfoo.so.1.3", "foo", 1, 3, ".1.3", SystemType.Linux);
      parseTest ("libfoo.so.1.3.066", "foo", 1, 3, ".1.3.066", SystemType.Linux);
      parseTest ("libfoo.3.dylib", "foo", 3, -1, ".3", SystemType.MacOS);
      parseTest ("libfoo.3.4.dylib", "foo", 3, 4, ".3.4", SystemType.MacOS);
      parseTest ("libfoo.3.4.1.dylib", "foo", 3, 4, ".3.4.1", SystemType.MacOS);
      parseTest ("foo.4.dll", "foo", 4, -1, ".4", SystemType.Windows);
      parseTest ("foo.4.7.dll", "foo", 4, 7, ".4.7", SystemType.Windows);
      parseTest ("foo.4.7.66.dll", "foo", 4, 7, ".4.7.66", SystemType.Windows);
      parseTest ("foo.5", "foo", 5, -1, ".5", SystemType.Generic);
      parseTest ("foo.5.6", "foo", 5, 6, ".5.6", SystemType.Generic);
      parseTest ("foo.5.6.088", "foo", 5, 6, ".5.6.088", SystemType.Generic);

      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.1.1", -1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.1.1.so", -1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.so.1.1", 1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.so.x1.1", -1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.so.1.1.12", 1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.so.2.1", -1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "libfoo.so.1", -1);
      matchMinorTest (SystemType.Linux32, "foo.1.2", "foo.so.1.1", -1);

      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.1.1", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.1.1.so", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.so.1.1", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.dylib.1.1", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.dylib.1", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.1.7.dylib", 7);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.1..7.dylib", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.2.7.dylib", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "foo.1.7.dylib", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.1.dylib", -1);
      matchMinorTest (SystemType.MacOS64, "foo.1.2", "libfoo.dylib", -1);

      matchMinorTest (SystemType.Windows32, "foo.1.2", "libfoo.1.1", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "libfoo.1.1.so", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "libfoo.so.1.1", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "libfoo.dll.1.1", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "libfoo.dll.1", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "foo.1.7.dll", 7);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "foo.1.x7.dll", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "foo.2.7.dll", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "foo.1.7.8.dll", 7);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "foo.1.dll", -1);
      matchMinorTest (SystemType.Windows32, "foo.1.2", "foo.dll", -1);
   }

   public static void main (String[] args) {
      NativeLibraryManagerTest tester = new NativeLibraryManagerTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");
   }

}