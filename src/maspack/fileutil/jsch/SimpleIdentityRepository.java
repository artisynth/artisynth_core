/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.jsch;

import java.util.Vector;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;

/**
 * Implements a basic IdentityRepository that only allows explicit
 * adding/removing of Identity objects
 * 
 * @author "Antonio Sanchez"
 * Creation date: 24 Oct 2012
 *
 */

public class SimpleIdentityRepository implements IdentityRepository {

   private static final String name = "Simple Identity Repository";
   private Vector<Identity> identities = new Vector<Identity>();

   public SimpleIdentityRepository () {
   }

   public String getName() {
      return name;
   }

   public int getStatus() {
      return RUNNING;
   }

   public synchronized Identity getIdentity(String name) {
      
      if (name == null) {
         return null;
      }
      for (Identity id : identities) {
         if (name.equals(id.getName())) {
            return id;
         }
      }
      return null;
   }
   
   public synchronized Identity getIdentity(int idx) {
      
      if (idx < 0 || idx >= identities.size()) {
         return null;
      }
      return identities.elementAt(idx);
   }
   
   public synchronized Vector<Identity> getIdentities() {
      return new Vector<Identity>(identities);
   }

   public synchronized void add(Identity identity) {
      if (!identities.contains(identity)) {
         identities.addElement(identity);
      }
   }


   public synchronized boolean remove(Identity id) {
      if (id == null)
         return false;

      for (Identity identity : identities) {
         if (id.equals(identity)) {
            identities.removeElement(id);
            id.clear(); // erase private key
            return true;
         }
      }
      return false;
   }

   public synchronized void removeAll() {
      for (Identity identity : identities) {
         identity.clear();
      }
      identities.removeAllElements();
   }

   // prevent explicit adding of blobs
   public synchronized boolean add(byte[] blob) {
      return false;
   }

   //prevent explicit removing of blobs
   public synchronized boolean remove(byte[] blob) {
      return false;
   }
}
