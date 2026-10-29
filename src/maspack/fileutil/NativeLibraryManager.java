/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import maspack.fileutil.uri.URIx;
import maspack.util.Logger.LogLevel;
import maspack.util.PathFinder;

/**
 * Class to ensure that necessary native libraries are installed on this
 * system. Native libraries that are not present, or which are out of date, can
 * be downloaded from the ArtiSynth web server.
 *
 *<p> Normally, this class operates as a singleton and is accessed through the
 * static methods {@link #verify verify()} and {@link #load load()}.  Native
 * libraries are stored in a native-specific directory within a generic library
 * directory that is specified using {@link #setLibDir setLibDir()}. This must
 * be set in order to enable the downloading and updating of libraries. If this
 * is not set, then <code>verify</code> and <code>load</code> will attempt to
 * find the specified libraries in the system's load library path, but no
 * attempt will be made to update or load them.
 * 
 *<p>
 * Libraries are specified generically using a name of the form
 * <pre>
 *    &lt;basename&gt;&lt;versionStr&gt;
 * </pre>
 * where &lt;versionStr&gt; is an optional string of the form ".I.J.K ..."
 * with I, J, K, etc. being non-negative integers that specify version
 * numbers. Versions are ordered by comparing these numbers from left to
 * right, with numbers to the left having priority. For example,
 * <code>solver.1.3</code> and <code>solver.1.3.77</code> both indicate a
 * library with a basename of <code>solver</code>, with
 * <code>solver.1.3.77</code> being the more recent version.
 *
 * <p>A versioned library name is satisfied by any library whose version
 * string starts with the same numbers, possibly followed by additional
 * ones. That is, <code>xxx.I.J</code> is satisfied by <code>xxx.I.J</code>,
 * <code>xxx.I.J.K</code>, <code>xxx.I.J.K.L</code>, etc., but not by
 * <code>xxx.I.JK</code> or <code>xxx.I.(J+1)</code>. This allows
 * code to request a library as (say) <code>gdcmjni.3.0.22</code>, so that
 * patch versions such as <code>gdcmjni.3.0.22.1</code> can be introduced
 * without changing the code. Unversioned names are only satisfied by
 * themselves.
 *
 * <p>{@link #load load()} resolves a library name as follows. If {@link
 * #verify verify()} was previously called (e.g., by the library installer)
 * for a name with the same basename that satisfies the requested name, then
 * that name is used. Otherwise, if the library file named exactly is not
 * present, the most recent satisfying library present is used. Failing that,
 * an attempt is made to download the exactly named library. {@link #verify
 * verify()}, which ensures that a specific library version is installed,
 * instead tries the download first, using a present satisfying library only
 * if the download fails.
 *
 * <p>Generic library names are converted into library file names appropriate
 * for the current native system. For example, <code>solver.1.3</code>,
 * would be converted into the following native file names:
 * <pre>
 *   libsolver.so.1.3     // Linux
 *   libsolver.1.3.dylib  // MacOS
 *   solver.1.3.dll       // Windows
 * </pre>
 *
 * <p>Alternative, it is also possible to specify an actual system-specific
 * library file name to a {@link #verify verify()} or {@link #load load()}
 * request. In that case, the system type is determined from the name, and the
 * request is carried out only if this matches the actual current system.
 *
 * <p>A versioned library can also be given a link with a different
 * (usually unversioned) file name, using {@link #verify(String,String)
 * verify(libName,linkName)} or {@link #load(String,String)
 * load(libName,linkName)}. This is for support libraries, such as {@code
 * libiomp5.so}, that other native libraries reference by a fixed name: the
 * versioned file (e.g., {@code libiomp5.so.2026.1}) is downloaded and
 * updated as usual, and the link (a symbolic link, or a copy on Windows)
 * makes it available under the fixed name.
 */
public class NativeLibraryManager {

   private static String SEP = File.separator;
   private static String PSEP = File.pathSeparator;
   private int downloadCnt = 0; // cumulative number of files downloaded

   /**
    * Indicates the operating system type.
    */
   public enum SystemType {
      Unknown,    // unknown system type
      Linux32,     
      Linux64,
      Linux,      // any version of Linux
      Windows32,
      Windows64,
      Windows,    // any version of Windows
      MacOS64,
      MacOSArm64, // 64 bit Arm architectue
      MacOS,      // any version of MacOS
      Generic;    // any system type

      /**
       * Returns the system types which are immediate subtypes of a given type
       */
      public SystemType[] getSubTypes () {
         switch (this) {
            case Generic:
               return new SystemType[] {Linux, Windows, MacOS};
            case Linux:
               return new SystemType[] {Linux32, Linux64};
            case MacOS:
               return new SystemType[] {MacOS64, MacOSArm64};
            case Windows:
               return new SystemType[] {Windows32, Windows64};
            default:
               return new SystemType[] {};
         }
      }
      
      private void recursivelyGetSubTypes (
         HashSet<SystemType> allSubTypes, SystemType sysType) {
         for (SystemType type : sysType.getSubTypes()) {
            allSubTypes.add (type);
            recursivelyGetSubTypes (allSubTypes, type);
         }
      }

      /**
       * Returns true if this system type is an instance of the (possibly
       * more general) system type specified by <code>sysType</code>.
       * 
       * @param sysType system type to test against
       * @return true if this type is an instance of sysType.
       */
      public boolean isInstanceOf (SystemType sysType) {
         if (sysType == this) {
            return true;
         }
         else {
            HashSet<SystemType> allSubTypes = new HashSet<SystemType>();
            recursivelyGetSubTypes (allSubTypes, sysType);
            return allSubTypes.contains (this);
         }
      }
   };
   
   public static SystemType getSystemType (String typeName) {
      try {
         return SystemType.valueOf (typeName);
      }
      catch (Exception e) {
         return null;
      }
   }
   
   File myLibDir;
   SystemType mySystemType;
   FileTransferListener myTransferListener;

   Pattern myVersionStrPattern;

   // exception (if any) from most recent grab attempt:
   Exception myGrabException;
   String myGrabErrorMessage;

   // names of links (or copies, on Windows) that refer to versioned library
   // files. These are never removed as older or obsolete libraries.
   HashSet<String> myLinkNames = new HashSet<>();

