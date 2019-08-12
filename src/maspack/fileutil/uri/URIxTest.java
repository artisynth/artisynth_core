package maspack.fileutil.uri;

import java.net.URI;
import java.net.URISyntaxException;

import maspack.util.TestException;

public class URIxTest {
   
   private static void assertStringsEqual(Object s1, Object s2) {
      if (s1 != s2) {
         if (s1 == null || s2 == null || !s1.toString().equals (s2.toString ())) {
            throw new TestException ("Strings do not match: " + s1 + " vs " + s2);
         }
      }
   }
   
   public static void testHTMLResolve() {
      
      URIx base = new URIx("https://antonio@domain.com:180/path/in/base");
      
      assertStringsEqual(base.resolve ("/absolute/path"), "https://antonio@domain.com:180/absolute/path");
      assertStringsEqual(base.resolve ("relative/path"), "https://antonio@domain.com:180/path/in/relative/path");
      assertStringsEqual(base.resolve ("relative/path#withfragment"), "https://antonio@domain.com:180/path/in/relative/path#withfragment");
      assertStringsEqual(base.resolve ("relative/path?withquery#withfragment"), "https://antonio@domain.com:180/path/in/relative/path?withquery#withfragment");
      
      base = new URIx("https://antonio@domain.com:180/path/in/base/");
      
      assertStringsEqual(base.resolve ("/absolute/path"), "https://antonio@domain.com:180/absolute/path");
      assertStringsEqual(base.resolve ("relative/path"), "https://antonio@domain.com:180/path/in/base/relative/path");
      assertStringsEqual(base.resolve ("relative/path#withfragment"), "https://antonio@domain.com:180/path/in/base/relative/path#withfragment");
      assertStringsEqual(base.resolve ("relative/path?withquery#withfragment"), "https://antonio@domain.com:180/path/in/base/relative/path?withquery#withfragment");
      
      assertStringsEqual(base.resolve ("../../relative/path?withquery#withfragment"), "https://antonio@domain.com:180/path/relative/path?withquery#withfragment");
      assertStringsEqual(base.resolve ("../.././relative/./path?withquery#withfragment"), "https://antonio@domain.com:180/path/relative/path?withquery#withfragment");
      assertStringsEqual(base.resolve ("../.././relative/.."), "https://antonio@domain.com:180/path/");
   }
   
   public static void testHTMLRelativize() {
      
      URIx base = new URIx("https://antonio@domain.com:180/path/in/base");
      
      try {
         URI parent = new URI(base.toString ());
         URI child = new URI("https://antonio@domain.com:180/path/in/base/subpath");
         URI rel = parent.relativize (child);
         URI recover = parent.resolve (rel);
         // assertStringsEqual (child, recover);  // XXX URI class fails (BUG JDK-6523089)
      }
      catch (URISyntaxException e) {
         e.printStackTrace();
      }
      
      assertStringsEqual(base.relativize("https://antonio@domain.com:180/path/in/base/subpath"), "base/subpath");
      assertStringsEqual(base.relativize("https://antonio@domain.com:180/path/in/child/subpath"), "child/subpath");
      assertStringsEqual(base.relativize("https://antonio@domain.com:180/path/subpath"), "../subpath");
      
      
      assertStringsEqual("../../absolute/path", base.relativize ("https://antonio@domain.com:180/absolute/path"));
      assertStringsEqual("relative/path", base.relativize ("https://antonio@domain.com:180/path/in/relative/path"));
      assertStringsEqual("relative/path#withfragment", base.relativize ("https://antonio@domain.com:180/path/in/relative/path#withfragment"));
      assertStringsEqual("relative/path?withquery#withfragment", base.relativize ("https://antonio@domain.com:180/path/in/relative/path?withquery#withfragment"));
      
      base = new URIx("https://antonio@domain.com:180/path/in/base/#removefragment");
      
      assertStringsEqual("../../../absolute/path", base.relativize ("https://antonio@domain.com:180/absolute/path"));
      assertStringsEqual("relative/path", base.relativize ("https://antonio@domain.com:180/path/in/base/relative/path"));
      assertStringsEqual("relative/path#withfragment", base.relativize ("https://antonio@domain.com:180/path/in/base/relative/path#withfragment"));
      assertStringsEqual("relative/path?withquery#withfragment", base.relativize ("https://antonio@domain.com:180/path/in/base/relative/path?withquery#withfragment"));
      
      assertStringsEqual("../../relative/path?withquery#withfragment", base.relativize ("https://antonio@domain.com:180/path/relative/path?withquery#withfragment"));
      assertStringsEqual("../../relative/path?withquery#withfragment", base.relativize ("https://antonio@domain.com:180/path/relative/path?withquery#withfragment"));
      assertStringsEqual("../../relative/path?withquery", base.relativize ("https://antonio@domain.com:180/path/relative/path?withquery"));
      assertStringsEqual("../../", base.relativize ("https://antonio@domain.com:180/path/"));
   }
   
