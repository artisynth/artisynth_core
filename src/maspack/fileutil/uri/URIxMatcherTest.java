/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

public class URIxMatcherTest {

   public static void doTest() {

      // create a "matcher"
      RegexMatcher uriMatch = new RegexMatcher();
      uriMatch.setPortRange("10-44, 140, 22-158");
      uriMatch.setHostPattern(".*\\.ece\\.ubc\\.ca");
      uriMatch.setSchemePattern(".*ftp");
      System.out.println(uriMatch.toString());
      
      URIx test1,test2,test3,test4,test5;
      try {
         test1 = new URIx("ftp://www.ece.ubc.ca:23/hello.txt");
         test2 = new URIx("sftp://wwwaece.ubc.ca:23/hello.txt");
         test3 = new URIx("sftp://www.ece.ubc.ca:162/hello.txt");
         test4 = new URIx("ftp://www.ece.ubc.ca:150/hello.txt");
         test5 = new URIx("http://www.ece.ubc.ca:150/hello.txt");
      } catch (URIxSyntaxException e) {
         e.printStackTrace();
         return;
      }

      System.out.println(uriMatch.matches(test1));
      System.out.println(uriMatch.matches(test2));
      System.out.println(uriMatch.matches(test3));
      System.out.println(uriMatch.matches(test4));
      System.out.println(uriMatch.matches(test5));
   }

   public static void main(String[] args) {
      doTest();
   }

}
