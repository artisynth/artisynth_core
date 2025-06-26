/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import java.util.Iterator;
import java.util.NoSuchElementException;

import maspack.interpolation.NumericListKnot;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.render.HasRenderProps;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import artisynth.core.modelbase.ModelComponent;

public class PointTracingProbe extends TracingProbe {
   ModelComponent myComponent;
   boolean myPointTracing = false;

   public static PropertyList myProps =
      new PropertyList (PointTracingProbe.class, TracingProbe.class);

   static {
      myProps.add ("pointTracing", "trace as points vs. lines", false);
   }

   public boolean getPointTracing() {
      return myPointTracing;
   }

   public void setPointTracing (boolean enable) {
      myPointTracing = enable;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createPointLineProps (this);
   }

   private void initRenderProps (ModelComponent comp) {
      RenderProps newProps = createRenderProps();
      if (comp instanceof HasRenderProps) {
         HasRenderProps rcomp = (HasRenderProps)comp;
         RenderProps compProps = rcomp.getRenderProps();
         if (compProps == null) {
            compProps = rcomp.createRenderProps();
         }
         newProps.set (compProps);
         // Set the line color from the point color
         float[] pointColor = compProps.getPointColorF();
         newProps.setLineColor (pointColor);
      }
      newProps.setLineWidth (3);
      newProps.setPointSize (3);
      setRenderProps (newProps);
   }

   private class VertexIterator implements Iterator<float[]> {
      double myTime;
      double myStep;
      NumericListKnot myKnot;

      VectorNd myVec = new VectorNd (3);
      double[] myBuf = myVec.getBuffer();
      float[] myVtx = new float[3];

      public boolean hasNext() {
         return (myKnot != null);
      }

      public float[] next() {
         if (myKnot == null) {
            throw new NoSuchElementException();
         }
         if (myStep <= 0) {
            myVec.set (myKnot.v);
            myKnot = myKnot.getNext();
         }
         else {
            NumericListKnot last =
               myNumericList.interpolate (
                  myVec, myTime, getInterpolation(), myKnot);
            myTime += myStep;
            myKnot = myNumericList.findKnotAtOrBefore (myTime, last);
            if (myKnot.getNext() == null &&
                myTime > myKnot.t) {
               myKnot = null;
            }
         }
         myVtx[0] = (float)myBuf[0];
         myVtx[1] = (float)myBuf[1];
         myVtx[2] = (float)myBuf[2];
         return myVtx;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      VertexIterator (double step) {
         myKnot = myNumericList.getKnot(0);
         myTime = 0;
         myStep = step;
      }
   }

   private class VertexIterable implements Iterable<float[]> {

      VertexIterable () {
      }

      public Iterator<float[]> iterator() {
         double interval = myRenderInterval;
         if (interval <= 0) {
            interval = getUpdateInterval();
         }
         double step = myRenderInterval/myScale;
         return new VertexIterator (step);
      }
   }

   public PointTracingProbe() {
      super();
      initVsize (3);
      createNumericList ();
   }

   public PointTracingProbe (
      ModelComponent comp, Property prop, double interval) {
      super (prop, interval);
      myComponent = comp;
      initRenderProps (comp);
      if (getVsize() != 3) {
         throw new IllegalArgumentException ("property '" + prop.getName()
         + "' does not map to a 3-tuple");
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      if (myNumericList == null) {
         return;
      }
      Point3d pnt = new Point3d();
      for (NumericListKnot knot : myNumericList) {
         VectorNd v = knot.v;
         pnt.x = v.get (0);
         pnt.y = v.get (1);
         pnt.z = v.get (2);
         pnt.updateBounds (pmin, pmax);
      }
   }

   public void render (Renderer renderer, int flags) {
      if (myNumericList == null || myNumericList.isEmpty()) {
         return;
      }
      double tend = myNumericList.getLast().t;
      VertexIterable vi = new VertexIterable ();
      if (myPointTracing) {
         for (float[] vtx : vi) {
            renderer.drawPoint (myRenderProps, vtx, isSelected());
         }
      }
      else {
         renderer.drawLineStrip (
            myRenderProps, vi, myRenderProps.getLineStyle(), isSelected());
      }
   }

   public Object clone() throws CloneNotSupportedException {
      PointTracingProbe probe = (PointTracingProbe)super.clone();
      probe.setRenderProps (createRenderProps());
      return probe;
   }

}
