/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;
import java.lang.reflect.Array;
import java.net.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.*;

import java.io.*;

import artisynth.core.util.*;

public class ScanWriteUtils {
   
   private static boolean myTokenPrinting = false;
   
   private static final int TT_WORD = ReaderTokenizer.TT_WORD;

   /**
    * Debugging hook to enable printing of the token queue produced
    * in the {@link #scanfull scanfull()} method, before <code>postscan()</code>
    * is called. 
    * @param enable if <code>true</code>, enables token printing.
    */
   public static void setTokenPrinting (boolean enable) {
      myTokenPrinting = enable;
   }

   /**
    * Returns <code>true</code> if token printing is enabled in the
    * {@link #scanfull scanfull()} method.
    * @return <code>true</code> if token printing is enabled in 
    * <code>scan()</code>
    */
   public static boolean getTokenPrinting () {
      return myTokenPrinting;
   }

   /**
    * If the next input token is a quoted string or word, then assume that
    * it corresponds to component reference path, store its value
    * in the token queue as a <code>StringToken</code>, and return
    * <code>true</code>.
    * 
    * @param rtok input token stream
    * @param tokens token queue for postscan
    * @return <code>true</code> if reference path is scanned and stored 
    * in the queue as a <code>StringToken</code>
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanAndStoreReference (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      boolean scanned = false;

      rtok.parseNumbers (false);
      int dsave = rtok.getCharSetting ('-');
      rtok.wordChar ('-');

      try {
         rtok.nextToken();
         // for backward compatibility, allow the path to be a word as 
         // well as a quoted string 
         if (rtok.ttype == TT_WORD || rtok.ttype == '"') {
            tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
            scanned = true;
         }
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         rtok.parseNumbers (true);
         rtok.setCharSetting ('-', dsave);
      }
      return scanned;
   }

   /**
    * Assumes that the next set of input tokens are a set of component 
    * reference paths enclosed between square brackets. Read these
    * reference paths, stored their values in the token queue as
    * a set of <code>StringTokens</code>, and return the number
    * of reference paths found.
    * 
    * @param rtok input token stream
    * @param tokens token storage queue for postscan
    * @return number of reference paths read
    * @throws IOException if an I/O or syntax error occurred
    */
   public static int scanAndStoreReferences (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.parseNumbers (false);
      int dsave = rtok.getCharSetting ('-');
      rtok.wordChar ('-');

      int oldsize = tokens.size();
      try {
         rtok.scanToken ('[');
         tokens.offer (ScanToken.BEGIN);
         while (rtok.nextToken() != ']') {
            if (rtok.ttype == ReaderTokenizer.TT_WORD || rtok.ttype == '"') {
               tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
            }
            else {
               throw new IOException (
                  "Expecting component reference, got "+rtok);
            }
         }
         tokens.offer (ScanToken.END);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         rtok.parseNumbers (true);
         rtok.setCharSetting ('-', dsave);
      }
      return tokens.size() - oldsize - 2;
   }
   
   public static boolean scanAttributeName (ReaderTokenizer rtok, String name)
   throws IOException {
      if (rtok.ttype == ReaderTokenizer.TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         return true;
      }
      return false;
   }
   
   /**
    * If the next input token is a quoted string or word, then assume that
    * it corresponds to property path, store its value
    * in the token queue as a <code>StringToken</code>, and return
    * <code>true</code>.
    * 
    * @param rtok input token stream
    * @param tokens token storage queue for postscan
    * @throws IOException if an I/O or syntax error occurred
    */
   public static void scanAndStorePropertyPath (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.parseNumbers (false);
      int dsave = rtok.getCharSetting ('-');
      int csave = rtok.getCharSetting (':');
      rtok.wordChar ('-');
      rtok.wordChar (':');

      try {
         // for backward compatibility, allow the path to be a word as 
         // well as a quoted string 
         rtok.nextToken();
         if (rtok.ttype == ReaderTokenizer.TT_WORD || rtok.ttype == '"') {
            tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
         }
         else {
            throw new IOException ("Expecting property path, got "+rtok);
         }
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         rtok.parseNumbers (true);
         rtok.setCharSetting ('-', dsave);
         rtok.setCharSetting (':', csave);
      }
   }
   
