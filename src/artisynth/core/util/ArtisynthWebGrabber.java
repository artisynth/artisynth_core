/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import maspack.fileutil.DefaultConsoleFileTransferListener;
import maspack.fileutil.FileManager;
import maspack.fileutil.FileTransferListener;
import maspack.fileutil.uri.URIx;

public class ArtisynthWebGrabber extends FileManager {

   private static final String ARTISYNTH_SOURCE = 
      "http://www.artisynth.org/files/data/";
      
   public ArtisynthWebGrabber() {
      setRemoteSource(ARTISYNTH_SOURCE);
      addListener();
   }
   
   public ArtisynthWebGrabber(String downloadDir, String zipOrFolder, int options) {
      this();
      setDownloadDir(downloadDir);
      String host = detectHost(zipOrFolder);
      setRemoteSource(host);
      setOptions(options);
   }
   
   private boolean isURI(String uri) {
      String schemeStr = URIx.getSchemeStr(uri);
      if (schemeStr != null) {
         return true;
      }
      return false;
   }
   
   private String detectHost(String zipOrFolder) {
      
      String host;
      if (zipOrFolder == null) {
         host = ARTISYNTH_SOURCE;
      } else if (zipOrFolder.endsWith(".tar.gz") || zipOrFolder.endsWith(".tgz")) {
         host = "tgz:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      } else if (zipOrFolder.endsWith(".zip")) {
         host = "zip:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      } else if (zipOrFolder.endsWith(".tar")) {
         host = "tar:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      }  else if (zipOrFolder.endsWith(".jar")) {
         host = "jar:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      } else if (zipOrFolder.endsWith(".gz")) {
         host = "gz:" + concatPaths(ARTISYNTH_SOURCE, zipOrFolder) + "!/";
      } else if (isURI(zipOrFolder)){
         host = zipOrFolder; 
      } else {
         host = concatPaths(ARTISYNTH_SOURCE, zipOrFolder);
      }
      
      return host;
   }
   
   public ArtisynthWebGrabber(String downloadDir) {
      this();
      setDownloadDir(downloadDir);
   }
      
   private void addListener() {
      FileTransferListener listener = new DefaultConsoleFileTransferListener();
      addTransferListener(listener);
   }
   
}
