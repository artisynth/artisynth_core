package maspack.crypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptorBase implements Cryptor {
   
   private static String DEFAULT_CIPHER = "AES";
   private static String DEFAULT_PASSPHRASE = "wG@.9Gz}65qLS}e<%/Vpt-m55$FY[Drq";
   private static String DEFAULT_IV = "0xca3608f50b34c4846560ccfb77f11faa";
   
   private SecretKeySpec keySpec;
   private IvParameterSpec ivSpec;
   private Cipher cipher;
   
   public CryptorBase() {
      this(null, null, null);
   }
   
   public CryptorBase(Cipher cipher, SecretKeySpec keySpec, IvParameterSpec ivSpec) {
      this.cipher = cipher;
      this.keySpec = keySpec;
      this.ivSpec = ivSpec;
   }
   
   protected void setCipher(Cipher cipher) {
      this.cipher = cipher;
   }
   
   protected void setCipher(String cipher) throws NoSuchAlgorithmException, NoSuchPaddingException {
      this.cipher = Cipher.getInstance(cipher);
   }

   protected void setKey(SecretKeySpec keySpec) {
      this.keySpec = keySpec;
   }
   
   public void setKey(byte[] key) {
      if (key == null) {
         key = generateKeyFromPassphrase(DEFAULT_PASSPHRASE);
      }
      if (cipher != null) {
         try {
            int max = Cipher.getMaxAllowedKeyLength(cipher.getAlgorithm())/8;
            // adjust key length to be maximum, repeating key if necessary
            if (key.length > max) {
               key = Arrays.copyOf(key, max);
            } else if (key.length < max) {
               byte[] newkey = new byte[max];
               for (int i=0; i<max; ++i) {
                  newkey[i] = key[i%key.length];
               }
               key = newkey;
            }
            String[] algorithm = cipher.getAlgorithm().split("/");
            keySpec = new SecretKeySpec(key, algorithm[0]);
         } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
         }   
      }
   }
   
   public void setKey(String base64key) throws Exception {
      byte[] key = Base64.decode(base64key);
      setKey(key);
   }

   public void setHexKey(String hexKey) throws Exception {
      byte[] key = Base16.decode(hexKey);
      setKey(key);
   }

   // create an AES key from a SHA-2 hash of a passphrase
   public void setKeyFromPassphrase(String passphrase) {
      byte[] key = generateKeyFromPassphrase(passphrase);
      setKey(key);

   }

   /**
    * Create an AES key from a SHA-2 hash of a passphrase
    * @param passphrase
    * @return generated key
    */
   public static byte[] generateKeyFromPassphrase(String passphrase) {

      MessageDigest digest;
      byte[] key = null;
      
      try {
         digest = MessageDigest.getInstance("SHA-256");
         digest.reset();
         digest.update(passphrase.getBytes());
         key = digest.digest();
      } catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
      } catch (Exception e) {
      }

      return key;

   }
   
   /**
    * Sets initialization vector for algorithm
    * @param ivSpec
    */
   public void setIV(IvParameterSpec ivSpec) {
      this.ivSpec = ivSpec;
   }
   
   /**
    * Sets initialization vector for algorithm
    * @param bytes
    */
   public void setIV(byte[] bytes) {
      ivSpec = new IvParameterSpec(bytes);
   }
   
   /**
    * Sets initialization vector for algorithm with a given
    * length.  The bytes are either trimmed or looped.
    * @param bytes
    * @param len
    */
   public void setIV(byte[] bytes, int len) {
      if (bytes.length > len) {
         ivSpec = new IvParameterSpec(bytes, 0, len);   
      } else {
         byte[] newbytes = Arrays.copyOf(bytes, len);
         for (int i=bytes.length; i<len; ++i) {
            newbytes[i] = bytes[i%bytes.length];
         }
         ivSpec = new IvParameterSpec(newbytes);
      }
      
   }
   
   /**
    * Sets initialization vector
    * @param iv64 base64-encoded bytes
    */
   public void setIV(String iv64) {
      setIV(Base64.decode(iv64));
   }
   
   /**
    * Sets initialization vector
    * @param ivHex base16-encoded bytes
    */
   public void setIVHex(String ivHex) {
      setIV(Base16.decode(ivHex));
   }
   
   // encrypt
   @Override
   public String encrypt(String str) {
      byte[] out = encrypt(str.getBytes()); 
      return Base64.encode(out);
   }

   @Override
   public String decrypt(String str) {
      byte [] out = decrypt(Base64.decode(str));
      return new String(out);
   }

   @Override
   public byte[] encrypt(byte[] data) {
      if (cipher == null) {
         try {
            setCipher(DEFAULT_CIPHER);
         } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to initialize cryptor", e);
         }
      }
      if (keySpec == null) {
         setKeyFromPassphrase(DEFAULT_PASSPHRASE);
      }
      if (ivSpec == null) {
         setIVHex(DEFAULT_IV);
      }
      
      if (ivSpec.getIV().length != keySpec.getEncoded().length) {
         // adjust iv
         setIV(ivSpec.getIV(), keySpec.getEncoded().length);
      }
      
      byte[] out = null;
      try {
         cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
         out = new byte[cipher.getOutputSize(data.length)];
         int outLength = cipher.update(data, 0, data.length, out, 0);
         outLength += cipher.doFinal(out, outLength);
      } catch (Exception e) {
         throw new RuntimeException("encryption failed", e);
      }

      return out;
   }

   @Override
   public byte[] decrypt(byte[] data) {
      if (cipher == null) {
         try {
            setCipher(DEFAULT_CIPHER);
         } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to initialize cryptor", e);
         }
      }
      if (keySpec == null) {
         setKeyFromPassphrase(DEFAULT_PASSPHRASE);
      }
      if (ivSpec == null) {
         setIVHex(DEFAULT_IV);
      }
      
      byte[] out = null;
      try {
         cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
         out = new byte[cipher.getOutputSize(data.length)];
         int outLen = cipher.update(data, 0, data.length, out, 0);
         outLen += cipher.doFinal(out, outLen);
         out = Arrays.copyOf(out, outLen);
      } catch (Exception e) {
         throw new RuntimeException("decryption failed", e);
      }
      return out;
   }

}
