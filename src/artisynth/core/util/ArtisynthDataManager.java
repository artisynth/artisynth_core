package artisynth.core.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.UserAuthenticator;

import maspack.crypt.Cryptor;
import maspack.fileutil.FileManager;
import maspack.fileutil.FileTransferException;
import maspack.fileutil.VFSCryptor;
import maspack.fileutil.uri.AnyMatcher;
import maspack.fileutil.uri.URIx;
import maspack.fileutil.uri.URIxMatcher;
import maspack.fileutil.vfs.EncryptedUserAuthenticator;

/**
 * Utility class for manager remote data files, allowing them to be downloaded
 * on demand (and hence stay out of source version control)
 * @author antonio
 *
 */
public class ArtisynthDataManager {
   
   /**
    * Default directory for storing downloaded files
    */
   public static final File DEFAULT_LOCAL_ROOT = new File(ArtisynthPath.getHomeDir() + "/tmp/.cache/data/");
   
   private FileManager manager;
   private Cryptor cryptor;
   
   public ArtisynthDataManager() {
      this(null, null);
   }
   
   /**
    * Creates a data manager capable of transfering files to/from a remote folder or server
    * @param remoteUri URI for remote root
    */
   public ArtisynthDataManager(String remoteUri) {
      this(remoteUri, DEFAULT_LOCAL_ROOT);
   }
   
   /**
    * Creates a data manager capable of transfering files to/from a remote folder or server
    * @param remoteUri URI for remote root
    * @param localRoot local directory to use for storing files
    */
   public ArtisynthDataManager(String remoteUri, File localRoot) {
      super();
      URIx remote = new URIx(remoteUri);
      File tmp = localRoot;
      if (tmp != null && !tmp.exists()) {
         tmp.mkdirs();
      }
      manager = new FileManager(tmp, remote);
      manager.setOptions(FileManager.DOWNLOAD_ZIP);
      
   }
   
   /**
    * Root directory of remote filesystem
    * @param remote
    */
   public void setRemoteRoot(String remote) {
      manager.setRemoteSource(remote);
   }
   
   /**
    * Local directory to download files to
    * @param file
    */
   public void setLocalRoot(File file) {
      if (!file.exists()) {
         file.mkdirs();
      }
      manager.setDownloadDir(file);
   }
   
   private String detectHost(String zipOrFolder) {
      
      //      Stringhost = grabber.getRemoteSource().toString();;
      //      if (zipOrFolder.endsWith(".tar.gz") || zipOrFolder.endsWith(".tgz")) {
      //         host = "tgz:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      //      } else if (zipOrFolder.endsWith(".zip")) {
      //         host = "zip:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      //      } else if (zipOrFolder.endsWith(".tar")) {
      //         host = "tar:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      //      }  else if (zipOrFolder.endsWith(".jar")) {
      //         host = "jar:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      //      } else if (zipOrFolder.endsWith(".gz")) {
      //         host = "gz:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      //      } else if (isURI(zipOrFolder)){
      //         host = zipOrFolder; 
      //      } else {
      //         host = concatPaths(ARTISYNTH_SOURCE, zipOrFolder);
      //      }
       
      return null;
   }
   
   /**
    * Set an en/de-cryptor to be used for storing passwords
    * @param cryptor en/de-cryptor
    */
   public void setCryptor(Cryptor cryptor) {
      this.cryptor = cryptor;
   }
   
   /**
    * Gets the current en/de-cryptor to be used for storing passwords.
    * If no cryptor was set, we use a default one provided by VFS2 
    * @param cryptor en/de-cryptor
    */
   public Cryptor getCryptor() {
      if (cryptor == null) {
         return new VFSCryptor(org.apache.commons.vfs2.util.CryptorFactory.getCryptor());
      }
      return cryptor;
   }
   
   /**
    * Set username and password credentials.  The password here is assumed to be in plaintext.
    * @param username user name
    * @param password plaintext password
    */
   public void setCredentials(String username, String password) {
      Cryptor cryptor = getCryptor();
      setEncryptedCredentials(username, cryptor.encrypt(password));
   }
   
   /**
    * Set the username and password credentials.  The password here is assumed to already be
    * encrypted using this manager's Cryptor (see {@link #setCryptor(Cryptor)}).  This allows
    * for some obfuscation of passwords.
    * @param username user name
    * @param encryptedPassword encrypted password
    */
   public void setEncryptedCredentials(String username, String encryptedPassword) {
      
      EncryptedUserAuthenticator userPasswordAuthenticator = new EncryptedUserAuthenticator(getCryptor());
      userPasswordAuthenticator.setUserName(username);
      userPasswordAuthenticator.setEncryptedPassword(encryptedPassword);

      manager.addUserAuthenticator(new AnyMatcher(), userPasswordAuthenticator);
   
   }
   
