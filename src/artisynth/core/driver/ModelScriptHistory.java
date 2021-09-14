/**
 * Copyright (c) 2015, by the Authors: C Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.util.*;
import maspack.util.*;

/**
 * 
 * Class for storing model load history, useful for recent items in menu
 *
 */
public class ModelScriptHistory implements Scannable {

   ArrayList<ModelScriptHistoryInfo> info;
   HashMap<ModelScriptInfo,ModelScriptHistoryInfo> infoMap;
   File myFile; // file name from which history is saved/loaded

   // purposely ascending so more efficient sorting on update
   TimeComparatorAscending cmp;
      
   public ModelScriptHistory (File file) {
      info = new ArrayList<>();
      infoMap = new HashMap<>();
      cmp = new TimeComparatorAscending();
      myFile = file;
   }

   private ModelScriptHistoryInfo addOrUpdate(ModelScriptInfo mi, Date dateTime) {

      // if (info.size() > 0) {
      //    ModelScriptHistoryInfo last = info.get(info.size()-1);
      //    System.out.println ("new=" + mi);
      //    System.out.println ("  hash=" + mi.hashCode());
      //    System.out.println ("last=" + last.mi);
      //    System.out.println ("  hash=" + last.mi.hashCode());
      //    System.out.println ("equal=" + mi.equals (last.mi));
      // }

      ModelScriptHistoryInfo omhi = infoMap.get (mi);
      int pidx = -1;
      if (omhi != null) {
         // update time
         pidx = omhi.getIndex ();
         omhi.setTime (dateTime);
      }
      else {
         // add
         ModelScriptHistoryInfo mhi = new ModelScriptHistoryInfo(mi, dateTime);
         infoMap.put (mi, mhi);
         pidx = info.size ();
         mhi.setIndex (pidx);
         info.add (mhi);
         omhi = mhi;
      }
         
      // search down from pidx, updating indices
      while (pidx > 0) {
         ModelScriptHistoryInfo prev = info.get (pidx-1);
         if (cmp.compare (prev, omhi) >= 0 ) {
            info.set (pidx, prev);
            prev.idx = pidx;
            --pidx;
         } else {
            break;
         }
      }
         
      // search up from pidx, updating indices
      while (pidx < info.size ()-1) {
         ModelScriptHistoryInfo next = info.get (pidx+1);
         if (cmp.compare (omhi, next) > 0 ) {
            info.set (pidx, next);
            next.idx = pidx;
            ++pidx;
         } else {
            break;
         }
      }
         
      info.set (pidx, omhi);
      omhi.idx = pidx;
         
      return omhi;
   }
      
   public ModelScriptHistoryInfo update(ModelScriptInfo mi, Date dateTime) {
      return addOrUpdate (mi, dateTime);
   }
      
   // /**
   //  * Returns the k most recently loaded models
   //  */
   // public ModelScriptHistoryInfo[] getRecent(int k) {
   //    int size = info.size();
   //    if (size < k) {
   //       k = size;
   //    }
   //    ModelScriptHistoryInfo[] mhis = new ModelScriptHistoryInfo[k];
         
   //    // since list is sorted, get the last k
   //    for (int i=0; i<k; ++i) {
   //       mhis[i] = info.get(info.size()-1-i);
   //    }
         
   //    return mhis;
   // }

   /**
    * Returns information for the {@code max} most recently loaded
    * models or scripts matching the specified types.
    */
   public ArrayList<ModelScriptInfo> getRecent (int max, InfoType... types) {

      ArrayList<ModelScriptInfo> list = new ArrayList<>();
         
      // since list is sorted, search from the end
      int k = info.size()-1;
      while (list.size() < max && k >= 0) {
         ModelScriptInfo mi = info.get(k).mi;
         for (int i=0; i<types.length; i++) {
            if (mi.getType() == types[i]) {
               list.add (mi);
               break;
            }
         }
         k--;
      }
      return list;
   }

   /**
    * Returns information for the most recently loaded model or script matching
    * the specified types, or {@code null} if there is none
    */
   public ModelScriptInfo getMostRecent (InfoType... types) {

      // since list is sorted, search from the end
      for (int k=info.size()-1; k>=0; k--) {
         ModelScriptInfo mi = info.get(k).mi;
         for (int i=0; i<types.length; i++) {
            if (mi.getType() == types[i]) {
               return mi;
            }
         }
      }
      return null;
   }

   public static class ModelScriptHistoryInfo {
      ModelScriptInfo mi;
      Date time;
      int idx;
      
      public ModelScriptHistoryInfo(ModelScriptInfo mi, Date time) {
         this.mi = mi;
         this.time = time;
         idx = -1;
      }
      
      public ModelScriptInfo getModelInfo() {
         return mi;
      }
      
      public void update(Date datetime) {
         if (datetime.compareTo(time) > 0) {
            this.time = datetime;
         }
      }
      
      int getIndex() {
         return idx;
      }
      
      void setIndex(int id) {
         idx = id;
      }

      public Date getTime() {
         return time;
      }
     
