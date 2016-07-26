/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Extended URI class that handles nested URIs required for zip files
 * @author antonio
 *
 */
public class URIx {

   // General Syntax:
   // --------------------------
   // Zip: <scheme>:<scheme-specific-part>![</fragment>]
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
   
   private static boolean encodeByDefault = true; 

   // percent-encoded chars
   static final HashMap<Character,String> PCHARS =
      new HashMap<Character,String>();
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

   public URIx () {
      clear();
   }

   public URIx (URIx uri) {
      if (uri == null) {
         clear();
      } else {
         set(uri);
      }
   }
   
   public URIx(URIx base, String relPath) {
      this(base);
      if (!base.isZip()) {
         // if relPath starts with /, path is absolute
         // if relpath starts with //, overwrites userinfo as well
         if (relPath.startsWith("//")) {
            setSchemeSpecificPart(relPath.substring(2));
         } else if (relPath.startsWith("/")) {
            setPath(relPath);
         } else {
            setPath(concatPaths(path, relPath));
         }
      } else {
         // if relPath starts with /, fragment is absolute
         if (relPath.startsWith("/")) {
            setFragment(relPath);
         } else {
            setFragment(concatPaths(fragment, relPath));
         }
      }
   }

   public void set (URIx uri) {
      setScheme(uri.getScheme());
      setFragment(uri.getRawFragment());
      setUserName(uri.getRawUserName());
      setPassword(uri.getRawPassword());
      setHost(uri.getRawHost());
      setPort(uri.getPort());
      setPath(uri.getRawPath());
      setQuery(uri.getRawQuery());
      if (uri.getNestedURI() != null) {
         setNestedURI(new URIx(uri.getNestedURI()));
      } else {
         setNestedURI(null);
      }
   }
   
   public URIx (String uri) throws URIxSyntaxException {
      set(uri);
   }
   
   public URIx(File file) {
      setScheme(URIxScheme.FILE);
      setPath(file.getAbsolutePath());      
   }

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
   public URIx (String scheme, String host, String path)
      throws URIxSyntaxException {
      clear();
      setScheme(scheme);
      setHost(host);
      setPath(path);

   }

   public URIx (URIxScheme scheme, String host, String path) {
      clear();
      setScheme(scheme);
      setHost(host);
      setPath(path);

   }

   public URIx (String scheme, String userInfo, String host, String path)
      throws URIxSyntaxException {
      clear();
      setScheme(scheme);
      setUserInfo(userInfo);
      setHost(host);
      setPath(path);

   }

   public URIx (String scheme, URIx nestedURI, String fragment)
      throws URIxSyntaxException {
      clear();

      setScheme(scheme);
      setNestedURI(nestedURI);
      setFragment(fragment);

   }

   // complete
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

   public static void buildURI(StringBuilder sb, String scheme,
      String userName, String password, String host,
      int port, String path, String query, String fragment) {

   }

   public String getRawUserName() {
      return userName;
   }
   public String getUserName() {
      return getUserName(encodeByDefault);
   }
   
   public String getUserName(boolean encoded) {
      if (encoded) {
        return percentEncode(userName, STANDARD_RESERVED);
      }
      return userName;
   }
   
   public void setUserName(String userName) {
      if ("".equals(userName) || userName == null) {
         this.userName = null;
      } else {
         this.userName = userName;
      }
   }

   public String getRawPassword() {
      return password;
   }
   public String getPassword() {
      return getPassword(encodeByDefault);
   }
   public String getPassword(boolean encoded) {
      if (encoded) {
         return percentEncode(password, PASSWORD_RESERVED);
      }
      return password;
   }
   public void setPassword(String password) {
      if ("".equals(password) || password == null) {
         this.password = null;
      } else {
         this.password = password;
      }
   }

   public String getRawHost() {
      return host;
   }
   public String getHost() {
      return getHost(encodeByDefault);
   }
   public String getHost(boolean encoded) {
      if (encoded) {
         return percentEncode(host, STANDARD_RESERVED);
      }
      return host;
   }
   public void setHost(String host) {
      if ("".equals(host) || host == null) {
         this.host = null;
      } else {
         this.host = host;
      }
   }

