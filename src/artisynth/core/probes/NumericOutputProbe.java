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
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;
import artisynth.core.modelbase.*;

import artisynth.core.util.*;

public class NumericOutputProbe extends NumericProbeBase 
   implements CopyableComponent {
   private boolean myShowTime;
   private static boolean defaultShowTime = true;

   private boolean myShowHeader;
   private static boolean defaultShowHeader = true;

   public static PropertyList myProps =
      new PropertyList (NumericOutputProbe.class, NumericProbeBase.class);

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

   public NumericOutputProbe() {
      setDefaultValues();
      myPlotTraceManager = new PlotTraceManager ("output");
   }

   public NumericOutputProbe (
      ModelComponent comp, String propName, String fileName, double interval) {
      this (comp, new String[] { propName }, fileName, interval);
   }

   public NumericOutputProbe (
      ModelComponent comp, String[] propNames, String fileName, double interval) {
      this();
      Property[] props = new Property[propNames.length];
      for (int i=0; i<props.length; i++) {
         props[i] = comp.getProperty (propNames[i]);
         if (props[i] == null) {
            throw new IllegalArgumentException ("cannot find property '"
            + propNames[i] + "'");
         }
      }
      setOutputProperties (props);
      setAttachedFileName (fileName);
      setUpdateInterval (interval);
      setStopTime (1);
   }

   public NumericOutputProbe (
      ModelComponent comp, String propName,
      double startTime, double stopTime, double interval) {
      this();
      Property prop = comp.getProperty (propName);
      if (prop == null) {
         throw new IllegalArgumentException ("cannot find property '"
         + propName + "'");
      }
      setOutputProperties (new Property[] { prop });
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
   }

   public NumericOutputProbe (
      Property[] props, double interval, double ymin, double ymax) {
      this();
      setUpdateInterval (interval);
      setOutputProperties (props);
      setDefaultDisplayRange (ymin, ymax);
   }

   public void setOutputProperties (Property[] props) {
      String[] driverExpressions = new String[props.length];
      String[] variableNames = new String[props.length];

      for (int i = 0; i < props.length; i++) {
         variableNames[i] = "P" + i;
         driverExpressions[i] = "P" + i;
      }
      set (props, driverExpressions, variableNames);
   }

   public NumericOutputProbe (Property[] props, double interval) {
      this (props, interval, 0, 0);
   }

//   public NumericOutputProbe (Property prop, long interval) {
//      this (new Property[] { prop }, interval, 0, 0);
//   }

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

   public void apply (double t) {
      if (myPropList == null) {
         throw new ImproperStateException ("probe not initialized");
      }
      // XXX don't we want to apply scaling here too?
      double tloc = (t-getStartTime())/myScale;

      NumericListKnot knot = new NumericListKnot (myVsize);
      int i = 0;
      for (NumericProbeVariable var : myVariables.values()) {
         Object obj = myPropList.get (i).get();
         var.setValues (myConverters[i].objectToArray (obj));
         i++;
      }
      updateJythonVariables (myVariables, tloc);
      int k = 0;
      double[] buf = knot.v.getBuffer();
      for (NumericProbeDriver driver : myDrivers) {
         double[] vals = driver.eval (myVariables, myJythonLocals);
         for (int j = 0; j < vals.length; j++) {
            buf[k++] = vals[j];
         }
      }
      knot.t = tloc;
      myNumericList.add (knot);
      myNumericList.clearAfter (knot);
   }

   // public void display (Component c, Graphics g)
   // {
   // }

   public Object clone() throws CloneNotSupportedException {
      NumericOutputProbe probe = (NumericOutputProbe)super.clone();
      //probe.myNumericList.clear();
      return probe;
   }

   public NumericList getOutput() {
      return myNumericList;
   }

   /** 
    * Returns an array of Objects that represents, for each driver output,
    * either the property that output maps to (if any), or the dimension
    * of the output. This information is used by the plotTraceManager.
    * 
    * @return property or dimension information for outputs
    */
   protected Object[] getPropsOrDimens () {
      Object[] propsOrDimens = new Object[myDrivers.size()];

      for (int idx=0; idx<myDrivers.size(); idx++) {
         NumericProbeDriver driver = myDrivers.get(idx);
         String singleVariable = driver.getSingleVariable();
         
         if (singleVariable != null) {
            int k = 0;
            for (String varname : myVariables.keySet()) {
               if (varname.equals (singleVariable)) {
                  propsOrDimens[idx] = myPropList.get(k);
                  break;
               }
               k++;
            }
         }
         else {
            propsOrDimens[idx] = driver.getOutputSize();
         }
      }
      return propsOrDimens;
   }

   private String[] tmpVariableNames = null;
   private String[] tmpDriverExpressions = null;
   private PlotTraceInfo[] tmpTraceInfos = null;

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanAndStorePropertyPaths (rtok,"props",tokens)>=0) {
         return true;
      }
      else if (scanAttributeName (rtok, "drivers")) {
         tmpDriverExpressions = Scan.scanQuotedStrings (rtok, '"');
         return true;
      }
      else if (scanAttributeName (rtok, "variables")) {
         tmpVariableNames = Scan.scanWords (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "plotTraceInfo")) {
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
      myPropList = null;
      tmpVariableNames = null;
      tmpDriverExpressions = null;
      tmpTraceInfos = null;

      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "props")) {
         myPropList = ScanWriteUtils.postscanProperties (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      super.postscan (tokens, ancestor);
      
      if (myPropList == null) {
         throw new IOException ("'props' not specified");
      }
      if (tmpVariableNames == null || tmpDriverExpressions == null) {
         setOutputProperties (myPropList.toArray (new Property[0]));
      }
      else {
         set (
            myPropList.toArray (new Property[0]), tmpDriverExpressions,
            tmpVariableNames, tmpTraceInfos, 
            /*initData=*/myNumericList==null);
      }

      tmpVariableNames = null;
      tmpDriverExpressions = null;
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("vsize=" + getVsize());
      if (myPropList != null && myPropList.size() > 0) {
         pw.println ("props=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i = 0; i < myPropList.size(); i++) {
            pw.println (ComponentUtils.getWritePropertyPathName (
               myPropList.get (i), ancestor));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
         pw.println ("variables=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (String name : myVariables.keySet()) {
            pw.println (name);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
      else {
         pw.println ("props=[ ]");
         pw.println ("variables=[ ]");
      }
      if (myDrivers != null && myDrivers.size() > 0) {
         pw.println ("drivers=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (NumericProbeDriver driver : myDrivers) {
            pw.println (driver);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
      else {
         pw.println ("drivers=[ ]");
      }
      if (myNumericList != null) {
         pw.println ("data=[");
         IndentingPrintWriter.addIndentation (pw, 2);      
         for (NumericListKnot knot : myNumericList) {
            pw.print (fmt.format(knot.t) + " ");
            pw.println (knot.v.toString (fmt));
         }
         IndentingPrintWriter.addIndentation (pw, -2);      
         pw.println ("]");
      }
      maybeWritePlotTraceInfo (pw);
   }

   public void set (
      Property[] props, String[] driverExpressions, String[] variableNames) {
      set (props, driverExpressions, variableNames, null, /*initData=*/true);
   }

   public void set (
      Property[] props, String[] driverExpressions,
      String[] variableNames, PlotTraceInfo[] traceInfos, boolean initData) {
      if (variableNames.length != props.length) {
         throw new IllegalArgumentException (
            "Number of variable names does not equal the number of properties");
      }
      NumericConverter[] newConverters = createConverters (props);
      LinkedHashMap<String,NumericProbeVariable> newVariables =
         new LinkedHashMap<String,NumericProbeVariable>();
      for (int i = 0; i < props.length; i++) {
         String name = variableNames[i];
         if (name == null || newVariables.get (name) != null) {
            throw new IllegalArgumentException (
               "null or repeated variable name '" + name + "'");
         }
         if (!isValidVariableName (name)) {
            throw new IllegalArgumentException ("variable name '" + name
            + "' is not a valid variable name");
         }
         newVariables.put (name, new NumericProbeVariable (
                              newConverters[i].getDimension()));
      }

      ArrayList<NumericProbeDriver> newDrivers =
         createDrivers (driverExpressions, newVariables);

      myVsize = 0;
      for (int i = 0; i < newDrivers.size(); i++) {
         myVsize += newDrivers.get (i).getOutputSize();
      }
      myDrivers = newDrivers;
      myPropList = createPropertyList (props);
      myVariables = newVariables;
      myConverters = newConverters;

      if (initData) {
         myVsize = 0;
         for (int i = 0; i < newDrivers.size(); i++) {
            myVsize += newDrivers.get (i).getOutputSize();
         }
         myNumericList = new NumericList (myVsize);
      }

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
   public boolean isDuplicatable() {
      return true;
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
      NumericOutputProbe probe;
      try {
         probe = (NumericOutputProbe)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone NumericOutputProbe");
      }
      double duration = getStopTime()-getStartTime();
      probe.setStartTime (probe.getStartTime()+duration);
      probe.setStopTime (probe.getStopTime()+duration);
      return probe;
   }

}
