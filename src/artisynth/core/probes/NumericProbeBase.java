/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import java.io.*;
import java.util.*;

import javax.swing.JPanel;

import maspack.interpolation.Interpolation;
import maspack.interpolation.Interpolation.Order;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.VectorNd;
import maspack.matrix.RotationRep;
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
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;

//import sun.security.action.GetLongAction;

public abstract class NumericProbeBase extends Probe implements Displayable {

   public static final double EXPLICIT_TIME = -1;

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
   protected VectorNd myTmpVec;
   // rotationRep and rotationSubvecOffsets are null unless the probe contains
   // explict subvectors with rotation information
   protected RotationRep myRotationRep;
   protected int[] myRotationSubvecOffsets;
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

   /** 
    * Sets the values of this numeric probe from the values of another numeric
    * probe {@code src}. The vector size of the source probe must be greater
    * than or equal to that of this probe; extra values in the source probe are
    * ignored.
    *
    * @param src source probe to copy data from
    * @param useAbsoluteTime if {@code true}, time values are mapped between
    * the probes using absolute time; otherwise, probe relative time is used
    */  
   public void setValues (NumericProbeBase src, boolean useAbsoluteTime) {
      if (src.getVsize() < getVsize()) {
         throw new IllegalArgumentException (
            "source probe vector size " + src.getVsize() +
            " less then destination size " + getVsize());
      }
      double tscale = 1.0;
      double toffset = 0.0;
      if (useAbsoluteTime) {
         tscale = src.getScale()/getScale();
         toffset = (src.getStartTime()-getStartTime())/getScale();
      }
      myNumericList.setValues (src.myNumericList, tscale, toffset);
      updateDisplays();
   }

   public abstract void apply (double t);

