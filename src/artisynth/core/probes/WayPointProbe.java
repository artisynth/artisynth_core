/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.*;
import java.io.*;

import javax.swing.JFileChooser;

import artisynth.core.driver.Main;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.util.*;

public class WayPointProbe extends OutputProbe {
   protected LinkedList<WayPoint> myWayPoints;
   protected RootModel myRootModel;
   protected WayPoint myWay0;
   protected boolean myCheckStateP = false;

   public WayPointProbe() {
      setStartTime (0);
      setStopTime (Double.POSITIVE_INFINITY);
      setUpdateInterval (-1);
      myActiveP = true;
      myWay0 = new WayPoint (0);
      myWayPoints = new LinkedList<WayPoint>();
      myWayPoints.add (myWay0);
      myWay0.setValid (false);
   }

   public WayPointProbe (RootModel model) {
      this();
      myRootModel = model;
   }

   public void setCheckState (boolean enable) {
      myCheckStateP = enable;
   }

   public boolean getCheckState() {
      return myCheckStateP;
   }

   /**
    * Adds a WayPoint to this probe, and returns any WayPoint that previously
    * occupied the same time location.
    * 
    * @param newWay
    * new WayPoint to add
    * @return previous WayPoint with the same time, if any
    */
   public WayPoint add (WayPoint newWay) {
      ListIterator<WayPoint> it = myWayPoints.listIterator();
      while (it.hasNext()) {
         WayPoint way = it.next();
         if (newWay.getTime() <= way.getTime()) {
            if (it.hasPrevious()) {
               it.previous();
               it.add (newWay);
            }
            else {
               myWayPoints.addFirst (newWay);
            }
            if (newWay.getTime() == way.getTime()) {
               myWayPoints.remove (way);
               return way;
            }
            else {
               return null;
            }
         }
      }
      myWayPoints.addLast (newWay);
      newWay.setValid (false);
      return null;
   }

   public Iterator<WayPoint> get() {
      return myWayPoints.iterator();
   }

   public boolean remove (WayPoint way) {
      if (way == myWay0) {
         return false;
      }
      else {
         return myWayPoints.remove (way);
      }
   }

   public void clear() {
      myWayPoints.clear();
      myWayPoints.add (myWay0);
   }

   public double nextEventTime (double t) {
      for (WayPoint way : myWayPoints) {
         if (TimeBase.compare (way.getTime(), t) > 0) {
            return way.getTime();
         }
      }
      return -1;
   }

   public boolean isEventTime (double t) {
      for (WayPoint way : myWayPoints) {
         int comp = TimeBase.compare (way.getTime(), t);
         if (comp == 0) {
            return true;
         }
         else if (comp > 0) {
            return false;
         }
      }
      return false;
   }

   public int size() {
      return myWayPoints.size();
   }
   
   public int numValid() {
      int count = 0;
      for (WayPoint way : myWayPoints) {
         if (way.isValid ())
            count++;
      }
      return count;
   }

   public double maxEventTime() {
      double max = 0;
      // XXX think we can just use the last way point here and forget the loop
      for (WayPoint way : myWayPoints) {
         double t = way.getTime();
         if (t > max) {
            max = t;
         }
      }
      return max;
   }

   public LinkedList<WayPoint> getPoints() {
      return myWayPoints;
   }

   public WayPoint getAfter (double t) {
      for (WayPoint way : myWayPoints) {
         if (TimeBase.compare (t, way.getTime()) < 0) {
            return way;
         }
      }
      return null;
   }

   public WayPoint getValidAfter (double t) {
      for (WayPoint way : myWayPoints) {
         if (TimeBase.compare (t, way.getTime()) < 0 && way.myValidP) {
            return way;
         }
      }
      return null;
   }
   
  /** 
   * Find the nearest valid waypoint to a time t, whose time
   * is less or equal to t. Return null if there is no such waypoint.
   */
   public WayPoint getNearestValidBefore (double time) {
      
      WayPoint nearest = null;
      for (WayPoint way : myWayPoints) {
         if (way.getTime() > time) {
            break;
         }
         else if (way.isValid()) {
            nearest = way;
         }
      }
      return nearest;
   }

