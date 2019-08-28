/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.UserAuthenticator;

import maspack.crypt.Hasher;
import maspack.fileutil.jsch.SimpleIdentityRepository;
import maspack.fileutil.uri.URIx;
import maspack.fileutil.uri.URIxMatcher;
import maspack.fileutil.uri.URIxScheme;
import maspack.fileutil.uri.URIxSyntaxException;
import maspack.util.Logger;
import maspack.util.Logger.LogLevel;
import maspack.util.StreamLogger;

/**
 * Downloads files from URIs satisfying the generic/zip URI syntax according to
 * specifications RFC 3986 and proposed jar/zip extension.
 * &lt;https://www.iana.org/assignments/uri-schemes/prov/jar&gt;<br>
 * <br>
 * 
 * A typical use will look like this:
 * 
 * <pre>
 * <code> FileManager Manager = new FileManager("data", "http://www.fileserver.com");
 * Manager.get("localFile", "remoteFile");</code>
 * </pre>
 * 
 * By default, the FileManager will simply return the localFile if it exists. To
 * force updates from a remote location, you can set options:
 * 
 * <pre>
 * <code> int options = FileManager.CHECK_HASH;
 * Manager.get("localFile", "remoteFile", options);</code>
 * </pre>
 * 
 * The CHECK_HASH flag downloads the remote file if its sha1 hash differs from
 * that of your local copy. There is also a FORCE_REMOTE flag that forces the
 * remote file to be downloaded always.<br>
 * <br>
 * 
 * For fine control and catching errors, the workflow is slightly different:
 * 
 * <pre>
 * <code> boolean download = true;
 * File dest = new File("localFile");
 * URI source = new URI("remoteFile");
 * File local = Manager.getLocal(dest);
 * 
 * if (local != null) {
 *    boolean match = false;
 *    try {
 *       match = Manager.equalsHash(dest, source);
 *    } catch (FileTransferException e) {
 *       System.out.println(e.getMessage());
 *    }
 *    download = !match;
 * }
 * 
 * if (download) {
 *    try {
 *       local = Manager.getRemote(dest, source);
 *    } catch (FileTransferException e) {
 *       System.out.println(e.getMessage());
 *    }
 * } 
 * 
 * if (local == null) {
 *    throw new RuntimeException("Unable to get file.");
 * } </code>
 * </pre>
 * 
 * See <a href=http://commons.apache.org/vfs/filesystems.html>VFS2 File
 * Systems</a> for further details regarding supported remote filesystems and
 * URI syntax.<br>
 * <br>
 * 
 * @author "Antonio Sanchez" Creation date: 13 Nov 2012
 * 
 */
public class FileManager {

   private FileTransferMonitor myTransferMonitor = null; // monitors transfers
   private FileTransferListener
   myDefaultConsoleListener = null; // default console listener
   private static FileManager staticManager = null; // used for static grabs
   private FileCacher cacher; // actually downloads files
   private static Logger logger; // used for logging messages

   // directories
   private File downloadDir; // default directory to save files to
   private URIx remoteSource; // default base for resolving relative URIs

   public ArrayList<FileTransferException> exceptionStack;

   // options
   /**
    * Always read from remote if possible
    */
   public static final int FORCE_REMOTE = 0x01;

   /**
    * Check file hashes, and if different, get from remote
    */
   public static final int CHECK_HASH = 0x02;

   /**
    * If file is in a remote zip file, get a local copy
    * of the entire zip file first
    */
   public static final int DOWNLOAD_ZIP = 0x10;

   // public static final int CHECK_DATE_SIZE = 0x08;

   // defaults
   public static int DEFAULT_OPTIONS = 0;
   public static LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO; // info and up
   public static Logger DEFAULT_LOGGER = new StreamLogger();

   public int myOptions = DEFAULT_OPTIONS;

   File lastFile = null;
   boolean lastWasRemote = false;


   // initialiazes some objects
   private void init() {
      cacher = new FileCacher();
      logger = DEFAULT_LOGGER;
      exceptionStack = new ArrayList<>();
      setVerbosityLevel(DEFAULT_LOG_LEVEL);
   }

   /**
    * Default constructor, sets the local directory to the current path, and
    * sets the default URI to be empty.
    */
   public FileManager () {
      init();
      File currDir = new File(""); // use current directory
      setDownloadDir(currDir);
      setRemoteSource((URIx)null);
   }

   /**
    * Sets default paths
    * 
    * @param downloadDir
    * the local path to save files to
    * @param remoteSource
    * the remote base URI to download files from
    */
   public FileManager (File downloadDir, URIx remoteSource) {
      init();
      setDownloadDir(downloadDir);
      setRemoteSource(remoteSource);
   }

   /**
    * Sets default download directory, leaves source as null
    * 
    * @param downloadDir
    * the local path to save files to
    */
   public FileManager (File downloadDir) {
      init();
      setDownloadDir(downloadDir);
      setRemoteSource((URIx)null);
   }

   /**
    * Sets local download path and remote URI source by parsing the supplied
    * strings
    * 
    * @param downloadPath
    * the local path to save files to
    * @param remoteSourceName
    * the remote base URI
    */
   public FileManager (String downloadPath, String remoteSourceName) {
      init();
      setRemoteSource(remoteSourceName);
      setDownloadDir(downloadPath);
   }

   /**
    * Sets directory where files are downloaded
    * 
    * @param dir
    * default download directory
    */
   public void setDownloadDir(File dir) {
      if (dir != null) {
         downloadDir = new File(dir.getAbsolutePath());
      } else {
         downloadDir = new File("");
      }
   }