   // versioned libraries that have been verified, keyed by basename. Used by
   // load() to resolve names that these libraries satisfy.
   HashMap<String,LibDesc> myVerifiedLibs = new HashMap<>();

   /**
    * Flag to indicate that when checking a library, we see if it matches the
    * latest version on the server, and if not, download the latest version
    * from the server.
    */
   public static int UPDATE = 0x01;

   /**
    * Flag to enable messages to be printed to console.
    */
   public static int VERBOSE = 0x02;

   // remote server from which to obtain libraries.
   private static String myRemoteServer = "www.artisynth.org";
   private static String myRemoteHost = // where to try and get missing files
      "https://"+myRemoteServer+"/files/lib";

   LibDesc createDesc (String libName) {
      return new LibDesc (libName);
   }         

   /**
    * Descriptor for a library name, which breaks out its basename, version
    * string (if any), and system type (if any).
    */
   class LibDesc {
      SystemType mySys; // General system type associated with the library
      String myBasename;    // Library basename
      String myVersionStr;  // version string, or "" if none
      
      public LibDesc (String libName) {
         set (libName);
      }

      private String setVersionInfo (String str) {
         Matcher m = myVersionStrPattern.matcher (str);
         if (m.matches()) {
            myVersionStr = m.group(2);
            return m.group(1);
         }
         else {
            myVersionStr = "";
            return str;
         }
      }         

      /**
       * Sets the components of this description from a string name.
       */
      public void set (String libName) {

         // check for explicit library names first ... 
         if (libName.endsWith (".dll")) {
            // indicates a Windows library
            mySys = SystemType.Windows;
            String base = libName.substring (0, libName.length()-4);
            myBasename = setVersionInfo (base);
         }
         else if (libName.endsWith (".dylib")) {
            // indicates a MacOS library
            if (!libName.startsWith ("lib")) {
               throw new IllegalArgumentException (
                  "Illegal library name: "+libName+" should have 'lib' prefix");
            }
            mySys = SystemType.MacOS;
            String base = libName.substring (3, libName.length()-6);
            myBasename = setVersionInfo (base);
         }
         else if (libName.endsWith (".so")) {
            // indicates a Linux library
            if (!libName.startsWith ("lib")) {
               throw new IllegalArgumentException (
                  "Illegal library name: "+libName+" should have 'lib' prefix");
            }
            mySys = SystemType.Linux;
            myBasename = libName.substring (3, libName.length()-3);
            myVersionStr = "";
         }
         else if (libName.contains (".so.")) {
            // also indicates a Linux library
            if (!libName.startsWith ("lib")) {
               throw new IllegalArgumentException (
                  "Illegal library name: "+libName+" should have 'lib' prefix");
            }
            int idx = libName.lastIndexOf (".so.");
            setVersionInfo (libName.substring (idx+3));
            if (myVersionStr.equals ("")) {
               throw new IllegalArgumentException (
                  "Illegal library name: '"+libName+"'");
            }
            mySys = SystemType.Linux;
            myBasename = libName.substring (3, idx);            
         }
         else {
            // generic library name - no system
            mySys = SystemType.Generic;
            myBasename = setVersionInfo (libName);
         }

         if (myBasename.equals ("")) {
            throw new IllegalArgumentException (
                  "Illegal library name: '"+libName+"'");
         }
      }

      /**
       * Returns true if this library name is satisfied by the library
       * described by {@code desc}; i.e., if they have the same basename, and
       * either their version strings are equal, or this library has a
       * non-empty version string which is followed in that of {@code desc}
       * by additional numbers. For example, {@code foo.1.2} is satisfied by
       * {@code foo.1.2} and {@code foo.1.2.3}, but not {@code foo.1.23} or
       * {@code foo.1}, while {@code foo} is satisfied only by {@code foo}.
       * System types are not compared.
       */
      boolean isSatisfiedBy (LibDesc desc) {
         if (!myBasename.equals (desc.myBasename)) {
            return false;
         }
         if (myVersionStr.equals (desc.myVersionStr)) {
            return true;
         }
         return (!myVersionStr.equals ("") &&
                 desc.myVersionStr.startsWith (myVersionStr+"."));
      }

      /**
       * Returns the actual library file name appropriate to the current
       * system.
       */
      public String getFileName (SystemType sysType) {
         StringBuilder builder = new StringBuilder();
         switch (sysType) {
            case MacOS64:
            case MacOSArm64: {
               builder.append ("lib");
               builder.append (myBasename);
               builder.append (myVersionStr);
               builder.append (".dylib");
               break;
            }
            case Linux32:
            case Linux64: {
               builder.append ("lib");
               builder.append (myBasename);
               builder.append (".so");
               builder.append (myVersionStr);
               break;
            }
            case Windows32:
            case Windows64: {
               builder.append (myBasename);
               builder.append (myVersionStr);
               builder.append (".dll");
               break;
            }
            default: {
               throw new NativeLibraryException (
                  "Unimplemented system type: "+sysType);
            }
         }
         return builder.toString();
      }

      /**
       * Checks to see if an indicated system type is compatible with
       * the system type associated with this library.
       */
      boolean matchesSystem (SystemType sysType) {
         return sysType.isInstanceOf (mySys);
      }

   }

   FileTransferListener getTransferListener() {
      return myTransferListener;
   }

   void setTransferListener (FileTransferListener l) {
      myTransferListener = l;
   }

   /**
    * Sets a file transfer listener for the library manager, to monitor the
    * progress of file transfers from the server. If set, this overrides the
    * default transfer listener that displays progress by writing to the
    * console. An application-specified listener can be used to drive a
    * GUI-based progress bar.
    *
    * @see #getFileTransferListener
    */
   public static void setFileTransferListener (FileTransferListener l) {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      myManager.setTransferListener(l);
   }

   /**
    * Returns the application-specified file transfer listener for this library
    * manager, if any.
    *
    * @see #setFileTransferListener
    */
   public static FileTransferListener getFileTransferListener() {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      return myManager.getTransferListener();
   }

   int myFlags = 0;

