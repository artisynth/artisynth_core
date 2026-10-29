/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * lib/LIBRARIES, where lib/ is the folder next to the one containing this
 * class (i.e., next to bin/ when run from bin/libraryInstaller.jar, or next to
 * classes/ when run from ArtiSynth's compiled classes).
 *
 * <p>Libraries that are no longer needed can be listed in the file
 * lib/OBSOLETE, so that they are removed along with older versions of the
 * current libraries (see {@link #removeOldJars} and {@link
 * #removeOldNativeLibs}).
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

   // Names of all jar files in the library list, for any system, and of the
   // native libraries for the current system. Unlike myJarnames and
   // myLibnames, these are not cleared, and are used to keep active
   // libraries from being removed by entries in the obsolete list.
   protected HashSet<String> myAllJarnames = new HashSet<String>();
   protected HashSet<String> myActiveLibnames = new HashSet<String>();
   // link names for native libraries that have them (see addLibrary)
   protected HashMap<String,String> myLinkNames = new HashMap<String,String>();
   // true if a library list has been read, without which obsolete libraries
   // are not removed, since the active libraries are unknown
   protected boolean myLibsRead = false;
   // jar files in the library list that have no system specifier, in the
   // order listed. These form the classpath (see bin/makeClasspath).
   protected LinkedList<String> myClasspathJarnames = new LinkedList<String>();

   // obsolete jar files and native libraries for the current system
   protected LinkedList<String> myObsoleteJarnames = new LinkedList<String>();
   protected LinkedList<String> myObsoleteLibnames = new LinkedList<String>();
   protected boolean myObsoleteRead = false;

   public String getRemoteSource () {
      return myRemoteSource;
   }

   public static void setRemoteSource (String source) {
      if (source != null) {
         try {
            URIx uri = new URIx (source);
         }
         catch (URIxSyntaxException e) {
            throw new IllegalArgumentException ("Malformed URI: " + source);
         }
      }
      myRemoteSource = source;
   }

   public void addLibrary (String libName) {
      if (libName.endsWith (".jar")) {
         myJarnames.add (libName);
         myAllJarnames.add (libName);
      }
      else {
         myLibnames.add (libName);
         myActiveLibnames.add (libName);
      }
   }

   /**
    * Adds a native library that is to be accessible through a link with
    * the name {@code linkName}, such as {@code libiomp5.so} for {@code
    * libiomp5.so.2026.1}. See {@link
    * NativeLibraryManager#verify(String,String)}.
    */
   public void addLibrary (String libName, String linkName) {
      addLibrary (libName);
      if (linkName != null) {
         myLinkNames.put (libName, linkName);
      }
   }

   /**
    * Returns the names of the jar files in the library list. For lists read
    * using {@link #readLibs}, this includes only jar files required for the
    * current system.
    */
   public List<String> getJarnames() {
      return Collections.unmodifiableList (myJarnames);
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

   /**
    * Infers the default library folder from the location of this class: the
    * folder lib/ next to the folder containing it (bin/ for
    * bin/libraryInstaller.jar, or classes/ for ArtiSynth's compiled
    * classes). Returns lib/ in the current folder if the location cannot be
    * determined.
    */
   private static File inferLibDir() {
      try {
         CodeSource src =
            LibraryInstaller.class.getProtectionDomain().getCodeSource();
         if (src != null && src.getLocation() != null) {
            File loc = new File (src.getLocation().toURI());
            File dir = loc.isFile() ? loc.getParentFile() : loc;
            if (dir != null && dir.getAbsoluteFile().getParentFile() != null) {
               return new File (dir.getAbsoluteFile().getParentFile(), "lib");
            }
         }
      }
      catch (Exception e) {
         // fall through to default
      }
      return new File ("lib");
   }

   /**
    * Creates a new LibraryInstaller. The library folder is inferred from the
    * location of this class (see {@link #inferLibDir}), and can be reset
    * using {@link #setLibDir}. The folder is not checked here, so that an
    * alternate folder can still be set if the inferred one does not exist.
    */
   public LibraryInstaller () {
      myLibDir = inferLibDir();
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
"-updateLibs             try to update libraries to latest versions\n"+
"-file <fileName>        use fileName instead of the default LIBRARIES file\n"+
"                        (with - indicating <stdin> and NONE no file)\n"+
"-lib <libName>          explicitly specify the name of a library\n" + 
"-remoteSource <url>     set the url used for obtaining the remote files\n" + 
"-ahome <dir>            use <dir>/lib as the library folder\n" +
"-systemType <type>      explcitly set the system type to Linux, Windows, etc.\n"+
"-syncEclipseClasses     update the jar file entries in Eclipse's .classpath\n"+
"                        file (in the parent of the library folder) to match\n"+
"                        the library list\n"+
"-syncEclipseClassesOnly update .classpath as for -syncEclipseClasses, without\n"+
"                        verifying, downloading or removing any libraries");
      System.out.println ("");
   }

   public static void main (String[] args) {

      LibraryInstaller installer = new LibraryInstaller();
      String libFileName = null;

      boolean updateLibs = false;
      boolean syncEclipseClasses = false;
      boolean syncEclipseClassesOnly = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-updateLibs")) {
            updateLibs = true;
         }
         else if (args[i].equals ("-syncEclipseClasses")) {
            syncEclipseClasses = true;
         }
         else if (args[i].equals ("-syncEclipseClassesOnly")) {
            syncEclipseClasses = true;
            syncEclipseClassesOnly = true;
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
      if (!installer.getLibDir().isDirectory()) {
         System.out.println (
            "Error: library folder "+installer.getLibDir()+
            " does not exist or is not a directory");
         System.exit(1);
      }
      try {
         if ("-".equals (libFileName)) {
            installer.readLibs (null); // null indicates use standard input
         }
         else if (!"NONE".equals (libFileName)) {
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
         boolean allOK = true;
         if (syncEclipseClasses) {
            // done first since it depends only on the library list
            File cpfile = new File (
               installer.getLibDir().getAbsoluteFile().getParentFile(),
               ".classpath");
            try {
               if (installer.updateEclipseClasspath (cpfile)) {
                  System.out.println (
                     "Notice: updated "+cpfile.getName()+
                     " from the library list");
               }
            }
            catch (IOException e) {
               System.out.println ("Warning: "+e.getMessage());
               allOK = false;
            }
            if (syncEclipseClassesOnly) {
               System.exit (allOK ? 0 : 1);
            }
         }
         if (installer.verifyJars (updateLibs) == -1) {
            allOK = false;
         }
         if (installer.verifyNativeLibs (updateLibs) == -1) {
            allOK = false;
         }
         // remove old versions after downloading, since removal requires
         // the current version to be present
         installer.removeOldJars();
         installer.removeOldNativeLibs();
         if (!allOK) {
            // non-zero exit lets callers (e.g., make) know to try again
            System.exit(1);
         }
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
    * Returns a short name for a file in the library folder, for use in
    * messages, consisting of the file name preceded by the name of the
    * library folder (e.g., "lib/jsoup-1.11.2.jar"). Full paths are long
    * enough that they tend to scroll console windows to the right, hiding
    * the start of the lines.
    */
   private String displayName (File file) {
      return myLibDir.getName()+"/"+file.getName();
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
               String linkName = myLinkNames.get (libname);
               int status = (linkName != null ?
                  NativeLibraryManager.verify (libname, linkName) :
                  NativeLibraryManager.verify (libname));
               if (status == 1) {
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
         // INFO omits FileManager's debug messages, such as "Downloading
         // file <url> to <path>", which duplicate the listener's messages
         grabber.setVerbosityLevel (LogLevel.INFO);
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
                  System.out.println (
                     "Downloaded jar file "+displayName(jfile));
               }
               catch (Exception e) {
                  if (isConnectionException (e)) {
                     throw e;
                  }
                  System.out.println (
                     "Failed to download jar file "+displayName(jfile)+":");
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
                  System.out.println (
                     "Updated jar file "+displayName(jfile));
               }
               catch (Exception e) {
                  if (isConnectionException (e)) {
                     throw e;
                  }
                  System.out.println (
                     "Failed to update jar file "+displayName(jfile)+":");
                  System.out.println (e.getMessage());
               }
            }
         }
      }
      return allOK ? numDownloads : -1;
   }

   /**
    * Returns the stem of a jar file name, which is the name with its
    * ".jar" suffix and any version information removed. The name is
    * divided into fields separated by '-', and the version information is
    * the first run of fields (after the first field) that consist solely of
    * digits and '.'. For example, the stems of
    * <pre>
    *   jogl-all-2.6.0.jar
    *   jogl-all-2.6.0-natives-macosx-universal.jar
    *   jython.jar
    * </pre>
    * are {@code jogl-all}, {@code jogl-all-natives-macosx-universal}, and
    * {@code jython}.
    */
   static String getJarStem (String jarname) {
      String name = jarname;
      if (name.endsWith (".jar")) {
         name = name.substring (0, name.length()-4);
      }
      String[] fields = name.split ("-", -1);
      int i0 = 1;
      while (i0 < fields.length && !fields[i0].matches ("[0-9][0-9.]*")) {
         i0++;
      }
      if (i0 == fields.length) {
         return name; // no version information
      }
      int i1 = i0+1;
      while (i1 < fields.length && fields[i1].matches ("[0-9][0-9.]*")) {
         i1++;
      }
      StringBuilder stem = new StringBuilder();
      for (int i=0; i<fields.length; i++) {
         if (i < i0 || i >= i1) {
            if (stem.length() > 0) {
               stem.append ('-');
            }
            stem.append (fields[i]);
         }
      }
      return stem.toString();
   }

   /**
    * Removes older versions of the jar files in the library list. A jar file
    * in the library folder is removed if it is not in the library list, but
    * has the same stem (see {@link #getJarStem}) as a jar file that is in the
    * list and is present in the folder. For example, if the list contains
    * {@code jogl-all-2.6.0.jar}, then {@code jogl-all-2.5.0.jar} would be
    * removed, but only if {@code jogl-all-2.6.0.jar} is present. This should
    * therefore be called after {@link #verifyJars}, so that an older version
    * can still be used if a download fails.
    *
    * <p>Jar files whose stems match nothing in the list are left alone, as
    * are files that cannot be deleted (which can happen on Windows if they
    * are in use by another process, such as a running instance of
    * ArtiSynth). A warning is printed for the latter.
    *
    * <p>Jar files listed in the obsolete list (see {@link #readObsolete})
    * are also removed, unless their stems match the stem of a jar file in
    * the library list (for any system), or they are special jar files.
    *
    * @return number of jar files actually removed
    */
   public int removeOldJars() {
      HashMap<String,String> currentJars = new HashMap<>();
      for (String jarname : myJarnames) {
         if (new File (myLibDir, jarname).exists()) {
            currentJars.put (getJarStem (jarname), jarname);
         }
      }
      int numRemoved = 0;
      String[] fnames = myLibDir.list();
      if (fnames == null) {
         return 0;
      }
      for (String fname : fnames) {
         if (!fname.endsWith (".jar") ||
             myJarnames.contains (fname) ||
             mySpecialJarnames.contains (fname)) {
            continue;
         }
         String replacement = currentJars.get (getJarStem (fname));
         if (replacement != null) {
            try {
               Files.delete (new File (myLibDir, fname).toPath());
               System.out.println (
                  "Notice: removed old jar file 'lib/"+fname+
                  "', replaced by 'lib/"+replacement+"'");
               numRemoved++;
            }
            catch (Exception e) {
               System.out.println (
                  "Warning: can't remove old jar file 'lib/"+fname+
                  "', replaced by 'lib/"+replacement+"':\n" + e);
               System.out.println (
                  "It may be in use by another program; "+
                  "if this persists, remove it manually");
            }
         }
      }
      numRemoved += removeObsoleteJars();
      return numRemoved;
   }

   /**
    * Removes the jar files in the obsolete list, except for those whose
    * stems match the stem of a jar file in the library list (for any
    * system), and special jar files.
    */
   private int removeObsoleteJars() {
      maybeReadObsolete();
      if (!myLibsRead || myObsoleteJarnames.isEmpty()) {
         return 0;
      }
      HashSet<String> activeStems = new HashSet<>();
      for (String jarname : myAllJarnames) {
         activeStems.add (getJarStem (jarname));
      }
      File[] files = myLibDir.listFiles();
      if (files == null) {
         return 0;
      }
      int numRemoved = 0;
      for (File file : files) {
         String fname = file.getName();
         if (file.isDirectory() ||
             !fname.endsWith (".jar") ||
             mySpecialJarnames.contains (fname) ||
             activeStems.contains (getJarStem (fname))) {
            continue;
         }
         for (String name : myObsoleteJarnames) {
            if (NativeLibraryManager.wildcardMatches (name, fname)) {
               try {
                  Files.delete (file.toPath());
                  System.out.println (
                     "Notice: removed obsolete jar file 'lib/"+fname+"'");
                  numRemoved++;
               }
               catch (Exception e) {
                  System.out.println (
                     "Warning: can't remove obsolete jar file 'lib/"+fname+
                     "':\n" + e);
                  System.out.println (
                     "It may be in use by another program; "+
                     "if this persists, remove it manually");
               }
               break;
            }
         }
      }
      return numRemoved;
   }

   /**
    * Removes older versions of the native libraries in the library list.
    * See {@link NativeLibraryManager#removeOlderVersions} for details. This
    * should be called after {@link #verifyNativeLibs}.
    *
    * <p>Native libraries listed in the obsolete list (see {@link
    * #readObsolete}) are also removed, unless they are libraries in the
    * library list or their links. An exactly named obsolete library is
    * removed even if another version of it is in the library list. See {@link
    * NativeLibraryManager#removeObsoleteLibraries} for details.
    *
    * @return number of library files actually removed
    */
   public int removeOldNativeLibs() {
      int numRemoved = 0;
      maybeReadObsolete();
      boolean removeObsolete = (myLibsRead && !myObsoleteLibnames.isEmpty());
      if (myLibnames.size() > 0 || removeObsolete) {
         NativeLibraryManager.setLibDir (myLibDir.toString());
      }
      for (String libname : myLibnames) {
         numRemoved += NativeLibraryManager.removeOlderVersions (libname);
      }
      if (removeObsolete) {
         numRemoved += NativeLibraryManager.removeObsoleteLibraries (
            myObsoleteLibnames, myActiveLibnames, myLinkNames.values());
      }
      return numRemoved;
   }



   protected void maybeAddLibrary (
      String libName, SystemType sysType, String linkName) {

      if (sysType != null) {
         if (!NativeLibraryManager.getSystemType().isInstanceOf (sysType)) {
            return;
         }
      }
      if (!libName.endsWith (".jar")) {
         if (NativeLibraryManager.libraryMatchesSystem (libName)) {
            addLibrary (libName, linkName);
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

   /**
    * Returns true if {@code name} is a valid name for an obsolete jar file:
    * it must end in ".jar", contain no path separators, and any '*'
    * wildcard must be preceded by at least one other character.
    */
   protected boolean isValidObsoleteJarname (String name) {
      return (name.endsWith (".jar") &&
              !name.contains ("/") && !name.contains ("\\") &&
              name.indexOf ('*') != 0);
   }

   protected void addObsolete (String name, int lineno) throws IOException {
      if (name.endsWith (".jar")) {
         if (!isValidObsoleteJarname (name)) {
            throw new IOException (
               "Invalid obsolete jar file name '"+name+"', line "+lineno);
         }
         myObsoleteJarnames.add (name);
      }
      else {
         if (!NativeLibraryManager.isValidObsoleteName (name)) {
            throw new IOException (
               "Invalid obsolete library name '"+name+"', line "+lineno);
         }
         myObsoleteLibnames.add (name);
      }
   }

   protected void readLine (ReaderTokenizer rtok, boolean obsolete)
      throws IOException {
      String word1 = null;
      String word2 = null;
      String linkName = null;

      rtok.nextToken();
      int lineno = rtok.lineno();
      while (rtok.ttype != ReaderTokenizer.TT_EOF &&
             rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (rtok.lineno() != lineno) {
            // a comment consumes the end of its line, so a token on a later
            // line belongs to the next line
            rtok.pushBack();
            break;
         }
         if (linkName != null) {
            throw new IOException ("Unexpected token "+rtok);
         }
         if (word1 != null && rtok.tokenIsWord() &&
             rtok.sval.equals ("link")) {
            // link attribute, of the form link=<name>
            rtok.nextToken();
            if (rtok.ttype != '=' || rtok.lineno() != lineno) {
               throw new IOException (
                  "Expected '=' after 'link', line "+lineno);
            }
            rtok.nextToken();
            if (!rtok.tokenIsWord() || rtok.lineno() != lineno) {
               throw new IOException (
                  "Expected link name after 'link=', line "+lineno);
            }
            linkName = rtok.sval;
         }
         else if (word1 == null && rtok.tokenIsWord()) {
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
      if (word1 == null) {
         return; // empty line
      }
      SystemType sysType = null;
      String name = word1;
      if (word2 != null) {
         sysType = NativeLibraryManager.getSystemType (word1);
         if (sysType == SystemType.Unknown || sysType == null) {
            throw new IOException (
                "Illegal system specifier '"+word1+"', line "+lineno);
         }
         name = word2;
      }
      if (linkName != null) {
         if (obsolete || name.endsWith (".jar")) {
            throw new IOException (
               "Link not allowed for '"+name+"', line "+lineno);
         }
         if (!NativeLibraryManager.isValidLinkName (name, linkName)) {
            throw new IOException (
               "Invalid link name '"+linkName+"' for library '"+name+
               "', line "+lineno);
         }
      }
      if (obsolete) {
         if (sysType == null ||
             NativeLibraryManager.getSystemType().isInstanceOf (sysType)) {
            addObsolete (name, lineno);
         }
      }
      else {
         if (name.contains ("*")) {
            throw new IOException (
               "Wildcard not allowed in library name '"+name+
               "', line "+lineno);
         }
         if (name.endsWith (".jar")) {
            myAllJarnames.add (name);
            if (sysType == null) {
               myClasspathJarnames.add (name);
            }
         }
         maybeAddLibrary (name, sysType, linkName);
      }
   }

   /**
    * Updates the jar file entries of an Eclipse .classpath file so that they
    * match the jar files in the library list that have no system specifier
    * (i.e., the jar files that form ArtiSynth's classpath; see
    * bin/makeClasspath). All existing entries for jar files in lib/ are
    * removed, and entries for the listed jar files are inserted, in the
    * listed order, where the first removed entry was (or before the output
    * entry if there were none). A new entry keeps the attributes (such as
    * javadoc locations) of any removed entry whose jar file has the same
    * stem (see {@link #getJarStem}). Each entry is also given the attribute
    * {@code optional="true"}, so that Eclipse does not flag the project as
    * having build path errors when the jar file is missing (as it is in a
    * fresh checkout, before the libraries are installed). Other entries, and
    * the file's
    * formatting, are left unchanged, and the file is rewritten only if its
    * contents change, so that Eclipse is not triggered to rebuild
    * needlessly. Nothing is done if no library list has been read.
    *
    * @param cpfile .classpath file to update
    * @return {@code true} if the file was changed
    */
   public boolean updateEclipseClasspath (File cpfile) throws IOException {
      if (!myLibsRead) {
         return false;
      }
      String text;
      try {
         text = new String (
            Files.readAllBytes (cpfile.toPath()), StandardCharsets.UTF_8);
      }
      catch (IOException e) {
         throw new IOException ("Can't read "+cpfile+": "+e, e);
      }
      String eol = text.contains ("\r\n") ? "\r\n" : "\n";
      // matches a complete classpathentry element, together with its
      // indentation and trailing line ending
      Pattern entryPat = Pattern.compile (
         "([ \\t]*)<classpathentry\\b([^>]*?)(/>|>.*?</classpathentry>)"+
         "[ \\t]*(\\r?\\n)?", Pattern.DOTALL);
      Pattern libPathPat = Pattern.compile (
         "\\bpath=\"lib/([^\"/\\\\]+\\.jar)\"");
      Pattern libKindPat = Pattern.compile ("\\bkind=\"lib\"");
      Pattern outputKindPat = Pattern.compile ("\\bkind=\"output\"");

      HashMap<String,String> oldEntries = new HashMap<>(); // stem -> entry
      HashMap<String,String> oldPaths = new HashMap<>();   // stem -> jarname
      StringBuilder head = new StringBuilder();
      StringBuilder tail = null; // text following the insertion point
      String indent = null;
      int outputIdx = -1;        // insertion point if no lib entries found
      int last = 0;
      Matcher m = entryPat.matcher (text);
      while (m.find()) {
         StringBuilder buf = (tail != null ? tail : head);
         buf.append (text, last, m.start());
         last = m.end();
         String attrs = m.group(2);
         Matcher pm = libPathPat.matcher (attrs);
         if (libKindPat.matcher(attrs).find() && pm.find()) {
            String stem = getJarStem (pm.group(1));
            if (!oldEntries.containsKey (stem)) {
               oldEntries.put (stem, m.group());
               oldPaths.put (stem, pm.group(1));
            }
            if (tail == null) {
               indent = m.group(1);
               tail = new StringBuilder();
            }
         }
         else {
            if (tail == null && outputIdx == -1 &&
                outputKindPat.matcher(attrs).find()) {
               outputIdx = head.length();
            }
            buf.append (m.group());
         }
      }
      if (tail == null) {
         tail = new StringBuilder();
         int idx = outputIdx;
         if (idx == -1) {
            idx = head.lastIndexOf ("</classpath>");
            if (idx != -1) {
               idx = head.lastIndexOf ("\n", idx)+1;
            }
         }
         if (idx == -1) {
            throw new IOException (
               "Can't update "+cpfile+": no </classpath> element");
         }
         tail.append (head, idx, head.length());
         head.setLength (idx);
      }
      tail.append (text, last, text.length());
      if (indent == null) {
         indent = "\t";
      }
      StringBuilder newText = head;
      for (String jarname : myClasspathJarnames) {
         String stem = getJarStem (jarname);
         String entry = oldEntries.remove (stem);
         if (entry != null) {
            // keep the old entry, with the path updated
            entry = entry.replace (
               "path=\"lib/"+oldPaths.get(stem)+"\"",
               "path=\"lib/"+jarname+"\"");
            if (!entry.endsWith ("\n")) {
               entry += eol;
            }
         }
         else {
            entry = indent+"<classpathentry exported=\"true\" kind=\"lib\" "+
               "path=\"lib/"+jarname+"\"/>"+eol;
         }
         newText.append (addOptionalAttribute (entry, indent, eol));
      }
      newText.append (tail);
      String newStr = newText.toString();
      if (newStr.equals (text)) {
         return false;
      }
      try {
         Files.write (cpfile.toPath(), newStr.getBytes (StandardCharsets.UTF_8));
      }
      catch (IOException e) {
         throw new IOException ("Can't write "+cpfile+": "+e, e);
      }
      return true;
   }

   /**
    * Adds the attribute {@code optional="true"} to a .classpath entry, if it
    * does not already have an {@code optional} attribute. Nested elements are
    * indented by one tab more than their parent, as Eclipse does.
    *
    * @param entry complete classpathentry element, with its indentation and
    * trailing line ending
    * @param indent indentation of the element
    * @param eol line ending used in the file
    * @return entry with the attribute added
    */
   private String addOptionalAttribute (
      String entry, String indent, String eol) {
      if (entry.contains ("name=\"optional\"")) {
         return entry;
      }
      String attr = "<attribute name=\"optional\" value=\"true\"/>";
      int idx;
      if ((idx = entry.indexOf ("</attributes>")) != -1) {
         // add to the existing attributes element
         int lineStart = entry.lastIndexOf ('\n', idx)+1;
         String attrsIndent = entry.substring (lineStart, idx);
         return (entry.substring (0, lineStart)+
                 attrsIndent+"\t"+attr+eol+entry.substring (lineStart));
      }
      String attrs =
         indent+"\t<attributes>"+eol+
         indent+"\t\t"+attr+eol+
         indent+"\t</attributes>"+eol;
      if ((idx = entry.indexOf ("</classpathentry>")) != -1) {
         // insert an attributes element before the closing tag
         int lineStart = entry.lastIndexOf ('\n', idx)+1;
         return (entry.substring (0, lineStart)+attrs+
                 entry.substring (lineStart));
      }
      // self-closing element: open it up to hold an attributes element
      idx = entry.lastIndexOf ("/>");
      return (entry.substring (0, idx)+">"+eol+attrs+
              indent+"</classpathentry>"+entry.substring (idx+2));
   }

   /**
    * Reads the library list from a file (in the format of lib/LIBRARIES),
    * or from standard input if {@code file} is {@code null}.
    */
   public void readLibs (File file) throws IOException {
      readFile (file, /*obsolete=*/false);
      myLibsRead = true;
   }

   /**
    * Reads the obsolete list from a file (in the format of lib/OBSOLETE),
    * or from standard input if {@code file} is {@code null}. Each line
    * contains the name of an obsolete jar file or native library, optionally
    * preceded by a system specifier, as in lib/LIBRARIES. Names may contain
    * '*' wildcards. If no obsolete list is read before {@link
    * #removeOldJars} or {@link #removeOldNativeLibs} is called, then
    * OBSOLETE in the library folder is read if present.
    */
   public void readObsolete (File file) throws IOException {
      myObsoleteRead = true;
      try {
         readFile (file, /*obsolete=*/true);
      }
      catch (IOException e) {
         myObsoleteJarnames.clear();
         myObsoleteLibnames.clear();
         throw e;
      }
   }

   private void maybeReadObsolete() {
      if (!myObsoleteRead) {
         File file = new File (myLibDir, "OBSOLETE");
         if (file.canRead()) {
            try {
               readObsolete (file);
            }
            catch (IOException e) {
               System.out.println (
                  "Warning: "+e.getMessage()+"; ignoring obsolete libraries");
            }
         }
         myObsoleteRead = true;
      }
   }

   private void readFile (File file, boolean obsolete) throws IOException {
      ReaderTokenizer rtok = null;
      if (file == null) {
         // open from standard input
         rtok = new ReaderTokenizer (new InputStreamReader (System.in));
      }
      else {
         rtok = new ReaderTokenizer (
            new BufferedReader (new FileReader (file)));
         rtok.setResourceName (file.toString());
      }
      
      try {
         rtok.wordChars (".-*");
         rtok.eolIsSignificant (true);
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            readLine (rtok, obsolete);
         }
      }
      catch (Exception e) {
         if (file == null) {
            throw new IOException (
               "Error reading input: " + e.getMessage(), e);
         }
         else {
            throw new IOException (
               "Error reading file "+file+": "+e.getMessage(), e);
         }
      }
      finally {
         if (rtok != null) {
            rtok.close();
         }
      }
   }
}
