/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;

import javax.crypto.spec.IvParameterSpec;

import artisynth.core.modelbase.*;
import maspack.matrix.Point3d;

public class MultiPointMuscleVia extends MultiPointMuscle {
   
   public class ViaPoint {
      Point p0;
      Point p1;
      double s; // distance between p0 and p1 to insert via point when len > activelen
      double len; // distance between p0 and p1 when via point become active
      Point pnt;

      public ViaPoint(Point p, double l, double s, Point p0, Point p1) {
	 pnt = p;
	 len = l;
	 this.s = s;
	 this.p0 = p0;
	 this.p1 = p1;
      }
      
      public boolean isActive() {
	 return p0.distance(p1) > len;
      }
      
      public void updateLocation() {
	 Point3d pos = new Point3d();
	 pos.interpolate(p0.getPosition(), s, p1.getPosition());
	 if (pnt instanceof FrameMarker) {
	    ((FrameMarker) pnt).setWorldLocation(pos);
	 }
	 else {
	    pnt.setPosition(pos);
	 }
      }
   }
   
   ArrayList<ViaPoint> viaPoints = new ArrayList<ViaPoint>();
   
   public ViaPoint addViaPoint(Point p, double l, double s, Point p0, Point p1) {
      ViaPoint vp = new ViaPoint(p, l, s, p0, p1);
      viaPoints.add(vp);
      return vp;
   }
   
   public void clearViaPoints() {
      viaPoints.clear();
   }
   
   public void updateViaPoints() {
      for (ViaPoint p : viaPoints) {
	 if (p.isActive() && !containsPoint(p.pnt)) {
	    // add via point at distance s
	    System.out.println("adding via "+p.pnt.getName());
	    p.updateLocation();
	    addPoint (indexOfPoint(p.p0), p.pnt);
	 }
	 else if (!p.isActive() && containsPoint(p.pnt)) {
	    // remove via point
	    System.out.println("removing via "+p.pnt.getName());
	    removePoint (p.pnt);
	    invalidateSegments();
	 }
      }
   }

   public void updateStructure() {
      updateViaPoints();
   }
}