   /**
    * Sets directory where files are downloaded
    * 
    * @param path
    * default download directory
    */
   public void setDownloadDir(String path) {
      downloadDir = new File(path);
   }

   /**
    * @return the default download directory
    */
   public File getDownloadDir() {
      return downloadDir;
   }

   /**
    * Sets the base URI for remote files, this is attached to any relative URIs
    * provided in the get(...) methods
    * 
    * @param uri base URI for remote files
    */
   public void setRemoteSource(URIx uri) {
      
      if (uri == null) {
         remoteSource = null;
      } else if (uri.isRelative()) {
         // assume File with current location as root
         String currPath = (new File("")).getAbsolutePath();
         remoteSource = URIx.resolve(new URIx(URIxScheme.FILE, null, currPath), uri);
      } else {
         remoteSource = new URIx(uri);
      }
   }

   /**
    * Sets the base URI for remote files, this is attached to any relative URIs
    * provided in the get(...) methods
    * 
    * @param uriStr base URI for remote files
    * @throws URIxSyntaxException if uriStr is malformed
    */
   public void setRemoteSource(String uriStr) throws URIxSyntaxException {

      if (uriStr == null) {
         remoteSource = null;
         return;
      }

      URIx remote = new URIx(uriStr);
      setRemoteSource(remote);

   }

   /**
    * @return the default URI source location for resolving relative URIs
    */
   public URIx getRemoteSource() {
      return remoteSource;
   }

   public List<? extends Exception> getExceptions() {
      return exceptionStack;
   }
   
   public Exception getLastException() {
      if (exceptionStack.isEmpty()) {
         return null;
      }
      return exceptionStack.get(exceptionStack.size()-1);
   }

   public void clearExceptions() {
      exceptionStack.clear();
   }

   public boolean hasExceptions() {
      return (exceptionStack.size() > 0);
   }

   /**
    * Converts a relative URI to an absolute one, using the remoteSource as a
    * base. If the supplied URI string is absolute, the corresponding URI object
    * is returned.
    * 
    * @param relURIstr
    * the relative URI
    * @throws URIxSyntaxException
    * if URI string is malformed
    * @return converted URI
    */
   public URIx getAbsoluteSourceURI(String relURIstr) throws URIxSyntaxException {

      URIx uri = new URIx(relURIstr);
      return getAbsoluteSourceURI(uri);

   }

   /**
    * Converts a relative URI to an absolute one, using the remote source as a
    * base. If the supplied URI is absolute, this is returned.
    * 
    * @param relURI relative URI
    * @return converted URI
    */
   public URIx getAbsoluteSourceURI(URIx relURI) {

      URIx uri = relURI;
      if (uri.isRelative()) {
         uri = URIx.resolve(remoteSource, uri);
      }
      return uri;

   }

   /**
    * Converts a relative file to an absolute one using the object's download
    * directory. If the supplied file is absolute, this is returned.
    * 
    * @param relFile
    * the relative file
    * @return absolute file
    */
   public File getAbsoluteDestFile(File relFile) {
      File file = relFile;
      if (!file.isAbsolute()) {
         file = new File(downloadDir.getAbsoluteFile(), relFile.getPath());
      }
      return file;
   }

   /**
    * Converts a relative file to an absolute one using the object's download
    * directory. If the supplied file is absolute, this is returned.
    * 
    * @param relPath relative file
    * @return absolute file
    */
   public File getAbsoluteDestFile(String relPath) {
      return getAbsoluteDestFile(new File(relPath));
   }


   // adds an extension onto a uri
   private static URIx mergeExtension(URIx base, String extension) {

      URIx merged = new URIx(base);

      if (merged.isZipType()) {
         String fn = base.getFragment() + extension;
         merged.setFragment(fn);
      } else {
         String fn = base.getRawPath() + extension;
         merged.setPath(fn);
      }

      return merged;

   }

   // make first character l
   private static String uncap(String word) {
      char chars[] = word.toCharArray();
      chars[0] = Character.toLowerCase(chars[0]);
      return new String( chars );
   }

   /**
    * Fetches a hash file from a remote location. The hash file is assumed to
    * have the form &lt;uri&gt;.sha1
    * 
    * @param uriStr
    * the URI of the file to obtain the hash
    * @return the hex-encoded hash value string
    * @throws FileTransferException if cannot retrieve remote hash
    * @throws URIxSyntaxException if uriStr is malformed
    */
   public String getRemoteHash(String uriStr) throws FileTransferException, URIxSyntaxException {

      URIx uri = getAbsoluteSourceURI(uriStr);
      return getRemoteHash(uri);

   }


   /**
    * Fetches a hash file from a remote location. The hash file is assumed to
    * have the form &lt;uri&gt;.sha1
    * 
    * @param uri
    * the URI of the file to obtain the hash
    * @return the hex-encoded hash value string
    */
   public String getRemoteHash(URIx uri) throws FileTransferException {

      uri = getAbsoluteSourceURI(uri);

      // construct a uri for the remote sha1 hash
      URIx hashURI = mergeExtension(uri, ".sha1");

      // create input stream and download hash
      InputStream in = null;
      String sha1 = null;
      try {
         cacher.initialize();
      } catch (FileSystemException e) {
         cacher.release();
         throw new FileTransferException("Failed to initialize FileCacher", e);
      }

      try {
         in = cacher.getInputStream(hashURI);

         byte[] hash = new byte[40];
         in.read(hash, 0, hash.length);
         sha1 = new String(hash);

      } catch (FileSystemException e) {
         String msg = decodeVFSMessage(e);
         throw new FileTransferException("Cannot obtain remote hash: " + uncap(msg), e);
      } catch (IOException e) {
         throw new FileTransferException("Cannot read hash from input stream: " + uncap(e.getMessage()), e);
      } finally {
         closeQuietly(in);
         cacher.release();
      }

      return sha1;
   }