   public WayPoint getLastValid () {
      ListIterator<WayPoint> it = myWayPoints.listIterator();
      WayPoint prev = null;
      while (it.hasNext()) {
         WayPoint way = it.next();
         if (!way.isValid()) {
            break;
         }
         prev = way;
      }
      return prev;
   }

   public WayPoint getBreakPointAfter (double time) {
      for (WayPoint way : myWayPoints) {
         if (TimeBase.compare (way.getTime(), time) > 0 && 
             way.isBreakPoint()) {
            return way;
         }
      }
      return null;
   }

   /**
    * Return the waypoint immediately on or before time, or null if
    * there is no such waypoint.
    */
   public WayPoint getValidOnOrBefore (double time) {
      WayPoint prev = null;
      for (WayPoint way : myWayPoints) {
         if (TimeBase.compare (way.getTime(), time) <= 0) {
            if (way.isValid()) {
               prev = way;
            }
         }
         else {
            break;
         }
      }
      return prev;
   }

   /**
    * Return the valid waypoint immediately before time, or null if
    * there is no such waypoint.
    */
   public WayPoint getValidBefore (double time) {
      WayPoint prev = null;
      for (WayPoint way : myWayPoints) {
         if (TimeBase.compare (way.getTime(), time) < 0) {
            if (way.isValid()) {
               prev = way;
            }
         }
         else {
            break;
         }
      }
      return prev;
   }

   public WayPoint get (double time) {
      for (WayPoint way : myWayPoints) {
         double t = way.getTime();
         if (t == time) {
            return way;
         }
         else if (t > time) {
            break;
         }
      }
      return null;
   }

   public void apply (double t) {
      
      Iterator<WayPoint> it = myWayPoints.iterator();
      while (it.hasNext()) {
         WayPoint way = it.next();
         if (TimeBase.equals (way.getTime(), t)) {
            ComponentState checkState = null;
            if (myCheckStateP && way.isValid()) {
               checkState = way.getState().duplicate();
            }
            way.setState (myRootModel);
            if (myCheckStateP && checkState != null) {
               if (!checkState.equals (way.getState())) {
                  System.out.println ("States unequal at time "+t);
               }
            }
         }
      }
   }

   /**
    * Invalidates all waypoints after a specified time.
    * 
    * @param t
    * invalidate waypoints after this time
    */
   public void invalidateAfterTime (double t) {
      Iterator<WayPoint> it = myWayPoints.iterator();
      while (it.hasNext()) {
         WayPoint way = it.next();
         if (way.getTime() > t) {
            way.setValid (false);
         }
      }
   }

   /**
    * Invalidates all waypoints
    */
   public void invalidateAll() {
      Iterator<WayPoint> it = myWayPoints.iterator();
      while (it.hasNext()) {
         WayPoint way = it.next();
         way.setValid (false);
      }
   }
   
   /**
    * Write all waypoint state to the attached file if it exists.
    * 
    */
   public void save () {
      File file = getAttachedFile ();
      if (file != null) {
         try {
            if (isAttachedFileRelative ()) {
               file.getParentFile ().mkdirs ();
            }
            System.out.println ("saving waypoint data to " + file.getName ());
            DataOutputStream dos =
               new DataOutputStream (new FileOutputStream (file));
            dos.writeInt (numValid ());
            for (WayPoint way : myWayPoints) {
               if (way.getState () != null) {
                  System.out.println (" writing way " + way.getTime());
                  dos.writeDouble (way.getTime());
                  way.getState ().writeBinary (dos);
               }
            }
         }
         catch (Exception e) {
            System.out.println ("Error writing file " + file.getName ());
            e.printStackTrace ();
         }
      }
   }

   public void saveas () {
      setAttachedFileFromUser ("Save As");
      save ();
   }
   
   public void saveas(String fileName) {
      setAttachedFileName(fileName);
      save ();
   }

