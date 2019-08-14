/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.webdav.WebdavFileSystemConfigBuilder;
import org.apache.http.ssl.TrustStrategy;

import com.jcraft.jsch.JSchException;

import maspack.fileutil.jsch.SimpleIdentityRepository;
import maspack.fileutil.uri.URIx;
import maspack.fileutil.uri.URIxMatcher;
import maspack.fileutil.vfs.SimpleIdRepoFactory;

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
   addIdentityRepository(URIxMatcher matcher, SimpleIdentityRepository repo) {
      identMap.put(matcher, repo);
   }

   public static void setDefaultFsOptions(FileSystemOptions opts)
   throws FileSystemException {

      // SSH Defaults
      // Don't check host key
      // Use paths relative to root (as opposed to the user's home dir)
      // 10 second timeout
      SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
         opts, "no");
      SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, true);
      SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);

      /**
       * Allow connection to silly UBC servers who don't update their credentials
       */
      TrustStrategy[] ts = {new UnsafeTrustStrategy()};
      HttpFileSystemConfigBuilder httpBuilder = HttpFileSystemConfigBuilder.getInstance();
      WebdavFileSystemConfigBuilder webdavBuilder = WebdavFileSystemConfigBuilder.getInstance();

      // allow all SSL connections
      httpBuilder.setTrustStrategies(opts, ts);
      webdavBuilder.setTrustStrategies(opts, ts);

      // silly deprecated UBC cipher suite
      String[] ciphers = httpBuilder.getDefaultSSLCipherSuites();
      ciphers = Arrays.copyOf(ciphers, ciphers.length+1);
      ciphers[ciphers.length-1] = "SSL_RSA_WITH_RC4_128_SHA";

      httpBuilder.setEnabledSSLCipherSuites(opts, ciphers);
      webdavBuilder.setEnabledSSLCipherSuites(opts, ciphers);

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

   public boolean exists(URIx uri) throws FileSystemException {
      FileObject remoteFile = null; // will resolve next

      // loop through authenticators until we either succeed or cancel
      boolean cancel = false;
      while (remoteFile == null && cancel == false) {
         remoteFile = resolveRemote(uri);
      }

      if (remoteFile == null || !remoteFile.exists()) {
         return false;
      }
      
      return true;
   }

   public File cache(URIx uri, File cacheFile, FileTransferMonitor monitor)
      throws FileSystemException {

      // For atomic operation, first download to temporary directory
      File tmpCacheFile = new File(cacheFile.getAbsolutePath() + TMP_EXTENSION);
      URIx cacheURI = new URIx(cacheFile.getAbsoluteFile());
      URIx tmpCacheURI = new URIx(tmpCacheFile.getAbsoluteFile());
      FileObject localTempFile = manager.resolveFile(tmpCacheURI.toString());
      FileObject localCacheFile = manager.resolveFile(cacheURI.toString());

      FileObject remoteFile = null; // will resolve next

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
         if (remoteFile.isFile()) {
            localTempFile.copyFrom(remoteFile, Selectors.SELECT_SELF);
         } else if (remoteFile.isFolder()) {
            // final FileObject fileSystem = manager.createFileSystem(remoteFile);
            localTempFile.copyFrom(remoteFile, new AllFileSelector());
            // fileSystem.close();
         }

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
         if (tmpCacheFile.isDirectory()) {
            SafeFileUtils.moveDirectory(tmpCacheFile, cacheFile);
         } else {
            SafeFileUtils.moveFile(tmpCacheFile, cacheFile);
         }
      } catch (Exception e) {
         localCacheFile.delete(); // delete if possible
         throw new RuntimeException("Failed to atomically move " +
         "to " + localCacheFile.getURL(), e);
      }

      return cacheFile;

   }
   
   public boolean copy(File from, URIx to, FileTransferMonitor monitor) throws FileSystemException {
      return copy(new URIx(from), to, monitor);
   }
   
   public boolean copy(File from, URIx to) throws FileSystemException {
      return copy(new URIx(from), to);
   }

   public boolean copy(URIx from, URIx to) throws FileSystemException {
      return copy(from, to, null);
   }
   
   public boolean copy(URIx from, URIx to, FileTransferMonitor monitor) throws FileSystemException {
      
      FileObject fromFile = null;
      FileObject toFile = null;

      // clear authenticators
      setAuthenticator(fsOpts, null);
      setIdentityFactory(fsOpts, null);

      // loop through authenticators until we either succeed or cancel
      boolean cancel = false;
      while (toFile == null && cancel == false) {
         toFile = resolveRemote(to);
      }

      cancel = false;
      while (fromFile == null && cancel == false) {
         fromFile = resolveRemote(from);
      }

      if (fromFile == null || !fromFile.exists()) {
         throw new FileSystemException("Cannot find source file <" + from.toString()+ ">",
            new FileNotFoundException( "<" + from.toString() + ">"));
      }
      
      if (toFile == null) {
         throw new FileSystemException("Cannot find destination <" + to.toString()+ ">",
            new FileNotFoundException( "<" + to.toString() + ">"));
      }

      // monitor the file transfer progress
      if (monitor != null) {
         monitor.monitor(fromFile, toFile, -1, fromFile.getName().getBaseName());
         monitor.start();
         monitor.fireStartEvent(toFile);
      }

      // transfer content
      try {
         if (fromFile.isFile()) {
            toFile.copyFrom(fromFile, Selectors.SELECT_SELF);
         } else if (fromFile.isFolder()) {
            // final FileObject fileSystem = manager.createFileSystem(remoteFile);
            toFile.copyFrom(fromFile, new AllFileSelector());
            // fileSystem.close();
         }

         if (monitor != null) {
            monitor.fireCompleteEvent(toFile);
         }
      } catch (Exception e) {
         throw new FileTransferException("Failed to complete transfer of " + fromFile.getURL() + " to " + toFile.getURL(), e);
      } finally {
         // close files if we need to
         fromFile.close();
         toFile.close();
         if (monitor != null) {
            monitor.release(toFile);
            monitor.stop();
         }
      }

      return true;

   }

   public InputStream getInputStream(URIx uri) throws FileSystemException {

      FileObject remoteFile = null; // will resolve next

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
   
   public OutputStream getOutputStream(URIx uri) throws FileSystemException {

      FileObject remoteFile = null; // will resolve next

      // loop through authenticators until we either succeed or cancel
      boolean cancel = false;
      while (remoteFile == null && cancel == false) {
         remoteFile = resolveRemote(uri);
      }

      if (remoteFile == null) {
         throw new FileSystemException("Cannot find remote file <" + uri.toString()+ ">",
            new FileNotFoundException( "<" + uri.toString() + ">"));
      }

      // open stream content
      OutputStream stream = null;
      try {
         stream = remoteFile.getContent().getOutputStream();
      } catch (Exception e) {
         throw new RuntimeException("Failed to open " +
         remoteFile.getURL(), e);
      } finally {
         remoteFile.close();
      }

      return stream;

   }

   private FileObject resolveRemote(URIx uri) throws FileSystemException  {

      FileObject remoteFile = null;
      URIx base = uri.getBaseURI(); // base determines the first protocol

      // clear authenticators
      setAuthenticator(fsOpts, null);
      setIdentityFactory(fsOpts, null);
      
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
      setIdentityFactory(fsOpts, null);
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
      
      // try without authentication last (otherwise it seems to resolve, which is bad)
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
      while (out.getCause() != null && out.getCause() != out) {
         out = out.getCause();
      }
      return out;
   }

}