   // close an input stream without throwing an error
   private static void closeQuietly(InputStream stream) {
      // force close
      if (stream != null) {
         try {
            stream.close();
         } catch (IOException ignore) {
         }
      }
   }


   /**
    * Gets the sha1 hash of a local file
    * 
    * @param fileName file to compute the hash of
    * @return the 20-byte hash as a hex-encoded String
    * @throws FileTransferException if fails to generate hash of local file
    */
   public String getLocalHash(String fileName) {
      return getLocalHash(new File(fileName));
   }

   /**
    * Gets the sha1 hash of a local file
    * 
    * @param file
    * to compute the hash of
    * @return the 20-byte hash as a hex-encoded String
    * @throws FileTransferException if fails to generate hash of local file
    */
   public String getLocalHash(File file) {

      file = getAbsoluteDestFile(file);

      if (!file.canRead()) {
         throw new FileTransferException("Cannot compute hash of local file: " 
         + file.getPath() + " does not exist");
      } else if (file.isDirectory()) {
         throw new FileTransferException("Cannot compute hash of local file: " 
         + file.getPath() + " is a directory");
      }


      String hash = null;
      try {
         hash = Hasher.sha1(file);
      } catch (IOException e) {
         throw new FileTransferException("Failed to read local file " + file.getPath(), e);
      }

      return hash;
   }

   /**
    * Compares sha1 hash values between a local file and remote URI
    * 
    * @param file
    * local file of which to compute hash
    * @param uri
    * remote file of which to determine hash
    * @return true if hashes are equal, false otherwise
    * @throws FileTransferException
    * if can't get either hash
    */
   public boolean equalsHash(File file, URIx uri) throws FileTransferException {

      String localHash = getLocalHash(file);
      String remoteHash = getRemoteHash(uri);

      if (localHash == null || remoteHash == null) {
         return false;
      }

      return localHash.equalsIgnoreCase(remoteHash);

   }

   public boolean equalsHash(String relPath) throws FileTransferException {

      File localFile = getAbsoluteDestFile(relPath);
      URIx localURI = getAbsoluteSourceURI(relPath);

      return equalsHash(localFile, localURI);

   }
   
   public boolean equalsHash(String local, String remote) throws FileTransferException {

      File localFile = getAbsoluteDestFile(local);
      URIx localURI = getAbsoluteSourceURI(remote);

      return equalsHash(localFile, localURI);

   }

   // extracts file name from URI
   private String extractFileName(URIx uri) {
      String fileName = null;
      if (uri == null) {
         return null;
      } else if (uri.isZipType()) {
         fileName = uri.getRawFragment();
      } else {
         fileName =  uri.getRawPath();
      }

      // get final part of path
      int idx = fileName.lastIndexOf('/'); // this is the only separator in uri's
      if (idx >= 0) {
         if (idx == fileName.length()-1) {
            int lidx = idx;
            idx = fileName.lastIndexOf('/', lidx-1);
            if (idx >= 0) {
               fileName = fileName.substring(idx, lidx);
            } else {
               fileName = fileName.substring(0, lidx);
            }
         } else {
            fileName = fileName.substring(idx+1);
         }
      }

      return fileName;

   }

   /**
    * Retrieves a local file if it exists, null otherwise. If the file
    * path is relative, then it prepends the download directory.
    * 
    * @param file path for the local file
    * @return the file handle
    */
   public File getLocal(File file) {

      if (!file.isAbsolute()) {
         file = new File(downloadDir, file.getPath());
      }
      if (!file.exists()) {
         return null;
      }
      lastFile = file;
      lastWasRemote = false;
      return file;
   }

   /**
    * Retrieves a local file if it exists, null otherwise.
    * 
    * @param fileName local file name
    * @return File handle
    */
   public File getLocal(String fileName) {
      File file = new File(fileName);
      return getLocal(file);
   }

   /**
    * Retrieves a remote file if it exists, null otherwise. If dest is null or a
    * directory, appends source filename
    * 
    * @param dest
    * the destination file (local)
    * @param source
    * the source URI
    * @return a File reference to the new local copy
    * @throws FileTransferException
    * if downloading the remote file fails
    */
   public File getRemote(File dest, URIx source) throws FileTransferException {

      if (dest == null) {    
         // if source is relative, take that
         if (source.isRelative()) {
            dest = new File(source.getRawPath());
         } else {
            // otherwise, simply extract the file name from source
            dest = new File(extractFileName(source));
         }
      } else if (dest.isDirectory()) {
         String srcFile = extractFileName(source);
         if (source.isRelative()) {            
            srcFile = source.getRawPath();
         }
         dest = new File(dest,srcFile);

      }

      // make absolute
      dest = getAbsoluteDestFile(dest);
      source = getAbsoluteSourceURI(source);

      try {
         cacher.initialize();
         logger.debug("Downloading file " + source.toString() + " to "
         + dest.getAbsolutePath() + "...");
         // download file
         cacher.cache(source, dest, myTransferMonitor);
      } catch (FileSystemException e) {

         String msg = decodeVFSMessage(e);
         throw new FileTransferException(msg, e);

      } finally {
         cacher.release();
      }

      lastFile = dest;
      lastWasRemote = true;
      return dest;

   }

