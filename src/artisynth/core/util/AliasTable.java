/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.net.URL;

import maspack.util.*;

/**
 * A hash-table that keeps track of names and aliases.
 */
public class AliasTable {
   private LinkedHashMap<String,String> aliasesToNames =
      new LinkedHashMap<String,String>();

   /**
    * Creates a new AliasTable with no entries.
    */

   public AliasTable() {
   }

   /**
    * Creates a new AliasTable and reads it's initial entries from a file.
    * 
    * @param file
    * File from which to read table entries
    * @throws IOException
    * if there was an error reading the file
    */
   public AliasTable (File file) throws IOException {
      super();
      read (new FileInputStream (file));
   }

   /**
    * Creates a new AliasTable and reads it's initial entries from a resource.
    * 
    * @param url
    * Resource from which to read table entries
    * @throws IOException
    * if there was an error reading the resource
    */
   public AliasTable (URL url) throws IOException {
      super();
      read (url.openStream());
   }

   /**
    * Reads the contents of an input stream and adds every successive pair of
    * strings into this table as an alias/name pair. Strings are delineated
    * either by double quotes <code>"</code> or by whitespace.
    * Whitespace-delineated strings may contain alphanumeric characters, plus
    * the characters <code>$./_-</code>, but must not start with a digit.
    * Quoted strings may contain any character and support the usual escape
    * sequences. This function returns a list of all the aliases read.
    * 
    * @param s
    * Stream from which to read table entries
    * @throws IOException
    * if there was an error reading the stream
    */
   public void read (InputStream s) throws IOException {
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new InputStreamReader (s)));

      rtok.commentChar ('#');
      rtok.wordChar ('/');
      rtok.wordChar ('.');
      rtok.wordChar ('$');
      rtok.wordChar ('_');
      rtok.wordChar ('-');

      String alias = null;
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF &&
             rtok.tokenIsWordOrQuotedString ('"')) {
         if (alias == null) {
            alias = rtok.sval;
         }
         else {
            addEntry (alias, rtok.sval);
            alias = null;
         }
      }
      if (rtok.ttype != ReaderTokenizer.TT_EOF) {
         throw new IOException ("Warning: unknown token " + rtok);
      }
   }

   /**
    * Add quotes to a string if it contains white space.
    */
   public String getPrintString (String str) {
      for (int i=0; i<str.length(); i++) {
         if (Character.isWhitespace(str.charAt(i))) {
            return "\"" + str + "\"";
         }
      }
      return str;
   }

   public void write (PrintWriter pw) throws IOException {

      for (Map.Entry<String,String> entry : aliasesToNames.entrySet()) {
         pw.println ("\""+entry.getKey()+"\" "+getPrintString(entry.getValue()));
      }
      pw.flush();
   }

   public void write (String fileName) throws IOException {
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new FileWriter (fileName)));
      write (pw);
      pw.close();
   }

   /**
    * Adds an alias and it's corresponding name to this AliasTable.
    * 
    * @param alias
    * alias which references the name
    * @param name
    * name which is referenced by the alias
    */
   public void addEntry (String alias, String name) {
      aliasesToNames.put (alias, name);
   }
   
   /**
    * Removes an alias from this AliasTable.
    * 
    * @param alias
    * alias which references the name
    */
   public void removeEntry(String alias) {
      aliasesToNames.remove(alias);
   }
   
   /**
    * Clears all entries from the AliasTable
    */
   public void clear() {
      aliasesToNames.clear();
   }

   /**
    * Gets the name referenced by a given alias, or null if the alias has no
    * entry.
    * 
    * @param alias
    * alias which references the name
    * @return name referenced by the alias
    */
   
   public String getName (String alias) {
      return aliasesToNames.get (alias);
   }

   /**
    * Gets the first alias found that references a given name, or null if no
    * such alias is found.
    * 
    * @param name
    * name which is referenced
    * @return first alias which references the name
    */

   public String getAlias (String name) {
      for (Map.Entry<String,String> e : aliasesToNames.entrySet()) {
         if (e.getValue().equals (name))
            return e.getKey();
      }
      return null;
   }

   /**
    * Returns a list of all the aliases in this table.
    * 
    * @return list of all the aliases
    */

   public String[] getAliases() {
      return aliasesToNames.keySet().toArray (new String[0]);
   }

   /**
    * Returns a list of all the names in this table.
    * 
    * @return list of all the name
    */
   public String[] getNames() {
      return aliasesToNames.values().toArray (new String[0]);
   }

   public Set<Entry<String,String>> getEntries() {
      return aliasesToNames.entrySet();
   }
   
   /**
    * Returns true if this table contains the specified name.
    * 
    * @param name
    * to check for
    * @return true if the name is present in this table
    */

   public boolean containsName (String name) {
      return aliasesToNames.containsValue (name);
   }

   /**
    * Returns true if this table contains the specified alias.
    * 
    * @param alias
    * to check for
    * @return true if the alias is present in this table
    */
   public boolean containsAlias (String alias) {
      return aliasesToNames.containsKey (alias);
   }

   /**
    * Returns a Set view of the entries for this table.
    */
   public Set<Map.Entry<String,String>> entrySet() {
      return aliasesToNames.entrySet();
   }
}
