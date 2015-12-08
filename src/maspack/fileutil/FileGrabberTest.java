/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.File;

import maspack.fileutil.jsch.IdentityFile;
import maspack.fileutil.jsch.SimpleIdentityRepository;
import maspack.fileutil.uri.ExactMatcher;
import maspack.fileutil.vfs.ConsoleUserAuthenticator;
import maspack.fileutil.vfs.EncryptedUserAuthenticator;
import maspack.fileutil.vfs.PasswordCryptor;
import maspack.fileutil.vfs.SimpleIdRepoFactory;
import maspack.util.PathFinder;
import maspack.util.Logger.LogLevel;

import com.jcraft.jsch.JSchException;

public class FileGrabberTest {

   String localDir = PathFinder.expand ("${srcdir FileGrabberTest}/");
   
   public static void main(String args[]) {
      FileGrabberTest test = new FileGrabberTest();
      test.run();
   }

   private void run() {
      
      FileGrabber grabber = new FileGrabber(localDir+"cache/", "file://" + localDir);
      grabber.setVerbosityLevel(LogLevel.TRACE);
      
      grabber.addTransferListener(new DefaultConsoleFileTransferListener());
      
      addAuthenticators(grabber);

      File elem1=null, node1=null, elem2=null, node2=null, elem3=null, node3=null;
      try {
         elem1 = grabber.get("mesh1.elem", "http://dl.dropbox.com/u/64872258/mesh.elem");
         node1 = grabber.get("mesh1.node", "https://www.ece.ubc.ca/~antonios/priv/mesh.node");
         elem2 = grabber.get("mesh2.elem", "tgz:data/mesh.tar.gz!/mesh.elem");
         node2 = grabber.get("mesh2.node", "tgz:sftp://artisynth@shellmix.com/files/mesh.tar.gz!/mesh.node");
         elem3 = grabber.get("masseter/mesh.elem", "tgz:sftp://artisynth@shellmix.com/files/mesh2.tar.gz!/mesh.elem"); 
         node3 = grabber.get("masseter/mesh.node", "tgz:sftp://artisynth@shellmix.com/files/mesh2.tar.gz!/mesh.node");
         
      } catch (FileGrabberException e) {
         e.printStackTrace();
      }
      
      System.out.println("\nLocal file locations:");
      
      if (elem1 != null) System.out.println("  " + elem1.getAbsolutePath());
      if (node1 != null) System.out.println("  " + node1.getAbsolutePath());
      if (elem2 != null) System.out.println("  " + elem2.getAbsolutePath());
      if (node2 != null) System.out.println("  " + node2.getAbsolutePath());
      if (elem3 != null) System.out.println("  " + elem3.getAbsolutePath());
      if (node3 != null) System.out.println("  " + node3.getAbsolutePath());

   }

   private void addAuthenticators(FileGrabber fileGrabber) {
      
      byte [] masterKey = PasswordCryptor.generateKeyFromPassphrase("helloworld", PasswordCryptor.AES128);
      PasswordCryptor SystemCryptor;
      try {
         SystemCryptor = new PasswordCryptor(masterKey);
      } catch (Exception e) {
         e.printStackTrace();
         return;
      }
      
      // console authenticator for https
      ConsoleUserAuthenticator conAuth = new ConsoleUserAuthenticator();
      ExactMatcher eceMatcher = new ExactMatcher("https", "www.ece.ubc.ca");
      fileGrabber.addUserAuthenticator(eceMatcher, conAuth);

      // encrypted authenticator for ssh
      EncryptedUserAuthenticator encAuth = new EncryptedUserAuthenticator(SystemCryptor, null, "artisynth", "artisynth");
      ExactMatcher artiMatcher = new ExactMatcher("sftp","shellmix.com");
      artiMatcher.setUser("artisynth"); // also force user to be artisynth
      fileGrabber.addUserAuthenticator(artiMatcher, encAuth);
      
      // identity stuff
      IdentityFile id1;
      IdentityFile id2;
      try {
         id1 = IdentityFile.newInstance(new File(localDir + "data/artisynth_rsa"), null);
         id2 = IdentityFile.newInstance(new File(localDir + "data/plain_rsa"), null);
      } catch (JSchException e) {
         e.printStackTrace();
         return;
      }
      SimpleIdentityRepository repo1 = new SimpleIdentityRepository();
      repo1.add(id1);
      SimpleIdRepoFactory.decryptIdentity(id1, "artisynth");
      
      SimpleIdentityRepository repo2 = new SimpleIdentityRepository();
      repo2.add(id2);

      ExactMatcher repo1Matcher = new ExactMatcher("sftp", "shellmix.com","/files/mesh2.tar.gz");
      repo1Matcher.setFragment("/mesh.elem");
      
      ExactMatcher repo2Matcher = new ExactMatcher("sftp", "shellmix.com","/files/mesh2.tar.gz");
      repo2Matcher.setFragment("/mesh.node");
      
      fileGrabber.addIdentityRepository(repo1Matcher, repo1);
      fileGrabber.addIdentityRepository(repo2Matcher, repo2);
      
   }

}