   /**
    * Uploads a local file to the remote destination if it exists. If dest is null or a
    * directory, appends source filename
    * 
    * @param source
    * the source file
    * @param dest
    * the destination uri 
    * @throws FileTransferException
    * if uploading the file fails
    */
   public void putRemote(File source, URIx dest) throws FileTransferException {

      // ensure that source is a file, and not a directory
      if (source.isDirectory() ) { //|| srcFile.endsWith("/")) {
         throw new IllegalArgumentException("Source file must refer to a file: <" + source + ">"); 
      }

      if (dest == null) {    
         // if source is relative, take that
         if (!source.isAbsolute()) {
            dest = new URIx(source.getPath());
         } else {
            // otherwise, simply extract the file name from source
            dest = new URIx(source.getName());
         }
      }

      // make absolute
      dest = getAbsoluteSourceURI(dest);
      source = getAbsoluteDestFile(source);

      try {
         cacher.initialize();
         logger.debug("Uploading file " + source.getAbsolutePath() + " to "
         + dest.toString() + "...");
         // download file
         cacher.copy(source, dest, myTransferMonitor);
      } catch (FileSystemException e) {
         String msg = decodeVFSMessage(e);
         throw new FileTransferException(msg, e);
      } finally {
         cacher.release();
      }

   }

   private static Throwable getRootThrowable(Throwable t) {

      Throwable root = t;
      while (root.getCause() != null && root.getCause() != root) {
         root = root.getCause();
      }
      return root;

   }

   /**
    * Retrieves a remote file if it exists, null otherwise. If dest is null, or
    * a directory, appends source filename
    * 
    * @param destName
    * the destination file (local)
    * @param sourceName
    * the source URI
    * @return a File reference to the new local copy
    * @throws URIxSyntaxException if the source URI is malformed
    * @throws FileTransferException if grabbing the remote file fails
    */
   public File getRemote(String destName, String sourceName)
   throws FileTransferException, URIxSyntaxException {

      URIx remote = new URIx(sourceName);

      File localFile = null;
      if (destName != null) {
         localFile = new File(destName);
      }
      return getRemote(localFile, remote);

   }

   /**
    * Same as {@link #getRemote(String, String)} with dest=null.
    *
    * @param sourceName
    * the source URI
    */
   public File getRemote(String sourceName) throws FileTransferException {
      return getRemote(null, sourceName);
   }

   /**
    * Same as {@link #getRemote(File, URIx)} with dest=null.
    *
    * @param source
    * the source URI
    */
   public File getRemote(URIx source) throws FileTransferException {
      return getRemote(null, source);
   }
   
   public String determineDefaultDestination(URIx uri) {
      String path;
      
      if (!uri.isZipType ()) {
         // get path directly
         path = uri.getRawPath ();
      } else {
         // get zip fragment path
         path = uri.getRawFragment ();
         if (path.startsWith ("/")) {
            path = path.substring (1);
         }
         if (path.isEmpty ()) {
            path = "./";
         }
         
         // check if we need to prepend with base
         URIx base = uri.getNestedURI ();
         if (base != null) {
            // append to base
            URIx tailURI = new URIx();
            tailURI.setRawPath (path);
            
            // get head path
            String head = determineDefaultDestination(base);
            URIx headURI = new URIx(head);
            
            // merge two, in same path as underlying zip file
            URIx merged = headURI.resolve (tailURI);
            
            // extract path
            path = merged.getRawPath ();
         }
         
      }
      
      return path;
   }

   /**
    * Returns a file handle to a local version of the requested file. Downloads
    * from URI if required (according to options). Works with absolute paths and
    * source URIs, otherwise combines path and source URI with downloadDir and
    * remoteSource, respectively. If the destination is null or a directory,
    * then the filename of source is appended.
    * 
    * If there is any internal problem, (such as failing to obtain a hash, or
    * failing to download a file), the function will log the error message and
    * continue.
    * 
    * @param dest
    * the local path (relative or absolute) to download file to
    * @param source
    * the remote URI to cache
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @return File handle to local file
    * @throws FileTransferException only if there is no local copy of the file 
    * at the end of the function call
    */
   public File get(File dest, URIx source, int options)
      throws FileTransferException {

      File olddest = dest;
      
      // default destination if none provided
      if (dest == null) {
         if (source.isRelative()) {
            dest = new File(determineDefaultDestination (source));
         } else {
            dest = new File(extractFileName(source));
         }
      } else if (dest.isDirectory()) {
         if (source.isRelative()) {
            // XXX should I just use filename?
            dest = new File(dest, determineDefaultDestination (source));
         } else {
            dest = new File(dest, extractFileName(source));
         }
      }

      // download zip file first if requested
      if ( source.isZipType() && (options & DOWNLOAD_ZIP) != 0) {

         // get zip file
         URIx zipSource = source.getBaseURI();
         
         File zipDest;
         if (olddest == null) {
            if (zipSource.isRelative()) {
               zipDest = new File(determineDefaultDestination (zipSource));
            } else {
               zipDest = new File(extractFileName(zipSource));
            }
         } else {
            if (olddest.isDirectory ()) {
               // XXX should I just use the filename?
               zipDest = new File(extractFileName(zipSource));
            } else {
               zipDest = new File(olddest.getParentFile (), extractFileName(zipSource));
            }
         }
         
         File zipFile = get(zipDest, zipSource, options);

         // replace source URI
         source.setBaseURI(new URIx(zipFile));

         // XXX no longer need to check hash, since zip's hash would
         // have changed, although we do need to replace if re-downloaded zip
         // options = options & (~CHECK_HASH);
      }
      
      // convert to absolute
      dest = getAbsoluteDestFile(dest);
      source = getAbsoluteSourceURI(source);

      // check if we need to actually fetch file
      boolean fetch = true;
      if ((options & FORCE_REMOTE) == 0) {

         // check if file exists
         if (dest.canRead()) {

            // check hash if options say so
            if ((options & CHECK_HASH) != 0) {
               try {
                  fetch = !equalsHash(dest, source);
                  if (fetch) {
                     logger.debug("Hash matches");
                  }
               } catch (FileTransferException e) {
                  logger.debug(
                     "Cannot obtain hash, assuming it doesn't match, "
                     + e.getMessage());
                  exceptionStack.add(e);
                  fetch = true;
               }
            } else {
               // file exists, so let it be
               fetch = false;
            }
         }
      }

      // download file if we need to
      if (fetch) {
         try {
            dest = getRemote(dest, source);
         } catch (FileTransferException e) {
            String msg = "Failed to fetch remote file <" + source + ">";
            logger.error(msg +", " +e.getMessage());
            exceptionStack.add(e);
         }
      } else {
         logger.debug("File '" + dest
            + "' exists and does not need to be cached.");
      }

      // at this point, we should have a file unless download failed 
      // and we have no local copy
      if (dest == null || !dest.exists()) {
         String msg = "Unable to find or create file " + dest + " <" 
         + source.toString() + ">";
         throw new FileTransferException(msg);
      }
      lastFile = dest;
      lastWasRemote = fetch;
      return dest; // return file

   }

