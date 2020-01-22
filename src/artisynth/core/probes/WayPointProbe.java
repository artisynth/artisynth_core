/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.util.*;

public class WayPointProbe extends OutputProbe implements Iterable<WayPoint> {
   protected TreeMap<Double,WayPoint> myWayPoints;
   protected RootModel myRootModel; // root model associated with the waypoints
   protected WayPoint myWay0; // hard wired way point at time = 0
   protected boolean myCheckStateP = false; // for testing only
   
   private boolean myResetInitialState = false;
   private boolean myInitialStateValidP = true;

   public static final int WRITE_FIRST_STATE = 0x01;
   public static final int WRITE_ALL_STATES = 0x02;

   protected int myWriteFlags = 0;

   private class MyIterator implements Iterator<WayPoint> {

      WayPoint myNext;

      MyIterator() {
         myNext = myWay0;
      }

      public boolean hasNext() {
         return myNext != null;
      }

      public WayPoint next() {
         WayPoint way = myNext;
         if (way == null) {
            throw new NoSuchElementException();
         }
         myNext = way.myNext;
         return way;
      }
   }

   private class MyListIterator implements ListIterator<WayPoint> {

      WayPoint myNext;
      WayPoint myPrev;
      int myIndex;

      MyListIterator() {
         myNext = myWay0;
         myPrev = null;
         myIndex = -1;
      }

      public boolean hasNext() {
         return myNext != null;
      }

      public WayPoint next() {
         WayPoint way = myNext;
         if (way == null) {
            throw new NoSuchElementException();
         }
         myNext = way.myNext;
         myPrev = way;
         myIndex++;
         return way;
      }

      public boolean hasPrevious() {
         return myPrev != null;
      }

      public WayPoint previous() {
         WayPoint way = myPrev;
         if (way == null) {
            throw new NoSuchElementException();
         }
         myPrev = way.myPrev;
         myNext = way;
         myIndex--;
         return way;
      }

      public int previousIndex() {
         return myIndex;
      }

      public int nextIndex() {
         return myIndex+1;
      }

      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }

