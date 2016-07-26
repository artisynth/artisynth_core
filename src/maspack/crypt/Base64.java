package maspack.crypt;

/**
 * Base64 encoding (RFC 4648 and RFC 2045), modified from source on 
 *     https://en.wikipedia.org/wiki/Base64
 *
 */
public class Base64 {

   private static final String CODES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

   public static byte[] decode(String input)    {

      if (input.length() % 4 != 0)    {
         throw new IllegalArgumentException("Invalid base64 input");
      }

      int equalidx = input.indexOf('=');
      byte decoded[] = new byte[((input.length() * 3) / 4) - (equalidx >= 0 ? (input.length() - equalidx) : 0)];
      char[] inChars = input.toCharArray();
      int j = 0;
      int b[] = new int[4];
      for (int i = 0; i < inChars.length; i += 4)     {
         // This could be made faster (but more complicated) by precomputing these index locations.
         b[0] = CODES.indexOf(inChars[i]);
         b[1] = CODES.indexOf(inChars[i + 1]);
         b[2] = CODES.indexOf(inChars[i + 2]);
         b[3] = CODES.indexOf(inChars[i + 3]);
         decoded[j++] = (byte) ((b[0] << 2) | (b[1] >> 4));
         if (b[2] < 64)      {
            decoded[j++] = (byte) ((b[1] << 4) | (b[2] >> 2));
            if (b[3] < 64)  {
               decoded[j++] = (byte) ((b[2] << 6) | b[3]);
            }
         }
      }

      return decoded;
   }

   public static String encode(byte[] in)       {
      StringBuilder out = new StringBuilder((in.length * 4) / 3);
      int b;
      for (int i = 0; i < in.length; i += 3)  {
         b = (in[i] & 0xFC) >> 2;
      out.append(CODES.charAt(b));
      b = (in[i] & 0x03) << 4;
      if (i + 1 < in.length)      {
         b |= (in[i + 1] & 0xF0) >> 4;
      out.append(CODES.charAt(b));
      b = (in[i + 1] & 0x0F) << 2;
      if (i + 2 < in.length)  {
         b |= (in[i + 2] & 0xC0) >> 6;
      out.append(CODES.charAt(b));
      b = in[i + 2] & 0x3F;
      out.append(CODES.charAt(b));
      } else  {
         out.append(CODES.charAt(b));
         out.append('=');
      }
      } else      {
         out.append(CODES.charAt(b));
         out.append("==");
      }
      }

      return out.toString();
   }

}
