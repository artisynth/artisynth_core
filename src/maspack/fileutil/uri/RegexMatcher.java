/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Used for checking if URIs match a certain regex pattern
 * @author "Antonio Sanchez"
 * Creation date: 24 Oct 2012
 *
 */
public class RegexMatcher implements URIxMatcher {

   private class RangeSet {
      private Vector<Integer> min = new Vector<Integer>();
      private Vector<Integer> max = new Vector<Integer>();
      
      public RangeSet() {}
      public RangeSet(int min, int max) {
         addRange(min,max);
      }
      public void addRange(int min, int max) {
         this.min.add(min);
         this.max.add(max);
      }
      public int numIntervals() {
         return min.size();
      }
      public boolean contains(int val) {
         for (int i=0; i<min.size(); i++) {
            if (val >= min.elementAt(i) && val <= max.elementAt(i)) {
               return true;
            }
         }
         return false;
      }
      public String toString() {
         String out = "";
         for (int i=0; i<min.size(); i++) {
            out += min.elementAt(i);
            if (max.elementAt(i) > min.elementAt(i)) {
               out += "-" + max.elementAt(i);
            }
            if (i<min.size()-1) {
               out += ",";
            }
         }
         return out;
      }
      
   }
   
   private Pattern scheme = null;
   private Pattern fragment = null;
   private Pattern userName = null;
   //   private Pattern password = null; // ignore password field
   private Pattern host = null;
   private RangeSet port = null;
   private Pattern path = null;
   private Pattern query = null;

   
   public RegexMatcher() {
   }
   
   public RegexMatcher(String schemePattern, String hostPattern) {
      setSchemePattern(schemePattern);
      setHostPattern(hostPattern);
   }
   public RegexMatcher(String schemePattern, String hostPattern, String pathPattern) {
      setSchemePattern(schemePattern);
      setHostPattern(hostPattern);
      setPathPattern(pathPattern);
   }
   
   public void setSchemePattern(String pattern) {
      scheme = buildPattern(pattern);
   }
   public void setHostPattern(String pattern) {
      host = buildPattern(pattern);
   }
   public void setUserPattern(String pattern) {
      userName = buildPattern(pattern);
   }
   public void setFragmentPattern(String pattern) {
      fragment = buildPattern(pattern);
   }
   // special chars, '-' to separate range, ',' to separate entries
   public void setPortRange(String rangeSet) {

      RangeSet range = new RangeSet();
      
      // search for first number
      int start = 0;
      int end = 0;
      while (start < rangeSet.length()) {
         int min = 0;
         int max = 0;
         start = advanceNumeric(rangeSet, start, false); // advance past non-numeric
         if (start == rangeSet.length()) {
            break;
         }

         // first number
         end = advanceNumeric(rangeSet,start, true); // advance to first non-numeric
         min = Integer.parseInt(rangeSet.substring(start,end));
         if (end==rangeSet.length()) {
            range.addRange(min, min);  // single number
            break;
         }
         
         // check if single digit or range
         if (rangeSet.charAt(end)=='-') {
            // we have a range, so find next integer
            start = advanceNumeric(rangeSet,end,false);
            if (start == rangeSet.length()) {
               range.addRange(min, min);
               break;
            }
            end = advanceNumeric(rangeSet, start, true);
            max = Integer.parseInt(rangeSet.substring(start,end));
            range.addRange(min, max);
         } else {
            range.addRange(min, min);
         }
         start = end+1;
      }
      
      if (range.numIntervals() > 0) {
         port = range;
      }
      
   }
   
   public int advanceNumeric(String buff, int start, boolean numeric) {
      while (isNumeric(buff.charAt(start))==numeric) {
         start++;
         if (start == buff.length()) {
            return buff.length();
         }
      }
      return start;
   }
   
   private boolean isNumeric(char c) {
      return ( c >= '0' && c <='9');
   }
   public void setPortRange(int min, int max) {
      port = new RangeSet(min,max);
   }
   public void addPortRange(int min, int max) {
      if (port == null) {
         port = new RangeSet(min,max);
      } else {
         port.addRange(min, max);
      }
   }
   public void clearPortRange() {
      port = null;
   }
   public void setPathPattern(String pattern) {
      path = buildPattern(pattern);
   }
   public void setQueryPattern(String pattern) {
      query = buildPattern(pattern);
   }
   
   private Pattern buildPattern(String pattern) {
      if (pattern == null) {
         return null;
      } else {
         return Pattern.compile(pattern);
      }
   }
   
   public boolean matches(URIx uri) {
      
      if (!matches(scheme,uri.getScheme().toString())) {
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
      } else if (port != null) {
         if(!port.contains(uri.getPort())) {
            return false;
         }
      }
      return true;
      
   }
   
   private boolean matches(Pattern p, String val) {
      if (p == null) {
         return true;
      }
      java.util.regex.Matcher m = p.matcher(val);
      return m.matches();
   }
   
   public String toString() {
      
      String out = "";
      if (scheme != null) {
         out += scheme.toString();
      } else {
         out += ".*";
      }
      out += ":";
      
      if (userName != null) {
         out += userName.toString();
         out += "@";
      } else {
         out += ".*";
      }
      
      if (host != null) {
         out += host.toString();
      } else {
         out += ".*";
      }
      
      if (port != null) {
         out += ":["+port.toString()+"]";
      } else {
         out += ".*";
      }
      
      if (path != null) {
         if (!path.toString().startsWith("/")) {
            out += "/";
         }
         out += path;
      } else {
         out += ".*";
      }
      if (query != null) {
         out += "?";
         out += query.toString();
      } else {
         out += ".*";
      }
      
      if (fragment != null) {
         out += "#";
         out += fragment.toString();
      } else {
         out += ".*";
      }
      
      return out;
   }
   
   
}