   public boolean isDisplayable() {
      return myNumericList != null;
   }
   
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
         // Oct 2024: keep small display around to preserve setting like
         // knot point visibility
         // if (display == mySmallDisplay) {
         //   mySmallDisplay = null;
         //}
         return myDisplays.remove ((NumericProbePanel)display);
      }
      else {
         return false;
      }
   }

   public int getVsize() {
      return myVsize;
   }

   protected void initVsize (int vsize) {
      myVsize = vsize;
      myTmpVec = new VectorNd(vsize);      
   }
   
   public RotationRep getRotationRep() {
      return myRotationRep;
   }

   public int[] getRotationSubvecOffsets() {
      return myRotationSubvecOffsets;
   }

   void setRotationSubvecOffsets (int[] offs) {
      NumericList.checkRotationSubvecOffsets (offs, myRotationRep, myVsize);
      myRotationSubvecOffsets = Arrays.copyOf (offs, offs.length);
   }

   protected void createNumericList () {
      if (myRotationRep != null && myRotationSubvecOffsets != null) {
         myNumericList = 
            new NumericList (myVsize, myRotationRep, myRotationSubvecOffsets); 
      }
      else {
         myNumericList = new NumericList (myVsize);
      }
      myNumericList.setInterpolation (myInterpolation);
   }

   public void clearData() {
      myNumericList.clear();
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

   public double[] getDefaultDisplayRange() {
      if (myDefaultDisplayMin != defaultDefaultDisplayRange[0] ||
          myDefaultDisplayMax != defaultDefaultDisplayRange[1]) {
         return new double[] { myDefaultDisplayMin, myDefaultDisplayMax };
      }
      else {
         return getVisibleRange();
      }
   }

   public double[] getMinMaxValues() {
      double[] minMax = new double[2];
      myNumericList.getMinMaxValues (minMax);
      return minMax;
   }
   
   public double[] getVisibleMinMaxValues() {
      return myPlotTraceManager.getVisibleYRange (myNumericList);
   }

   public boolean isEmpty() {
      return myNumericList.isEmpty();
   }

   public static double[] getVisibleRange (
      PlotTraceManager traceManager, NumericList list) {
      double[] range;

      if (list != null) {
         range = traceManager.getVisibleYRange(list);
      }
      else {
         range = new double[2];
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
   
   public double[] getVisibleRange() {
      return getVisibleRange (myPlotTraceManager, myNumericList);      
   }
   
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

   /**
    * Smooths the values in this probe by applying a mean average filter over a
    * moving window of specified size. This window is centered on each data
    * point, and is reduced in size near the end values to ensure a symmetric
    * fit. The end values themselves are not changed.
    * 
    * @param winSize size of the averaging window. The value should be odd; if
    * it is even, it will be incremented internally to be odd.  The method does
    * nothing if the value is is less than 1. Finally, {@code winSize} will be
    * reduced if necessary to fit the number of data points.
    */
   public void smoothWithMovingAverage (int winSize) {
      myNumericList.applyMovingAverageSmoothing (winSize);
   }

   /**
    * Smooths the values in this probe by applying Savitzky-Golay smoothing
    * over a moving window of specified size. Savitzky-Golay smoothing works by
    * fitting the data values in the window to a polynomial of degree {@code
    * deg}, and then using this to recompute the value in the middle of the
    * window. The polynomial is also used to interpolate the first and last
    * {@code winSize/2} values, since it is not possible to center the window
    * on these.
    * 
    * @param deg degree of the smoothing polynomial. Must be at least 1.
    * @param winSize size of the averaging window. The value must be {@code >=
    * deg+1} and should also be odd; if it is even, it will be incremented
    * internally to be odd. Finally, {@code winSize} will be reduced if
    * necessary to fit the number of data points.
    */
   public void smoothWithSavitzkyGolay (int winSize, int deg) {
      myNumericList.applySavitzkyGolaySmoothing (winSize, deg);
   }

   protected NumericConverter[] createConverters (Property[] props) {
      NumericConverter[] converters = new NumericConverter[props.length];
      for (int i = 0; i < props.length; i++) {
         try {
            converters[i] = 
               new NumericConverter (props[i].get(), getRotationRep());
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
      ArrayList<NumericProbeDriver> newDrivers =
         new ArrayList<NumericProbeDriver>();
      for (int i = 0; i < driverExpressions.length; i++) {
         NumericProbeDriver driver = new NumericProbeDriver();
         String expr = driverExpressions[i];
         try {
            driver.setExpression (expr, variables, getRotationRep());
         }
         catch (Exception e) {
            throw new IllegalArgumentException ("Illegal driver expression '"
            + expr + "': " + e.getMessage());
         }
         newDrivers.add (driver);
      }
      return newDrivers;
   }

   protected void updateJythonVariables (
      HashMap<String,NumericProbeVariable> variables, double time) {
      if (myJythonLocals == null) {
         for (NumericProbeDriver driver : myDrivers) {
            if (driver.usesJythonExpression()) {
               myJythonLocals = JythonInit.getArtisynthLocals().copy();
               break;
            }
         }
      }
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
      if (!myPlotTraceManager.hasDefaultSettings (
            propsOrDimens, myRotationRep)) {
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

      if (myRotationSubvecOffsets != null) {
         probe.myRotationSubvecOffsets =
            Arrays.copyOf (
               myRotationSubvecOffsets, myRotationSubvecOffsets.length);
      }

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
                  oldDriver.getExpression(), 
                  probe.myVariables, getRotationRep());
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
      myPlotTraceManager.resetTraceColors(/*useCurrentOrdering=*/false);
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
      myInterpolation.setOrder (interpolationOrder);
      createNumericList();
      addData (rtok, timeStep);
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

   /**
    * Still being implemented
    */
   public void importText (
      File file, boolean explicitTime, boolean overlay, char separator)
      throws IOException  {

      ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer(file);
      DynamicDoubleArray darray = new DynamicDoubleArray();
      try {
         rtok.eolIsSignificant(true);
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            darray.clear();
            if (separator == ' ') {
               while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
                  darray.add (rtok.nval);
               }
               if (rtok.ttype !=  ReaderTokenizer.TT_EOF &&
                   rtok.ttype !=  ReaderTokenizer.TT_EOL) {
                  throw new IOException ("Unexpected token: " + rtok);
               }
            }
            else if (separator == ',') {
            }
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
         rtok.close();
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
    * Still being implemented
    */
   public void importData (File file, boolean explicitTime, boolean overlay) 
      throws IOException {
      String name = file.getName();
      if (ArtisynthPath.getFileExtension(file).equals ("csv")) {
         importText (file, explicitTime, overlay, ',');
      }
      else if (ArtisynthPath.getFileExtension(file).equals ("txt")) {
         importText (file, explicitTime, overlay, ' ');
      }
      else {
         throw new IOException ("Unrecognized type for file "+name);
      }
   }

   /**
    * Find the position properties and rotation subvector offsets needed to
    * build a PositionInputProbe and PositionOutputProbe.
    */
   protected Property[] findPositionPropsAndOffsets (
      ModelComponent[] carray, RotationRep rotRep) {
      ArrayList<Property> props = new ArrayList<>();
      DynamicIntArray offsets = new DynamicIntArray();
      int off = 0;
      for (ModelComponent comp : carray) {
         if (comp instanceof Point) {
            props.add (((Point)comp).getProperty ("position"));
            off += 3;
         }
         else if (comp instanceof Frame) {
            props.add (((Frame)comp).getProperty ("position"));
            off += 3;
            props.add (((Frame)comp).getProperty ("orientation"));
            offsets.add (off);
            if (rotRep == null) {
               throw new IllegalArgumentException (
                  "rotRep must not be null if any components contain poses");
            }
            off += rotRep.size();
         }
         else if (comp instanceof FixedMeshBody) {
            props.add (((FixedMeshBody)comp).getProperty ("position"));
            off += 3;
            props.add (((FixedMeshBody)comp).getProperty ("orientation"));
            offsets.add (off);
            if (rotRep == null) {
               throw new IllegalArgumentException (
                  "rotRep must not be null if any components contain poses");
            }
            off += rotRep.size();
         }
         else {
            throw new IllegalArgumentException (
               "ModelComponent is " + comp.getClass() +
               "; must be a Point, Frame or FixedMeshBody");
         }
      }
      if (offsets.size() > 0) {
         myRotationSubvecOffsets = offsets.getArray();
      }
      myRotationRep = rotRep;
      return props.toArray(new Property[0]);
   }

   /**
    * Find the velocity properties needed to build a VelocityInputProbe and
    * VelocityOutputProbe.
    */
   protected Property[] findVelocityProps (ModelComponent[] carray) {
      ArrayList<Property> props = new ArrayList<>();
      for (ModelComponent comp : carray) {
         if (comp instanceof Point) {
            props.add (((Point)comp).getProperty ("velocity"));
         }
         else if (comp instanceof Frame) {
            props.add (((Frame)comp).getProperty ("velocity"));
         }
         else {
            throw new IllegalArgumentException (
               "ModelComponent is " + comp.getClass() +
               "; must be a Point or Frame");
         }
      }
      return props.toArray(new Property[0]);
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
