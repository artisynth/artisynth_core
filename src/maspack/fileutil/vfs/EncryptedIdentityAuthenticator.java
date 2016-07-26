/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.vfs;

import java.io.File;

import maspack.crypt.AESCryptor;
import maspack.crypt.Base16;

public class EncryptedIdentityAuthenticator  {

   private static final String UTF8="UTF-8"; 
   File identity = null;
   byte[] encryptedPassword = null;
   String user = null;
   AESCryptor myCrypter = null;

   public EncryptedIdentityAuthenticator (AESCryptor crypter) {
      myCrypter = crypter;
   }

   public EncryptedIdentityAuthenticator (AESCryptor crypter, File privateKey, 
      String username, String password) {
      this(crypter, privateKey, username, password, false);
   }

   public EncryptedIdentityAuthenticator (AESCryptor crypter, File privateKey, String username,
      String password, boolean plainPassword) {
      this(crypter);
      this.user = username;
      if (plainPassword == true) {
         try {
            encryptedPassword = myCrypter.encrypt(password.getBytes(UTF8));
         } catch (Exception e) {
            e.printStackTrace();
            return;
         }
      } else {
         encryptedPassword = Base16.decode(password);
      }
      this.identity = privateKey;
   }

   public byte[] requestAuthentication() {
      try {
         return myCrypter.decrypt(encryptedPassword);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
  
}
