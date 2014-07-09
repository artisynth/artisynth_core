package maspack.fileutil;

public interface Crypter {

   public byte[] encrypt(byte[] data) throws Exception;
   public byte[] decrypt(byte[] data) throws Exception;
   
   public String encrypt(String data) throws Exception;
   public String decrypt(String data) throws Exception;
   
}
