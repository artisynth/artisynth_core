/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

/**
 * Used for checking if URIs match exact conditions
 * @author "Antonio Sanchez"
 * Creation date: 24 Oct 2012
 *
 */
public class ExactMatcher implements URIxMatcher {
   
   private URIxScheme scheme = null;
   private String fragment = null;
   private String userName = null;
   private String host = null;
   private int port = -1;
   private String path = null;
   private String query = null;

   
   public ExactMatcher() {
   }
   
   public ExactMatcher(URIxScheme scheme, String host) {
      setScheme(scheme);
      setHost(host);
   }
   
   public ExactMatcher(String scheme, String host) {
      setScheme(scheme);
      setHost(host);
   }
   
   public ExactMatcher(URIxScheme scheme, String host, String path) {
      setScheme(scheme);
      setHost(host);
      setPath(path);
   }
   
   public ExactMatcher(String scheme, String host, String path) {
      setScheme(scheme);
      setHost(host);
      setPath(path);
   }
   
   public void setScheme(URIxScheme scheme) {
      this.scheme = scheme;
   }
   public void setScheme(String pattern) {
      scheme = URIxScheme.findScheme(pattern);
   }
   public void setHost(String pattern) {
      host = pattern;
   }
   public void setUser(String pattern) {
      userName = pattern;
   }
   public void setFragment(String pattern) {
      fragment = pattern;
   }

   public void setPort(int port) {
      this.port = port;
   }
   
   public void setPath(String pattern) {
      path = pattern;
   }
   public void setQuery(String pattern) {
      query = pattern;
   }
   
   public boolean matches(URIx uri) {
      
      if (!matches(scheme, uri.getScheme())) {
         return false;
      } else if (!matches(fragment, uri.getFragment())) {
         return false;
      } if (!matches(userName, uri.getUserName())) {
         return false;
      } else if (!matches(host,uri.getHost())) {
         return false;
      } else if (!matches(path, uri.getPath())) {
         return false;
      } else if (!matches(query, uri.getQuery())) {
         return false;
      } else if (!matches(port, uri.getPort())) {
         return false;
      }
      return true;
      
   }
   
   private static boolean matches(String p, String val) {
      if (p == null) {
         return true;
      }
      return p.equals(val);
   }
   
   private static boolean matches(int p, int val) {
      if (p <0) {
         return true;
      }
      return (p==val);
   }
   
   private static boolean matches(URIxScheme scheme, URIxScheme val) {
      if (scheme == null) {
         return true;
      }
      return scheme.equals(val);
   }
   
   public String toString() {
      
      String out = "";
      if (scheme != null) {
         out += scheme.toString();
      } else {
         out += "*";
      }
      out += ":";
      
      if (userName != null) {
         out += userName.toString();
         out += "@";
      } else {
         out += "*";
      }
      
      if (host != null) {
         out += host.toString();
      } else {
         out += "*";
      }
      
      if (port >=0 ) {
         out += ":"+port;
      } else {
         out += "*";
      }
      
      if (path != null) {
         if (!path.toString().startsWith("/")) {
            out += "/";
         }
         out += path;
      } else {
         out += "*";
      }
      if (query != null) {
         out += "?";
         out += query.toString();
      } else {
         out += "*";
      }
      
      if (fragment != null) {
         out += "#";
         out += fragment.toString();
      } else {
         out += "*";
      }
      
      return out;
   }
   
   
}
