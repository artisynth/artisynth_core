package maspack.fileutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import maspack.fileutil.jsch.SimpleIdentityRepository;
import maspack.fileutil.uri.URIx;
import maspack.fileutil.uri.URIxMatcher;
import maspack.fileutil.vfs.SimpleIdRepoFactory;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSchException;

public class FileCacher {

   // set log level to "Error" for commons logging
   static {
      System.setProperty(
         "org.apache.commons.logging.Log",
         "org.apache.commons.logging.impl.NoOpLog");
      System.setProperty(
         "org.apache.commons.logging.simplelog.defaultlog", "fatal");
   }
   static String TMP_EXTENSION = ".part";

   FileSystemOptions fsOpts;
   StandardFileSystemManager manager;
   HashMap<URIxMatcher,UserAuthenticator> authMap;
   HashMap<URIxMatcher,SimpleIdentityRepository> identMap;
   SimpleIdRepoFactory myIdFactory;
   boolean initialized = false;
   
   public FileCacher () {

      authMap = new HashMap<URIxMatcher,UserAuthenticator>();
      identMap = new HashMap<URIxMatcher,SimpleIdentityRepository>();
      myIdFactory = new SimpleIdRepoFactory();

      fsOpts = new FileSystemOptions();
      manager = new StandardFileSystemManager();
      initialized = false;
      
   }

   public void initialize() throws FileSystemException {
      if (!initialized) {
         manager.init();
         setDefaultFsOptions(fsOpts);
      }
      initialized = true;
   }

   public void release() {
      manager.close();
      initialized = false;
   }

   public void addAuthenticator(URIxMatcher matcher, UserAuthenticator auth) {
      authMap.put(matcher, auth);
   }

   public void
      addIdentityRepository(URIxMatcher matcher, IdentityRepository repo) {

   }