   public static void testZipResolve() {
      
      // regular
      URIx base = new URIx("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/zip.txt");
      assertStringsEqual(base.resolve ("/absolute/path"), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/absolute/path");
      assertStringsEqual(base.resolve ("relative/path"), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/relative/path");
   
      base = new URIx("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/");
      assertStringsEqual(base.resolve ("/absolute/path"), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/absolute/path");
      assertStringsEqual(base.resolve ("relative/path"), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/relative/path");

      assertStringsEqual(base.resolve ("../../relative/path"), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/relative/path");
      assertStringsEqual(base.resolve ("../.././relative/./path"), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/relative/path");
      assertStringsEqual(base.resolve ("../.././relative/.."), "tgz:https://antonio@domain.com:180/path/to/zip.tgz!/");
      
      // into zip
      base = new URIx("https://antonio@domain.com:180/path/to/zip.tar");
      assertStringsEqual(base.resolve ("tar:!/path/in/zip.txt"), "tar:https://antonio@domain.com:180/path/to/zip.tar!/path/in/zip.txt");
      assertStringsEqual(base.resolve ("tar:jar:zip.jar!/path/in/jar.tar!/funky/chicken.txt"), "tar:jar:https://antonio@domain.com:180/path/to/zip.jar!/path/in/jar.tar!/funky/chicken.txt");
      assertStringsEqual(base.resolve ("tar:other.tar!/path/in/zip.txt"), "tar:https://antonio@domain.com:180/path/to/other.tar!/path/in/zip.txt");
      
      base = new URIx("tar:https://antonio@domain.com:180/path/to/zip.tar!/path/in/tar.jar");
      assertStringsEqual(base.resolve ("jar:!/funky/chicken.txt"), "jar:tar:https://antonio@domain.com:180/path/to/zip.tar!/path/in/tar.jar!/funky/chicken.txt");
      assertStringsEqual(base.resolve ("jar:../out/other.jar!/funky/chicken.txt"), "jar:tar:https://antonio@domain.com:180/path/to/zip.tar!/path/out/other.jar!/funky/chicken.txt");
   }
   
   public static void testZipRelativize() {
      // regular
      URIx base = new URIx("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/zip.txt");
      assertStringsEqual("../../absolute/path", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/absolute/path"));
      assertStringsEqual("relative/path", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/relative/path"));
   
      base = new URIx("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/");
      assertStringsEqual("../../absolute/path", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/absolute/path"));
      assertStringsEqual("relative/path", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/path/in/relative/path"));

      assertStringsEqual("../../relative/path", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/relative/path"));
      assertStringsEqual("../../relative/path", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/relative/path"));
      assertStringsEqual("../../", base.relativize ("tgz:https://antonio@domain.com:180/path/to/zip.tgz!/"));
      
      // into zip
      base = new URIx("https://antonio@domain.com:180/path/to/zip.tar");
      assertStringsEqual("tar:!/path/in/zip.txt", base.relativize ("tar:https://antonio@domain.com:180/path/to/zip.tar!/path/in/zip.txt"));
      assertStringsEqual("tar:jar:zip.jar!/path/in/jar.tar!/funky/chicken.txt", base.relativize ("tar:jar:https://antonio@domain.com:180/path/to/zip.jar!/path/in/jar.tar!/funky/chicken.txt"));
      assertStringsEqual("tar:other.tar!/path/in/zip.txt", base.relativize ("tar:https://antonio@domain.com:180/path/to/other.tar!/path/in/zip.txt"));
      
      base = new URIx("tar:https://antonio@domain.com:180/path/to/zip.tar!/path/in/tar.jar");
      assertStringsEqual("jar:!/funky/chicken.txt", base.relativize ("jar:tar:https://antonio@domain.com:180/path/to/zip.tar!/path/in/tar.jar!/funky/chicken.txt"));
      assertStringsEqual("jar:../out/other.jar!/funky/chicken.txt", base.relativize ("jar:tar:https://antonio@domain.com:180/path/to/zip.tar!/path/out/other.jar!/funky/chicken.txt"));
   }
   
   public static void main (String[] args) {
      testHTMLResolve ();
      testHTMLRelativize ();
      testZipResolve ();
      testZipRelativize ();
      
      System.out.println ("PASSED!!");
   }

}
