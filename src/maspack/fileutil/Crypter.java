/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

public interface Crypter {

   public byte[] encrypt(byte[] data) throws Exception;
   public byte[] decrypt(byte[] data) throws Exception;
   
   public String encrypt(String data) throws Exception;
   public String decrypt(String data) throws Exception;
   
}
