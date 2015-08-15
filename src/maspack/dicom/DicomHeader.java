package maspack.dicom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import maspack.matrix.VectorNd;
import maspack.util.InternalErrorException;

public class DicomHeader {


   // add syntaxes
   private static ArrayList<DicomTransferSyntax> transferSyntaxes = new ArrayList<DicomTransferSyntax>(4);
   static {
      // Generic
      transferSyntaxes.add(new DicomTransferSyntax("Little Endian Explicit", "1.2.840.10008.1.2.1", true, true, false));
      transferSyntaxes.add(new DicomTransferSyntax("Big Endian Explicit", "1.2.840.10008.1.2.2", false, true, false));
      transferSyntaxes.add(new DicomTransferSyntax("Little Endian Implicit", "1.2.840.10008.1.2", false, false, false));
      // CT
      transferSyntaxes.add(new DicomTransferSyntax("CT Image Storage", "1.2.840.10008.5.2", true, true, true));
      // JPEG lossy
      transferSyntaxes.add(new DicomTransferSyntax("CT Image Storage", "1.2.840.10008.1.2.4.91", true, true, true));
      
   }

   HashMap<Integer,DicomElement> headerMap;

   public DicomHeader () {
      headerMap = new HashMap<Integer,DicomElement>();
   }

   public void addInfo(int key, DicomElement info) {
      headerMap.put(key, info);
   }

   public DicomElement removeInfo(int key) {
      return headerMap.remove(key);
   }

   public DicomElement getElement(int key) {
      return headerMap.get(key);
   }

   private static class DicomElementComparator implements Comparator<DicomElement> {

      static DicomElementComparator _instance;
      
      static DicomElementComparator instance() {
         if (_instance == null) {
            _instance = new DicomElementComparator();
         }
         return _instance;
      }
      
      @Override
      public int compare(DicomElement o1, DicomElement o2) {
         long id1 = o1.tagId & 0xFFFFFFFFl;
         long id2 = o2.tagId & 0xFFFFFFFFl;
         
         if (id1 < id2) {
            return -1;
         } else if (id1 > id2) {
            return 1;
         }
         return 0;
      }
   }
   
   
   @Override
   public String toString() {
      StringBuilder out = new StringBuilder();
      Collection<DicomElement> elems = headerMap.values();
      DicomElement[] arry = elems.toArray(new DicomElement[elems.size()]);
      Arrays.sort(arry, DicomElementComparator.instance());
      
      for (DicomElement value : arry) {
         out.append(value.toString());
         out.append('\n');
      }
      return out.toString();
   }

   public DicomTransferSyntax getTransferSyntax() {
      String uid = (String)(headerMap.get(DicomTag.TRANSFER_SYNTAX_UID).value);
      return getTransferSyntax(uid);
   }

   public String getStringValue(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }

