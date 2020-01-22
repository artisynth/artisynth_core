/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import artisynth.core.driver.Main;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.properties.HierarchyNode;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.io.*;
import java.util.Iterator;
import java.util.Deque;

public abstract class Probe extends ModelAgentBase {
   public static boolean useOldSaveMethod = true;
   public static boolean writeStartStopTimeInSeconds = true;

   protected double myStartTime;
   protected static double defaultStartTime = 0;

   protected double myStopTime;
   protected static double defaultStopTime = 5;

   protected double myScale = 1.0;
   protected static double defaultScale = 1;

   protected double myUpdateInterval;
   protected static double defaultUpdateInterval = -1;

   protected static final int SELECTED = 0x1;
   protected static final int MARKED = 0x2;
   protected static final int ANCESTOR_SELECTED = 0x4;

   protected boolean myScalableP = false;
   protected String myAttachedFileName;
   protected static String defaultAttachedFileName = null;
   protected String myExportFileName;

   protected int myTrackNum = -1;

   protected int PRINT_WRITER_INDENT = 2;

   public static PropertyList myProps =
   new PropertyList (Probe.class, ModelAgentBase.class);

   /**
    * Contains information about the types of files this probe
    * can be exported or imported to.
    */
   public static class ImportExportFileInfo {
      String myDescription;  // description of the file type
      String myExt;          // file extenstion (without the leading '.')
      
      public ImportExportFileInfo (String description, String ext) {
         myDescription = description;
         myExt = ext;
      }

      public String getDescription() {
         return myDescription;
      }

      public String getExt() {
         return myExt;
      }
   }

   public interface ExportProps extends HasProperties {
   }

   public String getExportFileName() {
      return null;
   }

   public File getExportFile() {
      return getFileFromPath (myExportFileName);
   }

   public void setExportFileName (String fileName) {
      myExportFileName = ArtisynthPath.convertToLocalSeparators (fileName);
   }

   public ExportProps getExportProps (String ext) {
      return null;
   }

   public ImportExportFileInfo[] getExportFileInfo() {
      return new ImportExportFileInfo[0];
   }

   public ImportExportFileInfo[] getImportFileInfo() {
      return new ImportExportFileInfo[0];
   }

   public void export (File file, ExportProps props)
      throws IOException {
   }