   /**
    * Downloads a file using the default options.
    *
    * @param dest
    * the local path (relative or absolute) to download file to
    * @param source
    * the remote URI to cache
    * @return File handle to local file
    * @throws FileTransferException only if there is no local copy of the file 
    * at the end of the function call
    * @see #get(File, URIx, int)
    */
   public File get(File dest, URIx source) throws FileTransferException {
      return get(dest, source, myOptions);
   }

   /**
    * Downloads a file using same relative path for source and destination.
    *
    * @param sourceDestName source and destination path name
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @return File handle to local file
    * @throws FileTransferException only if there is no local copy of the file 
    * at the end of the function call
    * @see #get(String, String, int)
    */
   public File get(String sourceDestName, int options)
      throws FileTransferException {
      return get(null, sourceDestName, options);

   }

   /**
    * Downloads a file using same relative path for source and destination
    * and default options
    * @see #get(String, String, int)
    */
   public File get(String sourceName) throws FileTransferException {
      return get(null, sourceName, myOptions);
   }
   
   /**
    * Downloads a file from an absolute or relative URI, with default options
    * @see #get(File, URIx, int)
    **/
   public File get(URIx source) throws FileTransferException {
      return get(null, source, myOptions);
   }
   
   /**
    * Checks for existence of file
    * @param source URI to check for existence
    * @return true if the resource exists, false otherwise
    */
   public boolean fileExists(URIx source) {
      source = getAbsoluteSourceURI(source);  // convert to absolute
      boolean exists = true;
      try {
         cacher.initialize();
         logger.debug("checking for file " + source.toString());
         // download file
         exists = cacher.exists (source);
      } catch (FileSystemException e) {

         String msg = decodeVFSMessage(e);
         logger.debug ("failed to check for file: " + msg);
         exists = false;
      } finally {
         cacher.release();
      }
      
      return exists;
   }

   /**
    * Converts the supplied destination path and source URI to a File and URI
    * object, respectively, and downloads the remote file according to the 
    * supplied options.
    *
    * @param destName destination path
    * @param sourceName source URI (as a string)
    * @return File handle to local file
    * @throws URIxSyntaxException if the sourceName is malformed
    * @throws FileTransferException if download fails.
    * @see #get(File, URIx, int)
    */
   public File get(String destName, String sourceName, int options)
   throws URIxSyntaxException, FileTransferException {

      // try to make URI from sourceName
      URIx source = new URIx(sourceName);

      File dest = null;
      if (destName != null) {
         dest = new File(destName);
      }

      return get(dest, source, options);

   }

   /**
    * Downloads a file with default options
    *
    * @param destName destination path
    * @param sourceName source URI (as a string)
    * @return File handle to local file
    * @throws URIxSyntaxException if the sourceName is malformed
    * @throws FileTransferException if download fails.
    * @see #get(String, String, int)
    */
   public File get(String destName, String sourceName)
   throws FileTransferException {
      return get(destName, sourceName, myOptions);
   }

   /**
    * Uploads a file, according to options. Works with absolute paths and
    * destination URIs, otherwise combines path and dest URI with downloadDir and
    * remoteSource, respectively. If the destination is null or a directory,
    * then the filename of source is appended.
    * 
    * If there is any internal problem, (such as failing to obtain a hash, or
    * failing to download a file), the function will log the error message and
    * continue.
    * 
    * @param source
    * the source file to upload
    * @param dest
    * the remote URI to upload to
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @throws FileTransferException if the upload fails
    */
   public void put(File source, URIx dest, int options)
      throws FileTransferException {

      // default destination if none provided
      if (dest == null) {
         if (!source.isAbsolute()) {
            dest = new URIx(source.getPath());
         } else {
            dest = new URIx(source.getName());
         }
      }

      // convert to absolute
      dest = getAbsoluteSourceURI(dest);
      source = getAbsoluteDestFile(source);

      // XXX TODO: check if we need to actually upload file
      boolean push = true;

      //      if ((options & FORCE_REMOTE) == 0) {
      //         // check if file exists
      //         if (dest.canRead()) {
      //
      //            // check hash if options say so
      //            if ((options & CHECK_HASH) != 0) {
      //               try {
      //                  fetch = !equalsHash(dest, source);
      //                  if (fetch) {
      //                     logger.debug("Hash matches");
      //                  }
      //               } catch (FileTransferException e) {
      //                  logger.debug(
      //                     "Cannot obtain hash, assuming it doesn't match, "
      //                     + e.getMessage());
      //                  exceptionStack.add(e);
      //                  fetch = true;
      //               }
      //            } else {
      //               // file exists, so let it be
      //               fetch = false;
      //            }
      //         }
      //      }

      // download file if we need to
      if (push) {
         putRemote(source, dest);
      } else {
         logger.debug("File '" + dest
            + "' exists and does not need to be uploaded.");
      }

   }

