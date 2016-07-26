package maspack.crypt;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class GenericCryptor extends CryptorBase {

   public GenericCryptor() {
      super();
   }
   
   public GenericCryptor(Cipher cipher) {
      super(cipher, null, null);
   }
   
   public GenericCryptor(Cipher cipher, SecretKeySpec keySpec) {
      super(cipher, keySpec, null);
   }
   
   public GenericCryptor(Cipher cipher, SecretKeySpec keySpec, IvParameterSpec ivSpec) {
      super(cipher, keySpec, ivSpec);
   }
   
   @Override
   public void setCipher(Cipher cipher) {
      super.setCipher(cipher);
   }
   
   public void setCipher(String cipher) throws NoSuchAlgorithmException, NoSuchPaddingException {
      super.setCipher(cipher);
   }
   
}
