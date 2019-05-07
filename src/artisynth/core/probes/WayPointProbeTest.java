package artisynth.core.probes;

import java.util.ListIterator;
import maspack.util.*;

public class WayPointProbeTest extends UnitTest {

   void validate (WayPointProbe wayPoints) {
      int i = 0;
      WayPoint prev = null;
      for (WayPoint way : wayPoints) {
         check ("invalid prev for way point "+i, way.myPrev == prev);
         if (prev != null) {
            check ("invalid next for way point "+(i-1), prev.myNext == way);
         }
         prev = way;
         i++;
      }
      if (prev != null) {
         check ("next != null for last way point", prev.myNext == null);
      }
      checkEquals ("iterated size", i, wayPoints.size());

      // check the list iterator. Start by copying all waypoints into an array
      WayPoint[] ways = new WayPoint[wayPoints.size()];
      i = 0;
      for (WayPoint way : wayPoints) {
         ways[i++] = way;
      }
      // now compare the array against the iterator
      ListIterator<WayPoint> li = wayPoints.listIterator();
      i = 0;
      while (li.hasNext()) {
         checkEquals ("list iterator next index", li.nextIndex(), i);
         checkEquals ("list iterator prev index", li.previousIndex(), i-1);
         WayPoint way = li.next();
         checkEquals ("listIterator.next()", way, ways[i]);
         i++;
      }
      checkEquals ("list iterated size", i, wayPoints.size());
      while (li.hasPrevious()) {
         checkEquals ("list iterator next index", li.nextIndex(), i);
         checkEquals ("list iterator prev index", li.previousIndex(), i-1);
         WayPoint way = li.previous();
         i--;
         checkEquals ("listIterator.previous()", way, ways[i]);
      }
      checkEquals ("list iterator prev index", li.previousIndex(), -1);
   }

   String wayName (WayPoint way) {
      if (way == null) {
         return null;
      }
      else {
         return ""+way.getTime();
      }
   }

   void checkEquals (String msg, WayPoint way0, WayPoint wchk) {
      if (way0 != wchk) {
         throw new TestException (
            msg + "=" + wayName(way0) + ", expecting " + wayName(wchk));
      }
   }

