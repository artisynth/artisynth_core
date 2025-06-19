package artisynth.core.probes;

import java.io.*;
import java.util.*;
import artisynth.core.mechmodels.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.interpolation.*;
import maspack.matrix.*;

/**
 * Writes a TRC (Track-Row-Column) marker data file. This file format is
 * defined by the Motion Analysis Corporation and described in the appendix of
 * the manual for their EVaRT software.
 *
 * <p>At present, the source data for writing the file must be a numeric probe
 * that contains position data for a set of points.
 */
public class TRCWriter {

   File myFile;
   DynamicIntArray myOffsets;
   LinkedList<String> myPointNames;
   String myUnitsStr = "mm";
   NumberFormat myFmt = null;

   /**
    * Creates a TRCWriter for the indicated file.
    *
    * @param file TRC file to be written
    */
   public TRCWriter (File file) {
      myFile = file;
   }

   /**
    * Creates a TRCWriter for the indicated file path.
    *
    * @param filePath path name of the TRC file to be written
    */
   public TRCWriter (String filePath) {
      this (new File(filePath));
   }

   /**
    * Queries the string representing units in this writer.
    *
    * @return string representing units
    */
   public String getUnitsString() {
      return myUnitsStr;
   }

   /**
    * Sets the string representing units in this writer. The default is {@code
    * "mm"}.
    *
    * @param str string representing units
    */
   public void setUnitsString (String str) {
      myUnitsStr = str;
   }

   /**
    * Queries the explict format for floating point data written by this
    * writer.  If no explicit format has been set (by {@link #setNumberFormat})
    * {@code null} is returned.
    *
    * @return floating point format
    */
   public String getNumberFormat() {
      return myFmt.toString();
   }

   /**
    * Sets an explicit format for floating point data written by this writer.
    * {@code str} should be a number string used for specifying {@link
    * NumberFormat}. If {@code str} is {@code null}, the explicit format is
    * removed and the format is inferred from the data.
    *
    * @param str string describing the number format
    */
   public void setNumberFormat (String str) {
      if (str == null) {
         myFmt = null;
      }
      else {
         myFmt = new NumberFormat (str);
      }
   }

   /**
    * Writes the file based on the contents of a position input probe.
    *
    * @param probe
    */
   public void writeData (NumericProbeBase probe) throws IOException {
      extractPointData (probe);
      PrintWriter pw = null;
      try {
         pw = new PrintWriter (new BufferedWriter (new FileWriter(myFile)));
         writeHeaders (pw, probe);
         writeData (pw, probe);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (pw != null) {
            pw.close();
         }
      }
   }

   private void extractPointData (NumericProbeBase probe) {
      myPointNames = new LinkedList<>();
      myOffsets = new DynamicIntArray();
      int offset = 0;
      boolean hasOtherData = false;
      boolean hasUnnamedPoints = false;
      for (Object obj : probe.getPropsOrDimens()) {
         Point point = null;
         int dimen = 0;
         if (obj instanceof Property) {
            Property prop = (Property)obj;
            HasProperties host = prop.getHost();
            dimen = prop.getInfo().getDimension();
            if (host instanceof Point && dimen == 3) {
               point = (Point)host;
            }
         }
         else if (obj instanceof Integer) {
            dimen = (Integer)obj;
         }
         else {
            throw new InternalErrorException (
               "probe.getPropsOrDimens() returns object that is neither " +
               " a property nor an integer");
         }
         if (point == null) {
            hasOtherData = true;
         }
         else {
            String name = point.getName();
            if (name == null) {
               name = "MKR"+(myPointNames.size()+1);
               hasUnnamedPoints = true;
            }
            myPointNames.add (name);
            myOffsets.add (offset);
         }
         offset += dimen;
      }
      if (hasOtherData) {
         System.out.println (
            "WARNING, TrcWriter: probe has non-point data; ignoring");
      }
      if (hasUnnamedPoints) {
         System.out.println (
            "WARNING, TrcWriter: some points are unnamed; "+
            "names will be assigned by number");
      }
   }

