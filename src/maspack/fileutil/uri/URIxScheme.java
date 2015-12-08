/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

public enum URIxScheme {
   
   GZ("gz",0), BZ2("bz2",0), ZIP("zip",0), JAR("jar",0), TAR("tar",0), 
   TGZ("tgz",0), TBZ2("tbz2",0), FILE("file",1), HTTP("http",1), 
   HTTPS("https",1), FTP("ftp",1), SFTP("sftp",1);
      
   public static int ZIP_TYPE = 0;
   public static int OTHER_TYPE = 1;
   private String name;
   private int type;
   
   URIxScheme(String name, int type) {
      this.type = type;
      this.name = name;
   }
   
   public boolean isZipType() {
      return type == ZIP_TYPE;
   }
   
   public String toString() {
      return name;
   }
   
   public static URIxScheme getScheme(String str) {
      for (URIxScheme scheme : URIxScheme.values()) {
         if (scheme.toString().equals(str)) {
            return scheme;
         }
      }
      return null;
   }
   
}
