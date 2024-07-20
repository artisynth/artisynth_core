/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ArtisynthPath;
import maspack.fileutil.DefaultConsoleFileTransferListener;
import maspack.fileutil.FileManager;
import maspack.fileutil.FileTransferListener;
import maspack.fileutil.NativeLibraryManager;
import maspack.fileutil.NativeLibraryManager.SystemType;
import maspack.fileutil.uri.URIx;
import maspack.fileutil.uri.URIxSyntaxException;
import maspack.util.Logger.LogLevel;
import maspack.util.ReaderTokenizer;
 
/**
 * Class to installer Artisynth libraries (both jar files and native binaries).
 * It is intended to be callable as both as a standalone application and from
 * within ArtiSynth. By default, it finds the libraries to load in the file
 * $ARTISYNTH_HOME/lib/LIBRARIES.
 */
public class LibraryInstaller {

   private static String myRemoteSource = null;

   private static String SEP = File.separator;
   private static String PSEP = File.pathSeparator;

   protected LinkedList<String> myJarnames = new LinkedList<String>();
   // special jars are ones we don't update but also don't remove
   protected LinkedList<String> mySpecialJarnames = new LinkedList<String>();
   protected LinkedList<String> myLibnames = new LinkedList<String>();
   protected File myLibDir;

   public String getRemoteSource () {
      return myRemoteSource;
   }

   public void setRemoteSource (String source) {
      try {
         URIx uri = new URIx (source);
      }
      catch (URIxSyntaxException e) {
         throw new IllegalArgumentException ("Malformed URI: " + source);
      }
      myRemoteSource = source;
   }

   public void addLibrary (String libName) {
      if (libName.endsWith (".jar")) {
         myJarnames.add (libName);
      }
      else {
         myLibnames.add (libName);
      }
   }

   public void clearJars() {
      myJarnames.clear();
   }

   public void clearNativeLibs() {
      myLibnames.clear();
   }

   public void setLibDir (String dirPath) {
      myLibDir = new File (dirPath);
      if (!myLibDir.isDirectory()) {
         throw new IllegalArgumentException (
            myLibDir+" does not exist or is not a directory");
      }
   }

   public File getLibDir () {
      return myLibDir;
   }

   public LibraryInstaller () {
      myLibDir = ArtisynthPath.getHomeRelativeFile ("lib", ".");
      if (!myLibDir.isDirectory()) {
         throw new IllegalArgumentException (
            myLibDir+" does not exist or is not a directory");
      }
      setRemoteSource ("https://www.artisynth.org/files/lib/");
      // ObjectSizeAgent.jar, if needed, is built internally
      mySpecialJarnames.add ("ObjectSizeAgent.jar");
   }
   
   private static void printUsage () {
      System.out.println ("Usage:\n");
      System.out.println (
         "   java artisynth.core.driver.LibraryInstaller [options]\n");
      System.out.println ("options:\n");
      System.out.println (
"-updateLibs            try to update libraries to latest versions\n"+
"-moveUnused            move unused jar files 'xxx.jar' to 'xxx.jar.save'\n"+
"-file <fileName>       use fileName instead of $ARTISYNTH_HOME/lib/LIBRARIES;\n"+
"                       (with - indicating <stdin> and NONE no file)\n"+
"-lib <libName>         explicitly specify the name of a library\n" + 
"-remoteSource <url>    set the url used for obtaining the remote files" + 
"-ahome <dir>           set alternative ARTISYNTH_HOME directory" +
"-systemType <type>     explcitly set the system type to Linux, Windows, etc.");
      System.out.println ("");
   }

