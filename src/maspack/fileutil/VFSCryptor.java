package maspack.fileutil;

import org.apache.commons.vfs2.util.Cryptor;

import maspack.crypt.Base64;


public class VFSCryptor implements Cryptor, maspack.crypt.Cryptor {

   Cryptor cryptor;
   
   public VFSCryptor(Cryptor c) {
      this.cryptor = c;
   }
   
   @Override
   public byte[] encrypt(byte[] data) {
      String str = Base64.encode(data);
      String out = encrypt(str);
      return Base64.decode(out);
   }

   @Override
   public byte[] decrypt(byte[] data) {
      String str = Base64.encode(data);
      String out = decrypt(str);
      return Base64.decode(out);
   }

   @Override
   public String encrypt(String data) {
      String out = null;
      try {
         out = cryptor.encrypt(data);
      } catch (Exception e) {
      }
      return out;
   }

   @Override
   public String decrypt(String data) {
      String out = null;
      try {
         out = cryptor.decrypt(data);
      } catch (Exception e) {
      }
      return out;
   }

}
