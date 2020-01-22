/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.io.*;
import java.util.*;

import maspack.interpolation.Interpolation;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.interpolation.Interpolation.Order;
import maspack.matrix.VectorNd;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class NumericControlProbe extends NumericDataFunctionProbe {

   public static final double EXPLICIT_TIME = -1;

   // private double[][] myPropValues;

   private VectorNd myTmpVec;

   protected static boolean defaultExtendData = true;

   public static PropertyList myProps =
      new PropertyList (NumericControlProbe.class, NumericProbeBase.class);

   static {
      myProps.add (
         "extendData * *", "extend data past last knot point",
         defaultExtendData);
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myInterpolation =
         new Interpolation (defaultInterpolationOrder, defaultExtendData);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public NumericControlProbe() {
      super();
      setScalable (true);
      myPlotTraceManager = new PlotTraceManager ("input"); // ???
   }

   public NumericControlProbe (String fileName) throws IOException {
      this();
      if (fileName == null) {
         throw new IllegalArgumentException ("null fileName");
      }
      setAttachedFileName (fileName);
      load(/*setTimes=*/true);
   }

   public NumericControlProbe (
      int vsize, double[] data, double timeStep,
      double startTime, double stopTime) {
      this();
      setVsize (vsize);
      setStartTime (startTime);
      setStopTime (stopTime);
      setData (getStartTime());
      setData (getStopTime());
      addData (data, timeStep);
   }

   /**
    * Enables extension of data past the last knot point.
    * 
    * @param extend
    * true if data is to be extended past the last knot point
    */
   public void setExtendData (boolean extend) {
      myInterpolation.setDataExtended (extend);
      if (myNumericList != null) {
         myNumericList.setInterpolation (myInterpolation);
      }
   }

   /**
    * Returns true if data is to be extended past the last knot point
    * 
    * @return true if data is extended
    */
   public boolean getExtendData() {
      return myInterpolation.isDataExtended();
   }
         
   /**
    * Reads the start and stop times, scale value, and data for this probe from
    * an ascii file. The following information should be provided in order,
    * separated by white space:
    * <ul>
    * <li>start time (either seconds or nanoseconds, as described below).
    * Ignored if <code>setTimes</code> is false. 
    * <li>stop time (either seconds or nanoseconds, as described below)
    * Ignored if <code>setTimes</code> is false. 
    * <li>scale value (floating point number; nominal value is 1)
    * Ignored if <code>setTimes</code> is false. 
    * <li>interpolation order ("step", "linear", or "cubic")
    * <li>number of data values n at each knot point (an integer)
    * <li>either the knot point time step (floating point number, in seconds),
    * OR the keyword "explicit", indicating that knot time values are to be
    * given explicitly
    * <li>the knot point data, with each knot point specified by n numbers,
    * preceeded (if explicit time has been specified) by the corresponding time
    * value (in seconds).
    * </ul>
    * The start and stop times can be indicated in either seconds or 
    * nanoseconds. The former is assumed if the value is a double with a
    * decimal point. 
    * 
    * For example, the following input
    * 
    * <pre>
    * 2.0 10.0 1.2
    * linear 2 explicit
    * 0.0 2.0 2.0
    * 1.1 4.0 3.0
    * 3.0 0.0 1.0
    * </pre>
    * 
    * specifies a probe with a start and stop time of 2 and 10 seconds,
    * respectively, a scale value of 1.2, linear interpolation, 2 values at each
    * knot point, and three knot points at times 0.0, 1.1, and 3.0.
    * 
    * If knot time is given implicitly by a time setp, then time is assumed to
    * start at 0. The following input
    * 
    * <pre>
    * 2000000000 3000000000 2.5
    * step 2 2.0
    * 2.0 2.0
    * 4.0 3.0
    * 0.0 1.0
    * </pre>
    * 
    * specifies a probe with a start and stop time of 2 and 3 seconds,
    * respectively, a scale value of 2.5, step interpolation, 2 values at each
    * knot point, and three knot points with times of 0, 2.0, and 4.0 (given
    * implicity by a step size of 2.0).
    * 
    * <p>
    * The character '#' is a comment character, causing all subsequent input up
    * to the next new line to be ignored.
    * 
    * @param file
    * File from which to read the probe information
    * @param setTimes if <code>true</code>, sets the start time, stop time,
    * and scale values to those indicated at the head of the file. If 
    * <code>false</code>, these values are ignored.
    * @throws IOException
    * if an I/O or format error occurred.
    */
   public void read (File file, boolean setTimes) throws IOException {
      // myAttachedFile = null;
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new FileReader (file)));
      try {
         read (rtok, setTimes);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         rtok.close();
      }
   }
   
   protected void read (ReaderTokenizer rtok, boolean setTimes) 
      throws IOException {
      rtok.commentChar ('#');
      rtok.ordinaryChar ('/');
      double time = 0;
      time = scanTimeQuantity (rtok);
      if (setTimes) {
         setStartTime (time);
      }
      time = scanTimeQuantity (rtok);
      if (setTimes) {
         setStopTime (time);
      }
      if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
         throw new IOException ("expecting scale value, line " + rtok.lineno());
      }
      if (setTimes) {
         setScale (rtok.nval);
      }

      int numValues = 0;
      Order interpolationOrder;
      double timeStep;
      
      if (rtok.nextToken() != ReaderTokenizer.TT_WORD) {
         throw new IOException ("expecting interpolation method, line "
         + rtok.lineno());
      }
      interpolationOrder = Order.fromString (rtok.sval);
      if (interpolationOrder == null) {
         if (rtok.sval.equalsIgnoreCase ("linear")) {
            interpolationOrder = Order.Linear;
         }
         else if (rtok.sval.equalsIgnoreCase ("step")) {
            interpolationOrder = Order.Step;
         }
         else if (rtok.sval.equalsIgnoreCase ("cubic")) {
            interpolationOrder = Order.Cubic;
         }
         else {
            throw new IOException ("unknown interpolation order '" + rtok.sval
               + "', line " + rtok.lineno());
         }
      }
      if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER ||
          (numValues = (int)rtok.nval) != rtok.nval) {
         throw new IOException ("expecting number of values, line "
         + rtok.lineno());
      }
      if (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         timeStep = rtok.nval;
      }
      else if (rtok.ttype == ReaderTokenizer.TT_WORD &&
               rtok.sval.equals ("explicit")) {
         timeStep = EXPLICIT_TIME;
      }
      else {
         throw new IOException (
            "expecting either a time step or the keyword 'explicit', line "
            + rtok.lineno());
      }
      // myNumericList = new NumericList (numValues);
      myNumericList = new NumericList (myVsize);
      myInterpolation.setOrder (interpolationOrder);
      myNumericList.setInterpolation (myInterpolation);
      addData (rtok, timeStep);
   }

   /**
    * Writes the start and stop times, scale value, and data for this probe to a
    * PrintWriter, using the format described for {@link #read(File,boolean)
    * read(File)}. The format used for producing floating point numbers can be
    * controlled using a printf-style format string, details of which are
    * described in {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param pw
    * writer which accepts the output
    * @param fmtStr
    * printf-style format string (if set to null then "%g" will be assumed,
    * which will produce full precision output).
    * @throws IOException
    * if an I/O error occurs.
    */
   public void write (PrintWriter pw, String fmtStr) throws IOException {
      pw.println (getStartTime() + " " + getStopTime() + " " + myScale);
      pw.print (myInterpolation.getOrder()+" "+myNumericList.getVectorSize());
      pw.println (" explicit");
      if (fmtStr == null) {
         fmtStr = "%g";
      }
      NumberFormat fmt = new NumberFormat (fmtStr);
      Iterator<NumericListKnot> it = myNumericList.iterator();
      while (it.hasNext()) {
         NumericListKnot knot = it.next();
         pw.println (fmt.format (knot.t) + "  " + knot.v.toString (fmt));
      }
   }

   public void setAttachedFileName (String fileName) {
      super.setAttachedFileName (fileName);
   }

   public void setAttachedFileName (String fileName, String fmtStr) {
      setAttachedFileName (fileName);
      myFormatStr = fmtStr;
   }

   /**
    * When called (perhaps by the Artsynth timeline), causes information about
    * this probe to be written to the attached file.
    * 
    * @see #write
    */
   public void save() throws IOException {
      File file = getAttachedFile();
      if (file != null) {
         if (isAttachedFileRelative()) {
            file.getParentFile().mkdirs();
         }
         PrintWriter pw =
            new PrintWriter (new BufferedWriter (new FileWriter (file)));
         try {
            write (pw, myFormatStr);
         }
         catch (IOException e) {
            throw e;
         }
         finally {
            pw.close();
         }
      }
   }

   public void loadEmpty() {
      NumericListKnot knotStart = new NumericListKnot (myVsize);
      NumericListKnot knotEnd = new NumericListKnot (myVsize);

      knotStart.t = getStartTime();
      knotEnd.t = getStopTime();

      myNumericList.add (knotStart);
      myNumericList.add (knotEnd);
   }

   protected void load(boolean setTimes) throws IOException {
      File file = getAttachedFile();
      if (file != null) {
         if (!file.exists ()) {
            throw new IOException ("File '"+file+"' does not exist");
         }
         else if (!file.canRead ()) {
            throw new IOException ("File '"+file+"' is not readable");
         }         
         else {
            read (file,setTimes);
         }
      }     
   }
   
   /**
    * When called (perhaps by the Artsynth timeline), causes information about
    * this probe to be loaded from the attached file.
    */
   public void load() throws IOException {
      load(/*setTimes=*/false);
   }

   public void addData (File file, double timeStep) throws IOException {
      addData (
         new ReaderTokenizer (new BufferedReader (new FileReader (file))),
         timeStep);
   }

   /**
    * Adds one or more data samples to internal data list.<br>
    * <br>
    * Samples can be evenly distributed with given time step or time of each
    * sample can be explicitly set. In this case, timeStep must have value of
    * {@link #EXPLICIT_TIME} and before every data sample must be its temporal
    * information.<br>
    * 
    * Internal data sample size of this input probe can be determined by
    * {@link #getVsize()} method.
    * 
    * @param data
    * data array, which can contain multiple data samples
    * @param timeStep
    * time step in seconds or {@link #EXPLICIT_TIME} if data contains explicitly
    * set timestamps
    */
   public void addData (double[] data, double timeStep) {
      int recSize = (timeStep == EXPLICIT_TIME ? myVsize + 1 : myVsize);
      int k = 0;
      double time = 0;
      while (k < data.length - recSize + 1) {
         NumericListKnot knot = new NumericListKnot (myVsize);
         if (timeStep == EXPLICIT_TIME) {
            knot.t = data[k++];
            time = knot.t;
         }
         else {
            knot.t = time;
            time += timeStep;
         }
         for (int i = 0; i < myVsize; i++) {
            knot.v.set (i, data[k++]);
         }
         myNumericList.add (knot);
      }
      // extendStopTimeIfNecessary();
   }

   /**
    * Adds data to internal data list.
    * 
    * @param t
    * time in seconds
    * @param v
    * vector of values
    * 
    * @throws IllegalArgumentException
    * if size of vector is not equal to {@link #getVsize()}
    */
   public void addData (double t, VectorNd v) {
      if (v.size() != myVsize) {
         throw new IllegalArgumentException ("input vector has size "
         + v.size() + " vs. " + myVsize);
      }
      NumericListKnot knot = new NumericListKnot (myVsize);
      knot.t = t;
      knot.v.set (v);
      myNumericList.add (knot);
      // extendStopTimeIfNecessary();
   }
   
   public void addData(double t, double[] v) {
      if (v.length != myVsize) {
         throw new IllegalArgumentException ("input vector has size "
            + v.length + " vs. " + myVsize);
      }
      NumericListKnot knot = new NumericListKnot (myVsize);
      knot.t = t;
      knot.v.set (v);
      myNumericList.add (knot);
   }

   /**
    * Adds data to internal data list.
    * 
    * @param t
    * time in seconds
    * @param v
    * vector of values
    * 
    * @throws IllegalArgumentException
    * if size of vector is not equal to {@link #getVsize()}
    */
   public void addData (double t, maspack.matrix.Vector v) {
      myNumericList.add (v, t);
      // extendStopTimeIfNecessary();
   }

   public void addData (ReaderTokenizer rtok, double timeStep)
      throws IOException {
      double time = 0;

      // If zero vector size, don't bother adding data
      if (myVsize == 0) {
         return;
      }
      
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         NumericListKnot knot = new NumericListKnot (myVsize);
         if (timeStep == EXPLICIT_TIME) {
            if (rtok.ttype != ReaderTokenizer.TT_NUMBER) {
               throw new IOException ("Expected time value, line "
               + rtok.lineno());
            }

            knot.t = rtok.nval;
            time = knot.t;
         }
         else {
            knot.t = time;
            time += timeStep;
            rtok.pushBack();
         }
         if (rtok.scanNumbers (knot.v.getBuffer(), myVsize) != myVsize) {
            if (rtok.ttype == ReaderTokenizer.TT_EOF) {
               return;
            }
            else {
               throw new IOException ("Unexpected token " + rtok.tokenName()
               + ", line " + rtok.lineno());
            }
         }
         myNumericList.add (knot);
      }
   }

   /**
    * Applies the current numeric data of this probe, as input in the argument
    * <code>vec</code>, and uses it to update the simulation for either
    * the current absolute time <code>t</code> or probe relative time
    * <code>trel</code>. The size of
    * <code>vec</code> will equal the vector size of the probe (as returned by
    * {@link #getVsize()}. Probe relative time is determined
    * from the probe's start time and scale factor
    * using <pre> trel = (t - startTime)/scale </pre>.
    *
    * @param vec supplies the current numeric data
    * @param t absolute time (seconds)
    * @param trel probe relative time
    */
   public void applyData (VectorNd vec, double t, double trel) {
      DataFunction func = getDataFunction();
      if (func != null) {
         func.eval (vec, t, trel);
      }
   }

   /**
    * Interpolate data to specified time and set related properties values.
    * 
    * @param t
    * current time
    */
   public void apply (double t) {
      double trel = (t-getStartTime()) / myScale;
      myNumericList.interpolate (myTmpVec, trel);
      int k = 0;
      double[] buf = myTmpVec.getBuffer();
      applyData (myTmpVec, t, trel);
   }
   
   public boolean isSettable() {
      // for now
      return false;
   }

   public void setData (double sec) {
   }

   public Object clone() throws CloneNotSupportedException {
      NumericControlProbe probe = (NumericControlProbe)super.clone();
      probe.myTmpVec = new VectorNd (myTmpVec.size());
      return probe;
   }

   public NumericList getInput() {
      return myNumericList;
   }

   /** 
    * Returns an array of Objects that represents, for each input variable,
    * either the property that variable maps to (if any), or the dimension
    * of that variable. This information is used by the plotTraceManager.
    * 
    * @return property or dimension information for input variables
    */
   protected Object[] getPropsOrDimens () {
      // just one dimension, for now, equal to vsize. TODO: extend
      // to use vsize for each data function.
      Object[] dimens = new Object[] { myVsize };
      return dimens;
   }

   private PlotTraceInfo[] tmpTraceInfos = null;

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "plotTraceInfo")) {
         tmpTraceInfos = scanPlotTraceInfo (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "data")) {
         createNumericList (getVsize());
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            NumericListKnot knot = new NumericListKnot (myVsize);
            knot.t = rtok.scanNumber();
            for (int i=0; i<myVsize; i++) {
               knot.v.set (i, rtok.scanNumber());
            }
            myNumericList.add (knot);
         }
         return true;
      }
      else if (scanAttributeName (rtok, "vsize")) {
         myVsize = rtok.scanInteger();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      tmpTraceInfos = null;
      super.scan (rtok, ref);
      if (tmpTraceInfos != null) {
         myPlotTraceManager.rebuild (getPropsOrDimens(), tmpTraceInfos);
      }
      else {
         myPlotTraceManager.rebuild (getPropsOrDimens());
      }
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("vsize=" + getVsize());
      pw.println ("data=[");
      IndentingPrintWriter.addIndentation (pw, 2);      
      for (NumericListKnot knot : myNumericList) {
         pw.print (fmt.format(knot.t) + " ");
         pw.println (knot.v.toString (fmt));
      }
      IndentingPrintWriter.addIndentation (pw, -2);      
      pw.println ("]");
      maybeWritePlotTraceInfo (pw);
   }

   public boolean isInput() {
      return true;
   }

   public void createNumericList (int vsize) {
      super.createNumericList (vsize);
      myTmpVec = new VectorNd (vsize);
   }

   public void setVsize (int vsize) {
      setVsize (vsize, null);
   }

   public void setVsize (int vsize, PlotTraceInfo[] traceInfos) {

      createNumericList (vsize);
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
      NumericControlProbe probe;
      try {
         probe = (NumericControlProbe)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone NumericControlProbe");
      }

      double duration = getStopTime()-getStartTime();
      probe.setStartTime (probe.getStartTime()+duration);
      probe.setStopTime (probe.getStopTime()+duration);
      return probe;
   }
}
