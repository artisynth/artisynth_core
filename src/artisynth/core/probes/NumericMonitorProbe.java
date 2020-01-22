/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.ScanToken;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * An output probe that generates data with an application-supplied
 * generating function. Applications can supply this function
 * by either
 * <ul>
 * <li>Supplying a <code>DataFunction</code> to generate the data;
 * <li>Overriding the {@link #generateData} method;
 * <li>Overriding the {@link #apply} method.
 * </ul>
 */
public class NumericMonitorProbe extends NumericDataFunctionProbe {

   private boolean myShowTime;
   private static boolean defaultShowTime = true;

   private boolean myShowHeader;
   private static boolean defaultShowHeader = true;

   public static PropertyList myProps =
      new PropertyList (NumericMonitorProbe.class, NumericProbeBase.class);

   static {
      myProps.add (
         "showTime * *", "show time explicitly in output file",
         defaultShowTime);

      myProps.add (
         "showHeader * *", "show header explicitly in output file",
         defaultShowHeader);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myShowTime = defaultShowTime;
      myShowHeader = defaultShowHeader;
   }

   public boolean getShowTime() {
      return myShowTime;
   }

   public void setShowTime (boolean enable) {
      myShowTime = enable;
   }

   public boolean getShowHeader() {
      return myShowHeader;
   }

   public void setShowHeader (boolean enable) {
      myShowHeader = enable;
   }

   public NumericMonitorProbe() {
      setDefaultValues();
      myPlotTraceManager = new PlotTraceManager ("output");
   }

   public NumericMonitorProbe (int vsize, double interval) {
      this (vsize, null, 0, 1, interval);
   }

   public NumericMonitorProbe (
      int vsize, String fileName,
      double startTime, double stopTime, double interval) {

      this();
      setVsize (vsize);
      setAttachedFileName (fileName);
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
   }

   /**
    * Writes the start and stop times, scale value, and data for this probe to a
    * PrintWriter, using the format described for {@link
    * artisynth.core.probes.NumericInputProbe#read(File,boolean)
    * NumericInputProbe.read(File)}. The format used for producing floating
    * point numbers can be controlled using a printf-style format string,
    * details of which are described in {@link maspack.util.NumberFormat
    * NumberFormat}.
    * 
    * @param pw
    * writer which accepts the output
    * @param fmtStr
    * printf-style format string (if set to null then "%g" will be assumed,
    * which will produce full precision output).
    * @param showTime
    * if true, then time values are written explicitly. Otherwise, an implicit
    * step size corresponding to the value returned by {@link #getUpdateInterval
    * getUpdateInterval} will be specified.
    * @throws IOException
    * if an I/O error occurs.
    */
   public void write (PrintWriter pw, String fmtStr, boolean showTime)
      throws IOException {
      pw.println (getStartTime() + " " + getStopTime() + " " + myScale);
      pw.print (myInterpolation.getOrder()+" "+myNumericList.getVectorSize());
      if (showTime) {
         pw.println (" explicit");
      }
      else {
         pw.println (" " + getUpdateInterval());
      }
      writeData (pw, fmtStr, showTime);
   }

   public void setAttachedFileName (String fileName, String fmtStr) {
      setAttachedFileName (fileName);
      setFormat (fmtStr);
   }

   public void setAttachedFileName (
      String fileName, String fmtStr, boolean showTime, boolean showHeader) {
      setAttachedFileName (fileName);
      setFormat (fmtStr);
      setShowTime (showTime);
      setShowHeader (showHeader);
   }

   /**
    * When called (perhaps by the Artsynth timeline), causes information about
    * this probe to be written to its attached file.
    * 
    * @see #write
    */
   public void save() throws IOException {
      File file = getAttachedFile();
      if (file != null && !file.isDirectory ()) {
         if (isAttachedFileRelative()) {
            file.getParentFile().mkdirs();
         }
         PrintWriter pw =
            new PrintWriter (new BufferedWriter (new FileWriter (file)));
         try {
            if (myShowHeader) {
               write (pw, myFormatStr, myShowTime);
            }
            else {
               writeData (pw, myFormatStr, myShowTime);
            }
         }
         catch (IOException e) {
            throw e;
         }
         finally {
            pw.close();
         }
      }
   }

   public void writeData (PrintWriter pw, String fmtStr, boolean showTime) {
      NumberFormat timeFmt = null;
      if (showTime) {
         if (getUpdateInterval() < 1e-5) {
            timeFmt = new NumberFormat ("%12.9f");
         }
         else {
            timeFmt = new NumberFormat ("%9.6f");
         }
      }
      NumberFormat fmt = new NumberFormat (fmtStr);
      Iterator<NumericListKnot> it = myNumericList.iterator();
      while (it.hasNext()) {
         NumericListKnot knot = it.next();
         if (showTime) {
            pw.print (timeFmt.format (knot.t) + " ");
         }
         pw.println (knot.v.toString (fmt));
      }
   }

   /**
    * Generates data for this probe by evaluating a vectored-valued function of
    * time and storing the result in <code>vec</code>. The size of
    * <code>vec</code> will equal the vector size of the probe (as returned by
    * {@link #getVsize()}. The function may generate the data using either
    * absolute time <code>t</code> or relative time <code>trel</code>, where
    * relative time is determined from the probe's start time and scale factor
    * using <pre> trel = (t - startTime)/scale </pre>
    *
    * @param vec returns the generated data.
    * @param t absolute time (seconds)
    * @param trel probe relative time
    */
   public void generateData (VectorNd vec, double t, double trel) {
      DataFunction func = getDataFunction();
      if (func != null) {
         func.eval (vec, t, trel);
      }
      else {
         vec.setZero();
      }
   }

   protected Object[] getPropsOrDimens () {
      Object[] propsOrDimens = new Object[1];
      propsOrDimens[0] = myVsize;
      return propsOrDimens;
   }

   public void apply (double t) {
      // XXX don't we want to apply scaling here too?
      double trel = (t-getStartTime())/myScale;

      NumericListKnot knot = new NumericListKnot (myVsize);
      generateData (knot.v, t, trel);
      knot.t = trel;
      myNumericList.add (knot);
      myNumericList.clearAfter (knot);
   }

   public NumericList getOutput() {
      return myNumericList;
   }

   private PlotTraceInfo[] tmpTraceInfos = null;

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "vsize")) {
         int vsize = rtok.scanInteger();
         setVsize (vsize);
         return true;
      }
      else if (scanAttributeName (rtok, "plotTraceInfo")) {
         tmpTraceInfos = scanPlotTraceInfo (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      tmpTraceInfos = null;
      super.scan (rtok, ref);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      
      pw.println ("vsize=" + myVsize);
      maybeWritePlotTraceInfo (pw);
   }

   public void setVsize (int vsize) {
      setVsize (vsize, null);
   }

   public void setVsize (int vsize, PlotTraceInfo[] traceInfos) {

      myVsize = vsize;
      myNumericList = new NumericList (myVsize);

      if (traceInfos != null) {
         myPlotTraceManager.rebuild (getPropsOrDimens(), traceInfos);
      }
      else {
         myPlotTraceManager.rebuild (getPropsOrDimens());
      }
      if (myLegend != null) {
         myLegend.rebuild();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      NumericMonitorProbe probe;
      
      try {
         probe = (NumericMonitorProbe)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone MonitorOutputProbe");
      }

      double duration = getStopTime()-getStartTime();
      probe.setStartTime (probe.getStartTime()+duration);
      probe.setStopTime (probe.getStopTime()+duration);
      return probe;
   }

}
