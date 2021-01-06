/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

import javax.swing.JPanel;

import maspack.interpolation.Interpolation;
import maspack.interpolation.Interpolation.Order;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.VectorNd;
import maspack.properties.GenericPropertyHandle;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PyStringMap;

import artisynth.core.gui.Displayable;
import artisynth.core.gui.LegendDisplay;
import artisynth.core.gui.NumericProbePanel;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;

//import sun.security.action.GetLongAction;

public abstract class NumericProbeBase extends Probe implements Displayable {
   protected NumericList myNumericList = null;
   protected LinkedHashMap<String,NumericProbeVariable> myVariables = null;
   protected ArrayList<NumericProbeDriver> myDrivers = null;
   protected ArrayList<Property> myPropList = null;
   protected NumericConverter[] myConverters;
   protected PyStringMap myJythonLocals; // maintained by createDrivers

   public NumericProbePanel mySmallDisplay = null;
   protected ArrayList<NumericProbePanel> myDisplays =
      new ArrayList<NumericProbePanel>();
   protected LegendDisplay myLegend = null;

   protected String myFormatStr;
   protected static String defaultFormatStr = "%g";

   protected static Order defaultInterpolationOrder = Order.Linear;
   protected Interpolation myInterpolation;

   protected double myDefaultDisplayMax = 0;
   protected double myDefaultDisplayMin = 0;
   protected static double[] defaultDefaultDisplayRange = new double[] { 0, 0 };

   protected int myVsize;
   protected PlotTraceManager myPlotTraceManager;

   protected static ImportExportFileInfo[] myExportFileInfo =
      new ImportExportFileInfo[] {
      new ImportExportFileInfo ("CSV format", "csv"),
      new ImportExportFileInfo ("Text format", "txt")
   };

   protected TextExportProps myTextExportProps = new TextExportProps();
   protected String myExportFileName;

   public static PropertyList myProps =
      new PropertyList (NumericProbeBase.class, Probe.class);

   static {
      myProps.add ("format * *", "format string for file I/O", defaultFormatStr);
      myProps.add (
         "interpolationOrder * *", "data interpolation order",
         defaultInterpolationOrder);
      myProps.add (
         "displayRange getDefaultDisplayRange setDefaultDisplayRange",
         "min and max display values for display", defaultDefaultDisplayRange);
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myFormatStr = defaultFormatStr;
      myInterpolation = new Interpolation (defaultInterpolationOrder, false);
      myDefaultDisplayMin = defaultDefaultDisplayRange[0];
      myDefaultDisplayMax = defaultDefaultDisplayRange[1];
   }

