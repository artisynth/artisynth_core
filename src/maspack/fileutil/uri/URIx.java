/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Extended URI class that handles nested URIs required for zip files
 * @author antonio
 *
 */
public class URIx {

   // General Syntax:
   // --------------------------
   // zip: <scheme>:<scheme-specific-part>![<fragment>]
   // Other: [<scheme>:]<scheme-specific-part>[#<fragment>]
   //
   // The scheme-specific-part:
   // ---------------------------
   // Zip: <nested-URI>
   // "file": //[<host>]</path>
   // Others: //<authority>[</path>][?<query>]
   //
   // Authority:
   // --------------------------
   // [<user-info>@]<host>[:<port>]
   //
   // User Info:
   // ---------------------------
   // <username>[:<password>]
   // NOTE: password field is technically deprecated, and shouldn't be used

   // From the standards (RFC3986):
   // gen-delims = ":/?#[]@"
   // sub-delims = "!$&'()*+,;="
   // pchar = unreserved, %-encoded, sub-delims, ":@"
   // reserved = gen-delims, sub-delims
   // alpha = "a-zA-z"
   // digit = "0-9"
   //
   // scheme = alpha *( alpha,digit, "+-." ), terminated by anything else
   // authority begins with "//", terminated by "/?#"
   // userinfo = *( unreserved, %-encoded, sub-delims, ":" )

   // path terminated by "?#"
   // query = *( pchar, "/?" )
   // query indicated by the first "?", terminated "#"
   // fragment = *( pchar , "/?" )

   private URIxScheme scheme;
   private String fragment;
   private String userName;
   private String password;
   private String host;
   private int port;
   private String path;
   private String query;
   private URIx nestedURI;

   private static final String PATH_RESERVED = "?#[]@ ";
   private static final String STANDARD_RESERVED = "/?#[]@ ";
   private static final String PASSWORD_RESERVED = ":/?#[]@ ";
   private static final String QUERY_RESERVED = "+/?#[]";
   private static final String FRAGMENT_RESERVED = "/?#[] ";
   // private static final String ZIP_FRAGMENT_RESERVED = "!/?#[]";
   private static final char SEP = '/';
   private static final char DOT = '.';
  
   // percent-encoded chars
   static final HashMap<Character,String> PCHARS = new HashMap<>();
   static {
      PCHARS.put('!', "%21");
      PCHARS.put('#', "%23");
      PCHARS.put('$', "%24");
      PCHARS.put('%', "%25");
      PCHARS.put('&', "%26");
      PCHARS.put('\'', "%27");
      PCHARS.put('(', "%28");
      PCHARS.put(')', "%29");
      PCHARS.put('*', "%2A");
      PCHARS.put('+', "%2B");
      PCHARS.put(',', "%2C");
      PCHARS.put('/', "%2F");
      PCHARS.put(':', "%3A");
      PCHARS.put(';', "%3B");
      PCHARS.put('=', "%3D");
      PCHARS.put('?', "%3F");
      PCHARS.put('@', "%40");
      PCHARS.put('[', "%5B");
      PCHARS.put(']', "%5D");
      PCHARS.put(' ', "%20");
   }

   static final HashMap<String,Character> IPCHARS = new HashMap<>();
   static {
      for (Entry<Character,String> entry : PCHARS.entrySet ()) {
         IPCHARS.put (entry.getValue (), entry.getKey ());
      }
   }
   /**
    * Creates empty relative URI
    */
   public URIx () {
      clear();
   }

   /**
    * Copies a URI
    * @param uri uri to copy
    */
   public URIx (URIx uri) {
      if (uri == null) {
         clear();
      } else {
         set(uri);
      }
   }
  
   /**
    * Sets this URI to the provided one
    * @param uri URI to copy
    */
   public void set (URIx uri) {
      setScheme(uri.getScheme());
      setRawFragment(uri.getRawFragment());
      setRawUserName(uri.getRawUserName());
      setRawPassword(uri.getRawPassword());
      setRawHost(uri.getRawHost());
      setPort(uri.getPort());
      setRawPath(uri.getRawPath());
      setRawQuery(uri.getRawQuery());
      if (uri.getNestedURI() != null) {
         // copy nested
         setNestedURI(new URIx(uri.getNestedURI()));
      } else {
         setNestedURI(null);
      }
   }
   
   /**
    * Constructs a URI based on a string version (percent-encoded)
    * @param uri string to parse
    * @throws URIxSyntaxException if the URI fails to parse
    */
   public URIx (String uri) throws URIxSyntaxException {
      set(uri);
   }
   
   /**
    * Constructs a URI for a File object
    * @param file URI file destination
    */
   public URIx(File file) {
      setScheme(URIxScheme.FILE);
      String path = file.getAbsolutePath().replace ('\\', '/');
      if (file.isDirectory () && !path.endsWith ("/")) {
         path = path + "/";
      }
      setPath(path);
   }

   /**
    * Sets the URI to an empty relative URI
    */
   public void clear() {
      scheme = null;
      fragment = null;
      userName = null;
      password = null;
      host = null;
      port = -1;
      path = null;
      query = null;
      nestedURI = null;
   }

   // Most common constructions
   // web: http://www.site.com/file, scheme/host/path
   // http://user@host.com/file scheme/user/host/path
   // zip: zip:<url>!/fileint, scheme/uri/fragment
   /**
    * Constructs a URI with given scheme, host, and path
    * @param scheme scheme string
    * @param host encoded host string
    * @param path encoded path string
    */
   public URIx (String scheme, String host, String path)
      throws URIxSyntaxException {
      clear();
      setScheme(scheme);
      setHost(host);
      setPath(path);

   }

