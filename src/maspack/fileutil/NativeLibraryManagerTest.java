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
      String name, String baseName, String vstr, SystemType type) {

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
      if (type != desc.mySys) {
         throw new TestException (
            "Expected system type "+type+", got "+desc.mySys);
      }
   }

   public void satisfyTest (String libName, String name, boolean expected) {
      LibDesc desc = myManager.createDesc (libName);
      LibDesc other = myManager.createDesc (name);
      if (desc.isSatisfiedBy (other) != expected) {
         throw new TestException (
            libName+" satisfied by "+name+": expected "+expected);
      }
   }

   /**
    * Creates the files {@code fileNames} in a temporary library directory,
    * and checks that the most recent library satisfying {@code libName} is
    * {@code expected} (or that there is none, if {@code expected} is {@code
    * null}). If there is one, also checks that getLibraryPath() with
    * satisfyFirst set returns it, or the exactly named file if present,
    * without attempting a download.
    */
   void findSatisfyingTest (
      SystemType type, String[] fileNames, String libName, String expected)
      throws IOException {
      File dir = java.nio.file.Files.createTempDirectory ("nlmtest").toFile();
      try {
         myManager.mySystemType = type;
         myManager.myLibDir = dir;
         myManager.myLinkNames.clear();
         for (String fileName : fileNames) {
            writeFile (new File (dir, fileName), "AAA");
         }
         LibDesc desc = myManager.createDesc (libName);
         File file = myManager.findSatisfying (desc, dir);
         String found = (file != null ? file.getName() : null);
         if (!Objects.equals (found, expected)) {
            throw new TestException (
               "findSatisfying("+libName+") in "+Arrays.toString(fileNames)+
               ": expected "+expected+", got "+found);
         }
         if (expected != null) {
            String path = myManager.getLibraryPath (desc, true);
            File exactFile = new File (dir, myManager.getFileName (desc));
            if (exactFile.exists()) {
               expected = exactFile.getName();
            }
            if (!new File (dir, expected).toString().equals (path)) {
               throw new TestException (
                  "getLibraryPath("+libName+"): expected "+expected+
                  ", got "+path);
            }
         }
      }
      finally {
         for (File file : dir.listFiles()) {
            file.delete();
         }
         dir.delete();
         myManager.myLibDir = null;
      }
   }

   void resolveTest (String libName, String expected) {
      String resolved =
         myManager.resolveVerified (myManager.createDesc (libName)).
         getFileName (SystemType.Linux64);
      if (!resolved.equals (expected)) {
         throw new TestException (
            "resolveVerified("+libName+"): expected "+expected+
            ", got "+resolved);
      }
   }

   public void test () {
      // check bad file names
      parseTest (".4", null, null, null);
      parseTest ("foo.so", null, null, null);
      parseTest ("foo.dylib", null, null, null);
      parseTest ("lib.so", null, null, null);
      parseTest ("lib.dylib", null, null, null);

      // check good file names
      parseTest ("libfoo.so", "foo", "", SystemType.Linux);
      parseTest ("libfoo.dylib", "foo", "", SystemType.MacOS);
      parseTest ("foo.dll", "foo", "", SystemType.Windows);
      parseTest ("foo", "foo", "", SystemType.Generic);

      parseTest ("libfoo.so.1", "foo", ".1", SystemType.Linux);
      parseTest ("libfoo.so.1.3", "foo", ".1.3", SystemType.Linux);
      parseTest ("libfoo.so.1.3.066", "foo", ".1.3.066", SystemType.Linux);
      parseTest ("libfoo.3.dylib", "foo", ".3", SystemType.MacOS);
      parseTest ("libfoo.3.4.dylib", "foo", ".3.4", SystemType.MacOS);
      parseTest ("libfoo.3.4.1.dylib", "foo", ".3.4.1", SystemType.MacOS);
      parseTest ("foo.4.dll", "foo", ".4", SystemType.Windows);
      parseTest ("foo.4.7.dll", "foo", ".4.7", SystemType.Windows);
      parseTest ("foo.4.7.66.dll", "foo", ".4.7.66", SystemType.Windows);
      parseTest ("foo.5", "foo", ".5", SystemType.Generic);
      parseTest ("foo.5.6", "foo", ".5.6", SystemType.Generic);
      parseTest ("foo.5.6.088", "foo", ".5.6.088", SystemType.Generic);

      satisfyTest ("foo.1.2", "foo.1.2", true);
      satisfyTest ("foo.1.2", "foo.1.2.3", true);
      satisfyTest ("foo.1.2", "foo.1.2.3.44", true);
      satisfyTest ("foo.1.2", "libfoo.so.1.2.3", true);
      satisfyTest ("foo.1.2", "libfoo.1.2.3.dylib", true);
      satisfyTest ("foo.1.2", "foo.1.2.3.dll", true);
      satisfyTest ("foo.1.2", "foo.1.23", false);
      satisfyTest ("foo.1.2", "foo.1.3", false);
      satisfyTest ("foo.1.2", "foo.1", false);
      satisfyTest ("foo.1.2", "foo", false);
      satisfyTest ("foo.1.2", "bar.1.2.3", false);
      satisfyTest ("foo.1.2", "foox.1.2.3", false);
      satisfyTest ("foo", "foo", true);
      satisfyTest ("foo", "libfoo.so", true);
      satisfyTest ("foo", "foo.1", false);

      checkValidLinkName ("libiomp5.so.2026.1", "libiomp5.so", true);
      checkValidLinkName ("libiomp5.so.2026.1", "libiomp5.dylib", false);
      checkValidLinkName ("libiomp5.so.2026.1", "lib/libiomp5.so", false);
      checkValidLinkName ("libiomp5.so.2026.1", "libiomp5*.so", false);
      checkValidLinkName ("libiomp5.so.2026.1", "iomp5", false);
      checkValidLinkName ("libiomp5md.2026.1.dll", "libiomp5md.dll", true);
      checkValidLinkName ("iomp5.2026.1", "libiomp5.dylib", true);
      checkValidLinkName ("libgfortran.so.5.0.0", "libgfortran.so.5", true);

      try {
         linkTest (SystemType.Linux64, "libbar.so.2026.1", "libbar.so");
         linkTest (SystemType.MacOSArm64, "libbar.2026.1.dylib", "libbar.dylib");
         linkTest (SystemType.Windows64, "bar.2026.1.dll", "bar.dll");
         linkTest (SystemType.Linux64, "libbar.so.5.0.0", "libbar.so.5");

         String[] linuxFiles = new String[] {
            "libfoo.so", "libfoo.so.1.2", "libfoo.so.1.2.3", "libfoo.so.1.2.10",
            "libfoo.so.1.23", "libfoo.so.1.3", "libfoo.1.2.4.dylib",
            "foo.1.2.5.dll", "foo.1.2.6" };
         findSatisfyingTest (
            SystemType.Linux64, linuxFiles, "foo.1.2", "libfoo.so.1.2.10");
         findSatisfyingTest (
            SystemType.Linux64, linuxFiles, "foo.1", "libfoo.so.1.23");
         findSatisfyingTest (
            SystemType.Linux64, linuxFiles, "foo.1.2.3", "libfoo.so.1.2.3");
         findSatisfyingTest (
            SystemType.Linux64, linuxFiles, "foo.1.4", null);
         findSatisfyingTest (
            SystemType.Linux64, linuxFiles, "foo", null);
         findSatisfyingTest (
            SystemType.MacOSArm64, linuxFiles, "foo.1.2", "libfoo.1.2.4.dylib");
         findSatisfyingTest (
            SystemType.Windows64, linuxFiles, "foo.1.2", "foo.1.2.5.dll");

         String[] winFiles = new String[] {
            "foo.dll", "foo.1.1.dll", "foo.1.2.dll", "foo.1.3.dll",
            "bar.dll", "bar.2.0.dll", "baz.dll" };
         String[] fooActive = new String[] { "foo.1.2", "bar.2.0" };
         // exact names remove unversioned and older files, but not the
         // active file
         obsoleteTest (
            SystemType.Windows64, winFiles,
            new String[] { "foo.dll", "foo.1.1.dll", "foo.1.2.dll" },
            fooActive, new String[] {},
            new String[] { "foo.dll", "foo.1.1.dll" });
         obsoleteTest (
            SystemType.Windows64, winFiles,
            new String[] { "foo", "foo.1.1", "foo.1.2" },
            fooActive, new String[] {},
            new String[] { "foo.dll", "foo.1.1.dll" });
         // wildcards don't remove any version of an active library
         obsoleteTest (
            SystemType.Windows64, winFiles,
            new String[] { "foo*.dll", "foo.*", "ba*.dll" },
            fooActive, new String[] {},
            new String[] { "baz.dll" });
         // links are kept, even if named exactly
         obsoleteTest (
            SystemType.Windows64, winFiles,
            new String[] { "bar.dll", "baz.dll" },
            fooActive, new String[] { "bar.dll" },
            new String[] { "baz.dll" });
         obsoleteTest (
            SystemType.Windows64, winFiles,
            new String[] { "bar.dll" },
            fooActive, new String[] {},
            new String[] { "bar.dll" });
         // names for other systems are ignored
         obsoleteTest (
            SystemType.Linux64,
            new String[] { "libfoo.so", "libfoo.so.1.2", "foo.dll" },
            new String[] { "libfoo.so", "foo.dll", "libfoo.so.1.2" },
            new String[] { "foo.1.2" }, new String[] {},
            new String[] { "libfoo.so" });
      }
      catch (IOException e) {
         throw new TestException ("Unexpected exception "+e);
      }

      myManager.myVerifiedLibs.put (
         "gdcmjni", myManager.createDesc ("libgdcmjni.so.3.0.22.1"));
      resolveTest ("gdcmjni.3.0.22", "libgdcmjni.so.3.0.22.1");
      resolveTest ("gdcmjni.3", "libgdcmjni.so.3.0.22.1");
      resolveTest ("gdcmjni.3.0.23", "libgdcmjni.so.3.0.23");
      resolveTest ("gdcmjni", "libgdcmjni.so");
      myManager.myVerifiedLibs.clear();
   }

   /**
    * Creates the files {@code fileNames} in a temporary library directory,
    * removes obsolete libraries, and checks that exactly the files {@code
    * removed} were removed.
    */
   void obsoleteTest (
      SystemType type, String[] fileNames, String[] obsolete, String[] active,
      String[] links, String[] removed) throws IOException {
      File dir = java.nio.file.Files.createTempDirectory ("nlmtest").toFile();
      try {
         myManager.mySystemType = type;
         myManager.myLibDir = dir;
         myManager.myLinkNames.clear();
         for (String fileName : fileNames) {
            writeFile (new File (dir, fileName), "AAA");
         }
         int num = myManager.removeObsoleteFiles (
            Arrays.asList (obsolete), Arrays.asList (active),
            Arrays.asList (links));
         HashSet<String> expected = new HashSet<>(Arrays.asList (removed));
         for (String fileName : fileNames) {
            boolean exists = new File (dir, fileName).exists();
            if (exists == expected.contains (fileName)) {
               throw new TestException (
                  "obsolete "+Arrays.toString(obsolete)+", active "+
                  Arrays.toString(active)+": "+fileName+
                  (exists ? " not removed" : " removed"));
            }
         }
         if (num != removed.length) {
            throw new TestException (
               "obsolete "+Arrays.toString(obsolete)+": removed "+num+
               " files, expected "+removed.length);
         }
      }
      finally {
         for (File file : dir.listFiles()) {
            file.delete();
         }
         dir.delete();
         myManager.myLibDir = null;
      }
   }

   void checkValidLinkName (String libName, String linkName, boolean valid) {
      if (NativeLibraryManager.isValidLinkName (libName, linkName) != valid) {
         throw new TestException (
            "isValidLinkName("+libName+","+linkName+"): expected "+valid);
      }
   }

   void writeFile (File file, String contents) throws IOException {
      try (FileWriter fw = new FileWriter (file)) {
         fw.write (contents);
      }
   }

   String readFile (File file) throws IOException {
      return new String (java.nio.file.Files.readAllBytes (file.toPath()));
   }

   void checkLink (File linkFile, File libFile, boolean useCopy)
      throws IOException {
      java.nio.file.Path linkPath = linkFile.toPath();
      if (useCopy) {
         if (java.nio.file.Files.isSymbolicLink (linkPath)) {
            throw new TestException (linkFile+" is a link, expected a copy");
         }
         if (!readFile(linkFile).equals (readFile(libFile))) {
            throw new TestException (linkFile+" differs from "+libFile);
         }
      }
      else {
         if (!java.nio.file.Files.isSymbolicLink (linkPath) ||
             !java.nio.file.Files.readSymbolicLink (linkPath).toString().equals (
                libFile.getName())) {
            throw new TestException (
               linkFile+" is not a link to "+libFile.getName());
         }
      }
   }

   /**
    * Returns true if symbolic links can be created in directory dir.
    */
   boolean canCreateLinks (File dir) {
      java.nio.file.Path link = new File (dir, "linkcheck").toPath();
      try {
         java.nio.file.Files.createSymbolicLink (
            link, java.nio.file.Paths.get ("linktarget"));
         java.nio.file.Files.delete (link);
         return true;
      }
      catch (IOException | UnsupportedOperationException e) {
         return false;
      }
   }

   void checkUpdate (LibDesc desc, String linkName, boolean expected) {
      if (myManager.updateLink (desc, linkName) != expected) {
         throw new TestException (
            "updateLink("+linkName+"): expected "+expected);
      }
   }

   /**
    * Tests creating and updating links (or copies on Windows), and that links
    * are not removed as older versions.
    */
   public void linkTest (SystemType type, String fileName, String linkName)
      throws IOException {
      File dir = java.nio.file.Files.createTempDirectory ("nlmtest").toFile();
      try {
         myManager.mySystemType = type;
         myManager.myLibDir = dir;
         myManager.myLinkNames.clear();
         myManager.myLinkNames.add (linkName);
         // copies are also used where the host can't create symbolic links
         // (e.g., Windows without admin rights or developer mode)
         boolean useCopy =
            type.isInstanceOf (SystemType.Windows) || !canCreateLinks (dir);
         LibDesc desc = myManager.createDesc (fileName);
         File libFile = new File (dir, fileName);
         File linkFile = new File (dir, linkName);
         writeFile (libFile, "AAA");

         checkUpdate (desc, linkName, true); // new link
         checkLink (linkFile, libFile, useCopy);
         checkUpdate (desc, linkName, false); // already up to date

         if (useCopy) {
            // copy is replaced when the library changes
            writeFile (libFile, "BBB");
            checkUpdate (desc, linkName, true);
            checkLink (linkFile, libFile, useCopy);
         }
         else {
            // an identical regular file is replaced by a link
            linkFile.delete();
            writeFile (linkFile, "AAA");
            checkUpdate (desc, linkName, true);
            checkLink (linkFile, libFile, useCopy);
            // a link to another file is repointed
            linkFile.delete();
            java.nio.file.Files.createSymbolicLink (
               linkFile.toPath(), java.nio.file.Paths.get ("other"));
            checkUpdate (desc, linkName, true);
            checkLink (linkFile, libFile, useCopy);
         }
         // links are not removed as older versions, even if they look like
         // one (e.g., libbar.so.5 for libbar.so.5.0.0)
         myManager.removeOlderVersions (desc);
         if (!java.nio.file.Files.exists (
                linkFile.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new TestException (
               linkFile.getName()+" removed as an older version");
         }
      }
      finally {
         for (File file : dir.listFiles()) {
            file.delete();
         }
         dir.delete();
         myManager.myLibDir = null;
      }
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