   public int getPort() {
      return port;
   }
   public void setPort(int port) {
      this.port = port;
   }

   public String getRawPath() {
      return path;
   }
   
   public String getPath() {
      return getPath(encodeByDefault);
   }
   public String getPath(boolean encoded) {
      if (encoded) {
         return percentEncode(path, PATH_RESERVED);
      }
      return path;
   }
   private static String convertSlashes(String in) {
      return in.replace('\\', '/');
   }
   public void setPath(String path) {
      if (path == null) {
         this.path = null;
      } else {
         this.path = convertSlashes(path);
      }
   }

   public String getRawQuery() {
      return query;
   }
   public String getQuery() {
      return getQuery(encodeByDefault);
   }
   public String getQuery(boolean encoded) {
      if (encoded) {
         return percentEncode(query, QUERY_RESERVED);
      }
      return query;
   }
   public void setQuery(String query) {
      if ("".equals(query) || query == null) {
         this.query = null;
      } else {
         this.query = query;
      }
   }

   public URIx getNestedURI() {
      return nestedURI;
   }

   public void setNestedURI(URIx nestedURI) {
      this.nestedURI = nestedURI;
   }

   public String getRawFragment() {
      return fragment;
   }
   public String getFragment() {
      return getFragment(encodeByDefault);
   }
   public String getFragment(boolean encoded) {
      if (encoded) {
         if (isZip()) {
            return fragment; // zip should have raw fragment
         }
         return percentEncode(fragment, FRAGMENT_RESERVED);
      }
      return fragment;
   }
   public void setFragment(String fragment) {
      if ("".equals(fragment) || fragment == null) {
         this.fragment = null;
      } else {
         this.fragment = convertSlashes(fragment);
      }
   }

   public void setScheme(URIxScheme scheme) {
      this.scheme = scheme;
   }
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

   public String getUserInfo() {
      return getUserInfo(encodeByDefault);
   }
   public String getUserInfo(boolean encoded) {
      String ui = null;
      if (userName != null) {
         ui = getUserName(encoded);
         if (password != null) {
            ui += ":" + getPassword(encoded);
         }
      }
      return ui;
   }
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

   public String getAuthority() {
      return getAuthority(encodeByDefault);
   }
   public String getAuthority(boolean encoded) {
      String auth = null;
      if (host != null) {
         auth = getHost(encoded);
         if (port > 0) {
            auth += ":" + port;
         }
         String ui = getUserInfo(encoded);
         if (ui != null) {
            auth = ui + "@" + auth;
         }
      }
      return auth;
   }
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

   public String getSchemeSpecificPart() {
      return getSchemeSpecificPart(encodeByDefault);
   }
   public String getSchemeSpecificPart(boolean encoded) {
      String ssp = "";

      String auth = getAuthority(encoded);
      String path = getPath(encoded);
      String query = getQuery(encoded);

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
            return nestedURI.toString(encoded);
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

      setAuthority(authority);
      setPath(path);
      setQuery(query);
      setNestedURI(nestedURI);
      
   }

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

