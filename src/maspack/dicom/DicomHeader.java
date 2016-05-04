/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import maspack.matrix.VectorNd;

/**
 * Stores all header DICOM information
 * @author Antonio
 *
 */
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

   /**
    * Add DICOM header entry
    * @param tagId integer tag identifying header element
    * @param info header information to add
    */
   public void addInfo(int tagId, DicomElement info) {
      headerMap.put(tagId, info);
   }

   /**
    * Remove DICOM header information
    * @param tagId integer tag identifying header element
    * @return corresponding header information if exists and removed, null otherwise
    */
   public DicomElement removeInfo(int tagId) {
      return headerMap.remove(tagId);
   }

   /**
    * Query DICOM header information
    * @param tagId integer tag identifying header element
    * @return corresponding header information if exists, null otherwise
    */
   public DicomElement getElement(int tagId) {
      return headerMap.get(tagId);
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

   /**
    * @return the transfer syntax, which identifies how the content and image is stored
    */
   public DicomTransferSyntax getTransferSyntax() {
      String uid = (String)(headerMap.get(DicomTag.TRANSFER_SYNTAX_UID).value);
      return getTransferSyntax(uid);
   }

   /**
    * Determines the string representation of a given header element
    * @param tagId integer tag identifier for the header element
    * @return string representation
    */
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

   /**
    * Determines the integer value of a given header element (if represents valid integer)
    * @param tagId DICOM tag identifier
    * @param defaultValue default integer value to return if the tag is not present in the header
    * @return the value of the header element if exists and is valid, 
    * otherwise the supplied default value is returned
    */
   public int getIntValue(int tagId, int defaultValue) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return defaultValue;
      }

      switch (elem.vr) {
         case DS:
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

   /**
    * Determines the decimal value of a given header element (if represents valid decimal number)
    * @param tagId DICOM tag identifier
    * @param defaultValue default decimal value to return if the tag is not present in the header
    * @return the value of the header element if exists and is valid, 
    * otherwise the supplied default value is returned
    */
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

   /**
    * Determines the vector value of a given header element (if represents valid vector)
    * @param tagId DICOM tag identifier
    * @return the value of the header element if exists and is valid, null otherwise
    */
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
   
   /**
    * Determines the string values of a given header element
    * @param tagId DICOM tag identifier
    * @return the value of the header element if exists and is valid
    */
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
   
   /**
    * Determines the integer array value of a given header element (if represents valid integer array)
    * @param tagId DICOM tag identifier
    * @return the value of the header element if exists and is valid, null otherwise
    */
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
   
   /**
    * Determines the decimal array value of a given header element (if represents valid decimal array)
    * @param tagId DICOM tag identifier
    * @return the value of the header element if exists and is valid, null otherwise
    */
   public double[] getMultiDecimalValue(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }

      switch (elem.vr) {
         case DS: {
            return DicomElement.parseMultiDecimalValue((String)elem.value);
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
   
   /**
    * Determines the data/time value of a given header element (if represents valid date/time)
    * @param tagId DICOM tag identifier
    * @return the value of the header element if exists and is valid, null otherwise
    */
   public DicomDateTime getDateTime(int tagId) {
      DicomElement elem = headerMap.get(tagId); 
      if (elem == null) {
         return null;
      }
      
      switch(elem.vr) {
         case DT: {
            return DicomElement.parseDateTime((String)elem.value);
         }
         case DA: {
            return DicomElement.parseDate((String)elem.value);
         }
         case TM: {
            return DicomElement.parseTime((String)elem.value);
         }
         default:
            return null;
      }
      
   }

   /**
    * Adds a possible transfer syntax, previously unknown, allowing for decoding of
    * new DICOM information
    * @param syntax transfer syntax to add, containing necessary decoding information
    */
   public static void addTransferSyntax(DicomTransferSyntax syntax) {
      transferSyntaxes.add(syntax);
   }

   /**
    * Determines the appropriate transfer syntax based on the syntax's uid
    * @param uid the identifying uid for the transfer syntax
    * @return the transfer syntax representation, null if not found
    */
   public static DicomTransferSyntax getTransferSyntax(String uid) {
      for (DicomTransferSyntax ts : transferSyntaxes) {
         if (uid.equals(ts.uid)) {
            return ts;
         }
      }
      return null;
   }


}
