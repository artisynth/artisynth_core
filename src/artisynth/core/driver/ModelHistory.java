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

import maspack.util.ReaderTokenizer;

/**
 * 
 * Class for storing model load history, useful for recent items in menu
 *
 */
public class ModelHistory {

      ArrayList<ModelHistoryInfo> info;
      HashMap<ModelInfo,ModelHistoryInfo> infoMap;
      
      // purposely ascending so more efficient sorting on update
      ModelHistoryTimeComparatorAscending cmp;
      
      public ModelHistory() {
         info = new ArrayList<>();
         infoMap = new HashMap<>();
         cmp = new ModelHistoryTimeComparatorAscending();
      }
        
      private ModelHistoryInfo addOrUpdate(ModelInfo mi, Date dateTime) {
         
         ModelHistoryInfo omhi = infoMap.get (mi);
         int pidx = -1;
         if (omhi != null) {
            // update time
            pidx = omhi.getIndex ();
            omhi.setTime (dateTime);
         } else {
            // add
            ModelHistoryInfo mhi = new ModelHistoryInfo(mi, dateTime);
            infoMap.put (mi, mhi);
            pidx = info.size ();
            mhi.setIndex (pidx);
            info.add (mhi);
            omhi = mhi;
         }
         
         // search down from pidx, updating indices
         while (pidx > 0) {
            ModelHistoryInfo prev = info.get (pidx-1);
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
            ModelHistoryInfo next = info.get (pidx+1);
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
      
      public ModelHistoryInfo update(ModelInfo mi, Date dateTime) {
         return addOrUpdate (mi, dateTime);
      }
      
      /**
       * Returns the k most recently loaded models
       */
      public ModelHistoryInfo[] getRecent(int k) {
         int size = info.size();
         if (size < k) {
            k = size;
         }
         ModelHistoryInfo[] mhis = new ModelHistoryInfo[k];
         
         // since list is sorted, get the last k
         for (int i=0; i<k; ++i) {
            mhis[i] = info.get(info.size()-1-i);
         }
         
         return mhis;
      }

   public static class ModelHistoryInfo {
      ModelInfo mi;
      Date time;
      int idx;
      
      public ModelHistoryInfo(ModelInfo mi, Date time) {
         this.mi = mi;
         this.time = time;
         idx = -1;
      }
      
      public ModelInfo getModelInfo() {
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
   private static class ModelHistoryTimeComparatorAscending 
      implements Comparator<ModelHistoryInfo> {

      @Override
      public int compare(ModelHistoryInfo o1, ModelHistoryInfo o2) {
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
   
   public void read(File file) throws IOException {
      // read history
      // format: longname "short name" time
      ReaderTokenizer rtok = new ReaderTokenizer(new FileReader(file));
      rtok.commentChar('#');
      rtok.eolIsSignificant(true);
      rtok.wordChar('.');
      rtok.wordChar ('\'');
      rtok.nextToken();
      while (rtok.ttype == ReaderTokenizer.TT_WORD) {
         String longName = rtok.sval;
         String shortName = rtok.scanQuotedString('"');
         long tval = rtok.scanLong();
         String[] args = readArgs(rtok);
         Date dateTime = new Date(tval);
         update(new ModelInfo(longName, shortName, args), dateTime);
         rtok.nextToken();  // read next token
      }
      rtok.close();
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
   
   public void save(File file) throws IOException {
      PrintWriter writer = new PrintWriter(file);
      
      writer.println("# class \"shortname\" time [args]");
      for (ModelHistoryInfo mhi : info) {
         ModelInfo mi = mhi.mi;
         writer.print(mi.getClassNameOrFile() + " \"" + mi.shortName + "\" " + mhi.time.getTime() );
         if (mi.args != null) {
            for (String str : mi.args) {
               writer.print(" ");
               writer.print(encode(str));
            }
         }
         writer.println();
         
      }
      writer.close();
      
   }
   
}