   public static void main (String[] args) {

      LibraryInstaller installer = new LibraryInstaller();
      String libFileName = null;

      boolean updateLibs = false;
      boolean moveUnused = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-updateLibs")) {
            updateLibs = true;
         }
         else if (args[i].equals ("-moveUnused")) {
            moveUnused = true;
         }
         else if (args[i].equals ("-help")) {
            printUsage();
            System.exit(0);
         }
         else if (args[i].equals ("-file")) {
            if (++i == args.length) {
               System.out.println (
                  "Option -file requires an additional argument");
               System.exit(1);
            }
            else if (args[i].equalsIgnoreCase ("NONE")) {
               libFileName = "NONE";
            }
            else if (args[i].equals ("-")) {
               libFileName = "-";
            }
            else {
               libFileName = args[i];
            }
         }
         else if (args[i].equals ("-lib")) {
            if (++i == args.length) {
               System.out.println (
                  "Option -file requires an additional argument");
               System.exit(1);
            }
            installer.addLibrary (args[i]);
         }
         else if (args[i].equals ("-ahome")) {
            if (++i == args.length) {
               System.out.println (
                  "Option -ahome requires an additional argument");
               System.exit(1);
            }
            installer.setLibDir (args[i]+SEP+"lib");
         }
         else if (args[i].equals ("-remoteSource")) {
            if (++i == args.length) {
               System.out.println (
                  "Option -remoteSource requires an additional argument");
               System.exit(1);
            }
            installer.setRemoteSource (args[i]);
         }
         else if (args[i].equals ("-systemType")) {
            if (++i == args.length) {
               System.out.println (
                  "Option -systemType requires an additional argument");
               System.exit(1);
            }
            SystemType sysType = NativeLibraryManager.getSystemType(args[i]);
            if (sysType == null || sysType == SystemType.Unknown) {
               System.out.println ("Unknown systemType: '"+args[i]+"'");
               System.exit(1); 
            }
            if (sysType.getSubTypes().length > 0) {
               System.out.println (
                  "Generic systemType '"+args[i]+"' not permitted");
               System.exit(1); 
            }
            NativeLibraryManager.setSystemType (sysType);
         }
         else {
            System.out.println ("Unrecognized option "+ args[i]);
            printUsage();
            System.exit(1); 
         }
      }
      try {
         if (libFileName == null || !libFileName.equals ("NONE")) {
            File file;
            if (libFileName == null) {
               file = new File (installer.getLibDir(), "LIBRARIES");
            }
            else {
               file = new File (libFileName);
            }
            if (!file.canRead()) {
               System.out.println (
                  "Warning: can't find or access file "+file+", ignoring");
            }
            else {
               installer.readLibs (file);
            }
         }
         else if (libFileName.equals ("-")) {
            installer.readLibs (null); // null indicates use standard input
         }
         if (moveUnused) {
            // move unused jars before anything else, since subsequent
            // operations seem to lead to jar files being openned/locked which
            // on Windows then makes them unmoveable.
            installer.moveUnusedJars ();
         }
         installer.verifyJars (updateLibs);
         installer.verifyNativeLibs (updateLibs);
      }
      catch (Exception e) {
         if (installer.isConnectionException(e)) {
            System.out.println (e.getMessage());
         }
         else {
            e.printStackTrace();
         }
         System.exit(1); 
      }
   }
   
   /**
    * Verify that all native libraries are present and, if requested, updated.
    * 
    * @param updateLibs if {@code true}, verifies that each library
    * is the most recent, and downloads it if it isn't.
    * @return number of libraries actually downloaded, or -1 if
    * an error was encountered.
    */
   public int verifyNativeLibs (boolean updateLibs) throws Exception {
      boolean allOK = true;
      int numDownloads = 0;
      NativeLibraryManager.setLibDir (myLibDir.toString());
      if (myLibnames.size() > 0) {
         int oldFlags = NativeLibraryManager.getFlags();
         int newFlags = NativeLibraryManager.VERBOSE;
         if (updateLibs) {
            newFlags |= NativeLibraryManager.UPDATE;
         }
         NativeLibraryManager.setFlags (newFlags);         
         for (String libname : myLibnames) {
            try {
               if (NativeLibraryManager.verify (libname) == 1) {
                  numDownloads++;
               }
            }
            catch (Exception e) {
               if (isConnectionException (e)) {
                  throw e;
               }
               System.out.println ("Error installing or updating "+libname+":");
               System.out.println (e.getMessage());
               allOK = false;
            }
         }
         NativeLibraryManager.setFlags (oldFlags);
      }
      return allOK ? numDownloads : -1;
   }

   public boolean isConnectionException (Exception e) {
      if (e.getMessage() != null &&
          e.getMessage().contains ("internet connection")) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Verify that all jar files are present and, if requested, updated.
    * 
    * @param updateLibs if {@code true}, verifies that each jar file
    * is the most recent, and downloads it if it isn't.
    * @return number of jar files actually downloaded, or -1 if
    * an error was encountered.
    */
   public int verifyJars (boolean updateLibs) throws Exception {
      boolean allOK = true;
      int numDownloads = 0;
      if (myJarnames.size() > 0) {
         FileManager grabber = new FileManager();
         grabber.setVerbosityLevel (LogLevel.ALL);
         FileTransferListener listener =
            new DefaultConsoleFileTransferListener();
         grabber.addTransferListener(listener);
         grabber.getTransferMonitor().setPollSleep(100);  // 100ms
         grabber.setDownloadDir (myLibDir);
         grabber.setRemoteSource (myRemoteSource);
         int options = (updateLibs ? FileManager.CHECK_HASH : 0);
         for (String jarname : myJarnames) {
            File jfile = new File (myLibDir, jarname);
            if (!jfile.exists()) {
               try {
                  grabber.getRemote (jarname);
                  numDownloads++;
                  System.out.println ("Downloaded jar file "+jfile);
               }
               catch (Exception e) {
                  if (isConnectionException (e)) {
                     throw e;
                  }
                  System.out.println ("Failed to download jar file "+jfile+":");
                  System.out.println (e.getMessage());
                  allOK = false;
               }
            }
            else if (updateLibs) {
               try {
                  if (!grabber.equalsHash (jarname)) {
                     grabber.getRemote (jarname);
                     numDownloads++;
                  }
                  System.out.println ("Updated jar file "+jfile);
               }
               catch (Exception e) {
                  if (isConnectionException (e)) {
                     throw e;
                  }
                  System.out.println ("Failed to update jar file "+jfile+":");
                  System.out.println (e.getMessage());
               }
            }
         }
      }
      return allOK ? numDownloads : -1;
   }

   public int moveUnusedJars() {
      int unused = 0;
      for (String fname : myLibDir.list()) {
         if (fname.endsWith (".jar") &&
             !myJarnames.contains(fname) &&
             !mySpecialJarnames.contains(fname)) {
            String tname;
            File target;
            int cnt = 1;
            boolean targetEqualsSource;
            File source = new File (myLibDir, fname);
            do {
               String suffix = ".save";
               if (cnt > 1) {
                  suffix += "." + cnt;
               }
               tname = fname + suffix;
               target = new File (myLibDir, tname);
               if (target.exists()) {
                  targetEqualsSource = fileContentsEqual (source,target);
               }
               else {
                  targetEqualsSource = false;
               }
               cnt++;
            }
            while (target.exists() && !targetEqualsSource);
            try {
               if (targetEqualsSource) {
                  Files.delete (source.toPath());
                  System.out.println (
                     "Notice: unused jar file 'lib/"+fname+
                     "' deleted; a copy exists in 'lib/"+tname+"'");
               }
               else {
                  Files.move (source.toPath(), target.toPath());
                  System.out.println (
                     "Notice: unused jar file 'lib/"+fname+
                     "' being moved to 'lib/"+tname+"'");
               }
            }
            catch (Exception e) {
               System.out.println (
                  "WARNING: jar file 'lib/"+fname+
                  "' is unused and cannot be moved or deleted:\n" + e);
               System.out.println (
                  "The file 'lib/"+fname+"' should be removed manually");
            }
            unused++;               
         }
      }
      return unused;
   }



   protected void maybeAddLibrary (String libName, SystemType sysType) {

      if (sysType != null) {
         if (!NativeLibraryManager.getSystemType().isInstanceOf (sysType)) {
            return;
         }
      }
      if (!libName.endsWith (".jar")) {
         if (NativeLibraryManager.libraryMatchesSystem (libName)) {
            addLibrary (libName);
         }
         else if (sysType != null) {
            System.out.println (
               "Warning: library "+libName+
               " does not match system "+sysType+", ignoring");
         }
      }
      else {
         addLibrary (libName);         
      }
   }

   protected void readLine (ReaderTokenizer rtok) throws IOException {
      String word1 = null;
      String word2 = null;

      rtok.nextToken();
      while (rtok.ttype != ReaderTokenizer.TT_EOF && 
             rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (word1 == null && rtok.tokenIsWord()) {
            word1 = rtok.sval;
         }
         else if (word2 == null && rtok.tokenIsWord()) {
            word2 = rtok.sval;
         }
         else {
            throw new IOException ("Unexpected token "+rtok);
         }
         rtok.nextToken();
      }
      if (word2 != null) {
         SystemType sysType = NativeLibraryManager.getSystemType (word1);
         if (sysType == SystemType.Unknown || sysType == null) {
            throw new IOException (
                "Illegal system specifier '"+word1+"', line "+rtok.lineno());
         }
         maybeAddLibrary (word2, /*system=*/sysType);
      }
      else if (word1 != null) {
         maybeAddLibrary (word1, /*system=*/null);
      }
   }

   public void readLibs (File file) throws IOException {
      ReaderTokenizer rtok = null;
      if (file == null) {
         // open from standard input
         rtok = new ReaderTokenizer (new InputStreamReader (System.in));
      }
      else {
         rtok = ArtisynthIO.newReaderTokenizer (file);
      }
      
      try {
         rtok.wordChars ("-");
         rtok.eolIsSignificant (true);
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            readLine (rtok);
         }
      }
      catch (Exception e) {
         if (file == null) {
            throw new IOException (
               "Error reading input: " + e.getMessage(), e);
         }
         else {
            throw new IOException (
               "Error reading file"+file+": "+e.getMessage(), e);
         }
      }
      finally {
         if (rtok != null) {
            rtok.close();
         }
      }
   }      

   private void closeQuietly (InputStream is) {
      try {
         if (is != null) {
            is.close();
         }
      }
      catch (IOException e) {
         // ignore
      }
   }

   /**
    * Checks to see if two files are identical.
    */
   public boolean fileContentsEqual (File file0, File file1) {
      String fname0 = file0.getName();
      String fname1 = file1.getName();
      if (!file0.canRead()) {
         System.out.println (
            "Warning: file '"+fname0+"' in library folder is unreadable");
         return false;
      }
      if (!file1.canRead()) {
         System.out.println (
            "Warning: file '"+fname1+"' in library folder is unreadable");
         return false;
      }
      Path path0 = file0.toPath();
      Path path1 = file1.toPath();
      BufferedInputStream is0 = null;
      BufferedInputStream is1 = null;
      try {
         long size = Files.size(path0);
         if (size != Files.size(path1)) {
            return false;
         }
         if (size < 5000000) {
            return Arrays.equals (
               Files.readAllBytes(path0), Files.readAllBytes(path1));
         }
         is0 = new BufferedInputStream (Files.newInputStream(path0));
         is1 = new BufferedInputStream (Files.newInputStream(path1));
         int data;
         while ((data = is0.read()) != -1) {
            if (data != is1.read()) {
               return false;
            }
         }
      }
      catch (IOException e) {
         System.out.println (
            "Warning: error comparing files '"+fname1+
            "' and '"+fname1+"' in library folder: "+e.getMessage());
         return false;
      }
      finally {
         closeQuietly (is0);
         closeQuietly (is1);
      }
      return true;
   }
}

