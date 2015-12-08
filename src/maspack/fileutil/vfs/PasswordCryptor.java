/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.vfs;

import maspack.fileutil.AESCrypter;

import org.apache.commons.vfs2.util.Cryptor;

/**
 * Wrapper around AESCrypter for use with apache commons vfs
 * @author "Antonio Sanchez"
 * Creation date: 29 Oct 2012
 *
 */
public class PasswordCryptor extends AESCrypter implements Cryptor {

   public PasswordCryptor() {
      super();
   }
   
   public PasswordCryptor(String passphrase) throws Exception {
      super(passphrase);
   }
   
   public PasswordCryptor(byte[] passphrase) throws Exception {
      super(passphrase);
   }

}
