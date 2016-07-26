package maspack.crypt;

/**
 * Compatible with vfs
 * @author antonio
 *
 */
public interface Cryptor {

   public byte[] encrypt(byte[] data);
   public byte[] decrypt(byte[] data);
   
   public String encrypt(String data);
   public String decrypt(String data);
   
}