   /**
    * Constructs a URI with given scheme, host, and path
    * @param scheme scheme type
    * @param host encoded host string
    * @param path encoded path string
    * @throws URIxSyntaxException if the URI fails to parse
    */
   public URIx (URIxScheme scheme, String host, String path) {
      clear();
      setScheme(scheme);
      setHost(host);
      setPath(path);

   }

   /**
    * Constructs a URI with given scheme, user info, host, and path
    * @param scheme scheme string
    * @param userInfo encoded userInfo string, which can include username and password
    * @param host encoded host string
    * @param path encoded path string
    * @throws URIxSyntaxException if the URI fails to parse
    */
   public URIx (String scheme, String userInfo, String host, String path)
      throws URIxSyntaxException {
      clear();
      setScheme(scheme);
      setUserInfo(userInfo);
      setHost(host);
      setPath(path);

   }

   /**
    * Creates a nested URI, the scheme must be a zip-type (e.g. zip, tar, jar, ...)
    * @param scheme scheme string
    * @param nestedURI nested URI to underlying zip resource
    * @param fragment encoded fragment string
    * @throws URIxSyntaxException if the URI fails to parse
    */
   public URIx (String scheme, URIx nestedURI, String fragment)
      throws URIxSyntaxException {
      clear();

      setScheme(scheme);
      if (!isZipType ()) {
         throw new URIxSyntaxException ("Only zip-types can have a nested URI");
      }
      setNestedURI(nestedURI);
      setFragment(fragment);

   }

   // complete
   /**
    * Complete constructor, builds a URI providing all info
    * @param scheme scheme string
    * @param userInfo encoded userinfo string
    * @param host encoded host string
    * @param port port
    * @param path encoded path string
    * @param query encoded query string
    * @param fragment encoded fragment string
    * @throws URIxSyntaxException if the URI fails to parse
    */
   public URIx (String scheme, String userInfo, String host, int port,
      String path, String query, String fragment) throws URIxSyntaxException {
      clear();

      setScheme(scheme);
      setUserInfo(userInfo);
      setHost(host);
      setPort(port);
      setPath(path);
      setQuery(query);
      setFragment(fragment);

   }

   /**
    * Checks if the provided scheme is supported.  If it is not, it can be added using
    * {@link URIxScheme#createScheme(String, int, String[])}
    * @param scheme scheme string
    * @return true if currently supported, false otherwise
    */
   public boolean isSchemeSupported(String scheme) {
      if (scheme == null) {
         return true;
      }
      URIxScheme s = URIxScheme.findScheme(scheme);
      if (s == null) {
         return false;
      }
      return true;
   }
   
   /**
    * Returns the decoded username
    * @return username
    */
   public String getRawUserName() {
      return userName;
   }
   
   /**
    * Returns the encoded username
    * @return username
    */
   public String getUserName() {
      return percentEncode(userName, STANDARD_RESERVED);
   }
   
   /**
    * Sets the raw username
    * @param userName username
    */
   public void setRawUserName(String userName) {
      if ("".equals(userName) || userName == null) {
         this.userName = null;
      } else {
         this.userName = userName;
      }
   }
   
   /**
    * Sets the encoded username
    * @param userName username
    */
   public void setUserName(String userName) {
      if ("".equals(userName) || userName == null) {
         this.userName = null;
      } else {
         userName = percentDecode (userName);
         this.userName = userName;
      }
   }

   /**
    * Returns the raw decoded password
    * @return password
    */
   public String getRawPassword() {
      return password;
   }
   
   /**
    * Returns the encoded password
    * @return password
    */
   public String getPassword() {
      return percentEncode(password, PASSWORD_RESERVED);
   }
   
   /**
    * Sets the raw password, null to clear
    * @param password password
    */
   public void setRawPassword(String password) {
      if ("".equals(password) || password == null) {
         this.password = null;
      } else {
         this.password = password;
      }
   }
   
   /**
    * Sets the encoded password, null to clear
    * @param password password
    */
   public void setPassword(String password) {
      if ("".equals(password) || password == null) {
         this.password = null;
      } else {
         this.password = percentDecode(password);
      }
   }

   /**
    * Returns the raw host
    * @return host
    */
   public String getRawHost() {
      return host;
   }
   
   /**
    * Returns the encoded host
    * @return host
    */
   public String getHost() {
      return percentEncode(host, STANDARD_RESERVED);
   }
   
   /**
    * Sets the raw host
    * @param host host
    */
   public void setRawHost(String host) {
      if ("".equals(host) || host == null) {
         this.host = null;
      } else {
         this.host = host;
      }
   }

   /**
    * Sets the encoded host
    * @param host host
    */
   public void setHost(String host) {
      if ("".equals(host) || host == null) {
         this.host = null;
      } else {
         this.host = percentDecode(host);
      }
   }
   
   /**
    * Returns the port
    * @return port
    */
   public int getPort() {
      return port;
   }
   
   /**
    * Sets the port
    * @param port port
    */
   public void setPort(int port) {
      this.port = port;
   }

   /**
    * Returns the raw path
    * @return path
    */
   public String getRawPath() {
      return path;
   }
   
