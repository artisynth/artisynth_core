/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
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
 * with I, J, K, etc. being positive integers that specify version
 * numbers. The first number I specifies the major version number,
 * while the second number J, if present, specifies the minor
 * version number. Less significant version numbers may be
 * present but are not used by the library manager.
 *
 * <p>For example, <code>solver.1.3</code> <code>solver.1.3.77</code> both
 * indicate a library with a basename of <code>solver</code>, a major version
 * number of 1, and a minor version number of 3, while <code>mesh.4</code>
 * indicates a library with a basename of <code>mesh</code>, a major version
 * number of 4, and no minor version.
 *
 * <p>Libraries with the same basename but different major version numbers are
 * assumed to be incompatible. However, libraries with the same basename and
 * major version are assumed to be backward compatible across minor version
 * numbers. That means that if we need a library <code>xxx.I.J</code>,
 * then any library <code>xxx.I.K</code> will suffice as long as K &gt;= J.
 * If a library is requested and it is not present and cannot be
 * downloaded, but a compatible library with a higher minor version
 * number is present, then that compatible library will be used instead.
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
 */
public class NativeLibraryManager {

   private static String SEP = File.separator;
   private static String PSEP = File.pathSeparator;

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
               return new SystemType[] {MacOS64};
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
   File[] myExistingLibs;
   SystemType mySystemType;
   FileTransferListener myTransferListener;

   Pattern myVersionStrPattern;

   // exception (if any) from most recent grab attempt:
   Exception myGrabException; 
   String myGrabErrorMessage;

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
      "http://"+myRemoteServer+"/files/lib";

   /**
    * Parses an integer starting from location i in str.
    */
   private int parseInt (String str, int i) {
      int value = -1;
      int len = str.length();
      int c;
      if (i < len && Character.isDigit ((c=str.charAt(i)))) {
         value = c-'0';
         i++;
         while (i < len && Character.isDigit ((c=str.charAt(i)))) {
            value = value*10 + c-'0';
            i++;
         }
      }
      return value;         
   }

   /**
    * Parses the major version number from a version string of the form
    * ".A.B.C ... ", where A, B, C, etc. are positive integers. The major
    * version number is A.
    */
   private int parseMajorNum (String version) {
      return parseInt (version, 1);
   }

   /**
    * Parses the minor version number from a version string of the form
    * ".A.B.C ... ", where A, B, C, etc. are positive integers. The minor
    * version number is B. If there is no minor version number (i.e.,
    * the string has the form ".A", then -1 is returned.
    */
   private int parseMinorNum (String version) {
      int secondDot = version.indexOf ('.', 1);
      if (secondDot != -1) {
         return parseInt (version, secondDot+1);
      }
      else {
         return -1;
      }
   }

   LibDesc createDesc (String libName) {
      return new LibDesc (libName);
   }         

   /**
    * Descriptor for a library name, which breaks out its basename, version
    * string, major and minor version numbers (if any), and system type (if
    * any).
    */
   class LibDesc {
      SystemType mySys; // General system type associated with the library
      String myBasename;    // Library basename
      String myVersionStr;  // version string, or "" if none
      int myMajorNum;       // major version number, or -1 if none
      int myMinorNum;       // minor version number, or -1 if none
      
      public LibDesc (String libName) {
         set (libName);
      }