   /**
    * Determines the current system type.
    */
   private SystemType determineSystemType () {
      
      String osname = System.getProperty ("os.name");
      String osarch = System.getProperty ("os.arch");
      //System.out.println ("osname=" + osname);
      if (osname.equals ("Linux")) {
         if (osarch.endsWith ("64")) {
            return SystemType.Linux64;
         }
         else {
            return SystemType.Linux32;
         }
      }
      else if (osname.startsWith ("Windows")) {
         if (osarch.endsWith ("64")) {
            return SystemType.Windows64;
         }
         else {
            return SystemType.Windows32;
         }
      }
      else if (osname.equals ("Darwin") ||
               osname.startsWith ("Mac")) {
         if (osarch.equals ("aarch64")) {
            return SystemType.MacOSArm64;
         }
         else {
            return SystemType.MacOS64;
         }
      }
      else {
         return SystemType.Unknown;
      }
   }

   private static NativeLibraryManager myManager = null;

   /**
    * Returns the current system type.
    */
   public static SystemType getSystemType() {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      return myManager.mySystemType;
   }

   /**
    * Explicitly sets the current system type.
    */
   public static void setSystemType (SystemType type) {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      myManager.mySystemType = type;
   }

   NativeLibraryManager () {
      myFlags = 0;
      mySystemType = determineSystemType();
      if (mySystemType == SystemType.Unknown) {
         System.out.println (
            "Error: NativeLibraryManager cannot determine system type; " +
            "cannot load native libraries");
      }
      myVersionStrPattern = Pattern.compile ("([^.]*)((\\.[0-9]+)+)$");
      setDefaultLibDir();
   }
   
   void setLibDir (File libBaseDir) {
      myLibDir = new File (libBaseDir, getNativeDirectoryName());
      if (!myLibDir.exists()) {
         if (!myLibDir.mkdirs()) {
            throw new NativeLibraryException (
               "NativeLibraryManager: can't create directory "+myLibDir);
         }
         else {
            if ((myFlags & VERBOSE) != 0) {
               System.out.println ("NativeLibraryManager: created "+myLibDir);
            }
         }
      }
      else if (!myLibDir.isDirectory()) {
         throw new NativeLibraryException (
            myLibDir+" already exists and is not a directory");
      }
   }

   void setDefaultLibDir () {
      // look for a default library directory in ../../../lib
      myLibDir = null;
      try {
         // find sourceDir using the actual class instead of the name since the
         // latter seems to leave some locked references in the lib folder.
         File baseDir = new File(PathFinder.findSourceDir (NativeLibraryManager.class));
         for (int i=0; i<3; i++) {
            baseDir = baseDir.getParentFile();
         }
         baseDir = new File(baseDir, "lib");
         File libDir = new File (baseDir, getNativeDirectoryName());
         if (libDir.exists() && libDir.isDirectory()) {
            myLibDir = libDir;
         }
      }
      catch (Exception e) {
         // ignore - myLibDir will just be null
      }
   }

   String getFileName (LibDesc desc) {
      return desc.getFileName (mySystemType);
   }

   /**
    * Returns the system-specific file name for the named library.  For
    * example, on Linux, <pre> getFileName ("foo.1.4"); </pre> will return
    * <code>libfoo.so.1.4</code>. See the discussion in the class header for
    * more information about how the library name is formatted.
    *
    * @param libName name of the library.
    */
   public static String getFileName (String libName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      LibDesc desc = myManager.createDesc (libName);
      return myManager.getFileName (desc);
   }

   /**
    * Sets the main library directory which contains the native directory.
    * This must be set in order to enable downloading and updating of
    * libraries from the server.
    *
    * @param libBaseName full path name of the base directory containing
    * the native library directory.
    */
   public static void setLibDir (String libBaseName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      File libBaseDir = null;
      if (libBaseName != null) {
         libBaseDir = new File (libBaseName);
         if (!libBaseDir.isDirectory()) {
            throw new IllegalArgumentException (
               "NativeLibraryManager: "+libBaseName+" is not a directory");
         }
      }
      myManager.setLibDir (libBaseDir);
   }

   /**
    * Returns the main library directory which currently set (or
    * <code>null</code> if no directory is set).
    */
   public static String getLibDir () {
      if (myManager == null) {
         myManager = new NativeLibraryManager();
      }
      return myManager.myLibDir.toString();
   }

   /**
    * Returns the most recent library file in {@code dir} that is for the
    * current system and satisfies the versioned library {@code desc} (as
    * determined by {@link LibDesc#isSatisfiedBy}), or {@code null} if there
    * is none, or {@code desc} is unversioned. Link names and symbolic links
    * are ignored.
    */
   File findSatisfying (LibDesc desc, File dir) {
      if (desc.myVersionStr.equals ("")) {
         return null;
      }
      File[] files = dir.listFiles();
      if (files == null) {
         return null;
      }
      File bestFile = null;
      LibDesc bestDesc = null;
      for (File file : files) {
         if (myLinkNames.contains (file.getName()) ||
             Files.isSymbolicLink (file.toPath())) {
            continue;
         }
         LibDesc fileDesc;
         try {
            fileDesc = createDesc (file.getName());
         }
         catch (IllegalArgumentException e) {
            continue; // not a library file name
         }
         if (fileDesc.mySys != SystemType.Generic &&
             fileDesc.matchesSystem (mySystemType) &&
             desc.isSatisfiedBy (fileDesc) &&
             (bestDesc == null ||
              compareVersionStrs (
                 fileDesc.myVersionStr, bestDesc.myVersionStr) > 0)) {
            bestFile = file;
            bestDesc = fileDesc;
         }
      }
      return bestFile;
   }

   private static String libPathProp = "java.library.path";

   String getFromLoadLibraryPath (LibDesc desc) {
      String libPath = System.getProperty(libPathProp, "");
      String[] libDirs = libPath.split (PSEP);
      String libFileName = getFileName (desc);
      for (int i=0; i<libDirs.length; i++) {
         File libFile = new File (libDirs[i], libFileName);
         if (libFile.exists()) {
            return libFile.toString();
         }
      }
      for (int i=0; i<libDirs.length; i++) {
         File libFile = findSatisfying (desc, new File (libDirs[i]));
         if (libFile != null) {
            return libFile.toString();
         }
      }
      return null;
   }