   /**
    * Assumes that the next set of input tokens are a set of property
    * paths enclosed between square brackets. Read these
    * reference paths, store their values in the token queue as
    * a set of <code>StringTokens</code>, and return the number
    * of paths found.
    * 
    * @param rtok input token stream
    * @param tokens token storage queue for postscan
    * @return number of paths read
    * @throws IOException if an I/O or syntax error occurred
    */
   public static int scanAndStorePropertyPaths (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.parseNumbers (false);
      int dsave = rtok.getCharSetting ('-');
      int csave = rtok.getCharSetting (':');
      rtok.wordChar ('-');
      rtok.wordChar (':');

      int oldsize = tokens.size();
      try {
         rtok.scanToken ('[');
         tokens.offer (ScanToken.BEGIN);
         while (rtok.nextToken() != ']') {
            if (rtok.ttype == ReaderTokenizer.TT_WORD || rtok.ttype == '"') {
               tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
            }
            else {
               throw new IOException (
                  "Expecting property reference, got "+rtok);
            }
         }
         tokens.offer (ScanToken.END);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         rtok.parseNumbers (true);
         rtok.setCharSetting ('-', dsave);
         rtok.setCharSetting (':', csave);
      }
      return tokens.size() - oldsize - 2;
   }

   /**
    * Attempts to scan a reference path associated with a given attribute name.
    * Checks if the current token is a word matching the attribute name. If so,
    * scans '=' and the reference path, stores the both the attribute name and
    * the reference path in the token queue, and returns <code>true</code>.
    * Otherwise, returns <code>false</code>.
    * 
    * @param rtok input token stream
    * @param name attribute name
    * @param tokens token storage queue for postscan
    * @return true if the attribute name was matched and the reference scanned
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanAndStoreReference (
      ReaderTokenizer rtok, String name, Deque<ScanToken> tokens) 
      throws IOException {
      if (rtok.ttype == TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         tokens.offer (new StringToken (name, rtok.lineno()));
         if (!scanAndStoreReference (rtok, tokens)) {
            throw new IOException ("Expected reference path, got " + rtok);
         }
         return true;
      }
      return false;
   }
   
   /**
    * Attempts to scan a set of reference paths associated with a given 
    * attribute name. Checks if the current token is a word matching the 
    * attribute name. If so, scan '=' and the reference paths (
    * assumed to be enclosed by square brackets), store the
    * paths in the token queue, and return the number of paths found.
    * Otherwise, return -1.
    * 
    * @param rtok input token stream
    * @param name attribute name 
    * @param tokens token storage queue for postscan
    * @return number of paths found, or -1 if the attribute name
    * did not match
    * @throws IOException if an I/O or syntax error occurred
    */
   public static int scanAndStoreReferences (
      ReaderTokenizer rtok, String name, Deque<ScanToken> tokens) 
      throws IOException {
      if (rtok.ttype == TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         tokens.offer (new StringToken (name, rtok.lineno()));
         return scanAndStoreReferences (rtok, tokens);
      }
      return -1;
   }
   
   /**
    * Attempts to scan a property path associated with a given attribute name.
    * Checks if the current token is a word matching the 
    * attribute name. If so, scan '=' and the property path, store the
    * path in the token queue, and return <code>true</code>.
    * Otherwise, return <code>false</code>.
    * 
    * @param rtok input token stream
    * @param name attribute name
    * @param tokens token storage queue for postscan
    * @return true if the attribute name was matched
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanAndStorePropertyPath (
      ReaderTokenizer rtok, String name, Deque<ScanToken> tokens) 
      throws IOException {
      if (rtok.ttype == TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         tokens.offer (new StringToken (name, rtok.lineno()));
         scanAndStorePropertyPath (rtok, tokens);
         return true;
      }
      return false;
   }
   
   /**
    * Attempts to scan a set of property paths associated with a given attribute
    * name. Checks if the current token is a word matching the 
    * attribute name. If so, scan '=' and the property paths (assumed to
    * be enclosed in square brackets), store the
    * paths in the token queue, and return <code>true</code>.
    * Otherwise, return <code>false</code>.
    * 
    * @param rtok input token stream
    * @param name attribute name
    * @param tokens token storage queue for postscan
    * @return number of paths found, or -1 if the attribute name
    * did not match
    * @throws IOException if an I/O or syntax error occurred
    */
   public static int scanAndStorePropertyPaths (
      ReaderTokenizer rtok, String name, Deque<ScanToken> tokens) 
      throws IOException {
      if (rtok.ttype == TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         tokens.offer (new StringToken (name, rtok.lineno()));
         return scanAndStorePropertyPaths (rtok, tokens);
      }
      return -1;
   }

