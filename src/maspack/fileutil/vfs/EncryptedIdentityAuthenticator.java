package maspack.fileutil.vfs;

import java.io.File;

import maspack.fileutil.AESCrypter;
import maspack.fileutil.HexCoder;

public class EncryptedIdentityAuthenticator  {

   private static final String UTF8="UTF-8"; 
   File identity = null;
   byte[] encryptedPassword = null;
   String user = null;
   AESCrypter myCrypter = null;

   public EncryptedIdentityAuthenticator (AESCrypter crypter) {
      myCrypter = crypter;
   }

   public EncryptedIdentityAuthenticator (AESCrypter crypter, File privateKey, 
      String username, String password) {
      this(crypter, privateKey, username, password, false);
   }

   public EncryptedIdentityAuthenticator (AESCrypter crypter, File privateKey, String username,
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
         encryptedPassword = HexCoder.decode(password);
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