   /**
    * Returns a short name for a file in the native library folder, for use
    * in messages, consisting of the file name preceded by the names of the
    * library folder and its parent (e.g., "lib/MacOS64/libfoo.dylib"). Full
    * paths are long enough that they tend to scroll console windows to the
    * right, hiding the start of the lines. Other files are named by their
    * full path.
    */
   String displayName (File file) {
      if (myLibDir != null && myLibDir.equals (file.getParentFile())) {
         File base = myLibDir.getParentFile();
         String prefix = (base != null ? base.getName()+"/" : "");
         return prefix+myLibDir.getName()+"/"+file.getName();
      }
      return file.toString();
   }

   void grabFile (File libFile, LibDesc desc, boolean checkHash) {
      
      FileManager Manager = new FileManager();
      // INFO omits FileManager's debug messages, such as "Downloading file
      // <url> to <path>", which duplicate the transfer listener's messages
      Manager.setVerbosityLevel (LogLevel.INFO);
      if ((myFlags & VERBOSE) != 0) {
         FileTransferListener listener = myTransferListener;
         if (listener == null) {
            listener = new DefaultConsoleFileTransferListener();
         }
         Manager.addTransferListener(listener);
         Manager.getTransferMonitor().setPollSleep(100);  // 100ms
      }
      String localLibFile = libFile.toString();
      String remoteLibFile =
         myRemoteHost+"/"+getNativeDirectoryName()+"/"+getFileName(desc);
      boolean grab = true;
      if (checkHash) {
         grab = !Manager.equalsHash(
         new File(localLibFile), new URIx(remoteLibFile));
      }
      if (grab) {
         Manager.getRemote (localLibFile, remoteLibFile);
         downloadCnt++;
      }
   }

   // void updateFile (File libFile, LibDesc desc) throws URIxSyntaxException {
      
   //    FileManager Manager = new FileManager();
   //    Manager.setVerbosityLevel (0);
   //    if ((myFlags & VERBOSE) != 0) {
   //       FileTransferListener listener = myTransferListener;
   //       if (listener == null) {
   //          listener = new DefaultConsoleFileTransferListener();
   //       }
   //       Manager.addTransferListener(listener);
   //       Manager.getTransferMonitor().setPollSleep(100);  // 100ms
   //    }
   //    String localLibFile = libFile.toString();
   //    String remoteLibFile =
   //       myRemoteHost+"/"+getNativeDirectoryName()+"/"+getFileName(desc);
   //    boolean match = Manager.equalsHash(
   //       new File(localLibFile), new URIx(remoteLibFile));
   //    if (!match) {
   //       Manager.getRemote (localLibFile, remoteLibFile);
   //    }
   // }

   /**
    * Gets a full path name to the specified library. If a base library
    * directory has been set using {@link #setLibDir setLibDir()}, then the
    * library will be downloaded or updated from the server if necessary.
    * Otherwise, the library will be searched for in the system's load library
    * path. If the library file is not present, and cannot be downloaded,
    * then the most recent library present that satisfies it is used instead
    * (see {@link LibDesc#isSatisfiedBy}). If {@code satisfyFirst} is {@code
    * true}, a satisfying library is looked for before attempting the
    * download. If it is not possible to locate the library or download it,
    * then <code>null</code> is returned, and <code>myGrabException</code>
    * can be queried for any exception that occured during an attempted
    * download.
    */
   String getLibraryPath (LibDesc desc, boolean satisfyFirst) {
      
      // check that the file exists in the load library dir

      myGrabException = null;
      String libPath = null;
      if (myLibDir != null) {
         // look for the file in the specified lib directiry
         File libFile = new File (myLibDir, getFileName(desc));
         if (!libFile.exists()) {
            File altFile = null;
            if (satisfyFirst) {
               altFile = findSatisfying (desc, myLibDir);
            }
            if (altFile == null) {
               // try to download the file
               try {
                  grabFile (libFile, desc, /*checkhash=*/false);
                  libPath = libFile.toString();
               }
               catch (Exception e) {
                  myGrabException = e;
               }           
               if ((myFlags & VERBOSE) != 0) {
                  if (myGrabException != null) {
                     System.out.println (
                        "NativeLibraryManager: failed to download "+
                        displayName(libFile)+":");
                     System.out.println (myGrabException.getMessage());
                  }
                  else {
                     System.out.println (
                        "NativeLibraryManager: downloaded "+
                        displayName(libFile));
                  }
               }
               if (libPath == null) {
                  altFile = findSatisfying (desc, myLibDir);
               }
            }
            if (altFile != null) {
               if ((myFlags & VERBOSE) != 0) {
                  System.out.println (
                     "NativeLibraryManager: using "+displayName(altFile)+
                     " in place of "+libFile.getName());
               }
               libPath = altFile.toString();
            }
         }
         else {
            if ((myFlags & UPDATE) != 0) {
               // try to update the file if possible
               try {
                  grabFile (libFile, desc, /*checkHash*/true);
               }
               catch (Exception e) {
                  myGrabException = e;
               }
               if ((myFlags & VERBOSE) != 0) {
                  if (myGrabException != null) {
                     System.out.println (
                        "NativeLibraryManager: failed to update "+
                        displayName(libFile)+":");
                     System.out.println (myGrabException.getMessage());
                  }
                  else {
                     System.out.println (
                        "NativeLibraryManager: updated "+displayName(libFile));
                  }
               }
            }
            libPath = libFile.toString();
         }
         // on Windows systems, make sure the .dll is executable
         if (libPath != null &&
             (mySystemType == SystemType.Windows32 ||
              mySystemType == SystemType.Windows64)) {
            try {
               File file = new File(libPath);
               if (!file.canExecute()) {
                  file.setExecutable(true);
               }
            }
            catch (Exception e) {
               System.out.println (
                  "Error trying to set execute permission for "+libPath);
            }
         }
      }
      else {
         libPath = getFromLoadLibraryPath (desc);
      }
      return libPath;
   }

   private static boolean messageContains (Throwable ex, String str) {
      return (ex.getMessage() != null && ex.getMessage().contains (str));
   }         