      private String setVersionInfo (String str) {
         Matcher m = myVersionStrPattern.matcher (str);
         if (m.matches()) {
            myVersionStr = m.group(2);
            myMajorNum = parseMajorNum (myVersionStr);
            myMinorNum = parseMinorNum (myVersionStr);
            return m.group(1);
         }
         else {
            myVersionStr = "";
            myMajorNum = -1;
            myMinorNum = -1;
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
            myMajorNum = -1;
            myMinorNum = -1;
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

      private int matchMajorMinorVersionStr (String vstr) {
         Matcher m = myVersionStrPattern.matcher (vstr);
         if (!m.matches() || !m.group(1).equals("")) {
            return -1;
         }
         if (parseMajorNum (vstr) != myMajorNum) {
            return -1;
         }
         return parseMinorNum (vstr);
      }

      /**
       * Checks to see if the indicated fileName is compatible with the
       * basename and major and minor version numbers for this library. If the
       * filename has both major and minor version numbers, and the basename
       * and major number matches this library, then the minor number is
       * returned. Otherwise, -1 is returned, indicating no match.
       */
      public int majorNameMatches (String fileName, SystemType sysType) {
         if (myMinorNum == -1) {
            return -1;
         }
         switch (sysType) {
            case MacOS64: {
               if (!fileName.startsWith ("lib"+myBasename)) {
                  return -1;
               }
               if (!fileName.endsWith (".dylib")) {
                  return -1;
               }
               return matchMajorMinorVersionStr (
                  fileName.substring(myBasename.length()+3, fileName.length()-6));
            }
            case Linux32:
            case Linux64: {
               if (!fileName.startsWith ("lib"+myBasename+".so")) {
                  return -1;
               }
               return matchMajorMinorVersionStr (
                  fileName.substring (myBasename.length()+6));
            }
            case Windows32:
            case Windows64: {
               if (!fileName.startsWith (myBasename)) {
                  return -1;
               }
               if (!fileName.endsWith (".dll")) {
                  return -1;
               }
               return matchMajorMinorVersionStr (
                  fileName.substring (myBasename.length(), fileName.length()-4));
            }
            default: {
               throw new NativeLibraryException (
                  "Unimplemented system type: "+sysType);
            }
         }            
      }

      /**
       * Returns the actual library file name appropriate to the current
       * system.
       */
      public String getFileName (SystemType sysType) {
         StringBuilder builder = new StringBuilder();
         switch (sysType) {
            case MacOS64: {
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
         return SystemType.MacOS64;
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
      myExistingLibs = myLibDir.listFiles();
   }

   void setDefaultLibDir () {
      // look for a default library directory in ../../../lib
      myLibDir = null;
      try {
         String baseDir =
            PathFinder.expand ("${srcdir NativeLibraryManager}/../../../lib");
         File libDir = new File (baseDir, getNativeDirectoryName());
         if (libDir.exists() && libDir.isDirectory()) {
            myLibDir = libDir;
            myExistingLibs = myLibDir.listFiles();
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

   File findMoreRecent (LibDesc desc) {

      if (desc.myMinorNum >= 0) {
         int superNum = -1;
         File superFile = null;

         for (int i=0; i<myExistingLibs.length; i++) {
            File libFile = myExistingLibs[i];
            String libFileName = libFile.getName();
            int mnum = desc.majorNameMatches (libFileName, mySystemType);
            if (mnum > desc.myMinorNum) {
               if (superNum == -1 || mnum < superNum) {
                  superNum = mnum;
                  superFile = libFile;
               }
            }
         }
         return superFile;
      }
      else {
         return null;
      }
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
      return null;
   }

   void grabFile (File libFile, LibDesc desc, boolean checkHash) {
      
      FileManager Manager = new FileManager();
      Manager.setVerbosityLevel (LogLevel.ALL);
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
    * path. If it is not possible to locate the library or download it, then
    * <code>null</code> is returned, and <code>myGrabException</code> can be
    * queried for any exception that occured during an attempted download.
    */
   String getLibraryPath (LibDesc desc) {
      
      // check that the file exists in the load library dir

      myGrabException = null;
      String libPath = null;
      if (myLibDir != null) {
         // look for the file in the specified lib directiry
         File libFile = new File (myLibDir, getFileName(desc));
         if (!libFile.exists()) {
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
                     "NativeLibraryManager: failed to download "+libFile+":");
                  System.out.println (myGrabException.getMessage());
               }
               else {
                  System.out.println (
                     "NativeLibraryManager: downloaded "+libFile+":");
               }
            }
            if (libPath == null) {
               File altFile = findMoreRecent (desc);
               if (altFile != null) {
                  if ((myFlags & VERBOSE) != 0) {
                     System.out.println (
                        "NativeLibraryManager: using "+altFile+
                        " in place of "+libFile);
                  }
                  libPath = altFile.toString();
               }
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
                        "NativeLibraryManager: failed to update "+libFile+":");
                     System.out.println (myGrabException.getMessage());
                  }
                  else {
                     System.out.println (
                        "NativeLibraryManager: updated "+libFile+":");
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
    * @param libName name of the library.
    * @throws maspack.fileutil.NativeLibraryException if the required library
    * is not present and cannot be obtained.
    */
   public static void verify (String libName) {
      if (myManager == null) {
         myManager = new NativeLibraryManager ();
      }
      LibDesc desc = myManager.createDesc (libName);
      if (desc.matchesSystem(myManager.mySystemType)) {
         if (myManager.getLibraryPath (desc) == null) {
            Exception grabEx = myManager.myGrabException;
            NativeLibraryException e =
               new NativeLibraryException (
                  "Could not download library "+getPathName(libName)+": "+
                  grabEx.getMessage());
            e.initCause (grabEx);
            throw e;
         }
      }
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
    * Loads a Java native library specified by libName. This is a wrapper for
    * <code>System.load()</code> that first attempts to locate the library
    * file, if necessary downloading or updating it from the server. See the
    * discussion in the class header for more information about how the library
    * name is formatted.
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
         String libPath = myManager.getLibraryPath(desc);
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
            //return "Darwin-x86_64";
         }
         default: {
            throw new NativeLibraryException (
               "Unimplemented system type: "+mySystemType);
         }
      }
   }

   public static void main (String[] args) {
 
      try {
         NativeLibraryManager man = new NativeLibraryManager();
         BufferedReader reader =
            new BufferedReader (new InputStreamReader (System.in));
         while (true) {
            String str = reader.readLine();
            LibDesc desc = null;
            try {
               desc = man.createDesc (str);
            }
            catch (Exception e) {
               e.printStackTrace(); 
            }
            if (desc != null) {
               System.out.println ("base=" + desc.myBasename);
               System.out.println ("sys=" + desc.mySys);
               System.out.println ("major=" + desc.myMajorNum);
               System.out.println ("minor=" + desc.myMinorNum);
               System.out.println ("vers=" + desc.myVersionStr);
            }
         }
      }
      catch (Exception e) {
      }
   }
}