   /**
    * Returns the encoded path
    * @return path
    */
   public String getPath() {
      return percentEncode(path, PATH_RESERVED);
   }
   
   /**
    * Sets the raw path
    * @param path path
    */
   public void setRawPath(String path) {
      if (path == null) {
         this.path = null;
      } else {
         this.path = normalize(path);
      }
   }
   
   /**
    * Sets the encoded path
    * @param path path
    */
   public void setPath(String path) {
      if (path == null) {
         this.path = null;
      } else {
         this.path = normalize(percentDecode(path));
      }
   }

   /**
    * Gets the raw query string
    * @return query
    */
   public String getRawQuery() {
      return query;
   }
   
   /**
    * Gets the encoded query string
    * @return query
    */
   public String getQuery() {
      return percentEncode(query, QUERY_RESERVED);
   }
   
   /**
    * Sets the raw query string
    * @param query query
    */
   public void setRawQuery(String query) {
      if ("".equals(query) || query == null) {
         this.query = null;
      } else {
         this.query = query;
      }
   }
   
   /**
    * Sets the encoded query string
    * @param query query
    */
   public void setQuery(String query) {
      if ("".equals(query) || query == null) {
         this.query = null;
      } else {
         this.query = percentDecode (query);
      }
   }

   /**
    * If this is a zip-type URI, returns the nested URI
    * @return nested URI
    */
   public URIx getNestedURI() {
      return nestedURI;
   }

   /**
    * Sets the nested URI for zip-type URIs
    * @param nestedURI nexted URI
    */
   public void setNestedURI(URIx nestedURI) {
      this.nestedURI = nestedURI;
   }

   /**
    * Returns the raw fragment
    * @return fragment
    */
   public String getRawFragment() {
      return fragment;
   }
   
   /**
    * Returns the encoded fragment
    * @return fragment
    */
   public String getFragment() {
      if (isZipType()) {
         return percentEncode(fragment, PATH_RESERVED); // zip should have path-like encoding
      }
      return percentEncode(fragment, FRAGMENT_RESERVED);
   }
   
   public void setRawFragment(String fragment) {
      if ("".equals(fragment) || fragment == null) {
         this.fragment = null;
      } else {
         if (isZipType ()) {
            this.fragment = normalize(fragment);
         } else {
            this.fragment = fragment;
         }
      }
   }
   
   /**
    * Sets the encoded fragment
    * @param fragment fragment
    */
   public void setFragment(String fragment) {
      if ("".equals(fragment) || fragment == null) {
         this.fragment = null;
      } else {
         if (isZipType ()) {
            this.fragment = percentDecode(normalize(fragment));
         } else {
            this.fragment = percentDecode(fragment);
         }
            
      }
   }

   /**
    * Sets the scheme
    * @param scheme scheme
    */
   public void setScheme(URIxScheme scheme) {
      this.scheme = scheme;
   }
   
   /**
    * Tries to set the scheme
    * @param scheme scheme string
    * @throws URIxSyntaxException if unknown scheme
    */
   public void setScheme(String scheme) throws URIxSyntaxException {
      URIxScheme s = null;
      if (scheme != null) {
         s = URIxScheme.findScheme(scheme);
         if (s == null) {
            throw new URIxSyntaxException("Unsupported scheme '" + scheme + "'");
         }
      }
      setScheme(s);
   }

   /**
    * Returns the encoded user info string
    */
   public String getUserInfo() {
      String ui = null;
      if (userName != null) {
         ui = getUserName();
         if (password != null) {
            ui += ":" + getPassword();
         }
      }
      return ui;
   }
   
   /**
    * Sets the encoded user info string
    * @param userInfo userInfo
    */
   public void setUserInfo(String userInfo) {

      if (userInfo == null || "".equals(userInfo)) {
         setUserName(null);
         setPassword(null);
         return;
      }

      int idx = userInfo.indexOf(':');
      if (idx < 0) {
         setUserName(userInfo);
         setPassword(null);
      } else {
         setUserName(userInfo.substring(0, idx));
         if (idx == userInfo.length() - 1) {
            setPassword(null);
         } else {
            setPassword(userInfo.substring(idx + 1));
         }
      }
   }

   /**
    * Returns the encoded authority string
    * @return authority
    */
   public String getAuthority() {
      String auth = null;
      if (host != null) {
         auth = getHost();
         if (port > 0) {
            auth += ":" + port;
         }
         String ui = getUserInfo();
         if (ui != null) {
            auth = ui + "@" + auth;
         }
      }
      return auth;
   }
   
   /**
    * Sets the encoded authority string
    * @param authority authority
    */
   public void setAuthority(String authority) {
      if (authority == null || "".equals(authority)) {
         setUserInfo(null);
         setHost(null);
         setPort(-1);
         return;
      }

      // try to find port
      int idx = authority.lastIndexOf(':');
      int port = -1;
      if (idx >= 0) {
         if (idx == authority.length() - 1) {
            authority = authority.substring(0, idx); // trim off trailing colon
         } else {
            String strPort = authority.substring(idx + 1);
            try {
               port = Integer.parseInt(strPort);
               authority = authority.substring(0, idx);
            } catch (NumberFormatException e) {/* not valid port */
            }
         }
      }
      setPort(port);

      // try to find host
      idx = authority.lastIndexOf('@');
      String userInfo = null;
      String host = null;
      if (idx >= 0) {
         // we assume we have user info and host then
         userInfo = authority.substring(0, idx);
         if (idx < authority.length() - 1) {
            host = authority.substring(idx + 1);
         }
      } else {
         // no userinfo
         host = authority;
      }
      setHost(host);
      setUserInfo(userInfo);
   }