   /**
    * Adds an authenticator that can respond to domain/username/password requests. 
    * This authenticator is used for any URIs that match patterns provided in `matcher'
    * 
    * @param matcher
    * object that checks whether a supplied URI matches a given set of criteria
    * @param auth
    * the authenticator object
    */
   public void addAuthenticator(URIxMatcher matcher, UserAuthenticator auth) {
      manager.addUserAuthenticator(matcher, auth);
   }
   
   /**
    * Returns a file retrieved from the data storage server, specified relative to 
    * a given package.  The fully qualified package name is expanded into a folder 
    * structure used for identifying the desired file on the storage server.
    * 
    * <p>The package is determined from <code>obj</code>, either directly,
    * if <code>obj</code> is an instance of <code>Package</code>, or by
    * determining the package that the supplied resource belongs to.
    *
    * @param obj
    * object used to determine the package
    * @param filename
    * path relative to the package directory
    * @return desired file
    */
   public File getPackageRelativeFile(Object obj, String filename) {
      Package pkg = null;
      if (obj instanceof Package) {
         pkg = (Package)obj;
      } else if (obj instanceof Class<?>) {
         pkg = ((Class<?>)obj).getPackage();
      } else {
         pkg = obj.getClass().getPackage();
      }
      return getPackageRelativeFile(pkg, filename);
   }
   
   /**
    * Returns a file retrieved from the data storage server, specified relative to 
    * a given package.  The fully qualified package name is expanded into a folder 
    * structure used for identifying the desired source file on the storage server.
    * 
    * @param pkg
    * package used for determining path on storage server
    * @param filename
    * path relative to the package directory
    * @return desired local file
    */
   public File getPackageRelativeFile(Package pkg, String filename) {
      String path = pkg.getName().replace('.', '/');
      if (filename != null) {
         filename.trim().replace('\\', '/');
         if (filename.startsWith("/")) {
            path = path + filename;
         } else {
            path = path + "/" + filename;
         }
      }
      return getFile(path);
   }
   
   /**
    * Get file with path relative to storage root
    * @param filename
    * @return link to local file
    */
   public File getFile(String filename) {
      File out = null;
      try {
          out = manager.get(filename);
      } catch (FileTransferException e) {
         e.printStackTrace();
      }
      return out;
   }
   
   /**
    * Uploads a file to the data storage server, at a location specified relative to 
    * a given package.  The fully qualified package name is expanded into a folder 
    * structure used for identifying the desired target destination on the storage server.
    * 
    * <p>The package is determined from <code>obj</code>, either directly,
    * if <code>obj</code> is an instance of <code>Package</code>, or by
    * determining the package that the supplied resource belongs to.
    *
    * @param source
    * file to upload
    * @param obj
    * object used to determine the package
    * @param dest
    * path relative to the package directory
    * @throws IOException if upload fails, such as invalid destination or insufficient privileges
    */
   public void putPackageRelativeFile(File source, Object obj, String dest) throws IOException {
      Package pkg = null;
      if (obj instanceof Package) {
         pkg = (Package)obj;
      } else if (obj instanceof Class<?>) {
         pkg = ((Class<?>)obj).getPackage();
      } else {
         pkg = obj.getClass().getPackage();
      }
      putPackageRelativeFile(source, pkg, dest);
   }
   
   /**
    * Uploads a file to the data storage server, specified relative to 
    * a given package.  The fully qualified package name is expanded into a folder 
    * structure used for identifying the desired target file on the storage server.
    * 
    * @param source
    * file to upload
    * @param pkg
    * package used for determining path on storage server
    * @param dest
    * path relative to the package directory
    * @throws IOException if upload fails, such as invalid destination or insufficient privileges
    */
   public void putPackageRelativeFile(File source, Package pkg, String dest) throws IOException {
      String path = pkg.getName().replace('.', '/');
      
      if (dest == null) {
         if (source.isAbsolute()) {
            dest = source.getName();
         } else {
            dest = source.getPath();
         }
      }
      
      dest.trim().replace('\\', '/');
      if (dest.startsWith("/")) {
         path = path + dest;
      } else {
         path = path + "/" + dest;
      }
      putFile(source, path);
   }
   
   /**
    * Upload a file with path relative to storage root
    * @param source file to upload
    * @param filename destination
    * @throws IOException if upload fails, such as invalid destination or insufficient privileges
    */
   public void putFile(File source, String filename) throws IOException {
      if (filename == null) {
         if (source.isAbsolute()) {
            filename = source.getName();
         } else {
            filename = source.getPath();
         }
      }
      try {
          manager.put(source, new URIx(filename));
      } catch (Exception e) {
         throw new IOException("Unable to upload file to <" + filename.toString() + ">", e);
      }
   }
   
   /**
    * Get underlying file manager
    * @return delegate for handling file transfers
    */
   protected FileManager getManager() {
      return manager;
   }

}