   public NumericProbeBase() {
      super();
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /** 
    * Returns the values of this probe as a two dimensional array of
    * doubles. This facilitates reading the values into a matlab array.  The
    * array is arranged so that each knot point corresponds to a row, the first
    * column gives the time values, and the remaining columns give the knot
    * point values.
    * 
    * @return Values of this numeric probe
    */   
   public double[][] getValues() {
      return myNumericList.getValues();
   }

   /** 
    * Sets the values of this numeric probe from a two dimensional array of
    * doubles. This facilitates settings the values from a matlab array.  The
    * arrangement of the array is described in {@link #getValues}.
    * 
    * @param vals Values used to set this numeric probe
    */  
   public void setValues(double[][] vals) {
      myNumericList.setValues (vals);
      updateDisplays();
   }

   public abstract void apply (double t);

   public JPanel getDisplay (int w, int h, boolean isLargeDisplay) {
   // track is only needed for large display
      NumericProbePanel display;

      if (isLargeDisplay) {
         display = new NumericProbePanel (this);
         display.setLargeDisplay (true);
         display.setDefaultXRange(); // reset for large display
         myDisplays.add (display);
      }
      else {
         if (mySmallDisplay == null) {
            mySmallDisplay = new NumericProbePanel (this);
            myDisplays.add (mySmallDisplay);
         }
         display = mySmallDisplay;
      }

      display.setDisplaySize (w, h);
      return display;
   }

   public boolean removeDisplay (JPanel display) {
      if (display instanceof NumericProbePanel) {
         if (display == mySmallDisplay) {
            mySmallDisplay = null;
         }
         return myDisplays.remove ((NumericProbePanel)display);
      }
      else {
         return false;
      }
   }

   public int getVsize() {
      return myVsize;
   }

   public void createNumericList (int vsize) {
      myVsize = vsize;
      myNumericList = new NumericList (myVsize);
      myNumericList.setInterpolation (myInterpolation);
   }

   public void updateDisplays() {
      updateDisplays (null);
   }

   public void setSmallDisplayVisible (boolean visible) {
      if (mySmallDisplay != null) {
         mySmallDisplay.setVisible (visible);
      }
   }

   public boolean isSmallDisplayVisible () {
      if (mySmallDisplay != null) {
         return mySmallDisplay.isVisible ();
      }
      else {
         return false;
      }
   }

   public void updateDisplays (NumericProbePanel notToUpdate) {
      for (NumericProbePanel display : myDisplays) {
         if (display.isVisible() && display != notToUpdate) {
            display.repaint();
         }
      }
   }

   /**
    * Update displays if associated properties (like interpolation) have
    * changed.
    */
   public void updateDisplaysForPropertyChanges() {
      for (NumericProbePanel display : myDisplays) {
         display.repaintForPropertyChanges();
      }
   }

   /**
    * Update displays with auto-ranging suppressed. This is for situations where
    * data is changed in the display itself (knot points being dragged) and
    * auto-ranging would cause problems with the usre interaction.
    */
   public void updateDisplaysWithoutAutoRanging() {
      for (NumericProbePanel display : myDisplays) {
         display.repaintWithoutAutoRanging();
      }
   }

   // public void removeDisplay (NumericProbePanel display) {
   //    if (myDisplays.contains (display))
   //       myDisplays.remove (display);
   // }
   
   /**
    * Sets the interpolation method for this numeric input probe.
    * 
    * @param method
    * interpolation method.
    */
   public void setInterpolation (Interpolation method) {
      myInterpolation = new Interpolation (method);
      if (myNumericList != null) {
         myNumericList.setInterpolation (method);
      }
   }

   /**
    * Returns the interpolation method for this numeric input probe.
    * 
    * @return interpolation method
    * @see #setInterpolation
    */
   public Interpolation getInterpolation() {
      return myInterpolation;
   }

   /**
    * Sets the interpolation order for this numeric probe.
    * 
    * @param order
    * new interpolation order
    */
   public void setInterpolationOrder (Order order) {
      myInterpolation.setOrder (order);
      if (myNumericList != null) {
         myNumericList.setInterpolation (myInterpolation);
      }
   }

   /**
    * Returns the interpolation order for this numeric probe.
    * 
    * @return interpolation order
    */
   public Order getInterpolationOrder() {
      return myInterpolation.getOrder();
   }

   public void setFormat (String fmtStr) {
      myFormatStr = fmtStr;
   }

   public String getFormat() {
      return myFormatStr;
   }

   public boolean isCloneable() {
      return true;
   }

   public double getDefaultDisplayMax() {
      return myDefaultDisplayMax;
   }

   public double getDefaultDisplayMin() {
      return myDefaultDisplayMin;
   }

   public void setDefaultDisplayRange (double min, double max) {
      myDefaultDisplayMin = min;
      myDefaultDisplayMax = max;
   }

   public void setDefaultDisplayRange (double[] minmax) {
      myDefaultDisplayMin = minmax[0];
      myDefaultDisplayMax = minmax[1];
   }

   /**
    * Increase the display range of each numeric probe panel.
    */
   public void increaseDisplayRanges() {
      for (NumericProbePanel display : myDisplays)
         display.increaseYRange();
   }

   /**
    * Decrease the display range of each numeric probe panel.
    */
   public void decreaseDisplayRanges() {
      for (NumericProbePanel display : myDisplays)
         display.decreaseYRange();
   }

   public void applyDefaultDisplayRanges() {
      for (NumericProbePanel display : myDisplays) {
         //display.setAutoRange();
         display.resetDisplay();
      }
   }

   //
   // public double[] getDefaultDisplayRange()
   // {
   // return new double[] {
   // myDefaultDisplayMin,
   // myDefaultDisplayMax};
   // }

   public double[] getDefaultDisplayRange() {
      if (myDefaultDisplayMin != defaultDefaultDisplayRange[0] ||
          myDefaultDisplayMax != defaultDefaultDisplayRange[1])
         return new double[] { myDefaultDisplayMin, myDefaultDisplayMax };
      return getRange();
   }

   public double[] getMinMaxValues() {
      double[] minMax = new double[2];
      myNumericList.getMinMaxValues (minMax);
      return minMax;
   }

   public boolean isEmpty() {
      return myNumericList.isEmpty();
   }

   public static double[] getRange (NumericList list) {
      double[] range = new double[2];

      if (list != null) {
         list.getMinMaxValues (range);
      }

      if (Math.abs (range[0] - range[1]) < (Double.MIN_VALUE * 1e3)) {
         range[0] -= 1;
         range[1] += 1;
      }

      double d = (range[1] - range[0]) * 0.1;
      range[0] -= d;
      range[1] += d;

      return range;
   }
   
   public double[] getRange() {
      return getRange (myNumericList);
   }

//   public void setRangeHints (double[] ranges) {
//      myDefaultDisplayMin = ranges[2];
//      myDefaultDisplayMax = ranges[3];
//   }
//
//   public void getRangeHints (double[] ranges) {
//      ranges[0] = 0;
//      ranges[1] = 0;
//      ranges[2] = myDefaultDisplayMin;
//      ranges[3] = myDefaultDisplayMax;
//   }

   /**
    * Scales the values of a numberic probe. Method added by Chad. author: Chad
    * Scales the values of a numberic probe.
    * 
    * @param s scale factor
    * the parameter by which to scale the values.
    */
   public void scaleNumericList (double s) {
      myNumericList.scale(s);
      updateDisplays();
   }

   @Override
   public void setScale (double s) {
      super.setScale (s);

      updateDisplays();
   }

   public NumericList getNumericList() {
      return myNumericList;
   }

   public LinkedHashMap<String,NumericProbeVariable> getVariables() {
      return myVariables;
   }

   public NumericProbeDriver[] getDrivers() {
      return myDrivers.toArray (new NumericProbeDriver[0]);
   }

   public Property[] getAttachedProperties() {
      return myPropList.toArray (new Property[0]);
   }

   // public abstract void set (
   // Property[] props,
   // NumericProbeDriver[] drivers,
   // HashMap<String,NumericProbeVariable> variables);

   // protected LinkedHashMap<String,NumericProbeVariable> copyVariables (
   // Map<String,NumericProbeVariable> variables)
   // {
   // LinkedHashMap<String,NumericProbeVariable> map =
   // new LinkedHashMap<String,NumericProbeVariable>();
   // for (Map.Entry<String,NumericProbeVariable> entry :
   // variables.entrySet())
   // { map.put (entry.getKey(), new NumericProbeVariable(entry.getValue()));
   // }

   // }

   protected NumericConverter[] createConverters (Property[] props) {
      NumericConverter[] converters = new NumericConverter[props.length];
      for (int i = 0; i < props.length; i++) {
         try {
            converters[i] = new NumericConverter (props[i].get());
         }
         catch (Exception e) {
            System.out.println (e);
            throw new IllegalArgumentException ("Property '"
            + props[i].getName() + "' is not numeric");
         }
      }
      return converters;
   }

   protected ArrayList<Property> createPropertyList (Property[] props) {
      ArrayList<Property> propList = new ArrayList<Property>();
      for (int i = 0; i < props.length; i++) {
         propList.add (props[i]);
      }
      return propList;
   }

   protected ArrayList<NumericProbeDriver> createDrivers (
      String[] driverExpressions, HashMap<String,NumericProbeVariable> variables) {
      myJythonLocals = null;
      boolean usesJython = false;
      ArrayList<NumericProbeDriver> newDrivers =
         new ArrayList<NumericProbeDriver>();
      for (int i = 0; i < driverExpressions.length; i++) {
         NumericProbeDriver driver = new NumericProbeDriver();
         String expr = driverExpressions[i];
         try {
            driver.setExpression (expr, variables);
         }
         catch (Exception e) {
            throw new IllegalArgumentException ("Illegal driver expression '"
            + expr + "': " + e.getMessage());
         }
         if (driver.usesJythonExpression()) {
            usesJython = true;
         }
         newDrivers.add (driver);
      }
      if (usesJython) {
         myJythonLocals = JythonInit.getArtisynthLocals().copy();
      }
      return newDrivers;
   }

   protected void updateJythonVariables (
      HashMap<String,NumericProbeVariable> variables, double time) {
      if (myJythonLocals != null) {
         PyStringMap map = myJythonLocals;
         for (Map.Entry<String,NumericProbeVariable> entry :
                 variables.entrySet()) {
            NumericProbeVariable var = entry.getValue();
            map.__setitem__ (
               new PyString (entry.getKey()), Py.java2py (var.getValue()));
         }
         map.__setitem__ ("t", Py.java2py (time));
      }
   }

   static public boolean isValidVariableName (String name) {
      int len = name.length();
      if (!Character.isJavaIdentifierStart (name.charAt (0))) {
         return false;
      }
      for (int i = 1; i < len; i++) {
         if (!Character.isJavaIdentifierPart (name.charAt (i))) {
            return false;
         }
      }
      return true;
   }

   protected abstract Object[] getPropsOrDimens ();

   protected PlotTraceInfo[] scanPlotTraceInfo (ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      ArrayList<PlotTraceInfo> traceInfo = new ArrayList<PlotTraceInfo>();
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         PlotTraceInfo pti = new PlotTraceInfo();
         pti.scan (rtok);
         traceInfo.add (pti);
      }
      return traceInfo.toArray (new PlotTraceInfo[0]);      
   }

