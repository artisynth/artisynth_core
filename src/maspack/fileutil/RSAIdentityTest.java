/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.File;

import maspack.crypt.AESCryptor;
import maspack.fileutil.jsch.IdentityFile;
import maspack.fileutil.vfs.SimpleIdRepoFactory;

import com.jcraft.jsch.JSchException;

public class RSAIdentityTest {

   public static void doTest() {
   
      File sshKey = new File("src/maspack/datautil/data/artisynth_rsa");
      IdentityFile id;
      try {
         id = SimpleIdRepoFactory.createIdentity(sshKey);
      } catch (JSchException e) {
         e.printStackTrace();
         return;
      }
      

      System.out.println(sshKey.getPath() + " encrypted: " + id.isEncrypted());
      String passphrase = "artisynth";
      byte[] passbytes =
         new byte[] { 'a', 'r', 't', 'i', 's', 'y', 'n', 't', 'h' };

      byte[] key = AESCryptor.generateKeyFromPassphrase("dolly");
      AESCryptor crypt;
      try {
         crypt = new AESCryptor(key);
         String encrypted = crypt.encrypt(passphrase);
         String decrypted = crypt.decrypt(encrypted);
         System.out.printf("Orig: %s, Encrypted: %s, Decrypted: %s \n", passphrase, encrypted, decrypted);
      } catch (Exception e) {
         e.printStackTrace();
         return;
      }

      boolean success = SimpleIdRepoFactory.decryptIdentity(id, passphrase);
      if (success) {
         System.out.println("success");
      }
      System.out.println(sshKey.getPath() + " encrypted: " + id.isEncrypted());

      sshKey = new File("src/maspack/datautil/data/plain_rsa");
      System.out.println(sshKey.getPath() + " encrypted: " + id.isEncrypted());
      SimpleIdRepoFactory.decryptIdentity(id, passbytes);
      System.out.println(sshKey.getPath() + " encrypted: " + id.isEncrypted());
      
   }
   
   public static void main(String args[]) {
      doTest();
   }
   
}