   /**
    * Set a default format based on the magnitude of the data.
    */
   private NumberFormat getDefaultFormat (NumericProbeBase probe) {
      NumericList nlist = probe.getNumericList();
      double max = 0;
      for (int k=0; k<nlist.getNumKnots(); k++) {
         NumericListKnot knot = nlist.getKnot(k);
         double mval = knot.v.infinityNorm();
         if (max < mval) {
            max = mval;
         }
      }
      int nleading = (int)Math.ceil(Math.log10(max));
      // try to assume 8 digits overall
      int nfrac = Math.max (2, 8-nleading);
      int ntotal = nleading+2+nfrac;
      return new NumberFormat ("%"+ntotal+"."+nfrac+"f");
   }
   
   private void writeHeaders (PrintWriter pw, NumericProbeBase probe) {
      NumericList nlist = probe.getNumericList();

      // find the number of frames and data rate from the numeric list:
      int numFrames = nlist.getNumKnots();
      int numMarkers = myPointNames.size();
      int dataRate = 60;
      if (numFrames > 1) {
         double time = nlist.getKnot(numFrames-1).t - nlist.getKnot(0).t;
         dataRate = (int)Math.round(numFrames/time);
      }
      pw.printf (
         "PathFileType\t4\t(X/Y/Z)\t%s\n", myFile.toString());
      pw.printf (
         "DataRate\tCameraRate\tNumFrames\tNumMarkers\tUnits\tOrigDataRate\t" +
         "OrigDataStartFrame\tOrigNumFrames\n");
      pw.printf (
         "%d\t%d\t%d\t%d\t%s\t%d\t%d\t%d\n",
         dataRate, dataRate, numFrames, numMarkers, myUnitsStr,
         dataRate, 1, numFrames);
      pw.printf ("Frame#\tTime");
      for (String name : myPointNames) {
         pw.printf ("\t%s\t\t", name);
      }
      pw.printf ("\n");
      pw.printf ("\t");
      for (int i=1; i<=numMarkers; i++) {
         pw.printf ("\tX%d\tY%d\tZ%d", i, i, i);
      }
      pw.printf ("\n");
      pw.printf ("\n");
   }

   private void writeData (PrintWriter pw, NumericProbeBase probe) {
      NumericList nlist = probe.getNumericList();

      NumberFormat fmt = myFmt;
      if (fmt == null) {
         fmt = getDefaultFormat (probe);
      }
      // extract and write the data from the numeric list
      int numMarkers = myPointNames.size();
      for (int k=0; k<nlist.getNumKnots(); k++) {
         NumericListKnot knot = nlist.getKnot(k);
         pw.printf ("%d\t%s", k+1, fmt.format(knot.t));
         Vector3d vec = new Vector3d();
         for (int i=0; i<numMarkers; i++) {
            knot.v.getSubVector (myOffsets.get(i), vec);
            pw.printf (
               "\t%s\t%s\t%s",
               fmt.format(vec.x), fmt.format(vec.y), fmt.format(vec.z));
         }
         pw.printf ("\n");
      }
   }

   /**
    * Writes a TRC file based on the contents of a given numeric probe, which
    * is assumed to contain point position data. The marker count, names, and
    * data rate is inferred from the probe.
    *
    * @param file TRC file to be created
    * @param probe numeric probe containing the data
    */
   public static void write (File file, NumericProbeBase probe)
      throws IOException {
      TRCWriter writer = new TRCWriter(file);
      writer.writeData (probe);
   }

   /**
    * Writes a TRC file based on the contents of a given numeric probe, which
    * is assumed to contain point position data. The marker count, names, and
    * data rate is inferred from the probe.
    *
    * @param filepath path name of the TRC file to be created
    * @param probe numeric probe containing the data
    */
   public static void write (String filepath, NumericProbeBase probe)
      throws IOException {
      TRCWriter writer = new TRCWriter(filepath);
      writer.writeData (probe);
   }
}
     