   static {
      //myProps.add ("name * *", "name for this probe", defaultName);
      myProps.add ("startTime * *", "start time", defaultStartTime, "NE NW");
      myProps.add ("stopTime * *", "stop time", defaultStopTime, "NE NW");
      myProps.add ("scale * *", "scale", defaultScale, "NE");
      myProps.add (
         "attachedFile getAttachedFileName setAttachedFileName",
         "file attached to this probe", defaultAttachedFileName);
      myProps.add (
         "updateInterval * *", "update interval", defaultUpdateInterval, "NW");
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      setStartTime (defaultStartTime);
      setStopTime (defaultStopTime);
      myScale = defaultScale;
      setUpdateInterval (defaultUpdateInterval);
      myActiveP = defaultActiveP;
      setAttachedFileName (defaultAttachedFileName);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Probe() {
      setDefaultValues();
   }

   static public double getDefaultStartTime() {
      return defaultStartTime;
   }

   static public double getDefaultStopTime() {
      return defaultStopTime;
   }

   static public double getDefaultScale() {
      return defaultScale;
   }

   static public double getDefaultUpdateInterval() {
      return defaultUpdateInterval;
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public double getStartTime() {
      return myStartTime;
   }

   public void setStartTime (double t) {
      myStartTime = t;
   }

   public double getStopTime() {
      return myStopTime;
   }

   public void setStopTime (double t) {
      myStopTime = t;
   }

   public double getDuration() {
      return myStopTime - myStartTime;
   }

   public void setInterval (double startTime, double stopTime) {
      myStartTime = startTime;
      myStopTime = stopTime;      
   }
   
   public void setStartStopTimes (double startTime, double stopTime) {
      setInterval (startTime, stopTime);
   }

   public double getScale() {
      return myScale;
   }

   public void setScale (double s) {
      if (!myScalableP) {
         throw new UnsupportedOperationException ("probe is not scalable");
      }
      myScale = s;
   }

   public void setScalable (boolean enable) {
      myScalableP = enable;
   }

   public boolean isScalable() {
      return myScalableP;
   }

   public double getUpdateInterval() {
      return myUpdateInterval;
   }

   public void setUpdateInterval (double t) {
      if (t < 0) {
         myUpdateInterval = -1;
      }
      else {
         myUpdateInterval = t;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setName (String name) {
      // XXX Override setName because some probes need to have
      // invalid names for historical reasons
      String err = ModelComponentBase.checkName (name, this);
      if (err != null) {
         System.out.println (
            "WARNING: probe name '" + name + "' invalid; please fix");
      }
      if (name != null && name.length() == 0) {
         name = null;
      }
      NameChangeEvent e = new NameChangeEvent (this, myName);
      myName = name;
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasChildren() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public Iterator<? extends HierarchyNode> getChildren() {
      return null;
   }

   /**
    * Queries whether time {@code t} is an event time for this probe.
    * 
    * @param t time to query
    * @return {@code true} if {@code t} is an event time
    */
   public boolean isEventTime (double t) {

      double timeFromStart = t - myStartTime;
      if (TimeBase.compare (timeFromStart, 0) < 0 ||
          TimeBase.compare (t, myStopTime) > 0) {
         return false;
      }
      else if (timeFromStart == 0 || TimeBase.equals (t, myStopTime)) {
         return true;
      }
      else if (myUpdateInterval != -1) {
         return TimeBase.modulo (timeFromStart, myUpdateInterval) == 0;
      }
      else {
         return false;
      }
   }            

   /**
    * Returns the time of the next probe event <i>after</i> time {@code t}.
    * 
    * @param t reference time for the next event
    * @return time of next probe event after t
    */
   public double nextEventTime (double t) {
      
      double timeFromStart = t - myStartTime;
      double tnext;

      if (TimeBase.compare (timeFromStart, 0) < 0) {
         tnext = myStartTime;
      }
      else if (TimeBase.compare (t, myStopTime) >= 0) {
         tnext = -1;
      }
      else {
         if (myUpdateInterval == -1) {
            tnext = myStopTime;
         }
         else {
            // compute tnext with respect to t instead of timeFromStart
            // to ensure consistent event alignment
            tnext = (t + myUpdateInterval - 
                     TimeBase.modulo (t, myUpdateInterval));
            if (tnext > myStopTime) {
               tnext = myStopTime;
            }
         }
      }
      return tnext;
   }

   /**
    * Called at time {@code t} to apply this probe
    * 
    * @param t time at which probe is being applied
    */
   public abstract void apply (double t);
   
   public boolean isCloneable() {
      return false;
   }

   public void save() throws IOException {
      System.out.println ("saving sate of the probe");
   }

   public void load() throws IOException {
   }

   public double getVirtualTime (double sec) {
      return (sec - getStartTime()) / myScale;
   }

   public double getTimelineTime (double t) {
      return (myScale * t) + getStartTime();
   }

   protected double scanTimeQuantity (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (!rtok.tokenIsNumber()) {
         throw new IOException (
            "expected time value; got "+rtok+", line "+rtok.lineno());
      }
      double time = 0;
      // Allow for times to be indicated using the old
      // format, as an integer indicating nanoseconds, or in the new
      // format, as a double indicating seconds.
      if (rtok.tokenIsInteger()) {
         time = 1.0e-9*rtok.lval;
      }
      else {
         time = rtok.nval;
      }     
      return time;
   }

   protected boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "track")) {
         myTrackNum = rtok.scanInteger();
         return true;
      }
      else if (scanAttributeName (rtok, "startTime")) {
         setStartTime (scanTimeQuantity(rtok));
         return true;
      }
      else if (scanAttributeName (rtok, "stopTime")) {
         setStopTime (scanTimeQuantity(rtok));
         return true;
      }
      else if (scanAttributeName (rtok, "updateInterval")) {
         setUpdateInterval (scanTimeQuantity(rtok));
         return true;
      }
      else if (scanAndStoreReference (rtok, "model", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      setDefaultValues();
      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "model")) {
         setModel (postscanReference (tokens, Model.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /**
    * Write probe attributes to a PrintWriter
    * 
    * @param pw writer to which components are written
    * @param fmt format for floating point numbers
    * @param ancestor ancestor component which respect to which references are
    * expressed
    * @throws IOException if an I/O error occurred
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (!(ancestor instanceof CompositeComponent)) {
         throw new IOException (
            "CompositeComponent reference object required for probe");
      }
      ancestor = (CompositeComponent)ancestor;

      if (writeStartStopTimeInSeconds) {
         pw.println ("startTime=" + getStartTime());
         pw.println ("stopTime=" + getStopTime());
         pw.println ("updateInterval=" + getUpdateInterval());
      }
      else {
         pw.println ("startTime=" + (long)(1e9)*getStartTime());
         pw.println ("stopTime=" + (long)(1e9)*getStopTime());
         pw.println ("updateInterval=" + (long)(1e9)*getUpdateInterval());
      }
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      pw.println ("track=" + myTrackNum);
      pw.println ("model=" + ComponentUtils.getPathName (ancestor, myModel));
   }

   public boolean isAttachedFileRelative() {
      if (myAttachedFileName == null) {
         return false;
      }
      return !(new File (myAttachedFileName)).isAbsolute();
   }

   public static File getFileFromPath (String fileName) {
      // JWS: should allow resources as well as files
      if (fileName == null) {
         return null;
      }
      File file = new File (fileName);
      if (file.isAbsolute()) {
         return file;
      }
      else {
         if (useOldSaveMethod) {
            return new File (ArtisynthPath.getWorkingDir(), fileName);
         }
         else {
            return new File (
               Main.getMain().getProbeDirectory(), fileName);
         }
      }
   }
   
   /**
    * Returns the path for a file. If the file is located beneath the default
    * location for probe files, the path will be relative to that
    * location. Otherwise, it will be absolute. At present, the 'default'
    * location is simply the current ArtiSynth working directory.
    */
   public static String getPathFromFile (File file) {
      if (file == null) {
         return null;
      }
      File dir;
      if (useOldSaveMethod) {
         dir = ArtisynthPath.getWorkingDir();
      }
      else {
         dir = Main.getMain().getProbeDirectory();
      }
      return ArtisynthPath.getRelativeOrAbsolutePath (dir, file);
   }

   public File getAttachedFile() {
      return getFileFromPath (myAttachedFileName);
   }

   public void setAttachedFileName (String fileName) {
      myAttachedFileName = ArtisynthPath.convertToLocalSeparators (fileName);
   }

   public String getAttachedFileName() {
      return myAttachedFileName;
   }

   public boolean hasAttachedFile() {
      return myAttachedFileName != null;
   }

   /**
    * Sets the Timeline track number associated with this probe. The Timeline
    * should use this method to keep the track information current.
    * 
    * @param num
    * new track number
    */
   public void setTrack (int num) {
      myTrackNum = num;
   }

   /**
    * Returns the Timeline track number associated with this probe.
    * 
    * @return timeline track number
    */
   public Integer getTrack() {
      return myTrackNum;
   }

   /**
    * Returns true if this probe is an input probe, and false otherwise.
    * 
    * @return true if this is an input probe
    */
   public boolean isInput() {
      return false;
   }

   public boolean isSettable() {
      return false;
   }

   public void setData (double sec) {
   }

   public boolean isPrintable() {
      return false;
   }

   public void print (double sec) {
   }

   public boolean hasState() {
      return false;
   }
   
   public ComponentState createState (
      ComponentState prevState) {
      return new EmptyState();
   }
   
   public void getState (ComponentState state) {
   }

   public void setState (ComponentState state) {
   }
   
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {
      if (oldstate == null) {
         getState (newstate);
      }
      else {
         newstate.set (oldstate);
      }
   }
   
}
