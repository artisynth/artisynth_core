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
      setRemoteSource ("http://www.artisynth.org/files/lib/");
   }
   
   private static void printUsage () {
      System.out.println ("Usage:\n");
      System.out.println (
         "   java artisynth.core.driver.LibraryInstaller [options]\n");
      System.out.println ("options:\n");
      System.out.println (
"-updateLibs            try to update libraries to latest versions\n"+
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
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-updateLibs")) {
            updateLibs = true;
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

   public boolean verifyNativeLibs (boolean updateLibs) throws Exception {
      boolean allOK = true;
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
               NativeLibraryManager.verify (libname);
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
      return allOK;
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

   public boolean verifyJars (boolean updateLibs) throws Exception {
      boolean allOK = true;
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
      return allOK;
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
}

