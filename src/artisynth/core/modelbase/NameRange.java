/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import artisynth.core.util.*;
import maspack.util.*;

/**
 * Performs checks to see if a name for a ModelComponent is valid.  The main
 * criteria are that (a) the name does not contain invalid characters, and (b)
 * the name is unique among the component's siblings in the component
 * hierarchy. Enforcement of the second criteria depends on static settings in
 * {@link ModelComponentBase}.
 */
public class NameRange extends RangeBase {

   // component to be named
   ModelComponent myComp; 
   // parent of the component; used in case comp is not yet connected
   CompositeComponent myParent; 
      
   private CompositeComponent getParent() {
      CompositeComponent parent = null;
      if (myComp != null) {
         if (ModelComponentBase.mustHaveUniqueName (myComp)) {
            parent = myComp.getParent();
            if (parent == null) {
               parent = myParent;
            }
         }
      }
      return parent;
   }
      
   private boolean nameInUse (String name, CompositeComponent parent) {
      ModelComponent c = parent.get(name);
      return c != null && c != myComp;
   }
      
   public NameRange() {
      this (null, null);
   }

   public NameRange (ModelComponent comp) {
      this (comp, null);
   }

   public NameRange (ModelComponent comp, CompositeComponent parent) {
      myComp = comp;         
      myParent = parent;         
   }

   private String getTrailingDigits (String name) {
      if (Character.isDigit (name.charAt (name.length()-1))) {
         StringBuilder str = new StringBuilder();
         int idx = name.length()-1;
         char c = name.charAt (idx);
         str.append (c);
         while (--idx > 0 && Character.isDigit ((c = name.charAt(idx)))) {
            str.insert (0, c);
         }
         return str.toString();
      }
      return null;
   }
      
   /**
    * Returns the valid name (passes {@link #isValid(Object, StringHolder)}).
    */
   public String makeValid (Object obj) {
      if (obj == null) {
         return null;
      } else if (obj instanceof String) {
         String name = (String)obj;

         if (name.length() == 0) {
            return name;
         }
         if (Character.isDigit (name.charAt (0))) {
            name = "_" + name;
            return makeValid(name);
         }
         CompositeComponent parent = getParent();
         if (parent != null) {
            if (nameInUse (name, parent)) {
               String endNum = getTrailingDigits (name);
               int cnt = 1;
               String base = name;
               if (endNum != null) {
                  cnt = Integer.parseInt(endNum);
                  base = name.substring (0, name.length()-endNum.length());
               }
               do {
                  name = base + (++cnt);
               }
               while (nameInUse (name, parent));
               // since name was in use, assume modified name will be OK
               return name;
            }
         }
         if (name.equals ("null")) {
            return "Null"; // fix by just capitalizing the first letter
         }
         boolean modified = false;
         StringBuilder str = new StringBuilder(name.length());
         for (int i=0; i<name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.' || c == '/' || c == ':' || c == '*' || c == '?') {
               str.append('_');
               modified = true;
            } else {
               str.append(c);
            }
         }
         if (modified) {
            return makeValid(str.toString());
         }
         else {
            return name;
         }
      }
      else {
         return null;
      }

   }
      
   public boolean isValid (Object obj, StringHolder errMsg) {
      if (obj == null) {
         return true;
      }
      else if (obj instanceof String) {
         String name = (String)obj;

         if (name.length() == 0) {
            return true;
         }
         if (Character.isDigit (name.charAt (0))) {
            setError (errMsg, "Component names must not begin with a digit");
            return false;
         }
         CompositeComponent parent = getParent();
         if (parent != null) {
            if (nameInUse (name, parent)) {
               setError (errMsg, 
                         "Parent has another component named '"+name+"'");
               return false;
            }
         }
         if (name.equals ("null")) {
            setError (errMsg, 
                      "Component names may not be set to the string \"null\"");
            return false;
         }
         for (int i=0; i<name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.' || c == '/' || c == ':' || c == '*' || c == '?') {
               setError (
                  errMsg, 
                  "Component names must not contain '.', '/', ':', '*', or '?'");
               return false;
            }
         }
         return true;
      }
      else {
         return false;
      }
   }      
}