      public void set (WayPoint obj) throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }

      public void add (WayPoint obj) throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }

   public WayPointProbe() {
      setStartTime (0);
      setStopTime (Double.POSITIVE_INFINITY);
      setUpdateInterval (-1);
      myActiveP = true;
      myWay0 = new WayPoint (0);
      myWayPoints = new TreeMap<Double,WayPoint>();
      myWayPoints.put (0.0, myWay0);
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

   public void resetInitialState() {
      RootModel root = getRootModel();
      WayPoint way0 = get (0);
      if (root != null && way0 != null) {
         CompositeState state = (CompositeState)root.createState(null);
         root.getInitialState (state, null);
         way0.setState (state);
         myInitialStateValidP = true;
      }
      invalidateAfterTime (0.0);
   }
   
   public void invalidateInitialState() {
      myInitialStateValidP = false;
   }

   public void updateInitialStateIfNecessary() {
      if (!myInitialStateValidP) {
         RootModel root = getRootModel();
         WayPoint way0 = get (0);
         if (root != null && way0 != null) {
            CompositeState state = 
               (CompositeState)root.createState(null);
            if (myResetInitialState) {
               root.getInitialState (state, null);
               myResetInitialState = false;
            }
            else {
               root.getInitialState (state, way0.getState());
            }
            way0.setState (state);
         }
         myInitialStateValidP = true;
      }
   }
   
   /**
    * Adds a waypoint to this probe, and returns any waypoint that previously
    * occupied the same time slot.
    * 
    * @param newWay
    * new waypoint to add
    * @return previous waypoint with the same time, if any
    */
   public WayPoint add (WayPoint newWay) {
      if (newWay.getTime() <= 0) {
         throw new IllegalArgumentException (
            "Added WayPoint must have time > 0");
      }
      Map.Entry<Double,WayPoint> prevEntry =
         myWayPoints.lowerEntry (newWay.getTime());
      if (prevEntry == null) {
         throw new InternalErrorException (
            "Added WayPoint does not have a previous entry"); 
      }
      WayPoint prev = prevEntry.getValue();
      WayPoint next = prev.myNext;
      WayPoint oldWay = null;
      if (next != null && next.getTime() == newWay.getTime()) {
         // replace current way point
         oldWay = next;
         next = oldWay.myNext;
      }
      newWay.myPrev = prev;
      newWay.myNext = next;
      prev.myNext = newWay;
      if (next != null) {
         next.myPrev = newWay;
      }
      WayPoint old = myWayPoints.put (newWay.getTime(), newWay);
      if (old != oldWay) {
         throw new InternalErrorException (
            "TreeMap and linked list report different existing WayPoint");
      }
      return oldWay;
   }

   /**
    * Removes and returns the waypoint at time {@code t} in this probe.  If the
    * waypoint does not exist, or if {@code t <= 0}, {@code null} is returned.
    *
    * @param t time of the waypoint that should be removed
    * @return removed waypoint, or {@code null}
    */
   public WayPoint remove (double t) {
      double tround = TimeBase.round (t);
      if (tround <= 0) {
         return null;
      }
      else {
         WayPoint way = myWayPoints.remove (tround);
         if (way != null) {
            WayPoint prev = way.myPrev;
            WayPoint next = way.myNext;
            if (prev == null) {
               throw new InternalErrorException (
                  "Removed WayPoint does not have a previous WayPoint"); 
            }
            prev.myNext = next;
            if (next != null) {
               next.myPrev = prev;
            }
         }
         return way;
      }
   }

   /**
    * Removes a specified waypoint from this probe, if it exists.
    * 
    * @param way waypoint to remove
    * @return {@code true} if the waypoint exists and was removed
    */
   public boolean remove (WayPoint way) {
      if (way == myWay0) {
         return false;
      }
      else {
         return remove (way.getTime()) != null;
      }
   }

   /**
    * Clears all waypoints in this probe, except for the waypoint at time 0.
    */
   public void clear() {
      myWayPoints.clear();
      myWayPoints.put (0.0, myWay0);
      myWay0.myNext = null;
   }

   /**
    * {@inheritDoc}
    */
   public double nextEventTime (double t) {
      Map.Entry<Double,WayPoint> nextEntry =
         myWayPoints.higherEntry (TimeBase.round(t));
      if (nextEntry != null) {
         return nextEntry.getKey();
      }
      else {
         return -1;
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isEventTime (double t) {
      return myWayPoints.get (TimeBase.round(t)) != null;
   }

   /**
    * Queries the number of waypoints in this probe.
    * 
    * @return number of waypoints
    */
   public int size() {
      return myWayPoints.size();
   }
   
   /**
    * Queries the number of currently valid waypoints in this probe.
    * 
    * @return number of valid waypoints
    */
   public int numValid() {
      int count = 0;
      for (WayPoint way : myWayPoints.values()) {
         if (way.isValid ()) {
            count++;
         }
      }
      return count;
   }
   
   /**
    * Queries the maximum event time in this probe. This corresponds
    * to the waypoint with the largest time value.
    * 
    * @return largest waypoint time value
    */
   public double maxEventTime() {
      Map.Entry<Double,WayPoint> lastEntry = myWayPoints.lastEntry();
      if (lastEntry != null) {
         return lastEntry.getKey();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns an interator for the waypoints in this probe
    * 
    * @return waypoint iterator
    */
   public Iterator<WayPoint> iterator() {
      return new MyIterator();
   }

   /**
    * Returns a list interator for the waypoints in this probe
    * 
    * @return waypoint iterator
    */
   public ListIterator<WayPoint> listIterator() {
      return new MyListIterator();
   }

   /**
    * Returns a {@code Collection} view of the waypoints in this probe.
    * 
    * @return collection of all the waypoints
    */
   public Collection<WayPoint> getPoints() {
      return myWayPoints.values();
   }

   /**
    * Returns the next waypoint immediately after time {@code t}, or {@code
    * null} if none exists.
    *
    * @param t time after which next waypoint is sought
    * @return next waypoint after {@code t}, or {@code null}
    */
   public WayPoint getAfter (double t) {
      Map.Entry<Double,WayPoint> nextEntry =
         myWayPoints.higherEntry(TimeBase.round(t));
      if (nextEntry != null) {
         return nextEntry.getValue();
      }
      else {
         return null;
      }      
   }

   /**
    * Returns the next <i>valid</i> waypoint immediately after time {@code t},
    * or {@code null} if none exists.
    * 
    * @param t time after which next valid waypoint is sought
    * @return next valid waypoint after {@code t}, or {@code null}
    */
   public WayPoint getValidAfter (double t) {
      Map.Entry<Double,WayPoint> nextEntry =
         myWayPoints.higherEntry(TimeBase.round(t));
      if (nextEntry != null) {
         WayPoint way = nextEntry.getValue();
         // search forward looking for first valid way point
         while (way != null && !way.isValid()) {
            way = way.myNext;
         }
         return way;
      }
      else {
         return null;
      }      
   }
   
   /**
    * Returns the last valid waypoint
    *
    * @return last valid waypoint
    */
   public WayPoint getLastValid () {
      WayPoint prev = null;
      for (WayPoint way : myWayPoints.values()) {
         if (!way.isValid()) {
            break;
         }
         prev = way;
      }
      return prev;
   }

   /**
    * Returns the next breakpoint immediately after time {@code t},
    * or {@code null} if none exists.
    * 
    * @param t time after which next breakpoint is sought
    * @return next breakpoint after {@code t}, or {@code null}
    */
   public WayPoint getBreakPointAfter (double t) {
      Map.Entry<Double,WayPoint> nextEntry =
         myWayPoints.higherEntry (TimeBase.round(t));
      if (nextEntry != null) {
         WayPoint way = nextEntry.getValue();
         // search forward looing for break point
         while (way != null && !way.isBreakPoint()) {
            way = way.myNext;
         }
         return way;
      }
      else {
         return null;
      }
   }

   /**
    * Returns the <i>valid</i> waypoint immediately at or before time {@code
    * t}, or {@code null} if none exists.
    * 
    * @param t time at or before which valid waypoint is sought
    * @return valid waypoint at or before {@code t}, or {@code null}
    */
   public WayPoint getValidOnOrBefore (double t) {
      double tround = TimeBase.round(t);
      if (tround < 0) {
         return null;
      }
      else if (tround == 0) {
         if (myWay0.isValid()) {
            return myWay0;
         }
         else {
            return null;
         }
      }
      else {
         Map.Entry<Double,WayPoint> prevEntry = myWayPoints.lowerEntry (tround);
         if (prevEntry == null) {
            throw new InternalErrorException (
               "No WayPoint with time < " + tround);
         }
         WayPoint way = prevEntry.getValue();
         if (way.myNext != null && way.myNext.getTime() == tround) {
            // bump starting waypoint to one with time == t
            way = way.myNext;
         }
         // search backward looking for first valid waypoint
         while (way != null && !way.isValid()) {
            way = way.myPrev;
         }
         return way;               
      }
   }

   /**
    * Returns the previous <i>valid</i> waypoint immediately before time {@code
    * t}, or {@code null} if none exists.
    * 
    * @param t time before which previous valid waypoint is sought
    * @return previous valid waypoint before {@code t}, or {@code null}
    */
   public WayPoint getValidBefore (double t) {
      double tround = TimeBase.round(t);
      if (tround <= 0) {
         return null;
      }
      else {
         Map.Entry<Double,WayPoint> prevEntry = myWayPoints.lowerEntry (tround);
         if (prevEntry == null) {
            throw new InternalErrorException (
               "No WayPoint with time < " + tround);
         }
         WayPoint way = prevEntry.getValue();
         // search backward looking for first valid waypoint
         while (way != null && !way.isValid()) {
            way = way.myPrev;
         }
         return way;               
      }
   }

   /**
    * Returns the waypoint at time {@code t}, or {@code null} if none exists.
    *
    * @param t time at which waypoint is sought
    * @return waypoint at {@code t}, or {@code null}
    */
   public WayPoint get (double t) {
      return myWayPoints.get (TimeBase.round(t));
   }
   
   /**
    * Returns the waypoint at a specified positional index.
    * 
    * @param idx index of the requested waypoint
    * @return waypoint at index {@code ix}
    * @throws IndexOutOfBoundsException if the index is out of bounds
    */
   public WayPoint getByIndex (int idx) {
      if (idx < 0 || idx >= size()) {
         throw new IndexOutOfBoundsException ("index="+idx+", size="+size());
      }
      WayPoint way = myWay0;
      for (int i=0; i<idx; i++) {
         way = way.myNext;
      }
      return way;
   }

   /**
    * {@inheritDoc}
    */
   public void apply (double t) {
      double tround = TimeBase.round(t);
      if (tround == 0) {
         // skip for t == 0 since way point should already be set
         return;
      }
      WayPoint way = myWayPoints.get(tround);
      if (way != null) {
         ComponentState checkState = null;
         if (myCheckStateP && way.isValid()) {
            checkState = way.getState().duplicate();
         }
         way.setState (myRootModel);
         if (myCheckStateP && checkState != null) {
            if (!checkState.equals (way.getState(), null)) {
               System.out.println ("States unequal at time "+tround);
            }
         }
      }
   }

   /**
    * Invalidates all waypoints after a specified time.
    * 
    * @param t time after which waypoints will be invalidated
    */
   public void invalidateAfterTime (double t) {
      double tround = TimeBase.round(t);
      WayPoint way = null;
      if (tround <= 0) {
         way = myWay0.myNext;
      }
      else {
         Map.Entry<Double,WayPoint> nextEntry = myWayPoints.higherEntry (tround);
         if (nextEntry != null) {
            way = nextEntry.getValue();
         }
      }
      while (way != null) {
         way.setValid (false);
         way = way.myNext;
      }
   }

//   /**
//    * Invalidates all waypoints in this probe
//    */
//   public void invalidateAll() {
//      for (WayPoint way : myWayPoints.values()) {
//         way.setValid (false);
//      }
//   }
   
   /**
    * Writes all waypoints and their state as binary data to the attached 
    * file if it exists.
    */
   public void save () throws IOException {
      updateInitialStateIfNecessary();
      File file = getAttachedFile ();
      if (file != null) {
         if (isAttachedFileRelative ()) {
            file.getParentFile ().mkdirs ();
         }
         DataOutputStream dos =
            new DataOutputStream (
               new BufferedOutputStream(new FileOutputStream (file)));
         try {
            dos.writeInt (myWayPoints.size());
            for (WayPoint way : myWayPoints.values()) {
               dos.writeDouble (way.getTime());
               dos.writeBoolean (way.isBreakPoint());
               if (way.isValid() && way.getState () != null) {
                  dos.writeBoolean (true);
                  way.getState ().writeBinary (dos);
               }
               else {
                  dos.writeBoolean (false);
               }
            }
         }
         catch (IOException e) {
            throw e;
         }
         finally {
            dos.close ();
         }
      }
   }


   public void saveas(String fileName) {
      setAttachedFileName(fileName);
      try {
         save ();
      }
      catch (IOException e){
         e.printStackTrace();
      }
   }

   /**
    * Loads waypoints and state from binary data in the attached file.
    */
   public void load () throws IOException {
      updateInitialStateIfNecessary();
      //invalidateAll();
      File file = getAttachedFile ();
      if (file != null) {
         if (!file.exists ()) {
            throw new IOException ("File '"+file+"' does not exist");
         }
         else if (!file.canRead ()) {
            throw new IOException ("File '"+file+"' is not readable");
         }
         else {
            // read data from binary file
            DataInputStream dis =
               new DataInputStream (
                  new BufferedInputStream(new FileInputStream (file)));
            try {
               int numways = dis.readInt();
               for (int i = 0; i < numways; i++) {
                  double t = dis.readDouble();
                  WayPoint way = get(t);
                  if (way == null) {
                     way = new WayPoint(t);
                     add (way);
                  }
                  way.setBreakPoint (dis.readBoolean());
                  boolean hasState = dis.readBoolean();
                  if (hasState) {
                     CompositeState cs = way.getState();
                     if (cs == null) {
                        cs = (CompositeState)myRootModel.createState (null);
                     }
                     cs.readBinary (dis);
                     way.setState (cs); // note: cs is set by reference
                  }
               }
            }
            catch (IOException e) {
               throw e;
            }
            finally {
               dis.close ();
            }
         }
      }
   }

   /**
    * Loads waypoint data from the specified file.
    *
    * @param fileName file to waypoints from
    */
   public void loadfrom (String fileName) {
      setAttachedFileName(fileName);
      try {
         load ();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   @Override
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
   }
   
   private void scanWayPoints (ReaderTokenizer rtok) throws IOException {
      WayPoint way = myWay0;
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         rtok.scanToken ('[');
         if (way == null) {
            way = new WayPoint (0);
         }
         while (rtok.nextToken() != ']') {
            if (scanAttributeName (rtok, "time")) {
               double t = scanTimeQuantity(rtok);
               if (t != 0) {
                  way.setTime (t);
               }
            }
            else if (scanAttributeName (rtok, "breakpoint")) {
               way.setBreakPoint (rtok.scanBoolean());
            }
            else if (scanAttributeName (rtok, "state")) {
               CompositeState state;
//               if (way.getTime() == 0) {
//                  // Create an initial state from root model, to get the 
//                  // annotations, and then override it's numeric contents 
//                  // with the saved state                  
//                  way.setState (myRootModel);
//                  way.getState().scan (rtok, null);
//               }
//               else {
                  state = new CompositeState();
                  state.scan (rtok, null);
                  way.setState (state);
               //}
            }
            else {
               throw new IOException ("Unexpected input: " + rtok);
            }
         }
         if (way != myWay0) {
            add (way);
         }
         way = null;
      }
   }

   private void scanWayPointsLegacy (ReaderTokenizer rtok) throws IOException {
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         double t = scanTimeQuantity(rtok);
         if (t != 0) {
            WayPoint newWay = new WayPoint (t);
            add (newWay);
            newWay.setValid (false);

            // check for "breakpoint", which signifies breakpoint
            rtok.nextToken();
            if (rtok.ttype == ReaderTokenizer.TT_WORD &&
                rtok.sval.equals ("breakpoint")) {
               newWay.setBreakPoint (true);
            }
            else {
               rtok.pushBack();
            }
         }
      }      
   } 

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "wayPoints")) {
         clear();
         rtok.scanToken ('[');
         if (rtok.nextToken() == '[') {
            rtok.pushBack();
            scanWayPoints (rtok);
         }
         else {
            rtok.pushBack();
            scanWayPointsLegacy (rtok);
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public int getWriteFlags() {
      return myWriteFlags;
   }
   
   public void setWriteFlags (int flags) {
      myWriteFlags = flags;
   }
   
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);

      if (myWayPoints.size() > 0) {
         pw.println ("wayPoints=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         int i = 0;
         for (WayPoint way : myWayPoints.values()) {
            pw.print ("[ ");
            IndentingPrintWriter.addIndentation (pw, 2);
            if (Probe.writeStartStopTimeInSeconds) {
               pw.println ("time=" + way.getTime());
            }
            else {
               pw.println ("time=" + (long)(1e9*way.getTime()));
            }
            if (way.isBreakPoint()) {
               pw.println ("breakpoint=true");
            }
            boolean writeState =
               (((myWriteFlags & WRITE_FIRST_STATE) != 0 && i == 0) ||
                ((myWriteFlags & WRITE_ALL_STATES) != 0));
            if (way.isValid() && writeState) {
               pw.print ("state=");
               CompositeState state = way.getState();
               state.write (pw, fmt, ancestor);
            }
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
            i++;
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   /**
    * Returns the RootModel associated with this probe.
    * 
    * @return RootModel associated with this probe
    */
   public RootModel getRootModel() {
      return myRootModel;
   }

}