   public void test() {
      WayPointProbe wayPoints = new WayPointProbe();

      // times for waypoints
      double[] times = new double[] {
         1.0, 2.0, 7.0, 4.0, 6.0, 3.0, 5.0, 9.0, 8.0 };

      // add the waypoints to the probe, which will then sort them
      for (double t : times) {
         wayPoints.add (new WayPoint (t));
         validate (wayPoints);
      }

      // create an array of the the waypoints, in the sorted order
      // make all even waypoints both valid and a breakpoint
      int nways = wayPoints.size();
      WayPoint[] ways = new WayPoint[nways];
      int i = 0;
      for (WayPoint way : wayPoints) {
         ways[i] = way;
         if ((i%2) == 0) {
            way.setValid(true);
            way.setBreakPoint(true);
         }
         i++;
      }
      
      checkEquals ("num valid way points", wayPoints.numValid(), 5);
      checkEquals ("max event time", wayPoints.maxEventTime(), 9.0);
      checkEquals ("getByIndex(0)", wayPoints.getByIndex(0), ways[0]);
      checkEquals ("getByIndex(3)", wayPoints.getByIndex(3), ways[3]);

      // iterate through all waypoints, and for each one, test the various
      // methods to locate the next waypoint, next valid waypoint, previous
      // valid waypoint, etc.
      i = 0;
      for (WayPoint way : wayPoints) {
         double t = (double)i;
         check ("way point time != "+t, way.getTime() == t);
         check ("way point not returned by query, time "+t,
                wayPoints.get(t) == way);
         
         double nextEventTime = (i < nways-1 ? t+1.0 : -1.0);
         checkEquals (
            "next event time", wayPoints.nextEventTime(t), nextEventTime);
            
         WayPoint after = (i < nways-1 ? ways[i+1] : null);
         checkEquals (
            "next way point", wayPoints.getAfter(t), after);

         WayPoint validAfter;
         WayPoint validOnOrBefore; 
         WayPoint validBefore;
         if ((i%2) == 0) {
            // current way is valid and a break point
            validAfter = (i < nways-2 ? ways[i+2] : null);            
            validOnOrBefore = ways[i];
            validBefore = (i >= 2 ? ways[i-2] : null);
         }
         else {
            // current way is not valid and not a break point
            validAfter = (i < nways-1 ? ways[i+1] : null);
            validOnOrBefore = (i >= 1 ? ways[i-1] : null);
            validBefore = validOnOrBefore;
         }
         checkEquals (
            "next valid after", wayPoints.getValidAfter(t), validAfter);        
         checkEquals (
            "next break point after", wayPoints.getBreakPointAfter(t),
            validAfter);        
         checkEquals (
            "valid on or before", wayPoints.getValidOnOrBefore(t),
            validOnOrBefore);
         checkEquals (
            "valid before", wayPoints.getValidBefore(t), validBefore);
                
         i++;
      }

      // delete all the valid waypoints with even times, excluding 0
      for (i=0; i<nways; i+=2) {
         double t = (double)i;
         WayPoint way = wayPoints.remove (t);
         if (i == 0) {
            check ("way point at 0 was removed", way == null);
         }
         else if (way != null) {
            check ("removed way has incorrect time", way.getTime()==t);
         }
         else {
            throw new TestException ("waypoint was not removed");
         }
         validate (wayPoints);
      }
      
      checkEquals ("num valid way points", wayPoints.numValid(), 1);
      checkEquals ("num way points", wayPoints.size(), 6);
      checkEquals ("max event time", wayPoints.maxEventTime(), 9.0);

      // now set 5 to be valid and a breakpoint and do some tests with that
      wayPoints.get(5.0).setValid (true);
      wayPoints.get(5.0).setBreakPoint (true);

      checkEquals ("num valid way points", wayPoints.numValid(), 2);

      checkEquals (
         "next valid after 0", wayPoints.getValidAfter (0.0), ways[5]);
      checkEquals (
         "next valid after 2.5", wayPoints.getValidAfter (2.5), ways[5]);
      checkEquals (
         "next valid after 3", wayPoints.getValidAfter (3.0), ways[5]);
      checkEquals (
         "next valid after 5", wayPoints.getValidAfter (5.0), null);
      checkEquals (
         "next valid after 6", wayPoints.getValidAfter (6.0), null);
      checkEquals (
         "next valid after 9", wayPoints.getValidAfter (9.0), null);

      checkEquals (
         "next breakpoint after 0", wayPoints.getBreakPointAfter (0.0), ways[5]);
      checkEquals (
         "next breakpoint after 3", wayPoints.getBreakPointAfter (3.0), ways[5]);
      checkEquals (
         "next breakpoint after 5", wayPoints.getBreakPointAfter (5.0), null);
      checkEquals (
         "next breakpoint after 6", wayPoints.getBreakPointAfter (6.0), null);
      checkEquals (
         "next breakpoint after 9", wayPoints.getBreakPointAfter (9.0), null);

      checkEquals (
         "valid on or before 0", wayPoints.getValidOnOrBefore (0.0), ways[0]);
      checkEquals (
         "valid on or before 3", wayPoints.getValidOnOrBefore (3.0), ways[0]);
      checkEquals (
         "valid on or before 5", wayPoints.getValidOnOrBefore (5.0), ways[5]);
      checkEquals (
         "valid on or before 6", wayPoints.getValidOnOrBefore (6.0), ways[5]);
      checkEquals (
         "valid on or before 9", wayPoints.getValidOnOrBefore (9.0), ways[5]);

      checkEquals (
         "valid before 0", wayPoints.getValidBefore (0.0), null);
      checkEquals (
         "valid before 3", wayPoints.getValidBefore (3.0), ways[0]);
      checkEquals (
         "valid before 5", wayPoints.getValidBefore (5.0), ways[0]);
      checkEquals (
         "valid before 6", wayPoints.getValidBefore (6.0), ways[5]);
      checkEquals (
         "valid before 9", wayPoints.getValidBefore (9.0), ways[5]);

      wayPoints.clear();
      validate(wayPoints);

      for (i=1; i<10; i++) {
         ways[i].setValid (true);
         wayPoints.add (ways[i]);
      }
      validate(wayPoints);
      wayPoints.invalidateAfterTime (3.5);
      for (i=4; i<10; i++) {
         check ("way point "+i+" is valid", !ways[i].isValid());
      }      

   }

   public static void main (String[] args) {
      WayPointProbeTest tester = new WayPointProbeTest();
      tester.runtest();
   }
}
