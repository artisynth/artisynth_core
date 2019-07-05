/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.security.PrivilegedAction;

/**
 * Taken from gnu.java.security.action.
 * 
 * PrivilegedAction implementation that calls System.getProperty() with the
 * property name passed to its constructor.
 * 
 * Example of use: <code>
 * GetPropertyAction action = new GetPropertyAction("http.proxyPort");
 * String port = AccessController.doPrivileged(action);
 * </code>
 */
public class GetPropertyAction implements PrivilegedAction<Object> {
   String name;
   String value = null;

   public GetPropertyAction() {
   }

   public GetPropertyAction (String propName) {
      setParameters (propName);
   }

   public GetPropertyAction (String propName, String defaultValue) {
      setParameters (propName, defaultValue);
   }

   public Object run() {
      return System.getProperty (name, value);
   }

   public GetPropertyAction setParameters (String propName) {
      this.name = propName;
      this.value = null;
      return this;
   }

   public GetPropertyAction setParameters (String propName, String defaultValue) {
      this.name = propName;
      this.value = defaultValue;
      return this;
   }
}