   /**
    * Uploads a file using the default options.
    *
    * @param source
    * the source file to upload
    * @param dest
    * the remote URI to upload to
    * @throws FileTransferException if the upload fails
    * @see #put(File, URIx, int)
    */
   public void put(File source, URIx dest) throws FileTransferException {
      put(source, dest, myOptions);
   }

   /**
    * Uploads a file using same relative path for source and destination
    *
    * @param sourceDestName source and destination path name
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @throws FileTransferException if the upload fails
    * @see #put(String, String, int)
    */
   public void put(String sourceDestName, int options)
      throws FileTransferException {
      put(sourceDestName, null, options);

   }

   /**
    * Uploads a file using same relative path for source and destination
    * and default options.
    * 
    * @param sourceDestName source and destination path name
    * @throws FileTransferException if the upload fails
    * @see #put(String, String, int)
    */
   public void put(String sourceDestName) throws FileTransferException {
      put(sourceDestName, null, myOptions);
   }

   /**
    * Converts the supplied source path and dest URI to a File and URI object,
    * respectively, and uploads the remote file according to the supplied
    * options.
    *
    * @param sourceName
    * source path
    * @param destName
    * remote URI to upload to (as a string)
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @throws URIxSyntaxException if the dest is malformed
    * @throws FileTransferException if upload fails.
    * @see #put(File, URIx, int)
    */
   public void put(String sourceName, String destName, int options)
   throws URIxSyntaxException, FileTransferException {

      // try to make URI from sourceName
      URIx dst = null;
      if (destName != null) {
         dst = new URIx(destName);
      }
      
      File src = new File(sourceName);
      put(src, dst, options);
   }

   /**
    * Downloads a file with default options.
    *
    * @param sourceName
    * source path
    * @param destName
    * remote URI to upload to (as a string)
    * @throws URIxSyntaxException if the dest is malformed
    * @throws FileTransferException if upload fails.
    * @see #put(String, String, int)
    */
   public void put(String sourceName, String destName)
      throws FileTransferException {
      put(sourceName, destName, myOptions);
   }

   /**
    * Sets the logger for printing messages, defaults
    * to printing to console.
    * 
    * @param log message logger
    */
   public static void setLogger(Logger log) {
      logger = log;
   }

   /**
    * Gets the logger for printing message
    * 
    * @return message logger
    */
   public static Logger getLogger() {
      return logger;
   }

   /**
    * Sets the verbosity level of this FileManager. Any messages ranked higher
    * than the supplied "level" will be printed.
    * 
    * @param level
    * The minimum message level to print, TRACE printing everything, NONE
    * printing nothing.
    */
   public void setVerbosityLevel(LogLevel level) {
      logger.setLogLevel(level);
   }

   /**
    * Enables or disables default printing of file transfer progress to the
    * console. Disabled by default. Progress printing is done by installing a
    * special transfer listener, which will not be returned by {@link
    * #getTransferListeners}.
    *
    * @param enable enables or disables console progress printing
    */
   public void setConsoleProgressPrinting (boolean enable) {
      if (enable != (myDefaultConsoleListener != null)) {
         if (enable) {
            myDefaultConsoleListener = new DefaultConsoleFileTransferListener();
            addTransferListener (myDefaultConsoleListener);
         }
         else {
            removeTransferListener (myDefaultConsoleListener);
            myDefaultConsoleListener = null;
         }
      }
   }

   /**
    * Returns true if default printing of file transfer progress to the
    * console is enabled.
    *
    * @return true if default progress printing is enabled.
    */
   public boolean getConsoleProgressPrinting () {
      return myDefaultConsoleListener != null;
   }

   /**
    * Adds a FileTransferListener object that responds to transfer events.
    * Useful for displaying download progress.
    * 
    * @param listener
    * The file listener object
    */
   public void addTransferListener(FileTransferListener listener) {
      if (myTransferMonitor == null) {
         myTransferMonitor = new MultiFileTransferMonitor();
      }
      myTransferMonitor.addListener(listener);
   }

   /**
    * Removes a listener that is listener to transfer events.
    * 
    * @param listener
    * FileTransferListener to remove
    */
   public void removeTransferListener(FileTransferListener listener) {
      if (myTransferMonitor != null) {
         myTransferMonitor.removeListener(listener);
      }
   }

   /**
    * Gets the set of listeners for all transfers handled by this FileManager
    * object
    * 
    * @return Array of FileTransferListeners
    */
   public FileTransferListener[] getTransferListeners() {
      FileTransferListener[] list = myTransferMonitor.getListeners();
      if (myDefaultConsoleListener != null) {
         // remove this from the list:
         FileTransferListener[] xlist =
         new FileTransferListener[list.length-1];
         int k = 0;
         for (int i=0; i<list.length; i++) {
            if (list[i] != myDefaultConsoleListener) {
               xlist[k++] = list[i];
            }
         }
         return xlist;
      }
      else {
         return list;
      }
   }

