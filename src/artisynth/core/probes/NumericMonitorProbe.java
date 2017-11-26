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
import java.util.*;

import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.ImproperStateException;
import maspack.matrix.VectorNd;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.HasAuxState;

import artisynth.core.util.*;

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
public class NumericMonitorProbe extends NumericProbeBase 
   implements CopyableComponent {
   private boolean myShowTime;
   private static boolean defaultShowTime = true;

   private boolean myShowHeader;
   private static boolean defaultShowHeader = true;
   private boolean myHasStateP = false;

   protected ArrayList<DataFunction> myDataFunctions =
      new ArrayList<DataFunction>();

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

   /**
    * Sets a data function to be used by this probe's {@link #generateData} 
    * method to generate data for this probe. If the data function is set to
    * <code>null</code>, then this probe's data is simply set to zero. If the
    * {@link #generateData} method is overridden by a subclass, then the data
    * generation is determined instead by the overriding method.
    *
    * @see #getDataFunction
    */
   public void setDataFunction (DataFunction func) {
      if (myDataFunctions.size() == 0) {
         if (func != null) {
            myDataFunctions.add (func);
         }
      }
      else {
         if (func != null) {
            myDataFunctions.set (0, func);
         }
         else {
            myDataFunctions.clear();
         }
      }
      myHasStateP = (func != null && func instanceof HasAuxState);
   }

   /**
    * Returns the data function, if any, that is used by this probe's {@link
    * #apply(double)} method to generate data for this probe.
    *
    * @see #setDataFunction
    */
   public DataFunction getDataFunction() {
      return myDataFunctions.size() > 0 ? myDataFunctions.get(0) : null;
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
         try {
            if (isAttachedFileRelative()) {
               file.getParentFile().mkdirs();
            }
            PrintWriter pw =
               new PrintWriter (new BufferedWriter (new FileWriter (file)));
            System.out.println ("saving output probe to " + file.getName());
            if (myShowHeader) {
               write (pw, myFormatStr, myShowTime);
            }
            else {
               writeData (pw, myFormatStr, myShowTime);
            }
            pw.close();
         }
         catch (Exception e) {
            System.out.println ("Error writing file " + file.getName());
            e.printStackTrace();
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

   public Object clone() throws CloneNotSupportedException {
      NumericMonitorProbe probe = (NumericMonitorProbe)super.clone();
      probe.myDataFunctions = copyDataFunctions (myDataFunctions);
      //probe.myNumericList.clear();
      return probe;
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

   public boolean hasState() {
      return myHasStateP;
   }

   public ComponentState createState (ComponentState prevState) {
      if (myHasStateP){
         return new NumericState();
      }
      else {
         return new EmptyState();
      }
   }

   public void getState (ComponentState state) {
      if (myHasStateP) {
         DataBuffer data = (NumericState)state;
         for (DataFunction func : myDataFunctions) {
            if (func instanceof HasAuxState) {
               ((HasAuxState)func).getAuxState (data);
            }
         }
      }
   }

   public void setState (ComponentState state) {
      if (myHasStateP) {
         DataBuffer data = (NumericState)state;
         for (DataFunction func : myDataFunctions) {
            if (func instanceof HasAuxState) {
               ((HasAuxState)func).setAuxState (data);
            }
         }
      }
   }

   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {
      if (myHasStateP) {
         DataBuffer newData = (NumericState)newstate;
         DataBuffer oldData = (oldstate != null ? (NumericState)oldstate : null);
         for (DataFunction func : myDataFunctions) {
            if (func instanceof HasAuxState) {
               HasAuxState sfunc = (HasAuxState)func;
               if (oldData != null) {
                  sfunc.skipAuxState (oldData);
               }
               sfunc.getInitialAuxState (newData, oldData);
            }
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return dataFunctionsAreCopyable (myDataFunctions);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCloneable() {
      return isDuplicatable();
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
