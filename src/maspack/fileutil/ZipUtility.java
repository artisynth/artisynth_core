/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import maspack.fileutil.uri.URIx;

/**
 * Class for unzipping zip/tar/gzip/etc... 
 * @author Antonio
 *
 */
public class ZipUtility {

   public static void unzip(final String src, final String dest) throws IOException {
      unzip(new URIx(src), new File(dest));
   }
   
   public static void unzip(final File src, final File dest) throws IOException {
      unzip(new URIx(src), dest);
   }
   
   public static void unzip(URIx src, final File dest) throws IOException {
      dest.mkdirs();

      final FileSystemManager fileSystemManager = VFS.getManager();
      final FileObject zipFileObject =
         fileSystemManager.resolveFile(src.toString());
      
      try {
         final FileObject fileSystem = fileSystemManager.createFileSystem(zipFileObject);
         try {
            fileSystemManager.toFileObject(dest).copyFrom(fileSystem, new AllFileSelector());
         } finally {
            fileSystem.close();
         }
      } finally {
         zipFileObject.close();
      }
   }
}