   /**
    * Returns the FileTransferMonitor object that is is responsible for
    * detecting changes to the destination file and firing FileTransferEvents to
    * the set of {@link FileTransferListener}s. By default, a
    * {@link MultiFileTransferMonitor} is created which can monitor several file
    * transfers at once.
    * 
    * @return the current monitor
    * @see #addTransferListener(FileTransferListener)
    */
   public FileTransferMonitor getTransferMonitor() {
      return myTransferMonitor;
   }

   /**
    * Sets the FileTransferMonitor which is responsible for detecting when the
    * destination file has changed in order to fire update events.
    * 
    * @param monitor
    * the monitor to use
    */
   public void setTransferMonitor(FileTransferMonitor monitor) {
      myTransferMonitor = monitor;
   }

   /**
    * Sets the sleep time between checking for transfer updates. Transfers are
    * monitored by a {@link FileTransferMonitor} object that runs in a separate
    * thread. This periodically polls the destination files to see if the
    * transfer has progressed, and fires events to any FileTransferListeners
    * supplied by the {@link #addTransferListener(FileTransferListener)}
    * function.
    * 
    * @param seconds sleep time in seconds
    */
   public void setMonitorSleep(double seconds) {
      myTransferMonitor.setPollSleep((long)(seconds * 1000));
   }

   /**
    * Adds an authenticator for HTTPS, SFTP, WEBDAVS, that can respond to
    * domain/username/password requests. This authenticator is used for any URIs
    * that match patterns provided in `matcher'
    * 
    * @param matcher
    * object that checks whether a supplied URI matches a given set of criteria
    * @param auth
    * the authenticator object
    */
   public void addUserAuthenticator(URIxMatcher matcher, UserAuthenticator auth) {
      cacher.addAuthenticator(matcher, auth);
   }

   /**
    * Adds an identity repository consisting of a set of private RSA keys for
    * use with SFTP authentication. The keys are used whenever a URI matches a
    * set of parameters described in 'matcher'
    * 
    * @param matcher
    * object that checks whether supplied URI matches a given set of criteria
    * @param repo
    * repository containing a set of private RSA keys
    */
   public void addIdentityRepository(
      URIxMatcher matcher, SimpleIdentityRepository repo) {
      cacher.addIdentityRepository(matcher, repo);
   }

   /**
    * Returns a FileManager object used for {@link #staticGet(File, URIx)}.
    * Creates one if it does not yet exist.
    * 
    * @return the static FileManager object
    */
   public static FileManager getStaticManager() {
      if (staticManager == null) {
         staticManager = new FileManager();
      }
      return staticManager;
   }

   /**
    * Sets the FileManager used for {@link #staticGet(File, URIx)}.
    * 
    * @param manager
    * the FileManager to set for static operations
    */
   public static void setStaticManager(FileManager manager) {
      staticManager = manager;
   }

   /**
    * Static convenience method for downloading a file in a single shot. This
    * uses a static copy of a FileManager objects, creating one with default
    * options if it doesn't exist.
    * 
    * @param dest
    * the local path (relative or absolute) to download file to
    * @param source
    * the remote URI to cache
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @return File handle to local file
    * @throws FileTransferException only if there is no local copy of the file 
    * at the end of the function call
    * @see #get(File, URIx, int)
    */
   public static File staticGet(File dest, URIx source, int options)
   throws FileTransferException {

      staticManager = getStaticManager();
      return staticManager.get(dest, source, options);

   }

   /**
    * Static convenience method for downloading a file.  Uses default options.
    * 
    * @param dest
    * the local path (relative or absolute) to download file to
    * @param source
    * the remote URI to cache
    * @return File handle to local file
    * @throws FileTransferException only if there is no local copy of the file 
    * at the end of the function call
    * @see #staticGet(File, URIx, int)
    */
   public static File staticGet(File dest, URIx source)
   throws FileTransferException {
      return staticGet(dest, source, DEFAULT_OPTIONS);
   }

   /**
    * Static convenience method for downloading a file.
    *
    * @param destName destination path
    * @param sourceName source URI (as a string)
    * @param options
    * set of options, either FORCE_REMOTE or CHECK_HASH
    * @return File handle to local file
    * @throws URIxSyntaxException if the sourceName is malformed
    * @throws FileTransferException if download fails.
    * @see #get(String, String, int)
    */
   public static File staticGet(String destName, String sourceName, int options)
   throws FileTransferException {

      staticManager = getStaticManager();
      return staticManager.get(destName, sourceName, options);

   }

   /**
    * Static convenience method for downloading a file with default options.
    * 
    * @param destName destination path
    * @param sourceName source URI (as a string)
    * @return File handle to local file
    * @throws URIxSyntaxException if the sourceName is malformed
    * @throws FileTransferException if download fails.
    * @see #get(String, String, int)
    */
   public static File staticGet(String destName, String sourceName)
   throws FileTransferException {
      return staticGet(destName, sourceName, DEFAULT_OPTIONS);
   }

   // Decode VFS messages to create our own message string
   private static String decodeVFSMessage(Throwable t) {

      Throwable root = getRootThrowable(t);

      String msg = null;

      if (root instanceof java.net.UnknownHostException) {
         msg = "Cannot connect to server <" + root.getMessage() +
         ">, check your internet connection or the server address";
      } else if (root instanceof FileNotFoundException) {
         msg = "Cannot find file " + root.getMessage();        
      } else {
         msg = root.getMessage();
      }

      return msg;

   }