   /**
    * Verifies that the indicated native library is present, downloading or
    * updating it from the server if necessary. If the library cannot be found
    * or obtained from the server, an exception is thrown.  See the discussion
    * in the class header for more information about how the library name is
    * formatted.
    *
    * @param libName name of the library
    * @return 1 if the library was downloaded or updated, 0 otherwise
    * @throws maspack.fileutil.NativeLibraryException if the required library
    * is not present and cannot be obtained.
    */
   public static int verify (String libName) {
      int retval = 0;
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      LibDesc desc = myManager.createDesc (libName);
      if (desc.matchesSystem(myManager.mySystemType)) {
         int oldcnt = myManager.downloadCnt;
         if (myManager.getLibraryPath (desc, /*satisfyFirst=*/false) == null) {
            Exception grabEx = myManager.myGrabException;
            NativeLibraryException e =
               new NativeLibraryException (
                  "Could not download library "+getPathName(libName)+": "+
                  grabEx.getMessage());
            e.initCause (grabEx);
            throw e;
         }
         if (!desc.myVersionStr.equals ("")) {
            myManager.myVerifiedLibs.put (desc.myBasename, desc);
         }
         // library was downloaded or updated if download count changed
         if (myManager.downloadCnt != oldcnt) {
            retval = 1;
         }
      }
      return retval;
   }

   /**
    * Verifies that the indicated native library is present, as with {@link
    * #verify(String)}, and then makes sure that the file {@code linkName} in
    * the native library directory refers to it. This allows a versioned
    * library file (such as {@code libiomp5.so.2026.1}) to be found under the
    * unversioned name (such as {@code libiomp5.so}) by which other native
    * libraries reference it. On Linux and MacOS, {@code linkName} is a
    * symbolic link to the library file; on Windows (or if symbolic links
    * cannot be created) it is a copy of the library file. If {@code
    * linkName} already exists, it is replaced if it does not refer to (or
    * is not identical to) the library file. Files named by {@code linkName}
    * are never removed by {@link #removeOlderVersions} or {@link
    * #removeObsoleteLibraries}.
    *
    * @param libName name of the library
    * @param linkName system-specific file name of the link (see {@link
    * #isValidLinkName})
    * @return 1 if the library was downloaded or updated, or the link was
    * created or changed, 0 otherwise
    * @throws maspack.fileutil.NativeLibraryException if the required library
    * is not present and cannot be obtained, or if the link does not exist
    * and cannot be created.
    */
   public static int verify (String libName, String linkName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      LibDesc desc = myManager.createDesc (libName);
      if (!desc.matchesSystem(myManager.mySystemType)) {
         return 0;
      }
      if (!isValidLinkName (libName, linkName)) {
         throw new IllegalArgumentException (
            "Invalid link name '"+linkName+"' for library "+libName);
      }
      myManager.myLinkNames.add (linkName);
      int retval = verify (libName);
      if (myManager.myLibDir != null &&
          myManager.updateLink (desc, linkName)) {
         retval = 1;
      }
      return retval;
   }

   /**
    * Loads a Java native library through a link to it, after first calling
    * {@link #verify(String,String)} to make sure that both the library and
    * the link are present. Loading through the link means the library is
    * known to the system under the link name. On Windows, this allows
    * imports of that name by other native libraries to be satisfied by the
    * loaded library.
    *
    * @param libName name of the library
    * @param linkName system-specific file name of the link
    */
   public static void load (String libName, String linkName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      LibDesc desc = myManager.createDesc (libName);
      if (!desc.matchesSystem(myManager.mySystemType)) {
         return;
      }
      if (myManager.myLibDir == null) {
         // no library directory, so look for the link in the library path
         load (linkName);
         return;
      }
      verify (libName, linkName);
      String linkPath = new File (myManager.myLibDir, linkName).toString();
      if ((getFlags() & VERBOSE) != 0) {
         System.out.println ("Loading native library "+linkPath);
      }
      try {
         System.load (linkPath);
      }
      catch (Error e) {
         throw new NativeLibraryException (e.getMessage());
      }
      catch (Exception e) {
         throw new NativeLibraryException (e.getMessage());
      }
   }

   /**
    * Returns true if {@code linkName} is a valid link name for the library
    * {@code libName} (see {@link #verify(String,String)}). The link name
    * must be a system-specific library file name (such as {@code
    * libiomp5.so}), with no path separators or wildcards, for a system that
    * is compatible with the library's, and must differ from the library's
    * own file name on that system.
    *
    * @param libName name of the library
    * @param linkName name of the link
    * @return true if {@code linkName} is valid
    */
   public static boolean isValidLinkName (String libName, String linkName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      if (linkName.contains ("/") || linkName.contains ("\\") ||
          linkName.contains ("*")) {
         return false;
      }
      SystemType linkSys = getSystemSpecificType (linkName);
      if (linkSys == null) {
         return false;
      }
      LibDesc desc;
      try {
         desc = myManager.createDesc (libName);
         myManager.createDesc (linkName);
      }
      catch (IllegalArgumentException e) {
         return false;
      }
      if (!linkSys.isInstanceOf (desc.mySys) &&
          !desc.mySys.isInstanceOf (linkSys)) {
         return false;
      }
      if (myManager.mySystemType.isInstanceOf (linkSys) &&
          linkName.equals (desc.getFileName (myManager.mySystemType))) {
         return false;
      }
      return true;
   }

   private boolean isWindows() {
      return mySystemType.isInstanceOf (SystemType.Windows);
   }

   /**
    * Returns true if two files have identical contents.
    */
   static boolean filesEqual (File file0, File file1) throws IOException {
      if (file0.length() != file1.length()) {
         return false;
      }
      byte[] buf0 = new byte[65536];
      byte[] buf1 = new byte[65536];
      try (java.io.InputStream in0 = Files.newInputStream (file0.toPath());
           java.io.InputStream in1 = Files.newInputStream (file1.toPath())) {
         int n0;
         while ((n0 = in0.read (buf0)) > 0) {
            int n1 = 0;
            while (n1 < n0) {
               int n = in1.read (buf1, n1, n0-n1);
               if (n <= 0) {
                  return false;
               }
               n1 += n;
            }
            for (int i=0; i<n0; i++) {
               if (buf0[i] != buf1[i]) {
                  return false;
               }
            }
         }
         return in1.read() == -1;
      }
   }