      if (elem.value instanceof String) {
         return (String)(elem.value);
      } 
      return elem.value.toString();

   }

   public int getIntValue(int tagId, int defaultValue) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return defaultValue;
      }

      switch (elem.vr) {
         case IS: {
            String is = (String)elem.value;
            return DicomElement.parseIntString(is);
         }
         case SL: 
         case UL: {
            int[] vals = (int[])elem.value;
            return vals[0];
         }
         case SS:
         {
            short[] vals = (short[])elem.value;
            return vals[0];
         }
         case US: {
            short[] vals = (short[])elem.value;
            return vals[0] & 0xFFFF;
         }
         default:
            return defaultValue;
      }
   }

   public double getDecimalValue(int tagId, double defaultValue) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return defaultValue;
      }

      switch (elem.vr) {
         case DS: {
            String ds = (String)elem.value;
            return DicomElement.parseDecimalString(ds);
         }
         case FL:
         {
            float[] f =  (float[])elem.value;
            return f[0];
         }
         case FD:
         {
            double[] d =  (double[])elem.value;
            return d[0];
         }
         default:
            return defaultValue;
      }
   }

   public VectorNd getVectorValue(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }

      switch (elem.vr) {
         case DS: 
         case IS: {
            String ds = (String)elem.value;
            String[] svals = ds.split("\\\\");
            VectorNd out = new VectorNd(svals.length);
            for (int i=0; i<svals.length; i++) {
               out.set(i, Double.parseDouble(svals[i]));
            }
            return out;
         }
         case SL: {
            int[] vals = (int[])elem.value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         case UL: {
            int[] vals = (int[])elem.value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i] & 0xFFFFFFFFl);
            }
            return out;
         }
         case SS:
         {
            short[] vals = (short[])elem.value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         case US: {
            short[] vals = (short[])elem.value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i] & 0xFFFF);
            }
            return out;
         }
         case FL:
         {
            float[] vals =  (float[])elem.value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         case FD:
         {
            double[] vals =  (double[])elem.value;
            VectorNd out = new VectorNd(vals.length);
            for (int i=0; i<vals.length; i++) {
               out.set(i, vals[i]);
            }
            return out;
         }
         default:
            break;
      }
      
      return null;
   }
   
   public String[] getMultiStringValue(int tagId) {
      DicomElement elem = headerMap.get(tagId);
      if (elem == null) {
         return null;
      }
      
      switch(elem.vr) {
         case AE:
            break;
         case AS:
            break;
         case CS:
            break;
         case DA:
            break;
         case DT:
            break;
         case LT:
         case ST:
         case UT:
            return new String[]{(String)elem.value};
         case LO: 
         case PN:
         case SH: {
            String str = ((String)elem.value).trim();
            return str.split("\\\\");
         }
         case TM:
            break;
         case UI:
            break;
         default:
            break;
         
      }
    
      return null;
   }
   
   public int[] getMultiIntValue(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }

      switch (elem.vr) {
         case IS: 
         case DS: {
            String is = (String)elem.value;
            String[] svals = is.split("\\\\");
            int[] ivals = new int[svals.length];
            for (int i=0; i<svals.length; i++) {
               ivals[i] = Integer.parseInt(svals[i]);
            }
            return ivals;
         }
         case SL: 
         case UL: {
            return (int[])elem.value;
         }
         case SS:
         {
            short[] vals = (short[])elem.value;
            int[] ivals = new int[vals.length];
            for (int i=0; i<vals.length; i++) {
               ivals[i] = vals[i];
            }
            return ivals;
         }
         case US: {
            short[] vals = (short[])elem.value;
            int[] ivals = new int[vals.length];
            for (int i=0; i<vals.length; i++) {
               ivals[i] = vals[i] & 0xFFFF;
            }
            return ivals;
         }
         default:
            break;
      }
      
      return null;
   }
   
   public double[] getMultiDecimalValue(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }

      switch (elem.vr) {
         case DS: {
            String ds = (String)elem.value;
            String[] svals = ds.split("\\\\");
            double[] dvals = new double[svals.length];
            for (int i=0; i<svals.length; i++) {
               dvals[i] = Double.parseDouble(svals[i]);
            }
            return dvals;
         }
         case FL:
         {
            float[] f =  (float[])elem.value;
            double[] dvals = new double[f.length];
            for (int i=0; i<f.length; i++) {
               dvals[i] = f[i];
            }
            return dvals;
         }
         case FD:
         {
            return (double[])elem.value;
         }
         default:
      }
      
      return null;
   }
   
   public DicomDateTime getDateTime(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }
      
      switch(elem.vr) {
         case DT: {
            // YYYYMMDDHHMMSS.FFFFFF&ZZZZ
            String dtStr = (String)(elem.value);
            
            // optional offset
            int offsetMinutes = 0;
            int idSign = dtStr.indexOf('+');
            if (idSign < 0) {
               idSign = dtStr.indexOf('-');
            }
            if (idSign >= 0) {
               String strOffset = dtStr.substring(idSign);
               dtStr = dtStr.substring(0, idSign);
               
               boolean minus = (dtStr.charAt(0) == '-');
               strOffset = strOffset.substring(1);
               if (strOffset.length() == 2) {
                  offsetMinutes = 60*Integer.parseInt(strOffset);
               } else if (strOffset.length() == 4) {
                  offsetMinutes = 60*Integer.parseInt(strOffset.substring(0, 2))
                     + Integer.parseInt(strOffset.substring(2,  4));
               } else {
                  throw new InternalErrorException(
                     "Date offset does not adhere to proper format. (" + strOffset + ")");
               }
               if (minus) {
                  offsetMinutes = -offsetMinutes;
               }
            }
            
            // parse actual Date/Time
            int micros = 0;
            int idPeriod = dtStr.indexOf('.');
            if (idPeriod >= 0) {
               String strDecimal = dtStr.substring(idPeriod);
               dtStr = dtStr.substring(0, idPeriod);
               micros = Math.round(Float.parseFloat(strDecimal)*1000000);
            }
            
            // YYYYMMDDHHMMSS
            int year = 1970;
            int month = 1;
            int date = 1;
            int hour = 0;
            int min = 0;
            int sec = 0;
            String substr;
            switch (dtStr.length()) {
               case 14: {
                  substr = dtStr.substring(12);
                  sec = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 12);
               }
               case 12: {
                  substr = dtStr.substring(10);
                  min = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 10);
               }
               case 10: {
                  substr = dtStr.substring(8);
                  hour = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 8);
               }
               case 8: {
                  substr = dtStr.substring(6);
                  date = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 6);
               }
               case 6: {
                  substr = dtStr.substring(4);
                  month = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 4);
               }
               case 4: {
                  year = Integer.parseInt(dtStr);
                  break;
               } 
               default: {
                  throw new InternalErrorException(
                     "Date/Time string in invalid format (" + dtStr + ")");
               }
                  
            }
            
            DicomDateTime dt = new DicomDateTime(year, month, date, hour, min, sec, micros); 
            if (offsetMinutes != 0) {
               dt.addTimeMinutes(offsetMinutes);
            }
            return dt;
         }
         case DA: {
            // YYYYMMDD or YYYY.MM.DD
            String dtStr = (String)(elem.value);
            // remove periods
            dtStr = dtStr.replace(".", "");
         
            int year = 1970;
            int month = 1;
            int date = 1;
            String substr;
            switch (dtStr.length()) {
               case 8: {
                  substr = dtStr.substring(6);
                  date = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 6);
               }
               case 6: {
                  substr = dtStr.substring(4);
                  month = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 4);
               }
               case 4: {
                  year = Integer.parseInt(dtStr);
                  break;
               } 
               default: {
                  throw new InternalErrorException(
                     "Date/Time string in invalid format (" + dtStr + ")");
               }
                  
            }
            
            DicomDateTime dt = new DicomDateTime(year, month, date, 0, 0, 0, 0); 
            return dt;
         }
         case TM: {
            // HHMMSS.FFFFFF or HH:MM:SS.FFFFFF
            String dtStr = (String)(elem.value);
            // remove colons
            dtStr = dtStr.replace(":", "");
            
            // fraction
            int micros = 0;
            int idPeriod = dtStr.indexOf('.');
            if (idPeriod >= 0) {
               String strDecimal = dtStr.substring(idPeriod);
               dtStr = dtStr.substring(0, idPeriod);
               micros = Math.round(Float.parseFloat(strDecimal)*1000000);
            }
            
            // HHMMSS
            int hour = 0;
            int min = 0;
            int sec = 0;
            String substr;
            switch (dtStr.length()) {
               case 6: {
                  substr = dtStr.substring(4);
                  sec = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 4);
               }
               case 4: {
                  substr = dtStr.substring(2);
                  min = Integer.parseInt(substr);
                  dtStr = dtStr.substring(0, 2);
               }
               case 2: {
                  hour = Integer.parseInt(dtStr);
                  break;
               }
               default: {
                  throw new InternalErrorException(
                     "Date/Time string in invalid format (" + dtStr + ")");
               }
                  
            }
            
            DicomDateTime dt = new DicomDateTime(1970, 1, 1, hour, min, sec, micros); 
            return dt;
         }
         default:
            return null;
      }
      
   }

   public static void addTransferSyntax(DicomTransferSyntax syntax) {
      transferSyntaxes.add(syntax);
   }

   public static DicomTransferSyntax getTransferSyntax(String uid) {
      for (DicomTransferSyntax ts : transferSyntaxes) {
         if (uid.equals(ts.uid)) {
            return ts;
         }
      }
      return null;
   }


}
