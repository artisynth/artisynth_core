/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
 Copyright (c) 2002-2012 ymnk, JCraft,Inc. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright 
 notice, this list of conditions and the following disclaimer in 
 the documentation and/or other materials provided with the distribution.

 3. The names of the authors may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
 INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Modified by Antonio Sanchez, Oct 23, 2012
 * Trimmed to include only parts used by KeyPair classes
 */

package maspack.fileutil.jsch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.HASH;

public class UtilTrim {

   private static final byte[] b64 =
      str2byte("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=");

   private static byte val(byte foo) {
      if (foo == '=')
         return 0;
      for (int j = 0; j < b64.length; j++) {
         if (foo == b64[j])
            return (byte)j;
      }
      return 0;
   }

   public static byte[] fromBase64(byte[] buf, int start, int length) {
      byte[] foo = new byte[length];
      int j = 0;
      for (int i = start; i < start + length; i += 4) {
         foo[j] = (byte)((val(buf[i]) << 2) | ((val(buf[i + 1]) & 0x30) >>> 4));
         if (buf[i + 2] == (byte)'=') {
            j++;
            break;
         }
         foo[j + 1] =
            (byte)(((val(buf[i + 1]) & 0x0f) << 4) | ((val(buf[i + 2]) & 0x3c) >>> 2));
         if (buf[i + 3] == (byte)'=') {
            j += 2;
            break;
         }
         foo[j + 2] =
            (byte)(((val(buf[i + 2]) & 0x03) << 6) | (val(buf[i + 3]) & 0x3f));
         j += 3;
      }
      byte[] bar = new byte[j];
      System.arraycopy(foo, 0, bar, 0, j);
      return bar;
   }

   public static byte[] toBase64(byte[] buf, int start, int length) {

      byte[] tmp = new byte[length * 2];
      int i, j, k;

      int foo = (length / 3) * 3 + start;
      i = 0;
      for (j = start; j < foo; j += 3) {
         k = (buf[j] >>> 2) & 0x3f;
         tmp[i++] = b64[k];
         k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
         tmp[i++] = b64[k];
         k = (buf[j + 1] & 0x0f) << 2 | (buf[j + 2] >>> 6) & 0x03;
         tmp[i++] = b64[k];
         k = buf[j + 2] & 0x3f;
         tmp[i++] = b64[k];
      }

      foo = (start + length) - foo;
      if (foo == 1) {
         k = (buf[j] >>> 2) & 0x3f;
         tmp[i++] = b64[k];
         k = ((buf[j] & 0x03) << 4) & 0x3f;
         tmp[i++] = b64[k];
         tmp[i++] = (byte)'=';
         tmp[i++] = (byte)'=';
      }
      else if (foo == 2) {
         k = (buf[j] >>> 2) & 0x3f;
         tmp[i++] = b64[k];
         k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
         tmp[i++] = b64[k];
         k = ((buf[j + 1] & 0x0f) << 2) & 0x3f;
         tmp[i++] = b64[k];
         tmp[i++] = (byte)'=';
      }
      byte[] bar = new byte[i];
      System.arraycopy(tmp, 0, bar, 0, i);
      return bar;

      // return sun.misc.BASE64Encoder().encode(buf);
   }

   private static String[] chars = {
                                    "0", "1", "2", "3", "4", "5", "6", "7",
                                    "8", "9", "a", "b", "c", "d", "e", "f"
   };

   public static String getFingerPrint(HASH hash, byte[] data) {
      try {
         hash.init();
         hash.update(data, 0, data.length);
         byte[] foo = hash.digest();
         StringBuffer sb = new StringBuffer();
         int bar;
         for (int i = 0; i < foo.length; i++) {
            bar = foo[i] & 0xff;
            sb.append(chars[(bar >>> 4) & 0xf]);
            sb.append(chars[(bar) & 0xf]);
            if (i + 1 < foo.length)
               sb.append(":");
         }
         return sb.toString();
      } catch (Exception e) {
         return "???";
      }
   }

   public static byte[] str2byte(String str, String encoding) {
      if (str == null)
         return null;
      try {
         return str.getBytes(encoding);
      } catch (java.io.UnsupportedEncodingException e) {
         return str.getBytes();
      }
   }

   public static byte[] str2byte(String str) {
      return str2byte(str, "UTF-8");
   }

   public static String byte2str(byte[] str, String encoding) {
      return byte2str(str, 0, str.length, encoding);
   }

   public static String byte2str(byte[] str, int s, int l, String encoding) {
      try {
         return new String(str, s, l, encoding);
      } catch (java.io.UnsupportedEncodingException e) {
         return new String(str, s, l);
      }
   }

   public static String byte2str(byte[] str) {
      return byte2str(str, 0, str.length, "UTF-8");
   }

   public static String byte2str(byte[] str, int s, int l) {
      return byte2str(str, s, l, "UTF-8");
   }

   public static void bzero(byte[] foo) {
      if (foo == null)
         return;
      for (int i = 0; i < foo.length; i++)
         foo[i] = 0;
   }

   public static byte[] fromStream(InputStream in, long length) throws IOException {
      
      try {
         byte[] result = new byte[(int)(length)];
         int len = 0;
         while (true) {
            int i = in.read(result, len, result.length - len);
            if (i <= 0)
               break;
            len += i;
         }
         in.close();
         return result;
      } finally {
         if (in != null)
            in.close();
      }
      
   }
   
   public static byte[] fromFile(File file) throws IOException {
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(file);
      } catch (FileNotFoundException e) {
         return null;
      }
      return fromStream(fis, file.length());
   }
   
   public static byte[] fromFile(String _file) throws IOException {
      File file = new File(_file);
      return fromFile(file);
      
   }

   public static boolean array_equals(byte[] foo, byte bar[]) {
      int i = foo.length;
      if (i != bar.length) {
         return false;
      } else {
         for (int j = 0; j < i; j++) {
            if (foo[j] != bar[j]) {
               return false;
            }
         }
      }

      return true;
   }
}