   public static void setDefaultFsOptions(FileSystemOptions opts)
      throws FileSystemException
   {

      // SSH Defaults
      // Don't check host key
      // Use paths relative to root (as opposed to the user's home dir)
      // 10 second timeout
      SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
         opts, "no");
      SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, true);
      SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);

   }

   public static void setAuthenticator(FileSystemOptions opts,
      UserAuthenticator auth) throws FileSystemException  {

      DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(
         opts, auth);
   }

   public static void setIdentityFactory(FileSystemOptions opts,
      IdentityRepositoryFactory factory) throws FileSystemException {
      SftpFileSystemConfigBuilder.getInstance().setIdentityRepositoryFactory(
         opts, factory);
   }

   public File cache(URIx uri, String cachePath) throws FileSystemException {
      return cache(uri, new File(cachePath));
   }   

   /**
    * Returns the size of the specified file
    */
   public long getFileSize(URIx uri) {
      return -1;
   }

   public long getFileSize(File file) {
      return -1;
   }

   public File cache(URIx uri, File cacheFile) throws FileSystemException {
      return cache(uri, cacheFile, null);
   }

   public File cache(URIx uri, File cacheFile, FileTransferMonitor monitor)
      throws FileSystemException {

      // For atomic operation, first download to temporary directory
      File tmpCacheFile = new File(cacheFile.getAbsolutePath() + TMP_EXTENSION);
      FileObject localTempFile =
         manager.resolveFile(tmpCacheFile.getAbsolutePath());
      FileObject localCacheFile =
         manager.resolveFile(cacheFile.getAbsolutePath());

      FileObject remoteFile = null; // will resolve next

      // clear authenticators
      setAuthenticator(fsOpts, null);
      setIdentityFactory(fsOpts, null);

      // loop through authenticators until we either succeed or cancel
      boolean cancel = false;
      while (remoteFile == null && cancel == false) {
         remoteFile = resolveRemote(uri);
      }

     
      if (remoteFile == null || !remoteFile.exists()) {
         throw new FileSystemException("Cannot find remote file <" + uri.toString()+ ">",
            new FileNotFoundException( "<" + uri.toString() + ">"));
      }

      // monitor the file transfer progress
      if (monitor != null) {
         monitor.monitor(localTempFile, remoteFile, -1, cacheFile.getName());
         monitor.start();
         monitor.fireStartEvent(localTempFile);
      }

      // transfer content
      try {
         localTempFile.copyFrom(remoteFile, Selectors.SELECT_SELF);
         if (monitor != null) {
            monitor.fireCompleteEvent(localTempFile);
         }
      } catch (Exception e) {
         // try to delete local file
         localTempFile.delete();
         throw new RuntimeException("Failed to complete transfer of " +
            remoteFile.getURL() + " to " + localTempFile.getURL(), e);
      } finally {
         // close files if we need to
         localTempFile.close();
         remoteFile.close();
         if (monitor != null) {
            monitor.release(localTempFile);
            monitor.stop();
         }

      }

      // now that the copy is complete, do a rename operation
      try {
         SafeFileUtils.moveFile(tmpCacheFile, cacheFile);
      } catch (Exception e) {
         localCacheFile.delete(); // delete if possible
         throw new RuntimeException("Failed to atomically move " +
            "to " + localCacheFile.getURL(), e);
      }

      return cacheFile;

   }
   
   public InputStream getInputStream(URIx uri) throws FileSystemException{
      
      FileObject remoteFile = null; // will resolve next

      // clear authenticators
      setAuthenticator(fsOpts, null);
      setIdentityFactory(fsOpts, null);

      // loop through authenticators until we either succeed or cancel
      boolean cancel = false;
      while (remoteFile == null && cancel == false) {
         remoteFile = resolveRemote(uri);
      }

     
      if (remoteFile == null || !remoteFile.exists()) {
         throw new FileSystemException("Cannot find remote file <" + uri.toString()+ ">",
            new FileNotFoundException( "<" + uri.toString() + ">"));
      }

      // open stream content
      InputStream stream = null;
      try {
         stream = remoteFile.getContent().getInputStream();
      } catch (Exception e) {
         remoteFile.close();
         throw new RuntimeException("Failed to open " +
            remoteFile.getURL(), e);
      } finally {
      }

      return stream;
      
   }

   private FileObject resolveRemote(URIx uri) throws FileSystemException  {

      FileObject remoteFile = null;
      URIx base = uri.getBaseURI(); // base determines the first protocol

      // first try to find matching identity
      for (URIxMatcher matcher : identMap.keySet()) {
         if (matcher.matches(base)) {

            // set identity, try to resolve file
            myIdFactory.setIdentityRepository(identMap.get(matcher));
            setIdentityFactory(fsOpts, myIdFactory);

            remoteFile = tryGettingRemote(uri);
            if (remoteFile != null) {
               return remoteFile;
            }

         }
      }

      // then try authenticator
      for (URIxMatcher matcher : authMap.keySet()) {
         if (matcher.matches(base)) {
            // we have found an authenticator
            setAuthenticator(fsOpts, authMap.get(matcher));

            remoteFile = tryGettingRemote(uri);
            if (remoteFile != null) {
               return remoteFile;
            }
         }
      }

      // finally try without authentication?
      remoteFile = tryGettingRemote(uri);
      if (remoteFile != null) {
         return remoteFile;
      }

      return null;
   }

   private FileObject tryGettingRemote(URIx uri) throws FileSystemException {

      FileObject remoteFile = null;
      try {
         remoteFile = manager.resolveFile(uri.toString(), fsOpts);
      } catch (FileSystemException e) {
         Throwable ex = getBaseThrowable(e);
         if (!(ex instanceof JSchException)) {
            throw e;
         } else {
            return null;
         }
      } catch (Exception e) {
         System.out.println("caught some type of exception");
      }
      return remoteFile;

   }

   private Throwable getBaseThrowable(Throwable b) {
      Throwable out = b;
      while (out.getCause() != null) {
         out = out.getCause();
      }
      return out;
   }

}
