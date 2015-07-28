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

      HashMap<String,Integer> infoOrder;
      ArrayList<ModelHistoryInfo> info;
      // purposely ascending so more efficient sorting on update
      ModelHistoryComparatorAscending cmp;
      
      public ModelHistory() {
         infoOrder = new HashMap<>();
         info = new ArrayList<>();
         cmp = new ModelHistoryComparatorAscending();
      }
      
      private int getPos(String longName) {
         Integer idx = infoOrder.get(longName);
         if (idx == null) {
            return -1;
         }
         return idx.intValue();
      }
      
      public ModelHistoryInfo get(String longName) {
         int idx = getPos(longName);
         if (idx < 0) {
            return null;
         }
         return info.get(idx);
      } 
      
      private void add(String longName, ModelHistoryInfo mhi) {
         // insertion
         int min = 0;
         int max = info.size()-1;
         int idx = 0;
         boolean done = false;
         
         if (info.size() == 0) {
            idx = 0;
         } else {
            // binary search for slot
            while (!done) {
               if (cmp.compare(mhi, info.get(min)) <= 0) {
                  idx = min;
                  done = true;
               } else if (cmp.compare(info.get(max), mhi) < 0) {
                  idx = max+1;
                  done = true;
               } else {
                  int mid = (min + max)/2;
                  if (cmp.compare(mhi, info.get(mid)) < 0) {
                     max = mid;
                  } else if (cmp.compare(info.get(mid), mhi) < 0) {
                     min = mid;
                  }
               }
            }
         }
         
         // insert into correct position
         info.add(idx, mhi);
         // adjust order indices
         for (int i=idx; i<info.size(); i++) {
            infoOrder.put(info.get(idx).getModelInfo().getClassNameOrFile(), i);
         }
         
      }
      
      private void updateOrder(int pos) {
         ModelHistoryInfo mhi = info.get(pos);
         
         // move down if need to
         while ( (pos > 0) && (cmp.compare(mhi, info.get(pos-1))<0) ) {
            ModelHistoryInfo mhip = info.get(pos-1); 
            info.set(pos, mhip);
            infoOrder.put(mhip.getModelInfo().getClassNameOrFile(), pos);
            --pos;
         }
         // move up if need to
         while ( (pos < info.size()-1) && (cmp.compare(mhi, info.get(pos+1))>0)) {
            ModelHistoryInfo mhip = info.get(pos+1);
            info.set(pos, mhip);
            infoOrder.put(mhip.getModelInfo().getClassNameOrFile(), pos);
            ++pos;
         }
         
         // set current position
         info.set(pos, mhi);
         infoOrder.put(mhi.getModelInfo().getClassNameOrFile(), pos);
            
      }
      
      public ModelHistoryInfo update(ModelInfo mi, Date dateTime) {
         int pos = getPos(mi.classNameOrFile);
         ModelHistoryInfo mhi = null;
         if (pos >= 0) {
            mhi = info.get(pos);
            mhi.update(mi, dateTime);
            updateOrder(pos);
         } else {
            mhi = new ModelHistoryInfo(mi, dateTime);
            add(mi.getClassNameOrFile(), mhi);
         }
         return mhi;
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

   public static class ModelHistoryInfo implements Comparable<ModelHistoryInfo> {
      ModelInfo mi;
      Date time;
      
      public ModelHistoryInfo(ModelInfo mi, Date time) {
         this.mi = mi;
         this.time = time;
      }
      
      public ModelInfo getModelInfo() {
         return mi;
      }
      
      public void update(ModelInfo mi, Date datetime) {
         if (datetime.compareTo(time) > 0) {
            this.time = datetime;
            this.mi = mi;
         }
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
      
      @Override
      public int compareTo(ModelHistoryInfo o) {
         // latest date wins
         return time.compareTo(o.time);
      }
   }
   
   private static class ModelHistoryComparatorAscending 
      implements Comparator<ModelHistoryInfo> {

      @Override
      public int compare(ModelHistoryInfo o1, ModelHistoryInfo o2) {
         return o1.compareTo(o2);
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
               char quote = (char)rtok.ttype;
               return  rtok.sval; // remove quote
            }
            return Character.toString((char)(rtok.ttype)); // other characters
         }
      }
   }

   protected static String[] readToEOL(ReaderTokenizer rtok)
      throws IOException {

      ArrayList<String> args = new ArrayList<String>();

      rtok.nextToken();
      while (rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (rtok.ttype == ReaderTokenizer.TT_EOF) {
            break;
         }
         args.add(getTokenString(rtok));
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
      rtok.nextToken();
      while (rtok.ttype == ReaderTokenizer.TT_WORD) {
         String longName = rtok.sval;
         String shortName = rtok.scanQuotedString('"');
         long tval = rtok.scanLong();
         String[] args = readToEOL(rtok);
         Date dateTime = new Date(tval);
         update(new ModelInfo(longName, shortName, args), dateTime);
         rtok.nextToken();  // read next token
      }
      rtok.close();
   }
   
   public void save(File file) throws IOException {
      PrintWriter writer = new PrintWriter(file);
      
      // System.out.println("History:");
      writer.println("# class \"shortname\" time [args]");
      for (ModelHistoryInfo mhi : info) {
         ModelInfo mi = mhi.mi;
         writer.print(mi.getClassNameOrFile() + " \"" + mi.shortName + "\" " + mhi.time.getTime() );
         if (mi.args != null) {
            for (String str : mi.args) {
               writer.print(" ");
               if (str.contains(" ")) {
                  writer.print("\""+str+"\"");   
               } else {
                  writer.print(str);
               }
            }
         }
         writer.println();
         
         //         System.out.print(mi.getClassNameOrFile() + " \"" + mi.shortName + "\" " + mhi.time.getTime() );
         //         if (mi.args != null) {
         //            for (String str : mi.args) {
         //               System.out.print(" ");
         //               if (str.contains(" ")) {
         //                  System.out.print("\""+str+"\"");   
         //               } else {
         //                  System.out.print(str);
         //               }
         //            }
         //         }
         //         System.out.println();
      }
      writer.close();
      
   }
   
}