   /**
    * Load waypoint state data from the attached file.
    * 
    */
   public void load () {
      invalidateAll();
      File file = getAttachedFile ();
      if (file != null) {
         if (!file.exists ()) {
            System.out.println ("Input probe file " + file.getName ()
            + " does not exist");
         }
         else {
            // read data from binary file
            try {
               DataInputStream dis =
                  new DataInputStream (new FileInputStream (file));

               ListIterator<WayPoint> li = myWayPoints.listIterator();

               int numvalid = dis.readInt();
               for (int i = 0; i < numvalid; i++) {
                  double time = dis.readDouble();
                  
                  WayPoint way = null;
                  if (li.hasNext()) {
                     while (li.hasNext()) {
                        way = li.next();
                        if (way.getTime() >= time) {
                           break;
                        }
                     }
                     if (TimeBase.equals (way.getTime(), time)) {
                        // use current way point
                     }
                     else if (way.getTime() > time) {
                        // use new WayPoint added before the current one
                        li.previous();
                        way = new WayPoint(time);
                        li.add (way);
                     }
                     else { // way.getTime() < time
                        // use new WayPoint added at the end of this list
                        way = new WayPoint(time);
                        li.add (way);
                     }
                     // else use current way point
                  }
                  else {
                     // use new WayPoint added at the end of this list
                     way = new WayPoint(time);
                     li.add (way);
                  }
                  CompositeState cs = (CompositeState)myRootModel.createState (null);
                  cs.readBinary (dis);
                  way.setState (cs);
               }
            }
            catch (IOException e) {
               System.err.println ("Could not load waypoint data: \n   " + e.getMessage ());
            }
            //Main.getTimeline ().updateToolbar();
         }
      }
   }

   public void loadfrom (String fileName) {
      setAttachedFileName(fileName);
      load ();
   }
   
   public void loadfrom () {
      setAttachedFileFromUser ("Load From");
      load ();
   }
   
   private void setAttachedFileFromUser(String text) {
      String workspace = new String (ArtisynthPath.getWorkingDirPath());
      File current = getAttachedFile();

      if (current == null)
         current = new File (workspace);

      String absfile = null;
      JFileChooser myFileChooser = new JFileChooser();
      myFileChooser.setCurrentDirectory (current);
      myFileChooser.setFileSelectionMode (JFileChooser.FILES_ONLY);
      myFileChooser.setApproveButtonText (text);
      int returnVal;
      if (text == "Save As")
         returnVal = myFileChooser.showSaveDialog (Main.getTimeline ());
      else if (text == "Load From")
         returnVal = myFileChooser.showOpenDialog (Main.getTimeline ());
      else {
         System.out.println("warning unknown filechooser type " + text);
         returnVal = myFileChooser.showSaveDialog (Main.getTimeline ());
      }
         
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         try {
            absfile = myFileChooser.getSelectedFile().getCanonicalPath();
         }
         catch (Exception e) {
            System.err.println ("File chooser: unable to get canonical path");
            e.printStackTrace();
         }
      }

      if (absfile != null) {
         if (absfile.startsWith (workspace))
            absfile = new String (absfile.substring (workspace.length() + 1));

         System.out.println ("Workspace: " + workspace);
         System.out.println ("Selected file address: " + absfile);
         
         setAttachedFileName (absfile);
      }
   }
   
   
   @Override
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      if (getAttachedFileName() != null) {
         load();
      }
   }
   

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "wayPoints")) {
         myWayPoints.clear();
         myWayPoints.add (myWay0);
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            double time = scanTimeQuantity(rtok);
            if (time != 0) {
               WayPoint newWay = new WayPoint (time);
               myWayPoints.addLast (newWay);
               newWay.setValid (false);

               // check for "breakpoint", which signifies breakpoint
               rtok.nextToken();
               if (rtok.ttype == ReaderTokenizer.TT_WORD &&
               rtok.sval.equals ("breakpoint"))
                  newWay.setBreakPoint (true);
               else
                  rtok.pushBack();
            }
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);

      if (myWayPoints.size() > 0) {
         pw.println ("wayPoints=\n[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (WayPoint way : myWayPoints) {
            if (Probe.writeStartStopTimeInSeconds) {
               pw.print (way.getTime());
            }
            else {
               pw.print ((long)(1e9*way.getTime()));
            }
            if (way.isBreakPoint())
               pw.print (" breakpoint");
            pw.println();
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   public RootModel getRootModel() {
      return myRootModel;
   }

}