   /**
    * Makes sure that the file {@code linkName} in the native library
    * directory refers to the library file for {@code desc}, which is assumed
    * to be present. Returns true if the link (or copy) was created or
    * changed.
    */
   boolean updateLink (LibDesc desc, String linkName) {
      File libFile = new File (myLibDir, getFileName (desc));
      File linkFile = new File (myLibDir, linkName);
      Path linkPath = linkFile.toPath();
      // relative target, so that the library directory can be moved
      Path target = Paths.get (libFile.getName());
      boolean exists = Files.exists (linkPath, LinkOption.NOFOLLOW_LINKS);
      boolean isLink = Files.isSymbolicLink (linkPath);
      try {
         if (isLink && Files.readSymbolicLink (linkPath).equals (target)) {
            return false;
         }
         // on Windows, an identical copy is what we want. Elsewhere, an
         // identical copy is replaced by a link if possible (see below).
         if (isWindows() && exists && !isLink &&
             filesEqual (linkFile, libFile)) {
            return false;
         }
      }
      catch (IOException e) {
         // can't read the link or file, so try to replace it
      }
      // create the link or copy under a temporary name and then move it into
      // place, so that the link name is never missing or partially written
      Path tmpPath = new File (myLibDir, linkName+".tmp").toPath();
      boolean linked = false;
      try {
         Files.deleteIfExists (tmpPath);
         if (!isWindows()) {
            try {
               Files.createSymbolicLink (tmpPath, target);
               linked = true;
            }
            catch (IOException | UnsupportedOperationException e) {
               // file system doesn't support links; use a copy instead
            }
         }
         if (!linked) {
            if (exists && !isLink && filesEqual (linkFile, libFile)) {
               return false;
            }
            Files.copy (libFile.toPath(), tmpPath);
         }
         Files.move (tmpPath, linkPath, StandardCopyOption.REPLACE_EXISTING);
      }
      catch (Exception e) {
         try {
            Files.deleteIfExists (tmpPath);
         }
         catch (IOException e2) {
            // ignore
         }
         String msg =
            "can't "+(exists ? "update " : "create ")+displayName(linkFile)+
            " for "+libFile.getName()+":\n"+e;
         if (exists) {
            System.out.println ("Warning: "+msg);
            System.out.println (
               "It may be in use by another program; "+
               "if this persists, remove it manually");
            return false;
         }
         else {
            throw new NativeLibraryException (msg);
         }
      }
      if (!linked && isWindows() && !linkFile.canExecute()) {
         linkFile.setExecutable (true);
      }
      if ((myFlags & VERBOSE) != 0) {
         if (linked) {
            System.out.println (
               "NativeLibraryManager: linked "+displayName(linkFile)+
               " to "+libFile.getName());
         }
         else {
            System.out.println (
               "NativeLibraryManager: copied "+libFile.getName()+
               " to "+displayName(linkFile));
         }
      }
      return true;
   }

   /**
    * Compares two version strings of the form ".A.B.C ...", returning a
    * negative number, 0, or a positive number if {@code vstr0} is less than,
    * equal to, or greater than {@code vstr1}. Numbers are compared from left
    * to right, and if one string is a prefix of the other, the shorter one is
    * less (e.g., .1.5 &lt; .1.5.1).
    */
   static int compareVersionStrs (String vstr0, String vstr1) {
      String[] nums0 = vstr0.substring(1).split ("\\.");
      String[] nums1 = vstr1.substring(1).split ("\\.");
      for (int i=0; i<Math.min(nums0.length, nums1.length); i++) {
         int cmp = Long.compare (
            Long.parseLong (nums0[i]), Long.parseLong (nums1[i]));
         if (cmp != 0) {
            return cmp;
         }
      }
      return nums0.length - nums1.length;
   }

   int removeOlderVersions (LibDesc desc) {
      if (myLibDir == null ||
          !desc.matchesSystem (mySystemType) ||
          desc.myVersionStr.equals ("")) {
         return 0;
      }
      File libFile = new File (myLibDir, getFileName (desc));
      if (!libFile.exists()) {
         return 0;
      }
      int numRemoved = 0;
      File[] files = myLibDir.listFiles();
      if (files == null) {
         return 0;
      }
      for (File file : files) {
         // links to libraries (e.g., libgfortran.so.5) may look like older
         // versions, but must be kept
         if (myLinkNames.contains (file.getName()) ||
             Files.isSymbolicLink (file.toPath())) {
            continue;
         }
         // parse the file name to see if it is a different version of the
         // same library for this system
         LibDesc fileDesc;
         try {
            fileDesc = createDesc (file.getName());
         }
         catch (IllegalArgumentException e) {
            continue; // not a library file name
         }
         if (fileDesc.mySys != SystemType.Generic &&
             fileDesc.matchesSystem (mySystemType) &&
             fileDesc.myBasename.equals (desc.myBasename) &&
             !fileDesc.myVersionStr.equals ("") &&
             compareVersionStrs (fileDesc.myVersionStr, desc.myVersionStr) < 0) {
            try {
               Files.delete (file.toPath());
               System.out.println (
                  "Notice: removed old native library "+displayName(file)+
                  ", replaced by "+libFile.getName());
               numRemoved++;
            }
            catch (Exception e) {
               System.out.println (
                  "Warning: can't remove old native library "+displayName(file)+
                  ", replaced by "+libFile.getName()+":\n" + e);
               System.out.println (
                  "It may be in use by another program; "+
                  "if this persists, remove it manually");
            }
         }
      }
      return numRemoved;
   }

   /**
    * Removes older versions of the indicated native library from the
    * native library directory. The library file for the current system must
    * be present (so this should be called after {@link #verify verify()}),
    * and the library name must have a version string. Files are removed if
    * they correspond to the same library, with a version string that is
    * lower than the one specified. For example, given the name
    * {@code TetgenJNI.1.5.1.1} on MacOS, {@code libTetgenJNI.1.5.dylib}
    * would be removed, but not {@code libTetgenJNI.1.6.dylib} or {@code
    * libTetgenJNI.dylib}. Files that cannot be removed (which can happen on
    * Windows if they are in use by another process) are left in place and a
    * warning is printed.
    *
    * @param libName name of the library
    * @return number of library files actually removed
    */
   public static int removeOlderVersions (String libName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      return myManager.removeOlderVersions (myManager.createDesc (libName));
   }