   /**
    * Checks if the next token in the queue is a StringToken
    * containing the specified name. If so, consumes that token
    * and returns <code>true</code>. Otherwise, returns <code>false</code>.
    * 
    * @param tokens queue of stored tokens
    * @param name name to check on the token queue
    * @return <code>true</code> if the next token is a StringToken containing
    * <code>name</code>.
    */
   public static boolean postscanAttributeName (Deque<ScanToken> tokens, String name) {
      if (tokens.peek() instanceof StringToken) {
         StringToken tok = (StringToken)tokens.peek();
         if (tok.value().equals (name)) {
            tokens.poll();
            return true;
         }
      }
      return false;
   }

   /**
    * Checks if the next token in the queue is a BEGIN token for
    * a component, and if so, consumes it. Otherwise, throws an exception.
    * @param tokens queue of stored tokens
    * @param comp component associated with the BEGIN
    * @throws IOException if an I/O or syntax error occurred
    */
   public static void postscanComponentBegin (
      Deque<ScanToken> tokens, ModelComponent comp) throws IOException {
      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException (
            "BEGIN token expected for " +
            ComponentUtils.getDiagnosticName(comp) + ", got " + tok);
      }
   }
    
   /**
    * Checks if the next token on the queue is an ObjectToken
    * holding a component reference. If so, consumes
    * the token, postscans the component, and returns <code>true</code>.
    * Otherwise, returns <code>false</code>.
    * 
    * @param tokens queue of stored tokens
    * @param ancestor ancestor for evaluating reference paths
    * @return true if the next token is an ObjectToken
    * holding a component reference
    * @throws IOException if unexpected token input was encountered
    */
   public static boolean postscanComponent (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {
      
      ScanToken tok = postscanComponentToken (tokens);
      if (tok != null) {
         ModelComponent comp = (ModelComponent)tok.value();
         comp.postscan (tokens, ancestor);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Checks if the next token on the queue is a StringToken matching a 
    * specific attribute name. If so, consumes the token, 
    * looks for an ObjectToken holding a component and postscans 
    * that component.
    * 
    * @param tokens queue of stored tokens
    * @param name attribute name
    * @param ancestor ancestor for evaluating reference paths
    * @return true if the attribute name was matched and the component scanned
    * @throws IOException if unexpected token input was encountered
    */
   public static boolean postscanComponent (
      Deque<ScanToken> tokens, String name, CompositeComponent ancestor) 
      throws IOException {
      
      if (postscanAttributeName (tokens, name)) {
         ScanToken tok = postscanComponentToken (tokens);
         if (tok != null) {
            ModelComponent comp = (ModelComponent)tok.value();
            comp.postscan (tokens, ancestor);
            return true;
         }
         else {
            throw new IOException (
               "ObjectToken holding a component expected");
         }
      }
      else {
         return false;
      }
   }
   
   /**
    * Checks if the next token on the queue is an ObjectToken
    * holding a component reference. If so, consumes
    * the token and returns it. Otherwise, returns <code>null</code>.
    * 
    * @param tokens queue of stored tokens
    * @return ObjectToken holding a component reference, or <code>null</code>.
    * @throws IOException if unexpected token input was encountered
    */
   public static ScanToken postscanComponentToken (Deque<ScanToken> tokens) 
      throws IOException {
      
      if (tokens.peek().value() instanceof ModelComponent) {
         return tokens.poll();
      }
      return null;
   }

   public static <C> C postscanReference (
      StringToken strtok, Class<C> clazz, CompositeComponent ancestor)
      throws IOException {

      ModelComponent comp = null;
      String str = strtok.value();
      if (str.equals ("null")) {
         return null;
      }
      comp = ComponentUtils.findComponent (ancestor, str);
      if (comp == null) {
         throw new IOException (
            "Can't find component corresponding to "+str+
            ", line "+strtok.lineno());
      }
      if (clazz.isAssignableFrom(comp.getClass())) {
         return (C)comp;
      }
      else {
         throw new IOException (
            "Component "+comp+" referenced by " + str +
            " not an instance of " + clazz +", line " + strtok.lineno());
      }
   }

   /**
    * Removes the next token from the queue, checks that it is a StringToken 
    * containing a reference path relative to <code>ancestor</code>, locates
    * the referenced component, verifies that it is an instance of 
    * <code>clazz</code>, and returns it.
    *
    * @param tokens queue of stored tokens
    * @param clazz class that the referenced component must be an instance of
    * @param ancestor ancestor for the reference path
    * @return referenced component 
    * @throws IOException if the next token is not a StringToken or
    * if the reference cannot be found
    */
   public static <C> C postscanReference (
      Deque<ScanToken> tokens, Class<C> clazz, CompositeComponent ancestor)
      throws IOException {
      
      ScanToken tok = tokens.poll();
      if (tok instanceof StringToken) {
         StringToken strtok = (StringToken)tok;
         return postscanReference (strtok, clazz, ancestor);
      }
      else {
         throw new IOException ("Token "+tok+" is not a string token");
      }
   }

   /**
    * Checks that the next set of tokens in the stream consist of
    * a BEGIN token, a set of reference path names, and an END token.
    * The corresponding components are then located with respect to
    * <code>ancestor</code>, type checked to ensure that they are
    * instances of <code>clazz</code>, and then returned as an array. 
    * 
    * @param tokens queue of stored tokens
    * @param clazz class that each referenced component must be an instance of
    * @param ancestor ancestor for the reference path
    * @return array of component references
    * @throws IOException if the token structure is incorrect or if
    * the referenced components cannot be found.
    */
   public static <C> C[] postscanReferences (
      Deque<ScanToken> tokens, Class<C> clazz, CompositeComponent ancestor)
      throws IOException {

      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException ("BEGIN token expected, got "+tok);
      }
      LinkedList<C> refs = new LinkedList<C>();
      while (tokens.peek() != ScanToken.END) {
         refs.add (postscanReference (tokens, clazz, ancestor));
      }
      tokens.poll(); // consume BEGIN token
      return refs.toArray ((C[])Array.newInstance (clazz, 0));
   }
   
   /**
    * Checks that the next set of tokens in the stream consist of
    * a BEGIN token, a set of reference path names, and an END token.
    * The corresponding components are then located with respect to
    * <code>ancestor</code>, type checked to ensure that they are
    * instances of <code>clazz</code>, and then returned in
    * the collection <code>refs</code>. 
    * 
    * @param tokens queue of stored tokens
    * @param refs returns the component references
    * @param clazz class that each referenced component must be an instance of
    * @param ancestor ancestor for the reference path
    * @throws IOException if the token structure is incorrect or if
    * the referenced components cannot be found.
    */
   public static <C> void postscanReferences (
      Deque<ScanToken> tokens, Collection<C> refs, Class<C> clazz,
      CompositeComponent ancestor) throws IOException {

      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException ("BEGIN token expected, got "+tok);
      }
      while (tokens.peek() != ScanToken.END) {
         refs.add (postscanReference (tokens, clazz, ancestor));
      }
      tokens.poll(); // eat terminator
   }

   /**
    * Removes the next token from the queue, checks that it is a StringToken
    * containing a property path relative to <code>ancestor</code>, and locates
    * and returns the property.
    *
    * @param tokens queue of stored tokens
    * @param ancestor ancestor for the property path
    * @return property corresponding to the path
    * @throws IOException if the next token is not a StringToken or
    * if the property cannot be found
    */
   public static Property postscanProperty (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {

      ScanToken tok = tokens.poll();
      if (tok instanceof StringToken) {
         StringToken strtok = (StringToken)tok;
         Property prop = null;
         prop = ComponentUtils.findProperty (ancestor, strtok.value());
         if (prop == null) {
            throw new IOException (
               "Can't find property corresponding to "+strtok.value()+
               ", line "+strtok.lineno());
         }
         return prop;
      }
      else {
         throw new IOException (
            "Token "+tok+" is not a StringToken");
      }  
   }
    
   /**
    * Checks that the next set of tokens in the stream consist of
    * a BEGIN token, a set of property paths, and an END token.
    * The corresponding properties are then located with respect to
    * <code>ancestor</code> and returned in an ArrayList.
    * 
    * @param tokens queue of stored tokens
    * @param ancestor ancestor for the reference path
    * @return ArrayList of properties
    * @throws IOException if the token structure is incorrect or if
    * the properties cannot be found
    */
   public static ArrayList<Property> postscanProperties (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {

      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException ("BEGIN token expected, got "+tok);
      }
      ArrayList<Property> list = new ArrayList<Property>();
      while (tokens.peek() !=  ScanToken.END) {
         list.add (postscanProperty (tokens, ancestor));
      }
      tokens.poll(); // consume BEGIN token
      return list;
   }


   /**
    * Performs both a scan and a postscan for a specific component.
    * This will include creating the token storage queue. If the
    * component is the top node of the hierarchy, then the specified
    * ancestor should be the component itself.
    *  
    * @param rtok input token stream
    * @param comp component to scan
    * @param ancestor ancestor component for resolving references
    * @throws IOException if an I/O or syntax error occurred
    */
   public static void scanfull (
      ReaderTokenizer rtok, ModelComponent comp, CompositeComponent ancestor)
      throws IOException {

      ArrayDeque<ScanToken> tokens = new ArrayDeque<ScanToken>();
      comp.scan (rtok, tokens);
      if (myTokenPrinting) {
         printTokens (tokens);
      }
      comp.postscan (tokens, ancestor);         
   }

   /**
    * Scans and discards the input associated with a component, which is
    * assumed to be enclosed within square brackets. This is used in situations
    * where we want to ignore the input associated with a particular component.
    *
    * @param rtok input token stream
    */
   public static void scanAndDiscard (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      int level = 0;
      while (rtok.nextToken() != ']' || level > 0) {
         if (rtok.ttype == '[') {
            level++;
         }
         else if (rtok.ttype == ']') {
            level--;
         }
      }
   }

   protected static ScanToken recursivelyPrintTokens (
      PrintWriter pw, Deque<ScanToken> tokens) {

      while (true) {
         ScanToken tok = tokens.poll();
         boolean recurse = false;
         if (tok == null || tok ==  ScanToken.END) {
            return tok;
         }
         else if (tok.value() instanceof ModelComponent) {
            ModelComponent comp = (ModelComponent)tok.value();
            pw.print (comp.getClass().getSimpleName());
            String name;
            if (comp.getName() != null) {
               name = "\"" + comp.getName() + "\"";
            }
            else {
               name = "" + comp.getNumber();
            }
            pw.println ("["+name+"] line " + tok.lineno());
         }
         else if (tok == ScanToken.BEGIN) {
            pw.println ("BEGIN");
            recurse = true;
         }
         else {
            pw.println (tok);
         }
         if (recurse) {
            IndentingPrintWriter.addIndentation (pw, 2);
            ScanToken endtok = recursivelyPrintTokens (pw, tokens);
            IndentingPrintWriter.addIndentation (pw, -2);           
            if (endtok == ScanToken.END) {
               pw.println ("END");
            }
         }
      }
   }

   /**
    * Diagnostic method to print a token queue to the standard 
    * output stream.
    * 
    * @param tokens token queue to be printed
    */
   public static void printTokens (Deque<ScanToken> tokens) {
      IndentingPrintWriter pw =
         new IndentingPrintWriter (new OutputStreamWriter (System.out));
      printTokens (pw, tokens);
   }
   
   /**
    * Diagnostic method to print a token queue to a print writer.
    * 
    * @param pw print writer for printing the token queue
    * @param tokens token queue to be printed
    */
   public static void printTokens (PrintWriter pw, Deque<ScanToken> tokens) {
      ArrayDeque<ScanToken> copyTokens = new ArrayDeque<ScanToken>(tokens);
      if (!(pw instanceof IndentingPrintWriter)) {
         pw = new IndentingPrintWriter (pw);
      }
      recursivelyPrintTokens (pw, copyTokens);
      pw.flush();
   }
   
   /**
    * Attempts to scan and set a property value for a specified host.
    * Checks if the current token is a word matching one of the host's
    * property names. If so, scan either '=' or ':' and the following
    * property value,
    * set this value within the host, and return <code>true</code>.
    * Otherwise, return <code>false</code>.
    * 
    * @param rtok input token stream
    * @param host host containing the property
    * @return <code>true</code> if a property was matched and scanned
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanProperty (
      ReaderTokenizer rtok, HasProperties host) throws IOException {
      
      if (rtok.ttype != TT_WORD) {
         return false;
      }
      String propName = rtok.sval;
      PropertyInfo propInfo = host.getAllPropertyInfo().get (propName);
      
      if (propInfo != null) {
         int tok = rtok.nextToken ();
         if (tok == '=') {
            Property prop = propInfo.createHandle (host);
            prop.set (propInfo.scanValue (rtok));
            return true;
         }
         else if (tok == ':') {
            if (!propInfo.isInheritable()) {
               throw new IOException (
                  "':' qualifier inappropriate for non-inheritable property '"+
                  propName+", "+rtok);
            }
            InheritableProperty prop =
               (InheritableProperty)propInfo.createHandle (host);
            String qualifier = rtok.scanWord();
            if (qualifier.equals ("Inherited")) {
               prop.setMode (PropertyMode.Inherited);
            }
            else if (qualifier.equals ("Inactive")) {
               prop.setMode (PropertyMode.Inactive);
            }
            else {
               throw new IOException (
                  "unrecognized qualifier " + qualifier +
                  " for property "+propName+", " + rtok);
            }
            return true;
         }
         else {
            throw new IOException (
               "Expected '=' or ':' for property "+propName+", got " + rtok);
         }
      }
      else {
         return false;
      }
   }
   
   /**
    * Scans a specific property value for a 
    * host, assuming that the property name has already been
    * scanned and matched. Scans either '=' or ':' and the 
    * following property value, and returns the value.
    * 
    * @param rtok input token stream
    * @param host host containing the property
    * @param propName property to try and scan
    * @return the scanning property value
    * @throws IOException if an I/O or syntax error occurred
    */
   public static Object scanPropertyValue (
      ReaderTokenizer rtok, HasProperties host, String propName) 
      throws IOException {

      PropertyInfo propInfo = host.getAllPropertyInfo().get (propName);
      if (propInfo != null) {
         int tok = rtok.nextToken ();
         if (tok == '=') {
            Property prop = propInfo.createHandle (host);
            return propInfo.scanValue (rtok);
         }
         else if (tok == ':') {
            if (!propInfo.isInheritable()) {
               throw new IOException (
                  "':' qualifier inappropriate for non-inheritable property '"+
                  propName+", "+rtok);
            }
            InheritableProperty prop =
               (InheritableProperty)propInfo.createHandle (host);
            String qualifier = rtok.scanWord();
            if (qualifier.equals ("Inherited")) {
               return PropertyMode.Inherited;
            }
            else if (qualifier.equals ("Inactive")) {
               return PropertyMode.Inactive;
            }
            else {
               throw new IOException (
                  "unrecognized qualifier " + qualifier +
                  " for property "+propName+", " + rtok);
            }
         }
         else {
            throw new IOException (
               "Expected '=' or ':' for property "+propName+", got " + rtok);
         }
      }
      else {
         throw new IOException (
            "Property '"+propName+"' not found in host "+host);
      }
   }

   protected static void postscanSetPropertyValue (
      HasProperties host, String propName, Object value)
      throws IOException {

      PropertyInfo propInfo = host.getAllPropertyInfo().get (propName);
      if (propInfo != null) {
         if (value instanceof PropertyMode) {
             InheritableProperty prop =
               (InheritableProperty)propInfo.createHandle (host);  
             prop.setMode ((PropertyMode)value);
         }
         else {
            Property prop = propInfo.createHandle (host);
            prop.set (value);
         }
      }
      else {
         throw new IOException (
            "Property '"+propName+"' not found in host "+host);
      }
   }

   /**
    * Attempts to scan and store a specific set of property values
    * for a specified host. Checks if the current token is a word 
    * matching one of the specified property names. 
    * If so, scan either '=' or ':' and the following
    * property value, store the property name and value in the token 
    * queue using a <code>StrinToken</code> and an <code>ObjectToken</code>, 
    * and return <code>true</code>.
    * Otherwise, return <code>false</code>.
    * <p>
    * This method is intended for handling property values which
    * must be set in the <i>post-scan</i> stage after references
    * have been set. 
    * 
    * @param rtok input token stream
    * @param host host containing the properties
    * @param propNames names of properties to match
    * @param tokens token queue for postscan
    * @return <code>true</code> if a property value was scanned
    * and stored. 
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanAndStorePropertyValues (
      ReaderTokenizer rtok, HasProperties host, String[] propNames,
      Deque<ScanToken> tokens) throws IOException {

      if (rtok.ttype == TT_WORD) {
         for (int i=0; i<propNames.length; i++) {
            if (rtok.sval.equals (propNames[i])) {
               tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
               Object val = scanPropertyValue (rtok, host, propNames[i]);
               tokens.offer (new ObjectToken (val));
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Attempts to scan and store a specific property value for a specified
    * host. Checks if the current token is a word matching the specified
    * property name.  If so, scan either '=' or ':' and the following property
    * value, store the property name and value in the token queue using a
    * <code>StrinToken</code> and an <code>ObjectToken</code>, and return
    * <code>true</code>.  Otherwise, return <code>false</code>.
    *
    * <p> This method is intended for handling property values which must be
    * set in the <i>post-scan</i> stage after references have been set.
    * 
    * @param rtok input token stream
    * @param host host containing the properties
    * @param propName name of the property to match
    * @param tokens token queue for postscan
    * @return <code>true</code> if z roperty value was scanned
    * and stored.
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanAndStorePropertyValue (
      ReaderTokenizer rtok, HasProperties host, String propName,
      Deque<ScanToken> tokens) throws IOException {

      if (rtok.ttype == TT_WORD) {
         if (rtok.sval.equals (propName)) {
            tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
            Object val = scanPropertyValue (rtok, host, propName);
            tokens.offer (new ObjectToken (val));
            return true;
         }
      }
      return false;
   }

   /**
    * Checks if the next token in the queue is a StringToken
    * containing one of a specified set of property names. If
    * so, consumes that token, along with a following token
    * giving the property value, sets that property value within
    * the host, and returns <code>true</code>. Otherwise,
    * returns <code>false</code>.
    * 
    * @param tokens queue of stored tokens
    * @param host host containing the properties
    * @param propNames names of the properties to match
    * @return <code>true</code> if a property was matched and set
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean postscanPropertyValues (
      Deque<ScanToken> tokens, HasProperties host, String[] propNames) 
      throws IOException {

      if (tokens.peek() instanceof StringToken) {
         StringToken strtok = (StringToken)tokens.peek();
         for (int i=0; i<propNames.length; i++) {
            if (strtok.value().equals (propNames[i])) {
               tokens.poll();
               if (tokens.peek() instanceof ObjectToken) {
                  postscanSetPropertyValue (
                     host, propNames[i], tokens.poll().value());
                  return true;
               }
               else {
                  throw new IOException ("Unexpected token " + tokens.peek());
               }
            }
         }
      }
      return false;
   }

   /**
    * Checks if the next token in the queue is a StringToken containing a
    * specified property name. If so, consumes that token, along with a
    * following token giving the property value, sets that property value
    * within the host, and returns <code>true</code>. Otherwise, returns
    * <code>false</code>.
    * 
    * @param tokens queue of stored tokens
    * @param host host containing the properties
    * @param propName names of the property to match
    * @return <code>true</code> if a property was matched and set
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean postscanPropertyValue (
      Deque<ScanToken> tokens, HasProperties host, String propName) 
      throws IOException {

      if (tokens.peek() instanceof StringToken) {
         StringToken strtok = (StringToken)tokens.peek();
         if (strtok.value().equals (propName)) {
            tokens.poll();
            if (tokens.peek() instanceof ObjectToken) {
               postscanSetPropertyValue (
                  host, propName, tokens.poll().value());
               return true;
            }
            else {
               throw new IOException ("Unexpected token " + tokens.peek());
            }
         }
      }
      return false;
   }

   /**
    * Writes the reference paths for a set of components to a
    * print writer, enclosed by square brackets.
    * 
    * @param pw PrintWriter to write the references to
    * @param comps components providing the reference paths
    * @param ancestor ancestor component with respect to which reference
    * paths should be generated
    */
   public static void writeBracketedReferences (
      PrintWriter pw, Collection<? extends ModelComponent> comps,
      CompositeComponent ancestor) throws IOException {
   
      if (comps.size() == 0) {
         pw.println ("[ ]");
      }
      else {
         IndentingPrintWriter.printOpening (pw, "[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (ModelComponent c : comps) {
            pw.println (ComponentUtils.getWritePathName (ancestor, c));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }
   
   /**
    * Writes the reference paths for a set of components to a
    * print writer. Unlike {@link #writeBracketedReferences
    * writeBracketedReferences()}, the paths are not enclosed
    * within square brackets.
    * 
    * @param pw PrintWriter to write the references to
    * @param comps components providing the reference paths
    * @param ancestor ancestor component with respect to which reference
    * paths should be generated
    */   
   public static void writeReferences (
      PrintWriter pw, Iterable<ModelComponent> comps,
      CompositeComponent ancestor) throws IOException {
      for (ModelComponent c : comps) {
         pw.println (ComponentUtils.getWritePathName (ancestor, c));
      }
   }

   /**
    * Attempts to scan a class-qualified component associated with a given
    * attribute name.  Checks if the current token is a word matching the
    * attribute name. If so, scans '=' and the component class name,
    * instantiates the component and scans that, places both the attribute name
    * and the component in the in the token queue, and returns
    * <code>true</code>.  Otherwise, returns <code>false</code>.
    * 
    * @param rtok input token stream
    * @param name attribute name
    * @param tokens token storage queue for postscan
    * @return true if the attribute name was matched and the component was
    * scanned
    * @throws IOException if an I/O or syntax error occurred
    */
   public static boolean scanAndStoreComponent (
      ReaderTokenizer rtok, String name, Deque<ScanToken> tokens)
      throws IOException {

      if (rtok.ttype == TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         tokens.offer (new StringToken (name, rtok.lineno()));

         String className = rtok.scanWord();
         Class<?> compClass = ClassAliases.resolveClass (className);
         if (compClass == null) {
            throw new IOException (
               "Class name or alias '"+className+"' can't be resolved, "+rtok);
         }
         Scannable comp = null;
         try {
            comp = (Scannable)compClass.newInstance();
         }
         catch (Exception e) {
            throw new IOException (
               "Class corresponding to '"+ className+
               "' cannot be instantiated", e);
         }
         tokens.offer (new ObjectToken (comp, rtok.lineno()));
         comp.scan (rtok, tokens);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Calls <code>postscan()</code> for an component stored in the token queue.
    * Removes the next token from the queue, checks that it is an ObjectToken
    * containing an component, verifies that the component it is an instance of
    * <code>clazz</code>, calls <code>postscan()</code> for the component and
    * returns it.
    *
    * @param tokens queue of stored tokens
    * @param clazz class that the component must be an instance of
    * @param ancestor ancestor uses for post-scanning the component
    * @return component that was on the queue
    * @throws IOException if the next token is not an ObjectToken, the component
    * is not an instance of <code>clazz</code>, or an error occured within the
    * component's <code>postscan()</code> call.
    */
   public static <C> C postscanComponent (
      Deque<ScanToken> tokens, Class<C> clazz, CompositeComponent ancestor)
      throws IOException {
      
      ScanToken tok = tokens.poll();
      if (tok instanceof ObjectToken) {
         Object obj = ((ObjectToken)tok).value();
         if (!(clazz.isAssignableFrom (obj.getClass()))) {
            throw new IOException ("Component "+obj+" not instance of "+clazz);
         }
         C comp = (C)obj;
         if (!(comp instanceof ModelComponent)) {
            throw new IOException ("Component "+obj+" is not a ModelComponent");
         }
         ((ModelComponent)comp).postscan (tokens, ancestor);
         return comp;
      }
      else {
         throw new IOException ("Token "+tok+" is not an ObjectToken");
      }
   }
   
   /**
    * Writes a class-qualified component to a print writer.
    * 
    * @param pw PrintWriter to write the component to
    * @param fmt numeric format for writing the component
    * @param obj component to be written
    * @param ref reference object used for writing the component
    */   
   public static void writeComponent (
      PrintWriter pw, NumberFormat fmt, Scannable obj, Object ref)
      throws IOException {

      pw.print (ClassAliases.getAliasOrName (obj.getClass()));
      pw.print (" ");
      obj.write (pw, fmt, ref);
   }

   public static void writeVector3dList (
      PrintWriter pw, NumberFormat fmt, Vector3d[] list) throws IOException {

      if (list.length == 0) {
         pw.println ("[ ]");
      }
      else {
         IndentingPrintWriter.printOpening (pw, "[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<list.length; i++) {
            list[i].write (pw, fmt);
            pw.println ("");
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   public static Vector3d[] scanVector3dList (ReaderTokenizer rtok)
      throws IOException {

      ArrayList<Vector3d> list = new ArrayList<Vector3d>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         Vector3d vec = new Vector3d();
         vec.scan (rtok);
         list.add (vec);
      }
      return list.toArray (new Vector3d[0]);
   }

}

 
