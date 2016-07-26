/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.File;

import maspack.fileutil.FileManager;
import maspack.fileutil.uri.URIx;

/**
 * A {@link FileManager} that by default looks for files in
 * the ArtiSynth source directory tree.  The main 
 * purpose is to easily get file handles from within
 * zip files using the {{@link #getInputStream(String)} function.
 * 
 * @author Antonio
 *
 */
public class ArtisynthFileManager extends FileManager {
      
   /**
    * Creates a {@link FileManager} object that looks for files
    * in the source tree.
    * @param obj object associated with source directory
    * @param relPathOrZip either a relative path to read files
    * from, or a zip file
    */
   public ArtisynthFileManager(Object obj, String relPathOrZip) {
      this(obj, relPathOrZip, "");
   }
   
   /**
    * Creates a {@link FileManager} object that looks for files
    * in the source tree.
    * @param obj object associated with source directory
    * @param relPathOrZip either a relative path to read files
    * from, or a zip file
    * @param subpath An additional path to be appended, for instance
    * within a provided zip file
    */
   public ArtisynthFileManager(Object obj, String relPathOrZip, String subpath) {
      String host = ArtisynthPath.getSrcRelativePath(obj, relPathOrZip);
      File hostFile = new File(host);
      if (hostFile.isDirectory()) {
         setDownloadDir(hostFile);
      } else {
         setDownloadDir(hostFile.getParentFile().getAbsolutePath());
      }
      
      String hostURIstr = detectHost(hostFile.toURI().toString(), subpath);
      URIx hostUri = new URIx(hostURIstr);
      setRemoteSource(hostUri);
   }
   
   /**
    * Constructs a {@link FileManager} object for reading from zip
    * files
    * @param zipFile the file from which to extract data
    * @param subpath sub folder within the zip file to use by default
    */
   public ArtisynthFileManager(File zipFile, String subpath) {
      String folder = zipFile.getParentFile().getAbsolutePath();
      setDownloadDir(folder);
      
      String hostURIstr = detectHost(zipFile.toURI().toString(), subpath);
      URIx hostUri = new URIx(hostURIstr);
      setRemoteSource(hostUri);
   }
   
   private String detectHost(String zipOrFolder, String subpath) {
      
      String host;
      if (zipOrFolder == null) {
         return null;
      } else if (zipOrFolder.endsWith(".tar.gz") || zipOrFolder.endsWith(".tgz")) {
         host = "tgz:" + concatPaths(zipOrFolder, subpath) + "!/";
      } else if (zipOrFolder.endsWith(".zip")) {
         host = "zip:" + concatPaths(zipOrFolder, subpath) + "!/";
      } else if (zipOrFolder.endsWith(".tar")) {
         host = "tar:" + concatPaths(zipOrFolder, subpath) + "!/";
      }  else if (zipOrFolder.endsWith(".jar")) {
         host = "jar:" + concatPaths(zipOrFolder, subpath) + "!/";
      } else if (zipOrFolder.endsWith(".gz")) {
         host = "gz:" + concatPaths(zipOrFolder, subpath) + "!/";
      } else {
         host = concatPaths(zipOrFolder, subpath);
      }
      
      return host;
   }
   
}