   protected void maybeWritePlotTraceInfo (PrintWriter pw) {
      Object[] propsOrDimens = getPropsOrDimens();
      if (!myPlotTraceManager.hasDefaultSettings (propsOrDimens)) {
         pw.println ("plotTraceInfo=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         PlotTraceInfo[] allTraces =
            myPlotTraceManager.getAllTraceInfo (propsOrDimens);
         for (PlotTraceInfo pti : allTraces) {
            pti.write (pw);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   protected ArrayList<Property> scanProperties (
      ReaderTokenizer rtok, CompositeComponent ancestor) throws IOException {

      ArrayList<Property> list = new ArrayList<Property>();
      IOException errorex = null;

      rtok.scanToken ('[');
      int csave = rtok.getCharSetting (':');
      int dsave = rtok.getCharSetting ('-');
      rtok.wordChar (':');
      rtok.wordChar ('-');

      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsWord() && rtok.ttype != '"') {
            errorex = new IOException ("expected property name; got " + rtok);
            break;
         }
         if (ancestor == null) {
            errorex = new IOException (
               "CompositeComponent reference object required");
            break;
         }
         Property prop = ComponentUtils.findProperty (ancestor, rtok.sval);
         if (prop == null) {
            errorex = new IOException (
               "Cannot find property " + rtok.sval);
            break;
         }
         list.add (prop);
      }
      rtok.setCharSetting (':', csave);
      rtok.setCharSetting ('-', dsave);
      if (errorex != null) {
         throw errorex;
      }
      return list;
   }

   public Object clone() throws CloneNotSupportedException {
      NumericProbeBase probe = (NumericProbeBase)super.clone();
      probe.myNumericList = (NumericList)myNumericList.clone();
      probe.myInterpolation = new Interpolation (myInterpolation);

      if (myVariables != null) {
         // make a deep copy of the variable list
         probe.myVariables = new LinkedHashMap<String,NumericProbeVariable>();
         for (Map.Entry<String,NumericProbeVariable> entry :
                 myVariables.entrySet()) {
            probe.myVariables.put (entry.getKey(), new NumericProbeVariable (
                                      entry.getValue()));
         }
      }
      if (myDrivers != null) {
         probe.myDrivers = new ArrayList<NumericProbeDriver>();
         for (NumericProbeDriver oldDriver : myDrivers) {
            NumericProbeDriver driver = new NumericProbeDriver();
            try {
               driver.setExpression (
                  oldDriver.getExpression(), probe.myVariables);
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "Illegal driver expression '" + oldDriver.getExpression() +
                  "': " + e.getMessage());
            }
            probe.myDrivers.add (driver);
         }
      }
      if (myPropList != null) {
         probe.myPropList = (ArrayList<Property>)myPropList.clone();
      }
      if (myConverters != null) {
         probe.myConverters = new NumericConverter[myConverters.length];
         for (int i = 0; i < myConverters.length; i++) {
            probe.myConverters[i] = new NumericConverter (myConverters[i]);
         }
      }
      
      // clear the displays because these are lazily allocated and
      // we don't want them reused
      probe.mySmallDisplay = null;
      probe.myDisplays = new ArrayList<NumericProbePanel>();

      // attached file should also not be brought into clone
      probe.myAttachedFileName = null;

      return probe;
   }

   public boolean isPrintable() {
      return true;
   }

   public VectorNd getData (double sec) {
	  VectorNd vals = new VectorNd(myVsize);
      double t = getVirtualTime (sec);
      myNumericList.interpolate (
         vals, t, myNumericList.getInterpolation().getOrder(), 
         true, myNumericList.getLast());
      return vals;
   }
   
   public void print (double sec) {
      System.out.println (getName() + " probe data = ["
      + getData(sec).toString (myFormatStr) + " ]");
   }

   public void setLegendLabels (List<String> labels) {
      if (labels.size() == myNumericList.getVectorSize()) {
         for (int i = 0; i < labels.size (); i++) {
            myPlotTraceManager.getTraceInfo (i).setLabel (labels.get (i));
         }
      }
      else
         System.out.println ("numeric probe too few explicit labels "
         + labels.size() + ", expecting " + myNumericList.getVectorSize());
   }
   
   public PlotTraceInfo getPlotTraceInfo (int idx) {
      return myPlotTraceManager.getTraceInfo (idx);
   }
   
   public int getOrderedTraceIndex (int order) {
      return myPlotTraceManager.getOrderedTraceIndex (order);
   }
   
   /**
    * Sets a new ordering for the plot traces. This is specified by an array
    * giving the indices of the plot traces in the order they should be
    * plotted.
    */
   public void setTraceOrder (int[] indices) {
      myPlotTraceManager.setTraceOrder (indices);
      updateDisplays();
   }

   public void swapPlotTraceOrder (PlotTraceInfo pti0, PlotTraceInfo pti1) {
      myPlotTraceManager.swapTraceOrder (pti0, pti1);
      updateDisplays();
   }

   public void resetTraceOrder (){
      myPlotTraceManager.resetTraceOrder();
      updateDisplays();
   }

   public void setTraceVisible (int idx, boolean visible) {
      myPlotTraceManager.setTraceVisible (idx, visible);
      updateDisplays();
   }

   public boolean isTraceVisible (int idx) {
      return myPlotTraceManager.isTraceVisible (idx);
   }

   public void setTraceColor (int idx, Color color) {
      myPlotTraceManager.setTraceColor (idx, color);
      updateDisplays();
   }

   public Color getTraceColor (int idx) {
      return myPlotTraceManager.getTraceColor (idx);
   }

   public void resetTraceColors (){
      myPlotTraceManager.resetTraceColors();
      updateDisplays();
   }
   
   public PlotTraceManager getTraceManager() {
      return myPlotTraceManager;
   }

   public void setTraceLabel (int idx, String label) {
      getPlotTraceInfo(idx).setLabel (label);
   }

   public String getTraceLabel (int idx) {
      return getPlotTraceInfo(idx).getLabel();
   }

   public LegendDisplay getLegend () {
      return myLegend;
   }
   
   public void setLegend (LegendDisplay legend) {
      myLegend = legend;
   }

   public void removeLegend () {
      if (myLegend != null) {
         if (myLegend.isVisible()) {
            myLegend.dispose();
         }
         myLegend = null;
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void dispose () {
      removeLegend();
   }

   public String getMatlabName () {
      if (getName() != null) {
         return getName();
      }
      else if (isInput()) {
         return "iprobe" + getNumber();
      }
      else {
         return "oprobe" + getNumber();
      }
   }

   public void saveToMatlab (MatlabInterface mi, String matlabName)
      throws MatlabInterfaceException {
        
      if (matlabName == null) {
         matlabName = getMatlabName();
      }
      mi.objectToMatlab (getValues(), matlabName);
   }

   public boolean loadFromMatlab (MatlabInterface mi, String matlabName) 
      throws MatlabInterfaceException {

      if (matlabName == null) {
         matlabName = getMatlabName();
      }
      double[][] vals = mi.arrayFromMatlab (matlabName);
      if (vals == null) {
         return false;
      }
      else {
         setValues (vals);
         return true;
      }
   }

   protected boolean dataFunctionsAreCopyable (
      ArrayList<DataFunction> dataFunctions) {

      for (DataFunction df : dataFunctions) {
         if (!(df instanceof Clonable)) {
            return false;
         }
      }
      return true;
   }

   protected ArrayList<DataFunction> copyDataFunctions (
      ArrayList<DataFunction> dataFunctions) {

      ArrayList<DataFunction> newFunctions = new ArrayList<DataFunction>();
      for (DataFunction df : dataFunctions) {
         if (df instanceof Clonable) {
            DataFunction newDf = null;
            try { 
               newDf = (DataFunction)((Clonable)df).clone();
            }
            catch (Exception e) {
               // clone not supported or failed for some reason
            }
            if (newDf != null) {
               newFunctions.add (newDf);
            }
         }
      }
      return newFunctions;
   } 
   
   public boolean isWritable() {
      if (myPropList != null) {
         for (Property prop : myPropList) {
            if (prop instanceof GenericPropertyHandle) {
               ModelComponent comp = ComponentUtils.getPropertyComponent(prop);
               if (comp != null && !comp.isWritable()) {
                  if (!(comp instanceof RootModel) || 
                      !RootModel.isBaseProperty (prop.getName())) {
                     return false;
                  }
               }
            }
         }
      }
      return true;
   }

   /* --- file export methods --- */

   public void writeText (
      File file, String fmtStr, String separator, boolean includeTime)
      throws IOException  {

      PrintWriter pw =
         new PrintWriter (new BufferedWriter (new FileWriter (file)));

      try {
         NumberFormat fmt = new NumberFormat (fmtStr);
         Iterator<NumericListKnot> it = myNumericList.iterator();
         while (it.hasNext()) {
            NumericListKnot knot = it.next();
            if (includeTime) {
               pw.print (fmt.format (knot.t));
            }
            for (int i=0; i<knot.v.size(); i++) {
               if (includeTime || i>0) {
                  pw.print (separator);
               }
               pw.print (fmt.format(knot.v.get(i)));
            }
            pw.println ("");
         }     
      }
      catch (Exception e) {
         if (e instanceof IOException) {
            throw (IOException)e;
         }
         else {
            throw new IOException ("Internal error: ", e);
         }
      }
      finally {
         pw.close();
      }
   }

   public ImportExportFileInfo[] getExportFileInfo() {
      return myExportFileInfo;
   }

   public ExportProps getExportProps (String ext) {
      if (ext != null && (ext.equals ("csv") || ext.equals ("txt"))) {
         return myTextExportProps;
      }
      else {
         return null;
      }
   }

   public void export (File file, ExportProps props)
      throws IOException {
      String name = file.getName();
      if (ArtisynthPath.getFileExtension(file).equals ("csv")) {
         if (!(props instanceof TextExportProps)) {
            throw new InternalErrorException (
               "Expected TextExportProps, got "+props);
         }
         TextExportProps tprops = (TextExportProps)props;
         writeText (file, tprops.getFormatStr(), ", ", tprops.getIncludeTime());
      }
      else if (ArtisynthPath.getFileExtension(file).equals ("txt")) {
         if (!(props instanceof TextExportProps)) {
            throw new InternalErrorException (
               "Expected TextExportProps, got "+props);
         }
         TextExportProps tprops = (TextExportProps)props;
         writeText (file, tprops.getFormatStr(), " ", tprops.getIncludeTime());
      }
      else {
         throw new IOException ("Unrecognized type for file "+name);
      }
   }

   /**
    * Ideally, we should implement updateReferences() to handle changes to the
    * soft references. Best would be to modify myPropList to remove properties
    * that no longer connect to valid components, but this is
    * complicated. Instead, at present, the probe will continue to set
    * properties in deleted components.
    */
   // @Override
   // public void getSoftReferences (List<ModelComponent> refs) {
   //    HashSet<ModelComponent> myrefs = new HashSet<ModelComponent>();
   //    if (myPropList != null) {
   //       for (Property prop : myPropList) {
   //          ModelComponent comp = null;
   //          if (prop instanceof GenericPropertyHandle) {
   //             comp = ComponentUtils.getPropertyComponent (prop);
   //          }
   //          if (comp != null) {
   //             myrefs.add (comp);
   //          }
   //       }
   //       refs.addAll (myrefs);
   //    }
   // }

}
