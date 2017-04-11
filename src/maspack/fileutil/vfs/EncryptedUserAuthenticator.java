/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.vfs;


import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticationData.Type;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.util.CryptorFactory;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import maspack.crypt.Cryptor;
import maspack.fileutil.VFSCryptor;

public class EncryptedUserAuthenticator implements UserAuthenticator, Comparable<EncryptedUserAuthenticator> {

   String username;
   String domain;
   String encryptedPassword;
   Cryptor myCryptor;

   public EncryptedUserAuthenticator () {
      myCryptor = null;
      domain = null;
      username = null;
      encryptedPassword = null;
   }

   public Cryptor getCryptor() {
      if (myCryptor == null) {
         return new VFSCryptor(CryptorFactory.getCryptor());
      }
      return myCryptor;
   }

   /**
    * Set Cryptor for encrypting/decrypting password
    */
   public void setCryptor(Cryptor crypt) {
      myCryptor = crypt;
   }

   public void setDomain(String domain) {
      this.domain = domain;
   }

   public void setUserName(String userName) {
      this.username = userName;
   }

   /**
    * Will encrypt password for internal storage using this authenticator's
    * Cryptor
    */
   public void setPlainPassword(String password) {
      String epass;
      try {
         epass = getCryptor().encrypt(password);
      } catch (Exception e) {
         throw new RuntimeException("Failed to securely store password", e);
      }
      this.encryptedPassword = epass;
   }
   
   /**
    * Set an already encrypted password, which must be able to be decrypted
    * using this authenticator's Cryptor
    */
   public void setEncryptedPassword(String encryptedPassword) {
      this.encryptedPassword = encryptedPassword;
   }

   public EncryptedUserAuthenticator (Cryptor crypter) {
      myCryptor = crypter;
   }

   public EncryptedUserAuthenticator (Cryptor crypter, String domain, String username,
      String password) {
      this(crypter);
      setDomain(domain);
      setUserName(username);
      setPlainPassword(password);
   }

   public EncryptedUserAuthenticator (String domain, String username,
      String password) {
      this();
      setCryptor(null);
      setDomain(domain);
      setUserName(username);
      setPlainPassword(password);
   }

   public EncryptedUserAuthenticator (String username,
      String password) {
      this();
      setCryptor(null);
      setDomain(null);
      setUserName(username);
      setPlainPassword(password);
   }

   public UserAuthenticationData requestAuthentication(
      UserAuthenticationData.Type[] types) {

      UserAuthenticationData data = new UserAuthenticationData();
      for (Type type : types) {
         if (type == UserAuthenticationData.DOMAIN) {
            data.setData(
               UserAuthenticationData.DOMAIN,
               UserAuthenticatorUtils.toChar(domain));
         } else if (type == UserAuthenticationData.USERNAME) {
            data.setData(
               UserAuthenticationData.USERNAME,
               UserAuthenticatorUtils.toChar(username));
         } else if (type == UserAuthenticationData.PASSWORD) {
            try {
               // unfortunately, we have to pass it in plaintext, but the original password
               // could be encrypted from the get-go using the global Cryptor
               String passwd = getCryptor().decrypt(encryptedPassword);
               char[] chars = UserAuthenticatorUtils.toChar(passwd);
               data.setData(
                  UserAuthenticationData.PASSWORD,
                  chars);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
      return data;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      String str = "";
      if (username != null) {
         str += username + ":";
      }
      if (domain != null) {
         str += domain + ":";
      }
      if (encryptedPassword != null) {
         try {
            str += getCryptor().decrypt(encryptedPassword) + ":";
         } catch (Exception e) {
         }
      }

      return str.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (obj instanceof EncryptedUserAuthenticator) {
         return equals((EncryptedUserAuthenticator)obj);
      } else if (obj instanceof UserAuthenticator) {
         return equals((UserAuthenticator)obj);
      }

      return false;
   }

   public boolean equals(EncryptedUserAuthenticator obj) {

      return (equals(this.domain, obj.domain) &&
      equals(this.username, obj.username) && equals(
         this.encryptedPassword, obj.encryptedPassword));
   }

   private static final Type[] ALL_AUTH_DATA = { UserAuthenticationData.DOMAIN, UserAuthenticationData.USERNAME,
                                                 UserAuthenticationData.PASSWORD };

   public boolean equals(UserAuthenticator obj) {

      UserAuthenticationData data = obj.requestAuthentication(ALL_AUTH_DATA);

      String str = new String(data.getData(UserAuthenticationData.DOMAIN));
      if (!equals(str, domain)) {
         return false;
      }

      str = new String(data.getData(UserAuthenticationData.USERNAME));
      if (!equals(str, username)) {
         return false;
      }

      str = new String(data.getData(UserAuthenticationData.PASSWORD));
      try {
         // encrypt password
         str = getCryptor().encrypt(str);
         if (!equals(str, encryptedPassword)) {
            return false;
         }
      } catch (Exception e) {
      } 

      return true;
   }

   private boolean equals(String str1, String str2) {
      if (str1 == null || str2 == null) {
         return str1 == str2;
      }
      return str1.equals(str2);
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(final EncryptedUserAuthenticator other) {

      int result = compareString(domain, other.domain);
      if (result != 0) {
         result = compareString(username, other.username);
         if (result != 0) {
            result = compareString(encryptedPassword, other.encryptedPassword);
         }
      }
      return result;
   }

   private int compareString(final String thisString, final String thatString) {

      if (thisString == null || thatString == null) {
         if (thisString == thatString) {
            return 0;
         } else if (thisString == null) {
            return -1;
         } else {
            return 1;
         }
      }

      return thisString.compareTo(thatString);
   }

   /**
    * {@inheritDoc}
    * 
    */
   @Override
   public String toString()
   {
      String out = "";
      if (domain != null) {
         out += domain + '\\';
      }
      if (username != null) {
         out += username;
      } else {
         out += "(null)";
      }
      if (encryptedPassword != null) {
         out += ":****";
      }
      return out;
   }

}