   public void setOptions(int options) {
      myOptions = options;
   }

   public int getOptions() {
      return myOptions;
   }

   protected static String concatPaths(String path1, String path2) {

      String separator = "/";
      String out = null;
      if (path1 != null) {
         path1 = path1.replace("\\", "/");
         out = path1;
      }      

      if (path2 != null) {
         path2 = path2.replace("\\", "/");
         if (out != null) {
            if (!out.endsWith(separator) && !path2.startsWith(separator)) {
               out += separator;
            }
            out += path2;
         } else {
            out = path2;
         }
      }

      return out;
   }

   /**
    * Returns an input stream for the file located at either {@code localCopy}
    * or {@code source} (according to options). Works with absolute paths and
    * source URIs, otherwise combines path and source URI with the default
    * download directory and remote source uri, respectively. If the 
    * destination is null or a directory, then the filename of source is 
    * appended.
    * 
    * If there is any internal problem, (such as failing to obtain a hash, or
    * failing to download a file), the function will log the error message and
    * continue.
    * 
    * @param localCopy
    * the local path (relative or absolute) to check for overriding file
    * @param source
    * the remote URI to read from
    * @param options
    * set of options, from {@code FORCE_REMOTE, DOWNLOAD_ZIP, CHECK_HASH}
    * @return File input stream
    * @throws FileTransferException if we cannot open the stream
    */
   public InputStream getInputStream(File localCopy, URIx source, int options) {

      // default local copy if none provided
      if (localCopy == null) {
         if (source.isRelative()) {
            localCopy = new File(source.getRawPath());
         } else {
            localCopy = new File(extractFileName(source));
         }
      } else if (localCopy.isDirectory()) {
         if (source.isRelative()) {
            localCopy = new File(localCopy, source.getRawPath());
         } else {
            localCopy = new File(localCopy, extractFileName(source));
         }
      }

      // convert to absolute
      localCopy = getAbsoluteDestFile(localCopy);
      source = getAbsoluteSourceURI(source);

      // download zip file first if requested
      if ( source.isZipType() && (options & DOWNLOAD_ZIP) != 0) {

         // get zip file
         URIx zipSource = source.getBaseURI();
         File zipDest = getAbsoluteDestFile(extractFileName(zipSource));
         File zipFile = get(zipDest, zipSource, options);

         // replace source URI
         source.setBaseURI(new URIx(zipFile));

         // XXX no longer need to check hash, since zip's hash would
         // have changed, although we do need to replace if re-downloaded zip
         // options = options & (~CHECK_HASH);
      }

      // check if we need to open remote stream
      boolean fetch = true;
      if ((options & FORCE_REMOTE) == 0) {

         // check if file exists
         if (localCopy.canRead()) {

            // check hash if options say so
            if ((options & CHECK_HASH) != 0) {
               try {
                  fetch = !equalsHash(localCopy, source);
                  if (fetch) {
                     logger.debug("Hash matches");
                  }
               } catch (FileTransferException e) {
                  logger.debug(
                     "Cannot obtain hash, assuming it doesn't match, "
                     + e.getMessage());
                  exceptionStack.add(e);
                  fetch = true;
               }
            } else {
               // file exists, so let it be
               fetch = false;
            }
         }
      }

      // open remote stream
      InputStream in = null;
      if (fetch) {
         try {
            cacher.initialize();
            logger.debug("Opening stream from " + source.toString());

            // open remote stream
            in = cacher.getInputStream(source);

         } catch (FileSystemException e) {
            cacher.release();
            String msg = "Failed to open remote file <" + source + ">";
            throw new FileTransferException(msg, e);
         }
      } else {
         logger.debug("Local file '" + localCopy + "' exists.");
         try {
            in = new FileInputStream(localCopy);
         } catch (IOException e) {
            String msg = "Failed to open local file <" + localCopy + ">";
            throw new FileTransferException(msg, e);
         }
      }

      lastFile = null;
      lastWasRemote = fetch;

      return in;
   }

   public InputStream getInputStream(URIx source, int options)
   throws FileTransferException {

      return getInputStream(null, source, options);
   }

   public InputStream getInputStream(URIx source) {
      return getInputStream(null, source, myOptions);
   }

   public InputStream getInputStream(String localCopy, String sourceName, 
      int options) throws FileTransferException {
      URIx source = new URIx(sourceName);
      File local = null;
      if (localCopy != null) {
         local = new File(localCopy);
      }
      return getInputStream(local, source, options);
   }

   public InputStream getInputStream(String localCopy, String sourceName) 
   throws FileTransferException {
      URIx source = new URIx(sourceName);
      File local = null;
      if (localCopy != null) {
         local = new File(localCopy);
      }
      return getInputStream(local, source, myOptions);
   }

   public InputStream getInputStream(String sourceName, int options) 
   throws FileTransferException {
      // try to make URI from sourceName
      URIx source = new URIx(sourceName);
      return getInputStream(null, source, options);
   }

   public InputStream getInputStream(String sourceName) 
      throws FileTransferException {
      return getInputStream(null, sourceName, myOptions);
   }

   /**
    * Must be called to free resources after streams have been read.
    */
   public void closeStreams() {
      cacher.release();
   }

   /**
    * Gets the last file retrieved.
    *
    * @return last file retrieved
    */
   public File getLastFile() {
      return lastFile;
   }

   /**
    * Returns true if the last file was fetched from remote,
    * false if last file was a local copy
    *
    * @return <code>true</code> if the last file was fetched from remote
    */
   public boolean wasLastRemote() {
      return lastWasRemote;
   }

}