   // generic obsolete library names: a base name (without '.' or '*'),
   // followed by an optional version string, which may end in ".*"
   private static Pattern myGenericObsoletePattern =
      Pattern.compile ("([^.*]+)((?:\\.[0-9]+)*)(\\.\\*)?");

   /**
    * Returns the system associated with a system-specific library name
    * (which may contain '*' wildcards), or {@code null} if the name is not
    * system-specific.
    */
   private static SystemType getSystemSpecificType (String name) {
      if (name.endsWith (".dll")) {
         return SystemType.Windows;
      }
      else if (name.endsWith (".dylib")) {
         return SystemType.MacOS;
      }
      else if (name.matches (".*\\.so([.*].*)?")) {
         return SystemType.Linux;
      }
      else {
         return null;
      }
   }

   /**
    * Returns true if {@code str} matches {@code pattern}, in which each '*'
    * matches any sequence of characters.
    */
   public static boolean wildcardMatches (String pattern, String str) {
      StringBuilder regex = new StringBuilder();
      String[] parts = pattern.split ("\\*", -1);
      for (int i=0; i<parts.length; i++) {
         if (i > 0) {
            regex.append (".*");
         }
         if (parts[i].length() > 0) {
            regex.append (Pattern.quote (parts[i]));
         }
      }
      return str.matches (regex.toString());
   }

   /**
    * Returns true if {@code name} is a valid name for an obsolete native
    * library (see {@link #removeObsoleteLibraries}).
    */
   public static boolean isValidObsoleteName (String name) {
      if (name.contains ("/") || name.contains ("\\")) {
         return false;
      }
      SystemType sys = getSystemSpecificType (name);
      if (sys != null) {
         // a wildcard must be preceded by at least part of the base name
         int prefixLen = 0;
         if (sys != SystemType.Windows) {
            if (!name.startsWith ("lib")) {
               return false;
            }
            prefixLen = 3;
         }
         int idx = name.indexOf ('*');
         return idx == -1 || idx > prefixLen;
      }
      else {
         return myGenericObsoletePattern.matcher (name).matches();
      }
   }

   /**
    * Returns true if a file name matches an obsolete library name for the
    * current system.
    */
   boolean obsoleteNameMatches (String name, String fileName) {
      SystemType sys = getSystemSpecificType (name);
      if (sys != null) {
         return (mySystemType.isInstanceOf (sys) &&
                 wildcardMatches (name, fileName));
      }
      Matcher m = myGenericObsoletePattern.matcher (name);
      if (!m.matches()) {
         return false;
      }
      LibDesc fileDesc;
      try {
         fileDesc = createDesc (fileName);
      }
      catch (IllegalArgumentException e) {
         return false; // not a library file name
      }
      if (fileDesc.mySys == SystemType.Generic ||
          !fileDesc.matchesSystem (mySystemType) ||
          !fileDesc.myBasename.equals (m.group(1))) {
         return false;
      }
      String vstr = m.group(2);
      if (m.group(3) != null) {
         // ".*" matches the version string and any that extend it
         return (fileDesc.myVersionStr.equals (vstr) ||
                 fileDesc.myVersionStr.startsWith (vstr+"."));
      }
      else {
         return fileDesc.myVersionStr.equals (vstr);
      }
   }

   /**
    * Returns true if a file should be removed as obsolete: it must match a
    * name in {@code obsoleteNames}, and not be an active library. An exact
    * name (without '*') protects only the active library files themselves,
    * while a wildcard name protects any version of an active library.
    */
   boolean isObsoleteFile (
      String fileName, Collection<String> obsoleteNames,
      Set<String> activeFileNames, Set<String> activeBasenames) {
      boolean activeBasename = false;
      try {
         activeBasename =
            activeBasenames.contains (createDesc (fileName).myBasename);
      }
      catch (IllegalArgumentException e) {
         // not a library file name, so not an active library
      }
      for (String name : obsoleteNames) {
         if (obsoleteNameMatches (name, fileName)) {
            if (name.indexOf ('*') == -1) {
               if (!activeFileNames.contains (fileName)) {
                  return true;
               }
            }
            else if (!activeBasename) {
               return true;
            }
         }
      }
      return false;
   }

   int removeObsoleteFiles (
      Collection<String> obsoleteNames, Collection<String> activeNames,
      Collection<String> linkNames) {
      if (myLibDir == null || obsoleteNames.isEmpty()) {
         return 0;
      }
      HashSet<String> activeBasenames = new HashSet<>();
      HashSet<String> activeFileNames = new HashSet<>();
      for (String name : activeNames) {
         try {
            LibDesc desc = createDesc (name);
            activeBasenames.add (desc.myBasename);
            activeFileNames.add (getFileName (desc));
         }
         catch (RuntimeException e) {
            // ignore malformed names
         }
      }
      File[] files = myLibDir.listFiles();
      if (files == null) {
         return 0;
      }
      int numRemoved = 0;
      for (File file : files) {
         if (file.isDirectory()) {
            continue;
         }
         String fileName = file.getName();
         if (myLinkNames.contains (fileName) || linkNames.contains (fileName)) {
            continue; // links to active libraries are kept
         }
         if (!isObsoleteFile (
                fileName, obsoleteNames, activeFileNames, activeBasenames)) {
            continue;
         }
         try {
            Files.delete (file.toPath());
            System.out.println (
               "Notice: removed obsolete native library "+displayName(file));
            numRemoved++;
         }
         catch (Exception e) {
            System.out.println (
               "Warning: can't remove obsolete native library "+
               displayName(file)+":\n"+e);
            System.out.println (
               "It may be in use by another program; "+
               "if this persists, remove it manually");
         }
      }
      return numRemoved;
   }

