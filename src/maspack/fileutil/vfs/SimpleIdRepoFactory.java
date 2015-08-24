/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.vfs;

import java.io.File;

import maspack.fileutil.jsch.IdentityFile;
import maspack.fileutil.jsch.SimpleIdentityRepository;

import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

/**
 * An "IdentityFactor" that stores a set of private keys to use for
 * authentication. 
 * 
 * @author "Antonio Sanchez" Creation date: 24 Oct 2012
 * 
 */
public class SimpleIdRepoFactory implements IdentityRepositoryFactory {

   private SimpleIdentityRepository myIdRepo;

   public SimpleIdRepoFactory () {
      myIdRepo = new SimpleIdentityRepository();
   }

   public void addIdentity(Identity id) {
      myIdRepo.add(id);
   }
   
   public static void byteErase(byte[] array) {
      for (int i=0; i<array.length; i++) {
         array[i] = 0;
      }
   }

   public static boolean decryptIdentity(Identity id, String passphrase) {
      
      byte[] passbytes = null;
      if (passphrase != null) {
         passbytes = passphrase.getBytes();
      }
      
      boolean success = decryptIdentity(id, passbytes);

      // erase password
      if (passbytes != null) {
         byteErase(passbytes);
      }
      return success;
      
   }
   
   public static boolean decryptIdentity(Identity id, byte[] passphrase) {

      // if encrypted and passphrase supplied, decrypt
      if (id.isEncrypted() && passphrase != null) {
         try {
            id.setPassphrase(passphrase);
         } catch (JSchException e) {
            System.err.println("Decryption failed");
            e.printStackTrace();
         }
      }
      
      return (!id.isEncrypted());
   }

   public static IdentityFile createIdentity(byte [] blob) throws JSchException {
      IdentityFile id = IdentityFile.newInstance("blob", blob, null);
      return id;
   }

   public static IdentityFile createIdentity(String fileName) throws JSchException {
      return createIdentity(new File(fileName));
   }
   
   public static IdentityFile createIdentity(File file) throws JSchException {
      IdentityFile id = IdentityFile.newInstance(file, null);
      return id;
   }
   
   public void setIdentityRepository(SimpleIdentityRepository repo) {
      myIdRepo = repo;
   }
   
   public IdentityRepository getIdentityRepository(){
      return myIdRepo;
   }
   
   public IdentityRepository create(JSch jsch) {
      return myIdRepo;
   }

}