   public static String
      percentEncode(String str, String reservedChars) {
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

   public static String percentDecode(String str) {
      if (str == null) {
         return null;
      }

      StringBuilder sb = new StringBuilder();
      String strCode;
      for (int i = 0; i < str.length() - 2; i++) {
         char c = str.charAt(i);
         if (c == '%') {

            strCode = str.substring(i, i + 3);
            char a = getKey(strCode, PCHARS);
            if (a > 0) {
               sb.append(a);
            } else {
               sb.append(c);
            }
         } else {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   private static char getKey(String val, HashMap<Character,String> pMap) {
      for (char c : pMap.keySet()) {
         if (pMap.get(c).equals(val)) {
            return c;
         }
      }
      return 0;
   }

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
         uri = uri.substring(scheme.toString().length() + 1);

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

   public static boolean isInString(char a, String str) {
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
         return null;
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

   public URIxScheme getScheme() {
      return scheme;
   }

   public static void buildURI(StringBuilder sb, String scheme, String ssp,
      String fragment) {
   }

   
   public String toString() {
      return toString(encodeByDefault);
   }
   
   public String toString(boolean encoded) {

      String out = "";
      boolean zipType = false;

      if (scheme != null) {
         out += scheme.toString() + ":";

         if (scheme.isZipType()) {
            zipType = true;
         }
      }

      String fragment = getFragment(encoded);
      String ssp = getSchemeSpecificPart(encoded);

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

   public void setBaseURI(URIx base) {
      URIx myBase = getBaseURI();
      if (myBase.isZip()){
         myBase.setNestedURI(base);
      } else {
         myBase.set(base);
      }
   }
   
   public URIx getBaseURI() {
      return getBaseURI(this);
   }

   public ArrayList<URIx> getURIStack() {
      return getURIStack(this);
   }

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

   public boolean isRelative() {
      return (getBaseURI().getScheme() == null);
   }
   
   public static void setEncodeByDefault(boolean encode) {
      encodeByDefault = encode;
   }

   /**
    * Merges two URIs
    * 
    * @param base
    * @param relative
    * @return merged URI
    * @throws URIxSyntaxException
    */
   public static URIx merge(URIx base, URIx relative) {

      if (base == null || base.isRelative()) {
         throw new IllegalArgumentException("base URI <"+base.toString()+"> must be absolute");
      } else if (!relative.isRelative()) {
         throw new IllegalArgumentException("relative URI <" + relative + "> must be relative");
      }
      
      URIx merged = null;
      
      if (!base.isZip()) {
         
         // append path, set fragment/query from relative
         if (!relative.isZip()) {
            merged = merge(base,relative.getPath());
            merged.query = relative.query;
            merged.fragment = relative.fragment;
            
         } else {
            
            // merge relative's base URI with base
            merged = new URIx(relative);
            URIx relBase = merged.getBaseURI();

            if (relBase.isRelative()) {
               relBase = merge(base, relBase);
               merged.setBaseURI(relBase);
            } else  {
               merged.setBaseURI(base);
            }
           
         }
      } else {
         
         if (!relative.isZip()) {
            
            // merge path to fragment, ignore other relative stuff
            merged = merge(base, relative.getPath());
            
         } else {
            
            // append relative path to fragment, add additional zip stuff
            merged = new URIx(relative);
            URIx relBase = merged.getBaseURI();
            if (relBase.isZip()) {
               relBase = new URIx();
               merged.setBaseURI(relBase);
            }
            
            // relBase is now a relative or non-zip
            String relBasePath = relBase.getPath(); 
            relBase.set(base);
            
            if (relBasePath.startsWith("/")) {
               relBase.setFragment(relBasePath);
            } else {
               relBase.setFragment(concatPaths(base.fragment, relBasePath));
            }
         }
         
      }

      return merged;
   }

   public static URIx merge(URIx base, File relPath) {
      return merge(base, relPath.getPath());
   }
   
   public static URIx merge(URIx base, String relPath) {
      
      URIx merged = new URIx(base);
      if (!base.isZip()) {
         
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
   
   private static String concatPaths(String path1, String path2) {

      String separator = "/";
      String out = null;
      if (path1 != null) {
         out = path1;
      }

      if (path2 != null) {
         if (out != null) {
            if (!out.endsWith(separator) && !path2.startsWith(separator)) {
               out += separator;
            }
            out += path2;
         } else {
            out = path2;
         }
      }

      // remove /../
      int idx;
      while ((idx = out.indexOf("/../")) > 0) {

         int a = out.lastIndexOf('/', idx - 1);
         if (a < 0) {
            out = out.substring(idx + 4);
         } else {
            out = out.substring(0, a);
            out += out.substring(idx + 3);
         }
      }

      return out;
   }

   public boolean isZip() {
      if (scheme == null) {
         return false;
      }
      return scheme.isZipType();
   }

   public static URIx getBaseURI(URIx uri) {

      URIx base = uri;
      // loop until we're at the bottom of the nested hierarchy
      while (base.getNestedURI() != null) {
         base = base.getNestedURI();
      }
      return base;

   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof URIx) {
         return equals((URIx)obj);
      }
      return false;
   }

   public boolean equals(URIx other) {
      return this.toString().equals(other.toString());
   }

}