   /**
    * Removes obsolete native libraries from the native library directory
    * for the current system. A file is removed if it matches one of the
    * names in {@code obsoleteNames}, unless it is one of the libraries in
    * {@code activeNames} or a link (see {@link #verify(String,String)}).
    * If the name it matches contains a '*' wildcard, the file is also kept
    * if it is any version of a library in {@code activeNames}. This allows
    * an unversioned or older file (such as {@code foo.dll}) to be removed
    * by naming it exactly, even when another version (such as {@code
    * foo.1.2.dll}) is in use. Obsolete names take two forms:
    *
    * <ul>
    * <li>A generic library name, such as {@code PardisoJNI.2021.1.1}, which
    * matches the file for that library and version on the current system.
    * The version string can end in ".*", so that {@code PardisoJNI.2021.*}
    * matches all 2021 versions, and {@code PardisoJNI.*} matches all
    * versions, including an unversioned file.
    *
    * <li>A system-specific file name, such as {@code libiomp5.so}, which
    * matches only on the indicated system. It can contain '*' wildcards,
    * provided that at least part of the base name precedes the first one
    * (e.g., {@code libiomp5*.so*} but not {@code lib*.so}).
    * </ul>
    *
    * Files that cannot be removed (which can happen on Windows if they are
    * in use by another process) are left in place and a warning is
    * printed.
    *
    * @param obsoleteNames names of the obsolete libraries
    * @param activeNames names of the libraries in use on the current system
    * @param linkNames file names of links to libraries in use, which are
    * kept in addition to those created by {@link #verify(String,String)}
    * @return number of library files actually removed
    */
   public static int removeObsoleteLibraries (
      Collection<String> obsoleteNames, Collection<String> activeNames,
      Collection<String> linkNames) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      return myManager.removeObsoleteFiles (
         obsoleteNames, activeNames, linkNames);
   }

   /**
    * Removes obsolete native libraries, as with {@link
    * #removeObsoleteLibraries(Collection,Collection,Collection)}, with
    * no links other than those created by {@link #verify(String,String)}.
    *
    * @param obsoleteNames names of the obsolete libraries
    * @param activeNames names of the libraries in use on the current system
    * @return number of library files actually removed
    */
   public static int removeObsoleteLibraries (
      Collection<String> obsoleteNames, Collection<String> activeNames) {
      return removeObsoleteLibraries (
         obsoleteNames, activeNames, Collections.emptyList());
   }

   /**
    * Returns true if the indicated native library name matches the current
    * system.
    */
   public static boolean libraryMatchesSystem (String libName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      LibDesc desc = myManager.createDesc (libName);
      return desc.matchesSystem(myManager.mySystemType);
   }

   /**
    * Sets the currently active flags for the library manager. At present,
    * possible flags consist of an or-ed collection of {@link #VERBOSE} or
    * {@link #UPDATE}.
    *
    * @see #getFlags
    */
   public static void setFlags (int flags) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      myManager.myFlags = flags;
   }

   /**
    * Returns the currently active flags for the library manager.
    *
    * @see #setFlags
    */
   public static int getFlags () {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      return myManager.myFlags;
   }

   /**
    * Returns the full path name for the specified library file on this
    * system. See the discussion in the class header for more information
    * about how the library name is formatted.
    *
    * @param libName name of the library.
    *
    * @throws IllegalStateException if the main library directory has
    * not been set using {@link #setLibDir setLibDir()}.
    */
   public static String getPathName (String libName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      if (myManager.myLibDir == null) {
         throw new IllegalStateException ("Library directory not set");
      }
      File libFile = new File (myManager.myLibDir, getFileName(libName));
      return libFile.toString();
   }

   /**
    * If a library that satisfies {@code desc} (as determined by {@link
    * LibDesc#isSatisfiedBy}) has been verified, returns the descriptor for
    * that library. Otherwise, returns {@code desc}.
    */
   LibDesc resolveVerified (LibDesc desc) {
      LibDesc vdesc = myVerifiedLibs.get (desc.myBasename);
      if (vdesc != null && desc.isSatisfiedBy (vdesc)) {
         return vdesc;
      }
      return desc;
   }

   /**
    * Loads a Java native library specified by libName. This is a wrapper for
    * <code>System.load()</code> that first attempts to locate the library
    * file, if necessary downloading or updating it from the server. See the
    * discussion in the class header for more information about how the library
    * name is formatted, and how libraries satisfying a versioned name are
    * resolved.
    *
    * @param libName name of the library.
    */
   public static void load (String libName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      LibDesc desc = myManager.createDesc (libName);
      // only load if the library matches the current system ...
      if (desc.matchesSystem(myManager.mySystemType)) {
         desc = myManager.resolveVerified (desc);
         String libPath = myManager.getLibraryPath(desc, /*satisfyFirst=*/true);
         if (libPath == null) {
            String libFileName = getFileName (libName);
            if (myManager.myLibDir != null) {
               IllegalStateException e = new IllegalStateException (
                  "Can't locate or fetch native library " + 
                  myManager.myLibDir+SEP+libFileName);
               e.initCause (myManager.myGrabException);
               throw e;
            }
            else {
               throw new IllegalStateException (
                  "Can't locate native library " + libFileName +
                  " in library path " + System.getProperty(libPathProp,""));
            }
         }     
         else {
            if ((getFlags() & VERBOSE) != 0) {
               System.out.println ("Loading native library "+libPath);
            }
            try {
               System.load (libPath);
            }
            catch (Error e) {
               throw new NativeLibraryException (e.getMessage());
            }
            catch (Exception e) {
               throw new NativeLibraryException (e.getMessage());
            }
         }
      }
   } 

   /**
    * Returns the native library directory name appropriate to the current
    * system, or null if the system is unknown.
    */
   private String getNativeDirectoryName () {
 
      switch (mySystemType) {
         case Linux32: {
            return "Linux32";
         }
         case Linux64: {
            return "Linux64";
         }
         case Windows32: {
            return "Windows32";
         }
         case Windows64: {
            return "Windows64";
         }
         case MacOS64: {
            return "MacOS64";
         }
         case MacOSArm64: {
            return "MacOSArm64";
            //return "Darwin-x86_64";
         }
         default: {
            throw new NativeLibraryException (
               "Unimplemented system type: "+mySystemType);
         }
      }
   }

   public static void main (String[] args) {
 
      NativeLibraryManager man = new NativeLibraryManager();
      for (int i=0; i<args.length; i++) {
         LibDesc desc = null;
         try {
            desc = man.createDesc (args[i]);
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         if (desc != null) {
            System.out.println ("base=" + desc.myBasename);
            System.out.println ("sys=" + desc.mySys);
            System.out.println ("versionStr=" + desc.myVersionStr);
            if (i < args.length-1) {
               System.out.println ("");
            }
         }
      }
   }
}
