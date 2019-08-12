/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

import java.util.HashMap;

public class URIxScheme {
   
   public static final int ZIP_TYPE = 1;
   private static HashMap<String,URIxScheme> schemeMap = new HashMap<>();
   
   public static final URIxScheme GZ = createScheme("gz", ZIP_TYPE);
   public static final URIxScheme BZ2 = createScheme("bz2", ZIP_TYPE);
   public static final URIxScheme TAR = createScheme("tar", ZIP_TYPE);
   public static final URIxScheme TGZ = createScheme("tgz", ZIP_TYPE);
   public static final URIxScheme TBZ2 = createScheme("tbz2", ZIP_TYPE);
   public static final URIxScheme ZIP = createScheme("zip", ZIP_TYPE);
   public static final URIxScheme JAR = createScheme("jar", ZIP_TYPE);
   public static final URIxScheme FILE = createScheme("file", 0);
   public static final URIxScheme HTTP = createScheme("http", 0);
   public static final URIxScheme HTTPS = createScheme("https", 0);
   public static final URIxScheme FTP = createScheme("ftp", 0);
   public static final URIxScheme SFTP = createScheme("sftp", 0);
   public static final URIxScheme WEBDAV = createScheme("webdav", 0, new String[]{"webdav", "dav"});
   public static final URIxScheme WEBDAVS = createScheme("webdavs", 0, new String[]{"webdavs", "davs"});
   
   private String name;
   private int type;
   private String[] aliases;
   
   private URIxScheme(String name, int type) {
      this(name, type, new String[]{name});
   }
   
   private URIxScheme(String name, int type, String[] aliases) {
      this.name = name;
      this.type = type;
      if (aliases == null) {
         aliases = new String[]{ name };
      }
      this.aliases = aliases;
   }
   
   public boolean isZipType() {
      return (type & ZIP_TYPE) > 0;
   }
   
   public String toString() {
      return name;
   }
   
   public static URIxScheme createScheme(String str, int typemask) {
      return createScheme(str, typemask, new String[]{str});
   }
   
   public static URIxScheme createScheme(String str, int typemask, String[] aliases) {
      URIxScheme scheme = new URIxScheme(str, typemask, aliases);
      synchronized (schemeMap) {
         for (String alias : scheme.aliases) {
            schemeMap.put(alias, scheme);
         }
      }
      return scheme;
   }
   
   public static URIxScheme findScheme(String str) {
      URIxScheme scheme = null;
      synchronized(schemeMap) {
         scheme = schemeMap.get(str);
         //  XXX do not create scheme by default
         //         if (scheme == null) {
         //            scheme = createScheme(str, 0, new String[]{str});
         //         }
      }
      return scheme;
   }
   
}