   /**
    * Returns the encoded scheme-specific part
    * @return scheme-specific part of URI
    */
   public String getSchemeSpecificPart() {
      String ssp = "";

      String auth = getAuthority();
      String path = getPath();
      String query = getQuery();

      if (scheme == null) {
         // we are relative, so assume only path and query
         if (path != null) {
            ssp += path;
         }
         if (query != null) {
            ssp += "?" + query;
         }

      } else if (scheme.isZipType()) {

         if (nestedURI != null) {
            return nestedURI.toString();
         } else {
            return null;
         }

      } else {

         ssp = "//"; // absolute

         if (auth != null) {
            ssp += auth;
         }
         if (path != null) {
            
            // check windows drive
            if (path.length() > 2 && isAlpha(path.charAt(0)) & path.charAt(1)==':') {
               // windows drive letter
            } else {
               if (!path.startsWith("/")) {
                  ssp += "/";
               }
            }
            ssp += path;
         }
         if (query != null) {
            ssp += "?" + query;
         }

      } // end checking scheme

      if ("".equals(ssp)) {
         return null;
      }
      return ssp;
   }
   
   /**
    * Sets the encoded scheme-specific part
    * @param ssp scheme-specific part
    * @throws URIxSyntaxException if there is a parse error
    */
   public void setSchemeSpecificPart(String ssp) throws URIxSyntaxException {

      if (ssp == null ) {
         setAuthority(null);
         setPath(null);
         setQuery(null);
         setNestedURI(null);
         return;
      } 
      
      String authority = null;
      String path = null;
      String query = null;
      URIx nestedURI = null;

      if (scheme == null) {

         // ssp assumed to consist of path, maybe query
         int queryIdx = ssp.indexOf('?');
         if (queryIdx >= 0) {
            path = ssp.substring(0, queryIdx);
            if (queryIdx < ssp.length() - 1) {
               query = ssp.substring(queryIdx + 1);
            }
         } else {
            path = ssp;
         }

      } else if (scheme.isZipType()) {
         nestedURI = new URIx(ssp);
      } else if (scheme == URIxScheme.FILE){
         // should start with "//" for absolute path
         if (ssp.startsWith("//")) {
            ssp = ssp.substring(2);
         }
         // rest is path
         path = ssp;
         //         if ((ssp.charAt(1) == ':') && (ssp.charAt(2) == '/')) {
         //            // full path
         //            path = ssp;
         //         }
         
      } else {

         // should start with "//" for absolute path
         if (ssp.startsWith("//")) {
            ssp = ssp.substring(2);
         }

         int authEnd = firstIndexOf(ssp, "/?#", 0);
         if (authEnd >= 0) {

            if (authEnd > 0) {
               authority = ssp.substring(0, authEnd);
               ssp = ssp.substring(authEnd);
            }

            // we have a path if first char is /, otherwise it's a query
            if (ssp.charAt(0) == '/') {
               int pathEnd = firstIndexOf(ssp, "?#", 0);
               if (pathEnd >= 0) {

                  path = ssp.substring(0, pathEnd);

                  if (ssp.charAt(pathEnd) == '?') {
                     if (pathEnd < ssp.length() - 1) {

                        query = ssp.substring(pathEnd + 1);

                     }
                  }
               } else {
                  path = ssp;
               }
            } else if (ssp.charAt(0) == '?') {
               if (ssp.length() > 1) {
                  query = ssp.substring(1);
               }
            }

         } else {

            // entire thing is authority
            authority = ssp;
         }
      } // end checking scheme type

      if ("".equals (path)) {
         path = null;
      }
      
      setAuthority(authority);
      setPath(path);
      setQuery(query);
      setNestedURI(nestedURI);
      
   }

   /**
    * Finds the first occurrence of a set of characters in a string
    * @param str string to search
    * @param chars set of characters
    * @param fromIndex searching from given index
    * @return location of first occurrence
    */
   private static int firstIndexOf(String str, String chars, int fromIndex) {

      for (int i = fromIndex; i < str.length(); i++) {
         for (int j = 0; j < chars.length(); j++) {
            if (str.charAt(i) == chars.charAt(j)) {
               return i;
            }
         }
      }
      return -1;

   }