      public void setTime(Date time) {
         this.time = time;
      }
      
      public void setTimeNow() {
         this.time = new Date();  //sets to the current date/time
      }
      
   }
   
   /**
    * Compares by date-time
    */
   private static class TimeComparatorAscending 
      implements Comparator<ModelScriptHistoryInfo> {

      @Override
         public int compare(ModelScriptHistoryInfo o1, ModelScriptHistoryInfo o2) {
         return o1.getTime ().compareTo(o2.getTime ());
      }
      
   }

   private static String getTokenString(ReaderTokenizer rtok) {
      switch (rtok.ttype) {
         case ReaderTokenizer.TT_NOTHING:
         case ReaderTokenizer.TT_EOF: {
            return "";
         }
         case ReaderTokenizer.TT_EOL: {
            return "\n";
         }
         case ReaderTokenizer.TT_WORD: {
            return rtok.sval;
         }
         case ReaderTokenizer.TT_NUMBER: {
            if (rtok.tokenIsHexInteger()) {
               return "0x" + Long.toHexString(rtok.lval);
            } else if (rtok.tokenIsInteger()) {
               return Long.toString(rtok.lval);
            } else {
               return Double.toString(rtok.nval);
            }
         }
         default: {
            if (rtok.isQuoteChar(rtok.ttype)) {
               return rtok.sval;
            }
            return Character.toString((char)(rtok.ttype)); // other characters
         }
      }
   }

   protected static String[] readArgs(ReaderTokenizer rtok)
      throws IOException {

      ArrayList<String> args = new ArrayList<String>();
      
      rtok.nextToken();
      while (rtok.ttype != ReaderTokenizer.TT_EOL 
             && rtok.ttype != ReaderTokenizer.TT_EOF) {
         
         args.add (getTokenString(rtok));
         
         rtok.nextToken();
      }
      
      return args.toArray(new String[args.size()]);
   }
   
   private String getFileExtension (String fileName) {
      int idx = fileName.lastIndexOf ('.');
      if (idx != -1) {
         return fileName.substring (idx+1);
      }
      else {
         return "";
      }
   }
   
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.commentChar('#');
      rtok.eolIsSignificant(true);
      rtok.wordChar('.');
      rtok.wordChar ('\'');
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         InfoType type;
         String longName;
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            longName = rtok.sval;
            type = InfoType.CLASS;
         }
         else if (rtok.tokenIsQuotedString ('"')) {
            longName = rtok.sval;
            String ext = getFileExtension (longName);
            if (ext.equals ("py") || ext.equals ("jy")) {
               type = InfoType.SCRIPT;               
            }
            else {
               type = InfoType.FILE;
            }
         }
         else {
            throw new IOException ("Unexpected token: " + rtok);
         }
         String shortName = rtok.scanQuotedString('"');
         long tval = rtok.scanLong();
         String[] args = readArgs(rtok);
         // ignore files that can't be read
         if (type != InfoType.CLASS && !(new File(longName)).canRead()) {
            break;
         }
         Date dateTime = new Date(tval);
         update(new ModelScriptInfo(type, longName, shortName, args), dateTime);
      }
   }

   public boolean load() {
      return ArtisynthIO.load (this, myFile, "model history");
   }
   
   public boolean loadOrCreate () {
      if (ArtisynthIO.loadOrCreate (this, myFile, "model history")) {
         return true;
      }
      myFile = null;
      return false;
   }
   
   private static String encode(String str) {
      boolean quote = false;
      // see if we need to quote
      for (int i=0; i<str.length (); ++i) {
         char c = str.charAt (i);
         if (Character.isWhitespace (c) || c == '"' ) {
            quote = true;
         }
      }
      
      if (!quote) {
         return str;
      }
      
      StringBuilder out = new StringBuilder();
      out.append ('"');
      // escape quotes and backslashes
      for (int i=0; i<str.length (); ++i) {
         char c = str.charAt (i);
         if (c == '"') {
            out.append ('\\');
         } else if (c == '\\') {
            out.append ('\\');
         }
         out.append (c);
      }
      out.append ('"');
      return out.toString ();
   }
   
   public boolean save() {
      if (!ArtisynthIO.save (this, myFile, "model history")) {
         myFile = null;
         return false;
      }
      else {
         return true;
      }
   }

   @Override
   public void write (PrintWriter pw, NumberFormat fmt, Object ref) {
      pw.println("# class \"shortname\" time [args]");
      for (ModelScriptHistoryInfo mhi : info) {
         ModelScriptInfo mi = mhi.mi;
         if (mi.getType() == InfoType.CLASS) {
            pw.print (mi.getClassNameOrFile());
         }
         else {
            pw.print (
               ReaderTokenizer.getQuotedString (mi.getClassNameOrFile(), '"'));
         }
         pw.print(" \""+mi.shortName+"\" "+mhi.time.getTime());
         if (mi.args != null) {
            for (String str : mi.args) {
               pw.print(" ");
               pw.print(encode(str));
            }
         }
         pw.println();
      }
   }

   @Override
   public boolean isWritable () {
      return true;
   }   
}
