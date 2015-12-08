/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.vfs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticationData.Type;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

public class ConsoleUserAuthenticator implements UserAuthenticator {

   HashMap<UserAuthenticationData.Type, String> storage = 
      new HashMap<UserAuthenticationData.Type, String>();
   
   public void clear() {
      storage.clear();
   }
   
   public UserAuthenticationData requestAuthentication(Type[] types) {

      UserAuthenticationData data = new UserAuthenticationData();
      System.out.println("Authentication requested...");
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      
      for (int i=0; i<types.length; i++) {
         
         if (types[i] == UserAuthenticationData.DOMAIN) {
            System.out.print("Domain: ");
            String domain = storage.get(UserAuthenticationData.DOMAIN);
            if (domain == null) {
               try {
                  domain = in.readLine();
            //      storage.put(UserAuthenticationData.DOMAIN, domain);
               } catch (IOException e) {
                  e.printStackTrace();
                  continue;
               }
            }
            data.setData(UserAuthenticationData.DOMAIN, UserAuthenticatorUtils.toChar(domain));
         } else if (types[i] == UserAuthenticationData.USERNAME) {
            System.out.print("Username: ");
            String user =  storage.get(UserAuthenticationData.DOMAIN);
            if (user == null) {
               try {
                  user = in.readLine();
                  // storage.put(UserAuthenticationData.USERNAME, user);
               } catch (IOException e) {
                  e.printStackTrace();
                  continue;
               }
            }
            data.setData(UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(user));
         } else if (types[i] == UserAuthenticationData.PASSWORD) {
            System.out.print("Password: ");
            String pass = storage.get(UserAuthenticationData.PASSWORD);
            if (pass == null) {
               try {
                  pass = in.readLine();
           //       storage.put(UserAuthenticationData.PASSWORD, pass);
               } catch (IOException e) {
                  e.printStackTrace();
                  continue;
               }
            }
            data.setData(UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(pass));
         }
         
         
         
      }
      
      return data;
   }

}