   /**
    * Percent-encodes a string with a given set of reserved characters.
    * @param str string to encode
    * @param reservedChars characters to encode
    * @return encoded string
    */
   public static String percentEncode(String str, String reservedChars) {
      if (str==null) {
         return null;
      }
      
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < str.length(); i++) {
         char c = str.charAt(i);
         
         // check if already percent-encoded value
         if (c == '%' && i<str.length()-2) {
            String ccode = str.substring(i,i+3);
            if (PCHARS.containsValue(ccode)) {
               sb.append(ccode);
               i+=2; // skip ahead two more characters
            } else {
               sb.append(PCHARS.get(c)); // encode %
            }
            
         } else if (isInString(c, reservedChars)) {
            sb.append(PCHARS.get(c));
         } else {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   /**
    * Percent-decodes a string
    * @param str string to decode
    * @return decoded string
    */
   public static String percentDecode(String str) {
      if (str == null) {
         return null;
      }

      StringBuilder sb = new StringBuilder();
      String strCode;
      int i = 0;
      for (i = 0; i < str.length() - 2; i++) {
         char c = str.charAt(i);
         if (c == '%') {
            strCode = str.substring(i, i + 3);
            Character a = IPCHARS.get (strCode);
            if (a != null) {
               sb.append(a);
               i += 2;
            } else {
               sb.append(c);
            }
         } else {
            sb.append(c);
         }
      }
      for (; i < str.length (); ++i) {
         sb.append (str.charAt (i));
      }
      
      return sb.toString();
   }

   /**
    * Sets this URI to the encoded string representation
    * @param uri URI string
    * @throws URIxSyntaxException if there is a parse error
    */
   public void set(String uri) throws URIxSyntaxException {
      
      if (uri == null) {
         uri = "";
      }
      
      String schemeStr = getSchemeStr(uri);
      URIxScheme scheme = null;

      String ssp = null;
      String frag = null;

      // check default file schemes:
      if (uri.length() > 2) {
         // Windows path
         if (uri.charAt(1) == ':') {
            setScheme(URIxScheme.FILE);
            uri = "file:" + uri.replace ('\\', '/');
         }
      }
      
      if (schemeStr == null) {
         int idx = uri.indexOf('#');
         if (idx < 0) {
            ssp = uri;
         } else {
            ssp = uri.substring(0, idx);
            if (idx < uri.length() - 1) {
               frag = uri.substring(idx + 1);
            }
         }
      } else {
         scheme = URIxScheme.findScheme(schemeStr);
         if (scheme == null) {
            // abort!!
            throw new URIxSyntaxException(uri, "unsupported scheme type '"
               + schemeStr + "'");
         }

         // trim off scheme
         uri = uri.substring(schemeStr.toString().length() + 1);

         int idx = -1;
         if (scheme.isZipType()) {
            idx = uri.lastIndexOf('!');
            if (idx < 0) {
               throw new URIxSyntaxException(
                  uri, "zip types require fragment to be specified");
            }
         } else {
            idx = uri.indexOf('#');
         }

         if (idx < 0) {
            ssp = uri;
         } else {
            ssp = uri.substring(0, idx);
            if (idx < uri.length() - 1) {
               frag = uri.substring(idx + 1);
            }
         }
      }

      setScheme(scheme);
      setSchemeSpecificPart(ssp);
      setFragment(frag);

   }

   private static boolean isAlpha(char a) {
      if (a >= 'a' && a <= 'z') {
         return true;
      } else if (a >= 'A' && a <= 'Z') {
         return true;
      }
      return false;
   }

   private static boolean isNumeric(char a) {
      if (a >= '0' && a <= '9') {
         return true;
      }
      return false;
   }

   private static boolean isInString(char a, String str) {
      for (int i = 0; i < str.length(); i++) {
         if (str.charAt(i) == a) {
            return true;
         }
      }
      return false;
   }

   private static boolean isValidSchemeChar(char a) {

      if (isAlpha(a)) {
         return true;
      } else if (isNumeric(a)) {
         return true;
      } else if (isInString(a, "+-.")) {
         return true;
      }
      return false;

   }

   /**
    * Extracts the scheme string from a URI
    * @param uri URI string
    */
   public static String getSchemeStr(String uri) {

      if (uri == null || "".equals(uri)) {
         return null;
      }

      // first must be alpha
      if (!isAlpha(uri.charAt(0))) {
         return null;
      }

      if (uri.length() == 1) {
         return null;
      } else if (uri.charAt(1) == ':') {
         // windows path
         return "file";
      }

      char a = uri.charAt(0);
      StringBuilder schemeStr = new StringBuilder();
      schemeStr.append(a);
      int i = 1;

      while (isValidSchemeChar(a = uri.charAt(i)) && i < uri.length() - 1) {
         schemeStr.append(a);
         i++;
      }

      // check we terminated correctly
      if (i == uri.length() - 1) {
         return null;
      } else if (a != ':') {
         return null;
      }

      return schemeStr.toString();

   }

   /**
    * Returns the scheme of this URI
    * @return scheme
    */
   public URIxScheme getScheme() {
      return scheme;
   }

   /**
    * Returns the full string representation of this encoded URI
    * @return URI string
    */
   public String toString() {

      String out = "";
      boolean zipType = false;

      if (scheme != null) {
         out += scheme.toString() + ":";

         if (scheme.isZipType()) {
            zipType = true;
         }
      }

      String fragment = getFragment();
      String ssp = getSchemeSpecificPart();

      if (ssp != null) {
         out += ssp;
      }

      if (fragment != null) {
         if (zipType) {
            out += "!";
         } else {
            out += "#";
         }
         out += fragment;
      }

      return out;
   }

   /**
    * Sets the nested base URI to the provided one.  This can be used to change the source of a zip-type URI
    * @param base new base URI
    */
   public void setBaseURI(URIx base) {
      URIx myBase = getBaseURI();
      if (myBase.isZipType()){
         myBase.setNestedURI(base);
      } else {
         myBase.set(base);
      }
   }
   
   /**
    * Returns the base URI for nested URIs, or the current URI if it is non-zip type
    * @return base URI
    */
   public URIx getBaseURI() {
      return getBaseURI(this);
   }
   
   /**
    * Resolves a relative URI string
    * @param rel relative URI
    * @return resolved URI
    */
   public URIx resolve(String rel) {
      return resolve(this, rel);
   }
   
   /**
    * Resolves a relative URI
    * @param uri relative URI
    * @return resolved URI
    */
   public URIx resolve(URIx uri) {
      return resolve(this, uri);
   }
   
   /**
    * Creates a URI relative to the current from the provided input.  If no such relative URI exists, then
    * a copy of the input is returned.
    * @param uri URI to relativize relative to this URI
    * @return relativized URI
    */
   public URIx relativize(URIx uri) {
      return relativize(this, uri);
   }
   
   /**
    * Creates a URI relative to the current from the provided input.  If no such relative URI exists, then
    * a copy of the input is returned.
    * @param uri URI to relativize relative to this URI
    * @return relativized URI
    */
   public URIx relativize(String uri) {
      return relativize(this, new URIx(uri));
   }

   /**
    * Returns a stack-representation of nested URIs with the base at the end of the stack
    */
   public ArrayList<URIx> getURIStack() {
      return getURIStack(this);
   }

   /**
    * Creates a stack representation of a nested URI with the base at the end of the stack
    * @param uri URI to extract
    * @return URI stack
    */
   public static ArrayList<URIx> getURIStack(URIx uri) {

      ArrayList<URIx> stack = new ArrayList<URIx>();
      URIx base = uri;
      stack.add(base);

      // loop until we're at the bottom of the nested hierarchy
      while (base.getNestedURI() != null) {
         base = base.getNestedURI();
         stack.add(base);
      }

      return stack;

   }

   /**
    * Checks if the URI is relative (i.e. the base URI has no scheme)
    */
   public boolean isRelative() {
      URIx base = getBaseURI ();
      if (base.getScheme () == null || base.isZipType ()) {
         return true;
      }
      return false;
   }

   /**
    * Merges two URIs
    * 
    * @param base URI base
    * @param relative extension relative to base
    * @return merged URI
    */
   public static URIx resolve(URIx base, URIx relative) {

      if (!relative.isRelative()) {
         return relative;
      }
      
      URIx merged = null;
      
      if (!base.isZipType()) {
         
         // append path, set fragment/query from relative
         if (!relative.isZipType()) {
            merged = resolvePath(base, relative.getPath());
            merged.query = relative.query;
            merged.fragment = relative.fragment;
            
         } else {
            
            // merge relative's base URI with base
            merged = new URIx(relative);
            URIx relBase = merged.getBaseURI();
            if (relBase.isZipType ()) {
               relBase.setNestedURI (base);
            } else {
               relBase = resolve(base, relBase);
               merged.setBaseURI(relBase);
            }
           
         }
      } else {
         
         if (!relative.isZipType()) {
            
            // merge path to fragment, ignore other relative stuff
            merged = resolvePath(base, relative.getPath ());
            
         } else {
            
            // append relative path to fragment, add additional zip stuff
            // XXX double-check nesting
            merged = new URIx(relative);
            URIx relBase = merged.getBaseURI();
            if (relBase.isZipType()) {
               relBase = new URIx();
               merged.setBaseURI(relBase);
            }
            
            // relBase is now a relative non-zip
            String relBasePath = relBase.getPath(); 
            
            // copy base to relative base
            relBase.set(base);
            
            // append path to outer base fragment
            if (relBasePath == null) {
               // relBase.setFragment (base.fragment); // nothing to merge
            } else if (relBasePath.startsWith("/")) {
               relBase.setFragment(relBasePath);
            } else {
               relBase.setFragment(concatPaths(base.fragment, relBasePath));
            }
         }
         
      }

      return merged;
   }
   
   /**
    * Resolves a URI provided a File.  If the file object is absolute,
    * replaces the path (or fragment for zip-types).  Otherwise, resolves the relative
    * path (or fragment).
    * 
    * @param base base URI
    * @param file file to resolves
    * @return resolved URI
    */
   public static URIx resolve(URIx base, File file) {
      return resolvePath(base, file.getPath());
   }
   
   /**
    * Resolves a URI provided a string representation of a URI
    * @param base base URI
    * @param rel relative or absolute URI string
    * @return resolved URI
    */
   public static URIx resolve(URIx base, String rel) {
      URIx relURI = new URIx(rel);
      return resolve(base, relURI);
   }
   
   /**
    * Resolves a path only (replaces or appends to existing base path)
    * @param base base URI
    * @param relPath path to resolve
    * @return resolved URI
    */
   private static URIx resolvePath(URIx base, String relPath) {
      
      URIx merged = new URIx(base);
      // empty relative path, return base
      if ( relPath == null ) {
         return merged;
      }
      
      if (!base.isZipType()) {
         // if relPath starts with /, path is absolute
         // if relpath starts with //, overwrites userinfo as well
         if (relPath.startsWith("//")) {
            merged.setSchemeSpecificPart(relPath.substring(2));
         } else if (relPath.startsWith("/")) {
            merged.setPath(relPath);
         } else {
            merged.setPath(concatPaths(merged.path, relPath));
         }
         
      } else {
         // if relPath starts with /, fragment is absolute
         if (relPath.startsWith("/")) {
            merged.setFragment(relPath);
         } else {
            merged.setFragment(concatPaths(merged.fragment, relPath));
         }
      }
      
      return merged;      
   }
   
   // split into segments, all but last contain trailing slash
   private static class URISegment {
      int start;
      int end;
      URISegment next;
      public URISegment(int start) {
         this.start = start;
         this.end = start;
         next = null;
      }
      public int len() {
         return end-start;
      }
   }
   
   /**
    * Splits a path into a linked-list of segments, each ending with '/'
    * except for the last
    * @param path path to split into segments
    * @return first segment link
    */
   private static URISegment splitPath(String path) {
      
      if (path == null) {
         return new URISegment (0);
      }
      // split path into segments, with dummy-start
      URISegment start = new URISegment(0);
      URISegment seg = start;
      for (int i=0; i<path.length (); ++i) {
         if (path.charAt (i) == SEP) {
            seg.end = i+1;
            seg.next = new URISegment (i+1);
            seg = seg.next;
         }
      }
      // terminate last segment
      seg.end = path.length ();
      
      return start;
   }
   
   /**
    * Normalize path component by resolving . and .. according to  RFC 2396 5.2
    * @param path path component
    * @return resolved path string
    */
   private static String normalize(String path) {
    
      if (path == null) {
         return "";
      }
      
      // handling of special-case inputs
      if (path.equals(".")) {
         path = "./";
      }
      if (path.equals("..")) {
         path = "../";
      }
      
      URISegment start = splitPath(path);

      // iterate through segments, resolving path
      ArrayDeque<URISegment> leading = new ArrayDeque<> ();  // leading / or ../
      ArrayDeque<URISegment> working = new ArrayDeque<> (); // working segments

      URISegment seg = start;
      
      // special handling of absolute paths, prevent removal of first segment
      if (start.len () == 1 && path.charAt (start.start) == SEP) {
         leading.addLast (start);
         seg = start.next;
      }
      
      // loop through remaining segments
      while (seg != null) {
         if (seg.len () == 2 
            && path.charAt (seg.start) == DOT
            && path.charAt (seg.start+1) == SEP) {
         
            // RFC 2396 5.2 6c
            // remove instances of ./
         
         } else if (seg.len () == 3 
            && path.charAt (seg.start) == DOT
            && path.charAt (seg.start+1) == DOT
            && path.charAt (seg.start+2) == SEP) {
            
            // RFC 2396 5.2 6e
            // back up, or pass on to leading
            if (working.size () > 0) {
               working.removeLast ();
            } else {
               leading.addLast (seg);
            }
         } else {
            // append segment to working section
            working.addLast (seg);
         }
         seg = seg.next;
      }
      
      // check last working segment
      if (working.size () > 0) {
         URISegment last = working.peekLast ();
         if (last.len () == 1 && path.charAt (last.start) == DOT) {
            // RFC 2396 5.2 6d
            // if single dot, remove
            working.removeLast ();
         } else if (last.len () == 2  
            && path.charAt (last.start) == DOT
            && path.charAt (last.start+1) == DOT) {
            // RFC 2396 5.2 6f
            // if double dot, back up or pass on to leading
            if (working.size () > 1) {
               working.removeLast ();
               working.removeLast ();
            } else {
               leading.addLast (last);
            }
         }
      }
      
      // RFC 2396 5.2 6g, choosing to keep leading .., merge results
      StringBuilder out = new StringBuilder();
      for (URISegment segl : leading) {
         for (int i=segl.start; i<segl.end; ++i) {
            out.append (path.charAt (i));
         }
      }
      for (URISegment segl : working) {
         for (int i=segl.start; i<segl.end; ++i) {
            out.append (path.charAt (i));
         }
      }
      
      return out.toString ();
   }
   
   /**
    * Concatenates paths, resolving any relative components
    * @param path1 first path
    * @param path2 second path
    * @return concatenated path
    */
   private static String concatPaths(String path1, String path2) {

      StringBuilder path = new StringBuilder();
      
      // RFC 2396 5.2 6a
      // append path1 up to final /
      if (path1 != null) {
         int idx = path1.lastIndexOf (SEP);
         if (idx >= 0) {
            path.append (path1.substring (0, idx+1));
         }
      }

      // RFC 2396 5.2 6b
      // append reference
      if (path2 != null) {
         path.append (path2);
      }
      
      return normalize(path.toString ());
   }
   
   /**
    * Compares two strings for equality, including of one or both are null.  If only
    * one is null, deemed not equal.
    * @param str1 first string
    * @param str2 second string
    * @return true if equal
    */
   private static boolean stringsEqual(String str1, String str2) {
      if (str1 == str2) {
         return true;
      } else if (str1 == null || str2 == null) {
         return false;
      }
      return str1.equals (str2);
   }
   
   /**
    * Creates a relative path between parent and child such that
    * the child path can be recovered via {@link #resolvePath(URIx, String)}
    * If there is no relative path, then null is returned
    * 
    * @param parent parent path
    * @param child child path
    * @return relative path or null
    */
   private static String relativizePath(String parent, String child) {
      
      URISegment psplit = splitPath (parent);
      URISegment csplit = splitPath (child);
      
      // advance through paths while segments are equal
      URISegment p = psplit;
      URISegment c = csplit;
      while (p != null && c != null && p.end == c.end) {
         boolean match = true;
         for (int i=p.start; i<p.end; ++i) {
            if (parent.charAt (i) != child.charAt (i)) {
               match = false;
               break;
            }
         }
         if (!match) {
            break;
         }
         p = p.next;
         c = c.next;
      }
      
      // one is null, or we are pointing to the first difference
      
      // if first segment differs, if child is absolute path then return child, 
      //otherwise no relative path exists
      if (p == psplit) {
         if (csplit.len () == 1 && child.charAt (csplit.start) == SEP) {
            return child;
         }
         // otherwise, there is no overlap between paths and no way to get there
         return null;
      }
      
      // there was at least some overlap
      StringBuilder out = new StringBuilder();
      
      // backtrack remaining paths in parent, except for last (no trailing slash)
      while (p != null && p.next != null) {
         // do not backtrack for last entry
         out.append ("../");
         p = p.next;
      }
      
      // advance remaining child paths
      while (c != null) {
         for (int i=c.start; i<c.end; ++i) {
            out.append (child.charAt (i));
         }
         c = c.next;
      }
      
      return out.toString ();
      //      if (!parent.equals(child)) {
      //         if (!parent.endsWith("/")) {
      //            // back up to last '/' if one exists
      //            int idx = parent.lastIndexOf ('/');
      //            parent = parent.substring (0, idx+1);
      //         }
      //         // no common prefix
      //         if (!child.startsWith(parent)) {
      //             return child;
      //         }
      //     }
      //      
      //      return child.substring(parent.length());
   }
   
   /**
    * Creates a relative URI such that child = URIx.resolve(base, relative);  Requires that
    * child is hierarchically equivalent to base, except for difference in path (if not a zip-type)
    * or fragment (if zip type).  Otherwise, returns the child URI.
    * 
    * @param base base URI
    * @param child child URI
    * @return relative URI between base and child
    */
   public static URIx relativize(URIx base, URIx child) {

      ArrayList<URIx> bstack = base.getURIStack ();
      ArrayList<URIx> cstack = child.getURIStack ();
      
      int bidx = bstack.size ()-1;
      int cidx = cstack.size ()-1;
      
      // child stack must be larger or equal to to have relative URI
      if (cidx < bidx) {
         return child;
      }
      
      // find where stacks differ
      while (bidx >= 0 && cidx >= 0) {
         
         URIx b = bstack.get (bidx);
         URIx c = cstack.get (cidx);
         
         if (b.getScheme () != c.getScheme ()) {
            break;
         }
         if (!stringsEqual (b.getAuthority (), c.getAuthority ())) {
            break;
         }
         if (b.isZipType ()) {
            if (!stringsEqual(b.getFragment (), c.getFragment ())) {
               break;
            }
         } else {
            if (!stringsEqual(b.getPath (), c.getPath ())) {
               break;
            }
         }
         
         --bidx;
         --cidx;
      }
      
      // if we are not at the top of the base URL stack, there is no relative URI
      if (bidx > 0) {
         return child;
      }
      
      // if we are identical up to and including all of base URI stack, then we can
      // remove nested component
      URIx relative = new URIx();
      if (bidx < 0) {
         if (cidx < 0) {
            // empty
         } else {
            // keep up to cidx
            relative.set(child);
            URIx next = relative;
            while (cidx > 0) {
               next = relative.getNestedURI ();
               --cidx;
            }
            // remove base
            next.setNestedURI (null);
         }
      } else {
         // remove commonality between cstack[cidx] and bstack[0]
         relative.set(child);
         URIx c = relative;
         while (cidx > 0) {
            c = c.getNestedURI ();
            --cidx;
         }
         c.setNestedURI (null);
         
         // check for differences, scheme and authority must match
         if (c.getScheme () != base.getScheme ()) {
            return child;
         }
         
         // merge c and bstack[0]
         if (!c.isZipType ()) {
            if (!stringsEqual(c.getAuthority (), base.getAuthority ())) {
               return child;
            }
            
            c.setScheme ((URIxScheme)null);
            c.setAuthority (null);
            
            // determine relative path
            String rpath = relativizePath (normalize(base.getPath ()), normalize(c.getPath ()));
            if (rpath == null) {
               return child;
            }
            c.setPath (rpath);
            
         } else {
            // determine relative fragment
            String rpath = relativizePath(normalize(base.getFragment ()), normalize(c.getFragment ()));
            if (rpath == null) {
               return child;
            }
            if (c.isZipType ()) {
               // move from fragment to path
               c.setFragment (null);
            }
            c.setScheme ((URIxScheme)null);
            c.setPath (rpath);
         }
         
      }
      
      relative.query = child.query;
      if (!child.isZipType ()) {
         relative.fragment = child.fragment;
      }
      
      return relative;
      
   }

   /**
    * Checks if the current URI is of zip-type, which allows for nested URIs.  Zip-types are
    * defined by the scheme properties.
    * @return true if zip type
    */
   public boolean isZipType() {
      if (scheme == null) {
         return false;
      }
      return scheme.isZipType();
   }

   /**
    * Determines the base URI, representing the lowest file or relative path in a set of nested URIs
    * @param uri URI to determine base of
    * @return base URI
    */
   public static URIx getBaseURI(URIx uri) {

      URIx base = uri;
      // loop until we're at the bottom of the nested hierarchy
      while (base.getNestedURI() != null) {
         base = base.getNestedURI();
      }
      return base;
   }
   
   /**
    * Converts to a java URI
    * @return URI
    * @throws URISyntaxException if there is a parsing error
    */
   public URI toURI() throws URISyntaxException {
      return new URI(toString());
   }
   
   public URL toURL() throws MalformedURLException, URISyntaxException {
      return toURI().toURL ();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof URIx) {
         return equals((URIx)obj);
      }
      return false;
   }

   /**
    * Checks equality of two URIs
    * @param other URI to compare to
    * @return true if equal
    */
   public boolean equals(URIx other) {
      return this.toString().equals(other.toString());
   }

}
