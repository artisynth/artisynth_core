/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.vfs;


import maspack.fileutil.HexCoder;

import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticationData.Type;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

public class EncryptedUserAuthenticator implements UserAuthenticator,
   Comparable<EncryptedUserAuthenticator> {

   String username;
   String domain;
   String encryptedPassword;
   PasswordCryptor myCrypter;

   public EncryptedUserAuthenticator () {
      myCrypter = new PasswordCryptor();
   }
   
   public PasswordCryptor getCryptor() {
      return myCrypter;
   }
   
   public void setCryptor(PasswordCryptor crypt) {
      myCrypter = crypt;
   }
   
   public void setDomain(String domain) {
      this.domain = domain;
   }
   
   public void setUserName(String userName) {
      this.username = userName;
   }
   
   public void setPassword(String password) {
      setPassword(password,isPasswordEncrypted(password));
   }
   public void setPassword(String password, boolean encrypted) {
      if (encrypted == false) {
         try {
            encryptedPassword = myCrypter.encrypt(password);
         } catch (Exception e) {
            e.printStackTrace();
            return;
         }
      } else {
         encryptedPassword = password;
      }
   }

   // best guess as to if password is encrypted
   private boolean isPasswordEncrypted(String pass) {
           
      // hex coded?
      if (!HexCoder.isCoded(pass)) {
         return false;
      }
      
      // can decode?
      try {
         myCrypter.decrypt(pass);
      } catch (Exception e) {
         return false;
      }
      
      return true;
   }
   
   public EncryptedUserAuthenticator (PasswordCryptor crypter) {
      myCrypter = crypter;
   }

   public EncryptedUserAuthenticator (PasswordCryptor crypter, String domain, String username,
      String password) {
      this(crypter);
      setDomain(domain);
      setUserName(username);
      setPassword(password, isPasswordEncrypted(password));
   }

   public EncryptedUserAuthenticator (PasswordCryptor crypter, String domain, String username,
      String password, boolean encrypted) {
      this();
      setCryptor(crypter);
      setUserName(username);
      setDomain(domain);
      setPassword(password, encrypted);
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
               // unfortunately, we seem to have to pass it in plaintext
               data.setData(
                  UserAuthenticationData.PASSWORD,
                  UserAuthenticatorUtils.toChar(
                     myCrypter.decrypt(encryptedPassword)));
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
         str += encryptedPassword + ":";
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

   private static final Type[] ALL_AUTH_DATA =
   { UserAuthenticationData.DOMAIN, UserAuthenticationData.USERNAME,
    UserAuthenticationData.PASSWORD };

   public boolean equals(UserAuthenticator obj) {

      UserAuthenticationData data = obj.requestAuthentication(ALL_AUTH_DATA);

      try {
         String str = new String(data.getData(UserAuthenticationData.DOMAIN));
         if (!equals(str, domain)) {
            return false;
         }
      } catch (NullPointerException e) {
         // no domain
         if (domain != null) {
            return false;
         }
      }

      try {
         String str = new String(data.getData(UserAuthenticationData.USERNAME));
         if (!equals(str, username)) {
            return false;
         }
      } catch (NullPointerException e) {
         // no username
         if (username != null) {
            return false;
         }
      }

      try {
         String str = new String(data.getData(UserAuthenticationData.PASSWORD));
         str = myCrypter.encrypt(str); // encrypt password

         if (!equals(str, encryptedPassword)) {
            return false;
         }
      } catch (Exception e) {
         // no password
         if (encryptedPassword != null) {
            return false;
         }
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
