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
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

public class SimpleUserAuthenticator implements UserAuthenticator,
   Comparable<SimpleUserAuthenticator> {

   String username;
   String domain;
   String password;

   public SimpleUserAuthenticator () {
      domain = null;
      username = null;
      password = null;
   }
   
   public void setDomain(String domain) {
      this.domain = domain;
   }
   
   public void setUserName(String userName) {
      this.username = userName;
   }
   
   public void setPassword(String password) {
      this.password = password;
   }
      
   
   public SimpleUserAuthenticator (String domain, String username,
      String password) {
      setDomain(domain);
      setUserName(username);
      setPassword(password);
   }
   
   /**
    * This is a test javadoc
    * @param username The user's name
    * @param password The user's password
    */
   public SimpleUserAuthenticator (String username,
      String password) {
      setDomain(null);
      setUserName(username);
      setPassword(password);
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
                  UserAuthenticatorUtils.toChar(password));
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
      if (password != null) {
         str += password + ":";
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

      if (obj instanceof SimpleUserAuthenticator) {
         return equals((SimpleUserAuthenticator)obj);
      } else if (obj instanceof SimpleUserAuthenticator) {
         return equals((SimpleUserAuthenticator)obj);
      }

      return false;
   }

   public boolean equals(SimpleUserAuthenticator obj) {

      return (equals(this.domain, obj.domain) &&
         equals(this.username, obj.username) && equals(
            this.password, obj.password));
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

         if (!equals(str, password)) {
            return false;
         }
      } catch (Exception e) {
         // no password
         if (password != null) {
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
   public int compareTo(final SimpleUserAuthenticator other) {

      int result = compareString(domain, other.domain);
      if (result != 0) {
         result = compareString(username, other.username);
         if (result != 0) {
            result = compareString(password, other.password);
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
      if (password != null) {
         out += ":****";
      }
      return out;
   }

}
